package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OppdragBuilderTest {

    private fun refusjonBuilder() = OppdragBuilder(
        mottaker = "orgnr",
        fagområde = Fagområde.SykepengerRefusjon,
        klassekode = Klassekode.RefusjonIkkeOpplysningspliktig
    )
    private fun personBuilder() = OppdragBuilder(
        mottaker = "fnr",
        fagområde = Fagområde.Sykepenger,
        klassekode = Klassekode.SykepengerArbeidstakerOrdinær
    )

    @Test
    fun `kan starte oppdrag på helg`() {
        val builder = refusjonBuilder()
        builder.betalingshelgedag(20.januar, 100)
        builder.betalingshelgedag(21.januar, 100)
        builder.betalingsdag(22.januar, 500, 100)
        val result = builder.build()

        assertEquals(1, result.size)
        assertEquals(20.januar, result.single().inspektør.fom)
        assertEquals(22.januar, result.single().inspektør.tom)
    }

    @Test
    fun `kan slutte linje på helg`() {
        val builder = refusjonBuilder()
        builder.betalingshelgedag(20.januar, 100)
        builder.betalingshelgedag(21.januar, 100)
        builder.betalingsdag(22.januar, 500, 100)
        builder.betalingsdag(23.januar, 500, 100)
        builder.betalingsdag(24.januar, 500, 100)
        builder.betalingsdag(25.januar, 500, 100)
        builder.betalingsdag(26.januar, 500, 100)
        builder.betalingshelgedag(27.januar, 100)
        builder.betalingshelgedag(28.januar, 100)
        builder.ikkeBetalingsdag()
        builder.betalingsdag(30.januar, 500, 100)
        val result = builder.build()

        assertEquals(2, result.size)
        assertEquals(20.januar til 28.januar, result[0].inspektør.fom til result[0].inspektør.tom)
        assertEquals(30.januar.somPeriode(), result[1].inspektør.tom til result[1].inspektør.tom)
    }

    @Test
    fun `kan slutte oppdrag på helg`() {
        val builder = refusjonBuilder()
        builder.betalingshelgedag(20.januar, 100)
        builder.betalingshelgedag(21.januar, 100)
        builder.betalingsdag(22.januar, 500, 100)
        builder.betalingsdag(23.januar, 500, 100)
        builder.betalingsdag(24.januar, 500, 100)
        builder.betalingsdag(25.januar, 500, 100)
        builder.betalingsdag(26.januar, 500, 100)
        builder.betalingshelgedag(27.januar, 100)
        builder.betalingshelgedag(28.januar, 100)
        val result = builder.build()

        assertEquals(1, result.size)
        assertEquals(20.januar til 26.januar, result[0].inspektør.fom til result[0].inspektør.tom)
    }

    @Test
    fun `siste dager i arbeidsgiverperioden er lørdag og søndag`() {
        val builder = personBuilder()
        builder.arbeidsgiverperiodedag(20.januar, 100)
        builder.arbeidsgiverperiodedag(21.januar, 100)
        builder.betalingsdag(22.januar, 500, 100)
        builder.betalingsdag(23.januar, 500, 100)
        builder.betalingsdag(24.januar, 500, 100)
        builder.betalingsdag(25.januar, 500, 100)
        builder.betalingsdag(26.januar, 500, 100)
        builder.betalingshelgedag(27.januar, 100)
        builder.betalingshelgedag(28.januar, 100)
        val result = builder.build()

        assertEquals(1, result.size)
        assertEquals(22.januar til 26.januar, result[0].inspektør.fom til result[0].inspektør.tom)
    }

    @Test
    fun `flekkvis utbetalingsdager i arbeidsgiverperioden`() {
        val builder = personBuilder()
        // to SykNav-dager i arbeidsgiverperioden
        builder.betalingsdag(1.januar, 500, 100)
        builder.betalingsdag(2.januar, 500, 100)
        // litt vanlig arbeidsgiverperiode
        builder.arbeidsgiverperiodedag(3.januar, 100)
        builder.arbeidsgiverperiodedag(4.januar, 100)
        // litt mer SykNav
        builder.betalingsdag(5.januar, 500, 100)
        // litt mer vanlig arbeidsgiverperiode
        builder.arbeidsgiverperiodedag(6.januar, 100)
        builder.arbeidsgiverperiodedag(7.januar, 100)
        // så over til NavDager f.eks.
        builder.betalingsdag(8.januar, 500, 100)
        builder.betalingsdag(9.januar, 500, 100)
        val result = builder.build()

        assertEquals(2, result.size)
        assertEquals(1.januar til 2.januar, result[0].inspektør.fom til result[0].inspektør.tom)
        assertEquals(5.januar til 9.januar, result[1].inspektør.fom til result[1].inspektør.tom)
    }
}
