package no.nav.helse.hendelser

import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.kilder
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Periodetype.FORLENGELSE
import no.nav.helse.person.Periodetype.INFOTRYGDFORLENGELSE
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
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
        grunnlagForSykepengegrunnlag: Inntekt,
        sammenligningsgrunnlag: Inntekt,
        periodetype: Periodetype,
        antallArbeidsgivereMedOverlappendeVedtaksperioder: Int
    ): Boolean {

        if (inntekter.antallMåneder() > 12) aktivitetslogg.error("Forventer 12 eller færre inntektsmåneder")
        if (inntekter.kilder(3) > antallArbeidsgivereMedOverlappendeVedtaksperioder) {
            val melding =
                "Brukeren har flere inntekter de siste tre måneder enn det som er brukt i sykepengegrunnlaget. Kontroller om brukeren har andre arbeidsforhold eller ytelser på sykmeldingstidspunktet som påvirker utbetalingen."
            if (periodetype in listOf(INFOTRYGDFORLENGELSE, FORLENGELSE))
                aktivitetslogg.info(melding)
            else {
                aktivitetslogg.warn(melding)
            }
        }
        if (sammenligningsgrunnlag <= Inntekt.INGEN) {
            aktivitetslogg.error("sammenligningsgrunnlaget er <= 0")
            return false
        }
        grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag).also { avvik ->
            avviksprosent = avvik
            if (avvik > MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT) {
                aktivitetslogg.lovtrace.`§8-30 ledd 2`(false)
                aktivitetslogg.error(
                    "Har mer enn %.0f %% avvik",
                    MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent()
                )
                return false
            } else {
                aktivitetslogg.lovtrace.`§8-30 ledd 2`(true)
                aktivitetslogg.info(
                    "Har %.0f %% eller mindre avvik i inntekt (%.2f %%)",
                    MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent(),
                    avvik.prosent()
                )
            }
        }
        return true
    }

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) =
        ArbeidsgiverInntekt.lagreInntekter(inntekter, person, skjæringstidspunkt, hendelse)
}
