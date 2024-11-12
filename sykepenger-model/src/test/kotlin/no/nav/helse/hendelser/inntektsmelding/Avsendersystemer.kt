package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding

object Avsendersystemer {
    val NAV_NO: (vedtaksperiodeId: UUID, inntektsdato: LocalDate, førsteFraværsdag: LocalDate?) -> Inntektsmelding.Avsendersystem = { vedtaksperiodeId: UUID, inntektsdato: LocalDate, _ -> Inntektsmelding.Avsendersystem.Nav(vedtaksperiodeId, inntektsdato) }
    val NAV_NO_SELVBESTEMT: (vedtaksperiodeId: UUID, inntektsdato: LocalDate, førsteFraværsdag: LocalDate?) -> Inntektsmelding.Avsendersystem = { vedtaksperiodeId: UUID, inntektsdato: LocalDate, _ -> Inntektsmelding.Avsendersystem.NavSelvbestemt(vedtaksperiodeId, inntektsdato) }
    val LPS: (vedtaksperiodeId: UUID, inntektsdato: LocalDate, førsteFraværsdag: LocalDate?) -> Inntektsmelding.Avsendersystem = { _,_, førsteFraværsdag -> Inntektsmelding.Avsendersystem.LPS(førsteFraværsdag) }
    val ALTINN: (vedtaksperiodeId: UUID, inntektsdato: LocalDate, førsteFraværsdag: LocalDate?) -> Inntektsmelding.Avsendersystem = { _,_, førsteFraværsdag ->  Inntektsmelding.Avsendersystem.Altinn(førsteFraværsdag) }
}