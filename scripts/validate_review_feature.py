"""Impede repetir o erro histórico de enviar classes novas sem ligar a interface a elas."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def read(path):return (root/path).read_text()
app=read('app/src/main/java/br/com/meutreino/ui/AppRoot.kt')
plan=read('app/src/main/java/br/com/meutreino/ui/PlanAndToday.kt')
review=read('app/src/main/java/br/com/meutreino/ui/ReviewScreen.kt')
vm=read('app/src/main/java/br/com/meutreino/MainViewModel.kt')
db=read('app/src/main/java/br/com/meutreino/data/WorkoutDatabase.kt')
workout=read('app/src/main/java/br/com/meutreino/ui/WorkoutScreen.kt')
checks={
 'ReviewScreen conectado à navegação': 'page=="review" -> ReviewScreen' in app,
 'Botão solicitar variação no Plano': 'OutlineButton("Solicitar variação da ficha"' in plan,
 'Aviso de revisão no Plano': 'if(reviewNotice)' in plan,
 'Dupla confirmação': 'CONFIRMACAO_ANTECIPADA' in review and 'Continuar para prévia' in review,
 'Confirmação final': 'Aplicar nova ficha' in review,
 'Aplicação no ViewModel': 'fun applyReviewedVariation' in vm,
 'Versão do Room atualizada': 'version=5,exportSchema=false' in db,
 'Migração 4->5 registrada': 'MIGRATION_4_5' in vm and 'val MIGRATION_4_5 =' in db,
 'Histórico de auditoria': 'PlanReviewEventEntity' in db and 'insertReviewEvent' in vm,
 'Registros de série com ID real': 'vm.recordSet(active.id,ex.id' in workout,
 'Ficha efetiva no treino': 'EffectiveTrainingPlan.build(p,active.environment)' in workout,
}
for desc,ok in checks.items():
    assert ok, f'ERRO INTEGRAÇÃO: {desc}'
print('INTEGRACAO DE REVISAO OK: UI, dupla confirmação, motor, versões, migração e IDs reais')
