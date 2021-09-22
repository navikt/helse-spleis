package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.person.*
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.PersonVisitor
import no.nav.helse.serde.api.InntektsgrunnlagDTO
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class OppsamletSammenligningsgrunnlagBuilder(person: Person) : PersonVisitor {
    private val akkumulator: MutableMap<String, NyesteInnslag> = mutableMapOf()

    init {
        person.accept(this)
    }

    override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        SammenligningsgrunnlagBuilder(arbeidsgiver).build()?.let { akkumulator[organisasjonsnummer] = it }
    }

    internal fun sammenligningsgrunnlag(organisasjonsnummer: String, skjæringstidspunkt: LocalDate) =
        akkumulator[organisasjonsnummer]?.sammenligningsgrunnlag(skjæringstidspunkt)
}

internal class NyesteInnslag(
    private val sammenligningsgrunnlagDTO: Map<LocalDate, InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO>
) {
    fun sammenligningsgrunnlag(skjæringstidspunkt: LocalDate) =
        sammenligningsgrunnlagDTO[skjæringstidspunkt]
}

internal class SammenligningsgrunnlagBuilder(arbeidsgiver: Arbeidsgiver) : ArbeidsgiverVisitor {

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

internal class InntektsopplysningBuilder(val innslag: Inntektshistorikk.Innslag) : InntekthistorikkVisitor {
    private val akkumulator = mutableMapOf<LocalDate, InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO>()

    init {
        innslag.accept(this)
    }

    fun build() = akkumulator.toMap()

    override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
        skattComposite.sammenligningsgrunnlag()?.let {
            akkumulator.put(
                dato, InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO(
                    beløp = InntektBuilder(it).build().årlig,
                    inntekterFraAOrdningen = InntekterFraAOrdningenBuilder(skattComposite).build()
                )
            )
        }
    }
}

internal class InntekterFraAOrdningenBuilder(skattComposite: Inntektshistorikk.SkattComposite) : InntekthistorikkVisitor {
    private val akkumulator = mutableListOf<InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO.InntekterFraAOrdningenDTO>()

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
            InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.SammenligningsgrunnlagDTO.InntekterFraAOrdningenDTO(måned, InntektBuilder(beløp).build().månedlig)
        )
    }
}


