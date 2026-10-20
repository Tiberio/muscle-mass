package br.com.meutreino.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import br.com.meutreino.MainViewModel
import br.com.meutreino.domain.ActivityEvents
import br.com.meutreino.domain.ActivityScore
import br.com.meutreino.domain.HealthIaaBridge
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Aqua = Color(0xFF45DEC5)
private val Violet = Color(0xFFBA9BFF)
private val Amber = Color(0xFFFFCB78)
private val DateLabel = DateTimeFormatter.ofPattern("EE", Locale("pt", "BR"))
private val TimeLabel = DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale("pt", "BR"))

private fun moment(at: Instant?): String = at?.atZone(ZoneId.systemDefault())?.format(TimeLabel) ?: "—"
private fun points(value: Double): String = String.format(Locale("pt", "BR"), "%.1f", value)

@Composable private fun Metric(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(PanelLight, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color=Muted, style=MaterialTheme.typography.labelMedium)
        Text(value,color=accent,style=MaterialTheme.typography.titleLarge)
    }
}

@Composable private fun StepsBars(values: List<Pair<LocalDate,Long>>) {
    val peak = (values.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    Row(Modifier.fillMaxWidth().height(148.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),
        verticalAlignment=Alignment.Bottom) {
        values.forEach { (day, count) ->
            Column(Modifier.weight(1f).fillMaxHeight(),horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.Bottom) {
                Text(if(count>0) "${count/1000}k" else "0",style=MaterialTheme.typography.labelSmall,color=Muted)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height((98.0 * count / peak).toFloat().coerceAtLeast(if(count>0) 3f else 0f).dp)
                    .background(if (count>0) Aqua else Color.Transparent, RoundedCornerShape(topStart=5.dp,topEnd=5.dp)))
                Spacer(Modifier.height(5.dp))
                Text(day.format(DateLabel).take(3),style=MaterialTheme.typography.labelSmall,color=Muted)
            }
        }
    }
}

/** Gráfico sem biblioteca externa: preserva lacunas (null) e não fabrica medições. */
@Composable private fun TrendPlot(days: List<LocalDate>, first: List<Double?>, firstColor: Color,
                                   second: List<Double?> = emptyList(), secondColor: Color = Violet) {
    val valid = (first + second).filterNotNull()
    if(valid.isEmpty()) { Subtext("Sem medidas suficientes para traçar o gráfico."); return }
    val lo = (valid.minOrNull() ?: 0.0) - 5.0
    val hi = (valid.maxOrNull() ?: 1.0) + 5.0
    val range = (hi - lo).coerceAtLeast(1.0)
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val w=size.width
        val h=size.height
        for (line in 0..3) {
            val y=h*line/3f
            drawLine(Color(0xFF355573),Offset(0f,y),Offset(w,y),strokeWidth=1.dp.toPx())
        }
        fun drawSeries(series: List<Double?>, tint: Color) {
            val pts=series.mapIndexed { idx, value ->
                if(value == null) null else Offset(
                    w*(idx.toFloat()/(days.size-1).coerceAtLeast(1)),
                    ((hi-value)/range*h).toFloat().coerceIn(2.dp.toPx(),h-2.dp.toPx())
                )
            }
            pts.zipWithNext().forEach { (a,b) ->
                if(a!=null && b!=null) drawLine(tint,a,b,strokeWidth=3.dp.toPx(),cap=StrokeCap.Round)
            }
            pts.filterNotNull().forEach { p -> drawCircle(tint,4.dp.toPx(),p) }
        }
        drawSeries(first,firstColor)
        if(second.isNotEmpty()) drawSeries(second,secondColor)
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        days.forEach { Text(it.format(DateLabel).take(3),color=Muted,style=MaterialTheme.typography.labelSmall) }
    }
}

@Composable fun HealthDashboard(vm:MainViewModel) {
    val h by vm.healthSnapshot.collectAsState()
    val message by vm.healthMessage.collectAsState()
    val busy by vm.healthBusy.collectAsState()
    val prefs by vm.prefs.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val sets by vm.allSets.collectAsState()
    val cardio by vm.cardioActivities.collectAsState()
    val now = LocalDate.now()
    val dates = (6 downTo 0).map { now.minusDays(it.toLong()) }
    val manualEvents = remember(sessions,sets,cardio) { ActivityEvents.events(sessions,sets,cardio) }
    val manual = ActivityScore.calculate(manualEvents,now)

    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Pane {
            Text("Painel de saúde e atividade",color=LightBlue,style=MaterialTheme.typography.titleLarge)
            // Consulta concluída pelo Meu Treino. Não corresponde ao momento em que
            // o Zepp Life enviou os registros ao Health Connect.
            Row(
                Modifier.fillMaxWidth().background(PanelLight, RoundedCornerShape(10.dp))
                    .padding(horizontal=12.dp, vertical=9.dp),
                horizontalArrangement=Arrangement.spacedBy(10.dp),
                verticalAlignment=Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccessTime, contentDescription=null, tint=Aqua,
                    modifier=Modifier.size(18.dp))
                Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text("ÚLTIMA CONSULTA AO HEALTH CONNECT",
                        style=MaterialTheme.typography.labelSmall,color=Muted)
                    Text(h?.let { moment(it.updatedAt) } ?: "Ainda não consultado",
                        style=MaterialTheme.typography.titleSmall,color=Aqua)
                }
                if(busy) CircularProgressIndicator(Modifier.size(18.dp),
                    strokeWidth=2.dp,color=Aqua)
            }
            Subtext("Este é o horário da leitura pelo Meu Treino, não o da sincronização do Zepp Life.")
            Subtext("Dados compartilhados com o Health Connect; não são uma leitura ao vivo da Mi Band 7.")
            if(h==null) {
                Subtext(message)
                PrimaryButton("Ler Health Connect",enabled=!busy,onClick=vm::refreshHealth)
            } else {
                OutlineButton(if(busy) "Atualizando…" else "Atualizar leitura", onClick=vm::refreshHealth)
                if(h!!.truncated) Text("Atenção: leitura parcial por limite de registros.",color=Amber)
            }
        }
        if(h!=null) {
            val snap=h!!
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Metric("Passos hoje",snap.stepsToday?.toString()?:"—",Aqua,Modifier.weight(1f))
                Metric("Passos / 7 dias",snap.stepsLast7Days?.toString()?:"—",LightBlue,Modifier.weight(1f))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Metric("Última FC",snap.latestHeartBpm?.let { "$it bpm" }?:"—",Violet,Modifier.weight(1f))
                Metric("Último peso",snap.latestWeightKg?.let { "%.1f kg".format(Locale("pt","BR"),it) }?:"—",Amber,Modifier.weight(1f))
            }
            Subtext("Medições: FC em ${moment(snap.latestHeartAt)} · Peso em ${moment(snap.latestWeightAt)}")
            Pane {
                Text("Passos nos últimos 7 dias",style=MaterialTheme.typography.titleMedium,color=Aqua)
                if(snap.stepsByDay.isNotEmpty()) {
                    StepsBars(dates.map { day -> day to (snap.stepsByDay.firstOrNull { it.date==day }?.steps?:0L) })
                    Subtext("Passos agregados pelo Health Connect, com prioridade de fontes aplicada pelo sistema.")
                } else Subtext("Sem autorização para leitura de passos.")
            }
            Pane {
                Text("Frequência cardíaca",style=MaterialTheme.typography.titleMedium,color=Violet)
                Subtext("Média diária de amostras disponíveis, em bpm. Uma média diária não é FC em repouso.")
                TrendPlot(dates,dates.map { day -> snap.heartByDay.firstOrNull { it.date==day }?.average?.toDouble() },Violet)
                Subtext("Última medição: ${moment(snap.latestHeartAt)} · Fonte: ${snap.latestHeartSource?:"não informada"}")
                snap.heartByDay.forEach { day ->
                    Subtext("${day.date.format(DateLabel)}: média ${day.average} · mín ${day.minimum} · máx ${day.maximum} bpm (${day.samples} amostras/minutos)")
                }
            }
            Pane {
                Text("Sono e recuperação",style=MaterialTheme.typography.titleMedium,color=Amber)
                if(snap.sleepPermission && snap.sleepByDay.isNotEmpty()) {
                    TrendPlot(dates,dates.map { day -> snap.sleepByDay.firstOrNull { it.date==day }?.minutes?.div(60.0) },Amber)
                    Subtext("Gráfico em horas de sono registrado; última sessão terminou em ${moment(snap.lastSleepAt)}.")
                    Subtext("Origem: ${snap.lastSleepSource?:"não informada"}")
                } else Subtext(if(snap.sleepPermission) "Nenhum registro de sono recebido." else "Permita leitura de sono em Conexões, se desejar.")
            }
            val age = prefs?.age?.toIntOrNull()
            val localDates = manualEvents.filter { it.kind==ActivityScore.Kind.CARDIO }
                .map { Instant.ofEpochMilli(it.timeMillis).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
            val hcSessions = snap.allExercise.map { it -> HealthIaaBridge.Session(
                "${it.source}:${it.recordId}",it.startedAt.toEpochMilli(),it.endedAt.toEpochMilli(),
                it.title,it.source?:"desconhecida"
            ) }
            val estimates = remember(snap,age,localDates) {
                HealthIaaBridge.assess(hcSessions,snap.heartSamples,age,localDates)
            }
            val joinedEvents = remember(manualEvents,estimates) { manualEvents+HealthIaaBridge.events(estimates) }
            val integrated = ActivityScore.calculate(joinedEvents,now)
            val accepted = estimates.count { it.status==HealthIaaBridge.Status.INCLUDED }
            Pane {
                Text("IAA — últimos sete dias",style=MaterialTheme.typography.titleMedium,color=LightBlue)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Metric("Registros locais",points(manual.balance),LightBlue,Modifier.weight(1f))
                    Metric("Com Health Connect*",points(integrated.balance),Aqua,Modifier.weight(1f))
                }
                TrendPlot(dates,manual.days.map { it.balance },LightBlue,integrated.days.map { it.balance },Aqua)
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("● Local",color=LightBlue,style=MaterialTheme.typography.labelSmall)
                    Text("● Integrado estimado",color=Aqua,style=MaterialTheme.typography.labelSmall)
                }
                Subtext("${accepted} de ${estimates.size} sessões externas incluídas na estimativa. Pontuação integrada calculada na leitura, sem gravar eventos duplicados no banco.")
                Subtext("*Só pontua sessões com FC em pelo menos 60% dos minutos, idade válida e ausência de cardio local no mesmo dia. Sem dados suficientes, o saldo local é preservado.")
                Subtext("Limiar estimado pela idade (220 − idade): 50–70% moderado; ≥70% vigoroso. Não é PAI e não mede risco clínico.")
                Subtext("IAA local: +${points(manual.earnedToday)} hoje, −${points(manual.expiredToday)} expirados. A janela contém as sete datas mais recentes.")
            }
            Pane {
                Text("Sessões importadas",style=MaterialTheme.typography.titleMedium,color=LightBlue)
                if(estimates.isEmpty()) Subtext("Nenhuma sessão de exercício compartilhada nos últimos 7 dias.")
                estimates.take(16).forEach { item ->
                    val reason = when(item.status) {
                        HealthIaaBridge.Status.INCLUDED -> "IAA +${points(item.estimatedPoints)} estimados (moderado ${item.moderateMinutes} min, vigoroso ${item.vigorousMinutes} min)"
                        HealthIaaBridge.Status.MISSING_AGE -> "Sem pontuação: informe idade válida no Perfil"
                        HealthIaaBridge.Status.INSUFFICIENT_HEART -> "Sem pontuação: FC insuficiente"
                        HealthIaaBridge.Status.DUPLICATE_LOCAL -> "Sem pontuação: cardio já registrado no Meu Treino neste dia"
                        HealthIaaBridge.Status.DUPLICATE_HEALTH -> "Sem pontuação: possível duplicidade entre fontes"
                        HealthIaaBridge.Status.INVALID_DURATION -> "Sem pontuação: duração fora dos limites"
                    }
                    Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text("${item.session.title} · ${moment(Instant.ofEpochMilli(item.session.startMillis))}")
                        Subtext("${(item.session.endMillis-item.session.startMillis)/60_000L} min · Cobertura de FC: ${item.coveragePercent}%")
                        Text(reason,style=MaterialTheme.typography.bodySmall,
                            color=if(item.status==HealthIaaBridge.Status.INCLUDED) Aqua else Amber)
                        Subtext("Fonte: ${item.session.source}")
                    }
                    HorizontalDivider(color=Color(0xFF36516B))
                }
                if(estimates.size>16) Subtext("Mostrando 16 de ${estimates.size} sessões.")
                Subtext("A fonte identifica o app que gravou os dados, não necessariamente o sensor. Mais de um app pode gravar a mesma sessão.")
            }
        }
    }
}
