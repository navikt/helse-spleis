package no.nav.helse.person.inntekt

import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.økonomi.Inntekt

internal interface SkatteopplysningVisitor {
    fun visitSkatteopplysning(
        skatteopplysning: Skatteopplysning,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Skatteopplysning.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {}
}