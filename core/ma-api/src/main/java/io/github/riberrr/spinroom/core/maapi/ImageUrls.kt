package io.github.riberrr.spinroom.core.maapi

import java.net.URLEncoder

/** Builds image-proxy URLs. Format is unconfirmed until fixtures are recorded: `/imageproxy?path=&provider=&size=`. */
object ImageUrls {
    fun url(baseUrl: String, image: MediaItemImage?, sizePx: Int): String? {
        if (image == null || image.path.isBlank()) return null
        val base = MaClient.normalizeBaseUrl(baseUrl)
        val enc = { s: String -> URLEncoder.encode(s, "UTF-8") }
        return "$base/imageproxy?path=${enc(image.path)}&provider=${enc(image.provider)}&size=$sizePx"
    }
}
