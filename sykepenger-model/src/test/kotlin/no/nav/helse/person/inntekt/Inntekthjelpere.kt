package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.økonomi.Inntekt

internal fun infotrygd(id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) =
    Infotrygd(id, Inntektsdata(hendelseId, dato, beløp, tidsstempel))

internal fun skattSykepengegrunnlag(
    hendelseId: UUID,
    dato: LocalDate,
    inntektsopplysninger: List<Skatteopplysning>
) =
    SkattSykepengegrunnlag(UUID.randomUUID(), Inntektsdata(hendelseId, dato, Skatteopplysning.omregnetÅrsinntekt(inntektsopplysninger), LocalDateTime.now()), inntektsopplysninger)

fun skjønnsmessigFastsatt(
    dato: LocalDate,
    beløp: Inntekt,
    overstyrtInntekt: Inntektsopplysning
) = SkjønnsmessigFastsatt(
    id = UUID.randomUUID(),
    inntektsdata = Inntektsdata(
        hendelseId = UUID.randomUUID(),
        dato = dato,
        beløp = beløp,
        tidsstempel = LocalDateTime.now()
    ),
    overstyrtInntekt = overstyrtInntekt,
    omregnetÅrsinntekt = overstyrtInntekt.omregnetÅrsinntekt()
)
