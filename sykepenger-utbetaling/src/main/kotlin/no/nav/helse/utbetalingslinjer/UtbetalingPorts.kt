package no.nav.helse.utbetalingslinjer

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

interface GrunnbeløpsreguleringPort: IAktivitetslogg {
    fun erRelevant(fagsystemId: String): Boolean
    fun fødselsnummer(): String
    fun organisasjonsnummer(): String
}

interface Beløpkilde {
    fun arbeidsgiverbeløp(): Int
    fun personbeløp(): Int
}

interface UtbetalingVedtakFattetBuilder {
    fun utbetalingVurdert(tidspunkt: LocalDateTime): UtbetalingVedtakFattetBuilder
    fun utbetalingId(id: UUID): UtbetalingVedtakFattetBuilder
}