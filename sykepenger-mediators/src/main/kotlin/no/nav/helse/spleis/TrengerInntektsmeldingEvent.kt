package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver
import no.nav.helse.rapids_rivers.JsonMessage

internal fun PersonObserver.ManglendeInntektsmeldingEvent.toJson() = JsonMessage.newMessage(mapOf(
    "@event_name" to "trenger_inntektsmelding",
    "vedtaksperiodeId" to vedtaksperiodeId,
    "fødselsnummer" to fødselsnummer,
    "organisasjonsnummer" to organisasjonsnummer,
    "opprettet" to opprettet,
    "fom" to fom,
    "tom" to tom
)).toJson()
