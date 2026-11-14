#!/usr/bin/env python3
"""Reconcile bibliographic source references; never import or relicense text bodies."""
import argparse
import csv
import difflib
import json
import re
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
REGISTRY = ASSETS / 'catalogue/source-references.json'
SPECS = {
    'vedamantra': ('stotrasamhita/vedamantra-book', ['mantras/', 'aranyakas/', 'ashtakas/', 'kandas/']),
    'puja_vidhanam': ('stotrasamhita/puja-vidhanam', ['pujas/', 'purvanga/', 'appendices/', 'kathas/']),
    'stotra_sangrahah': ('stotrasamhita/stotra-sangrahah', ['stotras/']),
    'chanda': ('hrishikeshrt/chanda', []),
}


def canonical(value):
    value = unicodedata.normalize('NFKD', value).casefold()
    value = ''.join(c for c in value if not unicodedata.combining(c))
    value = re.sub(r'[^a-z0-9]', '', value)
    for old, new in [('nrisimha', 'narasimha'), ('hrdaya', 'hridaya'), ('dwadasha', 'dvadasha')]:
        value = value.replace(old, new)
    return re.sub(r'(stotra|sukta|ashtaka|kavacha|hridaya)m$', r'\1', value)


def title_hint(path):
    name = Path(path).stem.replace('_', ' ').replace('-', ' ')
    return re.sub(r'(?<=[a-z])(?=[A-Z])', ' ', name)


def classify(source_id, path):
    if source_id == 'vedamantra':
        return 'mantra-or-sukta' if path.startswith('mantras/') else 'long-form-vedic-section'
    if source_id == 'puja_vidhanam':
        return {'pujas': 'puja-procedure', 'purvanga': 'puja-preliminary', 'appendices': 'reference-table', 'kathas': 'vrata-katha'}[path.split('/')[0]]
    return 'stotra'


def local_catalogues():
    rows, documents = [], {}
    for path in sorted((ASSETS / 'library').glob('*/index.json')):
        doc = json.loads(path.read_text(encoding='utf-8'))
        documents[path] = doc
        collection = path.parent.name
        for item in doc['items']:
            body = ASSETS / 'library' / collection / (item['id'] + '.txt')
            rows.append({'key': f'library:{collection}:{item["id"]}', 'id': item['id'], 'collection': collection,
                         'file': body.relative_to(ASSETS).as_posix(), 'readable': body.is_file(),
                         'aliases': [item['id']], 'item': item})
    for path in [ASSETS / 'madhwa/catalog.json', ASSETS / 'catalogue/index.json']:
        doc = json.loads(path.read_text(encoding='utf-8')); documents[path] = doc
        groups = doc.get('groups', [{'id': 'madhwa', 'items': doc.get('items', [])}])
        for group in groups:
            for item in group['items']:
                file = item.get('file', '')
                if file.startswith('lib:'):
                    body = ASSETS / 'library' / (file[4:] + '.txt')
                else:
                    body = ASSETS / file if file.startswith('madhwa/') else ASSETS / 'madhwa/texts' / file
                aliases = [item['id'], item.get('en', '')]
                if file: aliases.append(Path(file.split(':')[-1]).stem)
                rows.append({'key': f'{group["id"]}:{item["id"]}', 'id': item['id'], 'collection': group['id'],
                             'file': body.relative_to(ASSETS).as_posix() if file else '',
                             'readable': bool(file) and body.is_file(), 'aliases': aliases, 'item': item})
    return rows, documents


def snapshot_sources(snapshot_dir):
    sources, entries = [], []
    for source_id, (repo, roots) in SPECS.items():
        tree = json.loads((snapshot_dir / (repo.replace('/', '__') + '-tree.json')).read_text(encoding='utf-8'))
        revision = tree['sha']; base = f'https://github.com/{repo}/blob/{revision}/'
        source = {'id': source_id, 'repository': f'https://github.com/{repo}', 'revision': revision,
                  'kind': 'analysis-tool' if source_id == 'chanda' else 'text-reference',
                  'licence': 'AGPL-3.0-or-later' if source_id == 'chanda' else 'personal-use-notice; no general redistribution grant identified',
                  'rights_status': 'tool-not-integrated' if source_id == 'chanda' else 'redistribution-rights-review-required',
                  'evidence_url': base + ('LICENSE' if source_id == 'chanda' else 'frontmatter.tex'),
                  'readme_url': base + 'README.md', 'body_imported': False}
        sources.append(source)
        for node in tree['tree']:
            path = node['path']
            if node['type'] != 'blob' or not path.endswith('.tex') or not any(path.startswith(p) for p in roots): continue
            entries.append({'id': source_id + ':' + path, 'source_id': source_id, 'path': path,
                            'blob_sha': node['sha'], 'title_hint': title_hint(path),
                            'category_hint': classify(source_id, path), 'url': base + quote(path, safe='/')})
    return sources, entries


def reconcile(entries, local):
    exact, normalized = defaultdict(list), defaultdict(list)
    for row in local:
        for alias in set(row['aliases']):
            if not alias: continue
            exact[alias.casefold()].append(row)
            key = canonical(alias)
            if key: normalized[key].append(row)
    for entry in entries:
        stem = Path(entry['path']).stem
        matches = exact.get(stem.casefold(), [])
        method = 'exact-id-or-file-name'
        if not matches:
            matches = normalized.get(canonical(stem), [])
            method = 'normalized-title-or-id'
        matches = {m['key']: m for m in matches}
        entry['matches'] = [{'local_key': m['key'], 'file': m['file'], 'readable': m['readable']} for m in matches.values()]
        entry['comparison'] = method if matches else 'no-title-match'
        entry['state'] = 'existing-title-reference' if matches else 'new-reference-candidate'
        if re.search(r'(?:-learn|-old|Copy)', stem, re.I): entry['state'] = 'variant-review'
        entry['body_imported'] = False
        entry['rights_status'] = 'redistribution-rights-review-required'
        entry['possible_matches'] = []
        if not matches:
            key = canonical(stem)
            scored = sorted(((difflib.SequenceMatcher(None, key, k).ratio(), k) for k in normalized), reverse=True)
            seen = set()
            for score, k in scored[:3]:
                if score < .87: continue
                for row in normalized[k]:
                    if row['key'] not in seen:
                        entry['possible_matches'].append({'local_key': row['key'], 'similarity': round(score, 3)})
                        seen.add(row['key'])
            if entry['possible_matches']: entry['state'] = 'possible-variant-review'
    return entries


def write_outputs(registry, local, documents):
    REGISTRY.write_text(json.dumps(registry, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    refs = defaultdict(list)
    for entry in registry['references']:
        for match in entry['matches']:
            refs[match['local_key']].append({'source_id': entry['source_id'], 'path': entry['path'],
                                           'comparison': entry['comparison'], 'rights_status': entry['rights_status']})
    for row in local:
        if refs[row['key']]: row['item']['sourceReferences'] = refs[row['key']]
    for path, doc in documents.items():
        if path.parent.name in ['mantras', 'puja', 'stotra'] and path.parent.parent.name == 'library':
            doc['sourceReview'] = {'registry': 'catalogue/source-references.json',
                                   'basis': 'title/id comparison only; not a source-text equivalence or licence clearance',
                                   'redistributionPermission': 'not established by this comparison'}
        path.write_text(json.dumps(doc, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    report_dir = ROOT / 'docs/catalogue'
    report_dir.mkdir(exist_ok=True)
    with (report_dir / 'reference-source-comparison.csv').open('w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f); writer.writerow(['source_id','path','title_hint','category','state','comparison','local_keys','possible_matches','rights_status','url'])
        for e in registry['references']:
            writer.writerow([e['source_id'],e['path'],e['title_hint'],e['category_hint'],e['state'],e['comparison'],
                             ';'.join(m['local_key'] for m in e['matches']),';'.join(m['local_key'] for m in e['possible_matches']),e['rights_status'],e['url']])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--snapshot-dir', type=Path)
    parser.add_argument('--as-of')
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    previous = json.loads(REGISTRY.read_text(encoding='utf-8')) if REGISTRY.exists() else {}
    if args.snapshot_dir:
        sources, entries = snapshot_sources(args.snapshot_dir)
    else:
        sources, entries = previous.get('sources', []), previous.get('references', [])
        if not entries: parser.error('Provide --snapshot-dir for the initial source-tree snapshot')
    local, documents = local_catalogues()
    entries = reconcile(entries, local)
    summary = {source['id']: dict(Counter(e['state'] for e in entries if e['source_id']==source['id'])) for source in sources}
    registry = {'schema_version': 1, 'checked_on': args.as_of or previous.get('checked_on'),
                'scope': 'bibliographic reference metadata only; no source bodies imported or re-licensed',
                'comparison_limits': 'Title/id matches do not establish identical text, recension, provenance or redistribution rights. Possible matches require human review.',
                'sources': sources, 'summary': summary, 'references': entries}
    if args.write: write_outputs(registry, local, documents)
    print(json.dumps({'references': len(entries), 'sources': len(sources), 'summary': summary,
                      'local_records_with_reference': len({m['local_key'] for e in entries for m in e['matches']}),
                      'bodies_imported': 0}, indent=2))

if __name__ == '__main__': main()
