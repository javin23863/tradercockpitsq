from copy import deepcopy
from io import BytesIO
import json
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch

from tradercockpit import mt5_data_setup as setup
from tradercockpit import mt5_metadata_probe as probe


class Mt5DataSetupTests(unittest.TestCase):
    def setUp(self):
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        root = Path(self.tmp.name)
        self.path = root / 'terminal64.exe'
        self.path.write_bytes(b'fixture')
        self.process = {'pid': 123, 'path': str(self.path.resolve()), 'started': '123456'}
        self.request = {'process': self.process, 'data_paths': [str(root.resolve())], 'symbol': None}
        self.payload = {'terminal_id': 'mt5-123', 'identity_sha256': setup._digest(self.process)}
        self.symbol = SimpleNamespace(name='MYM', digits=0, trade_tick_size=1, trade_tick_value=0.5,
                                      trade_contract_size=0.5, volume_min=1, volume_step=1, spread=0)
        self.api = Mock()
        self.api.initialize.return_value = True
        self.api.terminal_info.return_value = SimpleNamespace(path=str(root.resolve()), data_path=str(root.resolve()),
                                                             company='Terminal vendor', connected=True, build=5640)
        self.api.account_info.return_value = SimpleNamespace(company='Broker', server='Broker-Demo', currency='USD',
                                                             login=999999, name='PRIVATE', balance=987654)
        self.api.symbols_get.return_value = [self.symbol]
        self.api.symbol_info.return_value = self.symbol

    def result(self):
        return probe.read_metadata(self.request, self.api, lambda pid: self.process)

    def test_worker_reads_raw_metadata_without_account_or_price_math_leak(self):
        result = self.result()
        self.api.initialize.assert_called_once_with(str(self.path.resolve()), timeout=5000)
        self.api.shutdown.assert_called_once()
        self.assertEqual(result['broker'], {'company': 'Broker', 'server': 'Broker-Demo', 'currency': 'USD'})
        self.assertEqual(result['symbols'][0]['trade_tick_value'], 0.5)
        self.assertNotIn('point_value', result['symbols'][0])
        text = json.dumps(result)
        self.assertNotIn('999999', text)
        self.assertNotIn('PRIVATE', text)
        self.assertNotIn('balance', text)
        self.assertIsNone(result['symbols'][0]['currency_profit'])
        self.assertEqual(set(name for name, _, _ in self.api.mock_calls),
                         {'initialize', 'terminal_info', 'account_info', 'symbols_get', 'shutdown'})

    def test_worker_refuses_closed_changed_terminal_context_and_broker(self):
        with self.assertRaisesRegex(ValueError, 'mt5_terminal_changed'):
            probe.read_metadata(self.request, self.api, lambda pid: {**self.process, 'started': '999'})
        self.api.initialize.assert_not_called()
        self.api.terminal_info.return_value.data_path = str(Path(self.tmp.name) / 'other')
        with self.assertRaisesRegex(ValueError, 'mt5_terminal_context_mismatch'):
            self.result()
        self.api.terminal_info.return_value.data_path = self.request['data_paths'][0]
        self.api.account_info.side_effect = [SimpleNamespace(company='Broker', server='A', currency='USD'),
                                             SimpleNamespace(company='Broker', server='B', currency='USD')]
        with self.assertRaisesRegex(ValueError, 'mt5_broker_changed'):
            self.result()

    def test_worker_requires_login_context_and_exact_symbol_and_never_truncates(self):
        self.api.account_info.return_value = None
        with self.assertRaisesRegex(ValueError, 'mt5_account_unavailable'):
            self.result()
        self.api.account_info.return_value = SimpleNamespace(company='Broker', server='Demo', currency='USD')
        self.request['symbol'] = 'OTHER'
        with self.assertRaisesRegex(ValueError, 'mt5_symbol_unavailable'):
            self.result()
        self.api.symbol_info.assert_not_called()
        self.request['symbol'] = 'MYM'
        self.assertEqual(self.result()['selected_symbol']['name'], 'MYM')
        with patch.object(probe, 'MAX_SYMBOLS', 0), self.assertRaisesRegex(ValueError, 'mt5_metadata_limit'):
            self.result()

    def test_literal_filter_narrows_native_catalog_without_truncation(self):
        self.api.symbols_get.side_effect = lambda **kwargs: [self.symbol] if kwargs == {'group': '*MYM*'} else [self.symbol] * 3
        with patch.object(probe, 'MAX_SYMBOLS', 2):
            with self.assertRaisesRegex(ValueError, 'mt5_metadata_limit'):
                self.result()
            self.request['symbol_filter'] = 'MYM'
            self.request['symbol'] = 'MYM'
            self.assertEqual([r['name'] for r in self.result()['symbols']], ['MYM'])
        self.api.symbols_get.assert_called_with(group='*MYM*')
        with patch.object(setup, '_runtime') as runtime:
            for value in ('*', '!MYM', 'MYM,EUR', 'MY?', 'MY\\M', 'x', '  ', 'x'*65):
                with self.subTest(value=value), self.assertRaisesRegex(setup.Mt5DataSetupError, 'mt5_request_invalid'):
                    setup.read_mt5_metadata(None, {**self.payload, 'symbol_filter': value}, register_worker=Mock())
            runtime.assert_not_called()

    def test_catalog_is_passive_and_stale_or_missing_supervisor_cannot_spawn(self):
        with patch.object(setup, '_runtime', return_value=self.path), patch.object(setup, '_running_terminals', return_value=[self.process]), patch.object(setup, '_probe') as run:
            catalog = setup.read_mt5_terminal_catalog(None)
            self.assertEqual(catalog['terminals'][0]['identity_sha256'], self.payload['identity_sha256'])
            run.assert_not_called()
            for payload, reason in (({**self.payload, 'path': 'C:/elsewhere'}, 'mt5_request_invalid'),
                                    ({**self.payload, 'identity_sha256': '0'*64}, 'mt5_terminal_changed'),
                                    (self.payload, 'mt5_supervisor_unavailable')):
                with self.subTest(reason=reason), self.assertRaises(setup.Mt5DataSetupError) as caught:
                    setup.read_mt5_metadata(None, payload)
                self.assertEqual(caught.exception.code, reason)
            run.assert_not_called()
        with patch.object(setup, '_runtime'), patch.object(setup, '_running_terminals', return_value=[]):
            self.assertEqual(setup.read_mt5_terminal_catalog(None)['status'], 'no_running_terminal')

    def test_parent_independently_rejects_extra_private_fields_bad_numbers_and_identity(self):
        result = self.result()
        for mutate in (lambda r: r['broker'].update(login=99999),
                       lambda r: r['symbols'][0].update(trade_tick_size=float('nan')),
                       lambda r: r['symbols'][0].update(digits=True),
                       lambda r: r.update(process={**self.process, 'started': '999'}),
                       lambda r: r.update(selected_symbol=r['symbols'][0]),
                       lambda r: r['terminal'].update(connected=False)):
            changed = deepcopy(result)
            mutate(changed)
            with self.assertRaises(setup.Mt5DataSetupError):
                setup._validate_result(changed, self.request)

    def test_public_response_has_only_observed_metadata_and_checks_identity_after_read(self):
        result = self.result()
        with patch.object(setup, '_runtime', return_value=self.path), patch.object(setup, '_running_terminals', return_value=[self.process]), patch.object(setup, '_data_paths', return_value=self.request['data_paths']), patch.object(setup, '_probe', return_value=result):
            response = setup.read_mt5_metadata(None, self.payload, register_worker=Mock())
            self.assertEqual(response['status'], 'observed')
            self.assertFalse(response['backtest_ready'])
            self.assertFalse(response['native_import_performed'])
            self.assertIn('timezone', response['unresolved'])
            self.assertNotIn(self.tmp.name, json.dumps(response))
        with patch.object(setup, '_runtime', return_value=self.path), patch.object(setup, '_running_terminals', side_effect=[[self.process], []]), patch.object(setup, '_data_paths', return_value=self.request['data_paths']), patch.object(setup, '_probe', return_value=result):
            with self.assertRaisesRegex(setup.Mt5DataSetupError, 'mt5_terminal_changed'):
                setup.read_mt5_metadata(None, self.payload, register_worker=Mock())
        with setup._READ_LOCK, self.assertRaisesRegex(setup.Mt5DataSetupError, 'mt5_read_busy'):
            setup.read_mt5_metadata(None, self.payload, register_worker=Mock())

    def test_process_contract_registers_hidden_worker_and_caps_output(self):
        worker = Mock()
        worker.stdout = BytesIO(json.dumps(self.result()).encode())
        worker.stdin = BytesIO()
        worker.poll.return_value = 0
        worker.returncode = 0
        register = Mock()
        with patch.object(setup.subprocess, 'Popen', return_value=worker) as start:
            self.assertEqual(setup._probe(self.path, self.request, register)['schema'], 'tc.mt5-probe.v1')
        argv = start.call_args.args[0]
        self.assertEqual(argv[:3], [str(self.path), '-I', '-B'])
        self.assertEqual(Path(argv[3]).name, 'mt5_metadata_probe.py')
        self.assertFalse(start.call_args.kwargs['shell'])
        self.assertEqual(start.call_args.kwargs['stderr'], subprocess.DEVNULL)
        register.assert_called_once_with(worker, label='mt5-metadata', timeout_seconds=1)
        worker.stdout = BytesIO(b'x'*33)
        worker.stdin = BytesIO()
        with patch.object(setup, 'MAX_OUTPUT_BYTES', 32), patch.object(setup.subprocess, 'Popen', return_value=worker), self.assertRaisesRegex(setup.Mt5DataSetupError, 'mt5_probe_invalid'):
            setup._probe(self.path, self.request, register)

    def test_process_timeout_and_registration_failure_clean_up_without_raw_error(self):
        for registration_failure in (True, False):
            worker = Mock()
            worker.stdout, worker.stdin = BytesIO(b'{}'), BytesIO()
            worker.poll.side_effect = [None, 0]
            worker.wait.side_effect = None if registration_failure else [subprocess.TimeoutExpired('secret/path', 30), 0]
            register = Mock(side_effect=RuntimeError('PRIVATE') if registration_failure else None)
            with patch.object(setup.subprocess, 'Popen', return_value=worker), self.assertRaises(setup.Mt5DataSetupError) as caught:
                setup._probe(self.path, self.request, register)
            self.assertEqual(caught.exception.code, 'mt5_probe_failed' if registration_failure else 'mt5_probe_timeout')
            self.assertNotIn('PRIVATE', str(caught.exception))
            worker.terminate.assert_called_once()

    def test_connection_failure_retains_only_numeric_native_reason(self):
        self.api.initialize.return_value = False
        self.api.last_error.return_value = (-6, 'PRIVATE account and path')
        with self.assertRaises(ValueError) as caught:
            self.result()
        self.assertEqual(caught.exception.api_error_code, -6)
        self.assertNotIn('PRIVATE', str(caught.exception))
        worker = Mock()
        worker.stdout = BytesIO(b'{"error":"mt5_connection_failed","api_error_code":-6}')
        worker.stdin = BytesIO()
        worker.poll.return_value = 1
        worker.returncode = 1
        with patch.object(setup.subprocess, 'Popen', return_value=worker), self.assertRaises(setup.Mt5DataSetupError) as caught:
            setup._probe(self.path, self.request, Mock())
        self.assertEqual(caught.exception.native_error_code, -6)
        self.assertIn('authorization', caught.exception.detail.lower())
        self.assertIn('-6', caught.exception.detail)


if __name__ == '__main__':
    unittest.main()
