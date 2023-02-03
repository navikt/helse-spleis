package no.nav.helse.person.etterlevelse

import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.person.SammenligningsgrunnlagVisitor
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.økonomi.Inntekt
import kotlin.properties.Delegates

internal class SammenligningsgrunnlagBuilder(sammenligningsgrunnlag: Sammenligningsgrunnlag) :
    SammenligningsgrunnlagVisitor {
    private var sammenligningsgrunnlag by Delegates.notNull<Double>()
    private val inntekter = mutableMapOf<String, List<Map<String, Any>>>()
    private lateinit var inntektliste: MutableList<Map<String, Any>>

    init {
        sammenligningsgrunnlag.accept(this)
    }

    fun build() = SubsumsjonObserver.SammenligningsgrunnlagDTO(sammenligningsgrunnlag, inntekter)

    override fun preVisitSammenligningsgrunnlag(sammenligningsgrunnlag1: Sammenligningsgrunnlag, sammenligningsgrunnlag: Inntekt) {
        this.sammenligningsgrunnlag = sammenligningsgrunnlag.reflection { årlig, _, _, _ -> årlig }
    }
    override fun preVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
        arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag,
        orgnummer: String,
        rapportertInntekt: Inntekt
    ) {
        inntektliste = mutableListOf()
        inntekter[orgnummer] = inntektliste
    }

    override fun visitSkatteopplysning(
        skatteopplysning: Skatteopplysning,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Skatteopplysning.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        inntektliste.add(
            mapOf(
                "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                "årMåned" to måned,
                "type" to type.fromInntekttype(),
                "fordel" to fordel,
                "beskrivelse" to beskrivelse
            )
        )
    }

    private fun Skatteopplysning.Inntekttype.fromInntekttype() = when (this) {
        Skatteopplysning.Inntekttype.LØNNSINNTEKT -> "LØNNSINNTEKT"
        Skatteopplysning.Inntekttype.NÆRINGSINNTEKT -> "NÆRINGSINNTEKT"
        Skatteopplysning.Inntekttype.PENSJON_ELLER_TRYGD -> "PENSJON_ELLER_TRYGD"
        Skatteopplysning.Inntekttype.YTELSE_FRA_OFFENTLIGE -> "YTELSE_FRA_OFFENTLIGE"
    }
    companion object {
        internal fun Sammenligningsgrunnlag.subsumsjonsformat(): SubsumsjonObserver.SammenligningsgrunnlagDTO = SammenligningsgrunnlagBuilder(this).build()
    }
}