package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi

internal sealed class UtbetalingstidslinjeBuilderException(private val kort: String, message: String) : RuntimeException(message) {
    internal fun logg(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Feilmelding: $message")
        aktivitetslogg.error("Feil ved utbetalingstidslinjebygging: $kort")
    }

    internal class ManglerInntektException(dagen: LocalDate, skjæringstidspunkter: List<LocalDate>) : UtbetalingstidslinjeBuilderException(
        "Mangler inntekt for dag",
        "Fant ikke inntekt for $dagen med skjæringstidspunkter $skjæringstidspunkter"
    )

    internal class UforventetDagException(dag: Dag, melding: String) : UtbetalingstidslinjeBuilderException(
        "Forventet ikke ${dag::class.simpleName}",
        "Forventet ikke ${dag::class.simpleName} i utbetalingstidslinjen. Melding: $melding"
    )

    internal class NegativDekningsgrunnlagException(dekningsgrunnlag: Inntekt, dagen: LocalDate, skjæringstidspunkt: LocalDate) : UtbetalingstidslinjeBuilderException(
        "Dekningsgrunnlag er negativ",
        "Dekningsgrunnlag for $dagen med skjæringstidspunkt $skjæringstidspunkt gir negativt beløp: $dekningsgrunnlag"
    )
}

internal interface IUtbetalingstidslinjeBuilder : ArbeidsgiverperiodeMediator {
    fun result(): Utbetalingstidslinje
}

internal class UtbetalingstidslinjeBuilder(private val inntekter: Inntekter) : IUtbetalingstidslinjeBuilder {
    private val periodebuilder = ArbeidsgiverperiodeBuilderBuilder()
    private var sisteArbeidsgiverperiode: Arbeidsgiverperiode? = null
    private val nåværendeArbeidsgiverperiode: Arbeidsgiverperiode? get() = sisteArbeidsgiverperiode ?: periodebuilder.build()

    private val builder = Utbetalingstidslinje.Builder()

    override fun result(): Utbetalingstidslinje {
        return builder.build()
    }

    override fun fridag(dato: LocalDate) {
        builder.addFridag(dato, inntekter.medFrivilligInntekt(dato, Økonomi.ikkeBetalt(nåværendeArbeidsgiverperiode)))
    }

    override fun arbeidsdag(dato: LocalDate) {
        builder.addArbeidsdag(dato, inntekter.medFrivilligInntekt(dato, Økonomi.ikkeBetalt(nåværendeArbeidsgiverperiode)))
    }

    override fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        periodebuilder.arbeidsgiverperiodedag(dato, økonomi)
        builder.addArbeidsgiverperiodedag(dato, inntekter.medFrivilligInntekt(dato, Økonomi.ikkeBetalt(nåværendeArbeidsgiverperiode)))
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi) {
        if (dato.erHelg()) return builder.addHelg(dato, inntekter.medSkjæringstidspunkt(dato, økonomi, nåværendeArbeidsgiverperiode))
        builder.addNAVdag(dato, inntekter.medInntekt(dato, økonomi, nåværendeArbeidsgiverperiode))
    }

    override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
        builder.addForeldetDag(dato, inntekter.medInntekt(dato, økonomi, nåværendeArbeidsgiverperiode))
    }

    override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse) {
        builder.addAvvistDag(dato, inntekter.medFrivilligInntekt(dato, Økonomi.ikkeBetalt(nåværendeArbeidsgiverperiode)), listOf(begrunnelse))
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        periodebuilder.arbeidsgiverperiodeAvbrutt()
        sisteArbeidsgiverperiode = null
    }

    override fun arbeidsgiverperiodeFerdig() {
        periodebuilder.arbeidsgiverperiodeFerdig()
        sisteArbeidsgiverperiode = periodebuilder.result().lastOrNull()
    }
}
