package no.nav.helse
import com.sun.net.httpserver.HttpServer
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.exporter.common.TextFormat
import org.slf4j.LoggerFactory
import java.io.CharArrayWriter
import java.net.InetSocketAddress
import java.util.concurrent.Executors

internal class ProbeApi {
    private fun HttpServer.håndter(path: String, contentType: String, response: () -> String) {
        createContext("/$path") { httpExchange ->
            incomingHttpRequests.labels(path).inc()
            val responseBody = response().toByteArray()
            httpExchange.responseHeaders.add("Content-Type", contentType)
            httpExchange.sendResponseHeaders(200, responseBody.size.toLong())
            httpExchange.responseBody.write(responseBody)
            httpExchange.responseBody.flush()
            httpExchange.responseBody.close()
        }
    }

    private val server = HttpServer.create(InetSocketAddress(port), 0).also {
        it.executor = Executors.newFixedThreadPool(10)
        it.håndter("isalive", "text/plain; charset=utf-8") { "I am alive!" }
        it.håndter("isready", "text/plain; charset=utf-8") { "I am ready!" }
        it.håndter("metrics", TextFormat.CONTENT_TYPE_004) {
            val metrics = CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(emptySet())
            CharArrayWriter(1024)
                .also { writer -> TextFormat.write004(writer, metrics) }
                .use { writer -> writer.toString() }
        }
    }

    internal fun start() = server.start().also {
        logger.info("Starter server på port $port")
        Runtime.getRuntime().addShutdownHook(Thread {
            server.stop(5)
            logger.info("Stopper server på port $port")
        })
    }

    private companion object {
        private val incomingHttpRequests =
            Counter.build("incoming_http_requests", "Antall innkommende HTTP requester").labelNames("path").register()
        private const val port = 8080
        private val logger = LoggerFactory.getLogger(ProbeApi::class.java)
    }
}
