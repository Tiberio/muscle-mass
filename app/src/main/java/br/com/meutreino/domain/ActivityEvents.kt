package br.com.meutreino.domain

import br.com.meutreino.data.CardioActivityEntity
import br.com.meutreino.data.WorkoutSessionEntity
import br.com.meutreino.data.WorkoutSetEntity
import java.time.LocalDate

/** Converte registros locais sem duplicar cardio e musculação de uma mesma sessão. */
object ActivityEvents {
    fun summary(sessions:List<WorkoutSessionEntity>, sets:List<WorkoutSetEntity>,
                cardio:List<CardioActivityEntity>, today:LocalDate):ActivityScore.Summary {
        return ActivityScore.calculate(events(sessions,sets,cardio),today)
    }
    fun events(sessions:List<WorkoutSessionEntity>, sets:List<WorkoutSetEntity>,
               cardio:List<CardioActivityEntity>):List<ActivityScore.Event> {
        val countBySession=sets.groupingBy{it.sessionId}.eachCount()
        val fromSessions=sessions.filter { it.status == "COMPLETED" }.flatMap { s ->
            buildList {
                val stamp=s.finishedAt?:s.startedAt
                val count=countBySession[s.id]?:0
                if(count>0) add(ActivityScore.Event("strength:${s.id}",stamp,ActivityScore.Kind.STRENGTH,sets=count))
                if(s.cardioMinutes>0) add(ActivityScore.Event("workout-cardio:${s.id}",stamp,ActivityScore.Kind.CARDIO,
                    minutes=s.cardioMinutes,intensity=ActivityScore.parseIntensity(s.cardioIntensity)))
            }
        }
        val manual=cardio.map { a -> ActivityScore.Event("extra:${a.id}",a.recordedAt,
            ActivityScore.Kind.CARDIO,a.minutes,ActivityScore.parseIntensity(a.intensity),source=a.source) }
        return fromSessions+manual
    }
}
