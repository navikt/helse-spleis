package no.nav.helse.spleis

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.util.*
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.Logger

private val ignoredPaths = listOf("/metrics", "/isalive", "/isready")

internal fun Application.requestResponseTracing(logger: Logger, registry: MeterRegistry) {
    intercept(ApplicationCallPipeline.Monitoring) {
        try {
            if (call.request.uri in ignoredPaths) return@intercept proceed()
            val headers = call.request.headers.toMap()
                .filterNot { (key, _) -> key.lowercase() in listOf("authorization") }
                .map { (key, values) ->
                    keyValue("req_header_$key", values.joinToString(separator = ";"))
                }.toTypedArray()
            logger.info("incoming callId=${call.callId} method=${call.request.httpMethod.value} uri=${call.request.uri}", *headers)

            val timer = Timer.start(registry)
            try {
                proceed()
            } finally {
                timer.stop(
                    Timer.builder("http_request_duration_seconds")
                        .description("Distribution of http request duration")
                        .register(registry)
                )
            }
        } catch (err: Throwable) {
            logger.error("feil i håndtering av request: ${err.message} callId=${call.callId}", err)
            throw err
        }
    }

    sendPipeline.intercept(ApplicationSendPipeline.After) { message ->
        val status = call.response.status() ?: (when (message) {
            is OutgoingContent -> message.status
            is HttpStatusCode -> message
            else -> null
        } ?: HttpStatusCode.OK).also { status ->
            call.response.status(status)
        }

        if (call.request.uri in ignoredPaths) return@intercept
        logger.info("responding with status=${status.value} callId=${call.callId} ")
        Counter.builder("http_requests_total")
            .description("Counts the http requests")
            .tag("method", call.request.httpMethod.value)
            .tag("code", "${status.value}")
            .register(registry)
            .increment()
    }
}
