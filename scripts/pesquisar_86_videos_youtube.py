#!/usr/bin/env python3
"""Pesquisa complementos para 86 movimentos no YouTube Data API.

NÃO aprova execução técnica, NÃO grava o JSON do app e NÃO baixa vídeos.
Consome uma solicitação search.list por exercício e pequenas chamadas videos.list.
"""
import argparse
import csv
from datetime import datetime, timezone
from difflib import SequenceMatcher
import json
import os
from pathlib import Path
import re
import unicodedata
import urllib.parse
import urllib.request
from urllib.error import HTTPError, URLError

SEARCH_URL = 'https://www.googleapis.com/youtube/v3/search'
VIDEOS_URL = 'https://www.googleapis.com/youtube/v3/videos'
ISO = re.compile(r'^P(?:(?P<days>\d+)D)?(?:T(?:(?P<hours>\d+)H)?(?:(?P<minutes>\d+)M)?(?:(?P<seconds>\d+)S)?)?$')
PENALTIES = {'mistakes','wrong','avoid','bad','errors','reaction','prank','challenge','top','best','compilation','workout','routine','fail','fails','versus'}
STOPWORDS = {'the','and','for','with','how','to','a','an','of','do','your','exercise','demonstration','form','proper','correct','guide','tutorial','training'}

def normalize(s):
    s=unicodedata.normalize('NFKD', s or '').encode('ascii','ignore').decode().lower()
    return re.sub(r'[^a-z0-9]+',' ',s).strip()

def rank_title(target, title):
    a=set(normalize(target).split())-STOPWORDS
    b=set(normalize(title).split())-STOPWORDS
    overlap=(len(a&b)/len(a)) if a else 0
    similar=SequenceMatcher(None,normalize(target),normalize(title)).ratio()
    penalty=sum(0.14 for t in PENALTIES if t in b)
    # Não é avaliação do movimento, só priorização para revisão.
    return round(max(0,min(1, 0.75*overlap+0.25*similar-penalty)),3)

def duration(iso):
    m=ISO.fullmatch(iso or '')
    if not m:return None
    d={k:int(v or 0) for k,v in m.groupdict().items()}
    return d['days']*86400+d['hours']*3600+d['minutes']*60+d['seconds']

def api_get(url, params, key):
    params={**params,'key':key}
    req=urllib.request.Request(url+'?'+urllib.parse.urlencode(params),headers={'Accept':'application/json','User-Agent':'MeuTreinoVideoResearch/1.0'})
    try:
        with urllib.request.urlopen(req,timeout=30) as response:
            return json.load(response)
    except HTTPError as ex:
        raise RuntimeError(f'YouTube API: HTTP {ex.code}; confira cota, chave e API habilitada.') from None
    except (URLError,TimeoutError) as ex:
        raise RuntimeError('Falha de conexão com a API do YouTube.') from None

def fetch_video_details(ids,key):
    details={}
    ids=sorted(set(x for x in ids if re.fullmatch(r'[A-Za-z0-9_-]{11}',x)))
    for i in range(0,len(ids),50):
        response=api_get(VIDEOS_URL,{'part':'snippet,contentDetails,status','id':','.join(ids[i:i+50])},key)
        for x in response.get('items',[]):
            details[x['id']]=x
    return details

def summarize_video(video):
    if not video:return {'disponivel':False,'incorporavel':False,'br':None,'duracao_segundos':None}
    region=video.get('contentDetails',{}).get('regionRestriction',{})
    allowed=region.get('allowed');blocked=region.get('blocked',[])
    return {'disponivel':True,'incorporavel':video.get('status',{}).get('embeddable') is True,
        'br':('BR' in allowed if allowed is not None else 'BR' not in blocked),
        'duracao_segundos':duration(video.get('contentDetails',{}).get('duration')),
        'titulo_youtube':video.get('snippet',{}).get('title',''),
        'canal':video.get('snippet',{}).get('channelTitle','')}

def run(candidates,missing,key):
    searches=[];failures=[]
    for i,item in enumerate(missing,1):
        query=f'{item["nome_en"]} exercise demonstration'
        try:
            result=api_get(SEARCH_URL,{'part':'snippet','q':query,'maxResults':5,'type':'video',
                 'videoEmbeddable':'true','relevanceLanguage':'en','safeSearch':'strict'},key)
            found=[]
            for v in result.get('items',[]):
                id=v.get('id',{}).get('videoId')
                if not id:continue
                sn=v.get('snippet',{})
                found.append({'youtube_id':id,'youtube_url':f'https://www.youtube.com/watch?v={id}',
                    'titulo_busca':sn.get('title',''),'canal_busca':sn.get('channelTitle',''),
                    'pontuacao_titulo':rank_title(item['nome_en'],sn.get('title',''))})
            found.sort(key=lambda x:x['pontuacao_titulo'],reverse=True)
            searches.append({'exercise_id':item['exercise_id'],'nome_pt':item['nome_pt'],'nome_en':item['nome_en'],
                'consulta':query,'status':'candidatos_pendentes_de_revisao' if found else 'sem_resultados',
                'opcoes':found[:3]})
        except RuntimeError as ex:
            failures.append({'exercise_id':item['exercise_id'],'erro':str(ex)})
            # Se a chave perder a cota, não repetir dezenas de solicitações que vão falhar.
            failures.extend({'exercise_id':x['exercise_id'],'erro':'Não consultado após falha de API'} for x in missing[i:])
            break
        if i%10==0:print(f'Pesquisados {i}/{len(missing)} exercícios')
    ids=[x['youtube_id'] for entry in searches for x in entry['opcoes']]
    ids.extend(x['youtube_id'] for x in candidates)
    try:detail=fetch_video_details(ids,key)
    except RuntimeError as ex:
        failures.append({'exercise_id':'METADADOS','erro':str(ex)});detail={}
    for entry in searches:
        for choice in entry['opcoes']:
            choice.update(summarize_video(detail.get(choice['youtube_id'])))
            choice['status']='CANDIDATO_A_REVISAO_MANUAL'
    reviewed=[]
    for entry in candidates:
        d=dict(entry);d['auditoria_youtube']=summarize_video(detail.get(entry['youtube_id']))
        reviewed.append(d)
    return {'schema_version':1,'gerado_em':datetime.now(timezone.utc).isoformat(),
        'aviso':'Associação e ranqueamento por metadados; NÃO há revisão visual nem aprovação da técnica.',
        'total_candidatos_previos':len(candidates),'total_pendentes_pesquisados':len(searches),
        'total_pendentes_sem_busca':len(missing)-len(searches),'candidatos_previos':reviewed,
        'pesquisas':searches,'falhas':failures}

def save_report(res,dest):
    dest.mkdir(parents=True,exist_ok=True)
    (dest/'RESULTADO_PESQUISA_130.json').write_text(json.dumps(res,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    lines=['# Meu Treino — pesquisa complementar de vídeos','',f"Consulta concluída para **{res['total_pendentes_pesquisados']} exercícios**; {res['total_pendentes_sem_busca']} não consultados.",
        '', '**Atenção:** os resultados são candidatos por título/metadados e NÃO vídeos aprovados.',
        '', '## 44 candidatos encontrados anteriormente','',
        '| Exercício | Link | Duração (API) | Incorporável |','|---|---|---:|---|']
    for e in res['candidatos_previos']:
        a=e['auditoria_youtube'];lines.append(f"| {e['nome_pt']} | [Assistir]({e['youtube_url']}) | {a.get('duracao_segundos') or '—'} | {'Sim' if a.get('incorporavel') else 'Não ou não informado'} |")
    lines += ['','## Busca para os demais exercícios','','| Exercício | Melhor resultado | Alternativas |','|---|---|---|']
    flat=[]
    for e in res['pesquisas']:
        opts=e['opcoes'];first=f"[Assistir]({opts[0]['youtube_url']}) ({opts[0]['pontuacao_titulo']:.2f})" if opts else '—'
        other=', '.join(f"[Vídeo {i+2}]({o['youtube_url']})" for i,o in enumerate(opts[1:])) if opts else '—'
        lines.append(f"| {e['nome_pt']} | {first} | {other} |")
        for j,o in enumerate(opts): flat.append({'exercise_id':e['exercise_id'],'nome_pt':e['nome_pt'],'nome_en':e['nome_en'],'ordem':j+1,**o})
    if res['falhas']:
        lines+=['','## Falhas ou pesquisas não realizadas','']+[f"- {x['exercise_id']}: {x['erro']}" for x in res['falhas']]
    (dest/'RELATORIO_PESQUISA_130.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    with (dest/'CANDIDATOS_YOUTUBE_86.csv').open('w',newline='',encoding='utf-8-sig') as f:
        fields=['exercise_id','nome_pt','nome_en','ordem','youtube_id','youtube_url','titulo_busca','canal_busca','pontuacao_titulo','disponivel','incorporavel','br','duracao_segundos','titulo_youtube','canal','status']
        w=csv.DictWriter(f,fieldnames=fields,extrasaction='ignore');w.writeheader();w.writerows(flat)

def main():
    p=argparse.ArgumentParser();p.add_argument('--directory',default='pesquisa_videos_130');p.add_argument('--output',default='reports/pesquisa130')
    args=p.parse_args();key=os.environ.get('YOUTUBE_API_KEY','').strip()
    if not key: raise SystemExit('Erro: variável YOUTUBE_API_KEY não definida nos Secrets do GitHub.')
    here=Path(args.directory)
    c=json.loads((here/'CANDIDATOS_A_REVISAR.json').read_text(encoding='utf-8'))
    m=json.loads((here/'PENDENTES_SEM_LINK.json').read_text(encoding='utf-8'))
    save_report(run(c,m,key),Path(args.output))
    print('Relatórios gerados em',args.output)

if __name__=='__main__':main()
