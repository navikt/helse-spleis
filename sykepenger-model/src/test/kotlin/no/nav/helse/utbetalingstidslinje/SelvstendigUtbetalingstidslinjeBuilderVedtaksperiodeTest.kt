package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.M
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SelvstendigUtbetalingstidslinjeBuilderVedtaksperiodeTest {
    @Test
    fun `melding til nav dager`() {
        undersøke(16.M + 15.S, dagerNavOvertarAnsvar = listOf(1.januar til 16.januar))
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.ventetidDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(
            PeriodeUtenNavAnsvar(
                omsluttendePeriode = 1.januar til 31.januar,
                dagerUtenAnsvar = listOf(1.januar til 16.januar),
                ferdigAvklart = true
            ), perioder.single()
        )
    }

    @Test
    fun `spredte melding til nav dager`() {
        undersøke(4.M + 2.A + 10.M + 15.S)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.ventetidDagTeller)
        assertEquals(4, inspektør.avvistDagTeller)
        assertEquals(2, perioder.size)
        assertEquals(
            PeriodeUtenNavAnsvar(
                omsluttendePeriode = 1.januar til 4.januar,
                dagerUtenAnsvar = listOf(1.januar til 4.januar),
                ferdigAvklart = false
            ), perioder[0]
        )
        assertEquals(
            PeriodeUtenNavAnsvar(
                omsluttendePeriode = 7.januar til 31.januar,
                dagerUtenAnsvar = listOf(7.januar til 22.januar),
                ferdigAvklart = true
            ), perioder[1]
        )
    }

    @BeforeEach
    fun setup() {
        reset()
    }

    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var utbetalingstidslinje: Utbetalingstidslinje
    private val perioder: MutableList<PeriodeUtenNavAnsvar> = mutableListOf()

    private fun undersøke(tidslinje: Sykdomstidslinje, dagerNavOvertarAnsvar: List<Periode> = emptyList()) {
        val ventetidberegner = Ventetidberegner()
        val ventetider = ventetidberegner.result(tidslinje)
        perioder.addAll(ventetider)

        val builder = SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(
            dekningsgrad = 80.prosent,
            ventetid = ventetider.lastOrNull()?.dagerUtenAnsvar?.singleOrNull(),
            dagerNavOvertarAnsvar = dagerNavOvertarAnsvar
        )

        utbetalingstidslinje = builder.result(tidslinje, 31000.månedlig, Beløpstidslinje())
        inspektør = utbetalingstidslinje.inspektør
    }

    // undersøker forskjellige tidslinjer som skal ha samme funksjonelle betydning
    private fun undersøkeLike(vararg tidslinje: () -> Sykdomstidslinje, dagerNavOvertarAnsvar: List<Periode> = emptyList(), assertBlock: () -> Unit) {
        tidslinje.forEach {
            undersøke(resetSeed(tidslinjegenerator = it), dagerNavOvertarAnsvar = dagerNavOvertarAnsvar)
            assertBlock()
            reset()
        }
    }

    private fun reset() {
        resetSeed()
        perioder.clear()
    }
}
