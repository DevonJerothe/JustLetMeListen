package com.devonjerothe.justletmelisten.core

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Modern app lifecycle observer using ProcessLifecycleOwner
 * Provides reactive state for app foreground/background status
 */
class AppLifecycleObserver : LifecycleEventObserver {

    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground

    init {
        // Observe the app's lifecycle
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                _isForeground.value = true
            }
            Lifecycle.Event.ON_STOP -> {
                _isForeground.value = false
            }
            else -> {
                // Ignore other events
            }
        }
    }

    /**
     * Clean up observer when no longer needed
     */
    fun destroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }
}
