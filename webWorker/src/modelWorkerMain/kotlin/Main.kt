import com.egan.core.worker.WorkerContract.FromSdk
import com.egan.core.worker.WorkerContract.FromWorker
import com.egan.core.worker.worker

fun main() = worker {
    receive { message: dynamic /* Request message type: WorkerContract.FromSdk */ ->
        when (message.code) {
            FromSdk.StartModel.CODE -> {
            }
            FromSdk.WarmupModel.CODE -> {
                FromWorker.ModelWarmupCompleted.of().forTransfer()
            }
            else -> {
                FromWorker.UnexpectedError.of(message = "Message retrieved in worker: $message").forTransfer()
            }
        }
    } /* Response message type: WorkerContract.FromWorker forTransfer() */
}