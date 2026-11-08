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
assert len(c['videos']) == 233, 'v0.4.7: 104 vídeos preservados + 129 aprovados' 
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
# Alternativas da pesquisa não equivalem a demonstrações aprovadas.
# São somente links externos, nunca um segundo player embutido.
alternatives=c.get('alternatives',[])
assert len(alternatives) == 172, 'A pesquisa final tinha 172 opções alternativas'
alt_keys=set()
for i, v in enumerate(alternatives):
    eid=v['exercise_id']; yt=v['youtube_id']
    assert eid in seen, (i,eid,'Alternativa sem vídeo principal')
    assert (eid,yt) not in alt_keys, (i,eid,yt,'Alternativa duplicada')
    alt_keys.add((eid,yt))
    assert yt != next(row['youtube_id'] for row in c['videos'] if row['exercise_id']==eid)
    assert re.fullmatch(r'[a-zA-Z0-9_-]{11}',yt)
    assert v['source_url']=='https://www.youtube.com/watch?v='+yt
    assert v['verification']=='not_visually_reviewed'
    assert v['playback_status']=='external_only'
    assert v['title'].strip() and v['author'].strip()
    assert isinstance(v['duration_seconds'], int) and v['duration_seconds']>0
assert len([v for v in c['videos'] if v['verification']=='visually_reviewed']) >= 189
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
assert len(search_ids) == 1 and 'cablechest' in search_ids, 'Somente supino em pé nos cabos permanece sem demonstração aprovada'
assert seen|candidate_ids|search_ids==ids, (len(seen),len(candidate_ids),len(search_ids),len(ids),'Falta cobertura')
assert not(seen & search_ids or seen & candidate_ids or candidate_ids & search_ids)
print(f'CATALOGO OK: {len(seen)} vídeos no player, {len(alternatives)} links externos não revisados, {len(candidate_ids)} candidatos, {len(search_ids)} buscas, {clips} trechos; {len(ids)} exercícios no total')

# Regressão v0.4.8: um catálogo íntegro não basta se a UI continuar usando
# o campo legado Exercise.youtubeVideoId. Isso causou a falha da v0.4.7.
ui_panel=(root/'app/src/main/java/br/com/meutreino/ui/ExerciseLibraryPanel.kt').read_text(encoding='utf-8')
ui_player=(root/'app/src/main/java/br/com/meutreino/ui/ExerciseVideoPlayer.kt').read_text(encoding='utf-8')
repository=(root/'app/src/main/java/br/com/meutreino/ui/ExerciseVideoAssetRepository.kt').read_text(encoding='utf-8')
assert re.search(r'\bExerciseVideoControl\s*\(\s*exercise\.id\s*\)', ui_panel), (
    'Biblioteca de exercícios não chama o componente do player. Verifique se ExerciseLibraryPanel.kt foi enviado ao GitHub.'
)
assert 'exercise.youtubeVideoId' not in ui_panel, 'A UI ainda usa o campo legado youtubeVideoId'
assert 'Vídeo específico ainda não verificado.' not in ui_panel, 'Mensagem legada encontrada: painel desatualizado'
assert 'ExerciseVideoAssetRepository.ensureLoaded' in ui_player, 'Player não carrega catálogo JSON'
assert 'exercise_videos.json' in repository, 'Repositório não utiliza o asset de vídeos'
print('INTEGRACAO UI OK: biblioteca -> ExerciseVideoControl -> catálogo JSON')
