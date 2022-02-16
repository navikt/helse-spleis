package no.nav.helse.serde.api.builders

import no.nav.helse.person.*
import no.nav.helse.serde.api.v2.buildere.*
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class InntektshistorikkForAOrdningenBuilder(person: Person): PersonVisitor {
    private val arbeidsgivere = mutableMapOf<String, ArbeidsgiverInntektBuilder>()
    init {
        person.accept(this)
    }

    internal fun hentInntekt(orgnummer: String, skjæringstidspunkt: LocalDate): IOmregnetÅrsinntekt? = arbeidsgivere[orgnummer]
        ?.hentInntekt(skjæringstidspunkt)
        ?.let { (inntekt, inntekterFraAOrdningen) ->
            IOmregnetÅrsinntekt(IInntektkilde.AOrdningen, inntekt.årlig, inntekt.månedlig, inntekterFraAOrdningen)
        }

    override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        arbeidsgivere[arbeidsgiver.organisasjonsnummer()] = ArbeidsgiverInntektBuilder(arbeidsgiver)
    }

    internal class ArbeidsgiverInntektBuilder(arbeidsgiver: Arbeidsgiver) : ArbeidsgiverVisitor {
        private var inntekterFraAOrdningen: Map<LocalDate, Pair<IInntekt, List<IInntekterFraAOrdningen>>>? = null
        init {
            arbeidsgiver.accept(this)
        }

        internal fun hentInntekt(skjæringstidspunkt: LocalDate) = inntekterFraAOrdningen?.get(skjæringstidspunkt)

        override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
            val innslag = inntektshistorikk.nyesteInnslag()
            if (innslag != null) {
                inntekterFraAOrdningen = SkattVisitor(innslag).build()
            }
        }
    }

    private class SkattVisitor(private val innslag: Inntektshistorikk.Innslag) : InntekthistorikkVisitor {
        private val inntekterFraAOrdningen = mutableMapOf<LocalDate, MutableMap<YearMonth, Double>>()

        init {
            innslag.accept(this)
        }

        fun build() = inntekterFraAOrdningen
            .mapValues { (skjæringstidspunnkt, inntekterFraAOrdningen) ->
                InntektBuilder(innslag.grunnlagForSykepengegrunnlag(skjæringstidspunnkt, null)!!.grunnlagForSykepengegrunnlag()).build() to inntekterFraAOrdningen
                    .map { (måned, sum) -> IInntekterFraAOrdningen(måned, sum) }
            }
            .toMap()


        override fun visitSkattSykepengegrunnlag(
            sykepengegrunnlag: Inntektshistorikk.Skatt.Sykepengegrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Inntektshistorikk.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            if (innslag.grunnlagForSykepengegrunnlag(dato, null) == null) return
            val inntekt = InntektBuilder(beløp).build()
            val inntekterFraAOrdningenForSkjæringstidspunkt = inntekterFraAOrdningen.getOrPut(dato, ::mutableMapOf)
            inntekterFraAOrdningenForSkjæringstidspunkt.merge(måned, inntekt.månedlig, Double::plus)
        }
    }
}
