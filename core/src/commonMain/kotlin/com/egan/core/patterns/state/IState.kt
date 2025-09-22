package com.egan.core.patterns.state

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

interface IOutputFeature<Result> {
    fun resultEvent(
        data: Result,
        event: IEvent,
    )
}

interface IInputFeature<in Input : Any?> {
    suspend fun process(data: Input)
}

interface IStateResponseData

abstract class IState<in Input : Any?, Result> : IOutputFeature<Result>, IInputFeature<Input> {
    protected lateinit var currentScope: CoroutineScope
    private lateinit var eventFeed: SharedFlow<IStateEvent<*>>
    private lateinit var results: MutableStateFlow<IStateEvent<*>>
    private lateinit var dataOutput: MutableSharedFlow<IStateResponseData>

    abstract override suspend fun process(data: Input)

    open fun onStart(data: Input) {}

    open fun onEvent(event: IStateEvent<*>) {}

    open fun onClose() {}

    open fun onCoroutineError(
        context: CoroutineContext,
        throwable: Throwable,
    ) {}

    open fun SharedFlow<IStateEvent<*>>.transformEvents(): Flow<IStateEvent<*>> = this

    @Suppress("UNCHECKED_CAST") // Type checking is handled with Edge registration
    suspend fun start(
        resultFlow: MutableStateFlow<IStateEvent<*>>,
        receivedEvents: SharedFlow<IStateEvent<*>>,
        emittedData: MutableSharedFlow<IStateResponseData>,
    ) {
        currentScope = CoroutineScope(currentCoroutineContext() + SupervisorJob())

        eventFeed = receivedEvents
        results = resultFlow
        dataOutput = emittedData

        val inputEvent = resultFlow.first() as IStateEvent<Input>
        val input = inputEvent.data

        onStart(input)

        launchWithHandler {
            process(input)
        }
    }

    fun handleEvents(events: (event: IStateEvent<*>) -> Unit): Job {
        val job =
            launchWithHandler {
                eventFeed
                    .transformEvents()
                    .collect { events(it) }
            }

        return job
    }

    fun emitResponseData(data: IStateResponseData): Job {
        val job =
            launchWithHandler {
                dataOutput.emit(data)
            }
        return job
    }

    override fun resultEvent(
        data: Result,
        event: IEvent,
    ) {
        launchWithHandler {
            results.emit(IStateEvent.eventFor(this@IState::class, data, event))
        }
    }

    fun close() {
        println("Closing StateMachine State: $this")

        if (::currentScope.isInitialized) {
            currentScope.cancel(EXPECTED_CANCELLATION_MESSAGE)
        }

        onClose()
    }

    private fun launchWithHandler(work: suspend () -> Unit) = currentScope.launch(coroutineErrorHandler()) { work() }

    protected fun coroutineErrorHandler() =
        CoroutineExceptionHandler { errorContext, throwable ->
            if (throwable.message?.endsWith(EXPECTED_CANCELLATION_MESSAGE) == true) {
                println("Expected Coroutine Cancellation in State: ${this::class.simpleName}")
                return@CoroutineExceptionHandler
            }

            println(
                """
                State: ${this::class.simpleName}
                  CoroutineException in: $errorContext
                    $throwable
                """.trimIndent()
            )
            onCoroutineError(errorContext, throwable)
        }

    companion object {
        const val EXPECTED_CANCELLATION_MESSAGE = "Expected State Cancellation"
    }
}