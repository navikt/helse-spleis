package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.NAP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetalingFilter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingkladderBuilderTest {

    @Test
    fun `kladdene starter på første arbeidsgiverperiodedag`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV, 16.ARB, 1.AP, 15.ARB, 15.AP, 15.NAV).betale()
        val builder = UtbetalingkladderBuilder(tidslinje, "orgnr", "fnr")
        val result = builder.build()
        assertEquals(2, result.size)
        result[0].also { kladd ->
            val utbetaling = kladd.lagUtbetaling(Utbetalingtype.UTBETALING, null, tidslinje, LocalDate.MAX, 0, 0)
            assertEquals(1.januar til 16.februar, utbetaling.inspektør.periode)
        }
        result[1].also { kladd ->
            val utbetaling = kladd.lagUtbetaling(Utbetalingtype.UTBETALING, null, tidslinje, LocalDate.MAX, 0, 0)
            assertEquals(17.februar til 3.april, utbetaling.inspektør.periode)
        }
    }

    @Test
    fun `kladdene starter på første arbeidsgiverperiodedagNav`() {
        val tidslinje = tidslinjeOf(16.NAP, 15.NAV, 16.ARB, 1.NAP, 15.ARB, 15.NAP, 15.NAV).betale()
        val builder = UtbetalingkladderBuilder(tidslinje, "orgnr", "fnr")
        val result = builder.build()
        assertEquals(2, result.size)
        result[0].also { kladd ->
            val utbetaling = kladd.lagUtbetaling(Utbetalingtype.UTBETALING, null, tidslinje, LocalDate.MAX, 0, 0)
            assertEquals(1.januar til 16.februar, utbetaling.inspektør.periode)
            val arbeidsgiverOppdrag = utbetaling.inspektør.arbeidsgiverOppdrag
            assertEquals(1, arbeidsgiverOppdrag.size)
            assertEquals(januar, arbeidsgiverOppdrag.single().periode)
        }
        result[1].also { kladd ->
            val utbetaling = kladd.lagUtbetaling(Utbetalingtype.UTBETALING, null, tidslinje, LocalDate.MAX, 0, 0)
            assertEquals(17.februar til 3.april, utbetaling.inspektør.periode)
            val arbeidsgiverOppdrag = utbetaling.inspektør.arbeidsgiverOppdrag
            assertEquals(1, arbeidsgiverOppdrag.size)
            assertEquals(5.mars til 3.april, arbeidsgiverOppdrag.single().periode)
        }
    }

    @Test
    fun `nav utbetaler starten av arbeidsgiverperioden`() {
        val tidslinje = tidslinjeOf(10.NAP, 6.AP, 9.NAV).betale()
        val builder = UtbetalingkladderBuilder(tidslinje, "orgnr", "fnr")
        val result = builder.build()
        assertEquals(1, result.size)
        result.single().also { kladd ->
            val utbetaling = kladd.lagUtbetaling(Utbetalingtype.UTBETALING, null, tidslinje, LocalDate.MAX, 0, 0)
            assertEquals(1.januar til 25.januar, utbetaling.inspektør.periode)
            val arbeidsgiverOppdrag = utbetaling.inspektør.arbeidsgiverOppdrag
            assertEquals(2, arbeidsgiverOppdrag.size)
            assertEquals(1.januar til 10.januar, arbeidsgiverOppdrag[0].inspektør.periode)
            assertEquals(17.januar til 25.januar, arbeidsgiverOppdrag[1].inspektør.periode)
        }
    }

    private fun Utbetalingstidslinje.betale(aktivitetslogg: Aktivitetslogg = Aktivitetslogg()) =
        MaksimumUtbetalingFilter().betal(listOf(this), this.periode(), aktivitetslogg, MaskinellJurist()).single()
}