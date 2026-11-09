#!/usr/bin/env python3
"""Verificações estáticas do v1.0.0; NÃO substitui compilação Android nem teste de restauração real."""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
app = root/'app/src/main/java/br/com/meutreino'
gradle = (root/'app/build.gradle.kts').read_text()
assert 'versionName = "1.0.0"' in gradle
assert 'versionCode = 20' in gradle
wf = (root/'.github/workflows/android-debug.yml').read_text()
assert 'MeuTreino-Android-v1.0.0-debug' in wf
assert 'validate_release_v100.py' in wf
code = (app/'data/LocalBackup.kt').read_text()
db = (app/'data/WorkoutDatabase.kt').read_text()
vm = (app/'MainViewModel.kt').read_text()
ui = (app/'ui/LocalBackupPanel.kt').read_text()
for table in ('sessions', 'sets', 'cardio_activities', 'weight_measurements', 'body_measurements',
              'plan_versions', 'recovery_checkins', 'body_measurement_goals', 'plan_review_events'):
    assert f'"{table}" to arr(' in code, f'Falta serializador {table}'
    assert f'rows("{table}")' in code, f'Falta leitor {table}'
    assert f'FROM {table}' in db, f'Falta consulta {table}'
assert 'version=5' in db, 'A versão do banco não deveria mudar no lançamento 1.0.0'
assert 'db.withTransaction' in code
assert 'check(d.countNonPlanRecords() == 0 && d.countPlanVersions() <= 1)' in code
assert code.index('snapshot.sessions.forEach') < code.index('snapshot.sets.forEach'), 'FK sessão/série'
assert 'backupBusy' in vm and 'exportLocalBackup' in vm and 'restoreLocalBackup' in vm
assert 'CreateDocument("application/json")' in ui and 'OpenDocument()' in ui
assert 'LocalBackupPanel(vm)' in (app/'ui/HistoryDevicesProfile.kt').read_text()
assert 'LocalBackupPanel(vm, compact=true)' in (app/'ui/WizardScreen.kt').read_text()
assert '"0.6.0"' not in gradle
assert 'OnConflictStrategy.REPLACE' not in re.search(r'suspend fun insertSession.*', db).group()
print('RELEASE v1.0.0 OK: 9 coleções de backup, SHA-256, restauração bloqueada para histórico ocupado, UI SAF e Room v5')
