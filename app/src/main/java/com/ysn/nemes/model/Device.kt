package com.ysn.nemes.model

data class Device(
        val serviceId: String = "",
        val name: String = "",
        var isConnected: Boolean = false,
        var deviceStatus: String = ""
)
