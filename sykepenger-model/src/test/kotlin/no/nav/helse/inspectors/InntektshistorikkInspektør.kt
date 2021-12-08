package no.nav.helse.inspectors

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
    internal lateinit var inntektshistorikk: Inntektshistorikk

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
    internal fun antallOpplysinger(
        vararg kilder: Kilde = Kilde.values(),
        element: (List<Innslag>) -> Innslag = { it.last() }
    ) = element(innslag.reversed()).opplysninger.filter { it.kilde in kilder }.size
    internal val sisteInnslag get() = innslag.firstOrNull()

    internal fun grunnlagForSykepengegrunnlag(dato: LocalDate, førsteFraværsdag: LocalDate) = inntektshistorikk.grunnlagForSykepengegrunnlag(dato, førsteFraværsdag)

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
                inntektsmelding.grunnlagForSykepengegrunnlag(dato, dato)?.grunnlagForSykepengegrunnlag(),
                inntektsmelding.grunnlagForSammenligningsgrunnlag(dato)?.grunnlagForSammenligningsgrunnlag(),
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
                infotrygd.grunnlagForSykepengegrunnlag(dato, dato)?.grunnlagForSykepengegrunnlag(),
                infotrygd.grunnlagForSammenligningsgrunnlag(dato)?.grunnlagForSammenligningsgrunnlag(),
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

    override fun postVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
        inntektsopplysninger.add(
            Opplysning(
                skattedato,
                skattComposite.grunnlagForSykepengegrunnlag(skattedato, skattedato)?.grunnlagForSykepengegrunnlag(),
                skattComposite.grunnlagForSammenligningsgrunnlag(skattedato)?.grunnlagForSammenligningsgrunnlag(),
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
                saksbehandler.grunnlagForSykepengegrunnlag(dato, dato)?.grunnlagForSykepengegrunnlag(),
                saksbehandler.grunnlagForSammenligningsgrunnlag(dato)?.grunnlagForSammenligningsgrunnlag(),
                Kilde.SAKSBEHANDLER
            )
        )
    }
}
