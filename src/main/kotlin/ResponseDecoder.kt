package com.github.onlaait.httputil

import java.io.InputStream
import java.net.http.HttpResponse
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import kotlin.jvm.optionals.getOrNull

object ResponseDecoder {

    fun decode(response: HttpResponse<InputStream>): InputStream {
        val body = response.body()
        val headers = response.headers()
        val encoding = headers.firstValue("Content-Encoding").getOrNull()
        return when (encoding?.lowercase()) {
            null -> body
            "gzip" -> GZIPInputStream(body)
            "deflate" -> InflaterInputStream(body)
            else -> throw UnsupportedOperationException("Unsupported Content-Encoding: $encoding")
        }
    }
}