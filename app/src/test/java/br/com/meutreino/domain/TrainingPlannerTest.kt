package br.com.meutreino.domain

import org.junit.Assert.*
import org.junit.Test

class TrainingPlannerTest {
    private val prefs=UserPrefs(
        age="40",environments=listOf("Casa sem equipamentos","Casa equipada"),
        defaultEnvironment="Casa equipada",equipments=listOf("Halteres","Banco","Elásticos"),
        weekdays=listOf("Seg","Qua","Sex"),priorityMuscles=listOf("Peito","Braços","Ombros")
    )
    @Test fun sessionsRespectScheduleAndAreDeterministic(){
        val a=TrainingPlanner.build(prefs)
        assertEquals(listOf("Seg","Qua","Sex"),a.sessions.map{it.day})
        assertEquals(a,TrainingPlanner.build(prefs))
    }
    @Test fun bodyweightExercisesRequireNoEquipment(){
        val a=TrainingPlanner.build(prefs.copy(defaultEnvironment="Casa sem equipamentos"))
        assertTrue(a.sessions.flatMap{it.exercises}.all{it.exercise.required.isEmpty()})
        assertTrue(a.warnings.any{it.contains("puxada")})
    }
    @Test fun shorterSessionsReceiveFewerExercises(){
        val a=TrainingPlanner.build(prefs.copy(minutesPerSession=20))
        assertTrue(a.sessions.all{it.exercises.size<=2})
    }
    @Test fun cardioNeverReducesStrengthExerciseCount(){
        val withCardio = prefs.copy(
            minutesPerSession=45,
            cardioActivities=listOf("Caminhada"),
            cardioWhen="Após musculação",
            cardioMinutes=30,
            cardioWeeklyDays=7
        )
        val noCardio = withCardio.copy(cardioActivities=emptyList())
        val a=TrainingPlanner.build(withCardio)
        val b=TrainingPlanner.build(noCardio)
        assertEquals(4,a.sessions.first().exercises.size)
        assertEquals(b.sessions.map{it.exercises.size},a.sessions.map{it.exercises.size})
        assertFalse(a.warnings.any{it.contains("deixa pouco tempo")})
        assertTrue(a.explanation.contains("independente"))
    }
    @Test fun longCardioDoesNotReduceStrengthSession(){
        val p=prefs.copy(
            minutesPerSession=60,cardioActivities=listOf("Elíptico"),
            cardioMinutes=90,cardioWhen="Misto"
        )
        val a=TrainingPlanner.build(p)
        assertEquals(5,a.sessions.first().exercises.size)
        assertEquals(60,a.sessions.first().minutes)
    }
    @Test fun balanceMetricsAreFinite(){
        val report=BalanceAnalyzer.analyze(TrainingPlanner.build(prefs))
        assertTrue(report.effectiveSets.isNotEmpty())
        assertTrue(report.effectiveSets.values.all{it>=0.0})
    }
}
