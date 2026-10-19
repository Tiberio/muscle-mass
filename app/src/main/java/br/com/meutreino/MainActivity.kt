package br.com.meutreino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.meutreino.ui.MeuTreinoApp
import br.com.meutreino.ui.MeuTreinoTheme

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeuTreinoTheme {
                val vm:MainViewModel=viewModel()
                val prefs by vm.prefs.collectAsState()
                if(prefs==null) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {CircularProgressIndicator()}
                else MeuTreinoApp(vm,prefs!!)
            }
        }
    }
}
