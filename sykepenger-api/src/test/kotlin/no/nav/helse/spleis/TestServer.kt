package no.nav.helse.spleis

import io.ktor.http.HttpMethod
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import kotlinx.coroutines.runBlocking
import no.nav.helse.Toggle
import no.nav.helse.spleis.testhelpers.ApiTestServer

fun main() = runBlocking {
    Toggle.GraphQLPlayground.enable {
        val server = ApiTestServer(4321)
        server.start()
    }
}

fun randomPort(): Int = ServerSocket(0).use {
    it.localPort
}

fun String.handleRequest(
    method: HttpMethod,
    path: String,
    builder: HttpURLConnection.() -> Unit = {}
): HttpURLConnection {
    val url = URL("$this$path")
    val con = url.openConnection() as HttpURLConnection
    con.requestMethod = method.value

    con.builder()

    con.connectTimeout = 1000
    con.readTimeout = 5000

    return con
}

val HttpURLConnection.responseBody: String
    get() {
        val stream: InputStream? = if (responseCode in 200..299) {
            inputStream
        } else {
            errorStream
        }

        return stream?.use { it.bufferedReader().readText() } ?: ""
    }
