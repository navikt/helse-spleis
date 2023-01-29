package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.Skatt
import no.nav.helse.person.inntekt.SkattComposite
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

internal enum class Kilde {
    SKATT, INFOTRYGD, INNTEKTSMELDING, SAKSBEHANDLER
}

internal fun List<Skatt.RapportertInntekt>.rapportertInntekt() = this
    .map(Skatt::rapportertInntekt)
    .summer()
    .div(12)


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
        inntektsmelding: Inntektsmelding,
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
                null,
                Kilde.INNTEKTSMELDING
            )
        )
    }

    override fun visitInfotrygd(
        infotrygd: Infotrygd,
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
                null,
                Kilde.INFOTRYGD
            )
        )
    }

    private lateinit var skattedato: LocalDate

    override fun visitSkattSykepengegrunnlag(
        sykepengegrunnlag: Skatt.Sykepengegrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        skattedato = dato
    }

    override fun visitSkattRapportertInntekt(
        rapportertInntekt: Skatt.RapportertInntekt,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        skattedato = dato
    }

    override fun postVisitSkatt(skattComposite: SkattComposite, id: UUID, dato: LocalDate) {
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
        saksbehandler: Saksbehandler,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        forklaring: String?,
        subsumsjon: Subsumsjon?,
        tidsstempel: LocalDateTime
    ) {
        inntektsopplysninger.add(
            Opplysning(
                dato,
                saksbehandler.omregnetÅrsinntekt(dato, dato)?.omregnetÅrsinntekt(),
                null,
                Kilde.SAKSBEHANDLER
            )
        )
    }
}
