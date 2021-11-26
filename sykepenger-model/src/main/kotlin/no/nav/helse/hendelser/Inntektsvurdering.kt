package no.nav.helse.hendelser

import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.kilder
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-30 ledd 2`
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosent.Companion.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT
import java.time.LocalDate

class Inntektsvurdering(private val inntekter: List<ArbeidsgiverInntekt>) {
    private var avviksprosent: Prosent? = null

    internal fun avviksprosent() = avviksprosent

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt,
        antallArbeidsgivereFraAareg: Int,
    ): Boolean {
        if (inntekter.antallMåneder() > 12) aktivitetslogg.error("Forventer 12 eller færre inntektsmåneder")
        if (inntekter.kilder(3) > antallArbeidsgivereFraAareg) {
            aktivitetslogg.warn("Bruker har flere inntektskilder de siste tre månedene enn arbeidsforhold som er oppdaget i Aa-registeret.")
        }
        grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag).also { avvik ->
            avviksprosent = avvik
            return validerAvvik(aktivitetslogg, avvik, grunnlagForSykepengegrunnlag.grunnlagForSykepengegrunnlag, sammenligningsgrunnlag)
        }
    }

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) =
        ArbeidsgiverInntekt.lagreSammenligningsgrunnlag(inntekter, person, skjæringstidspunkt, hendelse)

    internal companion object {
        internal fun validerAvvik(aktivitetslogg: IAktivitetslogg, avvik: Prosent, grunnlagForSykepengegrunnlag: Inntekt, sammenligningsgrunnlag: Inntekt): Boolean {
            return sjekkAvvik(avvik, aktivitetslogg).also { akseptabeltAvvik ->
                aktivitetslogg.`§8-30 ledd 2`(akseptabeltAvvik, MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT, grunnlagForSykepengegrunnlag, sammenligningsgrunnlag, avvik)
            }
        }

        internal fun sjekkAvvik(avvik: Prosent, aktivitetslogg: IAktivitetslogg): Boolean {
            val akseptabeltAvvik = akseptabeltAvvik(avvik)
            if (akseptabeltAvvik) {
                aktivitetslogg.info("Har %.0f %% eller mindre avvik i inntekt (%.2f %%)", MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent(), avvik.prosent())
            } else {
                aktivitetslogg.error("Har mer enn %.0f %% avvik", MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent())
            }
            return akseptabeltAvvik
        }

        private fun akseptabeltAvvik(avvik: Prosent) = avvik <= MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT
    }
}
