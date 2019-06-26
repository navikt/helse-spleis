package no.nav.helse

import io.ktor.application.log
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.random.Random

fun createTestApplicationEnvironment() = applicationEngineEnvironment {
    connector {
        port = Random.nextInt(1000, 9999)
    }

    module {
        sakskompleksApplication()
    }
}

fun testServer(shutdownTimeoutMs: Long = 10000,
               test: ApplicationEngine.() -> Unit) = embeddedServer(Netty, createTestApplicationEnvironment()).apply {
    val stopper = GlobalScope.launch {
        delay(shutdownTimeoutMs)
        this@apply.application.log.info("stopping server after timeout")
        stop(0, 0, TimeUnit.SECONDS)
    }
    start(wait = false)
    try {
        test()
    } finally {
        stopper.cancel()
        stop(0, 0, TimeUnit.SECONDS)
    }
}

fun ApplicationEngine.handleRequest(method: HttpMethod,
                                    path: String,
                                    builder: HttpURLConnection.() -> Unit = {},
                                    test: HttpURLConnection.(HttpStatusCode) -> Unit) {
    val url = environment.connectors[0].let { connector ->
        URL("${connector.type.name.toLowerCase()}://${connector.host}:${connector.port}$path")
    }
    val con = url.openConnection() as HttpURLConnection
    con.requestMethod = method.value

    con.builder()

    con.connectTimeout = 1000
    con.readTimeout = 1000

    con.test(HttpStatusCode.fromValue(con.responseCode))
}

val HttpURLConnection.responseBody get() =
    BufferedReader(InputStreamReader(
            if (responseCode in 200..299) {
                inputStream
            } else {
                errorStream
            }
    )).lines().collect(Collectors.joining())
