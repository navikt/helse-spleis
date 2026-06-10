package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi

internal class SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(
    private val dekningsgrad: Prosentdel,
    private val ventetid: Periode?,
    private val dagerNavOvertarAnsvar: List<Periode>,
    private val avslagstidslinje: Avslagstidslinje
) : UtbetalingstidslinjeBuilder {
    private fun medInntektHvisFinnes(dato: LocalDate, grad: Prosentdel, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje): Økonomi {
        return medInntekt(dato, grad, næringsinntekt, inntektjusteringer)
    }

    private fun medInntekt(dato: LocalDate, grad: Prosentdel, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje): Økonomi {
        return Økonomi.inntekt(
            sykdomsgrad = grad,
            aktuellDagsinntekt = næringsinntekt,
            dekningsgrad = dekningsgrad,
            refusjonsbeløp = INGEN,
            inntektjustering = (inntektjusteringer[dato] as? Beløpsdag)?.beløp ?: INGEN
        )
    }

    override fun result(sykdomstidslinje: Sykdomstidslinje, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje): Utbetalingstidslinje {
        val builder = Utbetalingstidslinje.Builder()
        sykdomstidslinje.forEach { dag ->
            when (val avslagsdag = avslagstidslinje[dag.dato]) {
                null -> builder.leggTil(dag, inntekt, inntektjusteringer)
                else -> avvistDag(builder, dag.dato, dag.sykdomsgrad(), avslagsdag.begrunnelser, inntekt, inntektjusteringer)
            }
        }
        return builder.build()
    }

    private fun Utbetalingstidslinje.Builder.leggTil(dag: Dag, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        val erVentetid = ventetid?.contains(dag.dato) == true
        when (dag) {
            is Dag.Arbeidsdag -> arbeidsdag(this, dag.dato, inntekt, inntektjusteringer)
            is Dag.ForeldetSykedag -> foreldetdag(this, dag.dato, dag.grad, inntekt, inntektjusteringer)
            is Dag.FriskHelgedag -> arbeidsdag(this, dag.dato, inntekt, inntektjusteringer)
            is Dag.SykHelgedag -> when (erVentetid) {
                true -> ventetidsdag(this, dag.dato, dag.grad, inntekt, inntektjusteringer)
                false -> helg(this, dag.dato, dag.grad, inntekt, inntektjusteringer)
            }
            is Dag.Sykedag -> when (erVentetid) {
                true -> when (navSkalUtbetaleVentetidsDag(dag.dato)) {
                    true -> forsikringsdag(this, dag.dato, dag.grad, inntekt, inntektjusteringer)
                    false -> ventetidsdag(this, dag.dato, dag.grad, inntekt, inntektjusteringer)
                }
                false -> navDag(this, dag.dato, dag.grad, inntekt, inntektjusteringer)
            }
            is Dag.MeldingTilNavDag -> when (erVentetid) {
                true -> ventetidsdag(this, dag.dato, dag.grad, inntekt, inntektjusteringer)
                else -> avvistDag(this, dag.dato, dag.grad, Begrunnelse.MeldingTilNavDagUtenforVentetid, inntekt, inntektjusteringer)
            }
            is Dag.MeldingTilNavHelgedag -> when (erVentetid) {
                true -> ventetidsdag(this, dag.dato, dag.grad, inntekt, inntektjusteringer)
                false -> avvistDag(this, dag.dato, dag.grad, Begrunnelse.MeldingTilNavDagUtenforVentetid, inntekt, inntektjusteringer)
            }
            is Dag.AndreYtelser -> {
                val begrunnelse = when (dag.ytelse) {
                    Dag.AndreYtelser.AnnenYtelse.AAP -> Begrunnelse.AndreYtelserAap
                    Dag.AndreYtelser.AnnenYtelse.Dagpenger -> Begrunnelse.AndreYtelserDagpenger
                    Dag.AndreYtelser.AnnenYtelse.Foreldrepenger -> Begrunnelse.AndreYtelserForeldrepenger
                    Dag.AndreYtelser.AnnenYtelse.Omsorgspenger -> Begrunnelse.AndreYtelserOmsorgspenger
                    Dag.AndreYtelser.AnnenYtelse.Opplæringspenger -> Begrunnelse.AndreYtelserOpplaringspenger
                    Dag.AndreYtelser.AnnenYtelse.Pleiepenger -> Begrunnelse.AndreYtelserPleiepenger
                    Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger -> Begrunnelse.AndreYtelserSvangerskapspenger
                }
                avvistDag(this, dag.dato, 0.prosent, begrunnelse, inntekt, inntektjusteringer)
            }
            is Dag.ArbeidIkkeGjenopptattDag,
            is Dag.Arbeidsgiverdag,
            is Dag.ArbeidsgiverHelgedag,
            is Dag.Permisjonsdag,
            is Dag.ProblemDag,
            is Dag.Feriedag,
            is Dag.UkjentDag -> error("Forventer ikke ${dag::class.simpleName} i utbetalingstidslinjen for selvstendig næringsdrivende")
        }
    }

    private fun navSkalUtbetaleVentetidsDag(dato: LocalDate): Boolean {
        return dagerNavOvertarAnsvar.any { it.contains(dato) }
    }

    private fun helg(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addHelg(dato, medInntektHvisFinnes(dato, grad, næringsinntekt, inntektjusteringer).ikkeBetalt())
    }

    private fun navDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addNAVdag(dato, medInntektHvisFinnes(dato, grad, næringsinntekt, inntektjusteringer))
    }

    private fun arbeidsdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addArbeidsdag(dato, medInntektHvisFinnes(dato, 0.prosent, næringsinntekt, inntektjusteringer).ikkeBetalt())
    }

    private fun ventetidsdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, sykdomsgrad: Prosentdel, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addVentetidsdag(dato, medInntektHvisFinnes(dato, sykdomsgrad, næringsinntekt, inntektjusteringer).ikkeBetalt())
    }

    private fun forsikringsdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, sykdomsgrad: Prosentdel, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addVentetidsdag(dato, medInntektHvisFinnes(dato, sykdomsgrad, næringsinntekt, inntektjusteringer))
    }

    private fun foreldetdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, sykdomsgrad: Prosentdel, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addForeldetDag(dato, medInntektHvisFinnes(dato, sykdomsgrad, næringsinntekt, inntektjusteringer).ikkeBetalt())
    }

    private fun avvistDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, begrunnelse: Begrunnelse, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addAvvistDag(dato, medInntektHvisFinnes(dato, grad, næringsinntekt, inntektjusteringer).ikkeBetalt(), listOf(begrunnelse))
    }

    private fun avvistDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, begrunnelser: List<Begrunnelse>, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addAvvistDag(dato, medInntektHvisFinnes(dato, grad, næringsinntekt, inntektjusteringer).ikkeBetalt(), begrunnelser)
    }
}
