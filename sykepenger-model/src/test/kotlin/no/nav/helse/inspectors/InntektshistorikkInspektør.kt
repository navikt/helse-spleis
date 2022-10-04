package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.økonomi.Inntekt

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

    internal fun omregnetÅrsinntekt(dato: LocalDate, førsteFraværsdag: LocalDate) = inntektshistorikk.omregnetÅrsinntekt(dato, førsteFraværsdag, Arbeidsforholdhistorikk())

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
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektsopplysninger.add(
            Opplysning(
                dato,
                inntektsmelding.omregnetÅrsinntekt(dato, dato)?.omregnetÅrsinntekt(),
                inntektsmelding.rapportertInntekt(dato)?.rapportertInntekt(),
                Kilde.INNTEKTSMELDING
            )
        )
    }

    override fun visitInfotrygd(
        infotrygd: Inntektshistorikk.Infotrygd,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektsopplysninger.add(
            Opplysning(
                dato,
                infotrygd.omregnetÅrsinntekt(dato, dato)?.omregnetÅrsinntekt(),
                infotrygd.rapportertInntekt(dato)?.rapportertInntekt(),
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

    override fun visitSkattRapportertInntekt(
        rapportertInntekt: Inntektshistorikk.Skatt.RapportertInntekt,
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
                skattComposite.omregnetÅrsinntekt(skattedato, skattedato)?.omregnetÅrsinntekt(),
                skattComposite.rapportertInntekt(skattedato)?.rapportertInntekt(),
                Kilde.SKATT
            )
        )
    }

    override fun visitSaksbehandler(
        saksbehandler: Inntektshistorikk.Saksbehandler,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektsopplysninger.add(
            Opplysning(
                dato,
                saksbehandler.omregnetÅrsinntekt(dato, dato)?.omregnetÅrsinntekt(),
                saksbehandler.rapportertInntekt(dato)?.rapportertInntekt(),
                Kilde.SAKSBEHANDLER
            )
        )
    }
}
