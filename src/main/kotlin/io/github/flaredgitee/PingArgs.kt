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

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

sealed class IpVersion {
    object Unspecified : IpVersion()
    object Ipv4 : IpVersion()
    object Ipv6 : IpVersion()
}

data class Config(
    val target: String,
    val hook: List<String> = emptyList(),
    val timeoutMillis: Long? = null,
    val intervalSeconds: Int? = null,
    val ipVersion: IpVersion = IpVersion.Unspecified,
)

fun parseArgs(args: Array<String>): Config {
    val parser = ArgParser("ping-forever")

    // Timeout now interpreted as milliseconds; accept as string and parse to Long for compatibility
    val timeoutRaw by parser.option(
        ArgType.String,
        shortName = "t",
        fullName = "timeout",
        description = "ping timeout in milliseconds"
    ).default("")
    val intervalSentinel by parser.option(
        ArgType.Int,
        shortName = "i",
        fullName = "interval",
        description = "interval seconds between pings"
    ).default(-1)
    val hookRaw by parser.option(
        ArgType.String,
        shortName = "H",
        fullName = "hook",
        description = "hook command to run on failure"
    ).default("")
    val flag4 by parser.option(ArgType.Boolean, shortName = "4", description = "force IPv4").default(false)
    val flag6 by parser.option(ArgType.Boolean, shortName = "6", description = "force IPv6").default(false)

    val target by parser.argument(ArgType.String, description = "target hostname or IP")

    parser.parse(args)

    val ipVersion = when {
        flag4 && flag6 -> IpVersion.Unspecified // conflicting, let system decide
        flag4 -> IpVersion.Ipv4
        flag6 -> IpVersion.Ipv6
        else -> IpVersion.Unspecified
    }

    val hookList = if (hookRaw.isBlank()) emptyList() else hookRaw.split(" ").filter { it.isNotBlank() }
    val timeout = if (timeoutRaw.isBlank()) null else (timeoutRaw.toLongOrNull() ?: throw IllegalArgumentException("invalid timeout value: $timeoutRaw"))
    val interval = if (intervalSentinel < 0) null else intervalSentinel

    return Config(
        target = target,
        hook = hookList,
        timeoutMillis = timeout,
        intervalSeconds = interval,
        ipVersion = ipVersion
    )
}
