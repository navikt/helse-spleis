package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver
import no.nav.helse.rapids_rivers.JsonMessage

internal fun PersonObserver.UtbetaltEvent.toJson() = JsonMessage.newMessage(mapOf(
    "@event_name" to "utbetalt",
    "aktørId" to this.aktørId,
    "fødselsnummer" to this.fødselsnummer,
    "utbetaling" to this.utbetaling,
        "vedtaksperiodeId" to this.vedtaksperiodeId.toString(),
    "forbrukteSykedager" to this.forbrukteSykedager,
    "opprettet" to this.opprettet
)).toJson()


