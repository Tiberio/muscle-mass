package br.com.meutreino.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.meutreino.MainViewModel
import br.com.meutreino.domain.TrainingPlanner
import br.com.meutreino.domain.UserPrefs

@Composable fun MeuTreinoApp(vm:MainViewModel,p:UserPrefs){
    var page by remember { mutableStateOf("main") }
    var tab by remember {mutableStateOf("Hoje")}
    var wizardTarget by remember {mutableStateOf<String?>(null)}
    var editFromProfile by remember {mutableStateOf(false)}
    var selectedPlanId by remember {mutableStateOf("T1")}
    val temporary by vm.temporaryEnvironment.collectAsState()
    val active by vm.activeSession.collectAsState()
    val allSets by vm.allSets.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val plan=remember(p){TrainingPlanner.build(p)}
    val currentPlan=plan.sessions.firstOrNull{it.id==selectedPlanId} ?:plan.sessions.firstOrNull()
    val showTabs=p.onboardingCompleted&&p.planAccepted&&page=="main"
    val message by vm.showMessage.collectAsState()
    Scaffold(containerColor=Midnight,
        bottomBar={if(showTabs) NavigationBar(containerColor=Panel) {
            listOf("Hoje","Plano","Histórico","Dispositivos","Perfil").forEachIndexed { n,name ->
                val icon=when(n){0->Icons.Default.Home;1->Icons.Default.DateRange;2->Icons.Default.History;3->Icons.Default.Bluetooth;else->Icons.Default.Person}
                NavigationBarItem(selected=tab==name,onClick={tab=name},icon={Icon(icon,name)},label={Text(name,maxLines=1)},alwaysShowLabel=true)
            }
        }}
    ){ outer ->
        Box(Modifier.fillMaxSize().padding(outer).background(Midnight)) {
            when {
                !p.onboardingCompleted || page=="wizard" -> WizardScreen(p,vm,
                    target=wizardTarget,fromProfile=editFromProfile,
                    onDone={ wizardTarget=null;page="main" },
                    onClose={wizardTarget=null;page="main"})
                !p.planAccepted && page!="workout" -> PlanProposal(plan,p,onAccept={vm.acceptPlan();page="main"},onEdit={
                    wizardTarget="review";editFromProfile=false;page="wizard"
                },onOverride=vm::changeExercise)
                page=="workout" && active!=null -> WorkoutScreen(vm, p,active!!,plan,allSets.filter{it.sessionId==active!!.id},
                    onExit={page="main"},onFinished={page="main";tab="Histórico"})
                else -> when(tab){
                    "Hoje" -> TodayScreen(p,plan,temporary,active,sessions,onChangeToday=vm::changeTodayEnvironment,onStart={ s ->
                        selectedPlanId=s.id
                        val env=temporary ?: p.defaultEnvironment.ifBlank {p.environments.firstOrNull()?:"Casa sem equipamentos"}
                        vm.start(s.id,s.title,env){page="workout"}
                    },onResume={page="workout"})
                    "Plano" -> PlanScreen(p,plan,onChangeDay={id,day->vm.changeAssignment(id,day)},onOverride={id,ex,replacement->vm.changeExercise(id,ex,replacement)},onRefresh={vm.patch{it.copy(assignments=emptyMap(),overrides=emptyMap())}})
                    "Histórico" -> HistoryScreen(sessions,allSets)
                    "Dispositivos" -> DeviceScreen(p)
                    else -> ProfileScreen(p,onEdit={ target ->wizardTarget=target;editFromProfile=true;page="wizard" },onResetPlan={vm.patch{it.copy(assignments=emptyMap(),overrides=emptyMap())}})
                }
            }
            if(message!=null){
                AlertDialog(onDismissRequest={vm.clearMessage()},title={Text("Aviso")},text={Text(message?:"")},confirmButton={TextButton(onClick={vm.clearMessage}){Text("OK")}})
            }
        }
    }
}
