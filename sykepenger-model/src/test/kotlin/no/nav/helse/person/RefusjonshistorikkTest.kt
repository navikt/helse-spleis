package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class RefusjonshistorikkTest {

    @Test
    fun `hente refusjon basert på utbetalingsperiode`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val refusjon = refusjon(1.januar til 16.januar)
        refusjonshistorikk.leggTilRefusjon(refusjon)
        assertSame(refusjon, refusjonshistorikk.finnRefusjon(1.januar til 31.januar))
    }

    @Test
    fun `hente refusjon basert på utbetalingsperiode med flere refusjoner`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val refusjon1 = refusjon(1.januar til 31.januar)
        val refusjon2 = refusjon(2.februar til 15.februar)
        refusjonshistorikk.leggTilRefusjon(refusjon1)
        refusjonshistorikk.leggTilRefusjon(refusjon2)
        assertSame(refusjon2, refusjonshistorikk.finnRefusjon(2.februar til 5.februar))
    }

    @Test
    fun `hente refusjon basert på utbetalingsperiode når første fraværsdag ikke treffer en refusjon`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val refusjon1 = refusjon(1.januar til 31.januar)
        val refusjon2 = refusjon(2.februar til 10.februar)
        refusjonshistorikk.leggTilRefusjon(refusjon1)
        refusjonshistorikk.leggTilRefusjon(refusjon2)
        assertNull(refusjonshistorikk.finnRefusjon(3.mars til 5.mars))
    }

    @Test
    fun `hente refusjon basert på utbetalingsperiode med en oppdelt arbeidsgiverperiode`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val refusjon1 = refusjon(1.januar til 5.januar, 15.januar til 20.januar, 25.januar til 29.januar)
        refusjonshistorikk.leggTilRefusjon(refusjon1)
        assertSame(refusjon1, refusjonshistorikk.finnRefusjon(25.januar til 31.januar))
    }

    @Test
    fun `legger ikke til refusjon for samme inntektsmelding flere ganger`() {
        val meldingsreferanseId = UUID.randomUUID()
        val refusjon1 = refusjon(1.januar til 16.januar, meldingsreferanseId = meldingsreferanseId)
        // I praksis vil ikke arbeidsgiverperioden være noe annet, men vi har satt den for å validere duplikatsjekk
        val refusjon2 = refusjon(1.mars til 16.mars, meldingsreferanseId = meldingsreferanseId)
        val refusjonshistorikk = Refusjonshistorikk()
        refusjonshistorikk.leggTilRefusjon(refusjon1)
        refusjonshistorikk.leggTilRefusjon(refusjon2)
        assertSame(refusjon1, refusjonshistorikk.finnRefusjon(1.januar til 31.januar))
        assertNull(refusjonshistorikk.finnRefusjon(1.mars til 20.mars))
    }

    @Test
    fun `bruker første dag i siste del av arbeidsgiverperiode når første fraværsdag ikke er satt`() {
        val refusjon = refusjon(1.januar til 16.januar, 20.januar til 25.januar, førsteFraværsdag = null)
        val refusjonshistorikk = Refusjonshistorikk()
        refusjonshistorikk.leggTilRefusjon(refusjon)
        assertSame(refusjon, refusjonshistorikk.finnRefusjon(1.januar til 31.januar))
    }

    @Test
    fun `utbetaling i Infotrygd før gjeldende periode`() {
        val refusjon = refusjon(4.januar til 19.januar)
        val refusjonshistorikk = Refusjonshistorikk()
        refusjonshistorikk.leggTilRefusjon(refusjon)
        assertSame(refusjon, refusjonshistorikk.finnRefusjon(22.januar til 31.januar))
    }

    private fun refusjon(vararg arbeidsgiverperiode: Periode, meldingsreferanseId: UUID = UUID.randomUUID(), førsteFraværsdag: LocalDate? = arbeidsgiverperiode.maxOf { it.start }) = Refusjonshistorikk.Refusjon(
        meldingsreferanseId = meldingsreferanseId,
        førsteFraværsdag = førsteFraværsdag,
        arbeidsgiverperioder = arbeidsgiverperiode.toList(),
        beløp = 31000.månedlig,
        sisteRefusjonsdag = null,
        endringerIRefusjon = emptyList()
    )
}
