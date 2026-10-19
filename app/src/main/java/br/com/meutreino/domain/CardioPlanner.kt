package br.com.meutreino.domain

/** Planejamento de cardio independente das sessões de musculação. */
object CardioPlanner {
    private val days=listOf("Seg","Ter","Qua","Qui","Sex","Sáb","Dom")
    data class CardioDay(val day:String,val minutes:Int,val intensity:String,
                         val separate:Boolean,val explanation:String)
    fun schedule(p:UserPrefs,plan:TrainingPlan):List<CardioDay> {
        if(p.cardioActivities.isEmpty())return emptyList()
        val strength=plan.sessions.map{it.day}.toSet()
        val count=if(p.cardioWeeklyDays==0)strength.size else p.cardioWeeklyDays.coerceIn(1,7)
        val selected=if(p.cardioWeeklyDays==0)days.filter{it in strength}
        else if(count==7)days else (days.filter{it in strength}+days.filter{it !in strength}).take(count)
        return selected.map { day ->
            val rest=day !in strength
            val chosen=when {
                rest && p.cardioIntensity=="Vigorosa" -> "Leve (sugestão de recuperação)"
                rest && p.cardioIntensity=="Adaptativa" -> "Leve ou moderada"
                p.cardioIntensity=="Adaptativa" -> "Moderada"
                else -> p.cardioIntensity
            }
            val separate=rest||p.cardioWhen=="Dias separados"
            CardioDay(day,p.cardioMinutes,chosen,separate,
                if(rest)"Dia sem musculação: intensidade menor sugerida; você pode adaptar."
                else "Cardio adicional, com duração independente da musculação.")
        }
    }
}
