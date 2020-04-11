package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver
import no.nav.helse.rapids_rivers.JsonMessage

internal fun PersonObserver.UtbetaltEvent.toJson() = JsonMessage.newMessage(
    mapOf(
        "@event_name" to "utbetalt",
        "aktørId" to this.aktørId,
        "fødselsnummer" to this.fødselsnummer,
        "gruppeId" to this.gruppeId.toString(),
        "vedtaksperiodeId" to this.vedtaksperiodeId.toString(),
        "utbetaling" to this.utbetalingslinjer.map { it.toJson() },
        "forbrukteSykedager" to this.forbrukteSykedager,
        "opprettet" to this.opprettet
    )
).toJson()

private fun PersonObserver.Utbetalingslinjer.toJson() = mapOf(
    "utbetalingsreferanse" to utbetalingsreferanse,
    "utbetalingslinjer" to utbetalingslinjer.map { it.toJson() }
)

private fun PersonObserver.Utbetalingslinje.toJson() = mapOf(
    "fom" to fom,
    "tom" to tom,
    "dagsats" to dagsats,
    "grad" to grad
)
