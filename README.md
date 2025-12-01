# ping-forever

Small CLI tool to continuously ping a target and run an optional hook on failures.

Usage

```
java -jar build/libs/ping-forever-0.1.0-all.jar [options] <target>
```

Options

- `-t, --timeout <ms>`: ping timeout in milliseconds (applies per ping invocation). If omitted, default is 30000 (30s).
- `-i, --interval <s>`: interval in seconds between pings (the main loop sleeps this long). If omitted, default is 1
  second.
- `-H, --hook <cmd>`: a simple hook command string (space-separated) to run when a ping times out or errors; e.g.
  `-H "echo failed"`.
- `-4`: force IPv4
- `-6`: force IPv6

Notes

- `timeout` is interpreted as milliseconds for both Windows and Unix. On Windows the ping `-w` flag accepts
  milliseconds. On Unix the code converts milliseconds to seconds (ceiling) and passes `-W <seconds>` for compatibility
  with common ping implementations.
- `interval` is in seconds.

Examples

Run against localhost with 1 second timeout (1000 ms) and 2 second interval:

```
java -jar build/libs/ping-forever-0.1.0-all.jar -t 1000 -i 2 127.0.0.1
```

Run with a hook:

```
java -jar build/libs/ping-forever-0.1.0-all.jar -t 500 -H "echo alerted" example.com
```

Building

Use the included Gradle wrapper (Windows PowerShell):

```
.\gradlew.bat clean build
```

The fat jar is created by the `fatJar` task and will appear as `build/libs/ping-forever-0.1.0-all.jar`.

