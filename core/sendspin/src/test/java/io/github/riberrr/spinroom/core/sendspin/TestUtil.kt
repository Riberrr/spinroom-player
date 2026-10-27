package io.github.riberrr.spinroom.core.sendspin

fun resource(path: String): String =
    requireNotNull(object {}.javaClass.classLoader!!.getResource(path)) { "missing resource $path" }.readText()

/** Fixture text without a trailing newline (JSON fixtures are one line). */
fun fixture(name: String): String = resource("fixtures/$name").trim()

/** `.hex` fixture: comment lines start with `#`, whitespace is ignored. */
fun hexFixture(name: String): ByteArray =
    hex(resource("fixtures/$name").lines().filterNot { it.startsWith("#") }.joinToString(""))

fun hex(text: String): ByteArray {
    val clean = text.filterNot { it.isWhitespace() }
    return ByteArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
