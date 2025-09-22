package com.egan.core.patterns

import kotlin.reflect.KClass

/**
 * Interface representing a registry for dependency injection.
 */
interface IRegistry {
    /**
     * The registry map containing class types and their corresponding injection instances.
     */
    val registry: MutableMap<KClass<*>, InjectionInstance>
}

/**
 * Registers a singleton instance in the dependency injection system.
 *
 * @param S The type of the singleton instance.
 * @param provider A function that provides the instance.
 * @return An InjectionInstance representing a singleton.
 */
inline fun <reified S : Any> singleton(noinline provider: () -> S): InjectionInstance =
    InjectionInstance(S::class, InjectionInstance.Storage.SINGLETON, Provider.Singleton(lazy(provider)))

/**
 * Registers a factory instance in the dependency injection system.
 *
 * @param S The type of the factory instance.
 * @param provider A function that provides a new instance.
 * @return An InjectionInstance representing a factory.
 */
inline fun <reified S : Any> factory(noinline provider: () -> S): InjectionInstance =
    InjectionInstance(S::class, InjectionInstance.Storage.FACTORY, Provider.Factory(provider))

/**
 * Defines how an instance is provided, either as a Singleton or a Factory.
 */
sealed class Provider {
    /**
     * Retrieves an instance of the provided object.
     *
     * @return The instance of the object.
     */
    abstract fun getInstance(): Any

    /**
     * Provides a singleton instance, initialized lazily.
     */
    class Singleton(private val instance: Lazy<Any>) : Provider() {
        override fun getInstance(): Any = instance.value
    }

    /**
     * Provides a new instance each time it is requested.
     */
    class Factory(private val creator: () -> Any) : Provider() {
        override fun getInstance(): Any = creator()
    }
}

/**
 * Represents an instance registered for dependency injection.
 */
data class InjectionInstance(
    val clazz: KClass<*>,
    val storage: Storage,
    val provider: Provider,
) {
    /**
     * Enum representing storage types for dependency injection.
     */
    enum class Storage {
        SINGLETON,
        FACTORY,
    }
}

/**
 * Custom exceptions for dependency injection errors.
 */
sealed class DIException(message: String) : Error(message)

/**
 * Thrown when a requested class is not registered.
 */
class NotRegistered(clazz: KClass<*>) : DIException("No registered instance for " + clazz.simpleName)

/**
 * Dependency Injection Container.
 */
object Injection {
    private val cache = mutableMapOf<KClass<*>, Any>()
    private val registry = mutableMapOf<KClass<*>, InjectionInstance>()

    /**
     * Registers instances into the DI container.
     *
     * @param instances Vararg list of InjectionInstances to register.
     */
    fun register(vararg instances: InjectionInstance) {
        instances.forEach { instance ->
            registry[instance.clazz] = instance
        }
    }

    /**
     * Clears cached instances. Does not clear registry as registry supports the
     * ability to override
     *
     * @return true if the cache is empty after clearing.
     */
    fun unload(): Boolean =
        cache.run {
            clear()
            isEmpty()
        }

    /**
     * Populates the registry with provided instances.
     * Accepts either InjectionInstances or Maps containing them.
     *
     * @param instances Vararg list of instances or instance maps.
     * @return A mutable map representing the registry.
     */
    fun registryOf(vararg instances: Any): MutableMap<KClass<*>, InjectionInstance> =
        mutableMapOf<KClass<*>, InjectionInstance>().apply {
            instances.forEach {
                when (it) {
                    is InjectionInstance -> this[it.clazz] = it
                    is Map<*, *> ->
                        @Suppress("UNCHECKED_CAST")
                        (it as Map<KClass<*>, InjectionInstance>).forEach { entry ->
                            this[entry.key] = entry.value
                        }
                }
            }
        }.also {
            register(*it.values.toTypedArray())
        }

    /**
     * Retrieves an instance of the specified class from the registry.
     *
     * @param clazz The class type to retrieve.
     * @param T The generic type parameter.
     * @return The retrieved instance.
     * @throws NotRegistered if the class is not registered.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: KClass<T>): T =
        registry[clazz]?.let { instance ->
            when (instance.storage) {
                InjectionInstance.Storage.SINGLETON -> cache.getOrPut(clazz) { instance.provider.getInstance() }
                InjectionInstance.Storage.FACTORY -> instance.provider.getInstance()
            }
        } as? T ?: throw NotRegistered(clazz)

    /**
     * Lazily injects an instance of the specified class.
     *
     * @param T The generic type parameter.
     * @return A lazy instance of the specified class.
     */
    inline fun <reified T : Any> inject(): Lazy<T> =
        lazy {
            get(T::class)
        }

    /**
     * Retrieves an instance of the specified class immediately.
     *
     * @param T The generic type parameter.
     * @return The retrieved instance.
     */
    inline fun <reified T : Any> using(): T = get(T::class)
}