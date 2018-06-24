package com.ysn.nemes.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.ysn.nemes.R
import com.ysn.nemes.model.Device

class AdapterDevices constructor(private val devices: MutableList<Device>,
                                 private val listenerAdapterDevices: ListenerAdapterDevices,
                                 private val context: Context) : RecyclerView.Adapter<AdapterDevices.ViewHolderDevice>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderDevice {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, null)
        return ViewHolderDevice(view)
    }

    override fun onBindViewHolder(holder: ViewHolderDevice, position: Int) {
        devices[position].let {
            holder.textViewDeviceName?.text = it.name
            holder.textViewServiceId?.text = it.serviceId
            holder.textViewDeviceStatus?.text = if (!it.isConnected) {
                it.deviceStatus
            } else {
                context.getString(R.string.status_connected)
            }
        }
    }

    override fun getItemCount(): Int = devices.size

    inner class ViewHolderDevice constructor(itemView: View?) : RecyclerView.ViewHolder(itemView) {
        private val linearLayoutRoot = itemView?.findViewById<LinearLayout>(R.id.linear_layout_root_item_device)
        val textViewDeviceName = itemView?.findViewById<TextView>(R.id.text_view_device_name)
        val textViewServiceId = itemView?.findViewById<TextView>(R.id.text_view_service_id)
        val textViewDeviceStatus = itemView?.findViewById<TextView>(R.id.text_view_device_status)

        init {
            linearLayoutRoot?.setOnClickListener {
                listenerAdapterDevices.onClickItemDevice(deviceSelected = devices[adapterPosition])
            }
        }
    }

    interface ListenerAdapterDevices {

        fun onClickItemDevice(deviceSelected: Device)

    }

}