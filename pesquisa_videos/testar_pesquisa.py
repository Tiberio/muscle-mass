import importlib.util, pathlib, csv, json
p=pathlib.Path(__file__).parent
spec=importlib.util.spec_from_file_location('search',p/'pesquisar_161.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
assert m.score('Dumbbell Floor Press','Dumbbell Floor Press')==1
assert m.score('Decline Push Up','Incline Push Up') < m.score('Incline Push Up','Incline Push Up')
assert not m.materials_match('barbell','dumbbells','Bench Press')
assert not m.materials_match('parallettes','monkey_bars','Dips')
assert m.materials_match('bodyweight','monkey_bars','Chin Ups')
assert len(json.loads((p/'levantamento_73_exercicios.json').read_text()))==73
r=list(csv.DictReader((p/'aliases_en.tsv').open(),delimiter='\t'))
assert len(r)==174 and len({x['exercise_id'] for x in r})==174
seeds=json.loads((p/'candidatos_conferidos_na_fonte.json').read_text())
assert len(seeds)==13 and len({x['exercise_id'] for x in seeds})==13
assert all(m.YOUTUBE_ID.fullmatch(x['youtube_id']) for x in seeds)
source=[{'id':'dumbbells_db_floor_press','exercise_name':{'en':'Dumbbell Floor Press'},
         'aliases':['Dumbbell Floor Press'],'material':{'id':'dumbbells'},
         'media':{'videos':[{'youtube_id':'O1x7AoUf5Vs','priority':1,'duration_seconds':13}]}}]
assert m.video_candidates({'english_name':'Dumbbell Floor Press','material':'dumbbells'},source)[0]['youtube_id']=='O1x7AoUf5Vs'
assert not m.video_candidates({'english_name':'Dumbbell Floor Press','material':'barbell'},source)
print('Verificações passaram.')
