package no.nav.helse.hendelser

import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosent.Companion.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT
import java.time.LocalDate

class Inntektsvurdering(
    private val inntekter: List<ArbeidsgiverInntekt>
) {

    private var avviksprosent: Prosent? = null

    internal fun avviksprosent() = avviksprosent

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt
    ): Boolean {
        if (inntekter.antallMåneder() > 12) aktivitetslogg.error("Forventer 12 eller færre inntektsmåneder")
        if (sammenligningsgrunnlag <= Inntekt.INGEN) {
            aktivitetslogg.error("sammenligningsgrunnlaget er <= 0")
            return false
        }
        grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag).also { avvik ->
            avviksprosent = avvik
            val akseptabeltAvvik = avvik <= MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT
            aktivitetslogg.etterlevelse.`§8-30 ledd 2`(
                akseptabeltAvvik,
                MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT,
                grunnlagForSykepengegrunnlag.grunnlagForSykepengegrunnlag,
                sammenligningsgrunnlag,
                avvik
            )
            if (akseptabeltAvvik) {
                aktivitetslogg.info("Har %.0f %% eller mindre avvik i inntekt (%.2f %%)", MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent(), avvik.prosent())
            } else {
                aktivitetslogg.error("Har mer enn %.0f %% avvik", MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent())
            }
            return akseptabeltAvvik
        }
    }

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) =
        ArbeidsgiverInntekt.lagreSammenligningsgrunnlag(inntekter, person, skjæringstidspunkt, hendelse)
}
