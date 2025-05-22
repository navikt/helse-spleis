package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.økonomi.Inntekt

internal fun arbeidsgiverinntekt(
    dato: LocalDate,
    beløp: Inntekt
) =
    ArbeidstakerFaktaavklartInntekt(
        id = UUID.randomUUID(),
        inntektsdata = Inntektsdata(MeldingsreferanseId(UUID.randomUUID()), dato, beløp, LocalDateTime.now()),
        inntektsopplysningskilde = Arbeidstakerinntektskilde.Arbeidsgiver
    )

internal fun infotrygd(id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) =
    ArbeidstakerFaktaavklartInntekt(
        id = UUID.randomUUID(),
        inntektsdata = Inntektsdata(MeldingsreferanseId(hendelseId), dato, beløp, tidsstempel),
        inntektsopplysningskilde = Arbeidstakerinntektskilde.Infotrygd
    )

internal fun skattSykepengegrunnlag(
    hendelseId: UUID,
    dato: LocalDate,
    inntektsopplysninger: List<Skatteopplysning>
) =
    ArbeidstakerFaktaavklartInntekt(
        id = UUID.randomUUID(),
        inntektsdata = Inntektsdata(MeldingsreferanseId(hendelseId), dato, Skatteopplysning.omregnetÅrsinntekt(inntektsopplysninger), LocalDateTime.now()),
        inntektsopplysningskilde = Arbeidstakerinntektskilde.AOrdningen(inntektsopplysninger)
    )

internal fun skjønnsmessigFastsatt(
    dato: LocalDate,
    beløp: Inntekt
) = SkjønnsmessigFastsatt(
    id = UUID.randomUUID(),
    inntektsdata = Inntektsdata(
        hendelseId = MeldingsreferanseId(UUID.randomUUID()),
        dato = dato,
        beløp = beløp,
        tidsstempel = LocalDateTime.now()
    )
)
