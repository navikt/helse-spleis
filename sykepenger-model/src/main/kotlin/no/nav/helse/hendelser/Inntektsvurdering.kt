package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.kilder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.utenOffentligeYtelser
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.Varselkode.RV_IV_1
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosent.Companion.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT

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
        if (inntekter.antallMåneder() > 12) aktivitetslogg.funksjonellFeil("Forventer 12 eller færre inntektsmåneder")
        if (inntekter.utenOffentligeYtelser().kilder(3) > antallArbeidsgivereFraAareg) aktivitetslogg.varsel(RV_IV_1)
        avviksprosent = grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag, subsumsjonObserver)
        return sjekkAvvik(avviksprosent, aktivitetslogg, IAktivitetslogg::funksjonellFeil, "Har mer enn 25 % avvik")
    }

    internal fun lagreRapporterteInntekter(person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) =
        ArbeidsgiverInntekt.lagreRapporterteInntekter(inntekter, person, skjæringstidspunkt, hendelse)

    internal companion object {
        internal fun sjekkAvvik(avvik: Prosent, aktivitetslogg: IAktivitetslogg, onFailure: IAktivitetslogg.(melding: String) -> Unit, melding: String): Boolean {
            val harAkseptabeltAvvik = harAkseptabeltAvvik(avvik)
            if (harAkseptabeltAvvik) {
                aktivitetslogg.info("Har %.0f %% eller mindre avvik i inntekt (%.2f %%)", MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent(), avvik.prosent())
            } else {
                onFailure(aktivitetslogg, melding)
            }
            return harAkseptabeltAvvik
        }

        private fun harAkseptabeltAvvik(avvik: Prosent) = avvik <= MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT
    }
}
