"""Verify the offline Naivedya catalogue and source facts required by the app."""
import json
from pathlib import Path
from urllib.parse import urlparse
ROOT=Path(__file__).resolve().parents[1]
folder=ROOT/'app/src/main/assets/naivedya'
book=json.loads((folder/'index.json').read_text(encoding='utf-8'))
sources=json.loads((folder/'sources.json').read_text(encoding='utf-8'))['sources']
by_id={s['id']:s for s in sources}
items=book['items']
assert len(items)==5 and len({r['id'] for r in items})==5
allowed={'udupi-recipes.com','smithakalluraya.com','www.sailusfood.com','swayampaaka.com'}
for recipe in items:
    source=by_id[recipe['source_id']]
    url=urlparse(source['url'])
    assert url.scheme=='https' and url.hostname in allowed
    assert not source.get('fetch_error')
    assert source.get('structured_recipe_found') or source.get('manual_recipe_review')
    assert len(source['sha256'])==64 and source['author']
    assert recipe['title_kn'] and recipe['title_en'] and recipe['time_kn'] and recipe['batch_kn']
    assert len(recipe['ingredients'])>=5 and len(recipe['steps_kn'])>=3
    assert all(i['name_kn'].strip() and i['quantity_kn'].strip() for i in recipe['ingredients'])
    assert len({i['name_kn'] for i in recipe['ingredients']})==len(recipe['ingredients'])
    assert all(step.strip() for step in recipe['steps_kn'])
    assert 'article_body' not in recipe and 'image' not in recipe
panaka=next(r for r in items if r['id']=='panaka')
assert panaka['batch_kn'].startswith('1 ')
assert any(i['name_kn']=='ಬೆಲ್ಲ' and i['quantity_kn']=='2 ದೊಡ್ಡಚಮಚ' for i in panaka['ingredients'])
assert any('1–2 ಗಂಟೆ' in r['time_kn'] for r in items if r['id']=='kosambari')
print('PASS: five complete, unique Kannada recipes; reviewed sources, quantities, methods and soaking time present')
