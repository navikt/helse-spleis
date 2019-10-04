package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.egenmeldingsdager
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.ferie
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.sykedager
import org.junit.jupiter.api.Assertions.assertEquals
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
    }

    private val Int.juli get() = LocalDate.of(2019, JULY, this)

}