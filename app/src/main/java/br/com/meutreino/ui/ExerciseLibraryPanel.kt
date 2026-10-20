package br.com.meutreino.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.meutreino.domain.Exercise
import br.com.meutreino.domain.ExerciseCatalog
import br.com.meutreino.domain.UserPrefs

/** Busca local, sem rede e sem anúncios. Mostra somente o que foi cadastrado. */
@Composable fun ExerciseLibraryPanel(p: UserPrefs) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todos") }
    var onlyAvailable by remember { mutableStateOf(true) }
    var detailId by remember { mutableStateOf<String?>(null) }
    val available = remember(p) { ExerciseCatalog.options(p.defaultEnvironment,p.equipments,p.experience).map { it.id }.toSet() }
    val matches = remember(query, category, onlyAvailable, available) {
        ExerciseCatalog.all.filter { ex ->
            (!onlyAvailable || ex.id in available) &&
                (category == "Todos" || (category == "Calistenia" && ex.modality == "Calistenia") || ex.primaryMuscle == category) &&
                (query.isBlank() || ex.name.contains(query,ignoreCase=true) || ex.primaryMuscle.contains(query,ignoreCase=true) || ex.required.any { it.contains(query,ignoreCase=true) })
        }.sortedWith(compareBy<Exercise>{it.primaryMuscle}.thenBy{it.name})
    }
    Title("Biblioteca de exercícios")
    Subtext("${ExerciseCatalog.all.size} movimentos e variantes cadastrados, incluindo calistenia. Filtros por ambiente e nível.")
    OutlinedTextField(value=query,onValueChange={query=it},label={Text("Buscar por nome, músculo ou equipamento")},singleLine=true,modifier=Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Choice("Disponíveis",onlyAvailable,{onlyAvailable=true},Modifier.weight(1f))
        Choice("Todos",!onlyAvailable,{onlyAvailable=false},Modifier.weight(1f))
    }
    listOf("Todos","Calistenia","Peito","Costas","Ombros","Braços","Pernas","Glúteos","Abdômen").chunked(3).forEach { group ->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            group.forEach { name -> Choice(name,category==name,{category=name},Modifier.weight(1f)) }
            repeat(3-group.size){ Spacer(Modifier.weight(1f)) }
        }
    }
    Subtext("${matches.size} resultados; exibindo até 40. Refine a busca para ver outros.")
    matches.take(40).forEach { exercise ->
        Pane {
            Text(exercise.name,style=MaterialTheme.typography.titleSmall)
            Subtext("${exercise.primaryMuscle} · ${exercise.modality} · nível ${when(exercise.minimumLevel){0->"iniciante";1->"intermediário";else->"avançado"}}")
            if(exercise.required.isNotEmpty())Subtext("Equipamento: ${exercise.required.joinToString(", ")}")
            if(exercise.id !in available)Subtext("Não disponível no ambiente ou no nível atual.")
            TextButton(onClick={detailId=if(detailId==exercise.id)null else exercise.id}) { Text(if(detailId==exercise.id)"Ocultar orientações" else "Ver orientações") }
            if(detailId == exercise.id) {
                Subtext("Execução: ${exercise.advice}")
                Subtext("Evite: ${exercise.commonMistakes}")
                if(exercise.safetyNote.isNotBlank())Subtext(exercise.safetyNote)
                exercise.regressionId?.let { id -> ExerciseCatalog.byId(id)?.let { Subtext("Mais fácil: ${it.name}") } }
                exercise.progressionId?.let { id -> ExerciseCatalog.byId(id)?.let { Subtext("Progressão: ${it.name}") } }
                if(exercise.youtubeVideoId==null)Subtext("Vídeo específico ainda não verificado.")
            }
        }
    }
    if(matches.isEmpty())Subtext("Nenhuma correspondência. Experimente remover filtros ou mudar o ambiente.")
}
