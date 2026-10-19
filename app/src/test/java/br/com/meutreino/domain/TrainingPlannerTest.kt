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
    @Test fun balanceMetricsAreFinite(){
        val report=BalanceAnalyzer.analyze(TrainingPlanner.build(prefs))
        assertTrue(report.effectiveSets.isNotEmpty())
        assertTrue(report.effectiveSets.values.all{it>=0.0})
    }
}
