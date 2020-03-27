package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver

internal fun PersonObserver.ManglendeInntektsmeldingEvent.toJson() = objectMapper.writeValueAsString(
    mapOf(
        "@event_name" to "trenger_inntektsmelding",
        "vedtaksperiodeId" to vedtaksperiodeId,
        "fødselsnummer" to fødselsnummer,
        "organisasjonsnummer" to organisasjonsnummer,
        "opprettet" to opprettet,
        "fom" to fom,
        "tom" to tom
    )
)
