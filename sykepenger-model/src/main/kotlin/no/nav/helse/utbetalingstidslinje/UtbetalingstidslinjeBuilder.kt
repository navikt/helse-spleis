package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RE_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_3
import no.nav.helse.person.inntekt.ManglerRefusjonsopplysning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Økonomi

internal sealed class UtbetalingstidslinjeBuilderException(message: String) : RuntimeException(message) {
    internal fun logg(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Feilmelding: $message")
        aktivitetslogg.funksjonellFeil(RV_UT_3)
    }

    internal class UforventetDagException(dag: Dag, melding: String) : UtbetalingstidslinjeBuilderException(
        "Forventet ikke ${dag::class.simpleName} i utbetalingstidslinjen. Melding: $melding"
    )

}

internal class UtbetalingstidslinjeBuilder(private val inntekter: Inntekter, private val beregningsperiode: Periode, private val hendelse: IAktivitetslogg) : ArbeidsgiverperiodeMediator {
    private val periodebuilder = ArbeidsgiverperiodeBuilderBuilder()
    private var sisteArbeidsgiverperiode: Arbeidsgiverperiode? = null
    private val nåværendeArbeidsgiverperiode: Arbeidsgiverperiode? get() = sisteArbeidsgiverperiode ?: periodebuilder.build()

    private val builder = Utbetalingstidslinje.Builder()

    private val kildeSykmelding = mutableSetOf<LocalDate>()
    private val manglerRefusjonsopplysninger = mutableSetOf<LocalDate>()
    private val manglerRefusjonsopplysning: ManglerRefusjonsopplysning = { dag, _ ->
       manglerRefusjonsopplysninger.add(dag)
    }

    internal fun result(): Utbetalingstidslinje {
        if (manglerRefusjonsopplysninger.isNotEmpty()) {
            hendelse.varsel(RV_RE_1)
            hendelse.info("Manglet refusjonsopplysninger ved beregning av utbetalingstidslinje. Manglet for periodene ${manglerRefusjonsopplysninger.grupperSammenhengendePerioder()}")
        }
        check(kildeSykmelding.isEmpty()) {
            inntekter.ugyldigUtbetalingstidslinje(kildeSykmelding)
            "Kan ikke opprette utbetalingsdager med kilde Sykmelding: ${kildeSykmelding.grupperSammenhengendePerioder()}"
        }
        return builder.build()
    }

    override fun fridag(dato: LocalDate) {
        builder.addFridag(dato, inntekter.medInntekt(dato, nåværendeArbeidsgiverperiode))
    }

    override fun fridagOppholdsdag(dato: LocalDate) {
        builder.addFridag(dato, inntekter.medInntekt(dato, nåværendeArbeidsgiverperiode))
    }

    override fun arbeidsdag(dato: LocalDate) {
        builder.addArbeidsdag(dato, inntekter.medInntekt(dato, nåværendeArbeidsgiverperiode))
    }

    override fun arbeidsgiverperiodedag(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        periodebuilder.arbeidsgiverperiodedag(dato, økonomi, kilde)
        builder.addArbeidsgiverperiodedag(dato, inntekter.medInntekt(dato, nåværendeArbeidsgiverperiode, økonomi.ikkeBetalt()))
    }

    override fun arbeidsgiverperiodedagNavAnsvar(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        periodebuilder.arbeidsgiverperiodedag(dato, økonomi, kilde)
        periodebuilder.utbetalingsdag(dato, økonomi, kilde)
        builder.addArbeidsgiverperiodedagNavAnsvar(dato, økonomi)
    }

    override fun ukjentDag(dato: LocalDate) {
        builder.addUkjentDag(dato)
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        if (dato.erHelg()) return builder.addHelg(dato, inntekter.utenInntekt(dato, økonomi, nåværendeArbeidsgiverperiode))
        val medUtbetalingsopplysninger = when (dato in beregningsperiode) {
            true -> inntekter.medUtbetalingsopplysninger(dato, nåværendeArbeidsgiverperiode, økonomi, manglerRefusjonsopplysning)
            false -> inntekter.medInntekt(dato, nåværendeArbeidsgiverperiode, økonomi)
        }
        builder.addNAVdag(dato, medUtbetalingsopplysninger)
    }

    override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
        builder.addForeldetDag(dato, inntekter.medInntekt(dato, nåværendeArbeidsgiverperiode, økonomi))
    }

    override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse) {
        builder.addAvvistDag(dato, inntekter.medInntekt(dato, nåværendeArbeidsgiverperiode), listOf(begrunnelse))
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        periodebuilder.arbeidsgiverperiodeAvbrutt()
        sisteArbeidsgiverperiode = null
    }

    override fun arbeidsgiverperiodeFerdig() {
        periodebuilder.arbeidsgiverperiodeFerdig()
        sisteArbeidsgiverperiode = periodebuilder.build()
    }
}
