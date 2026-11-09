package br.com.meutreino.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.meutreino.MainViewModel
import br.com.meutreino.domain.*

/** Revisão guiada: análise, dupla confirmação desfavorável, prévia e decisão final. */
@Composable
fun ReviewScreen(vm: MainViewModel, p: UserPrefs, onBack: () -> Unit) {
    val versions by vm.planVersions.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val sets by vm.allSets.collectAsState()
    val checkins by vm.recoveryCheckIns.collectAsState()
    val goals by vm.bodyGoals.collectAsState()
    val measurements by vm.bodyMeasurements.collectAsState()
    val events by vm.reviewEvents.collectAsState()
    val activeSession by vm.activeSession.collectAsState()
    var reason by remember { mutableStateOf("VARIEDADE") }
    var stage by remember { mutableStateOf("ANALISE") }
    var candidateIndex by remember { mutableIntStateOf(0) }
    var sending by remember { mutableStateOf(false) }
    val snapshot = ReviewSnapshotBuilder.prepare(p,versions,sessions,sets,checkins,goals,measurements,reason)
    val current = snapshot?.first
    val report = snapshot?.second
    // A proposta só é calculada após a decisão inicial e, quando exigida, a segunda confirmação.
    val proposals = remember(p,stage) {
        if(stage=="PREVIA" || stage=="FINAL") ReviewVariationProposal.candidates(p) else emptyList()
    }
    val selected = proposals.getOrNull(candidateIndex.coerceIn(0,(proposals.size-1).coerceAtLeast(0)))
    val verdictText = when(report?.verdict) {
        ReviewVerdict.MAINTAIN -> "Troca ainda não recomendada"
        ReviewVerdict.REVIEW -> "Reavaliação disponível; troca não obrigatória"
        ReviewVerdict.VARIATION_POSSIBLE -> "Variação pontual pode ser considerada"
        ReviewVerdict.CAUTION -> "Atenção à recuperação ou aos equipamentos"
        else -> "Sem dados suficientes"
    }
    val evidence = report?.explanations?.joinToString(" | ") ?: ""

    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick=onBack) { Text("← Plano") }
            Text("Revisão inteligente",color=LightBlue)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(current == null || report == null) {
                Pane {
                    Title("Ficha ainda não identificada")
                    Subtext("É preciso aceitar a ficha e registrar sua versão. Nenhuma troca foi feita.")
                    OutlineButton("Retornar ao plano",onBack)
                }
            } else if(stage=="PREVIA") {
                Pane {
                    Title("Prévia da variação")
                    Subtext("Esta proposta altera somente UM exercício, com mesmo padrão de movimento e grupo muscular, respeitando os equipamentos e o nível registrados. Não muda dias, cardio, séries ou descanso.")
                    if(proposals.isEmpty()) {
                        Subtext("Nenhuma substituição equivalente está disponível neste ambiente. A ficha será mantida. Você pode editar exercícios manualmente na aba Plano.")
                    } else {
                        Subtext("Escolha o exercício a variar (proposta ${candidateIndex.coerceIn(0,proposals.lastIndex)+1} de ${proposals.size}).")
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick={candidateIndex=(candidateIndex-1+proposals.size)%proposals.size},modifier=Modifier.weight(1f)) { Text("← Anterior") }
                            OutlinedButton(onClick={candidateIndex=(candidateIndex+1)%proposals.size},modifier=Modifier.weight(1f)) { Text("Próxima →") }
                        }
                        if(selected!=null) {
                            Text("${selected.planId} · ${selected.sessionTitle}",color=LightBlue)
                            Subtext("ATUAL: ${selected.previous.name}")
                            Subtext("PROPOSTA: ${selected.replacement.name}")
                            Subtext("Grupo: ${selected.previous.primaryMuscle} · movimento: ${selected.previous.pattern}")
                            Subtext("Equipamentos necessários: ${selected.replacement.required.ifEmpty { setOf("Nenhum adicional") }.joinToString()}")
                            Subtext("Orientação: ${selected.replacement.advice}")
                        }
                    }
                    OutlineButton("Cancelar e manter ficha") { stage="ANALISE" }
                    PrimaryButton("Revisar confirmação final",enabled=selected!=null&&activeSession==null&&!sending) { stage="FINAL" }
                }
            } else {
                Pane {
                    Title(verdictText)
                    Subtext("Ficha #${current.id} · avaliação local orientativa, sem diagnóstico médico. Limiares configuráveis são escolhas experimentais do aplicativo, não normas da OMS ou ACSM.")
                    report.explanations.forEach { Subtext("• $it") }
                    if(report.eligibleForEarlyReview) Text("Pelo menos um indicador alcançou a referência antecipada de ${p.reviewEarlyPercent}%.",color=LightBlue)
                    else Subtext("Ainda não alcançou o limiar antecipado de ${p.reviewEarlyPercent}% em tempo, sessões ou exposições.")
                }
                Pane {
                    Text("Motivo do pedido",color=LightBlue)
                    listOf("VARIEDADE" to "Desejo variar", "ESTAGNACAO" to "Não percebo progresso",
                        "EQUIPAMENTO" to "Equipamento indisponível", "DOR_PERSISTENTE" to "Dor persistente").forEach { (key,label) ->
                        Choice(label,reason==key,{ reason=key;stage="ANALISE" },Modifier.fillMaxWidth())
                    }
                    if(report.verdict == ReviewVerdict.CAUTION) {
                        Subtext("Nesta situação, não é seguro oferecer troca automática. Revise os equipamentos cadastrados ou procure orientação profissional se a dor persistir.")
                    } else {
                        OutlineButton("Manter ficha atual") {
                            vm.logReviewDecision(current.id,report.verdict.name,reason,evidence,"MANTIDA")
                            onBack()
                        }
                        PrimaryButton(if(report.requiresExtraConfirmation) "Quero variar mesmo assim" else "Ver variação proposta",
                            enabled=!sending && activeSession==null) {
                            stage=if(report.requiresExtraConfirmation)"CONFIRMACAO_ANTECIPADA" else "PREVIA"
                        }
                    }
                    if(activeSession!=null) Subtext("Finalize o treino atualmente aberto antes de aplicar alterações à ficha.")
                }
                Pane {
                    Text("Como você está se recuperando?",color=LightBlue)
                    Subtext("Registro opcional; não substitui avaliação de saúde. É possível revisar a ficha sem preencher.")
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick={vm.addRecovery("BEM"){}},modifier=Modifier.weight(1f)) { Text("Bem") }
                        OutlinedButton(onClick={vm.addRecovery("CANSADO"){}},modifier=Modifier.weight(1f)) { Text("Cansado") }
                    }
                    OutlineButton("Relatar dor persistente") { vm.addRecovery("DOR_PERSISTENTE") {} }
                    val recent=checkins.firstOrNull { it.planVersionId==current.id }
                    if(recent!=null) Subtext("Último registro: ${when(recent.state) { "BEM"->"bem"; "CANSADO"->"cansaço"; else->"dor persistente" }}")
                }
                Pane {
                    Text("Parâmetros de acompanhamento",color=LightBlue)
                    Subtext("Valores experimentais ajustáveis. Trocar um parâmetro não modifica a ficha nem apaga o histórico.")
                    Text("Janela: ${p.reviewWindowDays} dias")
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        listOf(42,56,70).forEach { days ->
                            Choice("${days/7} sem.",p.reviewWindowDays==days,
                                { vm.updateReviewSettings(days,p.reviewGroupExposures,p.reviewEarlyPercent) },Modifier.weight(1f))
                        }
                    }
                    Text("Exposições por grupo: ${p.reviewGroupExposures}")
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        listOf(8,10,12).forEach { n -> Choice("$n",p.reviewGroupExposures==n,
                            {vm.updateReviewSettings(p.reviewWindowDays,n,p.reviewEarlyPercent)},Modifier.weight(1f)) }
                    }
                    Text("Limiar de análise antecipada: ${p.reviewEarlyPercent}%")
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        listOf(75,80,90).forEach { pct -> Choice("$pct%",p.reviewEarlyPercent==pct,
                            {vm.updateReviewSettings(p.reviewWindowDays,p.reviewGroupExposures,pct)},Modifier.weight(1f)) }
                    }
                }
                if(events.isNotEmpty()) Pane {
                    Text("Últimas decisões",color=LightBlue)
                    events.take(5).forEach { event -> Subtext("${event.outcome} · ${event.userReason} · ${event.verdict} · ficha ${event.fromVersionId ?: "—"} → ${event.toVersionId ?: "mantida"}") }
                }
            }
        }
    }
    if(stage=="CONFIRMACAO_ANTECIPADA" && report!=null) AlertDialog(
        onDismissRequest={stage="ANALISE"},
        title={Text("Confirmar solicitação antecipada?")},
        text={Text("A ficha atual ainda pode oferecer bons resultados. Alterá-la agora pode dificultar a comparação da sua evolução. Deseja prosseguir para uma prévia, sem aplicar nada ainda?")},
        dismissButton={TextButton(onClick={stage="ANALISE"}) { Text("Cancelar e manter") }},
        confirmButton={TextButton(onClick={stage="PREVIA"}) { Text("Continuar para prévia") }}
    )
    if(stage=="FINAL" && current!=null && selected!=null && report!=null) AlertDialog(
        onDismissRequest={stage="PREVIA"},title={Text("Aplicar a nova ficha?")},
        text={Text("Confirme substituir ${selected.previous.name} por ${selected.replacement.name} em ${selected.sessionTitle}. A versão anterior permanecerá no histórico. Nenhuma outra troca será feita.")},
        dismissButton={TextButton(onClick={stage="PREVIA"}) { Text("Voltar à prévia") }},
        confirmButton={TextButton(enabled=!sending,onClick={
            sending=true
            vm.applyReviewedVariation(selected.planId,selected.originalId,selected.replacement.id,
                current.id,report.verdict.name,reason,evidence) { ok ->
                sending=false
                if(ok) onBack() else stage="ANALISE"
            }
        }) { Text("Aplicar nova ficha") }}
    )
}
