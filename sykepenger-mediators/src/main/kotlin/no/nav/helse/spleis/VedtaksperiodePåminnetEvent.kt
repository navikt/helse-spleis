package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Topics
import no.nav.helse.hendelser.Påminnelse
import org.apache.kafka.clients.producer.ProducerRecord

internal fun Påminnelse.producerRecord() =
    ProducerRecord<String, String>(
        Topics.rapidTopic,
        this.fødselsnummer(),
        toJson(this)
    )

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private fun toJson(event: Påminnelse) = objectMapper.writeValueAsString(
    mapOf(
        "@event_name" to "vedtaksperiode_påminnet",
        "aktørId" to event.aktørId(),
        "fødselsnummer" to event.fødselsnummer(),
        "organisasjonsnummer" to event.organisasjonsnummer(),
        "vedtaksperiodeId" to event.vedtaksperiodeId,
        "tilstand" to event.tilstand(),
        "antallGangerPåminnet" to event.antallGangerPåminnet(),
        "tilstandsendringstidspunkt" to event.tilstandsendringstidspunkt(),
        "påminnelsestidspunkt" to event.påminnelsestidspunkt(),
        "nestePåminnelsestidspunkt" to event.nestePåminnelsestidspunkt()
    )
)


