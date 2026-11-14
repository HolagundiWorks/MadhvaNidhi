import importlib.util
import json
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location('reconcile', Path(__file__).with_name('reconcile-catalogue-sources.py'))
r = importlib.util.module_from_spec(spec)
spec.loader.exec_module(r)

class ReferenceReconciliationTest(unittest.TestCase):
    def local(self, alias):
        return [{'key': 'local:1', 'aliases': [alias], 'file': 'existing.txt', 'readable': True}]

    def entry(self, path):
        return [{'source_id': 'stotra_sangrahah', 'path': path}]

    def test_transliteration_alias_is_reference_only(self):
        result = r.reconcile(self.entry('stotras/Nrisimha-Stotram.tex'), self.local('narasimha_stotra'))[0]
        self.assertEqual(result['comparison'], 'normalized-title-or-id')
        self.assertFalse(result['body_imported'])
        self.assertEqual(result['rights_status'], 'redistribution-rights-review-required')

    def test_near_title_is_not_automatically_merged(self):
        result = r.reconcile(self.entry('stotras/ShivaStotram2.tex'), self.local('ShivaStotram'))[0]
        self.assertFalse(result['matches'])
        self.assertTrue(result['possible_matches'])
        self.assertEqual(result['state'], 'possible-variant-review')

    def test_study_variant_requires_review(self):
        result = r.reconcile(self.entry('mantras/ChamakaPrashnah-learn.tex'), self.local('ChamakaPrashnah'))[0]
        self.assertFalse(result['matches'])
        self.assertIn(result['state'], ['variant-review', 'possible-variant-review'])

    def test_registry_integrity_and_existing_catalogue_links(self):
        registry = json.loads(r.REGISTRY.read_text(encoding='utf-8'))
        local, _ = r.local_catalogues()
        keys = {row['key'] for row in local}
        entries = registry['references']
        self.assertEqual(len(entries), 528)
        self.assertEqual(len({e['id'] for e in entries}), len(entries))
        for e in entries:
            self.assertFalse(e['body_imported'])
            for match in e['matches']:
                self.assertIn(match['local_key'], keys)
                if match['readable']:
                    self.assertTrue((r.ASSETS / match['file']).is_file())
        self.assertEqual(next(s for s in registry['sources'] if s['id']=='chanda')['kind'], 'analysis-tool')
        self.assertFalse(any(e['source_id']=='chanda' for e in entries))

if __name__ == '__main__': unittest.main()
