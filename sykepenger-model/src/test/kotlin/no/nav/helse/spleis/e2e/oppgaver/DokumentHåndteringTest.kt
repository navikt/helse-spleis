package no.nav.helse.spleis.e2e.oppgaver

import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DokumentHåndteringTest : AbstractEndToEndTest() {

    @Test
    fun `Inntektsmelding før søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        val id = håndterInntektsmelding(listOf(1.januar til 16.januar))
        val inntektsmeldingFørSøknadEvent = observatør.inntektsmeldingFørSøknad.single()
        inntektsmeldingFørSøknadEvent.let {
            assertEquals(id, it.inntektsmeldingId)
            assertEquals(listOf(1.januar til 16.januar), it.overlappendeSykmeldingsperioder)
        }
    }

    @Test
    fun `Inntektsmelding ikke håndtert`() {
        val id = håndterInntektsmelding(listOf(1.januar til 16.januar))
        val inntektsmelding = observatør.inntektsmeldingIkkeHåndtert.single()
        assertEquals(id, inntektsmelding)
    }

    @Test
    fun `Inntektsmelding bare håndtert inntekt`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSøknad(Sykdom(10.februar, 28.februar, 100.prosent))
        val im = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 10.februar)
        assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
        assertEquals(3, observatør.inntektsmeldingHåndtert.size)
        assertEquals(listOf(
            im to 2.vedtaksperiode.id(ORGNUMMER),
            im to 1.vedtaksperiode.id(ORGNUMMER) // todo: vedtaksperiode 1 håndterer tydligvis dagene
        ), observatør.inntektsmeldingHåndtert.takeLast(2))
    }

    @Test
    fun `Inntektsmelding noen dager håndtert`() {
        val søknad = håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        val im = håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
        assertEquals(listOf(søknad to 1.vedtaksperiode.id(ORGNUMMER)), observatør.søknadHåndtert)
        assertEquals(listOf(im to 1.vedtaksperiode.id(ORGNUMMER)), observatør.inntektsmeldingHåndtert)
    }

    @Test
    fun `Inntektsmelding håndteres av flere`() {
        val søknad1 = håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        val søknad2 = håndterSøknad(Sykdom(11.januar, 16.januar, 100.prosent))
        val søknad3 = håndterSøknad(Sykdom(17.januar, 20.januar, 100.prosent))
        val søknad4 = håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))
        val im = håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
        assertEquals(listOf(
            søknad1 to 1.vedtaksperiode.id(ORGNUMMER),
            søknad2 to 2.vedtaksperiode.id(ORGNUMMER),
            søknad3 to 3.vedtaksperiode.id(ORGNUMMER),
            søknad4 to 4.vedtaksperiode.id(ORGNUMMER)
        ), observatør.søknadHåndtert)
        assertEquals(listOf(
            im to 3.vedtaksperiode.id(ORGNUMMER),
            im to 1.vedtaksperiode.id(ORGNUMMER),
            im to 2.vedtaksperiode.id(ORGNUMMER),
            im to 4.vedtaksperiode.id(ORGNUMMER)
        ), observatør.inntektsmeldingHåndtert)
    }
    @Test
    fun `delvis overlappende søknad`() {
        val søknad1 = håndterSøknad(Sykdom(11.januar, 16.januar, 100.prosent))
        val søknad2 = håndterSøknad(Sykdom(10.januar, 15.januar, 100.prosent))
        assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
        assertEquals(listOf(
            søknad1 to 1.vedtaksperiode.id(ORGNUMMER),
            søknad2 to 1.vedtaksperiode.id(ORGNUMMER)
        ), observatør.søknadHåndtert)
        assertEquals(
            PersonObserver.VedtaksperiodeForkastetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = 2.vedtaksperiode.id(ORGNUMMER),
                gjeldendeTilstand = TilstandType.START,
                hendelser = setOf(søknad2),
                fom = 10.januar,
                tom = 15.januar
            ), observatør.forkastet(2.vedtaksperiode.id(ORGNUMMER)))
    }
}
