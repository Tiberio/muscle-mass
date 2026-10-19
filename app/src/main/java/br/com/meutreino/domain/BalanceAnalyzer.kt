package br.com.meutreino.domain

/** Indicador exploratório, não é um limiar clínico nem substitui educação física profissional. */
data class BalanceReport(val effectiveSets:Map<String,Double>,val remarks:List<String>)
object BalanceAnalyzer {
    fun analyze(plan:TrainingPlan):BalanceReport {
        val volume=mutableMapOf<String,Double>()
        plan.sessions.flatMap{it.exercises}.forEach{item->
            volume[item.exercise.primaryMuscle]=(volume[item.exercise.primaryMuscle]?:0.0)+item.sets
            item.exercise.secondaryMuscles.forEach{muscle->volume[muscle]=(volume[muscle]?:0.0)+item.sets*0.5}
        }
        val warnings=mutableListOf<String>()
        val push=plan.sessions.flatMap{it.exercises}.filter{it.exercise.pattern in listOf("push","verticalPush")}.sumOf{it.sets}
        val pull=plan.sessions.flatMap{it.exercises}.filter{it.exercise.pattern=="pull"}.sumOf{it.sets}
        if(pull==0)warnings+="Não há movimentos de puxar com resistência adequada. Considere cadastrar elásticos, barra ou ambiente de academia."
        else if(push>pull*1.5)warnings+="O volume estimado de empurrar supera consideravelmente o de puxar. Reveja a distribuição semanal."
        val total=volume.values.sum()
        val legs=(volume["Pernas"]?:0.0)+(volume["Glúteos"]?:0.0)
        if(total>0 && legs<total*0.15)warnings+="O estímulo estimado em membros inferiores está baixo em comparação com o restante da semana."
        warnings += "As séries efetivas são estimadas (músculo principal = 1; secundário = 0,5) e não representam uma dose clínica precisa."
        return BalanceReport(volume.toSortedMap(),warnings)
    }
}
