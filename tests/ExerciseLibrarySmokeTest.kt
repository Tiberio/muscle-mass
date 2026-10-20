import br.com.meutreino.domain.*

fun main() {
    val all=ExerciseCatalog.all
    check(all.size>=150) { "Total insuficiente: ${all.size}" }
    check(all.size==all.map{it.id}.distinct().size)
    check(all.size==all.map{it.name}.distinct().size)
    check(all.count{it.modality=="Calistenia"}>=40)
    check(all.all{it.advice.isNotBlank()&&it.commonMistakes.isNotBlank()})
    check(ExerciseCatalog.options("Casa sem equipamentos",emptyList(),"Iniciante").all{it.required.isEmpty()})
    check(ExerciseCatalog.equivalents(ExerciseCatalog.byId("curl")!!,"Academia",emptyList(),"Avançado").all{ExerciseCatalog.armAction(it)=="bíceps"})
    val p=UserPrefs(defaultEnvironment="Academia",experience="Intermediário",weekdays=listOf("Seg","Ter","Qua","Qui","Sex"),minutesPerSession=45,cardioActivities=listOf("Caminhada"),cardioMinutes=30,cardioWeeklyDays=7)
    val plan=TrainingPlanner.build(p)
    check(plan.sessions.size==5)
    check(plan.sessions.all{it.exercises.size==4})
    check(plan.sessions[0].exercises.map{it.exercise.id} != plan.sessions[2].exercises.map{it.exercise.id})
    check(plan.sessions.map{it.exercises.size}==TrainingPlanner.build(p.copy(cardioActivities=emptyList())).sessions.map{it.exercises.size})
    println("OK: ${all.size} exercícios; ${all.count{it.modality=="Calistenia"}} de calistenia; seleção e cardio independente validados.")
    println("Regiões: "+all.groupingBy{it.primaryMuscle}.eachCount())
}
