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

import java.io.BufferedReader
import java.time.Duration

sealed class PingResult {
    data class Success(val rttMs: Double?) : PingResult()
    data class Timeout(val message: String) : PingResult()
    data class Error(val message: String) : PingResult()
}

object PingRunner {
    // Runs a single ping using the built command and returns PingResult
    fun runOnce(command: List<String>, timeout: Duration = Duration.ofSeconds(30)): PingResult {
        return try {
            val pb = ProcessBuilder(command)
            pb.redirectErrorStream(true)
            val proc = pb.start()

            // Wait for process with timeout
            val finished = proc.waitFor(timeout.seconds, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return PingResult.Timeout("ping process timed out")
            }

            val exit = proc.exitValue()
            val output = proc.inputStream.bufferedReader().use(BufferedReader::readText)

            // On many systems exit code 0 means success, but parse output for RTT when possible
            if (exit == 0) {
                // Attempt to extract rtt in ms from output using a more flexible regex.
                // Matches patterns like "time=12.3 ms", "12.3 ms", "time=12ms", "12ms" (case-insensitive).
                val regex = Regex("(?:time=)?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*ms", RegexOption.IGNORE_CASE)
                val match = regex.find(output)
                val rtt = match?.groups?.get(1)?.value?.toDoubleOrNull()
                PingResult.Success(rtt)
            } else {
                // Non-zero exit, consider failure; differentiate timeout by inspecting output
                if (output.contains("timed out", ignoreCase = true) || output.contains(
                        "100% packet loss",
                        ignoreCase = true
                    ) || output.contains("Destination host unreachable", ignoreCase = true)
                ) {
                    PingResult.Timeout(output.lines().take(5).joinToString("\n"))
                } else {
                    PingResult.Error(output.lines().take(8).joinToString("\n"))
                }
            }
        } catch (e: Exception) {
            PingResult.Error(e.message ?: "unknown error")
        }
    }
}
