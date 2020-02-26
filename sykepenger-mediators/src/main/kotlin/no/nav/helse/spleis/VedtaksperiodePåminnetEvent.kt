package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.Påminnelse

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

internal fun Påminnelse.toJson() = objectMapper.writeValueAsString(
    mapOf(
        "@event_name" to "vedtaksperiode_påminnet",
        "aktørId" to this.aktørId(),
        "fødselsnummer" to this.fødselsnummer(),
        "organisasjonsnummer" to this.organisasjonsnummer(),
        "vedtaksperiodeId" to this.vedtaksperiodeId,
        "tilstand" to this.tilstand(),
        "antallGangerPåminnet" to this.antallGangerPåminnet(),
        "tilstandsendringstidspunkt" to this.tilstandsendringstidspunkt(),
        "påminnelsestidspunkt" to this.påminnelsestidspunkt(),
        "nestePåminnelsestidspunkt" to this.nestePåminnelsestidspunkt()
    )
)


