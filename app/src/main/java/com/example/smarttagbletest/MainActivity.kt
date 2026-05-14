package com.example.smarttagbletest

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var statusText: TextView
    private lateinit var scanButton: Button

    private var bluetoothGatt: BluetoothGatt? = null
    private var targetDevice: BluetoothDevice? = null

    private val handler = Handler(Looper.getMainLooper())
    private var clickCount = 0

    // MAC do teu chaveiro no log do nRF Connect
    private val TARGET_MAC = "FF:FF:11:98:9C:E8"

    // Serviço principal do chaveiro
    private val SERVICE_FFE0: UUID =
        UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")

    // Característica que manda 01 quando aperta o botão
    private val BUTTON_FFE1: UUID =
        UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    // Descriptor usado para ativar notification
    private val CCCD: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val clickRunnable = Runnable {
        when (clickCount) {
            1 -> acaoUmClique()
            2 -> acaoDoisCliques()
            3 -> acaoTresCliques()
            4 -> acaoQuatroCliques()
            else -> acaoCincoOuMaisCliques()
        }

        clickCount = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusText = TextView(this).apply {
            text = "Status: parado"
            textSize = 18f
            setPadding(30, 60, 30, 30)
        }

        scanButton = Button(this).apply {
            text = "Conectar no chaveiro"
            setOnClickListener {
                pedirPermissoes()
                iniciarScan()
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            addView(statusText)
            addView(scanButton)
        }

        setContentView(layout)
    }

    private fun pedirPermissoes() {
        val permissoes = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissoes.add(Manifest.permission.BLUETOOTH_SCAN)
            permissoes.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissoes.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        ActivityCompat.requestPermissions(this, permissoes.toTypedArray(), 1)
    }

    private fun temPermissaoScan(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun temPermissaoConnect(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun iniciarScan() {
        if (!temPermissaoScan()) {
            statusText.text = "Status: falta permissão Bluetooth"
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner

        if (scanner == null) {
            statusText.text = "Status: BLE indisponível"
            return
        }

        targetDevice = null
        statusText.text = "Status: escaneando..."

        try {
            scanner.startScan(scanCallback)

            handler.postDelayed({
                try {
                    scanner.stopScan(scanCallback)
                } catch (_: Exception) {}

                if (targetDevice == null) {
                    statusText.text = "Status: chaveiro não encontrado"
                }
            }, 10000)

        } catch (e: SecurityException) {
            statusText.text = "Erro de permissão no scan"
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device

            if (device.address == TARGET_MAC) {
                targetDevice = device

                try {
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
                } catch (_: Exception) {}

                runOnUiThread {
                    statusText.text = "Status: chaveiro encontrado. Conectando..."
                }

                conectar(device)
            }
        }
    }

    private fun conectar(device: BluetoothDevice) {
        if (!temPermissaoConnect()) {
            runOnUiThread {
                statusText.text = "Status: falta permissão para conectar"
            }
            return
        }

        try {
            bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: SecurityException) {
            statusText.text = "Erro de permissão ao conectar"
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            runOnUiThread {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    statusText.text = "Status: conectado. Descobrindo serviços..."
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    statusText.text = "Status: desconectado"
                    Toast.makeText(this@MainActivity, "Chaveiro desconectou", Toast.LENGTH_SHORT).show()
                }
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                try {
                    gatt.discoverServices()
                } catch (_: SecurityException) {}
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(SERVICE_FFE0)

            if (service == null) {
                runOnUiThread {
                    statusText.text = "Status: serviço FFE0 não encontrado"
                }
                return
            }

            val buttonChar = service.getCharacteristic(BUTTON_FFE1)

            if (buttonChar == null) {
                runOnUiThread {
                    statusText.text = "Status: característica FFE1 não encontrada"
                }
                return
            }

            ativarNotification(gatt, buttonChar)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == BUTTON_FFE1) {
                onBleButtonReceived(characteristic.value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == BUTTON_FFE1) {
                onBleButtonReceived(value)
            }
        }
    }

    private fun ativarNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        try {
            gatt.setCharacteristicNotification(characteristic, true)

            val descriptor = characteristic.getDescriptor(CCCD)
            if (descriptor != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(
                        descriptor,
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    )
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
            }

            runOnUiThread {
                statusText.text = "Status: conectado e escutando FFE1"
                Toast.makeText(this, "FFE1 ativado", Toast.LENGTH_SHORT).show()
            }

        } catch (e: SecurityException) {
            runOnUiThread {
                statusText.text = "Erro ao ativar notification"
            }
        }
    }

    private fun onBleButtonReceived(value: ByteArray) {
        if (value.isNotEmpty() && value[0] == 0x01.toByte()) {
            clickCount++

            runOnUiThread {
                statusText.text = "Recebeu 01. Cliques contando: $clickCount"
            }

            handler.removeCallbacks(clickRunnable)

            // Janela para contar vários cliques juntos.
            // Pelo teu log, 800ms é seguro.
            handler.postDelayed(clickRunnable, 800)
        }
    }

    private fun acaoUmClique() {
        runOnUiThread {
            statusText.text = "Ação: 1 clique"
            Toast.makeText(this, "1 clique: tocar som", Toast.LENGTH_SHORT).show()
        }

        try {
            val player = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            player?.setOnCompletionListener { it.release() }
            player?.start()
        } catch (_: Exception) {}
    }

    private fun acaoDoisCliques() {
        runOnUiThread {
            statusText.text = "Ação: 2 cliques"
            Toast.makeText(this, "2 cliques: gravar áudio aqui", Toast.LENGTH_SHORT).show()
        }
    }

    private fun acaoTresCliques() {
        runOnUiThread {
            statusText.text = "Ação: 3 cliques"
            Toast.makeText(this, "3 cliques: tirar foto aqui", Toast.LENGTH_SHORT).show()
        }
    }

    private fun acaoQuatroCliques() {
        runOnUiThread {
            statusText.text = "Ação: 4 cliques"
            Toast.makeText(this, "4 cliques: modo rastreio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun acaoCincoOuMaisCliques() {
        runOnUiThread {
            statusText.text = "Ação: 5+ cliques"
            Toast.makeText(this, "5+ cliques: ação especial", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            bluetoothGatt?.close()
        } catch (_: SecurityException) {}

        bluetoothGatt = null
    }
}
