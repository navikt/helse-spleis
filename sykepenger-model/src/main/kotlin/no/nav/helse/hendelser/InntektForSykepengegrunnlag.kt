package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.avklarSykepengegrunnlag
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.harInntektFor
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.harInntektI
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_3
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag

class InntektForSykepengegrunnlag(
    private val inntekter: List<ArbeidsgiverInntekt>,
    private val arbeidsforhold: List<Arbeidsforhold>
) {
    init {
        require(inntekter.antallMåneder() <= 3L) { "Forventer maks 3 inntektsmåneder" }
    }

    internal fun avklarSykepengegrunnlag(
        hendelse: IAktivitetslogg,
        person: Person,
        rapporterteArbeidsforhold: Map<String, SkattSykepengegrunnlag>,
        skjæringstidspunkt: LocalDate,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        meldingsreferanseId: UUID,
        subsumsjonslogg: Subsumsjonslogg
    ) =
        inntekter.avklarSykepengegrunnlag(hendelse, person, rapporterteArbeidsforhold, skjæringstidspunkt, sammenligningsgrunnlag, meldingsreferanseId, subsumsjonslogg)

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
    ): IAktivitetslogg {
        if (finnerFrilansinntektDeSiste3Månedene())
            aktivitetslogg.funksjonellFeil(RV_IV_3)
        return aktivitetslogg
    }

    private fun finnerFrilansinntektDeSiste3Månedene() =
        arbeidsforhold.any { arbeidsforhold ->
            arbeidsforhold.månedligeArbeidsforhold.any {
                it.erFrilanser && inntekter.harInntektFor(arbeidsforhold.orgnummer, it.yearMonth)
            }
        }

    internal fun finnerFrilansinntektDenSisteMåneden(skjæringstidspunkt: LocalDate): Boolean {
        val månedFørSkjæringstidspunkt = YearMonth.from(skjæringstidspunkt.minusMonths(1))
        return arbeidsforhold.any { arbeidsforhold ->
            arbeidsforhold.månedligeArbeidsforhold.any {
                it.erFrilanser && inntekter.harInntektFor(arbeidsforhold.orgnummer, månedFørSkjæringstidspunkt)
            }
        }
    }

    internal fun harInntektI(måned: YearMonth): Boolean {
        return inntekter.harInntektI(måned)
    }

    class Arbeidsforhold(
        val orgnummer: String,
        val månedligeArbeidsforhold: List<MånedligArbeidsforhold>
    )

    class MånedligArbeidsforhold(
        val yearMonth: YearMonth,
        val erFrilanser: Boolean
    )
}



