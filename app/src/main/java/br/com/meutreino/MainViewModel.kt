package br.com.meutreino

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import br.com.meutreino.data.SettingsStore
import br.com.meutreino.data.CardioActivityEntity
import br.com.meutreino.data.MIGRATION_1_2
import java.util.UUID
import br.com.meutreino.data.WorkoutDatabase
import br.com.meutreino.data.WorkoutSessionEntity
import br.com.meutreino.data.WorkoutSetEntity
import br.com.meutreino.domain.ExerciseCatalog
import br.com.meutreino.domain.UserPrefs
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
    private val db = Room.databaseBuilder(application,WorkoutDatabase::class.java,"workouts_v1.db").addMigrations(MIGRATION_1_2).build()
    private val dao = db.dao()
    private val writeLock = Mutex()
    val prefs: StateFlow<UserPrefs?> = store.data.stateIn(viewModelScope,SharingStarted.Eagerly,null)
    val sessions = dao.observeSessions().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val allSets = dao.observeAllSets().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val cardioActivities = dao.observeCardio().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val activeSession=MutableStateFlow<WorkoutSessionEntity?>(null)
    val temporaryEnvironment=MutableStateFlow<String?>(null)
    val showMessage=MutableStateFlow<String?>(null)

    init { viewModelScope.launch { activeSession.value=dao.activeSession() } }
    private fun current(): UserPrefs = prefs.value ?: UserPrefs()
    fun edit(change:(UserPrefs)->UserPrefs) {
        viewModelScope.launch { // serializar alterações evita sobrescrever taps consecutivos
            writeLock.withLock { store.save(change(store.data.first())) }
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
            val id=dao.insertSession(WorkoutSessionEntity(planId=planId,title=title,environment=environment,startedAt=System.currentTimeMillis()))
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
    fun notify(text:String){showMessage.value=text}
    fun clearMessage(){showMessage.value=null}
    override fun onCleared() { super.onCleared(); db.close() }
}
