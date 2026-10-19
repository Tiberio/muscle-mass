package br.com.meutreino.devices

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/**
 * Diagnóstico BLE não autenticado da Mi Band 7.
 * NÃO implementa protocolo privado da Huami, PAI, histórico ou pareamento.
 * A conexão é temporária, apenas por comando do usuário, sem persistir endereços.
 */
class MiBand7Probe(private val context: Context) {
    data class Nearby(val address: String, val name: String, val rssi: Int, val probableBand: Boolean) {
        val shortId get() = address.takeLast(5)
    }

    val nearby = MutableStateFlow<List<Nearby>>(emptyList())
    val scanning = MutableStateFlow(false)
    val connected = MutableStateFlow(false)
    val message = MutableStateFlow("Procure sua pulseira para diagnosticar o Bluetooth.")
    val services = MutableStateFlow<List<String>>(emptyList())
    val batteryPercent = MutableStateFlow<Int?>(null)
    val heartBpm = MutableStateFlow<Int?>(null)

    private val handler = Handler(Looper.getMainLooper())
    private val devices = mutableMapOf<String, BluetoothDevice>() // apenas memória, durante a tela
    private var scanCallback: ScanCallback? = null
    private var gatt: BluetoothGatt? = null
    private val serviceBattery = uuid16("180F")
    private val characteristicBattery = uuid16("2A19")
    private val serviceHeart = uuid16("180D")
    private val characteristicHeart = uuid16("2A37")
    private val descriptorCccd = uuid16("2902")
    private val bandServices = setOf(uuid16("FEE0"), uuid16("FEE1"))
    private val expireScan = Runnable {
        stopScan()
        if (nearby.value.isEmpty()) message.value = "Nenhum dispositivo BLE apareceu. A pulseira pode estar conectada exclusivamente ao Zepp Life."
        else message.value = "Busca concluída. Selecione o dispositivo que acredita ser sua pulseira."
    }
    private val expireConnect = Runnable {
        if (!connected.value) {
            closeConnection()
            message.value = "A conexão BLE não respondeu. O Zepp Life pode estar utilizando a pulseira."
        }
    }

    fun hasPermission(): Boolean = if (Build.VERSION.SDK_INT >= 31) {
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (scanning.value) return
        if (!hasPermission()) { message.value = "Autorize dispositivos próximos antes de iniciar."; return }
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !adapter.isEnabled) { message.value = "Ative o Bluetooth do celular."; return }
        val scanner = adapter.bluetoothLeScanner ?: run { message.value = "Scanner BLE indisponível."; return }
        stopScan()
        closeConnection()
        nearby.value = emptyList()
        devices.clear()
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handler.post {
                    if (!scanning.value) return@post
                    val device = result.device ?: return@post
                    val address = device.address ?: return@post
                    val announcedName = result.scanRecord?.deviceName
                    val deviceName = try { device.name } catch (_: SecurityException) { null }
                    val name = (announcedName ?: deviceName).orEmpty().trim()
                    // Não identifica automaticamente por MAC. Um nome parecido é apenas pista.
                    val guess = name.contains("band", ignoreCase = true) ||
                        name.contains("amazfit", ignoreCase = true) ||
                        result.scanRecord?.serviceUuids.orEmpty().any { it.uuid in bandServices }
                    devices[address] = device
                    val updated = nearby.value.associateBy { it.address }.toMutableMap()
                    updated[address] = Nearby(address, name.ifBlank { "Dispositivo sem nome" }, result.rssi, guess)
                    nearby.value = updated.values.sortedWith(compareByDescending<Nearby> { it.probableBand }.thenByDescending { it.rssi }).take(20)
                }
            }
            override fun onScanFailed(errorCode: Int) {
                handler.post {
                    stopScan()
                    message.value = "Falha na busca Bluetooth (código $errorCode)."
                }
            }
        }
        try {
            scanCallback = cb
            scanning.value = true
            message.value = "Procurando dispositivos próximos durante 20 segundos…"
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), cb)
            handler.postDelayed(expireScan, 20_000L)
        } catch (_: SecurityException) {
            stopScan(); message.value = "Permissão Bluetooth insuficiente."
        } catch (e: Exception) {
            stopScan(); message.value = "Não foi possível buscar: ${e.localizedMessage ?: "erro desconhecido"}"
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        handler.removeCallbacks(expireScan)
        val cb = scanCallback
        scanCallback = null
        if (cb != null) try {
            val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            adapter?.bluetoothLeScanner?.stopScan(cb)
        } catch (_: Exception) { }
        scanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        if (!hasPermission()) { message.value = "Permissão Bluetooth não concedida."; return }
        val selected = devices[address] ?: run { message.value = "Selecione um dispositivo da lista."; return }
        stopScan()
        closeConnection()
        batteryPercent.value = null
        heartBpm.value = null
        services.value = emptyList()
        message.value = "Tentando conexão BLE temporária (${address.takeLast(5)})…"
        try {
            gatt = selected.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(connection: BluetoothGatt, status: Int, newState: Int) {
                    if (gatt != null && gatt !== connection) return
                    if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                        connected.value = true
                        handler.removeCallbacks(expireConnect)
                        message.value = "Conexão BLE estabelecida. Descobrindo serviços disponíveis…"
                        try { connection.discoverServices() } catch (_: SecurityException) {
                            message.value = "Permissão revogada durante a leitura."
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                        connected.value = false
                        message.value = "BLE desconectado (status $status). O Zepp Life pode ocupar a conexão."
                        handler.post { if (gatt === connection) closeConnection() }
                    }
                }
                override fun onServicesDiscovered(connection: BluetoothGatt, status: Int) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        message.value = "Conectou, mas a descoberta de serviços falhou ($status)."; return
                    }
                    val discovered = connection.services.map { it.uuid }
                    services.value = listOfNotNull(
                        "Serviços GATT: ${discovered.size}",
                        if (discovered.any { it in bandServices }) "Protocolo Huami detectado (requer autenticação para dados privados)" else null,
                        if (serviceHeart in discovered) "Frequência cardíaca padrão (0x180D): presente" else "Frequência cardíaca padrão: indisponível",
                        if (serviceBattery in discovered) "Bateria padrão (0x180F): presente" else "Bateria padrão: indisponível"
                    )
                    message.value = "Conectou por BLE. Dados privados da Mi Band 7 exigem autenticação Huami."
                    val batteryChar = connection.getService(serviceBattery)?.getCharacteristic(characteristicBattery)
                    if (batteryChar != null && batteryChar.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                        try { if (!connection.readCharacteristic(batteryChar)) subscribeHeart(connection) }
                        catch (_: SecurityException) { subscribeHeart(connection) }
                    } else subscribeHeart(connection)
                }
                @Deprecated("BluetoothGatt callback legado pré-API 33")
                override fun onCharacteristicRead(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                    if (Build.VERSION.SDK_INT < 33) batteryRead(connection, characteristic.uuid, characteristic.value ?: byteArrayOf(), status)
                }
                override fun onCharacteristicRead(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                    batteryRead(connection, characteristic.uuid, value, status)
                }
                @Deprecated("BluetoothGatt callback legado pré-API 33")
                override fun onCharacteristicChanged(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                    if (Build.VERSION.SDK_INT < 33) heartChanged(characteristic.uuid, characteristic.value ?: byteArrayOf())
                }
                override fun onCharacteristicChanged(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                    heartChanged(characteristic.uuid, value)
                }
                override fun onDescriptorWrite(connection: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        message.value = "Notificações de FC habilitadas; aguardando amostras, se a pulseira as disponibilizar."
                    }
                }
            }, BluetoothDevice.TRANSPORT_LE)
            handler.postDelayed(expireConnect, 15_000L)
        } catch (e: Exception) {
            message.value = "Não foi possível conectar: ${e.localizedMessage ?: "erro desconhecido"}"
        }
    }

    private fun batteryRead(connection: BluetoothGatt, uuid: UUID, bytes: ByteArray, status: Int) {
        if (uuid == characteristicBattery && status == BluetoothGatt.GATT_SUCCESS && bytes.isNotEmpty()) {
            batteryPercent.value = (bytes[0].toInt() and 0xff).takeIf { it in 0..100 }
        }
        subscribeHeart(connection)
    }
    private fun heartChanged(uuid: UUID, bytes: ByteArray) {
        if (uuid == characteristicHeart) {
            StandardHeartRateParser.parse(bytes)?.let { heartBpm.value = it }
        }
    }
    @SuppressLint("MissingPermission")
    private fun subscribeHeart(connection: BluetoothGatt) {
        val characteristic = connection.getService(serviceHeart)?.getCharacteristic(characteristicHeart) ?: return
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) return
        val descriptor = characteristic.getDescriptor(descriptorCccd) ?: return
        try {
            if (!connection.setCharacteristicNotification(characteristic, true)) return
            if (Build.VERSION.SDK_INT >= 33) {
                connection.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                connection.writeDescriptor(descriptor)
            }
        } catch (_: SecurityException) { message.value = "Permissão insuficiente para notificações BLE." }
    }

    @SuppressLint("MissingPermission")
    fun closeConnection() {
        handler.removeCallbacks(expireConnect)
        val old = gatt
        gatt = null
        connected.value = false
        try { old?.disconnect() } catch (_: Exception) { }
        try { old?.close() } catch (_: Exception) { }
    }
    fun close() { stopScan(); closeConnection(); devices.clear(); nearby.value = emptyList() }
    private fun uuid16(hex: String): UUID = UUID.fromString("0000${hex.lowercase()}-0000-1000-8000-00805f9b34fb")
}
