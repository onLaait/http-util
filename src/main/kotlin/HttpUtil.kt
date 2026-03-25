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

object HttpUtil : Logging {

    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"
    const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36"
    const val SEC_CH_UA = "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\""

    val client = HttpClient.newHttpClient()

    fun request(request: HttpRequest, maxTry: Int = 5): HttpResponse<InputStream>? {
        val delay = IncreasingDelay(500)
        var fail = 0
        while (true) {
            val res =
                try {
                    logger.debug { "HTTP 요청 중: ${request.uri()}" }
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream())
                } catch (e: IOException) {
                    fail++
                    val msg = { "HTTP 요청 전송 중 오류 $fail: ${request.uri()}\n${e.stackTraceToString()}" }
                    if (fail == maxTry) {
                        logger.error(msg)
                        return null
                    }
                    logger.debug(msg)
                    delay.sleep()
                    continue
                }
            return res
        }
    }

    fun requestText(request: HttpRequest, maxTry: Int = 5): String? =
        request(request, maxTry)?.body()?.readText()

    fun InputStream.readText(): String =
        bufferedReader().use { it.readText() }

    fun String.query(vararg queries: Pair<String, Any?>): String =
        "$this?${queries.joinToString(separator = "&") { "${URLEncoder.encode(it.first, StandardCharsets.UTF_8)}=${if (it.second == null) "" else URLEncoder.encode(it.second.toString(), StandardCharsets.UTF_8)}" }}"

    fun HttpRequest.Builder.uri(uri: String): HttpRequest.Builder =
        uri(URI.create(uri))

    fun HttpRequest.Builder.setAccept(accept: String = "*/*"): HttpRequest.Builder =
        setHeader("accept", accept)

    fun HttpRequest.Builder.setAcceptEncoding(): HttpRequest.Builder =
        setHeader("accept-encoding", "gzip, deflate, br, zstd")

    fun HttpRequest.Builder.setAcceptLanguage(acceptLanguage: String = "ko-KR,ko;q=0.9"): HttpRequest.Builder =
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