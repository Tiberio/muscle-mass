package br.com.meutreino.domain

import org.junit.Assert.*
import org.junit.Test

class PlanVersionSnapshotTest {
    private val base = UserPrefs(
        defaultEnvironment="Casa sem equipamentos", weekdays=listOf("Seg","Qua","Sex")
    )
    @Test fun mesmaFichaGeraMesmaIdentidade() {
        val one = PlanVersionSnapshot.content(base,"Casa sem equipamentos")
        val two = PlanVersionSnapshot.content(base,"Casa sem equipamentos")
        assertEquals(one,two)
        assertEquals(PlanVersionSnapshot.fingerprint(one),PlanVersionSnapshot.fingerprint(two))
    }
    @Test fun alteraIdentidadeQuandoMudaDuracaoReal() {
        val one = PlanVersionSnapshot.content(base,"Casa sem equipamentos")
        val other = PlanVersionSnapshot.content(base.copy(minutesPerSession=60),"Casa sem equipamentos")
        assertNotEquals(PlanVersionSnapshot.fingerprint(one),PlanVersionSnapshot.fingerprint(other))
    }
}
