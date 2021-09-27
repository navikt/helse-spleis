package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.person.*
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.PersonVisitor
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

// Samler opp hver arbeidsgivers siste generasjon av sammenligningsgrunnlag per skjæringstidspunkt
internal class OppsamletSammenligningsgrunnlagBuilder(person: Person) : PersonVisitor {
    private val akkumulator: MutableMap<String, NyesteInnslag> = mutableMapOf()

    init {
        person.accept(this)
    }

    internal fun sammenligningsgrunnlag(organisasjonsnummer: String, skjæringstidspunkt: LocalDate) =
        akkumulator[organisasjonsnummer]?.sammenligningsgrunnlag(skjæringstidspunkt)

    override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        SammenligningsgrunnlagBuilder(arbeidsgiver).build()?.let { akkumulator[organisasjonsnummer] = it }
    }

    private class NyesteInnslag(
        private val sammenligningsgrunnlagDTO: Map<LocalDate, Double>
    ) {
        fun sammenligningsgrunnlag(skjæringstidspunkt: LocalDate) =
            sammenligningsgrunnlagDTO[skjæringstidspunkt]
    }

    private class SammenligningsgrunnlagBuilder(arbeidsgiver: Arbeidsgiver) : ArbeidsgiverVisitor {

        private var nyesteInnslag: NyesteInnslag? = null

        init {
            arbeidsgiver.accept(this)

        }

        override fun preVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) {
            if (nyesteInnslag != null) return
            nyesteInnslag = NyesteInnslag(
                InntektsopplysningBuilder(innslag).build()
            )
        }

        fun build() = nyesteInnslag
    }

    private class InntektsopplysningBuilder(innslag: Inntektshistorikk.Innslag) : InntekthistorikkVisitor {
        private val akkumulator = mutableMapOf<LocalDate, Double>()

        init {
            innslag.accept(this)
        }

        fun build() = akkumulator.toMap()

        override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
            skattComposite.sammenligningsgrunnlag()?.let {
                akkumulator.put(dato, InntektBuilder(it).build().årlig)
            }
        }
    }

    private class InntekterFraAOrdningenBuilder(skattComposite: Inntektshistorikk.SkattComposite) : InntekthistorikkVisitor {
        private val akkumulator = mutableListOf<InntekterFraAOrdningen>()

        init {
            skattComposite.accept(this)
        }

        fun build() = akkumulator.toList()

        override fun visitSkattSammenligningsgrunnlag(
            sammenligningsgrunnlag: Inntektshistorikk.Skatt.Sammenligningsgrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Inntektshistorikk.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            akkumulator.add(
                InntekterFraAOrdningen(måned, InntektBuilder(beløp).build().månedlig)
            )
        }
    }
}

internal data class Arbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetÅrsinntekt: OmregnetÅrsinntekt?,
    val sammenligningsgrunnlag: Double? = null
)

internal data class OmregnetÅrsinntekt(
    val kilde: Inntektkilde,
    val beløp: Double,
    val månedsbeløp: Double,
    val inntekterFraAOrdningen: List<InntekterFraAOrdningen>? = null //kun gyldig for A-ordningen
)

internal enum class Inntektkilde {
    Saksbehandler, Inntektsmelding, Infotrygd, AOrdningen
}

internal data class InntekterFraAOrdningen(
    val måned: YearMonth,
    val sum: Double
)



