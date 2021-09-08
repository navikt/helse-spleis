package no.nav.helse.spleis.e2e

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal enum class Kilde {
    SKATT, INFOTRYGD, INNTEKTSMELDING, SAKSBEHANDLER
}

internal data class Innslag(
    val innslagId: UUID,
    val opplysninger: List<InntektshistorikkInspektør.Opplysning>
)

internal class InntektshistorikkInspektør(arbeidsgiver: Arbeidsgiver) : ArbeidsgiverVisitor {

    private val innslag = mutableListOf<Innslag>()
    private val inntektsopplysninger = mutableListOf<Opplysning>()
    private lateinit var inntektshistorikk: Inntektshistorikk

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

    internal fun grunnlagForSykepengegrunnlag(dato: LocalDate) = inntektshistorikk.grunnlagForSykepengegrunnlagGammel(dato)

    internal fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) = inntektshistorikk.grunnlagForSammenligningsgrunnlag(dato)

    override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
        this.inntektshistorikk = inntektshistorikk
    }

    override fun preVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) {
        inntektsopplysninger.clear()
    }

    override fun postVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) {
        this.innslag.add(Innslag(id, inntektsopplysninger.toList()))
    }

    override fun visitInntektsmelding(
        inntektsmelding: Inntektshistorikk.Inntektsmelding,
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
        infotrygd: Inntektshistorikk.Infotrygd,
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
        skattedato = dato
    }

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
        skattedato = dato
    }

    override fun postVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID) {
        inntektsopplysninger.add(
            Opplysning(
                skattedato,
                skattComposite.grunnlagForSykepengegrunnlag(skattedato)?.second,
                skattComposite.grunnlagForSammenligningsgrunnlag(skattedato)?.second,
                Kilde.SKATT
            )
        )
    }

    override fun visitSaksbehandler(
        saksbehandler: Inntektshistorikk.Saksbehandler,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektsopplysninger.add(
            Opplysning(
                dato,
                saksbehandler.grunnlagForSykepengegrunnlag(dato)?.second,
                saksbehandler.grunnlagForSammenligningsgrunnlag(dato)?.second,
                Kilde.SAKSBEHANDLER
            )
        )
    }
}
