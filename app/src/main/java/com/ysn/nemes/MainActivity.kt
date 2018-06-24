package com.ysn.nemes

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.Toast
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.ysn.nemes.adapter.AdapterDevices
import com.ysn.nemes.model.Device
import com.ysn.nemes.utils.SerializationHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = javaClass.simpleName
    private var isCreatorRoom = false
    private lateinit var endpointIdConnected: String
    private lateinit var endpointNameConnected: String
    private var devices = mutableListOf<Device>()
    private lateinit var adapterDevices: AdapterDevices
    private val requestCodePermission = 100
    private val serviceId = BuildConfig.APPLICATION_ID
    private val manufacturer = Build.MANUFACTURER
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionResult(endpointId: String, connectionResolution: ConnectionResolution) {
            when (connectionResolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    endpointIdConnected = endpointId
                    setStatusConnected()
                }
                else -> {
                    /* nothing to do in here */
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            devices.clear()
            adapterDevices.notifyDataSetChanged()
            initDefaultView()
            isCreatorRoom = false
        }

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            val alertDialogConfirmation = AlertDialog.Builder(this@MainActivity)
            alertDialogConfirmation.setTitle("Info")
            alertDialogConfirmation.setCancelable(false)
            alertDialogConfirmation.setMessage("Do you want to accept connection ${connectionInfo.endpointName}?")
            alertDialogConfirmation.setPositiveButton("Accept", { _, _ ->
                Nearby.getConnectionsClient(this@MainActivity).stopAdvertising()
                Nearby.getConnectionsClient(this@MainActivity)
                        .acceptConnection(endpointId, object : PayloadCallback() {
                            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                                when (payload.type) {
                                    Payload.Type.BYTES -> {
                                        val dataBytes = payload.asBytes()
                                        val message = SerializationHelper.deserialize(bytes = dataBytes!!).toString()
                                        showToastShort(message = message)
                                    }
                                    Payload.Type.FILE -> {
                                        // TODO: do something in here if data is file
                                    }
                                    Payload.Type.STREAM -> {
                                        // TODO: do something in here if data is stream
                                    }
                                    else -> {
                                        /* nothing to do in here */
                                    }
                                }
                            }

                            override fun onPayloadTransferUpdate(endpointId: String, payloadTransferUpdate: PayloadTransferUpdate) {
                                // TODO: do something in here for update progress transfer if needed
                            }
                        })
                        .addOnSuccessListener {
                            endpointNameConnected = connectionInfo.endpointName
                            if (isCreatorRoom) {
                                val deviceConnected = Device(serviceId = endpointId, name = endpointNameConnected, isConnected = true, deviceStatus = getString(R.string.status_connected))
                                devices.add(deviceConnected)
                                adapterDevices.notifyDataSetChanged()
                            } else {
                                for (index in devices.indices) {
                                    val device = devices[index]
                                    if (device.serviceId == endpointId) {
                                        device.deviceStatus = getString(R.string.status_connected)
                                        device.isConnected = true
                                        devices[index] = device
                                        adapterDevices.notifyDataSetChanged()
                                        break
                                    }
                                }
                            }
                            showToastShort("Connection has been accepted")
                        }
                        .addOnFailureListener {
                            showToastShort(getString(R.string.error_occured))
                        }
            })
            alertDialogConfirmation.setNegativeButton("Reject", { _, _ ->
                Nearby.getConnectionsClient(this@MainActivity)
                        .rejectConnection(endpointId)
                        .addOnSuccessListener {
                            showToastShort("Connection has bee rejected")
                        }
                        .addOnFailureListener {
                            showToastShort(getString(R.string.error_occured))
                        }
            })
            alertDialogConfirmation.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        doCheckPermission()
        setViewManufacturer()
        initListeners()
        initAdapterRecyclerView()
        initDefaultView()
    }

    private fun doCheckPermission() {
        val permissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCodePermission)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            requestCodePermission -> {
                var isAllPermissionAccept = true
                for (index in grantResults.indices) {
                    val grantResult = grantResults[index]
                    when (grantResult) {
                        PackageManager.PERMISSION_DENIED -> {
                            isAllPermissionAccept = false
                        }
                        else -> {
                            /* nothing to do in here */
                        }
                    }
                }
                if (!isAllPermissionAccept) {
                    Snackbar.make(findViewById(android.R.id.content), "Please accept all required permission for launch this app", Snackbar.LENGTH_LONG)
                            .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                    super.onDismissed(transientBottomBar, event)
                                    finish()
                                }
                            })
                            .show()
                }
            }
            else -> {
            }
        }
    }

    private fun setViewManufacturer() {
        text_view_manufacturer.text = manufacturer
    }

    private fun initAdapterRecyclerView() {
        adapterDevices = AdapterDevices(devices = devices, listenerAdapterDevices = object : AdapterDevices.ListenerAdapterDevices {
            override fun onClickItemDevice(deviceSelected: Device) {
                val alertDialogItemDevice = AlertDialog.Builder(this@MainActivity)
                alertDialogItemDevice.setTitle(deviceSelected.name)
                if (deviceSelected.isConnected) {
                    alertDialogItemDevice.setItems(arrayOf("Send Buzz", "Disconnect"), { _, item ->
                        when (item) {
                            0 -> {
                                Nearby.getConnectionsClient(this@MainActivity)
                                        .sendPayload(endpointIdConnected, Payload.fromBytes(SerializationHelper.serialize("Buzz")))
                            }
                            1 -> {
                                disconnectedFromEndpoint()
                            }
                            else -> {
                                /* nothing to do in here */
                            }
                        }
                    })
                    alertDialogItemDevice.show()
                } else if (!deviceSelected.isConnected && deviceSelected.deviceStatus == getString(R.string.status_disconnected)) {
                    for (index in devices.indices) {
                        val device = devices[index]
                        if (device.serviceId == deviceSelected.serviceId) {
                            deviceSelected.deviceStatus = getString(R.string.status_connecting)
                            devices[index] = deviceSelected
                            adapterDevices.notifyDataSetChanged()
                            setStatusConnecting()
                            Nearby.getConnectionsClient(this@MainActivity).stopDiscovery()
                            Nearby.getConnectionsClient(this@MainActivity)
                                    .requestConnection(manufacturer, deviceSelected.serviceId, connectionLifecycleCallback)
                            break
                        }
                    }
                }
            }
        }, context = this)
        recycler_view_devices.layoutManager = LinearLayoutManager(this)
        recycler_view_devices.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view_devices.adapter = adapterDevices
    }

    private fun disconnectedFromEndpoint() {
        Nearby.getConnectionsClient(this@MainActivity)
                .disconnectFromEndpoint(endpointIdConnected)
        devices.clear()
        adapterDevices.notifyDataSetChanged()
        initDefaultView()
        isCreatorRoom = false
    }

    private fun setStatusDisconnected() {
        text_view_status_connection.text = getString(R.string.status_connection_s, getString(R.string.status_disconnected))
    }

    private fun setStatusConnecting() {
        text_view_status_connection.text = getString(R.string.status_connection_s, getString(R.string.status_connecting))
    }

    private fun setStatusConnected() {
        text_view_status_connection.text = getString(R.string.status_connection_s, getString(R.string.status_connected))
    }

    private fun initDefaultView() {
        linear_layout_container_not_connected.visibility = View.VISIBLE
        linear_layout_container_reset_connection.visibility = View.GONE
        setStatusDisconnected()
        devices.clear()
        adapterDevices.notifyDataSetChanged()
    }

    private fun initListeners() {
        button_create_room.setOnClickListener(this)
        button_discovery_room.setOnClickListener(this)
        button_reset_connection.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_create_room -> {
                Nearby.getConnectionsClient(this)
                        .startAdvertising(manufacturer, serviceId, connectionLifecycleCallback, AdvertisingOptions(Strategy.P2P_STAR))
                        .addOnSuccessListener {
                            isCreatorRoom = true
                            setViewCreatedRoom()
                            showToastShort("Room has been created")
                        }
                        .addOnFailureListener {
                            initDefaultView()
                            showToastShort("Create a room failed")
                        }
            }
            R.id.button_discovery_room -> {
                Nearby.getConnectionsClient(this)
                        .startDiscovery(serviceId, object : EndpointDiscoveryCallback() {
                            override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
                                val deviceFounded = Device(
                                        serviceId = endpointId,
                                        name = discoveredEndpointInfo.endpointName,
                                        isConnected = false,
                                        deviceStatus = getString(R.string.status_disconnected)
                                )
                                addItemDevice(deviceFounded)
                            }

                            override fun onEndpointLost(endpointId: String) {
                                removeItemDevice(endpointId)
                            }
                        }, DiscoveryOptions(Strategy.P2P_STAR))
                        .addOnSuccessListener {
                            isCreatorRoom = false
                            setViewDiscoveryRoom()
                            showToastShort("Discovery room success")
                        }
                        .addOnFailureListener {
                            initDefaultView()
                            showToastShort("Discovery room failed")
                        }
            }
            R.id.button_reset_connection -> {
                Nearby.getConnectionsClient(this)
                        .stopAllEndpoints()
                Nearby.getConnectionsClient(this)
                        .stopAdvertising()
                Nearby.getConnectionsClient(this)
                        .stopDiscovery()
                initDefaultView()
                isCreatorRoom = false
            }
            else -> {
                /* nothing to do in here */
            }
        }
    }

    private fun removeItemDevice(endpointId: String) {
        devices.forEachIndexed { index, device ->
            if (device.serviceId == endpointId) {
                devices.removeAt(index)
                adapterDevices.notifyDataSetChanged()
            }
        }
    }

    fun addItemDevice(deviceFounded: Device) {
        var isAlreadyExist = false
        for (device in devices) {
            if (device.serviceId == deviceFounded.serviceId) {
                isAlreadyExist = true
                break
            }
        }
        if (!isAlreadyExist) {
            devices.add(deviceFounded)
            adapterDevices.notifyDataSetChanged()
        }
    }

    private fun setViewDiscoveryRoom() {
        linear_layout_container_not_connected.visibility = View.INVISIBLE
        linear_layout_container_reset_connection.visibility = View.VISIBLE
    }

    private fun setViewCreatedRoom() {
        linear_layout_container_not_connected.visibility = View.INVISIBLE
        linear_layout_container_reset_connection.visibility = View.VISIBLE
    }

    fun Activity.showToastShort(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT)
                .show()
    }

}
