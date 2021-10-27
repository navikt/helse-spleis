package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class RefusjonshistorikkTest {

    @Test
    fun `hente refusjon basert på utbetalingsperiode`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val refusjon = refusjon(1.januar til 16.januar)
        refusjonshistorikk.leggTilRefusjon(refusjon)
        assertSame(refusjon, refusjonshistorikk.finnRefusjon(1.januar til 31.januar, aktivitetslogg = Aktivitetslogg()))
    }

    @Test
    fun `hente refusjon basert på utbetalingsperiode med flere refusjoner`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val refusjon1 = refusjon(1.januar til 31.januar)
        val refusjon2 = refusjon(2.februar til 15.februar)
        refusjonshistorikk.leggTilRefusjon(refusjon1)
        refusjonshistorikk.leggTilRefusjon(refusjon2)
        assertSame(refusjon2, refusjonshistorikk.finnRefusjon(2.februar til 5.februar, aktivitetslogg = Aktivitetslogg()))
    }

    @Test
    fun `hente refusjon basert på utbetalingsperiode når første fraværsdag ikke treffer en refusjon`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val refusjon1 = refusjon(1.januar til 31.januar)
        val refusjon2 = refusjon(2.februar til 10.februar)
        refusjonshistorikk.leggTilRefusjon(refusjon1)
        refusjonshistorikk.leggTilRefusjon(refusjon2)
        assertNull(refusjonshistorikk.finnRefusjon(3.mars til 5.mars, aktivitetslogg = Aktivitetslogg()))
    }

    @Test
    fun `hente refusjon basert på utbetalingsperiode med en oppdelt arbeidsgiverperiode`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val refusjon1 = refusjon(1.januar til 5.januar, 15.januar til 20.januar, 25.januar til 29.januar)
        refusjonshistorikk.leggTilRefusjon(refusjon1)
        assertSame(refusjon1, refusjonshistorikk.finnRefusjon(25.januar til 31.januar, aktivitetslogg = Aktivitetslogg()))
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
        assertSame(refusjon1, refusjonshistorikk.finnRefusjon(1.januar til 31.januar, aktivitetslogg = Aktivitetslogg()))
        assertNull(refusjonshistorikk.finnRefusjon(1.mars til 20.mars, aktivitetslogg = Aktivitetslogg()))
    }

    @Test
    fun `bruker første dag i siste del av arbeidsgiverperiode når første fraværsdag ikke er satt`() {
        val refusjon = refusjon(1.januar til 16.januar, 20.januar til 25.januar, førsteFraværsdag = null)
        val refusjonshistorikk = Refusjonshistorikk()
        refusjonshistorikk.leggTilRefusjon(refusjon)
        assertSame(refusjon, refusjonshistorikk.finnRefusjon(1.januar til 31.januar, aktivitetslogg = Aktivitetslogg()))
    }

    @Test
    fun `utbetaling i Infotrygd før gjeldende periode`() {
        val refusjon = refusjon(4.januar til 19.januar)
        val refusjonshistorikk = Refusjonshistorikk()
        refusjonshistorikk.leggTilRefusjon(refusjon)
        assertSame(refusjon, refusjonshistorikk.finnRefusjon(22.januar til 31.januar, aktivitetslogg = Aktivitetslogg()))
    }

    @Test
    fun `ny info melding når vi henter basert på direkte tilstøtende arbeidsgiver periode`() {
        val refusjon = refusjon(1.januar til 16.januar)
        val refusjonshistorikk = Refusjonshistorikk()
        val aktivitetslogg = Aktivitetslogg()
        refusjonshistorikk.leggTilRefusjon(refusjon)
        assertSame(refusjon, refusjonshistorikk.finnRefusjon(17.januar til 31.januar, aktivitetslogg = aktivitetslogg))
        assertEquals(listOf("Fant refusjon ved å finne tilstøtende arbeidsgiverperiode for første utbetalingsdag i sammenhengende utbetaling"), aktivitetslogg.infoMeldinger())
    }

    @Test
    fun `ny info melding når vi henter basert på direkte tilstøtende arbeidsgiver periode med helg`() {
        val refusjon = refusjon(4.januar til 19.januar)
        val refusjonshistorikk = Refusjonshistorikk()
        val aktivitetslogg = Aktivitetslogg()
        refusjonshistorikk.leggTilRefusjon(refusjon)
        assertSame(refusjon, refusjonshistorikk.finnRefusjon(22.januar til 31.januar, aktivitetslogg = aktivitetslogg))
        assertEquals(listOf("Fant refusjon ved å finne tilstøtende arbeidsgiverperiode for første utbetalingsdag i sammenhengende utbetaling"), aktivitetslogg.infoMeldinger())
    }

    @Test
    fun `ny info melding når vi henter basert på første fraværsdag`() {
        val refusjon = refusjon(4.januar til 19.januar)
        val refusjonshistorikk = Refusjonshistorikk()
        val aktivitetslogg = Aktivitetslogg()
        refusjonshistorikk.leggTilRefusjon(refusjon)
        assertSame(refusjon, refusjonshistorikk.finnRefusjon(4.januar til 31.januar, aktivitetslogg = aktivitetslogg))
        assertEquals(listOf("Fant refusjon ved å sjekke om første fraværsdag er i sammenhengende utbetaling"), aktivitetslogg.infoMeldinger())
    }

    @Test
    fun `Logger i aktivitetsloggen når vi bruker '16 dagers hopp'`() {
        val refusjon = refusjon(4.januar til 19.januar)
        val refusjonshistorikk = Refusjonshistorikk()
        val aktivitetslogg = Aktivitetslogg()
        refusjonshistorikk.leggTilRefusjon(refusjon)
        assertSame(refusjon, refusjonshistorikk.finnRefusjon(24.januar til 31.januar, aktivitetslogg = aktivitetslogg))
        assertEquals(listOf("Fant refusjon ved å gå 16 dager tilbake fra første utbetalingsdag i sammenhengende utbetaling"), aktivitetslogg.infoMeldinger())
    }

    @Test
    fun `Finner riktig refusjon for periode med samme arbeidsgiverperiode men nytt skjæringstidspunkt - tilstøtende`() {
        // Gjelder når vi finner refusjon ut ifra tilstøtende eller 16 dagers gap
        val refusjon1 = refusjon(1.januar til 16.januar, førsteFraværsdag = 1.januar)
        val refusjon2 = refusjon(1.januar til 16.januar, førsteFraværsdag = 1.februar)
        val refusjonshistorikk = Refusjonshistorikk()
        val aktivitetslogg = Aktivitetslogg()
        refusjonshistorikk.leggTilRefusjon(refusjon1)
        refusjonshistorikk.leggTilRefusjon(refusjon2)
        assertSame(refusjon1, refusjonshistorikk.finnRefusjon(17.januar til 20.januar, aktivitetslogg))
    }

    @Test
    fun `Finner riktig refusjon for periode med samme arbeidsgiverperiode men nytt skjæringstidspunkt - gap`() {
        // Gjelder når vi finner refusjon ut ifra tilstøtende eller 16 dagers gap
        val refusjon1 = refusjon(18.desember(2017) til 2.januar)
        val refusjon2 = refusjon(31.desember(2017) til 15.januar)
        val refusjonshistorikk = Refusjonshistorikk()
        val aktivitetslogg = Aktivitetslogg()
        refusjonshistorikk.leggTilRefusjon(refusjon1)
        refusjonshistorikk.leggTilRefusjon(refusjon2)
        assertSame(refusjon2, refusjonshistorikk.finnRefusjon(17.januar til 20.januar, aktivitetslogg))
    }

    private fun refusjon(vararg arbeidsgiverperiode: Periode, meldingsreferanseId: UUID = UUID.randomUUID(), førsteFraværsdag: LocalDate? = arbeidsgiverperiode.maxOf { it.start }) = Refusjonshistorikk.Refusjon(
        meldingsreferanseId = meldingsreferanseId,
        førsteFraværsdag = førsteFraværsdag,
        arbeidsgiverperioder = arbeidsgiverperiode.toList(),
        beløp = 31000.månedlig,
        sisteRefusjonsdag = null,
        endringerIRefusjon = emptyList()
    )

    private fun Aktivitetslogg.infoMeldinger(): List<String> {
        val meldinger = mutableListOf<String>()

        accept(object : AktivitetsloggVisitor {
            override fun visitInfo(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) {
                meldinger.add(melding)
            }
        })

        return meldinger
    }
}
