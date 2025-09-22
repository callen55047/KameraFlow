package com.egan.core.patterns.state

import kotlin.reflect.KClass

typealias StatePair<Data> = Pair<OutState<Data>, InState<Data>>

abstract class TransitionRegistry : ITransitionRegistry {
    private val _states = mutableSetOf<InState<*>>()
    private val _transitions = mutableSetOf<ATransition<*>>()

    override val states: Set<InState<*>> = _states
    override val edges: Set<ATransition<*>> = _transitions

    override fun <A, Event : IEvent> StatePair<A>.after(event: Event): ATransition<A> {
        val transition = ITransition.from(first, second, event)
        _transitions.add(transition)
        return transition
    }

    override fun <A> OutState<A>.into(next: InState<A>): StatePair<A> {
        _states.add(this)
        _states.add(next)

        return StatePair(this, next)
    }

    override fun <Data> startingFeatureFor(state: KClass<out InState<Data>>): OutState<Data> {
        val registeredState = getState(state)
        val startingState: OutState<Data> = StartingFeature()
        val edge = ITransition.from(startingState, registeredState, IEvent.SUCCESS)

        _transitions.add(edge)

        return startingState
    }

    override fun toString(): String = _transitions.toString()

    class StartingFeature<Data> : IState<Any, Data>() {
        override suspend fun process(data: Any) {}

        override fun toString(): String = "StartingFeature<Data>"
    }

    companion object {
        fun create(): ITransitionRegistry {
            return object : TransitionRegistry() {}
        }
    }
}

interface ITransitionRegistry {
    val states: Set<InState<*>>
    val edges: Set<ATransition<*>>

    infix fun <A> OutState<A>.into(next: InState<A>): StatePair<A> = StatePair(this, next)

    infix fun <A, Event : IEvent> StatePair<A>.after(event: Event): ATransition<A> = ITransition.from(first, second, event)

    fun <Data> startingFeatureFor(state: KClass<out InState<Data>>): OutState<Data>

    fun register(edges: ITransitionRegistry.() -> Unit) {
        this.edges()
    }

    fun <Data> getState(state: KClass<out InState<Data>>): InState<Data> {
        return states
            .filterIsInstance<InState<Data>>()
            .firstOrNull { it::class == state }
            ?: throw UnregisteredFeatureException(state)
    }

    fun <Data> getEdgeOrNull(
        current: InState<Data>,
        onEvent: IEvent,
    ): ATransition<Data>? {
        return edges
            .filterIsInstance<ATransition<Data>>()
            .firstOrNull { it.initial == current && onEvent == it.onEvent }
    }
}

class UnregisteredFeatureException(state: KClass<out IState<*, *>>) :
    Exception("Workflows must start with a registered feature. No Registered feature of: $state")