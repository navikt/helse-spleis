package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class OppdragBuilderTest {

    @Test
    fun `kan starte oppdrag på helg`() {
        val builder = OppdragBuilder(sisteArbeidsgiverdag = 19.januar, mottaker = "ornr", fagområde = Fagområde.SykepengerRefusjon)
        builder.betalingshelgedag(20.januar, 100)
        builder.betalingshelgedag(21.januar, 100)
        builder.betalingsdag(beløpskilde(500, 0), 22.januar, 100)
        val result = builder.build()

        assertEquals(1, result.size)
        assertEquals(20.januar, result.single().inspektør.fom)
        assertEquals(22.januar, result.single().inspektør.tom)
    }

    @Test
    fun `kan slutte linje på helg`() {
        val builder = OppdragBuilder(sisteArbeidsgiverdag = 19.januar, mottaker = "ornr", fagområde = Fagområde.SykepengerRefusjon)
        builder.betalingshelgedag(20.januar, 100)
        builder.betalingshelgedag(21.januar, 100)
        builder.betalingsdag(beløpskilde(500, 0), 22.januar, 100)
        builder.betalingsdag(beløpskilde(500, 0), 23.januar, 100)
        builder.betalingsdag(beløpskilde(500, 0), 24.januar, 100)
        builder.betalingsdag(beløpskilde(500, 0), 25.januar, 100)
        builder.betalingsdag(beløpskilde(500, 0), 26.januar, 100)
        builder.betalingshelgedag(27.januar, 100)
        builder.betalingshelgedag(28.januar, 100)
        builder.ikkeBetalingsdag()
        builder.betalingsdag(beløpskilde(500, 0), 30.januar, 100)
        val result = builder.build()

        assertEquals(2, result.size)
        assertEquals(20.januar til 28.januar, result[0].inspektør.fom til result[0].inspektør.tom)
        assertEquals(30.januar.somPeriode(), result[1].inspektør.tom til result[1].inspektør.tom)
    }

    @Test
    fun `kan slutte oppdrag på helg`() {
        val builder = OppdragBuilder(sisteArbeidsgiverdag = 19.januar, mottaker = "ornr", fagområde = Fagområde.SykepengerRefusjon)
        builder.betalingshelgedag(20.januar, 100)
        builder.betalingshelgedag(21.januar, 100)
        builder.betalingsdag(beløpskilde(500, 0), 22.januar, 100)
        builder.betalingsdag(beløpskilde(500, 0), 23.januar, 100)
        builder.betalingsdag(beløpskilde(500, 0), 24.januar, 100)
        builder.betalingsdag(beløpskilde(500, 0), 25.januar, 100)
        builder.betalingsdag(beløpskilde(500, 0), 26.januar, 100)
        builder.betalingshelgedag(27.januar, 100)
        builder.betalingshelgedag(28.januar, 100)
        val result = builder.build()

        assertEquals(1, result.size)
        assertEquals(20.januar til 26.januar, result[0].inspektør.fom til result[0].inspektør.tom)
    }

    private fun beløpskilde(arbeidsgiverbeløp: Int, personbeløp: Int = 0) = object: Beløpkilde {
        override fun arbeidsgiverbeløp() = arbeidsgiverbeløp
        override fun personbeløp() = personbeløp
    }
}