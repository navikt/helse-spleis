package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.serde.UtbetalingslinjeData
import no.nav.helse.utbetalingslinjer.Linjetype

internal fun PersonObserver.UtbetaltEvent.toJson() = JsonMessage.newMessage(mapOf(
    "@event_name" to "utbetalt",
    "aktørId" to this.aktørId,
    "fødselsnummer" to this.fødselsnummer,
    "utbetalingsreferanse" to this.utbetalingsreferanse,
    "vedtaksperiodeId" to this.vedtaksperiodeId.toString(),
    "utbetalingslinjer" to this.utbetalingslinjer.toJson(),
    "forbrukteSykedager" to this.forbrukteSykedager,
    "opprettet" to this.opprettet
)).toJson()

private fun List<UtbetalingslinjeData>.toJson(): List<Map<String, Any?>> = this.map {
    mapOf(
        "dagsats" to it.dagsats,
        "fom" to it.fom,
        "tom" to it.tom,
        "grad" to it.grad,
        "delytelseId" to it.delytelseId,
        "refDelytelseId" to it.refDelytelseId,
        "linjetype" to Linjetype.valueOf(it.linjetype)
    )
}


