package com.egan.core.patterns.state

interface IEvent {
    fun asString(): String = "IEvent::${this::class.simpleName!!}"

    object START : IEvent {
        override fun toString() = asString()
    }

    object SUCCESS : IEvent {
        override fun toString() = asString()
    }

    object FAIL : IEvent {
        override fun toString() = asString()
    }

    object PROGRESS : IEvent {
        override fun toString() = asString()
    }

    object RETRY : IEvent {
        override fun toString() = asString()
    }
}