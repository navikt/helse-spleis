package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.*

internal class EnkelUtbetalingsTest {

    companion object {
        private val tidspunktRapportert = Testhendelse(rapportertdato = LocalDateTime.of(2019, JULY,1,0, 0))


    }

    @Test
    internal fun `arbeidsgiverperioden gir 0 kr`(){
        val sykdomstidslinje = Sykdomstidslinje.sykedager(
            LocalDate.of(2019, JULY,1),
            LocalDate.of(2019, JULY,16),
            tidspunktRapportert
        )
        val dagsats = 1000.0

        val utbetalingsTidslinje = sykdomstidslinje.utbetalingstidslinje(dagsats)

        assertEquals(0.0, utbetalingsTidslinje.totalSum())
    }

    @Test
    internal fun `sykedager etter arbeidsgiverperioden gir dagsats`(){
        val sykdomstidslinje = Sykdomstidslinje.sykedager(
            LocalDate.of(2019, JULY,1),
            LocalDate.of(2019, AUGUST,1),
            tidspunktRapportert
        )
        val dagsats = 1000.0

        val utbetalingsTidslinje = sykdomstidslinje.utbetalingstidslinje(dagsats)

        assertEquals(11000.0, utbetalingsTidslinje.totalSum())
    }

}