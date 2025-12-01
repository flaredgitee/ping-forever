package io.github.flaredgitee

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
        val result = PingRunner.runOnce(cmd, timeout)

        when (result) {
            is PingResult.Success -> println("OK - rtt=${result.rttMs}ms")
            is PingResult.Timeout -> {
                println("TIMEOUT - ${result.message}")
                if (config.hook.isNotEmpty()) {
                    val ctx = mapOf(
                        "target" to config.target,
                        "reason" to "timeout",
                        "details" to result.message
                    )
                    val hookRes = HookRunner.runHook(config.hook, ctx)
                    if (hookRes.isFailure) {
                        println("Hook failed: ${hookRes.exceptionOrNull()?.message}")
                    } else {
                        println("Hook executed successfully")
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
                    val hookRes = HookRunner.runHook(config.hook, ctx)
                    if (hookRes.isFailure) {
                        println("Hook failed: ${hookRes.exceptionOrNull()?.message}")
                    } else {
                        println("Hook executed successfully")
                    }
                }
            }
        }

        try {
            Thread.sleep(loopIntervalSeconds * 1000L)
        } catch (e: InterruptedException) {
            println("Interrupted, exiting")
            break
        }
    }
}
