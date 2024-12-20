package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

typealias Avsenderutleder = (vedtaksperiodeId: UUID, inntektsdato: LocalDate?, førsteFraværsdag: LocalDate?) -> Inntektsmelding.Avsendersystem

val NAV_NO: Avsenderutleder = { vedtaksperiodeId: UUID, inntektsdato: LocalDate?, _ -> Inntektsmelding.Avsendersystem.NavPortal(vedtaksperiodeId, inntektsdato, true) }
val NAV_NO_SELVBESTEMT: Avsenderutleder = { vedtaksperiodeId: UUID, inntektsdato: LocalDate?, _ -> Inntektsmelding.Avsendersystem.NavPortal(vedtaksperiodeId, inntektsdato, false) }
val LPS: Avsenderutleder = { _, _, førsteFraværsdag -> Inntektsmelding.Avsendersystem.LPS(førsteFraværsdag) }
val ALTINN: Avsenderutleder = { _, _, førsteFraværsdag -> Inntektsmelding.Avsendersystem.Altinn(førsteFraværsdag) }

private fun utledet(avsenderutleder: Avsenderutleder) = avsenderutleder(UUID.randomUUID(), LocalDate.EPOCH, null)
fun erNavPortal(avsenderutleder: Avsenderutleder) = utledet(avsenderutleder).let { it is Inntektsmelding.Avsendersystem.NavPortal }
fun erForespurtNavPortal(avsenderutleder: Avsenderutleder) = utledet(avsenderutleder).let { it is Inntektsmelding.Avsendersystem.NavPortal && it.forespurt }
fun Inntektsmelding.validert() = apply { valider(emptyList(), Aktivitetslogg()) { _, _, _ -> } }
