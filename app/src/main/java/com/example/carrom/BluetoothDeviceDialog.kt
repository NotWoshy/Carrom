package com.example.carrom

import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class BluetoothDeviceDialog : DialogFragment() {

    interface BluetoothDeviceDialogListener {
        fun onDeviceSelected(device: BluetoothDevice)
        fun onRescanRequested()
    }

    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_bluetooth_devices, null)

        val listView = view.findViewById<android.widget.ListView>(R.id.deviceListView)
        val rescanBtn = view.findViewById<android.widget.Button>(R.id.btnRescan)

        adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            deviceList.map { device ->
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    device.name ?: device.address
                } else {
                    device.address // fallback si no se tiene permiso
                }
            }
        )
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            (activity as? BluetoothDeviceDialogListener)?.onDeviceSelected(deviceList[position])
            dismiss()
        }

        rescanBtn.setOnClickListener {
            (activity as? BluetoothDeviceDialogListener)?.onRescanRequested()
        }

        return AlertDialog.Builder(requireContext())
            //.setTitle("Dispositivos Bluetooth")
            .setView(view)
            .create()
    }

    fun refreshList() {
        adapter.clear()
        adapter.addAll(deviceList.map { device ->
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED
            ) {
                device.name ?: device.address
            } else {
                device.address
            }
        })
        adapter.notifyDataSetChanged()
    }

    companion object {
        private val deviceList = mutableListOf<BluetoothDevice>()
        private var instance: BluetoothDeviceDialog? = null

        fun addDevice(device: BluetoothDevice) {
            if (device !in deviceList) {
                deviceList.add(device)
                instance?.refreshList()
            }
        }

        fun clearDevices() {
            deviceList.clear()
            instance?.refreshList()
        }

        fun show(manager: androidx.fragment.app.FragmentManager, listener: BluetoothDeviceDialogListener) {
            val dialog = BluetoothDeviceDialog()
            instance = dialog
            dialog.show(manager, "BluetoothDeviceDialog")
        }
    }
}

