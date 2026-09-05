from contextlib import ExitStack
from copy import deepcopy
from datetime import datetime, timezone
from hashlib import sha256
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch

from tradercockpit import mt5_data_setup as setup
from tradercockpit import mt5_metadata_probe as probe
from tradercockpit.research_custody import EvidenceRef, FileResearchCustodyStore


class Mt5HistoryTests(unittest.TestCase):
    def setUp(self):
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name).resolve()
        self.store = FileResearchCustodyStore(self.root / 'custody')
        self.process = {'pid': 123, 'path': str(self.root / 'terminal64.exe'), 'started': '123456'}
        self.broker = {'company': 'Broker', 'server': 'Broker-Demo', 'currency': 'USD'}
        self.payload = {'terminal_id': 'mt5-123', 'identity_sha256': setup._digest(self.process),
                        'broker_sha256': setup._digest(self.broker), 'symbol': 'EURUSD', 'timeframe': 'H1',
                        'date_from': '2024-01-01', 'date_to': '2024-01-02'}
        self.request = {'process': self.process, 'data_paths': [str(self.root)], 'symbol': 'EURUSD',
                        'history': {k: self.payload[k] for k in ('broker_sha256', 'timeframe', 'date_from', 'date_to')}}
        self.api = Mock()
        self.api.initialize.return_value = True
        self.api.terminal_info.return_value = SimpleNamespace(path=str(self.root), data_path=str(self.root), company='Vendor', build=5640, connected=True)
        self.api.account_info.return_value = SimpleNamespace(**self.broker)
        self.api.symbol_info.return_value = SimpleNamespace(name='EURUSD', currency_profit='USD', trade_tick_size=0.00001)
        self.api.TIMEFRAME_H1 = 16385
        self.start = int(datetime(2024, 1, 1, tzinfo=timezone.utc).timestamp())
        self.rows = [dict(time=self.start + shift, open=1.0, high=2.0, low=0.5, close=1.5, tick_volume=5, spread=2, real_volume=0) for shift in (0, 3600, 10800)]
        self.api.copy_rates_range.return_value = self.rows

    def worker(self):
        return probe.read_metadata(self.request, self.api, lambda pid: self.process)

    def capture(self, result=None, payload=None, store=None):
        with ExitStack() as stack:
            stack.enter_context(patch.object(setup, '_runtime', return_value=self.root / 'python.exe'))
            stack.enter_context(patch.object(setup, '_running_terminals', return_value=[self.process]))
            stack.enter_context(patch.object(setup, '_data_paths', return_value=[str(self.root)]))
            stack.enter_context(patch.object(setup, '_probe', return_value=result if result is not None else self.worker()))
            return setup.read_mt5_history(None, payload or self.payload, store=store or self.store, register_worker=Mock())

    def test_native_utc_exclusive_capture_round_trips_immutable_csv(self):
        result = self.worker()
        self.api.copy_rates_range.assert_called_with('EURUSD', 16385, datetime(2024, 1, 1, tzinfo=timezone.utc), datetime(2024, 1, 1, 23, 59, 59, tzinfo=timezone.utc))
        self.api.symbols_get.assert_not_called()
        captured = self.capture(result)
        self.assertEqual(captured['request'], self.payload)
        self.assertEqual(captured['row_count'], 3)
        self.assertEqual(captured['gap_count'], 1)
        self.assertIsNone(captured['coverage_complete'])
        self.assertFalse(captured['native_import_performed'])
        self.assertFalse(captured['backtest_ready'])
        raw = setup.read_mt5_history_csv(self.store, {'history_ref': captured['history_ref']})
        self.assertTrue(raw.startswith(b'time,open,high,low,close,tick_volume,spread,real_volume\n2024-01-01T00:00:00+00:00,'))
        self.assertEqual(sha256(raw).hexdigest(), captured['source_sha256'])
        self.assertEqual(len(raw), captured['bytes'])
        self.assertNotIn(self.process['path'], json.dumps(captured))

    def test_request_limits_and_missing_custody_fail_before_probe(self):
        with patch.object(setup, '_probe') as run:
            for change in ({'date_to': '2024-01-01'}, {'date_to': '2999-01-01'}, {'date_from': '01/01/2024'},
                           {'timeframe': 'M2'}, {'broker_sha256': 'bad'}, {'date_from': '2023-01-01', 'timeframe': 'M1'},
                           {'path': '/private'}, {'timeframe': []}):
                with self.subTest(change=change), self.assertRaises(setup.Mt5DataSetupError):
                    setup.read_mt5_history(None, {**self.payload, **change}, store=self.store, register_worker=Mock())
            with self.assertRaisesRegex(setup.Mt5DataSetupError, 'mt5_history_custody_unavailable'):
                setup.read_mt5_history(None, self.payload, store=None, register_worker=Mock())
            run.assert_not_called()

    def test_worker_broker_mismatch_blocks_history_and_stale_terminal_blocks_storage(self):
        self.request['history']['broker_sha256'] = '0' * 64
        with self.assertRaisesRegex(ValueError, 'mt5_broker_changed'):
            self.worker()
        self.api.copy_rates_range.assert_not_called()
        self.request['history']['broker_sha256'] = self.payload['broker_sha256']
        result = self.worker()
        with patch.object(setup, '_runtime'), patch.object(setup, '_running_terminals', side_effect=[[self.process], []]), patch.object(setup, '_data_paths', return_value=[str(self.root)]), patch.object(setup, '_probe', return_value=result), patch.object(self.store, 'put_evidence') as put:
            with self.assertRaisesRegex(setup.Mt5DataSetupError, 'mt5_terminal_changed'):
                setup.read_mt5_history(None, self.payload, store=self.store, register_worker=Mock())
            put.assert_not_called()

    def test_worker_and_parent_reject_empty_invalid_ohlc_range_duplicates_and_volumes(self):
        changes = [[], [{**self.rows[0], 'high': 0.1}], [{**self.rows[0], 'open': float('nan')}],
                   [{**self.rows[0], 'spread': -1}], [{**self.rows[0], 'tick_volume': 1.5}],
                   [{**self.rows[0], 'time': self.start + 86400}], [self.rows[0], self.rows[0]]]
        for rows in changes:
            with self.subTest(rows=rows):
                self.api.copy_rates_range.return_value = rows
                with self.assertRaises(ValueError):
                    self.worker()
                with self.assertRaises(setup.Mt5DataSetupError):
                    setup._validated_bars(rows, self.payload)
        self.api.copy_rates_range.return_value = self.rows
        with patch.object(probe, 'MAX_HISTORY_BARS', 2), self.assertRaisesRegex(ValueError, 'mt5_history_request_invalid'):
            self.worker()
        with self.assertRaisesRegex(setup.Mt5DataSetupError, 'mt5_history_limit'):
            setup._validated_bars([self.rows[0]] * 10001, self.payload)

    def test_nominally_unclosed_broker_aligned_daily_bar_is_refused(self):
        payload = {**self.payload, 'timeframe': 'D1'}
        rows = [{**self.rows[0], 'time': self.start + 23 * 3600}]
        with self.assertRaisesRegex(setup.Mt5DataSetupError, 'mt5_history_open_bar'):
            setup._validated_bars(rows, payload, now=datetime(2024, 1, 2, 2, tzinfo=timezone.utc))
        self.request['history']['timeframe'] = 'D1'
        self.api.copy_rates_range.return_value = rows
        with patch.object(probe, 'datetime') as clock:
            clock.strptime.side_effect = datetime.strptime
            clock.side_effect = datetime
            clock.now.return_value = datetime(2024, 1, 2, 2, tzinfo=timezone.utc)
            with self.assertRaisesRegex(ValueError, 'mt5_history_open_bar'):
                self.worker()

    def test_custody_download_rejects_unrelated_evidence_and_tampered_manifest_or_csv(self):
        captured = self.capture()
        ref = EvidenceRef.parse(captured['history_ref'])
        manifest = json.loads(self.store.read_evidence(ref))
        for changes in ({'row_count': 2}, {'source_sha256': '0'*64}, {'coverage_complete': True}, {'broker': {**self.broker, 'server': 'Other'}}):
            changed = self.store.put_evidence(json.dumps({**manifest, **changes}).encode())
            with self.subTest(changes=changes), self.assertRaisesRegex(setup.Mt5DataSetupError, 'mt5_history_custody_invalid'):
                setup.read_mt5_history_csv(self.store, {'history_ref': str(changed)})
        unrelated = self.store.put_evidence(b'private unrelated evidence')
        with self.assertRaises(setup.Mt5DataSetupError):
            setup.read_mt5_history_csv(self.store, {'history_ref': str(unrelated)})
        path = self.store._evidence_path(EvidenceRef.parse(captured['csv_ref']))
        path.write_bytes(path.read_bytes() + b'tampered')
        with self.assertRaisesRegex(setup.Mt5DataSetupError, 'mt5_history_custody_invalid'):
            setup.read_mt5_history_csv(self.store, {'history_ref': captured['history_ref']})

    def test_parent_rejects_broker_and_bar_tampering_before_store_write(self):
        result = self.worker()
        for change in ('broker', 'bar'):
            mutated = deepcopy(result)
            if change == 'broker':
                mutated['broker']['server'] = 'Other'
            else:
                mutated['history'][0]['close'] = 99
            with patch.object(self.store, 'put_evidence') as put, self.assertRaises(setup.Mt5DataSetupError):
                self.capture(mutated)
            put.assert_not_called()
        store = Mock(put_evidence=Mock(side_effect=OSError('PRIVATE path')))
        with self.assertRaises(setup.Mt5DataSetupError) as caught:
            self.capture(result, store=store)
        self.assertEqual(caught.exception.code, 'mt5_history_custody_failed')
        self.assertNotIn('PRIVATE', str(caught.exception))


if __name__ == '__main__':
    unittest.main()
