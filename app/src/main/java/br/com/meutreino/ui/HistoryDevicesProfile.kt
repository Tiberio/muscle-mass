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
import br.com.meutreino.data.WorkoutSetEntity
import br.com.meutreino.data.CardioActivityEntity
import br.com.meutreino.domain.ActivityEvents
import java.time.LocalDate
import br.com.meutreino.domain.UserPrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun HistoryScreen(sessions:List<WorkoutSessionEntity>,sets:List<WorkoutSetEntity>,
    cardio:List<CardioActivityEntity>,onDelete:(Long,()->Unit)->Unit,
    onAddCardio:(Int,String,String,()->Unit)->Unit){
    var section by remember{mutableStateOf("Sessões")}
    var deleteConfirm by remember{mutableStateOf(false)}
    var manualMinutes by remember{mutableStateOf("")}
    var manualIntensity by remember{mutableStateOf("MODERADA")}
    var manualModality by remember{mutableStateOf("Caminhada")}
    val summary=ActivityEvents.summary(sessions,sets,cardio,LocalDate.now())
    var selectedSession by remember{mutableStateOf<Long?>(null)}
    var selectedExercise by remember{mutableStateOf<String?>(null)}
    val completed=sessions.filter{it.status=="COMPLETED"}
    val formatter=remember{SimpleDateFormat("dd/MM/yyyy HH:mm",Locale("pt","BR"))}
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Title("Histórico")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("Sessões","Evolução","Saúde").forEach{v->Choice(v,section==v,{section=v;selectedSession=null},Modifier.weight(1f))}
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            when(section){
                "Sessões" -> {
                    if(selectedSession!=null){
                        TextButton(onClick={selectedSession=null}){Text("← Todas as sessões")}
                        val session=sessions.firstOrNull{it.id==selectedSession}
                        Title(session?.title?:"Sessão")
                        sets.filter{it.sessionId==selectedSession}.forEach{set->Pane{
                            Text("${set.exerciseName} · Série ${set.setNumber}")
                            Subtext("${set.repetitions} repetições · ${set.loadKg?.let{v->"$v kg"}?:"peso corporal/elástico"} · RIR ${set.rir?.toString()?:"não informado"}")
                        }}
                        OutlineButton("Excluir esta sessão (inclusive séries)",onClick={deleteConfirm=true})
                    }else{
                        Pane{Text("${completed.size} treinos concluídos",style=MaterialTheme.typography.titleLarge)
                            Subtext("${sets.size} séries salvas no celular · ${completed.sumOf{it.cardioMinutes}} min de cardio informado")}
                        if(sessions.isEmpty())Pane{Subtext("Nenhum treino registrado ainda. Comece uma sessão na aba Hoje.")}
                        sessions.forEach{s->Pane{
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                                Column(Modifier.weight(1f)){
                                    Text(s.title)
                                    Subtext("${formatter.format(Date(s.startedAt))} · ${s.status}")
                                }
                                TextButton(onClick={selectedSession=s.id}){Text("Detalhes")}
                            }
                        }}
                    }
                }
                "Evolução" -> {
                    Subtext("Progressão baseada somente em séries realmente registradas.")
                    val byExercise=sets.groupBy{it.exerciseName}
                    if(byExercise.isEmpty())Pane{Subtext("Registre algumas sessões para começar a visualizar sua progressão.")}
                    byExercise.keys.sorted().forEach{name->Pane{
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            Text(name,modifier=Modifier.weight(1f))
                            TextButton(onClick={selectedExercise=if(selectedExercise==name)null else name}){Text("Detalhes")}
                        }
                        val values=byExercise[name].orEmpty()
                        Subtext("${values.size} séries · última carga: ${values.maxByOrNull{it.recordedAt}?.loadKg?.toString()?:"—"} kg")
                        if(selectedExercise==name){
                            values.sortedBy{it.recordedAt}.takeLast(12).forEach{set->
                                Subtext("${formatter.format(Date(set.recordedAt))}: ${set.repetitions} reps · ${set.loadKg?.let{"$it kg"}?:"sem carga registrada"}")
                            }
                        }
                    }}
                }
                else -> {
                    Pane{
                        Text("Dados de saúde",color=LightBlue)
                        Subtext("Ainda não recebemos dados de Mi Band, balança ou Health Connect.")
                        Subtext("Quando a integração estiver pronta, peso, frequência cardíaca, passos e sono aparecerão com identificação da fonte.")
                    }
                    Pane{Text("PAI Xiaomi");Subtext("Não disponível. Não é possível calcular ou exibir o PAI original sem uma fonte compatível.")}
                    ActivityScoreCard(summary)
                    Pane {
                        Text("Adicionar cardio realizado",color=LightBlue)
                        Subtext("Registre atividades fora das sessões de musculação. Dados reais, não planejados.")
                        OutlinedTextField(value=manualMinutes,onValueChange={manualMinutes=it.filter(Char::isDigit).take(3)},
                            label={Text("Minutos de cardio")},singleLine=true,modifier=Modifier.fillMaxWidth())
                        Text("Intensidade (teste da fala)")
                        listOf("LEVE","MODERADA","VIGOROSA").forEach{mode->
                            Choice(mode.lowercase().replaceFirstChar{it.uppercase()},manualIntensity==mode,{manualIntensity=mode},Modifier.fillMaxWidth())
                        }
                        Text("Modalidade")
                        listOf("Caminhada","Elíptico","Bicicleta","Corrida","Outro").forEach{v->
                            Choice(v,manualModality==v,{manualModality=v},Modifier.fillMaxWidth())
                        }
                        PrimaryButton("Registrar cardio",enabled=manualMinutes.toIntOrNull() in 1..720,onClick={
                            onAddCardio(manualMinutes.toIntOrNull()?:0,manualIntensity,manualModality){manualMinutes=""}
                        })
                    }
                }
            }
        }
    }
    if(deleteConfirm && selectedSession!=null)AlertDialog(onDismissRequest={deleteConfirm=false},
        title={Text("Excluir sessão de treino?")},
        text={Text("Esta sessão e todas as suas séries serão excluídas permanentemente. A ficha do plano será mantida.")},
        confirmButton={TextButton(onClick={
            val id=selectedSession!!
            deleteConfirm=false
            onDelete(id){selectedSession=null}
        }){Text("Excluir")}},
        dismissButton={TextButton(onClick={deleteConfirm=false}){Text("Cancelar")}})
}

@Composable fun DeviceScreen(p:UserPrefs){
    var section by remember{mutableStateOf("Dispositivos")}
    var selectedBrand by remember{mutableStateOf("Xiaomi")}
    var selectedModel by remember{mutableStateOf<String?>(null)}
    val catalog=mapOf(
        "Xiaomi" to listOf("Mi Band 7","Mi Body Composition Scale 2","Smart Band (outras gerações)"),
        "Polar" to listOf("H9","H10","Verity Sense"),
        "Samsung" to listOf("Galaxy Watch"),
        "Garmin" to listOf("Forerunner / Venu"),
        "Amazfit" to listOf("Balance / Bip / Active"),
        "Wahoo" to listOf("Cintas de frequência cardíaca"),
        "Withings" to listOf("Balanças e relógios")
    )
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Title("Dispositivos")
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("Dispositivos","Catálogo","Conexões").forEach{s->Choice(s,s==section,{section=s},Modifier.weight(1f))}
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            when(section){
                "Dispositivos" -> {
                    Subtext("Equipamentos informados no cadastro. Não significa que estejam conectados.")
                    if(p.deviceNames.isEmpty())Pane{Subtext("Nenhum aparelho cadastrado. Adicione no Perfil ou consulte o catálogo.")}
                    p.deviceNames.forEach{name->Pane{
                        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Watch,null,tint=LightBlue);Spacer(Modifier.width(12.dp));Text(name)}
                        Subtext("Estado: cadastrado · acesso aos dados não verificado")
                    }}
                    PrimaryButton("Ver marcas e modelos",onClick={section="Catálogo"})
                }
                "Catálogo" -> {
                    Subtext("Catálogo informativo: não afirmamos compatibilidade até testar cada método.")
                    catalog.keys.chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        row.forEach{brand->Choice(brand,brand==selectedBrand,{selectedBrand=brand;selectedModel=null},Modifier.weight(1f))}
                        repeat(3-row.size){Spacer(Modifier.weight(1f))}
                    }}
                    catalog[selectedBrand].orEmpty().forEach{device->Pane{
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            Text(device,Modifier.weight(1f))
                            TextButton(onClick={selectedModel=if(selectedModel==device)null else device}){Text("Ver")}
                        }
                        if(selectedModel==device){
                            Subtext(when {
                                device=="Mi Body Composition Scale 2"->"Bluetooth direto: candidato prioritário para testes com base no projeto openScale."
                                device=="Mi Band 7"->"Via Zepp Life: investigar sincronização intermediada, sem API pública garantida."
                                device in listOf("H9","H10","Verity Sense")->"Bluetooth direto documentado pela Polar; integração ainda não codificada."
                                else->"Via aplicativo do fabricante/Health Connect, quando disponível. Integração não verificada."
                            })
                        }
                    }}
                }
                else -> {
                    Pane{Text("Bluetooth direto",color=LightBlue)
                        Subtext("Ainda não implementado nesta versão. Não solicitamos permissões de Bluetooth nem simulamos conexões.")}
                    Pane{Text("Health Connect",color=LightBlue)
                        Subtext("Integração futura. Nenhum dado de saúde é lido sem permissão real do Android.")}
                    Pane{Text("Aplicativo informado");Text(p.deviceApp);Subtext("Histórico anterior: ${if(p.importHistory)"deseja importar quando possível" else "não solicitado"}")}
                }
            }
        }
    }
}

@Composable fun ProfileScreen(p:UserPrefs,onEdit:(String)->Unit,onResetPlan:()->Unit){
    var showConfirmation by remember{mutableStateOf(false)}
    val sections=listOf(
        Triple("Perfil físico","${p.age.ifBlank{"—"}} anos · ${p.experience}","basic"),
        Triple("Experiência",p.experience,"experience"),
        Triple("Limitações",p.limitations.ifBlank{"Sem limitações informadas"},"limitations"),
        Triple("Objetivos",p.objective,"goal"),
        Triple("Prioridades musculares",p.priorityMuscles.joinToString().ifBlank{"Sem foco específico"},"muscles"),
        Triple("Ambientes e padrão",p.defaultEnvironment.ifBlank{"Não definido"},"environments"),
        Triple("Equipamentos",p.equipments.joinToString().ifBlank{"Nenhum"},"equipment"),
        Triple("Disponibilidade",p.weekdays.joinToString().ifBlank{"Rotina flexível"},"days"),
        Triple("Tempo de musculação","${p.minutesPerSession} min (cardio à parte)","duration"),
        Triple("Cardio",p.cardioActivities.joinToString().ifBlank{"Não selecionado"},"cardio"),
        Triple("Duração do cardio","${p.cardioMinutes} min","cardioDuration"),
        Triple("Frequência do cardio",if(p.cardioWeeklyDays==7)"Todos os dias" else if(p.cardioWeeklyDays==0)"Nos dias da musculação" else "${p.cardioWeeklyDays} dias/semana","cardioFrequency"),
        Triple("Intensidade do cardio",p.cardioIntensity,"cardioIntensity"),
        Triple("Dispositivos",p.deviceNames.joinToString().ifBlank{"Nenhum"},"devices")
    )
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Title("Meu Perfil")
        Subtext("Altere apenas o que desejar. O histórico de treinos já realizados não será apagado.")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            sections.forEach{(label,summary,route)->Pane{
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)) {Text(label);Subtext(summary)}
                    TextButton(onClick={onEdit(route)}){Icon(Icons.Default.Edit,null);Text("Editar")}
                }
            }}
            Pane{
                Text("Planejamento")
                Subtext("Mudanças importantes devem gerar um novo plano para revisão, sem modificar o histórico passado.")
                OutlineButton("Rever plano de treino",onClick={showConfirmation=true})
            }
        }
    }
    if(showConfirmation)AlertDialog(onDismissRequest={showConfirmation=false},title={Text("Rever o plano?")},
        text={Text("O aplicativo montará novamente a proposta para sua aprovação. Seus registros de treinos serão preservados.")},
        confirmButton={TextButton(onClick={showConfirmation=false;onResetPlan()}){Text("Rever plano")}},
        dismissButton={TextButton(onClick={showConfirmation=false}){Text("Cancelar")}})
}
