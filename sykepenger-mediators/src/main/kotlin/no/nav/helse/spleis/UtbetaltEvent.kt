package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje

internal fun PersonObserver.UtbetaltEvent.toJson() = objectMapper.writeValueAsString(
    mapOf(
        "@event_name" to "utbetalt",
        "aktørId" to this.aktørId,
        "fødselsnummer" to this.fødselsnummer,
        "utbetalingsreferanse" to this.utbetalingsreferanse,
        "vedtaksperiodeId" to this.vedtaksperiodeId.toString(),
        "utbetalingslinjer" to this.utbetalingslinjer.toJson(),
        "forbrukteSykedager" to this.forbrukteSykedager,
        "opprettet" to this.opprettet
    )
)

private fun List<Utbetalingslinje>.toJson(): List<Map<String, Any>> = this.map {
    mapOf(
        "dagsats" to it.dagsats,
        "fom" to it.fom,
        "tom" to it.tom
    )
}


