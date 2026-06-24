package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.ForsikringsvurderingResultat
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.DagerUtenNavAnsvaravklaring
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Prosentdel.Companion.riktigProsent
import no.nav.helse.økonomi.Økonomi

internal class SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(
    private val forsikringsvurderingResultat: ForsikringsvurderingResultat?,
    private val dagerUtenNavAnsvar: DagerUtenNavAnsvaravklaring,
    private val avslagstidslinje: Avslagstidslinje
) : UtbetalingstidslinjeBuilder {
    private fun medInntektHvisFinnes(dato: LocalDate, grad: Prosentdel, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje): Økonomi {
        return medInntekt(dato, grad, næringsinntekt, inntektjusteringer)
    }

    private fun medInntekt(dato: LocalDate, grad: Prosentdel, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje): Økonomi {
        return Økonomi.inntekt(
            sykdomsgrad = grad,
            aktuellDagsinntekt = næringsinntekt,
            dekningsgrad = (
                forsikringsvurderingResultat
                    ?.takeUnless { it.erOpphørtPå(dato) }
                    ?.dekning?.grad
                    ?: 80
                ).riktigProsent,
            refusjonsbeløp = INGEN,
            inntektjustering = (inntektjusteringer[dato] as? Beløpsdag)?.beløp ?: INGEN
        )
    }

    private fun ForsikringsvurderingResultat.erOpphørtPå(dato: LocalDate): Boolean =
        opphørsdato != null && dato > opphørsdato

    private fun Utbetalingstidslinje.Builder.avslagsdagEller(dato: LocalDate, sykdomsgrad: Prosentdel, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje, eller: () -> Unit) = when (val avslagsdag = avslagstidslinje[dato]) {
        null -> eller()
        else -> avvistDag(this, dato, sykdomsgrad, avslagsdag.begrunnelser, inntekt, inntektjusteringer)
    }

    private fun Utbetalingstidslinje.Builder.avslagsdag(dato: LocalDate, sykdomsgrad: Prosentdel, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje, begrunnelser: List<Begrunnelse>) {
        check(begrunnelser.isNotEmpty()) { "Forventer at det finnes begrunnelser for avslagsdag" }
        val avslagsbegrunnelser = (avslagstidslinje[dato]?.begrunnelser ?: emptyList()) + begrunnelser
        avvistDag(this, dato, sykdomsgrad, avslagsbegrunnelser, inntekt, inntektjusteringer)
    }

    override fun result(sykdomstidslinje: Sykdomstidslinje, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje): Utbetalingstidslinje {
        val builder = Utbetalingstidslinje.Builder()
        sykdomstidslinje.forEach { dag ->
            val erVentetid = dagerUtenNavAnsvar.periode?.contains(dag.dato) == true
            when (dag) {
                is Dag.Arbeidsdag -> builder.avslagsdagEller(dag.dato, 0.prosent, inntekt, inntektjusteringer) {
                    arbeidsdag(builder, dag.dato, inntekt, inntektjusteringer)
                }
                is Dag.ForeldetSykedag -> builder.avslagsdagEller(dag.dato, dag.grad, inntekt, inntektjusteringer) {
                    foreldetdag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                }
                is Dag.FriskHelgedag -> builder.avslagsdagEller(dag.dato, 0.prosent, inntekt, inntektjusteringer) {
                    arbeidsdag(builder, dag.dato, inntekt, inntektjusteringer)
                }
                is Dag.SykHelgedag -> builder.avslagsdagEller(dag.dato, dag.grad, inntekt, inntektjusteringer) {
                    when (erVentetid) {
                        true -> ventetidsdag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                        false -> helg(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                    }
                }
                is Dag.Sykedag -> builder.avslagsdagEller(dag.dato, dag.grad, inntekt, inntektjusteringer) {
                    when (erVentetid) {
                        true -> when (navSkalUtbetaleVentetidsDag(dag.dato)) {
                            true -> forsikringsdag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                            false -> ventetidsdag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                        }
                        false -> navDag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                    }
                }
                is Dag.MeldingTilNavDag -> when (erVentetid) {
                    true -> builder.avslagsdagEller(dag.dato, dag.grad, inntekt, inntektjusteringer) {
                        ventetidsdag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                    }
                    false -> builder.avslagsdag(dag.dato, dag.grad, inntekt, inntektjusteringer, listOf(Begrunnelse.MeldingTilNavDagUtenforVentetid))
                }
                is Dag.MeldingTilNavHelgedag -> when (erVentetid) {
                    true -> builder.avslagsdagEller(dag.dato, dag.grad, inntekt, inntektjusteringer) {
                        ventetidsdag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                    }
                    false -> builder.avslagsdag(dag.dato, dag.grad, inntekt, inntektjusteringer, listOf(Begrunnelse.MeldingTilNavDagUtenforVentetid))
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
                    builder.avslagsdag(dag.dato, 0.prosent, inntekt, inntektjusteringer, listOf(begrunnelse))
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
        return builder.build()
    }

    private fun navSkalUtbetaleVentetidsDag(dato: LocalDate): Boolean {
        if (!dagerUtenNavAnsvar.dager.any { dato in it }) return false

        return forsikringsvurderingResultat != null
            && !forsikringsvurderingResultat.erOpphørtPå(dato)
            && forsikringsvurderingResultat.dekning?.iVentetid == true
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

    private fun avvistDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, begrunnelser: List<Begrunnelse>, næringsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addAvvistDag(dato, medInntektHvisFinnes(dato, grad, næringsinntekt, inntektjusteringer).ikkeBetalt(), begrunnelser)
    }
}
