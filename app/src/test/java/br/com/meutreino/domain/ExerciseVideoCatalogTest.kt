package br.com.meutreino.domain

import org.junit.Assert.*
import org.junit.Test

class ExerciseVideoCatalogTest {
    @Test fun catalogIndexesByStableExerciseId() {
        val videos = listOf(ExerciseVideo("dbbench", "PziFsPH2EZI", "Autor", "Demo"))
        ExerciseVideoCatalog.replaceWith(videos)
        assertEquals(videos.first(), ExerciseVideoCatalog.forExercise("dbbench"))
        assertNull(ExerciseVideoCatalog.forExercise("other"))
    }
    @Test fun clipUsesAbsoluteSeconds() {
        val video = ExerciseVideo("example", "PziFsPH2EZI", "Autor", "Título", 32, 48)
        assertTrue(video.embedUrl.contains("start=32"))
        assertTrue(video.embedUrl.contains("end=48"))
        assertTrue(video.watchUrl.contains("t=32s"))
    }
    @Test fun candidateVideoNeverBecomesAnApprovedDemoAutomatically() {
        val candidate = ExerciseVideoCandidate("chinup", "jIvbJzs1V4I", "Chin-up", "workout_database")
        ExerciseVideoCandidateCatalog.replaceWith(listOf(candidate))
        assertEquals("https://www.youtube.com/watch?v=jIvbJzs1V4I",
            ExerciseVideoCandidateCatalog.forExercise("chinup")?.watchUrl)
        assertNull(ExerciseVideoCatalog.forExercise("chinup"))
        assertThrows(IllegalArgumentException::class.java) {
            ExerciseVideoCandidateCatalog.replaceWith(listOf(candidate, candidate))
        }
    }
    @Test fun alternativesStayOutsideEmbeddedPlayer() {
        val alt = ExerciseVideoAlternative("dbbench", "x1X2X3X4X5X", "Outra técnica", "Canal")
        ExerciseVideoAlternativeCatalog.replaceWith(listOf(alt))
        assertEquals(alt, ExerciseVideoAlternativeCatalog.forExercise("dbbench").single())
        assertEquals("https://www.youtube.com/watch?v=x1X2X3X4X5X", alt.watchUrl)
        assertTrue(ExerciseVideoAlternativeCatalog.forExercise("unknown").isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            ExerciseVideoAlternativeCatalog.replaceWith(listOf(alt, alt))
        }
        ExerciseVideoAlternativeCatalog.replaceWith(emptyList())
    }
    @Test fun duplicatesOrInvalidIntervalsFail() {
        val v = ExerciseVideo("dbbench", "PziFsPH2EZI", "A", "T")
        assertThrows(IllegalArgumentException::class.java) {
            ExerciseVideoCatalog.replaceWith(listOf(v, v))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExerciseVideo("x", "PziFsPH2EZI", "A", "T", 30, 20)
        }
    }
}
