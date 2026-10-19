package br.com.meutreino.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.meutreino.MainViewModel
import br.com.meutreino.data.WorkoutSessionEntity
import br.com.meutreino.data.WorkoutSetEntity
import br.com.meutreino.domain.*
import kotlinx.coroutines.delay

@Composable fun WorkoutScreen(vm:MainViewModel,p:UserPrefs,active:WorkoutSessionEntity,
    originalPlan:TrainingPlan,logged:List<WorkoutSetEntity>,onExit:()->Unit,onFinished:()->Unit){
    val envPlan=remember(p,active.environment){TrainingPlanner.build(p.copy(defaultEnvironment=active.environment))}
    val planned=envPlan.sessions.firstOrNull{it.id==active.planId}?.exercises.orEmpty()
    var index by remember(active.id){mutableIntStateOf(0)}
    var resting by remember(active.id){mutableStateOf(false)}
    var seconds by remember(active.id){mutableIntStateOf(90)}
    var reps by remember(active.id){mutableStateOf("10")}
    var load by remember(active.id){mutableStateOf("")}
    var rir by remember(active.id){mutableStateOf<Int?>(null)}
    var showVideoHelp by remember{mutableStateOf(false)}
    var cardioMinutes by remember(active.id){mutableStateOf("0")}
    var cardioPage by remember(active.id){mutableStateOf(false)}
    val context=LocalContext.current
    val total=planned.sumOf{it.sets}
    // Retoma de maneira determinística depois de fechar o aplicativo.
    LaunchedEffect(active.id,logged.size){
        if(!resting && !cardioPage){
            val incomplete=planned.indexOfFirst{pres -> logged.count{it.exerciseId==pres.exercise.id}<pres.sets}
            if(incomplete>=0)index=incomplete else if(planned.isNotEmpty()&&logged.size>=total) cardioPage=true
        }
    }
    LaunchedEffect(resting){ while(resting&&seconds>0){delay(1000);seconds--} }
    val pres=planned.getOrNull(index)
    val base=pres?.exercise
    val ex=base?.let{ExerciseCatalog.byId(p.overrides["${active.planId}:${it.id}"]?:it.id)?:it}
    val doneForExercise=base?.let{b->logged.count{it.exerciseId==b.id}}?:0
    val hasLoad=ex?.required?.any{it=="Halteres"||it=="Máquinas"}==true
    fun advance(){
        resting=false;seconds=90;rir=null
        if(pres!=null && doneForExercise+1>=pres.sets){
            if(index>=planned.lastIndex)cardioPage=true else index++
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            TextButton(onClick=onExit){Icon(Icons.Default.ArrowBack,null);Text("Sair")}
            Text("Execução do Treino",color=LightBlue)
        }
        LinearProgressIndicator(progress={if(total>0)logged.size.toFloat()/total else 0f},modifier=Modifier.fillMaxWidth(),color=Blue,trackColor=PanelLight)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            when {
                cardioPage -> {
                    Title("Finalizar sessão")
                    Pane {
                        Text("${logged.size} séries registradas")
                        Subtext("Se realizou cardio, informe a duração em minutos; caso contrário, use zero.")
                        OutlinedTextField(value=cardioMinutes,onValueChange={cardioMinutes=it.filter(Char::isDigit).take(3)},label={Text("Cardio realizado (min)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
                        PrimaryButton("Salvar e concluir",enabled=cardioMinutes.toIntOrNull() in 0..180){
                            vm.finish(active.id,cardioMinutes.toIntOrNull()?:0,onFinished)
                        }
                    }
                }
                ex==null -> {Title("Sem exercícios compatíveis");Subtext("Revise o ambiente e os equipamentos. Você poderá encerrar esta sessão sem séries.")
                    PrimaryButton("Concluir sessão",onClick={vm.finish(active.id,0,onFinished)})}
                resting -> {
                    Title("Intervalo entre séries")
                    Pane {
                        Text("Série registrada",color=LightBlue)
                        Text("%02d:%02d".format(seconds/60,seconds%60),style=MaterialTheme.typography.displayLarge)
                        Subtext("Contagem ilustrativa. É possível avançar antes do fim ou aumentar a pausa.")
                        OutlineButton("+ 30 segundos",onClick={seconds+=30})
                        PrimaryButton(if(doneForExercise>=pres!!.sets)"Próximo exercício" else "Próxima série",onClick={
                            resting=false;seconds=90;rir=null
                            if(doneForExercise>=pres.sets){if(index==planned.lastIndex)cardioPage=true else index++}
                        })
                    }
                }
                else -> {
                    Text("Exercício ${index+1}/${planned.size} · Série ${doneForExercise+1}/${pres!!.sets}",color=LightBlue)
                    Title(ex.name)
                    Pane {
                        Text("Demonstração",color=LightBlue)
                        Subtext(ex.advice)
                        if(ex.youtubeVideoId!=null){
                            PrimaryButton("▶ Ver vídeo no YouTube",onClick={
                                val url="https://www.youtube.com/watch?v=${ex.youtubeVideoId}"
                                context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))
                            })
                        }else{
                            OutlineButton("Vídeo ainda não cadastrado",onClick={showVideoHelp=!showVideoHelp})
                            if(showVideoHelp)Subtext("Só adicionaremos vídeos específicos que forem previamente verificados. O registro do treino funciona sem vídeo.")
                        }
                    }
                    Pane {
                        Text("Registre a série realizada",style=MaterialTheme.typography.titleMedium)
                        Subtext("Meta: ${pres.minReps}–${pres.maxReps} repetições. Ajuste de acordo com a execução real.")
                        OutlinedTextField(value=reps,onValueChange={reps=it.filter(Char::isDigit).take(3)},
                            label={Text("Repetições")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
                        if(hasLoad)OutlinedTextField(value=load,onValueChange={load=it.replace(',','.').filter{ch->ch.isDigit()||ch=='.'}.take(7)},
                            label={Text(if("Halteres" in ex.required)"Carga (kg por halter)" else "Carga indicada (kg)")},
                            singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
                        Text("Esforço (RIR), opcional",color=Muted)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            listOf<Int?>(null,0,1,2,3).forEach { v ->
                                Choice(v?.toString()?:"—",rir==v,{rir=v},Modifier.weight(1f))
                            }
                        }
                        val repetitions=reps.toIntOrNull()
                        val numericLoad=load.toDoubleOrNull()
                        PrimaryButton("Concluir série",enabled=repetitions in 1..500 && (!hasLoad||numericLoad!=null&&numericLoad in 0.0..2000.0),onClick={
                            vm.recordSet(active.id,base!!.id,ex.name,doneForExercise+1,repetitions?:0,if(hasLoad)numericLoad else null,rir){
                                resting=true;seconds=90
                            }
                        })
                    }
                    Subtext("Cada série é salva imediatamente no banco local, inclusive se você sair do aplicativo depois.")
                }
            }
        }
    }
}
