import br.com.meutreino.domain.*

fun main() {
    val p = UserPrefs(
        age = "46", environments = listOf("Casa sem equipamentos", "Casa equipada"),
        defaultEnvironment = "Casa equipada", equipments = listOf("Banco", "Halteres", "Elásticos"),
        priorityMuscles = listOf("Peito", "Braços", "Ombros"),
        weekdays = listOf("Seg", "Qua", "Sex"), minutesPerSession = 45
    )
    val plan=TrainingPlanner.build(p)
    check(plan.sessions.size==3)
    check(plan.sessions.map { it.day } == listOf("Seg","Qua","Sex"))
    check(plan.sessions.all { it.exercises.size in 2..5 })
    check(plan.sessions.flatMap { it.exercises }.any{it.exercise.pattern=="pull"})
    check(plan==TrainingPlanner.build(p)) { "Planner should be deterministic" }
    val body=TrainingPlanner.build(p.copy(defaultEnvironment="Casa sem equipamentos"))
    check(body.warnings.any{it.contains("puxada")})
    check(body.sessions.flatMap{it.exercises}.all{it.exercise.required.isEmpty()})
    val busy=TrainingPlanner.build(p.copy(minutesPerSession=20))
    check(busy.sessions.all{it.exercises.size<=2})
    val flexible=TrainingPlanner.build(p.copy(scheduleMode="Rotina flexível",weekdays=emptyList(),weeklyCount=4))
    check(flexible.sessions.size==4)
    val withoutPriority=TrainingPlanner.build(p.copy(priorityMuscles=emptyList()))
    check(withoutPriority.explanation.contains("Distribuição geral"))
    val balance=BalanceAnalyzer.analyze(plan)
    check(balance.effectiveSets.isNotEmpty())
    check(balance.effectiveSets.values.all{it>=0.0})
    check(BalanceAnalyzer.analyze(body).remarks.any{it.contains("puxar")})
    println("OK: 10 verificações do núcleo de planejamento e equilíbrio passaram.")
}
