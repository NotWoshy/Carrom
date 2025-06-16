package com.example.carrom

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.controlwear.virtual.joystick.android.JoystickView
import android.content.pm.ActivityInfo
import java.io.OutputStream
import java.util.*

class RaspberryActivity: AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private var outputStream: OutputStream? = null
    private var bluetoothSocket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_raspberry)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val deviceName = intent.getStringExtra("DEVICE_NAME")
        val deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")
        val joystick = findViewById<JoystickView>(R.id.joystickView)

        val txtStatus = findViewById<TextView>(R.id.txtRaspberry)
        val toggleSensor: ImageButton = findViewById(R.id.btnToggleSensor)
        val sensorStatusText: TextView = findViewById(R.id.txtSensorStatus)
        var sensorState = true

        joystick.isEnabled = false
        toggleSensor.isEnabled = false

        if (deviceAddress == null) {
            Toast.makeText(this, "Dispositivo no recibido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        txtStatus.text = "Conectando a $deviceName..."
        device?.let { connectToDevice(it, txtStatus, joystick, toggleSensor) }

        joystick.setOnMoveListener({ angle, strength ->
            val command = when (angle) {
                in 45..135 -> "UP:$strength"
                in 136..225 -> "LEFT:$strength"
                in 226..315 -> "DOWN:$strength"
                else -> "RIGHT:$strength"
            }

            if (strength == 0) {
                sendCommand("STOP\n")
            } else {
                sendCommand("$command\n")
            }
        }, 200)

        toggleSensor.setOnClickListener {
            sensorState = !sensorState
            val nuevoIcono = if (sensorState) R.drawable.ico_sensor_on else R.drawable.ico_sensor_off
            toggleSensor.setImageResource(nuevoIcono)

            if (sensorState) {
                sendCommand("ON\n")
                sensorStatusText.text = "Sensor de proximidad: Activado!"
                sensorStatusText.setTextColor(ContextCompat.getColor(this, R.color.sensor_on))
            } else {
                sendCommand("OFF\n")
                sensorStatusText.text = "Sensor de proximidad: Desactivado"
                sensorStatusText.setTextColor(ContextCompat.getColor(this, R.color.sensor_off))
            }
        }
    }

    private fun connectToDevice(
        device: BluetoothDevice,
        txtStatus: TextView,
        joystickView: JoystickView,
        toggleSensor: ImageButton
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso BLUETOOTH_CONNECT no concedido", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                // Usar canal RFCOMM 1 directamente (igual que Serial Bluetooth Terminal)
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                bluetoothSocket = method.invoke(device, 1) as BluetoothSocket

                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()

                outputStream = bluetoothSocket?.outputStream

                runOnUiThread {
                    txtStatus.text = "Conectado a ${device.name}"
                    joystickView.isEnabled = true
                    toggleSensor.isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    txtStatus.text = "Error al conectar con ${device.name}"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun sendCommand(command: String) {
        Thread {
            try {
                outputStream?.write(command.toByteArray())
                outputStream?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error al enviar comando", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothSocket?.close()
        outputStream?.close()
    }
}