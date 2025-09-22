package com.egan.core.patterns.state

interface ITransition<Data, Event : IEvent> {
    val initial: OutState<Data>
    val resulting: InState<Data>
    val onEvent: Event

    companion object {
        fun <A> from(
            from: OutState<A>,
            to: InState<A>,
            event: IEvent,
        ): ATransition<A> {
            return object : ITransition<A, IEvent> {
                override val initial: OutState<A> = from
                override val resulting: InState<A> = to
                override val onEvent: IEvent = event

                override fun toString(): String =
                    """
                    Transition from: $from || to: ${to::class.simpleName} || after: $onEvent
                    """.trimIndent()
            }
        }
    }
}