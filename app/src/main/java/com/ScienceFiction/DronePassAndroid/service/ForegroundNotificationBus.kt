package com.ScienceFiction.DronePassAndroid.service

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal data class ForegroundNotification(
    val title: String,
    val body: String,
    val shapeId: String? = null,
)

internal object AppForegroundState {
    private val startedActivityCount = AtomicInteger(0)

    val isForeground: Boolean
        get() = startedActivityCount.get() > 0

    fun onActivityStarted() {
        startedActivityCount.incrementAndGet()
    }

    fun onActivityStopped() {
        while (true) {
            val current = startedActivityCount.get()
            if (current == 0) return
            if (startedActivityCount.compareAndSet(current, current - 1)) return
        }
    }
}

internal object ForegroundNotificationBus {
    private val _events = MutableSharedFlow<ForegroundNotification>(
        extraBufferCapacity = 1,
    )
    val events: SharedFlow<ForegroundNotification> = _events.asSharedFlow()

    fun publish(notification: ForegroundNotification) {
        _events.tryEmit(notification)
    }
}
