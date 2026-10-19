package br.com.meutreino.devices

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/** Busca somente enquanto o usuário solicita; nenhuma conexão permanente nem histórico BLE. */
class MiScaleScanner(private val context: Context) {
    data class Result(val kilograms: Double, val receivedAt: Long)
    val reading = MutableStateFlow<Result?>(null)
    val message = MutableStateFlow("Toque para procurar uma pesagem estabilizada.")
    val scanning = MutableStateFlow(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bodyService = ParcelUuid(UUID.fromString("0000181b-0000-1000-8000-00805f9b34fb"))
    private var callback: ScanCallback? = null
    private val expire = Runnable { stop(); if (reading.value == null) message.value = "Nenhuma pesagem encontrada. Suba na balança e tente novamente." }

    fun hasPermission(): Boolean = if (Build.VERSION.SDK_INT >= 31) {
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (scanning.value) return
        if (!hasPermission()) { message.value = "Autorize o Bluetooth para procurar a balança."; return }
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            message.value = "Ative o Bluetooth do celular e tente novamente."; return
        }
        val scanner = adapter.bluetoothLeScanner ?: run { message.value = "Scanner Bluetooth indisponível."; return }
        reading.value = null
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val payload = result.scanRecord?.getServiceData(bodyService) ?: return
                val measurement = MiScaleParser.parse(payload) ?: return
                reading.value = Result(measurement.kilograms, System.currentTimeMillis())
                message.value = "Peso estabilizado recebido. Confirme se a medição é sua."
                mainHandler.post { stop() }
            }
            override fun onScanFailed(errorCode: Int) {
                message.value = "Falha na busca Bluetooth (código $errorCode)."
                mainHandler.post { stop() }
            }
        }
        try {
            scanner.startScan(null,
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), cb)
            callback = cb
            scanning.value = true
            message.value = "Procurando Mi Body Composition Scale 2 por até 25 segundos…"
            mainHandler.removeCallbacks(expire)
            mainHandler.postDelayed(expire, 25_000L)
        } catch (e: SecurityException) {
            message.value = "Permissão Bluetooth negada."
        } catch (e: Exception) {
            message.value = "Não foi possível iniciar a leitura: ${e.localizedMessage ?: "erro desconhecido"}"
        }
    }
    @SuppressLint("MissingPermission")
    fun stop() {
        mainHandler.removeCallbacks(expire)
        val cb = callback
        callback = null
        if (cb != null) try {
            val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            adapter?.bluetoothLeScanner?.stopScan(cb)
        } catch (_: Exception) { }
        scanning.value = false
    }
}
