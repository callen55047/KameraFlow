package tasks

import java.io.File

object CommandLineRunner {
    private val bashErrorStrings =
        listOf(
            "error",
            "No such file or directory",
        )

    const val SUCCESS = 0

    fun String.cmdline(
        dir: File? = null,
        isDryRun: Boolean = false,
        onError: (err: Exception) -> Unit = {},
    ) {
        try {
            if (isDryRun.not()) {
                this.execAndThrow(dir)
            } else {
                println(
                    """
                    DryRun: 
                      CMD: $this
                      DIR: ${dir ?: File("./")}
                    """.trimIndent(),
                )
            }
        } catch (e: Exception) {
            println(
                """
                Execution error from:
                  CMD: $this
                  DIR: ${dir?.path}
                """.trimIndent(),
            )

            onError(e)
        }
    }

    private fun getProcessBuilder(cmd: String): ProcessBuilder =
        ProcessBuilder("/bin/sh", "-c", cmd)
            .redirectErrorStream(true)

    private fun String.runRedirectedCmd(
        dir: File? = null,
        defaultLog: Boolean = true,
    ): File {
        val file = File.createTempFile("cmd", ".log")
        file.deleteOnExit()

        val process =
            getProcessBuilder(this)
                .redirectOutput(file)
                .directory(dir)
                .start()

        if (process.waitFor() != 0) {
            file.readLines().forEach(::println)
            throw cmdError(this, process.inputStream.bufferedReader().readText())
        }

        if (defaultLog) file.readLines().forEach(::println)

        return file
    }

    private fun String.execAndThrow(
        dir: File? = null,
        preventEmptyLogs: Boolean = false,
    ): File {
        println(
            """
            
            Run command:
              $this
            """.trimIndent(),
        )

        val file = runRedirectedCmd(dir)

        file
            .enforceLogs(this, preventEmptyLogs)
            .handleErrors(this)

        return file
    }

    private fun File.enforceLogs(
        command: String,
        shouldEnforce: Boolean,
    ): List<String> {
        return readLines().also {
            if (shouldEnforce && it.isEmpty()) {
                throw cmdError(command, "No logs captured from command line")
            }
        }
    }

    private fun List<String>.handleErrors(command: String) {
        val errors =
            filter { line ->
                bashErrorStrings.any { line.contains(it) }
            }

        if (errors.isEmpty()) return

        println(
            """
            ${"=".repeat(20)}
            $CLR_ERROR Captured Errors:
              ${errors.joinToString("\n")}
            """.trimIndent(),
        )

        throw cmdError(command, errors.first())
    }

    private const val CLR_ERROR = "[CommmandLineRunner] ERROR:"

    private fun cmdError(
        cmd: String,
        reason: String,
    ): Error =
        Error(
            """
            $CLR_ERROR 
            CMD: $cmd
            Failed: $reason
            """.trimIndent(),
        )
}