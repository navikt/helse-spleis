package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.inntekt.Arbeidsgiverinntekt.Kilde
import no.nav.helse.økonomi.Inntekt

internal fun arbeidsgiverinntekt(
    dato: LocalDate,
    beløp: Inntekt
) =
    FaktaavklartInntekt(
        id = UUID.randomUUID(),
        inntektsdata = Inntektsdata(UUID.randomUUID(), dato, beløp, LocalDateTime.now()),
        inntektsopplysning = Arbeidsgiverinntekt(kilde = Kilde.Arbeidsgiver)
    )

internal fun infotrygd(id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) =
    FaktaavklartInntekt(
        id = UUID.randomUUID(),
        inntektsdata = Inntektsdata(hendelseId, dato, beløp, tidsstempel),
        inntektsopplysning = Infotrygd
    )

internal fun skattSykepengegrunnlag(
    hendelseId: UUID,
    dato: LocalDate,
    inntektsopplysninger: List<Skatteopplysning>
) =
    FaktaavklartInntekt(
        id = UUID.randomUUID(),
        inntektsdata = Inntektsdata(hendelseId, dato, Skatteopplysning.omregnetÅrsinntekt(inntektsopplysninger), LocalDateTime.now()),
        inntektsopplysning = SkattSykepengegrunnlag(inntektsopplysninger)
    )

internal fun skjønnsmessigFastsatt(
    dato: LocalDate,
    beløp: Inntekt
) = SkjønnsmessigFastsatt(
    id = UUID.randomUUID(),
    inntektsdata = Inntektsdata(
        hendelseId = UUID.randomUUID(),
        dato = dato,
        beløp = beløp,
        tidsstempel = LocalDateTime.now()
    )
)
