package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.økonomi.Økonomi

internal class UtbetalingkladdBuilder(førsteDag: LocalDate, arbeidsgivermottaker: String, personmottaker: String) {
    private var periode: Periode = førsteDag.somPeriode()
    // bruker samme "sak id" i OS for begge oppdragene
    // TODO: krever at Overføringer/kvitteringer inneholder fagområde, ellers
    // kan ikke meldingene mappes til riktig oppdrag
    // private val fagsystemId = genererUtbetalingsreferanse(UUID.randomUUID())
    private var arbeidsgiveroppdragBuilder = OppdragBuilder(
        sisteArbeidsgiverdag = førsteDag,
        mottaker = arbeidsgivermottaker,
        fagområde = SykepengerRefusjon
    )
    private var personoppdragBuilder = OppdragBuilder(
        sisteArbeidsgiverdag = førsteDag,
        mottaker = personmottaker,
        fagområde = Sykepenger
    )

    internal fun build() = Utbetalingkladd(periode, arbeidsgiveroppdragBuilder.build(), personoppdragBuilder.build())

    internal fun betalingsdag(beløpkilde: Beløpkilde, dato: LocalDate, økonomi: Økonomi) {
        periode = periode.oppdaterTom(dato)
        økonomi.medAvrundetData { grad, aktuellDagsinntekt ->
            arbeidsgiveroppdragBuilder.betalingsdag(beløpkilde, dato, grad, aktuellDagsinntekt)
            personoppdragBuilder.betalingsdag(beløpkilde, dato, grad, aktuellDagsinntekt)
        }
    }

    internal fun betalingshelgedag(dato: LocalDate, økonomi: Økonomi) {
        periode = periode.oppdaterTom(dato)
        økonomi.medAvrundetData { grad, _ ->
            arbeidsgiveroppdragBuilder.betalingshelgedag(dato, grad)
            personoppdragBuilder.betalingshelgedag(dato, grad)
        }
    }

    internal fun ikkeBetalingsdag(dato: LocalDate) {
        periode = periode.oppdaterTom(dato)
        arbeidsgiveroppdragBuilder.ikkeBetalingsdag()
        personoppdragBuilder.ikkeBetalingsdag()
    }
}