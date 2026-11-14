from urllib.request import Request,urlopen
from html.parser import HTMLParser
from pathlib import Path
import json,hashlib,os
ROOT=Path(__file__).resolve().parents[1]
sources=[('hayagreeva','https://udupi-recipes.com/2012/10/hayagreeva-hayagriva-recipe.html','Sumana / Udupi Recipes'),('avalakki','https://smithakalluraya.com/avalakki-panchakajjaya-recipe-sihi-avalakki-sweet-poha-recipe/','Smitha Kalluraya / Cook with Smile'),('kosambari','https://www.sailusfood.com/kosambari-recipe-moong-dal-salad','Sailaja Gudivada / Sailu’s Food'),('panaka','https://swayampaaka.com/juice/bellada-panaka-jaggery-panakam/','SwayamPaaka'),('payasa','https://udupi-recipes.com/2013/04/moong-dal-payasamhesaru-bele-payasa-recipe.html','Sumana / Udupi Recipes')]
class Page(HTMLParser):
    def __init__(self):super().__init__();self.in_title=False;self.title=[];self.in_json=False;self.parts=[];self.blocks=[]
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        if tag=='title':self.in_title=True
        if tag=='script' and a.get('type')=='application/ld+json':self.in_json=True;self.parts=[]
    def handle_endtag(self,tag):
        if tag=='title':self.in_title=False
        if tag=='script' and self.in_json:self.in_json=False;self.blocks.append(''.join(self.parts))
    def handle_data(self,data):
        if self.in_title:self.title.append(data)
        if self.in_json:self.parts.append(data)
def walk(obj):
    if isinstance(obj,list):
        for item in obj:yield from walk(item)
    if isinstance(obj,dict):
        typ=obj.get('@type',[])
        if typ=='Recipe' or isinstance(typ,list) and 'Recipe' in typ:yield obj
        for v in obj.values():
            if isinstance(v,(dict,list)):yield from walk(v)
registry=[];snapshots={}
for id,url,author in sources:
    try:
        raw=urlopen(Request(url,headers={'User-Agent':'MadhvaNidhi-recipe-reference/1.0'}),timeout=30).read();p=Page();p.feed(raw.decode('utf-8',errors='replace'))
        recipes=[]
        for block in p.blocks:
            try:recipes.extend(walk(json.loads(block)))
            except json.JSONDecodeError:pass
        snapshots[id]=recipes
        registry.append({'id':id,'url':url,'author':author,'page_title':' '.join(p.title).strip(),'checked_on':'2026-11-14','sha256':hashlib.sha256(raw).hexdigest(),'structured_recipe_found':bool(recipes),'manual_recipe_review':id=='panaka','usage':'Ingredient facts and concise original Kannada preparation summary; no article prose or photographs imported'})
        print(id+': '+str(len(recipes))+' structured recipe card(s)')
    except Exception as e:
        registry.append({'id':id,'url':url,'author':author,'checked_on':'2026-11-14','fetch_error':str(e),'usage':'Reference only; verify before bundling'})
        print(id+': '+type(e).__name__)
folder=ROOT/'app/src/main/assets/naivedya';folder.mkdir(exist_ok=True)
(folder/'sources.json').write_text(json.dumps({'sources':registry},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(Path(os.environ['TEMP'])/'mn-naivedya-structured.json').write_text(json.dumps(snapshots,ensure_ascii=False,indent=2),encoding='utf-8')
