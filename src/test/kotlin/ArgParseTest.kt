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

import io.github.flaredgitee.parseArgs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArgParseTest {
    @Test
    fun testParseSimple() {
        val cfg = parseArgs(arrayOf("example.com"))
        assertEquals("example.com", cfg.target)
        assertNull(cfg.timeoutMillis)
        assertNull(cfg.intervalSeconds)
        assertEquals(0, cfg.hook.size)
    }

    @Test
    fun testParseHookAndTimeout() {
        val cfg = parseArgs(arrayOf("-t", "5000", "-H", "echo hello", "example.com"))
        assertEquals("example.com", cfg.target)
        assertEquals(5000L, cfg.timeoutMillis)
        assertEquals(listOf("echo", "hello"), cfg.hook)
    }
}
