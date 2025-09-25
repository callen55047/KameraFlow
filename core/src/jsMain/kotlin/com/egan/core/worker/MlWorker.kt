package com.egan.core.worker

import io.ktor.client.fetch.fetch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.AbstractWorker
import org.w3c.dom.DedicatedWorkerGlobalScope
import org.w3c.dom.MessageEvent
import org.w3c.dom.WorkerOptions
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.url.URL
import org.w3c.dom.url.URLSearchParams
import org.w3c.files.Blob
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MlWorker {
    private lateinit var worker: TruliooWorker
    suspend fun send(data: WorkerContract): MessageEvent = worker.send(data.forTransfer() as Any)
    suspend fun send(data: WorkerContract, transfer: Array<ArrayBuffer>) =
        worker.send(data.forTransfer() as Any, transfer)

    suspend fun initialize() {
        worker = fetch(CDN_WORKER_URL)
            .then { it.blob() }
            .then { blob -> TruliooWorker(blob) }
            .await()

        val startModel = WorkerContract.FromSdk.StartModel.of(DEFAULT_MODEL_URL)
        val responseMessageEvent = send(startModel)

        val response = WorkerContract.fromMessageToData(responseMessageEvent.data)
    }

    fun isReady(): Boolean = ::worker.isInitialized

    companion object {
        const val DEFAULT_MODEL_URL = "https://cdn.trulioo.com/android/ml/1.0.1/document_detector.tflite"

        // Use INTERNAL for local testing, which will point to the locally hosted web worker file.
        const val INTERNAL = "./modelWorker.js"

        // TODO: DOCV-3575 Proper worker versioning from env variables instead of constant string in code.
        // Important to remember to update the version and sync with the web worker modelWorker.js changes update.
        const val CDN_WORKER_URL = "https://cdn.trulioo.com/js/worker/1.0.4/modelWorker.js"

        private lateinit var _worker: MlWorker

        suspend fun getInstance(): MlWorker {
            if (::_worker.isInitialized) return _worker
            _worker = MlWorker()
            _worker.initialize()

            return _worker
        }
    }
}

class TruliooWorker(blob: Blob) {
    private val worker = Worker(URL.createObjectURL(blob))

    suspend fun send(data: Any): MessageEvent = send(data, null)
    suspend fun send(data: WorkerContract): MessageEvent = send(data, null)

    suspend fun send(data: Any, transfer: Array<ArrayBuffer>?) =
        suspendCoroutine<MessageEvent> { continuation ->
            var responses = 0
            worker.onmessage = { event ->
                // TODO: Solution until prevent multiple messages per send
                if (++responses == 1) {
                    continuation.resume(event)
                }
            }

            worker.onerror = { event ->
                val throwable =  Throwable(event.toString())
                println("web worker exception: ${throwable.cause}")
                continuation.resumeWithException(throwable)
            }

            if (transfer == null) worker.postMessage(data)
            else worker.postMessage(data, transfer)
        }
}

open external class Worker(scriptURL: String, options: WorkerOptions = definedExternally) :
    EventTarget,
    AbstractWorker {
    var onmessage: ((MessageEvent) -> dynamic)?
    override var onerror: ((Event) -> dynamic)?
    fun terminate()
    fun postMessage(message: Any?, transfer: Array<dynamic> = definedExternally)
}

class WorkerScope(private val scope: DedicatedWorkerGlobalScope) {
    val workerId = URLSearchParams(scope.location.search).get("id") ?: "Unidentified Worker"

    fun receive(block: suspend (dynamic) -> dynamic) {
        scope.onmessage = { messageEvent ->
            try {
                CoroutineScope(Dispatchers.Default).launch {
                    val response = block(messageEvent.data)
                    scope.postMessage(response, response.transfer)
                }
            } catch (e: Throwable) {
                throw e
            }
        }
    }
}

fun worker(block: WorkerScope.() -> Unit) {
    val isWorkerGlobalScope = js("typeof(WorkerGlobalScope) !== \"undefined\"") as? Boolean
        ?: throw IllegalStateException("Boolean cast for WorkerGlobalScope Fail")
    if (isWorkerGlobalScope.not()) return

    val self = js("self") as? DedicatedWorkerGlobalScope
        ?: throw IllegalStateException("DedicatedWorkerGlobalScope failed cast")
    val scope = WorkerScope(self)
    block(scope)
}