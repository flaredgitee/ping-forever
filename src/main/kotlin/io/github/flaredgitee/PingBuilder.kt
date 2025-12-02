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

object Os {
    val name: String = System.getProperty("os.name").lowercase()
    val isWindows: Boolean = name.contains("win")
}

// Pure function: build ping command arguments based on config
fun buildPingCommand(config: Config): List<String> {
    // Use platform ping executable name
    val cmd = if (Os.isWindows) "ping" else "ping"
    val args = mutableListOf<String>()
    args.add(cmd)

    // IPv4/IPv6
    when (config.ipVersion) {
        is IpVersion.Ipv4 -> {
            if (!Os.isWindows) args.add("-4") else args.add("-4")
        }

        is IpVersion.Ipv6 -> {
            if (!Os.isWindows) args.add("-6") else args.add("-6")
        }

        else -> {}
    }

    // timeout: now in milliseconds
    config.timeoutMillis?.let { tMs ->
        if (Os.isWindows) {
            // Windows ping uses -w in milliseconds to wait for each reply
            args.add("-w")
            args.add(tMs.toString())
        } else {
            // Unix ping: use -W with seconds; convert ms -> seconds by ceiling
            val seconds = ((tMs + 999) / 1000).toString()
            args.add("-W")
            args.add(seconds)
        }
    }

    // interval: on Unix it's -i, on Windows there's no direct interval flag; we will implement interval by sleeping between loops
    config.intervalSeconds?.let { i ->
        if (!Os.isWindows) {
            args.add("-i")
            args.add(i.toString())
        } else {
            // no-op for windows; main loop will handle interval
        }
    }

    // single ping packet per invocation so we can detect success/failure quickly
    if (Os.isWindows) {
        args.add("-n")
        args.add("1")
    } else {
        args.add("-c")
        args.add("1")
    }

    args.add(config.target)

    return args
}
