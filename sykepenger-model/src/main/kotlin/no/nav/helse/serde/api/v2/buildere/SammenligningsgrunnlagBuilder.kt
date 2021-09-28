package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.person.*
import no.nav.helse.serde.api.v2.Arbeidsgiverinntekt
import no.nav.helse.serde.api.v2.InntekterFraAOrdningen
import no.nav.helse.serde.api.v2.Inntektkilde
import no.nav.helse.serde.api.v2.OmregnetÅrsinntekt
import java.time.LocalDate
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
        fun sammenligningsgrunnlag(skjæringstidspunkt: LocalDate) = sammenligningsgrunnlagDTO[skjæringstidspunkt]
    }

    private class SammenligningsgrunnlagBuilder(arbeidsgiver: Arbeidsgiver) : ArbeidsgiverVisitor {
        private var nyesteInnslag: NyesteInnslag? = null

        init {
            arbeidsgiver.accept(this)

        }

        fun build() = nyesteInnslag

        override fun preVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) {
            if (nyesteInnslag != null) return
            nyesteInnslag = NyesteInnslag(
                InntektsopplysningBuilder(innslag).build()
            )
        }
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
}

internal data class IArbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetÅrsinntekt: IOmregnetÅrsinntekt?,
    val sammenligningsgrunnlag: Double? = null
) {
    internal fun toDTO(): Arbeidsgiverinntekt {
        return Arbeidsgiverinntekt(
            organisasjonsnummer = arbeidsgiver,
            omregnetÅrsinntekt = omregnetÅrsinntekt?.toDTO(),
            sammenligningsgrunnlag = sammenligningsgrunnlag
        )
    }
}

internal data class IOmregnetÅrsinntekt(
    val kilde: IInntektkilde,
    val beløp: Double,
    val månedsbeløp: Double,
    val inntekterFraAOrdningen: List<IInntekterFraAOrdningen>? = null //kun gyldig for A-ordningen
) {
    internal fun toDTO(): OmregnetÅrsinntekt {
        return OmregnetÅrsinntekt(
            kilde = kilde.toDTO(),
            beløp = beløp,
            månedsbeløp = månedsbeløp,
            inntekterFraAOrdningen = inntekterFraAOrdningen?.map { it.toDTO() }
        )
    }
}

internal enum class IInntektkilde {
    Saksbehandler, Inntektsmelding, Infotrygd, AOrdningen;

    internal fun toDTO() = when (this) {
        Saksbehandler -> Inntektkilde.Saksbehandler
        Inntektsmelding -> Inntektkilde.Inntektsmelding
        Infotrygd -> Inntektkilde.Infotrygd
        AOrdningen -> Inntektkilde.AOrdningen
    }
}

internal data class IInntekterFraAOrdningen(
    val måned: YearMonth,
    val sum: Double
) {
    internal fun toDTO(): InntekterFraAOrdningen {
        return InntekterFraAOrdningen(måned, sum)
    }
}



