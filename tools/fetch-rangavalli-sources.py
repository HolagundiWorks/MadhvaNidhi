from html.parser import HTMLParser
from urllib.request import Request,urlopen
from urllib.parse import urljoin,urlparse
from pathlib import Path
import json,hashlib
ROOT=Path(__file__).resolve().parents[1]
pages=[('sans-dots-small','https://www.rangoli-sans-dots.com/2013/01/simple-kolam-with-dots-collection.html?m=0'),('sans-dots-archive','https://www.rangoli-sans-dots.com/2023/'),('traditional-kolam','https://rangolidesigns.net/traditional-kolam-designs-with-dots/')]
class Metadata(HTMLParser):
    def __init__(self):super().__init__();self.title=False;self.parts=[];self.images=[];self.canonical=''
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        if tag=='title':self.title=True
        if tag=='img':
            src=a.get('data-src') or a.get('src','')
            if src.startswith(('http','//')):self.images.append({'url':src,'alt':a.get('alt','')[:180]})
        if tag=='link' and a.get('rel')=='canonical':self.canonical=a.get('href','')
    def handle_endtag(self,tag):
        if tag=='title':self.title=False
    def handle_data(self,data):
        if self.title:self.parts.append(data)
results=[]
for id,url in pages:
    try:
        req=Request(url,headers={'User-Agent':'MadhvaNidhi-resource-review/1.0 (public page metadata only)'})
        raw=urlopen(req,timeout=35).read();parser=Metadata();parser.feed(raw.decode('utf-8',errors='replace'))
        unique={x['url']:x for x in parser.images if not any(w in x['url'].lower() for w in ['avatar','favicon','logo','badge'])}
        results.append({'id':id,'author':'Aishwarya' if id=='traditional-kolam' else 'Sudha Balaji','url':url,'canonical_url':parser.canonical or url,'title':' '.join(parser.parts).strip(),'checked_on':'2026-11-14','html_sha256':hashlib.sha256(raw).hexdigest(),'image_references':list(unique.values())[:24],'rights_status':'reference-only; no redistribution grant verified','image_bodies_imported':False})
        print(id+': '+str(len(unique))+' image references discovered; no image bodies downloaded')
    except Exception as error:
        results.append({'id':id,'url':url,'checked_on':'2026-11-14','error':str(error),'rights_status':'reference-only'})
        print(id+': '+type(error).__name__)
path=ROOT/'app/src/main/assets/rangavalli';path.mkdir(exist_ok=True)
(path/'sources.json').write_text(json.dumps({'scope':'Blog metadata and links only. App drawings are independent geometric studies, not reproductions of the linked photographs.','sources':results},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
