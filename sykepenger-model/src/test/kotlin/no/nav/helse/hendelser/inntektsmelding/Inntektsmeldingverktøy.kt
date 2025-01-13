package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

typealias Avsenderutleder = (førsteFraværsdag: LocalDate?) -> Inntektsmelding.Avsendersystem

val LPS: Avsenderutleder = { førsteFraværsdag -> Inntektsmelding.Avsendersystem.LPS(førsteFraværsdag) }
val ALTINN: Avsenderutleder = { førsteFraværsdag -> Inntektsmelding.Avsendersystem.Altinn(førsteFraværsdag) }

fun Inntektsmelding.validert() = apply { valider(emptyList(), Aktivitetslogg()) { _, _, _ -> } }
