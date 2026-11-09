package br.com.meutreino.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.meutreino.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Storage Access Framework: o usuário escolhe onde salvar ou ler, sem permissões amplas. */
@Composable fun LocalBackupPanel(vm: MainViewModel, compact: Boolean = false) {
    val busy by vm.backupBusy.collectAsState()
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportLocalBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }
    Pane {
        Text(if (compact) "Já tem um backup?" else "Cópia de segurança", style=MaterialTheme.typography.titleMedium)
        if (!compact) {
            Subtext("Salve seu perfil, sessões, séries, cardio, pesagens, circunferências, fichas e revisões em arquivo JSON local.")
            OutlinedButton(
                onClick={
                    val stamp=SimpleDateFormat("yyyyMMdd-HHmm",Locale.ROOT).format(Date())
                    export.launch("meu-treino-backup-$stamp.json")
                }, enabled=!busy, modifier=Modifier.fillMaxWidth()
            ) { Text("Exportar backup (.json)") }
        }
        OutlinedButton(
            onClick={importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))},
            enabled=!busy, modifier=Modifier.fillMaxWidth()
        ) { Text("Restaurar backup (.json)") }
        Subtext("Restauração aceita apenas se não houver registros de treino, medidas ou outros históricos neste dispositivo.")
        if (!compact) Subtext("A cópia não é criptografada e contém dados pessoais. Guarde-a em local privado. Dados do Health Connect e chaves Bluetooth não são exportados.")
        if (busy) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
    }
    val chosen = pendingRestore
    if (chosen != null) AlertDialog(
        onDismissRequest={ pendingRestore=null },
        title={ Text("Restaurar dados do aplicativo?") },
        text={ Text("Confira se escolheu um backup do Meu Treino. A restauração não mistura históricos e é recusada se já houver registros neste celular. Uma ficha inicial ainda não usada poderá ser substituída. Deseja continuar?") },
        confirmButton={ TextButton(onClick={pendingRestore=null;vm.restoreLocalBackup(chosen)}) {Text("Confirmar restauração")} },
        dismissButton={ TextButton(onClick={pendingRestore=null}) {Text("Cancelar")} }
    )
}
