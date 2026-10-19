package br.com.meutreino.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import br.com.meutreino.MainViewModel
import br.com.meutreino.domain.UserPrefs

private data class WizardItem(val id:String,val phase:Int,val title:String)
private val steps=listOf(
    WizardItem("welcome",1,"Boas-vindas"),
    WizardItem("basic",2,"Perfil físico"),WizardItem("experience",2,"Experiência"),WizardItem("limitations",2,"Cuidados e limitações"),
    WizardItem("goal",3,"Objetivos"),WizardItem("muscles",3,"Prioridades musculares"),
    WizardItem("environments",4,"Locais de treino"),WizardItem("equipment",4,"Equipamentos"),
    WizardItem("days",5,"Disponibilidade"),WizardItem("duration",5,"Tempo por sessão"),WizardItem("time",5,"Horário preferido"),
    WizardItem("cardio",6,"Atividades aeróbicas"),WizardItem("cardioWhen",6,"Organização do cardio"),WizardItem("cardioFrequency",6,"Dias de cardio"),WizardItem("cardioDuration",6,"Duração do cardio"),WizardItem("cardioIntensity",6,"Intensidade"),
    WizardItem("devices",7,"Seus dispositivos"),WizardItem("deviceApp",7,"Aplicativos de saúde"),WizardItem("dataAccess",7,"Dados e histórico"),
    WizardItem("review",8,"Revisão final")
)
private val weekdays=listOf("Seg","Ter","Qua","Qui","Sex","Sáb","Dom")

@Composable private fun StableInput(value:String,label:String,onValue:(String)->Unit,
                                    numeric:Boolean=false,decimal:Boolean=false,modifier:Modifier=Modifier){
    // O cursor pertence ao estado local de edição, não à gravação assíncrona no DataStore.
    var field by remember(label){ mutableStateOf(TextFieldValue(value)) }
    var focused by remember(label) { mutableStateOf(false) }
    // Importação externa pode mudar o valor; não sobrescrever digitação/cursor em andamento.
    LaunchedEffect(value,focused) {
        if(!focused && value != field.text)
            field=TextFieldValue(value,selection=TextRange(value.length))
    }
    OutlinedTextField(value=field,onValueChange={ next ->
        val cleaned=if(!numeric)next.text.take(500) else if(decimal)
            next.text.replace(',','.').filter { it.isDigit()||it=='.' }.take(7)
        else next.text.filter(Char::isDigit).take(3)
        val cursor=next.selection.start.coerceIn(0,cleaned.length)
        field=next.copy(text=cleaned,selection=TextRange(cursor))
        onValue(cleaned)
    }, label={Text(label)},keyboardOptions=KeyboardOptions(keyboardType=when {
        decimal->KeyboardType.Decimal;numeric->KeyboardType.Number;else->KeyboardType.Text
    }), modifier=modifier.fillMaxWidth().onFocusChanged { focused=it.isFocused },
        singleLine=!(!numeric&&label.contains("evitar")))
}
@Composable private fun InputNumber(value:String,label:String,onValue:(String)->Unit,modifier:Modifier=Modifier){
    StableInput(value,label,onValue,numeric=true,decimal=label.startsWith("Peso"),modifier=modifier)
}
@Composable private fun ToggleList(title:String,items:List<String>,chosen:List<String>,onToggle:(String)->Unit,max:Int=100){
    Text(title,color=Muted)
    items.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            row.forEach { item ->
                Choice(item,item in chosen,onClick={if(item in chosen||chosen.size<max) onToggle(item)},Modifier.weight(1f))
            }
            if(row.size==1)Spacer(Modifier.weight(1f))
        }
    }
}
@Composable private fun SingleList(items:List<String>,selected:String,onSelect:(String)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        items.forEach { Choice(it,it==selected,{onSelect(it)},Modifier.fillMaxWidth()) }
    }
}

@Composable fun WizardScreen(p:UserPrefs,vm:MainViewModel,target:String?,fromProfile:Boolean,onDone:()->Unit,onClose:()->Unit){
    var idx by remember(target,fromProfile){mutableIntStateOf(steps.indexOfFirst{it.id==target}.takeIf{it>=0}?:0)}
    var editingFromReview by remember {mutableStateOf(false)}
    val savedWeights by vm.weightHistory.collectAsState()
    val focusManager= LocalFocusManager.current
    val item=steps[idx]
    val inReview=item.id=="review"
    val phaseSteps=steps.filter{it.phase==item.phase}
    val part=phaseSteps.indexOfFirst{it.id==item.id}+1
    val canNext=when(item.id){
        "basic" -> p.age.toIntOrNull()?.let{it in 12..105}==true
        "environments" -> p.environments.isNotEmpty()
        "days" -> if(p.scheduleMode=="Dias definidos")p.weekdays.isNotEmpty() else p.weeklyCount in 1..7
        "duration" -> p.minutesPerSession in 10..240
        "cardioDuration" -> p.cardioActivities.isEmpty() || p.cardioMinutes in 1..180
        else -> true
    }
    val scroll=rememberScrollState()
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            Text("Configuração inicial",color=LightBlue)
            Text("${part}/${phaseSteps.size}  ·  Etapa ${item.phase}/8",color=Muted)
        }
        LinearProgressIndicator(progress={part.toFloat()/phaseSteps.size},modifier=Modifier.fillMaxWidth().height(5.dp),color=Blue,trackColor=PanelLight)
        Title(if(inReview)"Revise suas preferências" else item.title)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll),verticalArrangement=Arrangement.spacedBy(14.dp)){
            when(item.id){
                "welcome" -> {
                    Pane {Text("Seu treino, seu ritmo.",style=MaterialTheme.typography.titleLarge)
                        Subtext("Planos de força e cardio, exercícios demonstrados em vídeo, registro de séries e integração de saúde quando disponível.")}
                    Subtext("Os dados ficam no celular. Você pode alterar o plano e os equipamentos posteriormente.")
                }
                "basic" -> Pane {
                    Subtext("Idade é necessária para personalizar as recomendações; altura e peso são opcionais.")
                    InputNumber(p.age,"Idade (anos)",{vm.edit{a->a.copy(age=it)}})
                    InputNumber(p.heightCm,"Altura (cm, opcional)",{vm.edit{a->a.copy(heightCm=it)}})
                    InputNumber(p.weightKg,"Peso (kg, opcional)",{vm.edit{a->a.copy(weightKg=it)}})
                    OutlinedButton(onClick={focusManager.clearFocus();vm.importLatestSavedWeight()},enabled=savedWeights.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Text("Importar da balança")}
                    Subtext(if(savedWeights.isEmpty()) "Primeiro salve uma pesagem na aba Dispositivos; depois será possível importá-la aqui."
                        else "Último peso salvo: ${savedWeights.first().kilograms} kg. Toque para copiar ao Perfil.")
                }
                "experience" -> Pane {
                    Text("Qual sua experiência com musculação?")
                    SingleList(listOf("Iniciante","Intermediário","Avançado"),p.experience){v->vm.edit{it.copy(experience=v)}}
                    Text("Prática recente",color=Muted)
                    SingleList(listOf("Retornando / começando","Treinando regularmente"),p.regularity){v->vm.edit{it.copy(regularity=v)}}
                }
                "limitations" -> Pane {
                    Subtext("Existem movimentos que precisam ser evitados ou adaptados? Não é necessário informar diagnósticos.")
                    StableInput(p.limitations,"Movimentos a evitar (opcional)",{v->vm.edit{it.copy(limitations=v)}})
                    Subtext("Se houver dor ou restrição importante, consulte um profissional habilitado antes de seguir a proposta automática.")
                }
                "goal" -> Pane {
                    Text("O que você deseja alcançar?")
                    SingleList(listOf("Saúde e bem-estar","Hipertrofia","Força","Condicionamento","Redução de gordura"),p.objective){v->vm.edit{it.copy(objective=v)}}
                }
                "muscles" -> Pane {
                    Subtext("Selecione até três regiões. Não selecionar nenhuma é válido.")
                    ToggleList("Regiões prioritárias",listOf("Peito","Costas","Ombros","Braços","Abdômen","Pernas","Glúteos"),p.priorityMuscles,{v->vm.edit{it.copy(priorityMuscles=toggle(it.priorityMuscles,v))}},3)
                    Subtext("O planejamento inclui também regiões não prioritárias para favorecer o equilíbrio muscular.")
                }
                "environments" -> Pane {
                    Subtext("Você pode cadastrar vários locais. O principal fica salvo, mas pode ser trocado somente para um dia.")
                    ToggleList("Onde treinar?",listOf("Casa sem equipamentos","Casa equipada","Academia"),p.environments,{v->vm.edit{
                        val e=toggle(it.environments,v)
                        it.copy(environments=e,defaultEnvironment=if(it.defaultEnvironment in e)it.defaultEnvironment else e.firstOrNull().orEmpty())
                    }})
                    if(p.environments.isNotEmpty()){
                        Text("Ambiente padrão",color=Muted)
                        SingleList(p.environments,p.defaultEnvironment){v->vm.edit{it.copy(defaultEnvironment=v)}}
                    }
                }
                "equipment" -> Pane {
                    if("Casa equipada" in p.environments){
                        ToggleList("Equipamentos disponíveis em casa",listOf("Halteres","Banco","Barra fixa","Elásticos","Barra e anilhas","Kettlebell","Colchonete","Elíptico","Esteira","Bicicleta"),p.equipments,{v->vm.edit{it.copy(equipments=toggle(it.equipments,v))}})
                    }else Subtext("Sem equipamentos domésticos cadastrados. Você pode alterá-los depois no Perfil.")
                }
                "days" -> Pane {
                    Text("Como funciona sua rotina?")
                    SingleList(listOf("Dias definidos","Rotina flexível"),p.scheduleMode){v->vm.edit{it.copy(scheduleMode=v)}}
                    if(p.scheduleMode=="Dias definidos")ToggleList("Selecione os dias",weekdays,p.weekdays,{v->vm.edit{it.copy(weekdays=toggle(it.weekdays,v))}})
                    else {
                        Text("Sessões por semana: ${p.weeklyCount}")
                        Slider(value=p.weeklyCount.toFloat(),onValueChange={v->vm.edit{it.copy(weeklyCount=v.toInt().coerceIn(1,7))}},valueRange=1f..7f,steps=5)
                    }
                }
                "duration" -> Pane {
                    Text("Tempo disponível para musculação")
                    ToggleDuration(p.minutesPerSession,{v->vm.edit{it.copy(minutesPerSession=v)}})
                    Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=p.flexibleDuration,onCheckedChange={v->vm.edit{it.copy(flexibleDuration=v)}});Text("Meu tempo varia entre os dias")}
                    Subtext("Duração somente da musculação, incluindo séries, aquecimento e descansos. O cardio é adicional e tem tempo independente.")
                }
                "time" -> Pane {
                    Text("Horário preferido")
                    SingleList(listOf("Manhã","Tarde","Noite","Sem preferência"),p.preferredTime){v->vm.edit{it.copy(preferredTime=v)}}
                }
                "cardio" -> Pane {
                    Subtext("Cardio é opcional. Você poderá mudar essas escolhas depois.")
                    ToggleList("Atividades favoritas",listOf("Caminhada","Corrida","Bicicleta","Elíptico","Corda","Outra"),p.cardioActivities,{v->vm.edit{it.copy(cardioActivities=toggle(it.cardioActivities,v))}})
                    Choice("Não incluir cardio por enquanto",p.cardioActivities.isEmpty(),{vm.edit{it.copy(cardioActivities=emptyList())}},Modifier.fillMaxWidth())
                }
                "cardioWhen" -> Pane {
                    if(p.cardioActivities.isEmpty())Subtext("Você não selecionou cardio; esta etapa pode ser ignorada.")
                    else SingleList(listOf("Após musculação","Dias separados","Misto"),p.cardioWhen){v->vm.edit{it.copy(cardioWhen=v)}}
                }
                "cardioFrequency" -> Pane {
                    Subtext("Com que frequência deseja fazer cardio? A opção diária inclui dias sem musculação.")
                    if(p.cardioActivities.isEmpty())Subtext("Sem cardio selecionado.")
                    else SingleList(listOf("Nos dias da musculação","3 dias por semana","5 dias por semana","Todos os dias (7)"),
                        when(p.cardioWeeklyDays){3->"3 dias por semana";5->"5 dias por semana";7->"Todos os dias (7)";else->"Nos dias da musculação"}){v->
                        vm.edit{it.copy(cardioWeeklyDays=when(v){"3 dias por semana"->3;"5 dias por semana"->5;"Todos os dias (7)"->7;else->0})}
                    }
                }
                "cardioDuration" -> Pane {
                    if(p.cardioActivities.isEmpty())Subtext("Nenhuma duração de cardio necessária.")
                    else ToggleDuration(p.cardioMinutes,{v->vm.edit{it.copy(cardioMinutes=v)}},listOf(10,15,20,30))
                }
                "cardioIntensity" -> Pane {
                    if(p.cardioActivities.isEmpty())Subtext("Sem cardio por enquanto.")
                    else SingleList(listOf("Leve","Moderada","Vigorosa","Adaptativa"),p.cardioIntensity){v->vm.edit{it.copy(cardioIntensity=v)}}
                }
                "devices" -> Pane {
                    Subtext("Dispositivos apenas cadastrados. Nesta versão ainda não há conexão automática.")
                    ToggleList("Quais aparelhos você utiliza?",listOf("Mi Band 7","Mi Body Composition Scale 2","Outro dispositivo"),p.deviceNames,{v->vm.edit{it.copy(deviceNames=toggle(it.deviceNames,v))}})
                }
                "deviceApp" -> Pane {
                    SingleList(listOf("Zepp Life","Mi Fitness","Outro / não sei"),p.deviceApp){v->vm.edit{it.copy(deviceApp=v)}}
                    Subtext("Você poderá escolher marca e modelo, Bluetooth ou Health Connect na aba Dispositivos. Health Connect e leitura básica da balança estão disponíveis; outras integrações seguem em desenvolvimento.")
                }
                "dataAccess" -> Pane {
                    Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=p.importHistory,onCheckedChange={v->vm.edit{it.copy(importHistory=v)}});Text("Tenho interesse em importar histórico antigo")}
                    Subtext("Seus dados de saúde exigirão permissões específicas do Android. Nenhuma permissão é solicitada nesta versão.")
                }
                "review" -> {
                    Subtext("Esta é a única tela longa da configuração. Verifique tudo. Cada botão Editar abre diretamente a pergunta e retorna para cá depois de salvar.")
                    ReviewRow("Perfil físico","${p.age.ifBlank{"—"}} anos · ${p.heightCm.ifBlank{"—"}} cm · ${p.weightKg.ifBlank{"—"}} kg","basic",{idx=steps.indexOfFirst{it.id=="basic"};editingFromReview=true})
                    ReviewRow("Experiência e rotina","${p.experience} · ${p.regularity}","experience",{idx=steps.indexOfFirst{it.id=="experience"};editingFromReview=true})
                    ReviewRow("Limitações e cuidados",p.limitations.ifBlank{"Não informadas"},"limitations",{idx=steps.indexOfFirst{it.id=="limitations"};editingFromReview=true})
                    ReviewRow("Objetivo",p.objective,"goal",{idx=steps.indexOfFirst{it.id=="goal"};editingFromReview=true})
                    ReviewRow("Prioridades musculares",p.priorityMuscles.joinToString().ifBlank{"Sem prioridade"},"muscles",{idx=steps.indexOfFirst{it.id=="muscles"};editingFromReview=true})
                    ReviewRow("Locais e padrão","${p.environments.joinToString().ifBlank{"—"}} · Padrão: ${p.defaultEnvironment}","environments",{idx=steps.indexOfFirst{it.id=="environments"};editingFromReview=true})
                    ReviewRow("Equipamentos",p.equipments.joinToString().ifBlank{"Nenhum cadastrado"},"equipment",{idx=steps.indexOfFirst{it.id=="equipment"};editingFromReview=true})
                    ReviewRow("Dias disponíveis",if(p.scheduleMode=="Dias definidos")p.weekdays.joinToString() else "${p.weeklyCount} sessões flexíveis","days",{idx=steps.indexOfFirst{it.id=="days"};editingFromReview=true})
                    ReviewRow("Musculação","${p.minutesPerSession} minutos por sessão de força","duration",{idx=steps.indexOfFirst{it.id=="duration"};editingFromReview=true})
                    ReviewRow("Horário",p.preferredTime,"time",{idx=steps.indexOfFirst{it.id=="time"};editingFromReview=true})
                    ReviewRow("Cardio",p.cardioActivities.joinToString().ifBlank{"Sem cardio"},"cardio",{idx=steps.indexOfFirst{it.id=="cardio"};editingFromReview=true})
                    ReviewRow("Frequência do cardio",if(p.cardioWeeklyDays==7)"Todos os dias" else if(p.cardioWeeklyDays==0)"Nos dias da musculação" else "${p.cardioWeeklyDays} dias por semana","cardioFrequency",{idx=steps.indexOfFirst{it.id=="cardioFrequency"};editingFromReview=true})
                    ReviewRow("Distribuição",p.cardioWhen,"cardioWhen",{idx=steps.indexOfFirst{it.id=="cardioWhen"};editingFromReview=true})
                    ReviewRow("Duração do cardio","${p.cardioMinutes} min","cardioDuration",{idx=steps.indexOfFirst{it.id=="cardioDuration"};editingFromReview=true})
                    ReviewRow("Intensidade do cardio",p.cardioIntensity,"cardioIntensity",{idx=steps.indexOfFirst{it.id=="cardioIntensity"};editingFromReview=true})
                    ReviewRow("Dispositivos",p.deviceNames.joinToString().ifBlank{"Não informados"},"devices",{idx=steps.indexOfFirst{it.id=="devices"};editingFromReview=true})
                    ReviewRow("Aplicativo de origem",p.deviceApp,"deviceApp",{idx=steps.indexOfFirst{it.id=="deviceApp"};editingFromReview=true})
                    ReviewRow("Importar histórico",if(p.importHistory)"Quando possível" else "Não solicitar", "dataAccess",{idx=steps.indexOfFirst{it.id=="dataAccess"};editingFromReview=true})
                }
            }
        }
        if(inReview){
            PrimaryButton("Confirmar e gerar meu plano",enabled=p.environments.isNotEmpty()&&(p.age.toIntOrNull() ?: 0) in 12..105){vm.finishOnboarding();onDone()}
        }else{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                OutlinedButton(onClick={if(fromProfile){onClose()}else if(editingFromReview){idx=steps.lastIndex;editingFromReview=false}else{idx=(idx-1).coerceAtLeast(0)}},modifier=Modifier.weight(1f)) {Text("Voltar")}
                Button(onClick={
                    if(fromProfile){vm.patch{it};onDone()}
                    else if(editingFromReview){idx=steps.lastIndex;editingFromReview=false}
                    else idx=(idx+1).coerceAtMost(steps.lastIndex)
                },enabled=canNext,modifier=Modifier.weight(1f).heightIn(min=48.dp)){
                    Text(if(fromProfile||editingFromReview)"Salvar" else "Próximo")
                }
            }
        }
    }
}
private fun toggle(items:List<String>,v:String)=if(v in items)items-v else items+v

@Composable private fun ReviewRow(label:String,value:String,id:String,onEdit:()->Unit){
    Pane {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Text(label,color=Color.White,modifier=Modifier.weight(1f))
            TextButton(onClick=onEdit){Text("Editar")}
        }
        Subtext(value)
    }
}

@Composable private fun ToggleDuration(value:Int,onValue:(Int)->Unit,options:List<Int> = listOf(20,30,45,60)){
    var other by remember(value,options){mutableStateOf(value !in options)}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
        options.forEach { v -> Choice("${v} min",!other&&value==v,{other=false;onValue(v)},Modifier.weight(1f)) }
    }
    Choice("Outro tempo",other,{other=true},Modifier.fillMaxWidth())
    if(other){
        var temp by remember(value !in options) {mutableStateOf(value.toString())}
        OutlinedTextField(value=temp,onValueChange={ s->temp=s.filter(Char::isDigit).take(3);temp.toIntOrNull()?.let(onValue)},
            label={Text("Duração em minutos")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth())
    }
}
