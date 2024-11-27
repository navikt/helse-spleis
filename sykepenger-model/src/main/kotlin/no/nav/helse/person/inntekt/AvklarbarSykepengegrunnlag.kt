package no.nav.helse.person.inntekt

import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed class AvklarbarSykepengegrunnlag(
    id: UUID,
    hendelseId: UUID,
    dato: LocalDate,
    beløp: Inntekt,
    tidsstempel: LocalDateTime,
) : Inntektsopplysning(id, hendelseId, dato, beløp, tidsstempel)
