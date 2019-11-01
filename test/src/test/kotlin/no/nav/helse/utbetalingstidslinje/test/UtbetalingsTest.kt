package no.nav.helse.utbetalingstidslinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingsTest {

    companion object {
        private val sendtSykmelding = Testhendelse(Uke(3).mandag.atStartOfDay())
    }

    @Test
    fun `en sykdomstidslinje innenfor arbeidsgiverperioden gir ingen betalingslinjer`() {
        val sykdomstidslinje = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, sendtSykmelding)
        val betalingslinjer = sykdomstidslinje.betalingslinjer(1200.toBigDecimal())

        assertEquals(0, betalingslinjer.size)
    }

}
