#!/usr/bin/env python3
"""Cruzamento dos 161 exercícios pendentes com Workout Database.
Gera relatório completo, sem promover candidatos a vídeos visualmente aprovados.
Executa apenas com biblioteca padrão Python 3.10+.
"""
from __future__ import annotations
import argparse
import csv
import difflib
import json
from pathlib import Path
import re
import unicodedata
import urllib.request

ROOT=Path(__file__).resolve().parent
SOURCE_URL='https://raw.githubusercontent.com/rthepen/workout-database/main/dist/all_exercises.json'
YOUTUBE_ID=re.compile(r'^[A-Za-z0-9_-]{11}$')
MATERIALS={
    'dumbbells':{'dumbbells','dumbbell'},'barbell':{'barbell'},
    'resistance_band':{'resistance_band'},'kettlebell':{'kettlebell'},
    'cable':{'cable','cable_machine'},'machine':{'machine','cable_machine','smith_machine'},
    'smith_machine':{'smith_machine','machine'},
    'trx_suspension':{'trx_suspension'},
    'parallettes':{'parallettes','monkey_bars'},
    'bodyweight':{'bodyweight','monkey_bars'}
}
EQUIV={
    'dumbbell':'db','dumbbells':'db', 'dumbell':'db',
    'band':'resistance band','elastic':'resistance band',
    'pushup':'push up','pushups':'push up', 'pullups':'pull up',
    'chinups':'chin up','presses':'press','raises':'raise','rows':'row',
    'curls':'curl','squats':'squat','lunges':'lunge','flyes':'fly',
    'overhead':'overhead','machine':'machine'
}
TOKENS_FORBID={
  ('incline','decline'),('wide','close'),('regular','reverse'),('seated','standing'),
  ('single','double'),('assisted','negative'),('front','rear'),('one','two'),
  ('supinated','pronated'),('neutral','reverse'),('left','right')
}

def norm(s:str)->str:
    s=unicodedata.normalize('NFKD',str(s)).encode('ascii','ignore').decode().lower()
    s=s.replace('push-ups','pushup').replace('pull-ups','pullups').replace('chin-ups','chinups')
    s=re.sub(r'[^a-z0-9]+',' ',s)
    ts=[EQUIV.get(t,t) for t in s.split()]
    return ' '.join(' '.join(ts).split())

def tokens(s):return set(norm(s).split())

def score(a,b):
    a,b=norm(a),norm(b)
    if a==b:return 1.0
    ta,tb=set(a.split()),set(b.split())
    if not ta or not tb:return 0.
    dice=2*len(ta&tb)/(len(ta)+len(tb))
    seq=difflib.SequenceMatcher(None,a,b).ratio()
    v=.65*seq+.35*dice
    # Contradictory variants must not be silently treated as the same exercise.
    for x,y in TOKENS_FORBID:
        if (x in ta and y in tb) or (y in ta and x in tb):v-=.35
    if ('band' in ta and 'band' not in tb) or ('db' in ta and 'db' not in tb):v-=.18
    return max(0.,min(1.,v))

def materials_match(expect,actual,name):
    if actual not in MATERIALS.get(expect,{expect}):return False
    if expect=='bodyweight' and actual=='monkey_bars':
        return any(t in tokens(name) for t in ('pull','chin','hanging','hang','scapular','commando','archer'))
    if expect=='parallettes' and actual=='monkey_bars':
        return False  # Barra alta não substitui barras paralelas.
    if expect=='smith_machine' and actual=='machine':
        return 'smith' in tokens(name)
    return True

def video_candidates(row,source):
    vals=[]
    for ex in source:
        names=[ex.get('exercise_name',{}).get('en','')]+ex.get('aliases',[])
        names=[n for n in names if isinstance(n,str) and n]
        if not names:continue
        if not materials_match(row['material'],ex.get('material',{}).get('id'),names[0]):continue
        m=max((score(row['english_name'],n),n) for n in names)
        if m[0]<.64:continue
        vids=ex.get('media',{}).get('videos',[])
        vids=sorted((v for v in vids if YOUTUBE_ID.fullmatch(str(v.get('youtube_id','')))),key=lambda v:v.get('priority',99))
        if not vids:continue
        v=vids[0]; dur=v.get('duration_seconds')
        dur=dur if type(dur)==int and dur>0 else None
        vals.append({'source_id':ex.get('id'),'matched_name':names[0],
              'matched_alias':m[1],'source_material':ex.get('material',{}).get('id'),
              'score':round(m[0],4),'youtube_id':v['youtube_id'],
              'youtube_url':'https://www.youtube.com/watch?v='+v['youtube_id'],
              'source_duration_seconds':dur,'source_channel':v.get('channel',''),
              'video_type':v.get('type',''),
              'video_timing':'full_candidate_duration_le_40' if dur and dur<=40 else 'clip_needs_visual_review',
              'visual_audit':False})
    vals.sort(key=lambda c:(-c['score'],c['source_id']))
    return vals[:3]

def load_source(path):
    if path:return json.loads(Path(path).read_text(encoding='utf-8'))
    req=urllib.request.Request(SOURCE_URL,headers={'User-Agent':'MeuTreino/0.4.4 (exercise-curation; no YouTube videos downloaded)'})
    with urllib.request.urlopen(req,timeout=90) as r:return json.load(r)

def write_report(out, existing, pending):
    counts={s:sum(r['status']==s for r in pending) for s in sorted(set(r['status'] for r in pending))}
    doc=['# Pesquisa dos 161 exercícios pendentes — Meu Treino', '',
         'Fonte: '+SOURCE_URL,'',
         '## Método e limites',
         '- Todos os 161 IDs pendentes da lista acumulada foram comparados ao mesmo banco Workout Database.',
         '- Nome, sinônimos, variante e tipo de equipamento são considerados; sem correspondência segura, não há aprovação automática.',
         '- A duração é **a declarada pelo banco**, não uma inspeção independente do vídeo.',
         '- Se a duração publicada for superior a 40 s ou estiver ausente, **nenhum trecho foi inventado**.',
         '- Nenhum candidato novo é considerado visualmente auditado; antes de inseri-lo no aplicativo é necessário confirmar técnica, variante, disponibilidade e eventuais cortes.',
         '',f'## Totais: {len(existing)} vínculos prévios + {len(pending)} exercícios processados',
         '', '| Situação | Quantidade |','|---|---:|']
    doc.extend('| '+k+' | '+str(v)+' |' for k,v in counts.items())
    doc.extend(['','## Resultados dos 161 exercícios', '', '| ID | Exercício | Candidato prioritário | Similaridade | Situação |', '|---|---|---|---:|---|'])
    for r in pending:
        sel=r['selected']
        link='[Vídeo](https://www.youtube.com/watch?v='+sel['youtube_id']+')' if sel else '—'
        scr=str(sel.get('score','—')) if sel else '—'
        doc.append(f"| `{r['exercise_id']}` | {r['english_name']} | {link} | {scr} | {r['status']} |")
    doc += ['', '### IDs anteriores preservados', '']
    for eid,v in existing.items():
        if v.get('youtube_id'):
            doc.append(f"- `{eid}` — {v.get('exercise_name_pt',eid)}: https://www.youtube.com/watch?v={v['youtube_id']}")
    (out/'RELATORIO_161.md').write_text('\n'.join(doc)+'\n',encoding='utf-8')
    with (out/'RESULTADOS_161.csv').open('w',encoding='utf-8',newline='') as fp:
        wr=csv.writer(fp)
        wr.writerow(['exercise_id','english_name','material','status','score','matched_name','youtube_url','source_duration_seconds','needs_visual_review'])
        for r in pending:
            c=r['selected'] or {}
            wr.writerow([r['exercise_id'],r['english_name'],r['material'],r['status'],c.get('score',''),c.get('matched_name',''),c.get('youtube_url',''),c.get('source_duration_seconds',''),True])
    return counts

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--source',help='Arquivo all_exercises.json baixado anteriormente (se não informado, obtém do GitHub)')
    ap.add_argument('--output',default='resultado_pesquisa_161')
    args=ap.parse_args(); out=Path(args.output);out.mkdir(parents=True,exist_ok=True)
    existing=json.loads((ROOT/'levantamento_73_exercicios.json').read_text(encoding='utf-8'))
    seeds={r['exercise_id']:r for r in json.loads((ROOT/'candidatos_conferidos_na_fonte.json').read_text(encoding='utf-8'))}
    ali=list(csv.DictReader((ROOT/'aliases_en.tsv').open(encoding='utf-8'),delimiter='\t'))
    todo=[r for r in ali if not existing.get(r['exercise_id'],{}).get('youtube_id')]
    assert len(existing)==73 and len(todo)==161 and len({r['exercise_id'] for r in todo})==161
    source=load_source(args.source)
    if isinstance(source,dict):source=source.get('exercises',source.get('data'))
    if not isinstance(source,list) or len(source)<100:raise ValueError('Banco de origem não possui a lista esperada de exercícios.')
    pending=[]
    for row in todo:
        eid=row['exercise_id']; candidates=video_candidates(row,source)
        seed=seeds.get(eid)
        if seed:
            seedview={'source_id':seed['source_id'],'matched_name':seed['matched_name'],
                      'score':1.,'youtube_id':seed['youtube_id'],
                      'youtube_url':'https://www.youtube.com/watch?v='+seed['youtube_id'],
                      'source_duration_seconds':seed['duration_seconds'],
                      'source_channel':'','visual_audit':False,
                      'video_timing':'full_candidate_duration_le_40' if seed['duration_seconds'] and seed['duration_seconds']<=40 else 'clip_needs_visual_review',
                      'source_line':seed['source_line']}
            status='matched_in_public_source_metadata_needs_video_audit';selected=seedview
            # Put hand-researched IDs first; still include alternate dataset matches.
            candidates=[seedview]+[c for c in candidates if c['youtube_id']!=seed['youtube_id']][:2]
        elif candidates and candidates[0]['score']>=.92:
            status='strong_metadata_match_needs_video_audit';selected=candidates[0]
        elif candidates:
            status='ambiguous_metadata_match_needs_human_selection';selected=None
        else:
            status='no_compatible_video_in_source';selected=None
        pending.append({'exercise_id':eid,'english_name':row['english_name'],'material':row['material'],
                        'status':status,'selected':selected,'alternatives':candidates})
    result={'source':SOURCE_URL,'source_records':len(source),'baseline_links':73,
            'checked_pending':161,'no_new_visual_audit':True,'results':pending}
    (out/'RESULTADOS_161.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    stats=write_report(out,existing,pending)
    print('Banco:',len(source),'| registros pendentes:',len(pending),'| contagens:',stats)
    print('Saída:',out.resolve())

if __name__=='__main__':main()
