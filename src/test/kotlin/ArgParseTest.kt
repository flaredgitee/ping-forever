import kotlin.test.*
import io.github.flaredgitee.*

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
