package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.økonomi.Økonomi

class UtbetalingkladdBuilder(private var periode: Periode, arbeidsgivermottaker: String, personmottaker: String) {
    // bruker samme "sak id" i OS for begge oppdragene
    // TODO: krever at Overføringer/kvitteringer inneholder fagområde, ellers
    // kan ikke meldingene mappes til riktig oppdrag
    // private val fagsystemId = genererUtbetalingsreferanse(UUID.randomUUID())
    private var arbeidsgiveroppdragBuilder = OppdragBuilder(
        sisteArbeidsgiverdag = periode.endInclusive,
        mottaker = arbeidsgivermottaker,
        fagområde = SykepengerRefusjon
    )
    private var personoppdragBuilder = OppdragBuilder(
        sisteArbeidsgiverdag = periode.endInclusive,
        mottaker = personmottaker,
        fagområde = Sykepenger
    )

    fun build() = Utbetalingkladd(periode, arbeidsgiveroppdragBuilder.build(), personoppdragBuilder.build())

    fun betalingsdag(beløpkilde: Beløpkilde, dato: LocalDate, økonomi: Økonomi) {
        periode = periode.oppdaterTom(dato)
        økonomi.medAvrundetData { grad, aktuellDagsinntekt ->
            arbeidsgiveroppdragBuilder.betalingsdag(beløpkilde, dato, grad, aktuellDagsinntekt)
            personoppdragBuilder.betalingsdag(beløpkilde, dato, grad, aktuellDagsinntekt)
        }
    }

    fun betalingshelgedag(dato: LocalDate, økonomi: Økonomi) {
        periode = periode.oppdaterTom(dato)
        økonomi.brukAvrundetGrad { grad->
            arbeidsgiveroppdragBuilder.betalingshelgedag(dato, grad)
            personoppdragBuilder.betalingshelgedag(dato, grad)
        }
    }

    fun ikkeBetalingsdag(dato: LocalDate) {
        periode = periode.oppdaterTom(dato)
        arbeidsgiveroppdragBuilder.ikkeBetalingsdag()
        personoppdragBuilder.ikkeBetalingsdag()
    }
}