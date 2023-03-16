package no.nav.helse.utbetalingslinjer

import java.time.LocalDateTime
import java.util.UUID

interface Beløpkilde {
    fun arbeidsgiverbeløp(): Int
    fun personbeløp(): Int
}

interface UtbetalingVedtakFattetBuilder {
    fun utbetalingVurdert(tidspunkt: LocalDateTime): UtbetalingVedtakFattetBuilder
    fun utbetalingId(id: UUID): UtbetalingVedtakFattetBuilder
}