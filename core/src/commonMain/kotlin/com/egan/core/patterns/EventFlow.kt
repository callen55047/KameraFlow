package com.egan.core.patterns

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlin.reflect.KClass

class EventFlow : IEventFlow()

/**
 * A wrapper for [MutableSharedFlow] used to provide an Event Bus for communication between different
 * layers of the Client. This is exposed through the [eventFlow] property, which is a raw unfiltered stream of events.
 * Also is exposed via the [subscribeTo] method that will filter events to any implementor of the [IEventParcel]
 *
 * [SharedFlow] is a hot, non-closing Flow, so the caller is in charge of termination of the subscription
 *
 */
abstract class IEventFlow {
    private val _eventFlow = MutableSharedFlow<IEventParcel>()

    /**
     * The raw Flow without any modification - Providing all events
     */
    val eventFlow: SharedFlow<IEventParcel> get() = _eventFlow

    /**
     * Synchronously send a new event to the Flow
     *
     * TODO: tryEmit() Should have error handling if it cannot emit
     */
    fun sendEvent(event: IEventParcel) {
        _eventFlow.tryEmit(event)
    }

    /**
     * Asynchronously send a new event to the Flow
     */
    suspend fun sendAsyncEvent(event: IEventParcel) {
        _eventFlow.emit(event)
    }

    /**
     * Using any implementor of the [IEventParcel] will return a pre-filtered flow of events
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : IEventParcel> subscribeTo(): Flow<T> {
        return eventFlow.filter { it is T } as Flow<T>
    }

    /**
     * @param [eventTypes] A list of all Events to watch
     * @return A flow that filters to all passed in [IEventParcel] types. The consumer will still have to check for each
     *   type as they are being fed in
     */
    fun subscribeTo(eventTypes: List<KClass<IEventParcel>>): Flow<IEventParcel> {
        return eventFlow.filter { eventTypes.contains(it::class) }
    }
}