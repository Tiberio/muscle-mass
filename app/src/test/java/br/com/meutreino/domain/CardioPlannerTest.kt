package br.com.meutreino.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardioPlannerTest {
    @Test fun preservesFiveStrengthDaysAndSevenCardioDays(){
        val p=UserPrefs(environments=listOf("Casa equipada"),defaultEnvironment="Casa equipada",
            equipments=listOf("Halteres","Banco","Elásticos"),
            weekdays=listOf("Seg","Ter","Qua","Qui","Sex"),
            cardioActivities=listOf("Caminhada"),cardioWeeklyDays=7)
        val plan=TrainingPlanner.build(p)
        val cardio=CardioPlanner.schedule(p,plan)
        assertEquals(5,plan.sessions.size)
        assertEquals(7,cardio.size)
        assertTrue(plan.sessions[0].title.startsWith("Superiores"))
        assertTrue(plan.sessions[1].title.startsWith("Inferiores"))
        assertTrue(cardio.takeLast(2).all{it.separate})
    }
}
