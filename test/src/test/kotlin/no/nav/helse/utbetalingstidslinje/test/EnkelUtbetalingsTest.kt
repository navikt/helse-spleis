package no.nav.helse.utbetalingstidslinje.test

import no.nav.helse.sykdomstidlinje.test.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.egenmeldingsdager
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.ferie
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.sykedager
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.utenlandsdag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.JULY

internal class EnkelUtbetalingsTest {

    companion object {
        private val testKildeHendelse = Testhendelse(rapportertdato = LocalDateTime.of(2019, JULY, 1, 0, 0))
    }

    @Test
    internal fun `arbeidsgiverperioden gir 0 kr`() {
        val sykdomstidslinje = sykedager(
            1.juli, 16.juli,
            testKildeHendelse
        )
        val dagsats = 1000.0

        val utbetalingsTidslinje = sykdomstidslinje.utbetalingstidslinje(dagsats)

        assertEquals(0.0, utbetalingsTidslinje.totalSum(), "")
    }

    @Test
    internal fun `sykedager etter arbeidsgiverperioden gir dagsats`() {
        val dagsats = 1000.0
        assertEquals(
            dagsats,
            sykedager(1.juli, 17.juli, testKildeHendelse).utbetalingstidslinje(dagsats).totalSum(),
            "Første dag etter arbeidsgiverperioden skal utbetales"
        )
    }

    @Disabled
    @Test
    internal fun `syke-, ferie- og egenmeldingsdager blir med i utbetalingstidslinja`() {
        val sykdomstidslinje = egenmeldingsdager(1.juli, 3.juli, testKildeHendelse) +
                sykedager(4.juli, 16.juli, testKildeHendelse) +
                ferie(17.juli, 21.juli, testKildeHendelse) +
                sykedager(22.juli, 24.juli, testKildeHendelse)

        val dagsats = 1000.0

        val utbetalingsTidslinje = sykdomstidslinje.utbetalingstidslinje(dagsats)

        assertEquals(
            3000.0,
            utbetalingsTidslinje.totalSum(),
            "Forventet utbetaling skal være tre ganger dagsats siden tidslinjen har tre sykedager utenfor arbeidsgiverpoerioden"
        )
        assertTrue(utbetalingsTidslinje.erAvklart())
    }

    @Test
    internal fun `utenlandsopphold i arbeidsgiverperioden påvirker ikke totalsummen`() {
        val sykdomstidslinje = utenlandsdag(1.juli, testKildeHendelse)

        val utbetalingstidslinje = sykdomstidslinje.utbetalingstidslinje(1000.0)

        assertEquals(
            0.0,
            utbetalingstidslinje.totalSum()
        )

        assertFalse(utbetalingstidslinje.erAvklart())
    }

    @Disabled
    @Test
    internal fun `utenlandsopphold i arbeidsgiverperioden telles ikke som en dag i arbeidsgiverperioden`() {
        val sykdomstidslinje = sykedager(1.juli, 8.juli, testKildeHendelse) +
                utenlandsdag(9.juli, testKildeHendelse) +
                sykedager(10.juli, 18.juli, testKildeHendelse)

        val utbetalingstidslinje = sykdomstidslinje.utbetalingstidslinje(1000.0)

        assertEquals(
            1000.0,
            utbetalingstidslinje.totalSum()
        )

        assertFalse(utbetalingstidslinje.erAvklart())
    }

    @Disabled
    @Test
    internal fun `utenlandsopphold utbetales ikke når trygden yter`() {
        val sykdomstidslinje = sykedager(1.juli, 17.juli, testKildeHendelse) +
                utenlandsdag(18.juli, testKildeHendelse)

        val utbetalingstidslinje = sykdomstidslinje.utbetalingstidslinje(1000.0)

        assertEquals(
            1000.0,
            utbetalingstidslinje.totalSum()
        )

        assertFalse(utbetalingstidslinje.erAvklart())
    }

    private val Int.juli get() = LocalDate.of(2019, JULY, this)

}
