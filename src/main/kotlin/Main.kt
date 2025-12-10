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

import io.github.flaredgitee.*
import java.time.Duration
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val config = try {
        parseArgs(args)
    } catch (e: Exception) {
        println("Error parsing args: ${e.message}")
        exitProcess(2)
    }

    println("Starting ping-forever for target=${config.target}")

    // Determine interval sleep behavior: if interval provided and platform doesn't support it, main loop handles it
    val loopIntervalSeconds = config.intervalSeconds ?: 1 // default to 1s when unspecified for loop pacing

    while (true) {
        val cmd = buildPingCommand(config)
        val timeout = Duration.ofMillis(config.timeoutMillis ?: 30000L)
        when (val result = PingRunner.runOnce(cmd, timeout)) {
            is PingResult.Success -> onSuccess(result.rttMs)
            is PingResult.Timeout -> {
                println("TIMEOUT - ${result.message}")
                if (config.hook.isNotEmpty()) {
                    val ctx = mapOf(
                        "target" to config.target,
                        "reason" to "timeout",
                        "details" to result.message
                    )
                    val hookRes = runHook(config.hook, ctx)
                    if (hookRes.isFailure) {
                        val ex = hookRes.exceptionOrNull()
                        println("Hook execution error: ${ex?.message}")
                    } else {
                        val out = hookRes.getOrNull()!!
                        if (out.exitCode == 0) {
                            println("Hook executed successfully (exit=0)")
                        } else {
                            println("Hook exited with code=${out.exitCode}")
                        }
                        if (out.stdout.isNotBlank()) println("Hook stdout: ${out.stdout}")
                        if (out.stderr.isNotBlank()) println("Hook stderr: ${out.stderr}")
                    }
                }
            }

            is PingResult.Error -> {
                println("ERROR - ${result.message}")
                if (config.hook.isNotEmpty()) {
                    val ctx = mapOf(
                        "target" to config.target,
                        "reason" to "error",
                        "details" to result.message
                    )
                    val hookRes = runHook(config.hook, ctx)
                    if (hookRes.isFailure) {
                        val ex = hookRes.exceptionOrNull()
                        println("Hook execution error: ${ex?.message}")
                    } else {
                        val out = hookRes.getOrNull()!!
                        if (out.exitCode == 0) {
                            println("Hook executed successfully (exit=0)")
                        } else {
                            println("Hook exited with code=${out.exitCode}")
                        }
                        if (out.stdout.isNotBlank()) println("Hook stdout: ${out.stdout}")
                        if (out.stderr.isNotBlank()) println("Hook stderr: ${out.stderr}")
                    }
                }
            }
        }

        try {
            Thread.sleep(loopIntervalSeconds * 1000L)
        } catch (_: InterruptedException) {
            println("Interrupted, exiting")
            break
        }
    }
}

private fun onSuccess(rttMs: Double?): Unit = Unit
