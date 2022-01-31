package no.nav.helse.hendelser

import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.kilder
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosent.Companion.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT
import java.time.LocalDate

class Inntektsvurdering(private val inntekter: List<ArbeidsgiverInntekt>) {
    private lateinit var avviksprosent: Prosent

    internal fun avviksprosent() = avviksprosent

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt,
        antallArbeidsgivereFraAareg: Int,
        subsumsjonObserver: SubsumsjonObserver
    ): Boolean {
        if (inntekter.antallMåneder() > 12) aktivitetslogg.error("Forventer 12 eller færre inntektsmåneder")
        if (inntekter.kilder(3) > antallArbeidsgivereFraAareg) {
            aktivitetslogg.warn("Bruker har flere inntektskilder de siste tre månedene enn arbeidsforhold som er oppdaget i Aa-registeret.")
        }
        avviksprosent = grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag)
        return validerAvvik(
            aktivitetslogg, avviksprosent, grunnlagForSykepengegrunnlag.grunnlagForSykepengegrunnlag, sammenligningsgrunnlag, subsumsjonObserver
        ) { melding, tillattAvvik ->
            error(melding, tillattAvvik)
        }
    }

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) =
        ArbeidsgiverInntekt.lagreSammenligningsgrunnlag(inntekter, person, skjæringstidspunkt, hendelse)

    internal companion object {
        internal fun validerAvvik(
            aktivitetslogg: IAktivitetslogg,
            avvik: Prosent,
            grunnlagForSykepengegrunnlag: Inntekt,
            sammenligningsgrunnlag: Inntekt,
            subsumsjonObserver: SubsumsjonObserver,
            onFailure: IAktivitetslogg.(melding: String, tillattAvvik: Double) -> Unit
        ): Boolean {
            val harAkseptabeltAvvik = sjekkAvvik(avvik, aktivitetslogg, onFailure)
            subsumsjonObserver.`§ 8-30 ledd 2 punktum 1`(harAkseptabeltAvvik, MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT, grunnlagForSykepengegrunnlag, sammenligningsgrunnlag, avvik)
            return harAkseptabeltAvvik
        }

        internal fun sjekkAvvik(avvik: Prosent, aktivitetslogg: IAktivitetslogg, onFailure: IAktivitetslogg.(melding: String, tillattAvvik: Double) -> Unit): Boolean {
            val harAkseptabeltAvvik = harAkseptabeltAvvik(avvik)
            if (harAkseptabeltAvvik) {
                aktivitetslogg.info("Har %.0f %% eller mindre avvik i inntekt (%.2f %%)", MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent(), avvik.prosent())
            } else {
                onFailure(aktivitetslogg, "Har mer enn %.0f %% avvik", MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent())
            }
            return harAkseptabeltAvvik
        }

        private fun harAkseptabeltAvvik(avvik: Prosent) = avvik <= MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT
    }
}
