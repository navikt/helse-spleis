package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.util.*
import no.nav.helse.erHelg
import no.nav.helse.nesteDag

class OppdragBuilder(
    private val mottaker: String,
    private val fagområde: Fagområde,
    private val klassekode: Klassekode,
    private val fagsystemId: String = genererUtbetalingsreferanse(UUID.randomUUID())
) {
    private val utbetalingslinjer = mutableListOf<Utbetalingslinje>()
    private var tilstand: Tilstand = MellomLinjer
    private val sisteLinje get() = utbetalingslinjer.last()

    fun build() = Oppdrag(mottaker, fagområde, utbetalingslinjer, fagsystemId)

    fun ikkeBetalingsdag() {
        tilstand = MellomLinjer
    }

    fun arbeidsgiverperiodedag(dato: LocalDate, grad: Int) {
        when (tilstand) {
            LinjeMedSats,
            LinjeUtenSats -> when (dato.erHelg()) {
                true -> betalingshelgedag(dato, grad)
                false -> ikkeBetalingsdag()
            }
            MellomLinjer -> {
                // trenger ikke gjøre noe
            }
        }
    }

    fun betalingsdag(dato: LocalDate, beløp: Int, grad: Int) {
        val nyLinje = nyLinje(beløp, dato, grad)

        // må lage ny linje hvis det ikke er noen linjer fra før,
        // eller at siste linje ikke kan utvides
        when (tilstand) {
            is LinjeMedSats -> when {
                kanLinjeUtvides(nyLinje) -> utvideLinje(dato)
                else -> addLinje(nyLinje)
            }
            is LinjeUtenSats -> {
                when {
                    sisteLinje.grad == nyLinje.grad -> utvideLinje(dato, beløp)
                    else -> addLinje(nyLinje)
                }
                tilstand = LinjeMedSats
            }
            is MellomLinjer -> {
                addLinje(nyLinje)
                tilstand = LinjeMedSats
            }
        }
    }

    fun betalingshelgedag(dato: LocalDate, grad: Int) {
        val nyLinje = nyLinje(0, dato, grad)
        when (tilstand) {
            LinjeMedSats -> when {
                sisteLinje.grad == grad -> utvideLinje(dato)
                else -> addLinje(nyLinje)
            }
            LinjeUtenSats -> when {
                sisteLinje.grad == grad -> utvideLinje(dato)
                else -> addLinje(nyLinje)
            }
            MellomLinjer -> {
                addLinje(nyLinje)
                tilstand = LinjeUtenSats
            }
        }
    }

    private fun utvideLinje(dato: LocalDate, beløp: Int = sisteLinje.beløp) {
        check(sisteLinje.tom.nesteDag == dato) {
            "builderen kalles ikke riktig"
        }
        val førsteLinje = utbetalingslinjer.removeLast()
        utbetalingslinjer.add(førsteLinje.kopier(
            beløp = beløp,
            tom = dato
        ))
    }

    private fun kanLinjeUtvides(nyLinje: Utbetalingslinje): Boolean {
        if (sisteLinje.grad != nyLinje.grad) return false
        return nyLinje.beløp == sisteLinje.beløp
    }

    private fun nyLinje(beløp: Int, dato: LocalDate, grad: Int): Utbetalingslinje {
        return Utbetalingslinje(
            fom = dato,
            tom = dato,
            beløp = beløp,
            grad = grad,
            refFagsystemId = fagsystemId,
            klassekode = klassekode
        )
    }

    private fun addLinje(nyLinje: Utbetalingslinje) {
        utbetalingslinjer.add(nyLinje)
    }

    private sealed interface Tilstand
    private data object  MellomLinjer : Tilstand
    private data object LinjeMedSats : Tilstand
    private data object LinjeUtenSats : Tilstand
}
