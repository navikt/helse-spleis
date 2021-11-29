package no.nav.helse.spleis

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import org.slf4j.Logger

private val ignoredPaths = listOf("/metrics", "/isalive", "/isready")

internal fun Application.requestResponseTracing(logger: Logger) {
    val httpRequestCounter = Counter.build(
        "http_requests_total",
        "Counts the http requests"
    )
        .labelNames("method", "code")
        .register()

    val httpRequestDuration = Histogram.build(
        "http_request_duration_seconds",
        "Distribution of http request duration"
    )
        .register()

    intercept(ApplicationCallPipeline.Monitoring) {
        try {
            if (call.request.uri in ignoredPaths) return@intercept proceed()
            logger.info("incoming callId=${call.callId} method=${call.request.httpMethod.value} uri=${call.request.uri}")
            httpRequestDuration.startTimer().use {
                proceed()
            }
        } catch (err: Throwable) {
            logger.error("exception thrown during processing: ${err.message} callId=${call.callId}")
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
        httpRequestCounter.labels(call.request.httpMethod.value, "${status.value}").inc()
    }
}
