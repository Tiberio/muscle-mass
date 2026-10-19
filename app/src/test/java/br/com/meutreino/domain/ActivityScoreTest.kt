package br.com.meutreino.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ActivityScoreTest {
    private val zone=ZoneId.of("America/Sao_Paulo")
    private val d=LocalDate.of(2026,10,19)
    private fun event(id:String,date:LocalDate,mins:Int,v:ActivityScore.Intensity)=
        ActivityScore.Event(id,date.atStartOfDay(zone).toInstant().toEpochMilli(),ActivityScore.Kind.CARDIO,mins,v)

    @Test fun sevenDayWindowExpiresOldPoints() {
        val prev=event("past",d.minusDays(7),30,ActivityScore.Intensity.MODERATE)
        val now=event("today",d,30,ActivityScore.Intensity.MODERATE)
        val output=ActivityScore.calculate(listOf(prev,now),d,zone)
        assertEquals(20.0,output.balance,0.000001)
        assertEquals(20.0,output.expiredToday,0.000001)
        assertEquals(20.0,output.earnedToday,0.000001)
    }

    @Test fun moderateAndVigorousReachHundredAerobicPoints() {
        assertEquals(100,ActivityScore.calculate(listOf(event("m",d,150,ActivityScore.Intensity.MODERATE)),d,zone).roundedPoints)
        assertEquals(100,ActivityScore.calculate(listOf(event("v",d,75,ActivityScore.Intensity.VIGOROUS)),d,zone).roundedPoints)
    }

    @Test fun duplicateIdDoesNotCountTwice() {
        val e=event("same",d,30,ActivityScore.Intensity.MODERATE)
        assertEquals(20.0,ActivityScore.calculate(listOf(e,e),d,zone).balance,0.000001)
    }

    @Test fun dailyStrengthBonusIsCapped() {
        val a=ActivityScore.Event("a",d.atStartOfDay(zone).toInstant().toEpochMilli(),ActivityScore.Kind.STRENGTH,sets=2)
        val b=a.copy(id="b")
        assertEquals(4.0,ActivityScore.calculate(listOf(a,b),d,zone).balance,0.000001)
    }
}
