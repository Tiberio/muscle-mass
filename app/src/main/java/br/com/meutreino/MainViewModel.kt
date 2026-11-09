package br.com.meutreino

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import br.com.meutreino.data.SettingsStore
import br.com.meutreino.data.CardioActivityEntity
import br.com.meutreino.data.MIGRATION_1_2
import br.com.meutreino.data.MIGRATION_2_3
import br.com.meutreino.data.MIGRATION_4_5
import br.com.meutreino.data.MIGRATION_3_4
import br.com.meutreino.data.RecoveryCheckInEntity
import br.com.meutreino.data.BodyMeasurementGoalEntity
import br.com.meutreino.data.PlanReviewEventEntity
import br.com.meutreino.data.BodyMeasurementEntity
import br.com.meutreino.data.PlanVersionEntity
import br.com.meutreino.data.WeightMeasurementEntity
import br.com.meutreino.devices.HealthConnectGateway
import br.com.meutreino.devices.HealthSnapshot
import br.com.meutreino.devices.MiScaleScanner
import br.com.meutreino.devices.MiBand7Probe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import br.com.meutreino.data.WorkoutDatabase
import br.com.meutreino.data.WorkoutSessionEntity
import br.com.meutreino.data.WorkoutSetEntity
import br.com.meutreino.domain.UserPrefs
import br.com.meutreino.domain.PlanVersionSnapshot
import br.com.meutreino.domain.ReviewSnapshotBuilder
import br.com.meutreino.domain.ExerciseCatalog
import br.com.meutreino.domain.TrainingPlanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainViewModel(application: Application): AndroidViewModel(application) {
    private val store = SettingsStore(application)
    private val db = Room.databaseBuilder(application,WorkoutDatabase::class.java,"workouts_v1.db").addMigrations(MIGRATION_1_2,MIGRATION_2_3,MIGRATION_3_4,MIGRATION_4_5).build()
    private val dao = db.dao()
    private val health = HealthConnectGateway(application)
    val scaleScanner = MiScaleScanner(application)
    val bandProbe = MiBand7Probe(application)
    val healthSnapshot = MutableStateFlow<HealthSnapshot?>(null)
    val healthMessage = MutableStateFlow("Acesso opcional. Você controla as permissões no Android.")
    val healthBusy = MutableStateFlow(false)
    val weightHistory = dao.observeWeights().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val bodyMeasurements = dao.observeBodyMeasurements().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val planVersions = dao.observePlanVersions().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val recoveryCheckIns = dao.observeRecoveryCheckIns().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val bodyGoals = dao.observeBodyGoals().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val reviewEvents = dao.observeReviewEvents().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    private val writeLock = Mutex()
    val prefs: StateFlow<UserPrefs?> = store.data.stateIn(viewModelScope,SharingStarted.Eagerly,null)
    val sessions = dao.observeSessions().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val allSets = dao.observeAllSets().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val cardioActivities = dao.observeCardio().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val activeSession=MutableStateFlow<WorkoutSessionEntity?>(null)
    val temporaryEnvironment=MutableStateFlow<String?>(null)
    val showMessage=MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            activeSession.value=dao.activeSession()
            // Criar a primeira versão somente a partir de agora para perfis pré-v0.5.0.
            val existingPrefs = store.data.first()
            if (existingPrefs.planAccepted) ensurePlanVersion(existingPrefs, existingPrefs.defaultEnvironment, "Ficha existente ao atualizar")
        }
    }

    private suspend fun ensurePlanVersion(p: UserPrefs, environment: String, reason: String): Long {
        val env = environment.ifBlank { p.environments.firstOrNull() ?: "Casa sem equipamentos" }
        val content = PlanVersionSnapshot.content(p, env)
        val fingerprint = PlanVersionSnapshot.fingerprint(content)
        dao.findPlanVersion(fingerprint)?.let { return it.id }
        val inserted = dao.insertPlanVersion(PlanVersionEntity(
            createdAt=System.currentTimeMillis(), fingerprint=fingerprint, content=content, reason=reason
        ))
        return if(inserted > 0L) inserted else dao.findPlanVersion(fingerprint)?.id
            ?: error("Não foi possível registrar a versão da ficha")
    }
    private fun current(): UserPrefs = prefs.value ?: UserPrefs()
    fun edit(change:(UserPrefs)->UserPrefs) {
        viewModelScope.launch { // serializar alterações evita sobrescrever taps consecutivos
            writeLock.withLock {
                val before = store.data.first()
                val updated = change(before)
                store.save(updated)
                if (updated.planAccepted) ensurePlanVersion(updated, updated.defaultEnvironment, "Plano aprovado ou editado")
            }
        }
    }
    fun patch(change:(UserPrefs)->UserPrefs) = edit { change(it).copy(planAccepted=false) }
    fun finishOnboarding() = edit { it.copy(onboardingCompleted=true,planAccepted=false) }
    fun acceptPlan() = edit { it.copy(planAccepted=true) }
    fun changeTodayEnvironment(value:String?) { temporaryEnvironment.value=value }
    fun changeAssignment(planId:String,day:String) = edit { prefs ->
        val planned = br.com.meutreino.domain.TrainingPlanner.build(prefs).sessions
        val currentDay = planned.firstOrNull { it.id == planId }?.day ?: day
        val other = planned.firstOrNull { it.id != planId && it.day == day }?.id
        val changes = prefs.assignments.toMutableMap()
        changes[planId] = day
        if (other != null) changes[other] = currentDay
        prefs.copy(assignments=changes)
    }
    fun changeExercise(planId:String,originalId:String,newId:String) = edit {
        it.copy(overrides=it.overrides+("$planId:$originalId" to newId))
    }
    fun start(planId:String,title:String,environment:String,onStarted:(Long)->Unit) {
        viewModelScope.launch {
            val existing=dao.activeSession()
            if(existing!=null) { activeSession.value=existing;onStarted(existing.id);return@launch }
            val activePrefs = store.data.first()
            val version = ensurePlanVersion(activePrefs, environment, "Ficha utilizada em sessão")
            val id=dao.insertSession(WorkoutSessionEntity(
                planId=planId,planVersionId=version,title=title,environment=environment,
                startedAt=System.currentTimeMillis()
            ))
            activeSession.value=dao.activeSession()
            onStarted(id)
        }
    }
    fun recordSet(sessionId:Long,exerciseId:String,exerciseName:String,number:Int,reps:Int,load:Double?,rir:Int?,onSaved:()->Unit) {
        if(reps<1||reps>500||load!=null&&(load<0||load>2000)) return
        viewModelScope.launch {
            dao.insertSet(WorkoutSetEntity(sessionId=sessionId,exerciseId=exerciseId,exerciseName=exerciseName,setNumber=number,repetitions=reps,loadKg=load,rir=rir,recordedAt=System.currentTimeMillis()))
            onSaved()
        }
    }
    fun finish(sessionId:Long,cardio:Int,intensity:String,onDone:()->Unit) {
        viewModelScope.launch {
            dao.finishSession(sessionId,System.currentTimeMillis(),cardio,intensity)
            activeSession.value=null
            temporaryEnvironment.value=null
            onDone()
        }
    }
    fun deleteSession(id:Long,onDone:()->Unit) {
        viewModelScope.launch {
            dao.deleteSession(id) // FK CASCADE remove as séries da sessão
            if(activeSession.value?.id==id)activeSession.value=null
            onDone()
        }
    }
    fun addManualCardio(minutes:Int,intensity:String,modality:String,onSaved:()->Unit) {
        if(minutes !in 1..720 || intensity !in listOf("LEVE","MODERADA","VIGOROSA")) return
        viewModelScope.launch {
            dao.insertCardio(CardioActivityEntity(UUID.randomUUID().toString(),System.currentTimeMillis(),minutes,intensity,modality))
            onSaved()
        }
    }
    fun addBodyMeasurement(site: String, centimeters: Double, onSaved: ()->Unit) {
        if (site !in br.com.meutreino.domain.BodyMeasurementRules.sites ||
            !br.com.meutreino.domain.BodyMeasurementRules.valid(centimeters)) return
        viewModelScope.launch {
            dao.insertBodyMeasurement(BodyMeasurementEntity(
                recordedAt=System.currentTimeMillis(), site=site, centimeters=centimeters, source="manual"
            ))
            onSaved()
        }
    }
    fun deleteBodyMeasurement(item: BodyMeasurementEntity, onDone: ()->Unit) {
        viewModelScope.launch { dao.deleteBodyMeasurement(item); onDone() }
    }
    fun updateReviewSettings(days: Int, groupCount: Int, earlyPercent: Int) {
        if (days !in 28..112 || groupCount !in 6..20 || earlyPercent !in 60..100) return
        edit { it.copy(reviewWindowDays=days, reviewGroupExposures=groupCount, reviewEarlyPercent=earlyPercent) }
    }
    fun addRecovery(state: String, onSaved: ()->Unit) {
        if (state !in setOf("BEM","CANSADO","DOR_PERSISTENTE")) return
        viewModelScope.launch {
            val p=store.data.first()
            val version=ReviewSnapshotBuilder.activeVersion(p,planVersions.value)
            dao.insertRecoveryCheckIn(RecoveryCheckInEntity(
                recordedAt=System.currentTimeMillis(),planVersionId=version?.id,state=state
            ))
            onSaved()
        }
    }
    fun setBodyGoal(site:String, initial:Double, target:Double, onSaved:()->Unit) {
        if(site !in br.com.meutreino.domain.BodyMeasurementRules.sites ||
            !initial.isFinite() || !target.isFinite() || initial !in 5.0..250.0 ||
            target !in 5.0..250.0 || initial==target) return
        viewModelScope.launch {
            dao.upsertBodyGoal(BodyMeasurementGoalEntity(site,initial,target,System.currentTimeMillis()))
            onSaved()
        }
    }
    fun removeBodyGoal(goal:BodyMeasurementGoalEntity) { viewModelScope.launch { dao.deleteBodyGoal(goal) } }

    /** Alteração pontual solicitada pelo usuário, somente depois da prévia e confirmações. */
    fun applyReviewedVariation(planId:String, originalId:String, replacementId:String,
                               fromVersionId:Long, verdict:String, reason:String, evidence:String,
                               onDone:(Boolean)->Unit) {
        viewModelScope.launch {
            try {
                writeLock.withLock {
                    if(dao.activeSession()!=null) {
                        notify("Finalize ou descarte o treino ativo antes de modificar a ficha.")
                        onDone(false)
                        return@withLock
                    }
                    val p=store.data.first()
                    val lastRecovery=dao.latestRecovery(fromVersionId)
                    if(reason=="DOR_PERSISTENTE" || reason=="EQUIPAMENTO" ||
                       (lastRecovery?.state=="DOR_PERSISTENTE" &&
                        System.currentTimeMillis()-lastRecovery.recordedAt <= 14L*86_400_000L)) {
                        notify("Não será gerada variação automática diante de dor persistente ou equipamentos incompatíveis. Revise as condições e busque orientação profissional se a dor continuar.")
                        onDone(false)
                        return@withLock
                    }
                    val active=ReviewSnapshotBuilder.activeVersion(p,planVersions.value)
                    if(active?.id != fromVersionId) {
                        notify("A ficha mudou desde a análise. Reabra a revisão antes de aplicar.")
                        onDone(false)
                        return@withLock
                    }
                    val env=p.defaultEnvironment.ifBlank { p.environments.firstOrNull() ?: "Casa sem equipamentos" }
                    val base=TrainingPlanner.build(p.copy(defaultEnvironment=env))
                    val session=base.sessions.firstOrNull { it.id==planId }
                    val original=session?.exercises?.firstOrNull { it.exercise.id==originalId }?.exercise
                    val replacement=ExerciseCatalog.byId(replacementId)
                    val currentId=p.overrides["$planId:$originalId"] ?: originalId
                    val inSession=session?.exercises?.any { ex ->
                        val effectiveId=p.overrides["$planId:${ex.exercise.id}"] ?: ex.exercise.id
                        ex.exercise.id!=originalId && effectiveId==replacementId
                    } ?: false
                    if(original==null || replacement==null || currentId==replacementId || inSession ||
                       replacement !in ExerciseCatalog.equivalents(original,env,p.equipments,p.experience)) {
                        notify("Alternativa indisponível ou incompatível. Nenhuma alteração foi feita.")
                        onDone(false)
                        return@withLock
                    }
                    val proposed=p.copy(overrides=p.overrides+("$planId:$originalId" to replacementId))
                    val oldFingerprint=PlanVersionSnapshot.fingerprint(PlanVersionSnapshot.content(p,env))
                    val newFingerprint=PlanVersionSnapshot.fingerprint(PlanVersionSnapshot.content(proposed,env))
                    if(oldFingerprint==newFingerprint) {
                        notify("A proposta não muda o conteúdo da ficha. Nenhuma alteração foi feita.")
                        onDone(false)
                        return@withLock
                    }
                    // Registra o snapshot antes de tornar o novo plano ativo.
                    val newVersion=ensurePlanVersion(proposed,env,"Variação confirmada: $reason")
                    store.save(proposed)
                    dao.insertReviewEvent(PlanReviewEventEntity(
                        recordedAt=System.currentTimeMillis(),fromVersionId=fromVersionId,toVersionId=newVersion,
                        verdict=verdict,userReason=reason,outcome="APLICADA",evidence=evidence
                    ))
                    onDone(true)
                    notify("Variação aplicada. A versão anterior permanece no Histórico → Fichas.")
                }
            } catch (e:Exception) {
                notify("Não foi possível salvar a variação: ${e.localizedMessage ?: "erro"}")
                onDone(false)
            }
        }
    }

    fun logReviewDecision(versionId:Long?, verdict:String, reason:String, evidence:String, outcome:String) {
        if(outcome !in setOf("MANTIDA","CANCELADA")) return
        viewModelScope.launch { dao.insertReviewEvent(PlanReviewEventEntity(
            recordedAt=System.currentTimeMillis(),fromVersionId=versionId,toVersionId=null,
            verdict=verdict,userReason=reason,outcome=outcome,evidence=evidence
        )) }
    }

    fun healthAvailable(): Boolean = try {
        HealthConnectGateway.available(getApplication())
    } catch (_: Exception) { false }

    fun refreshHealth() {
        if (healthBusy.value) return
        viewModelScope.launch {
            healthBusy.value = true
            try {
                if (!healthAvailable()) {
                    healthMessage.value = "Health Connect não está disponível. No Android 13 ou anterior, instale-o pela Play Store."
                    return@launch
                }
                if (health.grantedPermissions().intersect(HealthConnectGateway.permissions).isEmpty()) {
                    healthMessage.value = "Autorize pelo menos um tipo de dado antes de atualizar. As demais permissões são opcionais."
                    healthSnapshot.value = null
                    return@launch
                }
                healthSnapshot.value = withContext(Dispatchers.IO) { health.readSnapshot() }
                healthMessage.value = "Leitura concluída. Dados fornecidos pelo Health Connect, não necessariamente pela Mi Band."
            } catch (e: SecurityException) {
                healthMessage.value = "Permissões revogadas ou insuficientes."
                healthSnapshot.value = null
            } catch (e: Exception) {
                healthMessage.value = "Falha ao ler Health Connect: ${e.localizedMessage ?: "erro desconhecido"}"
            } finally { healthBusy.value = false }
        }
    }
    fun saveScaleWeight(measurement: MiScaleScanner.Result, onDone:()->Unit) {
        if (measurement.kilograms !in 2.0..350.0) return
        viewModelScope.launch {
            dao.insertWeight(WeightMeasurementEntity(
                recordedAt = measurement.receivedAt,
                kilograms = measurement.kilograms,
                source = "Mi Body Composition Scale 2 (Bluetooth; confirmado)"
            ))
            onDone()
        }
    }
    fun importLatestSavedWeight() {
        val mostRecent = weightHistory.value.firstOrNull() ?: return
        edit { it.copy(weightKg = String.format(java.util.Locale.US, "%.2f", mostRecent.kilograms)) }
        notify("Peso importado do histórico da balança. Você pode editá-lo no Perfil.")
    }
    fun notify(text:String){showMessage.value=text}
    fun clearMessage(){showMessage.value=null}
    override fun onCleared() { scaleScanner.stop(); bandProbe.close(); super.onCleared(); db.close() }
}
