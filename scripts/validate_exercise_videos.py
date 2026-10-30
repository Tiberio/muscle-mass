#!/usr/bin/env python3
"""Validação estrutural do catálogo de vídeos (rodado também no GitHub Actions)."""
from pathlib import Path
import json,re,sys
root=Path(__file__).resolve().parents[1]
p=root/'app/src/main/assets/exercise_videos.json'
c=json.loads(p.read_text(encoding='utf-8'))
assert c['schema_version']==1
src=(root/'app/src/main/java/br/com/meutreino/domain/ExerciseCatalog.kt').read_text(encoding='utf-8')
extra=(root/'app/src/main/java/br/com/meutreino/domain/ExerciseLibraryData.kt').read_text(encoding='utf-8')
ids=set(re.findall(r'Exercise\("([a-z0-9]+)"',src)) | set(re.findall(r'^([a-zA-Z0-9]+)\|',extra,flags=re.M))
seen=set(); clips=0
assert len(c['videos']) >= 104, 'v0.4.6 preserva 44 vínculos e incorpora 60 revisões' 
for i,v in enumerate(c['videos']):
    eid=v['exercise_id']; yt=v['youtube_id']
    assert eid in ids,(i,eid,'ID ausente')
    assert eid not in seen,(i,eid,'duplicado')
    seen.add(eid)
    assert re.fullmatch(r'[a-zA-Z0-9_-]{11}',yt),(i,yt)
    assert v['source_url']=='https://www.youtube.com/watch?v='+yt
    assert len(v['author'])>0 and len(v['title'])>0
    if v.get('publisher_page'): assert v['publisher_page'].startswith('https://')
    start=v.get('start_seconds',0); end=v.get('end_seconds')
    assert type(start)==int and start>=0
    assert end is None or (type(end)==int and end>start)
    if end is not None:
        clips+=1
        assert v['verification'] in ('creator_chapters', 'visually_reviewed'), 'Não registrar recorte sem fonte de tempo'
    if v.get('verification') == 'visually_reviewed':
        assert v.get('review_source') == 'user_video_reviewer', (i,eid,'Revisão sem procedência')
        duration = v.get('duration_seconds')
        assert type(duration) is int and duration > 0, (i,eid,'Duração da auditoria inválida')
        if duration <= 40:
            assert start == 0 and end is None, (i,eid,'Vídeo curto deve permanecer integral')
        else:
            assert end is not None and end <= duration and 0 < end-start <= 40, (i,eid,'Recorte maior que 40 s ou inválido')
candidates=c.get('candidates',[])
candidate_ids=set()
for i, candidate in enumerate(candidates):
    eid=candidate['exercise_id']; yt=candidate['youtube_id']
    assert eid in ids and eid not in seen and eid not in candidate_ids, (i,eid,'Candidato duplicado')
    assert re.fullmatch(r'[a-zA-Z0-9_-]{11}',yt), (i,yt)
    assert candidate['source_url']=='https://www.youtube.com/watch?v='+yt
    assert candidate['verification']=='metadata_only'
    assert candidate['playback_status']=='external_review_only'
    assert len(candidate.get('matched_title','').strip())>1
    assert candidate.get('source') in ('levantamento_anterior','workout_database')
    duration=candidate.get('duration_seconds_from_source')
    assert duration is None or (type(duration)==int and duration>0)
    candidate_ids.add(eid)
searches=c.get('discovery',[])
search_ids=set()
for i, entry in enumerate(searches):
    eid=entry['exercise_id']
    assert eid in ids and eid not in seen and eid not in candidate_ids and eid not in search_ids, (i,eid,'Busca duplicada/inválida')
    assert entry.get('status') == 'search_only', (i,eid,'Status enganoso')
    assert len(entry.get('search_query','').strip())>10
    assert entry.get('exercise_name')
    search_ids.add(eid)
assert seen|candidate_ids|search_ids==ids, (len(seen),len(candidate_ids),len(search_ids),len(ids),'Falta cobertura')
assert not(seen & search_ids or seen & candidate_ids or candidate_ids & search_ids)
print(f'CATALOGO OK: {len(seen)} vídeos no player, {len(candidate_ids)} candidatos, {len(search_ids)} buscas, {clips} trechos; {len(ids)} exercícios no total')
