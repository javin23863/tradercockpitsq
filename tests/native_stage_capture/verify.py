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
import re

from receipts import read_visit

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


def compare_archives(source, captured):
    with ZipFile(source) as a, ZipFile(captured) as b:
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


def verify_bound(root):
    """Reconcile v2 observations with retained native graphs, archives and task logs."""
    manifest = json.loads((root / 'manifest.json').read_text())
    observed = json.loads((root / 'native-seam-receipt.json').read_text())
    runtime = json.loads((root / 'runtime.json').read_text())
    assert runtime['status'] == 'stopped' and runtime['exit_code'] == 0
    assert runtime['protected_unchanged'] and not runtime['forced_cleanup']
    assert observed['status'] == 'observed' and observed['graphs'] == manifest['projects']
    assert len(observed['outcomes']) == len(manifest['projects'])
    sources = {p.stem: p for p in (root / 'input').glob('*.sqx')}
    expected_sources = manifest.get('inputs', [{'archive': 'Strategy 3.3.115.sqx', 'sha256': manifest.get('source_sha256')}])
    assert set(sources) == {Path(row['archive']).stem for row in expected_sources}
    for row in expected_sources:
        assert sha256(sources[Path(row['archive']).stem].read_bytes()).hexdigest() == row['sha256']
    visits = []; archive_count = 0
    for graph, outcome in zip(manifest['projects'], observed['outcomes']):
        project = graph['project']; wrong = project.endswith('-wrong'); filtered = project.endswith('-filter')
        assert outcome['project'] == project and outcome['start'].get('success') and not outcome['start'].get('error')
        assert outcome['remaining']['count'] == (1 if wrong else 0)
        saved_graph = root / (project + '-native-saved.cfx')
        if not saved_graph.exists(): saved_graph = root / (project + '.cfx')
        assert sha256(saved_graph.read_bytes()).hexdigest() == graph['sha256']
        progress = json.loads((root / (project + '-progress.json')).read_text())
        assert progress[-1]['stats']['running_status'] == ('error' if wrong else 'stopped')
        logs = list((root / (project + '-after-native') / 'log').glob('global_log*.log'))
        assert len(logs) == 1
        log = logs[0].read_text()
        task_numbers = [int(n) for n in re.findall(r'^Task: (\d+)\.', log, re.M)]
        expected_tasks = [1, 2] if wrong else [1, 2, 3, 4, 5, 4, 5, 4, 5, 6, 7] if filtered else [1, 2, 3, 2, 3, 2, 3, 4, 5]
        assert task_numbers == expected_tasks, (task_numbers, expected_tasks)
        if filtered:
            assert 'Deleted: 1' in log
        for binding in graph['bindings']:
            folder = root / 'capture-spool' / binding['checkpoint']
            rows = sorted((read_visit(root / 'capture-spool', p.name, binding) for p in folder.iterdir()), key=lambda row: row['started'])
            checkpoint = binding['checkpoint']
            expected_counts = [] if wrong else [0] if checkpoint == 'empty' else [2] if checkpoint == 'before' else [1, 1, 1]
            assert [row['native_count'] for row in rows] == expected_counts
            assert all(row['state'] == 'completed' for row in rows)
            for row in rows:
                if filtered:
                    expected_names = {'Strategy 3.3.115', 'Strategy 4.2.186'} if checkpoint == 'before' else {'Strategy 4.2.186'} if checkpoint == 'loop' else set()
                    assert {a['name'] for a in row['artifacts']} == expected_names
                for artifact in row['artifacts']:
                    compare_archives(sources[artifact['name']], folder / row['visit'] / f"{artifact['index']}.sqx")
                    archive_count += 1
            visits.extend(rows)
    assert len({row['visit'] for row in visits}) == len(visits)
    # A second disk read must return the same observations after SQX has exited.
    for row in visits:
        binding = {k: row[k] for k in ('project', 'run', 'checkpoint', 'task_entry', 'task', 'databank', 'graph_sha256')}
        assert read_visit(root / 'capture-spool', row['visit'], binding) == row
    return {'checkpoint_visits': len(visits), 'preserved_archives': archive_count,
            'native_loop_visits': 3, 'filter_removal_captured': 'inputs' in manifest,
            'reopened_after_native_exit': True, 'product_execution_approved': False}


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
            compare_archives(source, archive)
    assert sorted(counts) == [0, 1, 1]
    return {'visits': 3, 'preserved_archives': 2, 'empty_capture': True, 'blocked_capture_preserved_bank': True}


if __name__ == '__main__':
    bound = sys.argv[1] == '--bound'
    print(json.dumps((verify_bound if bound else verify)(Path(sys.argv[2 if bound else 1])), indent=2))
