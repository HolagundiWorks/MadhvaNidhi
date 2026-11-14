"""Create reproducible original geometric line studies and their SVG source paths."""
import json,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'resources/rangavalli';ART.mkdir(parents=True,exist_ok=True)
ASSETS=ROOT/'app/src/main/assets/rangavalli';ASSETS.mkdir(parents=True,exist_ok=True)
def rot(points,a,tx=256,ty=256):return [[round(tx+x*math.cos(a)-y*math.sin(a),3),round(ty+x*math.sin(a)+y*math.cos(a),3)] for x,y in points]
def curve(p0,p1,p2,p3):
    return [[(1-t)**3*p0[0]+3*(1-t)**2*t*p1[0]+3*(1-t)*t*t*p2[0]+t**3*p3[0],(1-t)**3*p0[1]+3*(1-t)**2*t*p1[1]+3*(1-t)*t*t*p2[1]+t**3*p3[1]] for t in [i/40 for i in range(41)]]
def petal(a,inner,outer,width):
    d=outer-inner
    return rot(curve((inner,0),(inner+d*.3,width),(outer-d*.25,width),(outer,0))+curve((outer,0),(outer-d*.25,-width),(inner+d*.3,-width),(inner,0))[1:],a)
def circle(r,cx=256,cy=256):return [[round(cx+r*math.cos(i*math.pi/40),3),round(cy+r*math.sin(i*math.pi/40),3)] for i in range(81)]
def polygon(r,n=4,angle=0):return [[round(256+r*math.cos(angle+i*2*math.pi/n),3),round(256+r*math.sin(angle+i*2*math.pi/n),3)] for i in range(n+1)]
patterns=[]
def add(id,kn,en,category,paths,steps,source):
    patterns.append({'id':id,'title_kn':kn,'title_en':en,'category':category,'construction':'Freehand geometric study','asset':f'rangavalli/{id}.png','source_id':source,'artwork_origin':'independent-original-geometric-study','steps_kn':steps,'paths':paths})
add('four_petals','ನಾಲ್ಕು ದಳದ ಹೂವು','Four-petal flower','floral',[petal(i*math.pi/2,20,180,75) for i in range(4)]+[petal(math.pi/4+i*math.pi/2,155,215,18) for i in range(4)]+[circle(18)],['ಮಧ್ಯದಲ್ಲಿ ಸಣ್ಣ ವೃತ್ತ ಬಿಡಿಸಿ.','ನಾಲ್ಕು ದಿಕ್ಕುಗಳಲ್ಲಿ ಸಮಾನ ಉದ್ದದ ದಳಗಳನ್ನು ಬಿಡಿಸಿ.','ಮೂಲೆಗಳಲ್ಲಿ ಸಣ್ಣ ಎಲೆಗಳನ್ನು ಸೇರಿಸಿ.'],'sans-dots-small')
add('lotus_eight','ಎಂಟು ದಳದ ಪದ್ಮ','Eight-petal lotus study','floral',[petal(i*math.pi/4,26,210,52) for i in range(8)]+[circle(26),circle(12)],['ಒಂದೇ ಕೇಂದ್ರದ ಎರಡು ಸಣ್ಣ ವೃತ್ತಗಳನ್ನು ಬಿಡಿಸಿ.','ನಾಲ್ಕು ಮುಖ್ಯ ದಿಕ್ಕುಗಳಲ್ಲಿ ದಳಗಳನ್ನು ಬಿಡಿಸಿ.','ಉಳಿದ ನಾಲ್ಕು ದಿಕ್ಕುಗಳಲ್ಲಿ ಸಮಾನ ದಳಗಳನ್ನು ಪೂರ್ಣಗೊಳಿಸಿ.'],'sans-dots-small')
star=[[256+(212 if i%2==0 else 145)*math.cos(i*math.pi/8),256+(212 if i%2==0 else 145)*math.sin(i*math.pi/8)] for i in range(17)]
add('eight_point_star','ಎಂಟು ಮೊನೆಯ ನಕ್ಷತ್ರ','Eight-point star','geometric',[star,polygon(110,4,math.pi/4),polygon(110,4,0),circle(24)],['ಎಂಟು ದಿಕ್ಕುಗಳಲ್ಲಿ ಹೊರ ಮೊನೆಗಳನ್ನು ಗುರುತಿಸಿ.','ಒಳ ಮತ್ತು ಹೊರ ಮೊನೆಗಳನ್ನು ಪರ್ಯಾಯವಾಗಿ ಜೋಡಿಸಿ.','ಮಧ್ಯದಲ್ಲಿ ಎರಡು ತಿರುಗಿದ ಚೌಕ ಮತ್ತು ಸಣ್ಣ ವೃತ್ತ ಸೇರಿಸಿ.'],'sans-dots-archive')
add('nested_diamonds','ಪದರದ ವಜ್ರಾಕೃತಿ','Nested diamonds','geometric',[polygon(r) for r in [55,122,190]]+[circle(8,256+219*math.cos(i*math.pi/2),256+219*math.sin(i*math.pi/2)) for i in range(4)],['ಮಧ್ಯದಲ್ಲಿ ಒಂದು ಸಣ್ಣ ವಜ್ರಾಕೃತಿ ಬಿಡಿಸಿ.','ಒಂದೇ ಅಂತರದಲ್ಲಿ ಇನ್ನೆರಡು ಹೊರ ಪದರಗಳನ್ನು ಬಿಡಿಸಿ.','ನಾಲ್ಕು ಹೊರ ಮೊನೆಗಳ ಬಳಿ ಸಣ್ಣ ವೃತ್ತಗಳನ್ನು ಸೇರಿಸಿ.'],'traditional-kolam')
squares=[]
for r in [80,115,150]:squares.append([[256-r,256-r],[256+r,256-r],[256+r,256+r],[256-r,256+r],[256-r,256-r]])
add('padi_square','ಸಮಾನಾಂತರ ಚೌಕ','Parallel-square study','geometric',squares+[petal(i*math.pi/2,153,218,32) for i in range(4)]+[polygon(38),circle(9)],['ಒಂದೇ ಕೇಂದ್ರದ ಮೂರು ಸಮಾನಾಂತರ ಚೌಕ ಬಿಡಿಸಿ.','ಪ್ರತಿ ಬದಿಯ ಮಧ್ಯದಲ್ಲಿ ಹೊರಮುಖ ದಳ ಸೇರಿಸಿ.','ಕೇಂದ್ರದಲ್ಲಿ ಸಣ್ಣ ವಜ್ರಾಕೃತಿ ಮತ್ತು ವೃತ್ತ ಬಿಡಿಸಿ.'],'sans-dots-archive')
lamps=[]
for i in range(4):
    a=i*math.pi/2
    local=[[[ -44,8],[44,8]],curve((44,8),(32,60),(-32,60),(-44,8)),curve((0,-8),(-28,-30),(0,-60),(6,-80))+curve((6,-80),(32,-42),(25,-20),(0,-8))[1:],[[-20,56],[20,56]]]
    for pts in local:lamps.append(rot([[x,y-138] for x,y in pts],a))
add('four_lamps','ನಾಲ್ಕು ದೀಪ','Four lamps','lamps',lamps+[petal(i*math.pi/2,6,55,18) for i in range(4)],['ಮಧ್ಯದಲ್ಲಿ ನಾಲ್ಕು ಸಣ್ಣ ದಳಗಳನ್ನು ಬಿಡಿಸಿ.','ನಾಲ್ಕು ದಿಕ್ಕುಗಳಲ್ಲಿ ದೀಪದ ಪಾತ್ರೆ ಮತ್ತು ಪಾದ ಬಿಡಿಸಿ.','ಪ್ರತಿ ಪಾತ್ರೆಯ ಮೇಲೆ ಹೊರಮುಖ ಜ್ವಾಲೆಯನ್ನು ಸೇರಿಸಿ.'],'sans-dots-small')
for p in patterns:
    lines=['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="1024" height="1024"><g fill="none" stroke="#000" stroke-width="5" stroke-linecap="round" stroke-linejoin="round">']
    lines += ['<polyline points="'+' '.join(f'{x},{y}' for x,y in pts)+'"/>' for pts in p['paths']]
    lines.append('</g></svg>');(ART/(p['id']+'.svg')).write_text('\n'.join(lines)+'\n',encoding='utf-8')
(ART/'geometry.json').write_text(json.dumps(patterns,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
metadata=[{k:v for k,v in p.items() if k!='paths'} for p in patterns]
(ASSETS/'index.json').write_text(json.dumps({'schema_version':1,'artwork_note':'Independent geometric line studies. Linked blog photographs and articles are references only, not bundled or traced.','items':metadata},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'Created {len(patterns)} original geometric SVGs and catalogue entries')
