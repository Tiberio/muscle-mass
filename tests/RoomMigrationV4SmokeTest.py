"""Simula a migração SQL 3→4 sobre dados existentes. Não substitui teste Android/Room."""
import re
import sqlite3
from pathlib import Path
source = (Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/meutreino/data/WorkoutDatabase.kt').read_text()
block = source.split('val MIGRATION_3_4 = ',1)[1].split('val MIGRATION_4_5 = ',1)[0]
statements = re.findall(r'db\.execSQL\("([^\"]+)"\)',block)
assert len(statements)==5, f'Esperados cinco comandos SQL, encontrados {len(statements)}'
conn = sqlite3.connect(':memory:')
conn.execute('PRAGMA foreign_keys=ON')
conn.execute('CREATE TABLE sessions (id INTEGER PRIMARY KEY NOT NULL, planId TEXT NOT NULL, title TEXT NOT NULL, environment TEXT NOT NULL, startedAt INTEGER NOT NULL, finishedAt INTEGER, status TEXT NOT NULL, cardioMinutes INTEGER NOT NULL, cardioIntensity TEXT NOT NULL)')
conn.execute('CREATE TABLE sets (id INTEGER PRIMARY KEY, sessionId INTEGER NOT NULL, exerciseId TEXT NOT NULL, exerciseName TEXT NOT NULL, setNumber INTEGER NOT NULL, repetitions INTEGER NOT NULL, loadKg REAL, rir INTEGER, recordedAt INTEGER NOT NULL)')
conn.execute('CREATE TABLE weight_measurements (id INTEGER PRIMARY KEY, recordedAt INTEGER NOT NULL, kilograms REAL NOT NULL, source TEXT NOT NULL)')
conn.execute('CREATE TABLE cardio_activities (id TEXT PRIMARY KEY, recordedAt INTEGER NOT NULL, minutes INTEGER NOT NULL, intensity TEXT NOT NULL, modality TEXT NOT NULL, source TEXT NOT NULL)')
conn.execute("INSERT INTO sessions VALUES (5,'T1','Treino antigo','Casa',100,NULL,'COMPLETED',0,'UNKNOWN')")
conn.execute("INSERT INTO sets VALUES (7,5,'pushup','Flexão de braços',1,10,NULL,2,101)")
conn.execute("INSERT INTO weight_measurements VALUES (9,102,70.0,'BLE')")
conn.execute("INSERT INTO cardio_activities VALUES ('abc',103,20,'LEVE','Caminhada','manual')")
for statement in statements: conn.execute(statement)
assert conn.execute('SELECT id,planVersionId FROM sessions').fetchall()==[(5,None)]
assert conn.execute('SELECT id,exerciseId FROM sets').fetchall()==[(7,'pushup')]
assert conn.execute('SELECT id,kilograms FROM weight_measurements').fetchall()==[(9,70.0)]
assert conn.execute('SELECT id FROM cardio_activities').fetchall()==[('abc',)]
conn.execute("INSERT INTO body_measurements(recordedAt,site,centimeters,source) VALUES (200,'Abdômen',82.4,'manual')")
conn.execute("INSERT INTO plan_versions(createdAt,fingerprint,content,reason) VALUES(201,'abc123','Treino A','Inicial')")
try:
    conn.execute("INSERT INTO plan_versions(createdAt,fingerprint,content,reason) VALUES(202,'abc123','Duplicado','Teste')")
except sqlite3.IntegrityError:
    pass
else:
    raise AssertionError('Fingerprint duplicado foi aceito')
assert conn.execute('SELECT count(*) FROM plan_versions').fetchone()[0]==1
print('MIGRACAO 3→4 OK: sessões, séries, cardio e pesagens preservados; medidas e fichas funcionais')
