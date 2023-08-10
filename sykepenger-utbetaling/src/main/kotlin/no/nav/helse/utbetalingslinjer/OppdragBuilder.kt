package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.util.UUID

class OppdragBuilder(
    private val fagsystemId: String = genererUtbetalingsreferanse(UUID.randomUUID()),
    private val sisteArbeidsgiverdag: LocalDate,
    private val mottaker: String,
    private val fagområde: Fagområde
) {
    private val utbetalingslinjer = mutableListOf<Utbetalingslinje>()
    private var tilstand: Tilstand = MellomLinjer()
    private val linje get() = utbetalingslinjer.last()

    fun build() = Oppdrag(mottaker, fagområde, utbetalingslinjer, fagsystemId, sisteArbeidsgiverdag)

    fun betalingsdag(beløpkilde: Beløpkilde, dato: LocalDate, grad: Int) {
        if (utbetalingslinjer.isEmpty() || !fagområde.kanLinjeUtvides(linje, beløpkilde, grad))
            tilstand.nyLinje(beløpkilde, dato, grad)
        else
            tilstand.betalingsdag(beløpkilde, dato, grad)
    }

    fun betalingshelgedag(dato: LocalDate, grad: Int) {
       if (utbetalingslinjer.isEmpty() || grad != linje.grad)
            tilstand.nyLinje(dato, grad)
        else
            tilstand.helgedag(dato, grad)
    }

    fun ikkeBetalingsdag() {
        tilstand.ikkeBetalingsdag()
    }

    private fun addLinje(beløpkilde: Beløpkilde, dato: LocalDate, grad: Int) {
        utbetalingslinjer.add(fagområde.linje(fagsystemId, beløpkilde, dato, grad))
    }

    private fun addLinje(dato: LocalDate, grad: Int) {
        utbetalingslinjer.add(fagområde.linje(fagsystemId, dato, grad))
    }

    private interface Tilstand {

        fun betalingsdag(
            beløpkilde: Beløpkilde,
            dato: LocalDate,
            grad: Int
        ) {}

        fun helgedag(dato: LocalDate, grad: Int) {}

        fun nyLinje(
            beløpkilde: Beløpkilde,
            dato: LocalDate,
            grad: Int
        ) {}

        fun nyLinje(dato: LocalDate, grad: Int) {}

        fun ikkeBetalingsdag() {}
    }

    private inner class MellomLinjer : Tilstand {
        override fun betalingsdag(
            beløpkilde: Beløpkilde,
            dato: LocalDate,
            grad: Int
        ) {
            addLinje(beløpkilde, dato, grad)
            tilstand = LinjeMedSats()
        }


        override fun nyLinje(
            beløpkilde: Beløpkilde,
            dato: LocalDate,
            grad: Int
        ) {
            addLinje(beløpkilde, dato, grad)
            tilstand = LinjeMedSats()
        }


        override fun helgedag(dato: LocalDate, grad: Int) {
            nyLinje(dato, grad)
        }

        override fun nyLinje(dato: LocalDate, grad: Int) {
            addLinje(dato, grad)
            tilstand = LinjeUtenSats()
        }
    }

    private inner class LinjeMedSats : Tilstand {

        override fun ikkeBetalingsdag() {
            tilstand = MellomLinjer()
        }

        override fun betalingsdag(
            beløpkilde: Beløpkilde,
            dato: LocalDate,
            grad: Int
        ) {
            val førsteLinje = utbetalingslinjer.removeLast()
            utbetalingslinjer.add(førsteLinje.kopier(tom = dato))
        }

        override fun nyLinje(
            beløpkilde: Beløpkilde,
            dato: LocalDate,
            grad: Int
        ) {
            addLinje(beløpkilde, dato, grad)
        }

        override fun helgedag(dato: LocalDate, grad: Int) {
            val førsteLinje = utbetalingslinjer.removeLast()
            utbetalingslinjer.add(førsteLinje.kopier(tom = dato))
        }

        override fun nyLinje(dato: LocalDate, grad: Int) {
            addLinje(dato, grad)
            tilstand = LinjeUtenSats()
        }
    }

    private inner class LinjeUtenSats : Tilstand {

        override fun ikkeBetalingsdag() {
            tilstand = MellomLinjer()
        }

        override fun betalingsdag(
            beløpkilde: Beløpkilde,
            dato: LocalDate,
            grad: Int
        ) {
            utbetalingslinjer.add(fagområde.utvidLinje(utbetalingslinjer.removeLast(), dato, beløpkilde))
            tilstand = LinjeMedSats()
        }

        override fun nyLinje(
            beløpkilde: Beløpkilde,
            dato: LocalDate,
            grad: Int
        ) {
            addLinje(beløpkilde, dato, grad)
            tilstand = LinjeMedSats()
        }

        override fun helgedag(dato: LocalDate, grad: Int) {
            val førsteLinje = utbetalingslinjer.removeLast()
            utbetalingslinjer.add(førsteLinje.kopier(tom = dato))
        }

        override fun nyLinje(dato: LocalDate, grad: Int) {
            addLinje(dato, grad)
        }
    }
}