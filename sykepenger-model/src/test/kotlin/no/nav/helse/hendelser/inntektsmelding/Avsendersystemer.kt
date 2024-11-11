package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding

object Avsendersystemer {
    val NAV_NO: (vedtaksperiodeId: UUID, inntektsdato: LocalDate) -> Inntektsmelding.Avsendersystem = { vedtaksperiodeId: UUID, inntektsdato: LocalDate -> Inntektsmelding.Avsendersystem.NAV_NO(vedtaksperiodeId, inntektsdato) }
    val NAV_NO_SELVBESTEMT: (vedtaksperiodeId: UUID, inntektsdato: LocalDate) -> Inntektsmelding.Avsendersystem = { vedtaksperiodeId: UUID, inntektsdato: LocalDate -> Inntektsmelding.Avsendersystem.NAV_NO_SELVBESTEMT(vedtaksperiodeId, inntektsdato) }
    val LPS: (vedtaksperiodeId: UUID, inntektsdato: LocalDate) -> Inntektsmelding.Avsendersystem = { _,_ -> Inntektsmelding.Avsendersystem.LPS }
    val ALTINN: (vedtaksperiodeId: UUID, inntektsdato: LocalDate) -> Inntektsmelding.Avsendersystem = { _,_ ->  Inntektsmelding.Avsendersystem.ALTINN }
}

fun main() {
    Avsendersystemer.NAV_NO
}