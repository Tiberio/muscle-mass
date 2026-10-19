"""Smoke test da alteração SQL 2→3 (não substitui MigrationTestHelper no Android)."""
import sqlite3

con = sqlite3.connect(':memory:')
con.execute("CREATE TABLE sessions (id INTEGER PRIMARY KEY NOT NULL, title TEXT)")
con.execute("CREATE TABLE cardio_activities (id TEXT PRIMARY KEY NOT NULL, recordedAt INTEGER)")
con.execute("INSERT INTO sessions VALUES (1,'Treino existente')")
con.execute("INSERT INTO cardio_activities VALUES ('abc', 99)")
# Espelha MIGRATION_2_3
con.execute("CREATE TABLE IF NOT EXISTS `weight_measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordedAt` INTEGER NOT NULL, `kilograms` REAL NOT NULL, `source` TEXT NOT NULL)")
con.execute("CREATE INDEX IF NOT EXISTS `index_weight_measurements_recordedAt` ON `weight_measurements` (`recordedAt`)")
con.execute("INSERT INTO weight_measurements(recordedAt,kilograms,source) VALUES(123,70.25,'BLE')")
assert con.execute("SELECT title FROM sessions WHERE id=1").fetchone() == ('Treino existente',)
assert con.execute("SELECT id FROM cardio_activities").fetchone() == ('abc',)
assert con.execute("SELECT kilograms FROM weight_measurements").fetchone() == (70.25,)
print('Migração SQL 2→3 simulada; dados antigos preservados')
