import br.com.meutreino.domain.*
import java.time.LocalDate
import java.time.ZoneId

fun main() {
    val zone = ZoneId.of("America/Sao_Paulo")
    val day = LocalDate.of(2026, 10, 20)
    val start = day.atTime(9,0).atZone(zone).toInstant().toEpochMilli()
    val s = HealthIaaBridge.Session("zepp:1", start, start+30*60_000, "Caminhada", "zepp")
    fun hr(minutes:Int, moderate:Boolean=true) = (0 until minutes).map { i ->
        HealthIaaBridge.Heart(start + i*60_000L, if(moderate) 110L else 140L, "zepp")
    }
    fun assess(sessions:List<HealthIaaBridge.Session>, samples:List<HealthIaaBridge.Heart>,
               age:Int?=46, blocked:Set<LocalDate> = emptySet()) =
        HealthIaaBridge.assess(sessions,samples,age,blocked,zone)
    val normal=assess(listOf(s),hr(30)).single()
    check(normal.status == HealthIaaBridge.Status.INCLUDED && normal.moderateMinutes==30)
    check(normal.estimatedPoints == 20.0)
    val sparse=assess(listOf(s),hr(5)).single()
    check(sparse.status==HealthIaaBridge.Status.INSUFFICIENT_HEART && sparse.estimatedPoints==0.0)
    val vigorous=assess(listOf(s),hr(30,false)).single()
    check(vigorous.vigorousMinutes==30 && vigorous.estimatedPoints==40.0)
    check(assess(listOf(s),hr(30),blocked=setOf(day)).single().status==HealthIaaBridge.Status.DUPLICATE_LOCAL)
    check(assess(listOf(s),hr(30),age=null).single().status==HealthIaaBridge.Status.MISSING_AGE)
    val duplicate=s.copy(id="fit:2",source="fit")
    val overlap=assess(listOf(s,duplicate),hr(30))
    check(overlap.count { it.status==HealthIaaBridge.Status.INCLUDED }==1)
    check(overlap.count { it.status==HealthIaaBridge.Status.DUPLICATE_HEALTH }==1)
    check(HealthIaaBridge.events(overlap).size==1)
    val future=ActivityScore.calculate(HealthIaaBridge.events(listOf(normal)),day.plusDays(7),zone)
    check(future.balance==0.0 && future.expiredToday==20.0)
    val short=s.copy(endMillis=start+3*60_000)
    check(assess(listOf(short),hr(3)).single().status==HealthIaaBridge.Status.INVALID_DURATION)
    println("OK: 9 testes Health IAA (cobertura, intensidade, duplicação, expiração, limites)")
}
