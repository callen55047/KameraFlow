package com.egan.core

import com.egan.core.patterns.EventFlow
import com.egan.core.patterns.IEventFlow
import com.egan.core.patterns.IRegistry
import com.egan.core.patterns.Injection.registryOf
import com.egan.core.patterns.InjectionInstance
import com.egan.core.patterns.singleton
import kotlin.reflect.KClass

object Core : ICore, IRegistry {
    override val registry =
        registryOf(
            singleton<IEventFlow> { EventFlow() }
        )
}

interface ICore {
    fun initialize() {
        // setup logic here
    }

    fun operation(arg: String): Boolean {
        if (arg == "test") {
            return true
        }

        return false
    }
}