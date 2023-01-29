package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.kilder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.lagreInntekter
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.utenOffentligeYtelser
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_1
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag

class Inntektsvurdering(private val inntekter: List<ArbeidsgiverInntekt>) {
    init {
        require(inntekter.antallMåneder() <= 12) { "Forventer 12 eller færre inntektsmåneder" }
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, antallArbeidsgivereFraAareg: Int): Boolean {
        if (inntekter.utenOffentligeYtelser().kilder(3) > antallArbeidsgivereFraAareg) aktivitetslogg.varsel(RV_IV_1)
        return true
    }

    internal fun lagreInntekter(hendelse: IAktivitetslogg, person: Person, skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID, other: List<ArbeidsgiverInntekt>) =
        (this.inntekter + other).lagreInntekter(hendelse, person, skjæringstidspunkt, meldingsreferanseId)

    internal fun sammenligningsgrunnlag(skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID, subsumsjonObserver: SubsumsjonObserver): Sammenligningsgrunnlag {
        val arbeidsgiverInntektsopplysninger = inntekter.map {
            it.tilSammenligningsgrunnlag(skjæringstidspunkt, meldingsreferanseId)
        }
        val sammenligningsgrunnlag = Sammenligningsgrunnlag(arbeidsgiverInntektsopplysninger)
        subsumsjonObserver.`§ 8-30 ledd 2`(skjæringstidspunkt, sammenligningsgrunnlag.subsumsjonsformat())
        return sammenligningsgrunnlag
    }
}
