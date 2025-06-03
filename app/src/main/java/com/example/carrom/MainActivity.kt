package com.example.carrom

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.ActivityInfo
import com.example.carrom.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), BluetoothDeviceDialog.BluetoothDeviceDialogListener {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothReceiver: BluetoothReceiver
    private lateinit var intentFilter: IntentFilter

    private var selectedTarget: String = ""

    private val permissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }

        bluetoothReceiver = BluetoothReceiver { device ->
            BluetoothDeviceDialog.addDevice(device)
        }

        intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)

        findViewById<Button>(R.id.btnESP32).setOnClickListener {
            selectedTarget = "ESP32"
            showDeviceDialog()
        }

        findViewById<Button>(R.id.btnArduino).setOnClickListener {
            selectedTarget = "Arduino"
            showDeviceDialog()
        }

        findViewById<Button>(R.id.btnRaspberry).setOnClickListener {
            selectedTarget = "Raspberry"
            showDeviceDialog()
        }

        findViewById<Button>(R.id.btnMaxV).setOnClickListener {
            selectedTarget = "MaxV"
            showDeviceDialog()
        }

    }

    private fun hasPermissions(): Boolean = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun showDeviceDialog() {
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, 1)
            return
        }

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if (!bluetoothAdapter.isEnabled) {
                    Toast.makeText(this, "Bluetooth no activado", Toast.LENGTH_SHORT).show()
                    return
                }

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()

                    BluetoothDeviceDialog.clearDevices()
                    registerReceiver(bluetoothReceiver, intentFilter)
                    bluetoothAdapter.startDiscovery()

                    BluetoothDeviceDialog.show(supportFragmentManager, this)
                } else {
                    Toast.makeText(this, "Permiso BLUETOOTH_SCAN no concedido", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Permiso BLUETOOTH_CONNECT no concedido", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "Error de permisos Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeviceSelected(device: BluetoothDevice) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
            }
            unregisterReceiver(bluetoothReceiver)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        val intent = when (selectedTarget) {
            "ESP32" -> Intent(this, ESP32Activity::class.java)
            "Arduino" -> Intent(this, ArduinoActivity::class.java)
            "Raspberry" -> Intent(this, RaspberryActivity::class.java)
            "MaxV" -> Intent(this, MaxVActivity::class.java)
            else -> return
        }

        intent.putExtra("DEVICE_NAME", device.name)
        intent.putExtra("DEVICE_ADDRESS", device.address)
        startActivity(intent)
    }

    override fun onRescanRequested() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
                BluetoothDeviceDialog.clearDevices()
                bluetoothAdapter.startDiscovery()
            } else {
                Toast.makeText(this, "Permiso BLUETOOTH_SCAN no concedido", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

}