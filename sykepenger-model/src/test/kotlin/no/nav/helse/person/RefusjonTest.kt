package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

internal class RefusjonTest {

    @Test
    fun `refusjonsbeløp uten endringer i refusjon`() {
        assertEquals(31000.månedlig, refusjon().beløp(1.januar, Aktivitetslogg()))
    }

    @Test
    fun `refusjonsbeløp med én endring i refusjon uten opphørsdato`() {
        val refusjon = refusjon(endringerIRefusjon = listOf(Refusjonshistorikk.Refusjon.EndringIRefusjon(beløp = 28000.månedlig, endringsdato = 3.januar)))
        assertEquals(31000.månedlig, refusjon.beløp(1.januar, Aktivitetslogg()))
        assertEquals(28000.månedlig, refusjon.beløp(3.januar, Aktivitetslogg()))
        assertEquals(28000.månedlig, refusjon.beløp(4.januar, Aktivitetslogg()))
    }

    @Test
    fun `hente refusjonsbeløp for dag før første fraværsdag`() {
        val refusjon = refusjon(endringerIRefusjon = emptyList())
        val aktivitetslogg = Aktivitetslogg()
        assertThrows<Aktivitetslogg.AktivitetException> { refusjon.beløp(1.januar.minusDays(1), aktivitetslogg) }
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `hente refusjonsbeløp for dag når første fraværsdag ikke er satt`() {
        val refusjon = refusjon(endringerIRefusjon = emptyList(), førsteFraværsdag = null, arbeidsgiverperioder = listOf(2.januar til 3.januar, 5.januar til 7.januar))
        val aktivitetslogg = Aktivitetslogg()

        assertEquals(31000.månedlig, refusjon.beløp(3.januar, aktivitetslogg))
        assertEquals(31000.månedlig, refusjon.beløp(5.januar, aktivitetslogg))
    }

    @Test
    fun `refusjonsbeløp med endringer i refusjon med opphørsdato`() {
        val aktivitetslogg = Aktivitetslogg()
        val refusjon = refusjon(sisteRefusjonsdag = 6.januar, endringerIRefusjon = listOf(
            Refusjonshistorikk.Refusjon.EndringIRefusjon(beløp = 28000.månedlig, endringsdato = 3.januar),
            Refusjonshistorikk.Refusjon.EndringIRefusjon(beløp = 20000.månedlig, endringsdato = 5.januar)
        ))

        assertEquals(31000.månedlig, refusjon.beløp(2.januar, aktivitetslogg))
        assertEquals(28000.månedlig, refusjon.beløp(3.januar, aktivitetslogg))
        assertEquals(20000.månedlig, refusjon.beløp(5.januar, aktivitetslogg))
        assertEquals(20000.månedlig, refusjon.beløp(6.januar, aktivitetslogg))
        assertEquals(Inntekt.INGEN, refusjon.beløp(7.januar, aktivitetslogg))
    }

    private fun refusjon(endringerIRefusjon: List<Refusjonshistorikk.Refusjon.EndringIRefusjon> = emptyList(), sisteRefusjonsdag: LocalDate? = null, førsteFraværsdag: LocalDate? = 1.januar, arbeidsgiverperioder: List<Periode> = emptyList()) = Refusjonshistorikk.Refusjon(
        meldingsreferanseId = UUID.randomUUID(),
        førsteFraværsdag = førsteFraværsdag,
        arbeidsgiverperioder = arbeidsgiverperioder,
        beløp = 31000.månedlig,
        sisteRefusjonsdag = sisteRefusjonsdag,
        endringerIRefusjon = endringerIRefusjon
    )
}
