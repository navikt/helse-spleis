package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Varselkode.RV_UT_3
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
        check(!kilde.erAvType(Sykmelding::class)) { "Kan ikke opprette arbeidsgiverperiodedag for $dato med kilde Sykmelding" }
        periodebuilder.arbeidsgiverperiodedag(dato, økonomi, kilde)
        builder.addArbeidsgiverperiodedag(dato, inntekter.medInntekt(dato, nåværendeArbeidsgiverperiode))
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        check(!kilde.erAvType(Sykmelding::class)) { "Kan ikke opprette utbetalingsdag for $dato med kilde Sykmelding" }
        if (dato.erHelg()) return builder.addHelg(dato, inntekter.utenInntekt(dato, økonomi, nåværendeArbeidsgiverperiode))
        builder.addNAVdag(dato, inntekter.medInntekt(dato, nåværendeArbeidsgiverperiode, økonomi))
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
        sisteArbeidsgiverperiode = periodebuilder.result().lastOrNull()
    }
}
