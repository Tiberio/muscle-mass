package br.com.meutreino.domain

import org.junit.Assert.*
import org.junit.Test

class ExerciseLibraryTest {
    @Test fun distinctAndDetailed() {
        assertTrue("Esperados ao menos 150 exercícios", ExerciseCatalog.all.size >= 150)
        assertEquals(ExerciseCatalog.all.size, ExerciseCatalog.all.map { it.id }.toSet().size)
        assertEquals(ExerciseCatalog.all.size, ExerciseCatalog.all.map { it.name }.toSet().size)
        assertTrue(ExerciseCatalog.all.all { it.id.isNotBlank() && it.name.isNotBlank() && it.advice.isNotBlank() && it.primaryMuscle.isNotBlank() })
        assertTrue(ExerciseCatalog.all.all { it.minimumLevel in 0..2 && it.commonMistakes.isNotBlank() })
        assertTrue(ExerciseCatalog.all.count { it.modality=="Calistenia" } >= 40)
    }
    @Test fun previousIdsStillExist() {
        listOf("pushup","inclinepush","dbbench","machinepress","dbrow","bandrow","cable","pullup","squat","goblet","legpress","lunge","bridge","rdeadlift","dbshoulder","bandshoulder","lateral","curl","bandcurl","triceps","plank","deadbug","bird").forEach { assertNotNull(it, ExerciseCatalog.byId(it)) }
    }
    @Test fun supportedEnvironmentAndLevelAreRespected() {
        val body = ExerciseCatalog.options("Casa sem equipamentos", emptyList(), "Iniciante")
        assertTrue(body.isNotEmpty())
        assertTrue(body.all { it.required.isEmpty() && it.minimumLevel==0 })
        assertTrue(body.any { it.modality=="Calistenia" })
        val home = ExerciseCatalog.options("Casa equipada", listOf("Halteres","Banco"), "Iniciante")
        assertTrue(home.any { it.required.contains("Halteres") })
        assertFalse(home.any { "Máquinas" in it.required })
        assertTrue(ExerciseCatalog.options("Academia",emptyList(),"Avançado").size > home.size)
    }
    @Test fun equivalentsNeverChangePrimeMuscleOrArmFocus() {
        val base = ExerciseCatalog.byId("curl")!!
        val options = ExerciseCatalog.equivalents(base,"Academia", emptyList(),"Avançado")
        assertTrue(options.size >= 5)
        assertTrue(options.all { it.pattern==base.pattern && it.primaryMuscle==base.primaryMuscle && ExerciseCatalog.armAction(it)=="bíceps" })
        assertFalse(options.any { it.name.contains("Tríceps") })
    }
    @Test fun progressionReferencesResolve() {
        ExerciseCatalog.all.forEach {
            it.progressionId?.let { id -> assertNotNull(id, ExerciseCatalog.byId(id)) }
            it.regressionId?.let { id -> assertNotNull(id, ExerciseCatalog.byId(id)) }
            if (it.youtubeVideoId != null) assertTrue(it.youtubeVideoId!!.matches(Regex("[A-Za-z0-9_-]{11}")))
        }
    }
    @Test fun fiveDaysWithDailyCardioKeepStrengthVolume() {
        val p=UserPrefs(experience="Intermediário", defaultEnvironment="Academia", weekdays=listOf("Seg","Ter","Qua","Qui","Sex"), minutesPerSession=45, cardioMinutes=30, cardioActivities=listOf("Caminhada"), cardioWeeklyDays=7)
        val plan=TrainingPlanner.build(p)
        assertEquals(5,plan.sessions.size)
        assertTrue(plan.sessions.all{it.exercises.size==4})
        assertTrue(plan.sessions.indices.filter{it%2==0}.all { i -> plan.sessions[i].exercises.count{it.exercise.primaryMuscle in listOf("Pernas","Glúteos")} == 0 })
        assertTrue(plan.sessions.indices.filter{it%2==1}.all { i -> plan.sessions[i].exercises.count{it.exercise.primaryMuscle in listOf("Peito","Costas","Braços","Ombros")} == 0 })
        assertNotEquals(plan.sessions[0].exercises.map{it.exercise.id},plan.sessions[2].exercises.map{it.exercise.id})
        assertEquals(plan.sessions.map{it.exercises.size},TrainingPlanner.build(p.copy(cardioActivities=emptyList())).sessions.map{it.exercises.size})
    }
}
