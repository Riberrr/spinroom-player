package io.github.riberrr.spinroom.feature.settings.latency

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Minimal HTTP server so MA can fetch [TestSignal] from the TV: `GET|HEAD /spinroom-sync.wav`, with
 * `Range: bytes=N-` support (ffmpeg probes and seeks). Anything else is 404. One thread per connection.
 */
class TestSignalServer(bindAddress: InetAddress? = null) : AutoCloseable {
    private val socket = ServerSocket(0, 8, bindAddress)
    val port: Int get() = socket.localPort

    @Volatile private var closed = false

    init {
        thread(name = "Spinroom-signal", isDaemon = true) {
            while (!closed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                thread(name = "Spinroom-signal-conn", isDaemon = true) { client.use(::serve) }
            }
        }
    }

    private fun serve(client: Socket) {
        runCatching {
            client.soTimeout = 15_000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.ISO_8859_1))
            val request = reader.readLine() ?: return
            var rangeStart: Long? = null
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                if (line.startsWith("range:", ignoreCase = true)) rangeStart = parseRangeStart(line.substringAfter(':'))
            }
            val parts = request.split(' ')
            val method = parts.getOrNull(0).orEmpty()
            val path = parts.getOrNull(1).orEmpty().substringBefore('?')
            val out = client.getOutputStream().buffered(64 * 1024)
            if ((method != "GET" && method != "HEAD") || path != PATH) {
                out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                out.flush()
                return
            }
            val total = TestSignal.totalBytes
            val start = rangeStart?.takeIf { it in 0 until total } ?: 0L
            val head = buildString {
                append(if (rangeStart != null) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
                append("Content-Type: audio/wav\r\n")
                append("Accept-Ranges: bytes\r\n")
                append("Content-Length: ${total - start}\r\n")
                if (rangeStart != null) append("Content-Range: bytes $start-${total - 1}/$total\r\n")
                append("Connection: close\r\n\r\n")
            }
            out.write(head.toByteArray(Charsets.ISO_8859_1))
            if (method == "GET") stream(out, start)
            out.flush()
        }
    }

    private fun stream(out: OutputStream, from: Long) {
        val buf = ByteArray(32 * 1024)
        var pos = from
        while (!closed) {
            val n = TestSignal.read(pos, buf)
            if (n == 0) break
            out.write(buf, 0, n)
            pos += n
        }
    }

    override fun close() {
        closed = true
        runCatching { socket.close() }
    }

    companion object {
        const val PATH = "/spinroom-sync.wav"

        /** `bytes=N-` or `bytes=N-M` → N; anything else → null (serve the whole file). */
        fun parseRangeStart(value: String): Long? =
            Regex("""\s*bytes=(\d+)-\d*\s*""").matchEntire(value)?.groupValues?.get(1)?.toLongOrNull()
    }
}
