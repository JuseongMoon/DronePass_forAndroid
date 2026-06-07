package com.ScienceFiction.DronePassAndroid.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal data class ForegroundNotification(
    val title: String,
    val body: String,
    val shapeId: String? = null,
)

internal object ForegroundNotificationBus {
    private val _events = MutableSharedFlow<ForegroundNotification>(
        extraBufferCapacity = 1,
    )
    val events: SharedFlow<ForegroundNotification> = _events.asSharedFlow()

    fun publish(notification: ForegroundNotification) {
        _events.tryEmit(notification)
    }
}
