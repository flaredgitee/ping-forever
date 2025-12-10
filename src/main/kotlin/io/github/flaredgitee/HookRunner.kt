/*
 * Copyright 2025 flaredgitee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.flaredgitee

import java.io.ByteArrayOutputStream
import java.time.Instant

data class HookOutput(val stdout: String, val stderr: String, val exitCode: Int)

// Run the hook command (list where first is executable). This is side-effecting.
fun runHook(hook: List<String>, context: Map<String, Any?>): Result<HookOutput> = try {
    if (hook.isEmpty()) Result.success(HookOutput("", "", 0))
    else {
        val pb = ProcessBuilder(hook)
        // Provide context JSON-ish on stdin for the hook
        val proc = pb.start()
        val input = proc.outputStream
        val json = buildContextJson(context)
        input.write(json.toByteArray())
        input.flush()
        input.close()

        // Capture stdout/stderr concurrently
        val stdoutStream = ByteArrayOutputStream()
        val stderrStream = ByteArrayOutputStream()
        val outThread = Thread { proc.inputStream.copyTo(stdoutStream) }
        val errThread = Thread { proc.errorStream.copyTo(stderrStream) }
        outThread.start()
        errThread.start()

        val finished = proc.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)
        if (finished) {
            // wait for reader threads to complete reading remaining output
            outThread.join()
            errThread.join()

            val exit = proc.exitValue()
            val stdout = stdoutStream.toString(Charsets.UTF_8)
            val stderr = stderrStream.toString(Charsets.UTF_8)

            // Always return a HookOutput so callers can inspect stdout/stderr/exit code.
            Result.success(HookOutput(stdout, stderr, exit))
        } else {
            proc.destroyForcibly()
            // ensure readers finish
            outThread.join(100)
            errThread.join(100)
            Result.failure(RuntimeException("hook timed out"))
        }
    }
} catch (e: Exception) {
    Result.failure(e)
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
