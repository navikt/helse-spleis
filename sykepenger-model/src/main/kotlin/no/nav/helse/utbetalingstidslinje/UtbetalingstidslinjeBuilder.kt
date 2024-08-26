package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.Vedtaksperioder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_3
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.økonomi.Økonomi

internal sealed class UtbetalingstidslinjeBuilderException(message: String) : RuntimeException(message) {
    internal fun logg(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Feilmelding: $message")
        aktivitetslogg.funksjonellFeil(RV_UT_3)
    }

    internal class ProblemdagException(melding: String) : UtbetalingstidslinjeBuilderException(
        "Forventet ikke ProblemDag i utbetalingstidslinjen. Melding: $melding"
    )

}

internal class UtbetalingstidslinjeBuilder(private val vedtaksperioder: Vedtaksperioder, private val beregningsperiode: Periode) : ArbeidsgiverperiodeMediator {
    private val builder = Utbetalingstidslinje.Builder()
    private val kildeSykmelding = mutableSetOf<LocalDate>()

    internal fun result(): Utbetalingstidslinje {
        check(kildeSykmelding.isEmpty()) { "Kan ikke opprette utbetalingsdager med kilde Sykmelding: ${kildeSykmelding.grupperSammenhengendePerioder()}" }
        return builder.build()
    }

    override fun fridag(dato: LocalDate) {
        builder.addFridag(dato, vedtaksperioder.medInntekt(dato, Økonomi.ikkeBetalt()))
    }

    override fun fridagOppholdsdag(dato: LocalDate) {
        builder.addFridag(dato, vedtaksperioder.medInntekt(dato, Økonomi.ikkeBetalt()))
    }

    override fun arbeidsdag(dato: LocalDate) {
        builder.addArbeidsdag(dato, vedtaksperioder.medInntekt(dato, Økonomi.ikkeBetalt()))
    }

    override fun arbeidsgiverperiodedag(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
        builder.addArbeidsgiverperiodedag(dato, vedtaksperioder.medInntekt(dato, økonomi.ikkeBetalt()))
    }

    override fun arbeidsgiverperiodedagNav(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
        val medUtbetalingsopplysninger = when (dato in beregningsperiode) {
            true -> vedtaksperioder.medUtbetalingsopplysninger(dato, økonomi)
            false -> vedtaksperioder.medInntekt(dato, økonomi)
        }
        builder.addArbeidsgiverperiodedagNav(dato, medUtbetalingsopplysninger)
    }

    override fun ukjentDag(dato: LocalDate) {
        builder.addUkjentDag(dato)
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (dato.erHelg()) return builder.addHelg(dato, vedtaksperioder.medInntekt(dato, økonomi.ikkeBetalt()))
        val medUtbetalingsopplysninger = when (dato in beregningsperiode) {
            true -> vedtaksperioder.medUtbetalingsopplysninger(dato, økonomi)
            false -> vedtaksperioder.medInntekt(dato, økonomi)
        }
        builder.addNAVdag(dato, medUtbetalingsopplysninger)
    }

    override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
        builder.addForeldetDag(dato, vedtaksperioder.medInntekt(dato, økonomi))
    }

    override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse, økonomi: Økonomi) {
        builder.addAvvistDag(dato, vedtaksperioder.medInntekt(dato, økonomi.ikkeBetalt()), listOf(begrunnelse))
    }
}
