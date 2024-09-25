package no.nav.helse.person.refusjon

import no.nav.helse.desember
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.somPersonidentifikator
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class RefusjonsfaktabøtteTest {

    @Test
    fun `en helt normal inntektsmelding går oppi bøtta kan avklare en Refusjonstidslinje`() {
        val bøtte = Refusjonsfaktabøtte()
        val inntektsmelding = arbeidsgiverHendelsefabrikk.lagInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = INGEN,
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(
                beløp = INNTEKT,
                opphørsdato = null,
                endringerIRefusjon = emptyList()
            )
        )
        bøtte.leggTil(inntektsmelding)
        val avklartRefusjonstidslinje = Refusjonstidslinje(
            1.januar til 31.desember, EtBeløpMedKildePåSeg(INNTEKT, Kilde(inntektsmelding.meldingsreferanseId(), Avsender.ARBEIDSGIVER))
        )
        assertEquals(avklartRefusjonstidslinje, bøtte.avklar(1.januar til 31.desember))
    }

    @Test
    fun `en inntektsmelding uten første fraværsdag går oppi bøtta kan avklare en Refusjonstidslinje`() {
        val bøtte = Refusjonsfaktabøtte()
        val inntektsmelding = arbeidsgiverHendelsefabrikk.lagInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = INGEN,
            førsteFraværsdag = null,
            refusjon = Inntektsmelding.Refusjon(
                beløp = INNTEKT,
                opphørsdato = null,
                endringerIRefusjon = emptyList()
            )
        )
        bøtte.leggTil(inntektsmelding)
        val avklartRefusjonstidslinje = Refusjonstidslinje(
            1.januar til 31.desember, EtBeløpMedKildePåSeg(INNTEKT, Kilde(inntektsmelding.meldingsreferanseId(), Avsender.ARBEIDSGIVER))
        )
        assertEquals(avklartRefusjonstidslinje, bøtte.avklar(1.januar til 31.desember))
    }

    @Test
    fun `en inntektsmelding med opphør på refusjon går oppi bøtta kan avklare en Refusjonstidslinje`() {
        val bøtte = Refusjonsfaktabøtte()
        val inntektsmelding = arbeidsgiverHendelsefabrikk.lagInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = INGEN,
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(
                beløp = INNTEKT,
                opphørsdato = 28.desember,
                endringerIRefusjon = emptyList()
            )
        )
        bøtte.leggTil(inntektsmelding)
        val avklartRefusjonstidslinje = Refusjonstidslinje(
            1.januar til 28.desember to EtBeløpMedKildePåSeg(INNTEKT, Kilde(inntektsmelding.meldingsreferanseId(), Avsender.ARBEIDSGIVER)),
            29.desember til 31.desember to EtBeløpMedKildePåSeg(INGEN, Kilde(inntektsmelding.meldingsreferanseId(), Avsender.ARBEIDSGIVER))
        )
        assertEquals(avklartRefusjonstidslinje, bøtte.avklar(1.januar til 31.desember))
    }

    @Test
    fun `en inntektsmelding uten refusjonsbeløp går oppi bøtta kan avklare en Refusjonstidslinje`() {
        val bøtte = Refusjonsfaktabøtte()
        val inntektsmelding = arbeidsgiverHendelsefabrikk.lagInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = INGEN,
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(
                beløp = null,
                opphørsdato = null,
                endringerIRefusjon = emptyList()
            )
        )
        bøtte.leggTil(inntektsmelding)
        val avklartRefusjonstidslinje = Refusjonstidslinje(
            1.januar til 31.desember to EtBeløpMedKildePåSeg(INGEN, Kilde(inntektsmelding.meldingsreferanseId(), Avsender.ARBEIDSGIVER))
        )
        assertEquals(avklartRefusjonstidslinje, bøtte.avklar(1.januar til 31.desember))
    }

    private companion object {
        val arbeidsgiverHendelsefabrikk = ArbeidsgiverHendelsefabrikk("1", "11111111111".somPersonidentifikator(), "a1")
    }
}