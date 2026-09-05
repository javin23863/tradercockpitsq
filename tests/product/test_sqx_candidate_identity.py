from base64 import b64encode
from hashlib import sha256
from io import BytesIO
import json
import struct
from unittest import TestCase
from xml.etree import ElementTree as ET
from zipfile import ZipFile, ZIP_DEFLATED

from tradercockpit.sqx_candidate_identity import (SqxCandidateIdentityError,
    stamp_candidate_token, stamp_import_candidate_token, read_candidate_token, verify_native_reserialization, verify_native_import)


TOKEN = "a" * 64


def stats(records=(('Trades', 1, 3), ('Profit', 3, 12.5))):
    raw = bytearray()
    for name, kind, value in records:
        encoded = name.encode()
        raw.extend(bytes([100 + kind]) + struct.pack('>H', len(encoded)) + encoded)
        raw.extend(struct.pack({1: '>i', 2: '>q', 3: '>f'}[kind], value))
    return b64encode(raw).decode()


def archive(settings=None, **replace):
    members = {'version.txt': b'1', 'strategy_Portfolio.xml': b'<StrategyFile AppVersion="SQX Build 144.2953"><Strategy><Rule value="1"/><Rule value="2"/></Strategy></StrategyFile>',
        'orders.bin': b'exact-native-orders', 'Results/Main: EURUSD_H1/dailyEquity.bin': b'exact-equity',
        'lastSettings.xml': b'<Settings><Data><Instrument pointValue="1"/></Data><Rules><Rule value="1"/><Rule value="2"/></Rules></Settings>',
        'settings.xml': (settings or ('<ResultsGroup ResultName="Original"><ResultsMap><Results><Result resultKey="Main" special="false"><ValuesMap><Metric type="com.strategyquant.tradinglib.SQStats"><SQStats version="2" e="b64">' + stats() + '</SQStats></Metric></ValuesMap><SettingsMap><Spread type="Double">2</Spread><Currency type="String">USD</Currency></SettingsMap></Result></Results></ResultsMap><SymbolsMap><SymbolInfo symbolName="EURUSD" instrumentName="EURUSD"/></SymbolsMap><SpecialValuesMap><SettingsMap><Note type="String"/><FiltersResultFailedReason type="String">passed</FiltersResultFailedReason></SettingsMap></SpecialValuesMap></ResultsGroup>')).encode()}
    members.update(replace)
    output = BytesIO()
    with ZipFile(output, 'w', compression=ZIP_DEFLATED) as zipped:
        for key, value in members.items():
            zipped.writestr(key, value)
    return output.getvalue()


def edit(raw, member, transform):
    output = BytesIO()
    with ZipFile(BytesIO(raw)) as source, ZipFile(output, 'w') as target:
        for info in source.infolist():
            data = source.read(info)
            target.writestr(info, transform(data) if info.filename == member else data)
    return output.getvalue()


def xml_edit(raw, change, member='settings.xml'):
    def apply(data):
        root = ET.fromstring(data)
        change(root)
        return ET.tostring(root)
    return edit(raw, member, apply)


class CandidateIdentityTests(TestCase):
    def setUp(self):
        self.original = archive()
        self.stamped = stamp_candidate_token(self.original, TOKEN)

    def test_explicit_derivative_preserves_original_and_other_member_bytes(self):
        digest = sha256(self.original).hexdigest()
        self.assertIsNone(read_candidate_token(self.original))
        self.assertEqual(read_candidate_token(self.stamped), TOKEN)
        self.assertEqual(sha256(self.original).hexdigest(), digest)
        with ZipFile(BytesIO(self.original)) as a, ZipFile(BytesIO(self.stamped)) as b:
            self.assertEqual(a.namelist(), b.namelist())
            self.assertTrue(all(a.read(n) == b.read(n) for n in a.namelist() if n != 'settings.xml'))
        for token in (TOKEN, 'A' * 64, 'b' * 63, None):
            with self.subTest(token=token), self.assertRaises(SqxCandidateIdentityError):
                stamp_candidate_token(self.stamped, token)

    def test_native_map_and_stats_order_changes_reconcile_with_known_token(self):
        def reorder(root):
            bank = root.find('SpecialValuesMap/SettingsMap')
            bank.remove(bank.find('Note'))
            for node in root.iter('SettingsMap'):
                node[:] = list(reversed(list(node)))
            root.find('.//SQStats').text = stats((('Profit', 3, 12.5), ('Trades', 1, 3)))
        changed = xml_edit(self.stamped, reorder)
        proof = verify_native_reserialization(self.stamped, changed, TOKEN)
        self.assertEqual(proof['previous_sha256'], sha256(self.stamped).hexdigest())
        self.assertEqual(proof['current_sha256'], sha256(changed).hexdigest())
        self.assertEqual(proof['changed_members'], ['settings.xml'])

    def test_import_only_accepts_the_explicit_native_filename_label(self):
        current = xml_edit(self.stamped, lambda r: r.set('ResultName', 'Uploaded strategy'))
        proof = verify_native_import(self.stamped, current, TOKEN, 'Uploaded strategy.sqx')
        self.assertEqual(proof['schema'], 'tc.sqx-native-import.v1')
        self.assertEqual(proof['allowed_name_change'], {'path': 'ResultsGroup/@ResultName', 'previous': 'Original', 'current': 'Uploaded strategy'})
        self.assertEqual(proof['archive_name'], 'Uploaded strategy.sqx')
        self.assertIsNone(verify_native_import(self.stamped, self.stamped, TOKEN, 'Uploaded strategy.sqx')['allowed_name_change'])
        missing = xml_edit(self.stamped, lambda r: r.attrib.pop('ResultName'))
        self.assertIsNone(verify_native_import(missing, current, TOKEN, 'Uploaded strategy.sqx')['allowed_name_change']['previous'])
        self.assertIsNone(verify_native_import(missing, missing, TOKEN, 'Uploaded strategy.sqx')['allowed_name_change'])
        for name in ('Other.sqx', '../Uploaded strategy.sqx', 'Uploaded strategy.zip', '.sqx', None):
            with self.subTest(name=name), self.assertRaises(SqxCandidateIdentityError):
                verify_native_import(self.stamped, current, TOKEN, name)
        with self.assertRaises(SqxCandidateIdentityError):
            verify_native_reserialization(self.stamped, current, TOKEN)
        for changed in (xml_edit(current, lambda r: r.set('unexpected', 'changed')),
                        xml_edit(current, lambda r: r.find('SymbolsMap/SymbolInfo').set('instrumentName', 'Other')),
                        edit(current, 'orders.bin', lambda b: b + b'changed')):
            with self.subTest(), self.assertRaises(SqxCandidateIdentityError):
                verify_native_import(self.stamped, changed, TOKEN, 'Uploaded strategy.sqx')

    def test_new_import_replaces_only_valid_carrier_and_is_deterministic(self):
        original = self.stamped
        digest = sha256(original).hexdigest()
        imported = stamp_import_candidate_token(original, 'b' * 64)
        self.assertEqual(imported, stamp_import_candidate_token(original, 'b' * 64))
        self.assertEqual(read_candidate_token(imported), 'b' * 64)
        self.assertEqual(read_candidate_token(original), TOKEN)
        self.assertEqual(sha256(original).hexdigest(), digest)
        self.assertEqual(stamp_import_candidate_token(self.original, TOKEN), self.stamped)
        with ZipFile(BytesIO(original)) as before, ZipFile(BytesIO(imported)) as after:
            self.assertEqual(before.namelist(), after.namelist())
            for name in before.namelist():
                if name == 'settings.xml':
                    root = ET.fromstring(before.read(name))
                    root.find('SpecialValuesMap/SettingsMap/TraderCockpitCandidateTokenV1').text = 'b' * 64
                    self.assertEqual(ET.tostring(root), ET.tostring(ET.fromstring(after.read(name))))
                else:
                    self.assertEqual(before.read(name), after.read(name))
        duplicate = xml_edit(original, lambda r: ET.SubElement(r.find('SpecialValuesMap/SettingsMap'), 'TraderCockpitCandidateTokenV1', {'type': 'String'}))
        malformed = xml_edit(original, lambda r: r.find('SpecialValuesMap/SettingsMap/TraderCockpitCandidateTokenV1').set('type', 'Integer'))
        for raw in (duplicate, malformed):
            with self.subTest(), self.assertRaises(SqxCandidateIdentityError):
                stamp_import_candidate_token(raw, 'b' * 64)

    def test_supported_display_cache_and_empty_oos_cache(self):
        def caches(root):
            bank = root.find('SpecialValuesMap/SettingsMap')
            for key, values in [('MEC_FULL_Main', [0] * 50), ('MEC_OOS_Portfolio', [])]:
                ET.SubElement(bank, key, {'type': 'String'}).text = "{{sparklinesWidget data='" + json.dumps({'values': values, 'zeroPoint': 0}) + "'}}"
        verify_native_reserialization(self.stamped, xml_edit(self.stamped, caches), TOKEN)

    def test_missing_foreign_duplicate_and_misplaced_markers_refuse(self):
        for raw in (self.original, stamp_candidate_token(self.original, 'b' * 64),
                    xml_edit(self.stamped, lambda r: ET.SubElement(r.find('SpecialValuesMap/SettingsMap'), 'TraderCockpitCandidateTokenV1', {'type': 'String'})),
                    xml_edit(self.stamped, lambda r: ET.SubElement(r, 'TraderCockpitCandidateTokenV2', {'type': 'String'}))):
            with self.subTest(), self.assertRaises(SqxCandidateIdentityError):
                verify_native_reserialization(self.stamped, raw, TOKEN)

    def test_changed_metrics_types_duplicates_configuration_and_artifacts_refuse(self):
        changes = [xml_edit(self.stamped, lambda r: setattr(r.find('.//SQStats'), 'text', stats((('Trades', 1, 4), ('Profit', 3, 12.5))))),
            xml_edit(self.stamped, lambda r: setattr(r.find('.//SQStats'), 'text', stats((('Trades', 2, 3), ('Profit', 3, 12.5))))),
            xml_edit(self.stamped, lambda r: setattr(r.find('.//SQStats'), 'text', stats((('Trades', 1, 3), ('Trades', 1, 3), ('Profit', 3, 12.5))))),
            xml_edit(self.stamped, lambda r: r.find('Data/Instrument').set('pointValue', '2'), 'lastSettings.xml'),
            xml_edit(self.stamped, lambda r: r.find('Rules').__setitem__(slice(None), list(reversed(list(r.find('Rules'))))), 'lastSettings.xml'),
            edit(self.stamped, 'orders.bin', lambda b: b + b'changed'), edit(self.stamped, 'version.txt', lambda b: b'2'),
            edit(self.stamped, 'strategy_Portfolio.xml', lambda b: b.replace(b'value="1"', b'value="3"'))]
        for current in changes:
            with self.subTest(), self.assertRaises(SqxCandidateIdentityError):
                verify_native_reserialization(self.stamped, current, TOKEN)

    def test_duplicate_map_keys_unsupported_stats_and_unsafe_caches_refuse(self):
        def duplicate(root):
            ET.SubElement(root.find('.//Result/SettingsMap'), 'Spread', {'type': 'Integer'}).text = '2'
        variants = [xml_edit(self.stamped, duplicate),
            xml_edit(self.stamped, lambda r: r.find('.//SQStats').set('version', '3')),
            xml_edit(self.stamped, lambda r: setattr(r.find('.//SQStats'), 'text', b64encode(b'\x7f').decode())),
            xml_edit(self.stamped, lambda r: r.remove(r.find('SymbolsMap')))]
        for key, value in [('MEC_NEW_Main', 'unknown'), ('MEC_FULL_Main', '{{script}}'),
                ('MEC_FULL_Main', "{{sparklinesWidget data='" + json.dumps({'values': [0] * 50, 'zeroPoint': 0, 'html': '<script>'}) + "'}}")]:
            def add(root, key=key, value=value):
                ET.SubElement(root.find('SpecialValuesMap/SettingsMap'), key, {'type': 'String'}).text = value
            variants.append(xml_edit(self.stamped, add))
        for raw in variants + [b'bad ZIP', edit(self.stamped, 'settings.xml', lambda b: b'<ResultsGroup>')]:
            with self.subTest(), self.assertRaises(SqxCandidateIdentityError):
                verify_native_reserialization(self.stamped, raw, TOKEN)

    def test_duplicate_stats_records_are_preserved_not_silently_deduplicated(self):
        duplicate = xml_edit(self.stamped, lambda r: setattr(r.find('.//SQStats'), 'text', stats((('Trades', 1, 3), ('Trades', 1, 3), ('Profit', 3, 12.5)))))
        reordered = xml_edit(duplicate, lambda r: setattr(r.find('.//SQStats'), 'text', stats((('Profit', 3, 12.5), ('Trades', 1, 3), ('Trades', 1, 3)))))
        verify_native_reserialization(duplicate, reordered, TOKEN)

    def test_unknown_cache_preserved_exactly_but_cannot_change(self):
        def add(root):
            ET.SubElement(root.find('SpecialValuesMap/SettingsMap'), 'MEC_UNKNOWN', {'type': 'String'}).text = 'opaque future value'
        original = xml_edit(self.stamped, add)
        verify_native_reserialization(original, original, TOKEN)
        changed = xml_edit(original, lambda r: setattr(r.find('SpecialValuesMap/SettingsMap/MEC_UNKNOWN'), 'text', 'different'))
        with self.assertRaises(SqxCandidateIdentityError):
            verify_native_reserialization(original, changed, TOKEN)

    def test_stat_trailing_bytes_conflicting_duplicates_and_missing_member_refuse(self):
        for encoded in (b64encode(b'\x65\x00').decode(), stats((('Trades', 1, 3), ('Trades', 1, 4))), '!!!!'):
            changed = xml_edit(self.stamped, lambda r: setattr(r.find('.//SQStats'), 'text', encoded))
            with self.subTest(encoded=encoded), self.assertRaises(SqxCandidateIdentityError):
                verify_native_reserialization(self.stamped, changed, TOKEN)
        output = BytesIO()
        with ZipFile(BytesIO(self.stamped)) as source, ZipFile(output, 'w') as target:
            for name in source.namelist():
                if name != 'lastSettings.xml':
                    target.writestr(name, source.read(name))
        with self.assertRaises(SqxCandidateIdentityError):
            verify_native_reserialization(self.stamped, output.getvalue(), TOKEN)

    def test_note_addition_or_nonempty_removal_is_not_a_display_exception(self):
        without = xml_edit(self.stamped, lambda r: r.find('SpecialValuesMap/SettingsMap').remove(r.find('SpecialValuesMap/SettingsMap/Note')))
        nonempty = xml_edit(self.stamped, lambda r: setattr(r.find('SpecialValuesMap/SettingsMap/Note'), 'text', 'user note'))
        for before, after in ((without, self.stamped), (nonempty, without)):
            with self.subTest(), self.assertRaises(SqxCandidateIdentityError):
                verify_native_reserialization(before, after, TOKEN)

    def test_native_nonfinite_stats_preserve_exact_bits_without_becoming_valid_metrics(self):
        def payload(bits):
            return b64encode(b'\x67\x00\x08Sentinel' + bytes.fromhex(bits)).decode()
        for bits in ('7f800000', 'ff800000', '7fc00000', '7fa00001'):
            raw = xml_edit(self.stamped, lambda r: setattr(r.find('.//SQStats'), 'text', payload(bits)))
            verify_native_reserialization(raw, raw, TOKEN)
            changed = xml_edit(raw, lambda r: setattr(r.find('.//SQStats'), 'text', payload('7fc00002')))
            with self.subTest(bits=bits), self.assertRaises(SqxCandidateIdentityError):
                verify_native_reserialization(raw, changed, TOKEN)

    def test_legacy_absent_map_has_no_token_but_reserved_token_outside_map_refuses(self):
        legacy = xml_edit(self.original, lambda r: r.remove(r.find('SpecialValuesMap')))
        self.assertIsNone(read_candidate_token(legacy))
        self.assertEqual(read_candidate_token(stamp_candidate_token(legacy, TOKEN)), TOKEN)
        bad = xml_edit(legacy, lambda r: ET.SubElement(r, 'TraderCockpitCandidateTokenV1', {'type': 'String'}))
        with self.assertRaises(SqxCandidateIdentityError):
            read_candidate_token(bad)

    def test_native_mixed_type_duplicates_preserve_each_record_and_type(self):
        raw = xml_edit(self.stamped, lambda r: setattr(r.find('.//SQStats'), 'text', stats((('AvgDrawdown', 1, 2), ('AvgDrawdown', 3, 2.0)))))
        reordered = xml_edit(raw, lambda r: setattr(r.find('.//SQStats'), 'text', stats((('AvgDrawdown', 3, 2.0), ('AvgDrawdown', 1, 2)))))
        verify_native_reserialization(raw, reordered, TOKEN)
        for changed_stats in (stats((('AvgDrawdown', 3, 2.0),)), stats((('AvgDrawdown', 2, 2), ('AvgDrawdown', 3, 2.0)))):
            changed = xml_edit(raw, lambda r: setattr(r.find('.//SQStats'), 'text', changed_stats))
            with self.subTest(), self.assertRaises(SqxCandidateIdentityError):
                verify_native_reserialization(raw, changed, TOKEN)
