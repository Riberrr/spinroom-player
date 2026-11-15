package io.github.riberrr.spinroom.core.maapi

import java.net.URLEncoder

/**
 * Builds image URLs. MA 2.10 serves images by id: `/imageproxy/{proxy_id}?size=` (the old
 * `/imageproxy?path=&provider=` form answers 400 there). Older servers without `proxy_id` get
 * the remote URL directly when it is public, else the old query form.
 * The proxy only accepts sizes 256/512/1024 (others: 400), so requests are rounded up to one of them.
 */
object ImageUrls {
    private val SIZES = intArrayOf(256, 512, 1024)

    /** Smallest size the proxy serves that is at least [px] (1024 for anything bigger). */
    fun proxySize(px: Int): Int = SIZES.firstOrNull { it >= px } ?: SIZES.last()

    fun url(baseUrl: String, image: MediaItemImage?, sizePx: Int): String? {
        if (image == null) return null
        val base = MaClient.normalizeBaseUrl(baseUrl)
        val enc = { s: String -> URLEncoder.encode(s, "UTF-8") }
        val proxyId = image.proxyId?.trim().orEmpty()
        val size = proxySize(sizePx)
        return when {
            proxyId.isNotEmpty() -> "$base/imageproxy/${enc(proxyId)}?size=$size"
            image.path.isBlank() -> null
            image.remotelyAccessible && image.path.startsWith("http") -> image.path
            else -> "$base/imageproxy?path=${enc(image.path)}&provider=${enc(image.provider)}&size=$size"
        }
    }
}
