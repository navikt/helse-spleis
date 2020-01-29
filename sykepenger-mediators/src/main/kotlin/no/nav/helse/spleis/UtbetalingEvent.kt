package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Topics
import no.nav.helse.person.VedtaksperiodeObserver
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import org.apache.kafka.clients.producer.ProducerRecord

internal fun VedtaksperiodeObserver.UtbetalingEvent.producerRecord() = ProducerRecord<String, String>(
    Topics.rapidTopic, this.fødselsnummer, toJson(this)
)

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private fun toJson(event: VedtaksperiodeObserver.UtbetalingEvent) = objectMapper.writeValueAsString(
    mapOf(
        "@event_name" to "utbetaling",
        "aktørId" to event.aktørId,
        "fødselsnummer" to event.fødselsnummer,
        "organisasjonsnummer" to event.organisasjonsnummer,
        "utbetalingsreferanse" to event.utbetalingsreferanse,
        "vedtaksperiodeId" to event.vedtaksperiodeId.toString(),
        "utbetalingslinjer" to event.utbetalingslinjer.toJson(),
        "opprettet" to event.opprettet
    )
)

private fun List<Utbetalingslinje>.toJson(): List<Map<String, Any>> = this.map {
    mapOf(
        "dagsats" to it.dagsats,
        "fom" to it.fom,
        "tom" to it.tom
    )
}


