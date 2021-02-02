package no.nav.helse.spleis.e2e

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal enum class Kilde {
    SKATT, INFOTRYGD, INNTEKTSMELDING, SAKSBEHANDLER
}

internal class InntektshistorikkVol2Inspektør(arbeidsgiver: Arbeidsgiver) : ArbeidsgiverVisitor {

    private val innslag = mutableListOf<List<Opplysning>>()
    private val inntektsopplysninger = mutableListOf<Opplysning>()
    private lateinit var inntektshistorikk: InntektshistorikkVol2

    class Opplysning(
        val dato: LocalDate,
        val sykepengegrunnlag: Inntekt?,
        val sammenligningsgrunnlag: Inntekt?,
        val kilde: Kilde
    )

    init {
        arbeidsgiver.accept(this)
    }

    val antallInnslag get() = innslag.size
    internal val sisteInnslag get() = innslag.firstOrNull()

    internal fun grunnlagForSykepengegrunnlag(dato: LocalDate) = inntektshistorikk.grunnlagForSykepengegrunnlag(dato)

    internal fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) = inntektshistorikk.grunnlagForSammenligningsgrunnlag(dato)

    override fun preVisitInntekthistorikkVol2(inntektshistorikk: InntektshistorikkVol2) {
        this.inntektshistorikk = inntektshistorikk
    }

    override fun preVisitInnslag(innslag: InntektshistorikkVol2.Innslag, id: UUID) {
        inntektsopplysninger.clear()
    }

    override fun postVisitInnslag(innslag: InntektshistorikkVol2.Innslag, id: UUID) {
        this.innslag.add(inntektsopplysninger.toList())
    }

    override fun visitInntektsmelding(
        inntektsmelding: InntektshistorikkVol2.Inntektsmelding,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektsopplysninger.add(
            Opplysning(
                dato,
                inntektsmelding.grunnlagForSykepengegrunnlag(dato)?.second,
                inntektsmelding.grunnlagForSammenligningsgrunnlag(dato)?.second,
                Kilde.INNTEKTSMELDING
            )
        )
    }

    override fun visitInfotrygd(
        infotrygd: InntektshistorikkVol2.Infotrygd,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektsopplysninger.add(
            Opplysning(
                dato,
                infotrygd.grunnlagForSykepengegrunnlag(dato)?.second,
                infotrygd.grunnlagForSammenligningsgrunnlag(dato)?.second,
                Kilde.INFOTRYGD
            )
        )
    }

    private lateinit var skattedato: LocalDate

    override fun visitSkattSykepengegrunnlag(
        sykepengegrunnlag: InntektshistorikkVol2.Skatt.Sykepengegrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: InntektshistorikkVol2.Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        skattedato = dato
    }

    override fun visitSkattSammenligningsgrunnlag(
        sammenligningsgrunnlag: InntektshistorikkVol2.Skatt.Sammenligningsgrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: InntektshistorikkVol2.Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        skattedato = dato
    }

    override fun postVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite, id: UUID) {
        inntektsopplysninger.add(
            Opplysning(
                skattedato,
                skattComposite.grunnlagForSykepengegrunnlag(skattedato)?.second,
                skattComposite.grunnlagForSammenligningsgrunnlag(skattedato)?.second,
                Kilde.SKATT
            )
        )
    }
}
