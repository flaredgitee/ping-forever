package io.github.flaredgitee

import java.io.File
import java.time.Instant

object HookRunner {
    // Run the hook command (list where first is executable). This is side-effecting.
    fun runHook(hook: List<String>, context: Map<String, Any?>): Result<Unit> {
        return try {
            if (hook.isEmpty()) return Result.success(Unit)
            val pb = ProcessBuilder(hook)
            // Provide context JSON-ish on stdin for the hook
            val proc = pb.start()
            val input = proc.outputStream
            val json = buildContextJson(context)
            input.write(json.toByteArray())
            input.flush()
            input.close()

            val finished = proc.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return Result.failure(RuntimeException("hook timed out"))
            }
            val exit = proc.exitValue()
            if (exit == 0) Result.success(Unit) else Result.failure(RuntimeException("hook exit=$exit"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildContextJson(context: Map<String, Any?>): String {
        val parts = context.entries.joinToString(",") { (k, v) ->
            val value = when (v) {
                null -> "null"
                is Number -> v.toString()
                is String -> "\"${v.replace("\"", "\\\"")}\""
                else -> "\"${v.toString().replace("\"", "\\\"")}\""
            }
            "\"$k\":$value"
        }
        return "{\"timestamp\":\"${Instant.now()}\",$parts}"
    }
}
