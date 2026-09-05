"""Verify retained native capture-probe receipts; does not start or mutate SQX.

PYTHONPATH=product python tests/native_stage_capture/verify.py <receipt-directory>
The installed native Code Editor compiles the adjacent Java probe. It is not a
production plugin or an assertion that full workflow stage tracking is complete.
"""
import json
from hashlib import sha256
from pathlib import Path
import sys
from xml.etree import ElementTree as ET
from zipfile import ZipFile

from tradercockpit.sqx_databank_grid import decode_sqstats_v2


def normalized_settings(data):
    node = ET.fromstring(data)
    for item in reversed(list(node.iter())):
        if item.tag == 'SQStats':
            decoded = decode_sqstats_v2(item.text)
            assert decoded, 'Undecodable native statistics'
            item.text = json.dumps(decoded, sort_keys=True)
        if item.tag.endswith('Map'):
            item[:] = sorted(item, key=lambda child: ET.tostring(child, encoding='unicode'))
    return ET.canonicalize(ET.tostring(node, encoding='unicode'), strip_text=True)


def verify(root):
    manifest = json.loads((root / 'capture-manifest.json').read_text())
    observed = json.loads((root / 'native-seam-receipt.json').read_text())
    assert observed['status'] == 'observed'
    assert observed['graphs'] == manifest['projects']
    assert [row['remaining']['count'] for row in observed['outcomes']] == [0, 1]
    assert all(row['start'].get('success') and not row['start'].get('error') for row in observed['outcomes'])
    for graph, status in zip(manifest['projects'], ['stopped', 'error']):
        rows = json.loads((root / (graph['project'] + '-progress.json')).read_text())
        assert rows[-1]['stats']['running_status'] == status
    source = next((root / 'input').glob('*.sqx'))
    assert sha256(source.read_bytes()).hexdigest() == manifest['source_sha256']
    visits = sorted((root / 'capture-spool' / 'capture').iterdir())
    assert len(visits) == 3 and len({path.name for path in visits}) == 3
    counts = []
    for visit in visits:
        items = ET.parse(visit / 'completed.xml').getroot().findall('entry')
        data = {item.attrib['key']: item.text for item in items}
        assert len(data) == len(items), 'Duplicate manifest field'
        assert data['schema'] == 'tc.native-capture-probe.v1'
        assert data['visit'] == visit.name
        assert data['graph_sha256'] == manifest['projects'][0]['sha256']
        assert data['project'] == manifest['projects'][0]['project']
        assert (visit / 'started.xml').is_file() and not (visit / 'failed.xml').exists()
        count = int(data['count']); counts.append(count)
        assert len(list(visit.glob('*.sqx'))) == count
        for i in range(count):
            archive = visit / f'{i}.sqx'
            assert sha256(archive.read_bytes()).hexdigest() == data[f'artifact.{i}.sha256']
            assert archive.stat().st_size == int(data[f'artifact.{i}.bytes'])
            with ZipFile(source) as a, ZipFile(archive) as b:
                assert a.testzip() is None and b.testzip() is None
                assert set(a.namelist()) == set(b.namelist())
                assert len(b.namelist()) == len(set(b.namelist()))
                for name in a.namelist():
                    left, right = a.read(name), b.read(name)
                    if name == 'settings.xml':
                        assert normalized_settings(left) == normalized_settings(right)
                    elif name == 'lastSettings.xml':
                        assert ET.canonicalize(left.decode()) == ET.canonicalize(right.decode())
                    else:
                        assert left == right, f'Native member changed: {name}'
    assert sorted(counts) == [0, 1, 1]
    return {'visits': 3, 'preserved_archives': 2, 'empty_capture': True, 'blocked_capture_preserved_bank': True}


if __name__ == '__main__':
    print(json.dumps(verify(Path(sys.argv[1])), indent=2))
