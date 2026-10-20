package br.com.meutreino.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HealthIaaBridgeTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val start = LocalDate.of(2026,10,20).atTime(10,0).atZone(zone).toInstant().toEpochMilli()
    private val s = HealthIaaBridge.Session("id",start,start+30*60_000,"Caminhada","Mi Fitness")
    private fun hr(n:Int,bpm:Long) = (0 until n).map { HealthIaaBridge.Heart(start+it*60_000L,bpm,"Mi Fitness") }

    @Test fun moderateSessionProducesTwentyPoints() {
        val a=HealthIaaBridge.assess(listOf(s),hr(30,110),46,emptySet(),zone).single()
        assertEquals(HealthIaaBridge.Status.INCLUDED,a.status)
        assertEquals(20.0,a.estimatedPoints,0.0001)
    }
    @Test fun insufficientSamplesMustNotInventActivity() {
        val a=HealthIaaBridge.assess(listOf(s),hr(4,130),46,emptySet(),zone).single()
        assertEquals(HealthIaaBridge.Status.INSUFFICIENT_HEART,a.status)
        assertEquals(0.0,a.estimatedPoints,0.0001)
    }
    @Test fun overlappingSourcesCountOnce() {
        val assessments=HealthIaaBridge.assess(listOf(s,s.copy(id="another",source="Google Fit")),
            hr(30,130),46,emptySet(),zone)
        assertEquals(1,assessments.count{it.status==HealthIaaBridge.Status.INCLUDED})
        assertEquals(1,assessments.count{it.status==HealthIaaBridge.Status.DUPLICATE_HEALTH})
    }
    @Test fun manualCardioOnSameDaySuppressesEstimate() {
        val day=LocalDate.of(2026,10,20)
        val a=HealthIaaBridge.assess(listOf(s),hr(30,120),46,setOf(day),zone).single()
        assertEquals(HealthIaaBridge.Status.DUPLICATE_LOCAL,a.status)
    }
}
