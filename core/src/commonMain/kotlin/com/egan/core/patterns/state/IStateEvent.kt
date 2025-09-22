package com.egan.core.patterns.state

import kotlin.reflect.KClass

interface IStateEvent<Data> {
    val state: KClass<out IState<*, *>>
    val data: Data
    val event: IEvent

    companion object {
        fun <Data> eventFor(
            state: KClass<out IState<*, Data>>,
            data: Data,
            event: IEvent,
        ): IStateEvent<*> = externalEventFor(state, data, event)

        fun <Data> externalEventFor(
            state: KClass<out IState<*, *>>,
            data: Data,
            event: IEvent,
        ): IStateEvent<*> =
            object : PrintableStateEvent<Data>() {
                override val name: String = "eventFor(IState, Data, IEvent)"
                override val state: KClass<out IState<*, *>> = state
                override val data: Data = data
                override val event: IEvent = event
            }
    }
}

abstract class PrintableStateEvent<Data> : IStateEvent<Data> {
    abstract val name: String

    override fun toString(): String {
        return StringBuilder("Factory: $name $ ").apply {
            append("State: $state $ ")
            append("Data: $data $ ")
            append("Event: $event")
        }.toString()
    }
}

@Suppress("unused")
interface IProgressEvent<Progress> : IStateEvent<Progress> {
    val currentProgress: Progress
    val maxProgress: Progress
}

class InitialStateData<Data>(override val data: Data) : IStateEvent<Data> {
    override val state: KClass<out IState<*, *>> = StateMachine.Companion.EmptyState::class
    override val event: IEvent = IEvent.SUCCESS
}