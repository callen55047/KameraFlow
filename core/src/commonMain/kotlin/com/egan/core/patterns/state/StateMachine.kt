package com.egan.core.patterns.state

import com.egan.core.patterns.state.IEvent.SUCCESS
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transform
import kotlin.reflect.KClass

internal typealias OutState<A> = IState<*, A>
internal typealias InState<A> = IState<A, *>
internal typealias ATransition<A> = ITransition<A, *>

data class StateContext(
    val states: MutableStateFlow<IState<*, *>>,
    val events: MutableStateFlow<IStateEvent<*>>,
)

abstract class StateMachine : IStateMachine {
    override val transitions: ITransitionRegistry = TransitionRegistry.create()
    private val externalEvents = MutableSharedFlow<IStateEvent<*>>()
    private val currentState = MutableStateFlow<IState<*, *>>(EmptyState)
    private val featureResponses = MutableSharedFlow<IStateResponseData>(replay = 1)

    override fun currentState(): IState<*, *> = currentState.value

    override fun flowOfStates(): Flow<IState<*, *>> = currentState

    override suspend fun <Data : Any> sendEventForFeature(
        data: Data,
        feature: KClass<out IState<*, *>>,
        event: IEvent,
    ) {
        externalEvents.emit(IStateEvent.externalEventFor(feature, data, event))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : IStateResponseData> subscribeToResponse(clazz: KClass<T>): Flow<IStateResponseData> {
        return featureResponses.filter { clazz.isInstance(it) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun <Data> startWith(
        data: Data,
        state: KClass<out InState<Data>>,
    ): Flow<IStateEvent<*>> {
        externalEvents.resetReplayCache()
        currentState.value = EmptyState

        val current: IState<*, Data> = transitions.startingFeatureFor(state)
        val stateContext = StateContext(MutableStateFlow(current), MutableStateFlow(InitialStateData(data)))

        return launchFlow(stateContext.states, stateContext.events)
    }

    private fun launchFlow(
        states: MutableStateFlow<IState<*, *>>,
        results: MutableStateFlow<IStateEvent<*>>,
    ): Flow<IStateEvent<*>> {
        var prevState: IState<*, *>? = null
        var prevEvent: IStateEvent<*>? = null

        var complete = false

        return results.combineTransform(states) { stateEvent, state ->
            currentState.emit(state)

            val oldEventForNextFeature = state != prevState && stateEvent == prevEvent
            if (oldEventForNextFeature) return@combineTransform

            prevState = state
            prevEvent = stateEvent

            prevState?.close()

            val resultTransition = transitions.getEdgeOrNull(state, stateEvent.event)

            val outputEvent =
                if (resultTransition != null) {
                    resultTransition.resulting.start(results, externalEvents, featureResponses)

                    states.value = resultTransition.resulting

                    state.featureEvent(stateEvent.data, stateEvent.event)
                } else {
                    val eventType = if (stateEvent.event is SUCCESS) COMPLETE else stateEvent.event
                    state.featureEvent(stateEvent.data, eventType)
                }

            if (outputEvent.state != TransitionRegistry.StartingFeature::class) {
                emit(outputEvent)
            }
        }
            .takeWhile { !complete }
            .transform {
                if (it.event == COMPLETE) complete = true

                emit(it)
            }
    }

    data object COMPLETE : IEvent {
        override fun toString() = "COMPLETE"
    }

    private fun flowComplete(event: IEvent) = CancellationException("Flow Completed on Event: $event")

    private fun <Data> IState<*, *>.featureEvent(
        data: Data,
        event: IEvent,
    ): PrintableStateEvent<Data> = CachedFeatureEvent(this::class, data, event)

    private class CachedFeatureEvent<Data>(
        override val state: KClass<out IState<*, *>>,
        override val data: Data,
        override val event: IEvent,
    ) : PrintableStateEvent<Data>() {
        override val name: String = "IFeature.featureEvent(Data, IEvent)"
    }

    companion object {
        fun of(builder: StateMachine.() -> Unit = {}): StateMachine {
            return object : StateMachine() {}.apply(builder)
        }

        object EmptyState : IState<Any, Any>() {
            override suspend fun process(data: Any) {
                resultEvent(Any(), SUCCESS)
            }
        }
    }
}

interface IStateMachine {
    val transitions: ITransitionRegistry

    fun currentState(): IState<*, *>

    fun flowOfStates(): Flow<IState<*, *>>

    suspend fun <Data : Any> sendEventForFeature(
        data: Data,
        feature: KClass<out IState<*, *>>,
        event: IEvent,
    )

    fun <T : IStateResponseData> subscribeToResponse(clazz: KClass<T>): Flow<IStateResponseData>

    fun register(registration: ITransitionRegistry.() -> Unit) {
        transitions.registration()
    }

    fun <Data> startWith(
        data: Data,
        state: KClass<out InState<Data>>,
    ): Flow<IStateEvent<*>>
}

suspend fun Flow<IStateEvent<*>>.completeEvent() = this.first { it.event == StateMachine.COMPLETE }