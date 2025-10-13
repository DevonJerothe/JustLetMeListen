package com.devonjerothe.justletmelisten.domain

import android.content.Context
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.Observer
import kotlinx.coroutines.flow.MutableStateFlow

enum class CarConnectionStatus {
    CONNECTED, // default android auto
    CONNECTED_OS, // android auto OS
    NOT_CONNECTED, // no android auto
    DISCONNECTED // was connected but not anymore

}


class CarConnectionManager(context: Context) {
    private val carConnection = CarConnection(context)

    private val _connectionStatus = MutableStateFlow(CarConnectionStatus.NOT_CONNECTED)
    val connectionStatus = _connectionStatus

    private val carConnectionObserver = Observer<Int> { connection ->
        _connectionStatus.value = when (connection) {
            CarConnection.CONNECTION_TYPE_NATIVE -> CarConnectionStatus.CONNECTED
            CarConnection.CONNECTION_TYPE_PROJECTION -> CarConnectionStatus.CONNECTED_OS
            else -> {
                // Check if we are disconnecting from a previous connection
                if (_connectionStatus.value == CarConnectionStatus.CONNECTED ||
                    _connectionStatus.value == CarConnectionStatus.CONNECTED_OS
                ) {
                    CarConnectionStatus.DISCONNECTED
                } else {
                    CarConnectionStatus.NOT_CONNECTED
                }
            }
        }
    }

    init {
        carConnection.type.observeForever(carConnectionObserver)
    }

    fun isCarConnected(): Boolean {
        return connectionStatus.value != CarConnectionStatus.NOT_CONNECTED
    }

    fun destroy() {
        carConnection.type.removeObserver(carConnectionObserver)
    }

}

