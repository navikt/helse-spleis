package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import java.time.LocalDate
import java.time.Year

internal class Feriepengeberegner(
    private val utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
    private val alder: Alder
) : InfotrygdhistorikkVisitor, Iterable<LocalDate> {
    private companion object {
        private const val MAGIC_NUMBER = 48
    }

    private val dager = mutableSetOf<LocalDate>()

    init {
        utbetalingshistorikkForFeriepenger.accept(this)
    }

    override fun visitInfotrygdhistorikkUtbetalingsperiode(orgnr: String, periode: Utbetalingsperiode, grad: Prosentdel, inntekt: Inntekt) {
        dager.addAll(periode.filterNot { it.erHelg() })
    }

    internal fun beregn() {
        val datoer = dager
            .sorted()
            .groupBy { Year.from(it) }
            .flatMap { (_, prÅr) -> prÅr.take(MAGIC_NUMBER) }
    }

    override fun iterator() = dager
        .sorted()
        .groupBy { Year.from(it) }
        .flatMap { (_, prÅr) -> prÅr.take(MAGIC_NUMBER) }
        .iterator()
}
