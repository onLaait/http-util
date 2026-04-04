package com.github.onlaait.httputil

import org.apache.logging.log4j.kotlin.Logging
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import kotlin.jvm.optionals.getOrNull

object HttpUtil : Logging {

    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"
    const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36"
    const val SEC_CH_UA = "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\""

    val client = HttpClient.newHttpClient()!!

    fun HttpClient.request(request: HttpRequest, maxTry: Int = 5): HttpResponse<InputStream>? {
        val delay = IncreasingDelay(500)
        var fail = 0
        while (true) {
            val res =
                try {
                    logger.debug { "HTTP 요청 중: ${request.uri()}" }
                    send(request, HttpResponse.BodyHandlers.ofInputStream())
                } catch (e: IOException) {
                    fail++
                    val msg = { "HTTP 요청 전송 중 오류 $fail: ${request.uri()}\n${e.stackTraceToString()}" }
                    if (fail == maxTry) {
                        logger.error(msg)
                        return null
                    }
                    logger.info(msg)
                    delay.sleep()
                    continue
                }
            return res
        }
    }

    fun request(request: HttpRequest, maxTry: Int = 5): HttpResponse<InputStream>? =
        client.request(request, maxTry)

    fun HttpClient.requestText(request: HttpRequest, maxTry: Int = 5): String? {
        val res = request(request, maxTry) ?: return null
        return res.decodedBody().readText()
    }

    fun requestText(request: HttpRequest, maxTry: Int = 5): String? =
        client.requestText(request, maxTry)

    fun HttpResponse<InputStream>.decodedBody(): InputStream {
        val body = body()
        val headers = headers()
        val encoding = headers.firstValue("Content-Encoding").getOrNull()
        return when (encoding?.lowercase()) {
            null -> body
            "gzip" -> GZIPInputStream(body)
            "deflate" -> InflaterInputStream(body)
            else -> throw UnsupportedOperationException("Unsupported Content-Encoding: $encoding")
        }
    }

    fun InputStream.readText(): String =
        bufferedReader().use { it.readText() }

    fun encodeQuery(vararg queries: Pair<String, Any?>): String =
        queries.joinToString("&") { "${URLEncoder.encode(it.first, StandardCharsets.UTF_8)}=${if (it.second == null) "" else URLEncoder.encode(it.second.toString(), StandardCharsets.UTF_8)}" }

    fun String.query(vararg queries: Pair<String, Any?>): String {
        return buildString {
            append(this)
            if (last().let { it != '?' && it != '&' }) append(if (contains('?')) '&' else '?')
            append(encodeQuery(*queries))
        }
    }

    fun HttpRequest.Builder.uri(uri: String): HttpRequest.Builder =
        uri(URI.create(uri))

    fun HttpRequest.Builder.setAccept(accept: String = "*/*"): HttpRequest.Builder =
        setHeader("accept", accept)

    fun HttpRequest.Builder.setAcceptEncoding(): HttpRequest.Builder =
        setHeader("accept-encoding", "gzip, deflate, br, zstd")

    fun HttpRequest.Builder.setAcceptLanguage(acceptLanguage: String = "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7"): HttpRequest.Builder =
        setHeader("accept-language", acceptLanguage)

    fun HttpRequest.Builder.setOrigin(origin: String): HttpRequest.Builder =
        setHeader("origin", origin)

    fun HttpRequest.Builder.setReferer(referer: String): HttpRequest.Builder =
        setHeader("referer", referer)

    fun HttpRequest.Builder.setContentType(contentType: String): HttpRequest.Builder =
        setHeader("content-type", contentType)

    fun HttpRequest.Builder.setUserAgent(value: String = USER_AGENT): HttpRequest.Builder =
        setHeader("user-agent", value)

    fun HttpRequest.Builder.setPriority(): HttpRequest.Builder =
        setHeader("priority", "u=1, i")

    fun HttpRequest.Builder.setSecChUa(mobile: Boolean = false): HttpRequest.Builder =
        setHeader("sec-ch-ua", SEC_CH_UA)
            .setHeader("sec-ch-ua-mobile", "?${if (mobile) 1 else 0}")
            .setHeader("sec-ch-ua-platform", "\"${if (mobile) "Android" else "Windows"}\"")

    fun HttpRequest.Builder.setSecFetch(dest: String = "empty", mode: String = "cors", site: String = "same-site"): HttpRequest.Builder =
        setHeader("sec-fetch-dest", dest)
            .setHeader("sec-fetch-mode", mode)
            .setHeader("sec-fetch-site", site)

    fun HttpRequest.Builder.setCookie(map: Map<String, Any>): HttpRequest.Builder =
        setHeader("cookie", map.entries.joinToString("; ") { "${it.key}=${it.value}" })

    fun HttpRequest.Builder.setCookie(value: String): HttpRequest.Builder =
        setHeader("cookie", value)
}