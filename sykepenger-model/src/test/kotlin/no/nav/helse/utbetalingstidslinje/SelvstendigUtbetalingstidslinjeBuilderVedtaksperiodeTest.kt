package no.nav.helse.utbetalingstidslinje

import java.util.UUID
import no.nav.helse.hendelser.ForsikringsvurderingResultat
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.DagerUtenNavAnsvaravklaring
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Pleiepenger
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.M
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.YF
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Prosentdel.Companion.riktigProsent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SelvstendigUtbetalingstidslinjeBuilderVedtaksperiodeTest {

    @Test
    fun `melding til nav dager`() {
        undersøke(
            tidslinje = 16.M + 15.S,
            forsikringsvurderingResultat = ForsikringsvurderingResultat(
                forsikringsvurderingId = UUID.randomUUID(),
                harForsikring = true,
                dekning = ForsikringsvurderingResultat.Dekning(grad = 80, iVentetid = true),
                opphørsdato = null,
            )
        )
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

    @Test
    fun `En dag som både er avslått på avslagstidslinjen og 'maskinelt' avslått`() {
        val periode = 1.januar til 10.januar
        undersøke(10.YF(Pleiepenger), avslagstidslinje = Avslagstidslinje(periode to Avslagstidslinje.Avslagsdag(listOf(Begrunnelse.AvslåttMeldingTilNavDag), "Test")))
        periode.forEach { dato ->
            val avslagsdag = utbetalingstidslinje[dato] as? Utbetalingsdag.AvvistDag
            assertNotNull(avslagsdag)
            assertEquals(listOf(Begrunnelse.AvslåttMeldingTilNavDag, Begrunnelse.AndreYtelserPleiepenger), avslagsdag.begrunnelser)
        }
    }

    @Test
    fun `100 prosent dekning forsikring utløper i periode, gir forskjellig utbetaling før og etter utløp`() {
        val opphørsdato = 22.januar
        val dekningsgradIForsikring = 100
        undersøke(
            tidslinje = 16.M + 14.S,
            forsikringsvurderingResultat = ForsikringsvurderingResultat(
                forsikringsvurderingId = UUID.randomUUID(),
                harForsikring = true,
                dekning = ForsikringsvurderingResultat.Dekning(grad = dekningsgradIForsikring, iVentetid = true),
                opphørsdato = opphørsdato,
            )
        )
        assertEquals(30, utbetalingstidslinje.size)
        for (dato in (14.januar til opphørsdato)) {
            assertEquals(dekningsgradIForsikring.riktigProsent, utbetalingstidslinje.inspektør.dekningsgrad(dato)) { "Frem til opphørsdato skal dekning i utbetaling være 100 %" }
        }
        for (dato in opphørsdato.plusDays(1) til 30.januar) {
            assertEquals(80.prosent, utbetalingstidslinje.inspektør.dekningsgrad(dato)) { "Etter opphørsdato for forsikring skal dekningsgrad være 80 %" }
        }
    }

    @BeforeEach
    fun setup() {
        reset()
    }

    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var utbetalingstidslinje: Utbetalingstidslinje
    private val perioder: MutableList<PeriodeUtenNavAnsvar> = mutableListOf()

    private fun undersøke(
        tidslinje: Sykdomstidslinje,
        forsikringsvurderingResultat: ForsikringsvurderingResultat = ForsikringsvurderingResultat(
            forsikringsvurderingId = UUID.randomUUID(),
            harForsikring = false,
            dekning = null,
            opphørsdato = null,
        ),
        avslagstidslinje: Avslagstidslinje = Avslagstidslinje()
    ) {
        val ventetidberegner = Ventetidberegner()
        val ventetider = ventetidberegner.result(tidslinje)
        perioder.addAll(ventetider)

        val builder = SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(
            forsikringsvurderingResultat = forsikringsvurderingResultat,
            dagerUtenNavAnsvar = DagerUtenNavAnsvaravklaring(true, ventetider.lastOrNull()?.dagerUtenAnsvar.orEmpty()),
            avslagstidslinje = avslagstidslinje
        )

        utbetalingstidslinje = builder.result(tidslinje, 31000.månedlig, Beløpstidslinje())
        inspektør = utbetalingstidslinje.inspektør
    }

    private fun reset() {
        resetSeed()
        perioder.clear()
    }
}
