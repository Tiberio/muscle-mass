package br.com.meutreino.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import br.com.meutreino.data.BodyMeasurementGoalEntity
import br.com.meutreino.domain.ReviewEngine
import br.com.meutreino.data.BodyMeasurementEntity
import br.com.meutreino.data.PlanVersionEntity
import br.com.meutreino.data.WorkoutSessionEntity
import br.com.meutreino.data.WorkoutSetEntity
import br.com.meutreino.domain.BodyMeasurementRules
import br.com.meutreino.domain.ExerciseCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private fun day(time: Long): String = SimpleDateFormat("dd/MM/yy",Locale("pt","BR")).format(Date(time))
private fun number(value: Double): String = String.format(Locale("pt","BR"),"%.1f",value)

/** Gráfico simples offline com eixo vertical e valores acessíveis em texto abaixo. */
@Composable
fun SmallTrendChart(points: List<Pair<Long,Double>>, unit: String) {
    val series = points.sortedBy { it.first }
    if(series.isEmpty()) return
    val values = series.map { it.second }
    val low = values.minOrNull() ?: return
    val high = values.maxOrNull() ?: return
    val spread = (high-low).coerceAtLeast(1.0)
    val bottom = low - spread*0.08
    val top = high + spread*0.08
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("${number(top)} $unit",style=MaterialTheme.typography.labelSmall,color=Muted)
        Canvas(Modifier.fillMaxWidth().height(136.dp).background(PanelLight,RoundedCornerShape(10.dp))) {
            val left = 14.dp.toPx(); val right = size.width-14.dp.toPx()
            val ceiling = 12.dp.toPx(); val floor = size.height-12.dp.toPx()
            drawLine(color=Muted.copy(alpha=0.45f),start=Offset(left,ceiling),end=Offset(left,floor),strokeWidth=1.dp.toPx())
            drawLine(color=Muted.copy(alpha=0.45f),start=Offset(left,floor),end=Offset(right,floor),strokeWidth=1.dp.toPx())
            val positions = series.mapIndexed { index, (_,value) ->
                val x = if(series.size==1) (left+right)/2 else left+(right-left)*index/(series.size-1)
                val y = floor-(floor-ceiling)*((value-bottom)/(top-bottom)).toFloat()
                Offset(x,y)
            }
            positions.zipWithNext().forEach { (a,b) ->
                drawLine(color=LightBlue,start=a,end=b,strokeWidth=3.dp.toPx(),cap=StrokeCap.Round)
            }
            positions.forEach { point -> drawCircle(color=Blue,radius=4.dp.toPx(),center=point) }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("${number(low)} $unit",style=MaterialTheme.typography.labelSmall,color=Muted)
            Text("${day(series.first().first)} — ${day(series.last().first)}",style=MaterialTheme.typography.labelSmall,color=Muted)
        }
    }
}

/** Analisa somente séries de treinos concluídos, sem confundir treino planejado com executado. */
@Composable
fun PerformancePanel(sessions: List<WorkoutSessionEntity>, sets: List<WorkoutSetEntity>) {
    val done = remember(sessions) { sessions.filter { it.status=="COMPLETED" }.associateBy { it.id } }
    val recorded = remember(done,sets) { sets.filter { it.sessionId in done } }
    var selected by remember { mutableStateOf<String?>(null) }
    val grouped = remember(recorded) { recorded.groupBy { it.exerciseId }.toSortedMap() }
    Subtext("Evolução calculada exclusivamente com séries realmente registradas em sessões concluídas. A carga é a maior registrada em cada sessão; não representa força máxima.")
    Pane {
        Text("${done.size} sessões concluídas · ${recorded.size} séries",style=MaterialTheme.typography.titleMedium)
        val muscleExposures = remember(done, recorded) {
            done.values.flatMap { session ->
                recorded.filter { it.sessionId==session.id }
                    .mapNotNull { ExerciseCatalog.byId(it.exerciseId)?.primaryMuscle }
                    .distinct()
            }.groupingBy { it }.eachCount()
        }
        if (muscleExposures.isNotEmpty()) {
            Text("Exposições reais por grupo muscular",color=LightBlue)
            muscleExposures.toSortedMap().forEach { (muscle,count) ->
                Subtext("$muscle: $count sessões com pelo menos uma série registrada")
            }
        } else Subtext("Nenhum grupo muscular registrado em sessões concluídas.")
    }
    if(grouped.isEmpty()) Pane { Subtext("Conclua sessões com séries registradas para visualizar a progressão.") }
    grouped.forEach { (id, entries) ->
        val name = entries.firstOrNull()?.exerciseName ?: id
        Pane {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(name,modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
                TextButton(onClick={selected=if(selected==id)null else id}) {
                    Text(if(selected==id)"Fechar" else "Ver evolução")
                }
            }
            val last = entries.maxByOrNull { it.recordedAt }
            Subtext("${entries.size} séries · última: ${last?.repetitions ?: 0} repetições" +
                (last?.loadKg?.let { " · ${number(it)} kg" } ?: " · sem carga informada"))
            if(selected==id) {
                val bySession = entries.groupBy { it.sessionId }
                val bestLoads = bySession.mapNotNull { (sessionId, items) ->
                    val time = done[sessionId]?.finishedAt ?: done[sessionId]?.startedAt ?: return@mapNotNull null
                    val max = items.mapNotNull { it.loadKg }.maxOrNull() ?: return@mapNotNull null
                    time to max
                }.sortedBy { it.first }
                if(bestLoads.isNotEmpty()) {
                    Text("Maior carga por sessão",color=LightBlue)
                    SmallTrendChart(bestLoads,"kg")
                } else Subtext("Sem cargas registradas. Exercícios com peso corporal continuam no histórico abaixo.")
                entries.sortedByDescending { it.recordedAt }.take(20).forEach { item ->
                    Subtext("${day(item.recordedAt)} · ${item.repetitions} reps · " +
                        "${item.loadKg?.let { "${number(it)} kg" } ?: "peso não informado"} · " +
                        "RIR ${item.rir ?: "—"}")
                }
            }
        }
    }
}

@Composable
fun BodyMeasurementsPanel(
    items: List<BodyMeasurementEntity>,
    goals: List<BodyMeasurementGoalEntity>,
    onAdd: (String,Double,()->Unit)->Unit,
    onDelete: (BodyMeasurementEntity,()->Unit)->Unit,
    onSaveGoal: (String,Double,Double,()->Unit)->Unit,
    onDeleteGoal: (BodyMeasurementGoalEntity)->Unit
) {
    var site by remember { mutableStateOf(BodyMeasurementRules.sites.first()) }
    var menuOpen by remember { mutableStateOf(false) }
    var raw by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<BodyMeasurementEntity?>(null) }
    var targetRaw by remember { mutableStateOf("") }
    val parsed = BodyMeasurementRules.parse(raw)
    val valid = parsed!=null && BodyMeasurementRules.valid(parsed)
    Pane {
        Text("Avaliação corporal",style=MaterialTheme.typography.titleLarge,color=LightBlue)
        Subtext("Registre circunferências em centímetros, sempre no mesmo ponto anatômico e em condições semelhantes. São medidas de acompanhamento, não diagnóstico.")
        Text("Local da medida")
        Box {
            OutlinedButton(onClick={menuOpen=true},modifier=Modifier.fillMaxWidth()) {
                Text("$site  ▾")
            }
            DropdownMenu(expanded=menuOpen,onDismissRequest={menuOpen=false}) {
                BodyMeasurementRules.sites.forEach { option ->
                    DropdownMenuItem(text={Text(option)},onClick={site=option;menuOpen=false})
                }
            }
        }
        OutlinedTextField(
            value=raw,onValueChange={ candidate ->
                raw=candidate.filter { it.isDigit() || it==',' || it=='.' }.take(8)
            },label={Text("Circunferência (cm)")},singleLine=true,modifier=Modifier.fillMaxWidth()
        )
        if(raw.isNotBlank()&&!valid)Subtext("Informe um valor numérico entre 5 e 250 cm.")
        PrimaryButton("Salvar medição",enabled=valid) {
            onAdd(site,parsed!!) { raw="" }
        }
    }
    val readings = items.filter { it.site==site }.sortedBy { it.recordedAt }
    Pane {
        Text("Evolução — $site",style=MaterialTheme.typography.titleMedium)
        if(readings.isEmpty()) {
            Subtext("Ainda não há medidas para este local.")
        } else {
            val initial = readings.first(); val latest = readings.last()
            val diff = latest.centimeters-initial.centimeters
            Subtext("Primeira: ${number(initial.centimeters)} cm · última: ${number(latest.centimeters)} cm")
            if (readings.size>1) Subtext("Diferença observada: ${if(diff>=0)"+" else ""}${number(diff)} cm (não indica isoladamente ganho ou perda muscular).")
            SmallTrendChart(readings.map { it.recordedAt to it.centimeters },"cm")
            readings.asReversed().take(12).forEach { item ->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("${day(item.recordedAt)} · ${number(item.centimeters)} cm",modifier=Modifier.weight(1f))
                    TextButton(onClick={pendingDelete=item}) { Text("Excluir") }
                }
            }
        }
    }
    Pane {
        Text("Meta de circunferência (opcional)",color=LightBlue)
        Subtext("A meta registra a variação desejada de uma circunferência; não determina por si só troca de ficha nem comprova hipertrofia.")
        val goal=goals.firstOrNull { it.site==site }
        if(goal!=null) {
            Subtext("Início: ${number(goal.initialCm)} cm · alvo: ${number(goal.targetCm)} cm")
            val updated=items.filter { it.site==site && it.recordedAt>=goal.startedAt }.maxByOrNull { it.recordedAt }
            val progress=updated?.let { ReviewEngine.goalProgress(site,goal.initialCm,goal.targetCm,it.centimeters) }
            Subtext(if(progress==null) "Registre uma nova medição após definir a meta para acompanhar o progresso."
                else "Avanço da variação planejada: ${ReviewEngine.pct(progress.fraction)} (não corresponde à porcentagem da circunferência).")
            OutlineButton("Excluir meta",onClick={onDeleteGoal(goal)})
        } else {
            val latest=readings.lastOrNull()
            if(latest==null) Subtext("Registre primeiro uma medição para definir valor inicial e alvo.")
            else {
                Subtext("Medida inicial proposta: ${number(latest.centimeters)} cm (última registrada).")
                OutlinedTextField(value=targetRaw,onValueChange={targetRaw=it.filter { ch -> ch.isDigit()||ch==','||ch=='.' }.take(8)},
                    label={Text("Meta em centímetros")},singleLine=true,modifier=Modifier.fillMaxWidth())
                val desired=BodyMeasurementRules.parse(targetRaw)
                PrimaryButton("Definir meta",enabled=desired!=null && BodyMeasurementRules.valid(desired) && desired!=latest.centimeters) {
                    onSaveGoal(site,latest.centimeters,desired!!) { targetRaw="" }
                }
            }
        }
    }
    if(pendingDelete!=null) AlertDialog(
        onDismissRequest={pendingDelete=null},
        title={Text("Excluir medição?")},
        text={Text("Essa medida será removida do histórico deste aparelho.")},
        confirmButton={TextButton(onClick={
            val chosen=pendingDelete!!
            pendingDelete=null
            onDelete(chosen) { }
        }){Text("Excluir")}},
        dismissButton={TextButton(onClick={pendingDelete=null}){Text("Cancelar")}}
    )
}

@Composable
fun PlanVersionsPanel(versions: List<PlanVersionEntity>,sessions: List<WorkoutSessionEntity>,sets: List<WorkoutSetEntity>) {
    var expandedId by remember { mutableStateOf<Long?>(null) }
    val completed = remember(sessions) { sessions.filter { it.status=="COMPLETED" } }
    val legacy = sessions.count { it.planVersionId==null }
    Pane {
        Text("Histórico de fichas",style=MaterialTheme.typography.titleLarge,color=LightBlue)
        Subtext("Uma nova versão é registrada ao aprovar ou modificar o conteúdo da ficha e ao iniciar uma sessão em um ambiente diferente. As fichas anteriores ficam consultáveis.")
        if(legacy>0) Subtext("$legacy sessões anteriores à v0.5.0 não possuem vínculo histórico de versão; seus registros continuam preservados.")
    }
    if(versions.isEmpty()) Pane{ Subtext("Nenhuma ficha versionada encontrada. A primeira versão será criada com o plano aprovado.") }
    versions.forEachIndexed { index, version ->
        val completedHere = completed.filter { it.planVersionId==version.id }
        val allHere = sessions.filter { it.planVersionId==version.id }
        val days = TimeUnit.MILLISECONDS.toDays((System.currentTimeMillis()-version.createdAt).coerceAtLeast(0))
        Pane {
            Text("Ficha #${version.id}${if(index==0) " · registro mais recente" else ""}",style=MaterialTheme.typography.titleMedium)
            Subtext("Criada em ${day(version.createdAt)} · $days dias desde o registro")
            Subtext("${completedHere.size} sessões concluídas · ${allHere.size} sessões vinculadas")
            Subtext("Origem: ${version.reason}")
            TextButton(onClick={expandedId=if(expandedId==version.id)null else version.id}) {
                Text(if(expandedId==version.id)"Ocultar ficha" else "Ver ficha e grupos trabalhados")
            }
            if(expandedId==version.id) {
                val exposure = completedHere.flatMap { session ->
                    sets.filter { it.sessionId==session.id }
                        .mapNotNull { ExerciseCatalog.byId(it.exerciseId)?.primaryMuscle }.distinct()
                }.groupingBy { it }.eachCount()
                Text("Grupos musculares efetivamente treinados",color=LightBlue)
                if(exposure.isEmpty()) Subtext("Ainda não há séries registradas em sessões concluídas.")
                exposure.toSortedMap().forEach { (group,count) -> Subtext("$group: $count sessões") }
                Text("Conteúdo registrado da ficha",color=LightBlue)
                Subtext(version.content)
            }
        }
    }
}
