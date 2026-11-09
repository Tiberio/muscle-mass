"""Teste SQLite local de migração 4→5. O teste instrumentado do Room ainda é necessário."""
import sqlite3
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root/'app/src/main/java/br/com/meutreino/data/WorkoutDatabase.kt').read_text()
block=source.split('val MIGRATION_4_5 =',1)[1]
statements=re.findall(r'db\.execSQL\("([^"]+)"\)',block)
assert len(statements)==5, f'Esperados 5 SQLs, recebidos {len(statements)}'
con=sqlite3.connect(':memory:')
con.executescript('''
CREATE TABLE sessions(id INTEGER PRIMARY KEY, planId TEXT NOT NULL, planVersionId INTEGER,
 title TEXT NOT NULL, environment TEXT NOT NULL, startedAt INTEGER NOT NULL, finishedAt INTEGER,
 status TEXT NOT NULL, cardioMinutes INTEGER NOT NULL, cardioIntensity TEXT NOT NULL);
CREATE TABLE sets(id INTEGER PRIMARY KEY, sessionId INTEGER NOT NULL, exerciseId TEXT NOT NULL,
 exerciseName TEXT NOT NULL, setNumber INTEGER NOT NULL, repetitions INTEGER NOT NULL,
 loadKg REAL, rir INTEGER, recordedAt INTEGER NOT NULL);
CREATE TABLE weight_measurements(id INTEGER PRIMARY KEY, recordedAt INTEGER NOT NULL,
 kilograms REAL NOT NULL, source TEXT NOT NULL);
CREATE TABLE cardio_activities(id TEXT PRIMARY KEY,recordedAt INTEGER NOT NULL,minutes INTEGER NOT NULL,
 intensity TEXT NOT NULL,modality TEXT NOT NULL,source TEXT NOT NULL);
CREATE TABLE body_measurements(id INTEGER PRIMARY KEY, recordedAt INTEGER NOT NULL,site TEXT NOT NULL,
 centimeters REAL NOT NULL,source TEXT NOT NULL);
CREATE TABLE plan_versions(id INTEGER PRIMARY KEY,createdAt INTEGER NOT NULL,fingerprint TEXT NOT NULL,
 content TEXT NOT NULL,reason TEXT NOT NULL);
INSERT INTO sessions VALUES (7,'T1',3,'Treino anterior','Academia',123,456,'COMPLETED',0,'UNKNOWN');
INSERT INTO sets VALUES (9,7,'curl','Rosca',1,12,7.5,2,125);
INSERT INTO body_measurements VALUES (5,111,'Peitoral',95.3,'manual');
INSERT INTO weight_measurements VALUES (4,120,68.2,'Bluetooth');
INSERT INTO cardio_activities VALUES ('c',200,20,'LEVE','Caminhada','manual');
INSERT INTO plan_versions VALUES (3,100,'fingerprint1','Ficha anterior','Anterior');
''')
for sql in statements:con.execute(sql)
assert con.execute('SELECT planVersionId,status FROM sessions').fetchall()==[(3,'COMPLETED')]
assert con.execute('SELECT exerciseId,loadKg FROM sets').fetchall()==[('curl',7.5)]
assert con.execute('SELECT centimeters FROM body_measurements').fetchone()[0]==95.3
assert con.execute('SELECT kilograms FROM weight_measurements').fetchone()[0]==68.2
assert con.execute('SELECT id FROM cardio_activities').fetchone()[0]=='c'
assert con.execute('SELECT fingerprint FROM plan_versions').fetchone()[0]=='fingerprint1'
con.execute("INSERT INTO body_measurement_goals VALUES ('Peitoral',95.3,98.0,300)")
con.execute("INSERT INTO recovery_checkins(planVersionId,recordedAt,state) VALUES (3,301,'BEM')")
con.execute("INSERT INTO plan_review_events(recordedAt,fromVersionId,toVersionId,verdict,userReason,outcome,evidence) VALUES (302,3,NULL,'REVIEW','VARIEDADE','MANTIDA','dados reais')")
assert con.execute('SELECT COUNT(*) FROM plan_review_events').fetchone()[0]==1
assert con.execute('SELECT COUNT(*) FROM recovery_checkins').fetchone()[0]==1
assert con.execute('SELECT COUNT(*) FROM body_measurement_goals').fetchone()[0]==1
print('MIGRACAO 4→5 OK: dados anteriores preservados; metas, check-ins e auditoria operantes')
