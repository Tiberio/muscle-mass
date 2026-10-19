import br.com.meutreino.domain.*

fun main(){
    val prefs=UserPrefs(environments=listOf("Casa equipada"),defaultEnvironment="Casa equipada",
        equipments=listOf("Banco","Halteres","Elásticos"),weekdays=listOf("Seg","Ter","Qua","Qui","Sex"),
        cardioActivities=listOf("Caminhada","Elíptico"),cardioWeeklyDays=7,
        cardioIntensity="Adaptativa",cardioMinutes=15)
    val plan=TrainingPlanner.build(prefs)
    check(plan.sessions.size==5 && plan.sessions.map{it.day}==listOf("Seg","Ter","Qua","Qui","Sex"))
    check(plan.sessions[0].title.startsWith("Superiores"))
    check(plan.sessions[1].title.startsWith("Inferiores"))
    check(plan.sessions[2].title.startsWith("Superiores"))
    val days=CardioPlanner.schedule(prefs,plan)
    check(days.size==7 && days.map{it.day}==listOf("Seg","Ter","Qua","Qui","Sex","Sáb","Dom"))
    check(days.takeLast(2).all{it.separate && it.intensity=="Leve ou moderada"})
    check(days.take(5).all{!it.separate})
    check(CardioPlanner.schedule(prefs.copy(cardioWeeklyDays=0),plan).size==5)
    check(CardioPlanner.schedule(prefs.copy(cardioActivities=emptyList()),plan).isEmpty())
    println("OK: 8 testes agenda cardio e alternância de força")
}
