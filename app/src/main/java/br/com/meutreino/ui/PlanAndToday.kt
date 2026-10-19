package br.com.meutreino.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.meutreino.data.WorkoutSessionEntity
import br.com.meutreino.domain.*
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@Composable fun PlanProposal(plan:TrainingPlan,p:UserPrefs,onAccept:()->Unit,onEdit:()->Unit,
    onOverride:(String,String,String)->Unit){
    var editingExercise by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Title("Seu primeiro plano")
        Subtext("Proposta inicial baseada nas preferências informadas. Revise antes de aceitar.")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Pane {Subtext(plan.explanation)}
            plan.sessions.forEach { session -> Pane {
                Text("${session.day} · ${session.title}",style=MaterialTheme.typography.titleMedium)
                Subtext("${session.minutes} min · ${session.exercises.size} exercícios")
                session.exercises.forEach { prescribed ->
                    val ex=prescribed.exercise
                    val key="${session.id}:${ex.id}"
                    val actual=ExerciseCatalog.byId(p.overrides[key]?:ex.id)?:ex
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text("• ${actual.name} · ${prescribed.sets}×${prescribed.minReps}–${prescribed.maxReps}",modifier=Modifier.weight(1f))
                        TextButton(onClick={editingExercise=if(editingExercise==key)null else key}){Text("Trocar")}
                    }
                    if(editingExercise==key){
                        val candidates=ExerciseCatalog.options(p.defaultEnvironment,p.equipments,p.experience).filter{it.pattern==ex.pattern}
                        if(candidates.isEmpty())Subtext("Nenhuma alternativa compatível encontrada.")
                        candidates.forEach{candidate->
                            Choice(candidate.name,candidate.id==actual.id,{
                                onOverride(session.id,ex.id,candidate.id);editingExercise=null
                            },Modifier.fillMaxWidth())
                        }
                    }
                }
                session.warning?.let{ Text(it,color=Color(0xFFFFC77A)) }
            }}
            if(plan.warnings.isNotEmpty())Pane{
                Text("Observações antes de começar",color=LightBlue)
                plan.warnings.forEach{Subtext("• $it")}
            }
            Subtext("Este programa é uma proposta inicial. Pare um exercício que cause dor e procure orientação profissional quando houver restrições ou dúvidas de segurança.")
        }
        OutlineButton("Revisar minhas respostas",onEdit)
        PrimaryButton("Aceitar plano e ir para Hoje",onClick=onAccept)
    }
}

@Composable fun TodayScreen(
    p:UserPrefs,plan:TrainingPlan,temporary:String?,active:WorkoutSessionEntity?,
    history:List<WorkoutSessionEntity>,onChangeToday:(String?)->Unit,
    onStart:(PlannedSession)->Unit,onResume:()->Unit
){
    val env=temporary?:p.defaultEnvironment.ifBlank{p.environments.firstOrNull()?:"Casa sem equipamentos"}
    val actualPlan=remember(p,env){TrainingPlanner.build(p.copy(defaultEnvironment=env))}
    val now=LocalDate.now()
    val todayIndex=now.dayOfWeek.value-1
    val weekStart=now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val completedThisWeek=history.filter{it.status=="COMPLETED"&&it.startedAt>=weekStart}.map{it.planId}.toSet()
    val weekdayNames=listOf("Seg","Ter","Qua","Qui","Sex","Sáb","Dom")
    val session=actualPlan.sessions.filter{it.id !in completedThisWeek}
        .minByOrNull{(weekdayNames.indexOf(it.day)-todayIndex+7)%7}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Title("Treino de hoje")
        Subtext("${p.objective} · ${history.count { it.status=="COMPLETED" }} sessões concluídas no histórico")
        if(active!=null)Pane{
            Text("Treino em andamento",color=LightBlue)
            Text(active.title,style=MaterialTheme.typography.titleLarge)
            Subtext("As séries já registradas são preservadas localmente.")
            PrimaryButton("Retomar treino",onClick=onResume)
        }else if(session!=null)Pane{
            Text("PRÓXIMA SESSÃO · ${session.day}",color=LightBlue)
            Text(session.title,style=MaterialTheme.typography.titleLarge)
            Subtext("${session.minutes} min · ${session.exercises.size} exercícios · $env")
            session.exercises.forEachIndexed{i,exercise->Text("${i+1}. ${exercise.exercise.name}")}
            session.warning?.let{ Text(it,color=Color(0xFFFFC77A),style=MaterialTheme.typography.bodySmall) }
            PrimaryButton("Começar treino",onClick={onStart(session)})
        } else Pane {
            Text("Semana concluída",color=LightBlue)
            Subtext("Todas as sessões previstas desta semana constam como concluídas. Seu planejamento permanece disponível na aba Plano.")
        }
        Pane {
            Text("Ambiente somente para hoje")
            Subtext("Padrão salvo: ${p.defaultEnvironment}")
            p.environments.forEach{environment->
                Choice(environment,environment==env,onClick={onChangeToday(if(environment==p.defaultEnvironment)null else environment)},Modifier.fillMaxWidth())
            }
            if(temporary!=null)OutlineButton("Restaurar padrão",onClick={onChangeToday(null)})
        }
        Pane {
            Text("Dados de atividade",color=LightBlue)
            Subtext("Sem dispositivos sincronizados. A integração Xiaomi, Bluetooth e Health Connect será ativada em versões posteriores.")
        }
    }
}

@Composable fun PlanScreen(p:UserPrefs,plan:TrainingPlan,onChangeDay:(String,String)->Unit,
    onOverride:(String,String,String)->Unit,onRefresh:()->Unit){
    var selected by remember {mutableStateOf<String?>(null)}
    var showBalance by remember{mutableStateOf(false)}
    var swap by remember {mutableStateOf<String?>(null)}
    val current=plan.sessions.firstOrNull{it.id==selected}
    val days=listOf("Seg","Ter","Qua","Qui","Sex","Sáb","Dom")
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            if(selected!=null||showBalance){TextButton(onClick={selected=null;swap=null;showBalance=false}){Icon(Icons.Default.ArrowBack,null);Text("Semana")}}
            else Title("Plano semanal")
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            when {
                showBalance -> {
                    Title("Equilíbrio muscular")
                    Subtext("Distribuição estimada, sem afirmar equivalência clínica entre exercícios.")
                    val report=BalanceAnalyzer.analyze(plan)
                    Pane {
                        Text("Séries efetivas estimadas (semana)",color=LightBlue)
                        report.effectiveSets.forEach { (muscle,sets) ->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                                Text(muscle);Text("%.1f".format(sets),color=Muted)
                            }
                        }
                    }
                    Pane { report.remarks.forEach { Subtext("• $it") } }
                    Pane {Subtext(plan.explanation);plan.warnings.forEach{Text(it,color=Color(0xFFFFC77A))}}
                }
                current!=null -> {
                    Title(current.title)
                    Pane{
                        Text("Dia da semana")
                        days.chunked(4).forEach{chunk-> Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            chunk.forEach{day->Choice(day,day==current.day,{onChangeDay(current.id,day)},Modifier.weight(1f))}
                            repeat(4-chunk.size){Spacer(Modifier.weight(1f))}
                        }}
                    }
                    current.exercises.forEachIndexed{index,prescription->
                        val ex=prescription.exercise
                        val resolved=ExerciseCatalog.byId(p.overrides["${current.id}:${ex.id}"]?:ex.id)?:ex
                        Pane{
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                                Column(Modifier.weight(1f)) {
                                    Text("${index+1}. ${resolved.name}")
                                    Subtext("${prescription.sets}×${prescription.minReps}–${prescription.maxReps} · ${patternName(ex.pattern)}")
                                }
                                TextButton(onClick={swap=if(swap==ex.id)null else ex.id}){Text("Trocar")}
                            }
                            if(swap==ex.id){
                                val pool=ExerciseCatalog.options(p.defaultEnvironment,p.equipments,p.experience)
                                val available=pool.filter{it.pattern==ex.pattern}
                                if(available.isEmpty())Subtext("Nenhuma alternativa equivalente encontrada neste ambiente.")
                                else available.forEach{candidate->Choice(candidate.name,candidate.id==resolved.id,{onOverride(current.id,ex.id,candidate.id);swap=null},Modifier.fillMaxWidth())}
                            }
                        }
                    }
                    Pane{Subtext(current.explanation);current.warning?.let{Text(it,color=Color(0xFFFFC77A))}}
                }
                else -> {
                    Subtext("Toque em uma sessão para mudar o dia ou trocar exercícios.")
                    plan.sessions.sortedBy{days.indexOf(it.day)}.forEach { s ->
                        Pane {
                            Text("${s.day}  ·  ${s.title}",style=MaterialTheme.typography.titleMedium)
                            Subtext("${s.minutes} min · ${s.exercises.size} exercícios")
                            OutlineButton("Abrir sessão",onClick={selected=s.id})
                        }
                    }
                    if(p.cardioActivities.isNotEmpty())Pane{Text("Cardio complementar");Subtext(p.cardioActivities.joinToString()+" · ${p.cardioMinutes} min · ${p.cardioWhen}")}
                    OutlineButton("Ver equilíbrio muscular",onClick={showBalance=true})
                    OutlineButton("Gerar uma nova proposta",onRefresh)
                }
            }
        }
    }
}
private fun patternName(p:String)=when(p){"push"->"Empurrar";"pull"->"Puxar";"squat"->"Agachar";"hinge"->"Quadril";"lunge"->"Avanço";"verticalPush"->"Empurrar vertical";"core"->"Core";else->"Isolamento"}
