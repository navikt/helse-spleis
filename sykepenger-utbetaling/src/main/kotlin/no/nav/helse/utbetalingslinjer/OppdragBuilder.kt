package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.økonomi.Økonomi

class OppdragBuilder(
    private val fagsystemId: String = genererUtbetalingsreferanse(UUID.randomUUID()),
    private val mottaker: String,
    private val fagområde: Fagområde
) {
    private val økonomibeløp: (Økonomi) -> Int = { økonomi ->
        when (fagområde) {
            Fagområde.SykepengerRefusjon -> økonomi.arbeidsgiverbeløp
            Fagområde.Sykepenger -> økonomi.personbeløp
        }?.daglig?.toInt()!!
    }
    private val utbetalingslinjer = mutableListOf<Utbetalingslinje>()
    private var tilstand: Tilstand = MellomLinjer()
    private val linje get() = utbetalingslinjer.last()

    fun build() = Oppdrag(mottaker, fagområde, utbetalingslinjer, fagsystemId)

    fun betalingsdag(økonomi: Økonomi, dato: LocalDate, grad: Int) {
        if (utbetalingslinjer.isEmpty() || !kanLinjeUtvides(linje, økonomi, grad))
            tilstand.nyLinje(økonomi, dato, grad)
        else
            tilstand.betalingsdag(økonomi, dato, grad)
    }

    private fun kanLinjeUtvides(linje: Utbetalingslinje, økonomi: Økonomi, grad: Int): Boolean {
        return grad == linje.grad && (linje.beløp == null || linje.beløp == økonomibeløp(økonomi))
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

    private fun addLinje(økonomi: Økonomi, dato: LocalDate, grad: Int) {
        utbetalingslinjer.add(Utbetalingslinje(
            fom = dato,
            tom = dato,
            satstype = Satstype.Daglig,
            beløp = økonomibeløp(økonomi),
            grad = grad,
            refFagsystemId = fagsystemId,
            klassekode = fagområde.klassekode
        ))
    }

    private fun addLinje(dato: LocalDate, grad: Int) {
        utbetalingslinjer.add(Utbetalingslinje(
            fom = dato,
            tom = dato,
            satstype = Satstype.Daglig,
            beløp = null,
            grad = grad,
            refFagsystemId = fagsystemId,
            klassekode = fagområde.klassekode
        ))
    }

    private interface Tilstand {

        fun betalingsdag(
            økonomi: Økonomi,
            dato: LocalDate,
            grad: Int
        ) {
        }

        fun helgedag(dato: LocalDate, grad: Int) {}

        fun nyLinje(
            økonomi: Økonomi,
            dato: LocalDate,
            grad: Int
        ) {
        }

        fun nyLinje(dato: LocalDate, grad: Int) {}

        fun ikkeBetalingsdag() {}
    }

    private inner class MellomLinjer : Tilstand {
        override fun betalingsdag(
            økonomi: Økonomi,
            dato: LocalDate,
            grad: Int
        ) {
            addLinje(økonomi, dato, grad)
            tilstand = LinjeMedSats()
        }


        override fun nyLinje(
            økonomi: Økonomi,
            dato: LocalDate,
            grad: Int
        ) {
            addLinje(økonomi, dato, grad)
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
            økonomi: Økonomi,
            dato: LocalDate,
            grad: Int
        ) {
            val førsteLinje = utbetalingslinjer.removeLast()
            utbetalingslinjer.add(førsteLinje.kopier(tom = dato))
        }

        override fun nyLinje(
            økonomi: Økonomi,
            dato: LocalDate,
            grad: Int
        ) {
            addLinje(økonomi, dato, grad)
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
            økonomi: Økonomi,
            dato: LocalDate,
            grad: Int
        ) {
            utbetalingslinjer.add(utbetalingslinjer.removeLast().kopier(tom = dato, beløp = økonomibeløp(økonomi)))
            tilstand = LinjeMedSats()
        }

        override fun nyLinje(
            økonomi: Økonomi,
            dato: LocalDate,
            grad: Int
        ) {
            addLinje(økonomi, dato, grad)
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
