package no.nav.helse.hendelser.inntektsmelding

import no.nav.helse.hendelser.Inntektsmelding
import java.time.LocalDate
import java.util.UUID

typealias Avsenderutleder = (vedtaksperiodeId: UUID, inntektsdato: LocalDate, førsteFraværsdag: LocalDate?) -> Inntektsmelding.Avsendersystem

val NAV_NO: Avsenderutleder = { vedtaksperiodeId: UUID, inntektsdato: LocalDate, _ ->
    Inntektsmelding.Avsendersystem.NavPortal(
        vedtaksperiodeId,
        inntektsdato,
        true,
    )
}
val NAV_NO_SELVBESTEMT: Avsenderutleder = {
    vedtaksperiodeId: UUID,
    inntektsdato: LocalDate,
    _,
    ->
    Inntektsmelding.Avsendersystem.NavPortal(vedtaksperiodeId, inntektsdato, false)
}
val LPS: Avsenderutleder = { _, _, førsteFraværsdag -> Inntektsmelding.Avsendersystem.LPS(førsteFraværsdag) }
val ALTINN: Avsenderutleder = { _, _, førsteFraværsdag -> Inntektsmelding.Avsendersystem.Altinn(førsteFraværsdag) }

fun erNavPortal(avsenderutleder: Avsenderutleder) =
    avsenderutleder(UUID.randomUUID(), LocalDate.EPOCH, null) is Inntektsmelding.Avsendersystem.NavPortal
