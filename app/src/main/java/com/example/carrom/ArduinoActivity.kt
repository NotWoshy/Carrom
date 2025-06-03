package com.example.carrom

import android.Manifest
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.controlwear.virtual.joystick.android.JoystickView
import android.content.pm.ActivityInfo
import android.widget.ImageButton

import java.util.*

class ArduinoActivity : AppCompatActivity() {

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val serviceUUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val characteristicUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    private var sensorState = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arduino)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val deviceName = intent.getStringExtra("DEVICE_NAME")
        val deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")

        val txtStatus = findViewById<TextView>(R.id.txtArduino)
        val joystick = findViewById<JoystickView>(R.id.joystickView)
        val toggleSensor: ImageButton = findViewById(R.id.btnToggleSensor)
        val sensorStatusText: TextView = findViewById(R.id.txtSensorStatus)

        joystick.isEnabled = false
        toggleSensor.isEnabled = false

        if (deviceAddress == null) {
            Toast.makeText(this, "Dispositivo no recibido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        try {
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

            txtStatus.text = if (deviceName != null) {
                "Conectando a $deviceName..."
            } else {
                "Conectando..."
            }

            connectToDevice(device, txtStatus, joystick, toggleSensor)

            toggleSensor.setOnClickListener {
                sensorState = !sensorState
                val comando = if (sensorState) "ON\n" else "OFF\n"
                sendCommand(comando)

                val nuevoIcono = if (sensorState) R.drawable.ico_sensor_on else R.drawable.ico_sensor_off
                toggleSensor.setImageResource(nuevoIcono)

                if (sensorState) {
                    sensorStatusText.text = "Sensor de proximidad: Activado!"
                    sensorStatusText.setTextColor(ContextCompat.getColor(this, R.color.sensor_on))
                } else {
                    sensorStatusText.text = "Sensor de proximidad: Desactivado"
                    sensorStatusText.setTextColor(ContextCompat.getColor(this, R.color.sensor_off))
                }
            }

            joystick.setOnMoveListener({ angle, strength ->
                val comando = when {
                    strength == 0 -> "STOP\n"
                    angle in 45..135 -> "UP:${strength}\n"
                    angle in 135..225 -> "LEFT:${strength}\n"
                    angle in 225..315 -> "DOWN:${strength}\n"
                    else -> "RIGHT:${strength}\n"
                }
                sendCommand(comando)
            }, 200)

        } catch (e: SecurityException) {
            Toast.makeText(this, "Permisos insuficientes para acceder al dispositivo", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun connectToDevice(
        device: BluetoothDevice,
        txtStatus: TextView,
        joystick:  JoystickView,
        toggleSensor: ImageButton
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso BLUETOOTH_CONNECT no concedido", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        runOnUiThread {
                            txtStatus.text = "Conectando a ${device.name}..."
                        }
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        runOnUiThread {
                            txtStatus.text = "Desconectado"
                            Toast.makeText(applicationContext, "Conexión perdida", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        try {
                            val service = gatt.getService(serviceUUID)
                            val characteristic = service?.getCharacteristic(characteristicUUID)

                            if (characteristic != null) {
                                writeCharacteristic = characteristic
                                runOnUiThread {
                                    txtStatus.text = "Conectado a ${device.name}"
                                    joystick.isEnabled = true
                                    toggleSensor.isEnabled= true
                                }
                            } else {
                                runOnUiThread { txtStatus.text = "Característica no encontrada" }
                            }
                        } catch (e: SecurityException) {
                            runOnUiThread {
                                txtStatus.text = "Permiso denegado al acceder a servicios"
                            }
                            e.printStackTrace()
                        }
                    }
                }

                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?,
                    status: Int
                ) {
                    runOnUiThread {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            //Toast.makeText(applicationContext, "Comando enviado", Toast.LENGTH_SHORT).show()
                        } else {
                            //Toast.makeText(applicationContext, "Error al escribir", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error de permisos al conectar", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun sendCommand(command: String) {
        try {
            val characteristic = writeCharacteristic ?: return
            characteristic.setValue(command)
            bluetoothGatt?.writeCharacteristic(characteristic)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permiso denegado para escribir", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothGatt?.close()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        bluetoothGatt = null
    }
}
