package br.com.meutreino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.meutreino.ui.MeuTreinoTheme

/** Tela real de justificativa que o Android pode abrir ao solicitar dados de saúde. */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeuTreinoTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Privacidade — Meu Treino", style = MaterialTheme.typography.headlineSmall)
                    Text("O aplicativo solicita, somente com sua autorização, leitura de passos, " +
                        "frequência cardíaca, pesagens e sessões de exercício pelo Health Connect.")
                    Text("Esses dados são mostrados para seu acompanhamento pessoal. Nesta versão, " +
                        "a consulta ocorre quando você toca em Atualizar; não há sincronização automática " +
                        "em segundo plano e os dados de Health Connect não alimentam o IAA.")
                    Text("Apenas pesagens que você confirmar na aba Dispositivos são gravadas " +
                        "no banco local do aplicativo. Nada é enviado a servidores ou anunciantes.")
                    Text("As permissões podem ser negadas ou revogadas em Configurações do Android " +
                        "→ Health Connect → Permissões de aplicativos.")
                    Button(onClick = { finish() }) { Text("Voltar") }
                }
            }
        }
    }
}
