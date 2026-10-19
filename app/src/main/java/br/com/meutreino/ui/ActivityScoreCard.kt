package br.com.meutreino.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.meutreino.domain.ActivityScore
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun ActivityScoreCard(score: ActivityScore.Summary) {
    Pane {
        Text("IAA · atividade nos últimos 7 dias",color=LightBlue)
        Text("${score.roundedPoints} pontos",style=MaterialTheme.typography.headlineMedium)
        Subtext("Hoje: +${"%.1f".format(Locale.US,score.earnedToday)} · Expiraram: −${"%.1f".format(Locale.US,score.expiredToday)}")
        LinearProgressIndicator(progress={ (score.balance/100.0).toFloat().coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth(),color=Blue)
        Subtext("Minutos moderados equivalentes: ${score.moderateEquivalentMinutes} · Dias com força: ${score.strengthDays}/7")
        Subtext("100 pontos é uma referência interna de acompanhamento, não uma meta clínica nem o PAI Xiaomi. Cardio leve não soma pontos, mas continua sendo atividade útil.")
        val fmt=DateTimeFormatter.ofPattern("EEE dd/MM",Locale("pt","BR"))
        score.days.forEach { d->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(d.date.format(fmt),style=MaterialTheme.typography.bodySmall)
                Text("+${"%.1f".format(Locale.US,d.earned)}  −${"%.1f".format(Locale.US,d.expired)}",style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
