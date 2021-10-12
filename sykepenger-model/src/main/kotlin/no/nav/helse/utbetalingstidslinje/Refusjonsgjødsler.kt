package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class Refusjonsgjødsler(
    private val tidslinje: Utbetalingstidslinje,
    private val refusjonshistorikk: Refusjonshistorikk
) {
    internal fun gjødsle(aktivitetslogg: IAktivitetslogg) {
        val refusjon = refusjonshistorikk.finnRefusjon(tidslinje.periode())
        if (refusjon == null) aktivitetslogg.warn("Fant ikke refusjon for perioden. Defaulter til 100%% refusjon. placeholder") // TODO: Spør voksne om tekst

        tidslinje.forEach { utbetalingsdag ->
            when (refusjon) {
                null -> utbetalingsdag.økonomi.settFullArbeidsgiverRefusjon()
                else -> utbetalingsdag.økonomi.arbeidsgiverRefusjon(refusjon.beløp(utbetalingsdag.dato, aktivitetslogg))
            }
        }
    }

    internal class SammenhengendeUtbetalingerVisitor() : UtbetalingsdagVisitor {

        private val sammenhengendeUtbetalinger = mutableListOf<Periode>()
        private var iSammenhengende = false

        private fun detErEnDagViBryrOssOm(dato: LocalDate) {
            if (iSammenhengende) {
                sammenhengendeUtbetalinger.add(sammenhengendeUtbetalinger.removeLast().oppdaterTom(dato))
            } else {
                sammenhengendeUtbetalinger.add(dato til dato)
                iSammenhengende = true
            }
        }

        private fun detErEnDagViIkkeBryrOssOm() {
            iSammenhengende = false
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag, dato: LocalDate, økonomi: Økonomi) {
            detErEnDagViBryrOssOm(dato)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag, dato: LocalDate, økonomi: Økonomi) {
            detErEnDagViBryrOssOm(dato)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag, dato: LocalDate, økonomi: Økonomi) {
            detErEnDagViBryrOssOm(dato)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag, dato: LocalDate, økonomi: Økonomi) {
            detErEnDagViBryrOssOm(dato)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag, dato: LocalDate, økonomi: Økonomi) {
            detErEnDagViBryrOssOm(dato)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag, dato: LocalDate, økonomi: Økonomi) {
            detErEnDagViBryrOssOm(dato)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag, dato: LocalDate, økonomi: Økonomi) {
            detErEnDagViIkkeBryrOssOm()
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag, dato: LocalDate, økonomi: Økonomi) {
            detErEnDagViIkkeBryrOssOm()
        }

    }
}
