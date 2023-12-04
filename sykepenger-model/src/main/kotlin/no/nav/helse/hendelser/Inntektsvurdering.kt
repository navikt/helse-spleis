package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.etterlevelse.SammenligningsgrunnlagBuilder.Companion.subsumsjonsformat
class Inntektsvurdering(private val inntekter: List<ArbeidsgiverInntekt>) {
    init {
        require(inntekter.antallMåneder() <= 12) { "Forventer 12 eller færre inntektsmåneder" }
    }

    internal fun valider(): Boolean {
        return false
    }

    internal fun sammenligningsgrunnlag(skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID, subsumsjonObserver: SubsumsjonObserver): Sammenligningsgrunnlag {
        if (Toggle.AvviksvurderingFlyttet.enabled) return Sammenligningsgrunnlag(emptyList())
        val arbeidsgiverInntektsopplysninger = inntekter.map {
            it.tilSammenligningsgrunnlag(meldingsreferanseId)
        }
        val sammenligningsgrunnlag = Sammenligningsgrunnlag(arbeidsgiverInntektsopplysninger)
        subsumsjonObserver.`§ 8-30 ledd 2`(skjæringstidspunkt, sammenligningsgrunnlag.subsumsjonsformat())
        return sammenligningsgrunnlag
    }
}
