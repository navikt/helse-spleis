package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

internal class RefusjonTilBeløpstidslinjeTest {

    @Test
    fun `Planke opplysning`() {
        val refusjonstidslinje = refusjontidslinje(31.januar, førsteFraværsdag = 1.januar, beløp = 100.daglig)
        assertEquals(Beløpstidslinje.fra(1.januar til 31.januar, 100.daglig, kilde), refusjonstidslinje)
    }

    @Test
    fun `Siste refusjonsdag er satt`() {
        val refusjonstidslinje = refusjontidslinje(31.januar, førsteFraværsdag = 1.januar, beløp = 100.daglig, sisteRefusjonsdag = 20.januar)
        assertEquals(Beløpstidslinje.fra(1.januar til 20.januar, 100.daglig, kilde) + Beløpstidslinje.fra(21.januar til 31.januar, INGEN, kilde), refusjonstidslinje)
    }

    @Test
    fun `Bruker endring i refusjon som siste refusjonsdag`() {
        val refusjonstidslinje = refusjontidslinje(31.januar, førsteFraværsdag = 1.januar, beløp = 100.daglig, endringer = listOf(EndringIRefusjon(INGEN, 21.januar)))
        assertEquals(Beløpstidslinje.fra(1.januar til 20.januar, 100.daglig, kilde) + Beløpstidslinje.fra(21.januar til 31.januar, INGEN, kilde), refusjonstidslinje)
    }

    @Test
    fun `Endringer i refusjon`() {
        val refusjonstidslinje = refusjontidslinje(31.januar, førsteFraværsdag = 1.januar, beløp = 100.daglig, endringer = listOf(EndringIRefusjon(INGEN, 21.januar), EndringIRefusjon(250.daglig, 22.januar), EndringIRefusjon(125.daglig, 31.januar)))
        assertEquals(Beløpstidslinje.fra(1.januar til 20.januar, 100.daglig, kilde) + Beløpstidslinje.fra(21.januar til 21.januar, INGEN, kilde) + Beløpstidslinje.fra(22.januar til 30.januar, 250.daglig, kilde) + Beløpstidslinje.fra(31.januar til 31.januar, 125.daglig, kilde), refusjonstidslinje)
    }

    @Test
    fun `siste refusjonsdag før første fraværsdag`() {
        val refusjonstidslinje = refusjontidslinje(31.januar, førsteFraværsdag = 5.januar, beløp = 100.daglig, sisteRefusjonsdag = 5.januar)
        assertEquals(Beløpstidslinje.fra(5.januar til 5.januar, 100.daglig, kilde) + Beløpstidslinje.fra(6.januar til 31.januar, INGEN, kilde), refusjonstidslinje)
    }

    @Test
    fun `Kombinerer endringer og siste refusjonsdag`() {
        val refusjonstidslinje = refusjontidslinje(31.januar, førsteFraværsdag = 1.januar, beløp = 100.daglig, endringer = listOf(EndringIRefusjon(200.daglig, 15.januar)), sisteRefusjonsdag = 20.januar)
        assertEquals(Beløpstidslinje.fra(1.januar til 14.januar, 100.daglig, kilde) + Beløpstidslinje.fra(15.januar til 20.januar, 200.daglig, kilde) + Beløpstidslinje.fra(21.januar til 31.januar, INGEN, kilde), refusjonstidslinje)
    }

    @Test
    fun `Endring i refusjon før første fraværsdag`() {
        val refusjonstidslinje = refusjontidslinje(31.januar, førsteFraværsdag = 15.januar, beløp = 100.daglig, endringer = listOf(EndringIRefusjon(200.daglig, 1.januar)))
        assertEquals(Beløpstidslinje.fra(15.januar til 31.januar, 100.daglig, kilde), refusjonstidslinje)
    }

    @Test
    fun `Tom tidslinje før første fraværsdag`(){
        val refusjonstidslinje = refusjontidslinje(1.januar, førsteFraværsdag = 2.januar, beløp = 500.daglig)
        assertEquals(Beløpstidslinje(), refusjonstidslinje)
    }

    @Test
    fun `Endringer i refusjon i fremtiden`(){
        val refusjonstidslinje = refusjontidslinje(31.januar, førsteFraværsdag = 1.januar, beløp = 500.daglig, endringer = listOf(EndringIRefusjon(1000.daglig, 1.februar)))
        assertEquals(Beløpstidslinje.fra(1.januar til 31.januar, 500.daglig, kilde), refusjonstidslinje)
    }

    private val kilde = Kilde(UUID.randomUUID(), Avsender.ARBEIDSGIVER)
    private fun refusjontidslinje(
        tilOgMed: LocalDate,
        førsteFraværsdag: LocalDate? = null,
        arbeidsgiverperioder: List<Periode> = emptyList(),
        beløp: Inntekt? = null,
        sisteRefusjonsdag: LocalDate? = null,
        endringer: List<EndringIRefusjon> = emptyList()
    ) = Refusjonshistorikk.Refusjon(
        meldingsreferanseId = kilde.meldingsreferanseId,
        førsteFraværsdag = førsteFraværsdag,
        arbeidsgiverperioder = arbeidsgiverperioder,
        beløp = beløp,
        sisteRefusjonsdag = sisteRefusjonsdag,
        endringerIRefusjon =  endringer
    ).beløpstidslinje(tilOgMed)
}
