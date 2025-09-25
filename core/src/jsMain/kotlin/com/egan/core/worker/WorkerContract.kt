package com.egan.core.worker

import org.khronos.webgl.ArrayBuffer

sealed interface WorkerContract {
    /**
     * Used as the unique code to determine which command has been received.
     */
    val code: String

    /**
     * Used as a message to communicate extra data on top of the [code].
     */
    val message: String

    /**
     * Any information that needs to be passed between the Worker and SDK.
     */
    val data: Any

    /**
     * Take the instance of the [WorkerContract] and convert it to a dynamic JS object.
     * This conversion function have to be called to transfer data between the SDK and the worker properly.
     */
    fun forTransfer(): dynamic

    sealed interface FromSdk : WorkerContract {

        override fun forTransfer(): dynamic = jsObject {
            this.data = data
            this.code = code
            this.message = message
        }

        interface StartModel : FromSdk {

            companion object {
                const val CODE = "StartModel"
                private const val DEFAULT_MESSAGE = "Start Model with Model URL"

                fun of(
                    modelUrl: String,
                    message: String = DEFAULT_MESSAGE
                ): StartModel {
                    return object : StartModel {
                        override val code: String = CODE
                        override val data: String = modelUrl
                        override val message: String = message
                    }
                }
            }
        }

        interface WarmupModel : FromSdk {

            companion object {
                const val CODE = "WarmupModel"
                private const val DEFAULT_MESSAGE = "Warmup Model after model loaded"
                private const val DEFAULT_DATA = "StartWarmup"

                fun of(
                    message: String = DEFAULT_MESSAGE
                ): WarmupModel {
                    return object : WarmupModel {
                        override val code: String = CODE
                        override val data: String = DEFAULT_DATA
                        override val message: String = message
                    }
                }
            }
        }

        interface Detect : FromSdk {

            companion object {
                const val CODE = "Detect"
                private const val DEFAULT_MESSAGE = "Call Tensorflow Detect with image data"

                fun of(
                    message: String = DEFAULT_MESSAGE,
                    data: org.khronos.webgl.ArrayBuffer
                ): Detect {
                    return object : Detect {
                        override val code: String = CODE
                        override val data: org.khronos.webgl.ArrayBuffer = data
                        override val message: String = message
                    }
                }
            }
        }
    }

    sealed interface FromWorker : WorkerContract {

        override fun forTransfer(): dynamic = jsObject {
            this.data = data
            this.code = code
            this.message = message
        }

        interface ModelStarted : FromWorker {

            companion object {
                const val CODE = "ModelStarted"
                private const val DEFAULT_MESSAGE = "Model Successfully started"

                fun of(message: String = DEFAULT_MESSAGE, data: Any): ModelStarted {
                    return object : ModelStarted {
                        override val message: String = message
                        override val code: String = CODE
                        override val data: Any = data
                    }
                }
            }
        }

        interface ModelWarmupCompleted : FromWorker {

            companion object {
                const val CODE = "ModelWarmupCompleted"
                private const val DEFAULT_MESSAGE = "Model Warmup Completed"

                fun of(message: String = DEFAULT_MESSAGE): ModelWarmupCompleted {
                    return object : ModelWarmupCompleted {
                        override val message: String = message
                        override val code: String = CODE
                        override val data: Any = Any()
                    }
                }
            }
        }

        interface DetectResult : FromWorker {

            companion object {
                const val CODE = "DetectResult"
                private const val DEFAULT_MESSAGE = "Detection result"

                fun of(message: String = DEFAULT_MESSAGE, data: Array<org.khronos.webgl.ArrayBuffer>): DetectResult {
                    return object : DetectResult {
                        override val message: String = message
                        override val code: String = CODE
                        override val data: Array<org.khronos.webgl.ArrayBuffer> = data
                    }
                }
            }
        }

        interface UnexpectedError : FromWorker {
            val exception: Exception?

            override fun forTransfer(): dynamic = jsObject {
                this.data = data
                this.code = code
                this.message = message
                this.exception = exception
            }

            companion object {
                const val CODE = "UnexpectedError"
                private const val DEFAULT_MESSAGE = "Unexpected Error happened in worker"

                fun of(message: String = DEFAULT_MESSAGE, exception: Exception? = null): UnexpectedError {
                    return object : UnexpectedError {
                        override val exception: Exception? = exception
                        override val message: String = message
                        override val code: String = CODE
                        override val data: Any = Any()
                    }
                }
            }
        }
    }

    fun jsObject(block: dynamic.() -> Unit): dynamic {
        val o = js("({})")
        block(o)
        return o
    }

    companion object {
        /**
         * Used to convert incoming messages in a JSON object form from the worker to the sdk.
         * Since messages and data from the worker are only preserved if they are a String or Array type.
         * Therefore, other object types have to be recreated on the SDK side.
         */
        fun fromMessageToData(data: dynamic): WorkerContract {
            return when (data.code) {
                FromWorker.ModelStarted.CODE -> FromWorker.ModelStarted.of(data.message as String, data.data as Any)
                FromWorker.ModelWarmupCompleted.CODE -> FromWorker.ModelWarmupCompleted.of(data.message as String)
                FromWorker.DetectResult.CODE -> FromWorker.DetectResult.of(data.message as String, data.data as Array<ArrayBuffer>)
                FromWorker.UnexpectedError.CODE -> {
                    val unexpectedError = FromWorker.UnexpectedError.of(data.message as String, exception = Exception(message = data.exception.message as? String))
                    println("Unexpected Error from Worker: ${unexpectedError.message}")
                    unexpectedError
                }
                else -> {
                    val unexpectedError = FromWorker.UnexpectedError.of(message = "Unknown message type coming from Worker")
                    println(unexpectedError.message)
                    unexpectedError
                }
            }
        }
    }
}
