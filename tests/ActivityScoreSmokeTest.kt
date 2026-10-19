import br.com.meutreino.domain.ActivityScore
import java.time.LocalDate
import java.time.ZoneId

fun main() {
    val z = ZoneId.of("America/Sao_Paulo")
    val day = LocalDate.of(2026, 10, 19)
    fun stamp(d: LocalDate)=d.atTime(12, 0).atZone(z).toInstant().toEpochMilli()
    fun cardio(id: String, d: LocalDate, minutes: Int, intensity: ActivityScore.Intensity)=
        ActivityScore.Event(id,stamp(d),ActivityScore.Kind.CARDIO,minutes,intensity)
    val old = cardio("old",day.minusDays(7),30,ActivityScore.Intensity.MODERATE)
    val fresh = cardio("new",day,30,ActivityScore.Intensity.MODERATE)
    val today = ActivityScore.calculate(listOf(old,fresh),day,z)
    check(today.balance == 20.0) { "Exclui D-7, inclui D" }
    check(today.earnedToday == 20.0 && today.expiredToday == 20.0)
    check(ActivityScore.calculate(listOf(fresh,fresh),day,z).balance == 20.0) { "duplicatas" }
    val tomorrow = ActivityScore.calculate(listOf(fresh),day.plusDays(7),z)
    check(tomorrow.balance==0.0 && tomorrow.expiredToday==20.0) { "expiração no oitavo dia" }
    val mod = ActivityScore.calculate(listOf(cardio("m",day,150,ActivityScore.Intensity.MODERATE)),day,z)
    val vig = ActivityScore.calculate(listOf(cardio("v",day,75,ActivityScore.Intensity.VIGOROUS)),day,z)
    check(mod.roundedPoints==100 && vig.roundedPoints==100)
    val light = ActivityScore.calculate(listOf(cardio("l",day,100,ActivityScore.Intensity.LIGHT)),day,z)
    check(light.balance==0.0)
    val str1 = ActivityScore.Event("s1",stamp(day),ActivityScore.Kind.STRENGTH,sets=3)
    val str2 = ActivityScore.Event("s2",stamp(day),ActivityScore.Kind.STRENGTH,sets=5)
    val withStrength=ActivityScore.calculate(listOf(str1,str2),day,z)
    check(withStrength.balance==4.0 && withStrength.strengthDays==1)
    check(ActivityScore.calculate(listOf(old),day.minusDays(1),z).balance==20.0)
    println("OK: 8 testes do IAA (janela, expiração, eventos, intensidade, força)")
}
