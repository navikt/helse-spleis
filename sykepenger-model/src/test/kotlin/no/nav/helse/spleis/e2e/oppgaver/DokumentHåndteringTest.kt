package no.nav.helse.spleis.e2e.oppgaver

import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.forkastAlle
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        val im1 = håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        val hendelserHåndtertFør = inspektør.hendelser(1.vedtaksperiode)
        assertEquals(listOf(
            im1 to 1.vedtaksperiode.id(ORGNUMMER), // pga dager
            im1 to 1.vedtaksperiode.id(ORGNUMMER), // pga inntekt
        ), observatør.inntektsmeldingHåndtert)
        assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
        val søknad = håndterSøknad(Sykdom(10.februar, 28.februar, 100.prosent))
        val im = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 10.februar)
        // im1 blir replayet av søknad i februar, men plukkes naturligvis ikke opp av noen
        // spre-oppgaver vil ikke lage oppgave likevel, siden inntektsmeldingen har blitt håndtert tidligere
        assertEquals(listOf(im1), observatør.inntektsmeldingIkkeHåndtert)
        assertEquals(hendelserHåndtertFør, inspektør.hendelser(1.vedtaksperiode))
        assertEquals(listOf(
            Dokumentsporing.søknad(søknad),
            Dokumentsporing.inntektsmeldingInntekt(im)
        ), inspektør.hendelser(2.vedtaksperiode))
        assertEquals(3, observatør.inntektsmeldingHåndtert.size)
        assertEquals(im to 2.vedtaksperiode.id(ORGNUMMER), observatør.inntektsmeldingHåndtert.last())
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

        assertEquals(listOf(
            Dokumentsporing.søknad(søknad1),
            Dokumentsporing.inntektsmeldingDager(im)
        ), inspektør.hendelser(1.vedtaksperiode))
        assertEquals(listOf(
            Dokumentsporing.søknad(søknad2),
            Dokumentsporing.inntektsmeldingDager(im)
        ), inspektør.hendelser(2.vedtaksperiode))
        assertEquals(listOf(
            Dokumentsporing.søknad(søknad3),
            Dokumentsporing.inntektsmeldingInntekt(im)
        ), inspektør.hendelser(3.vedtaksperiode))
        assertEquals(listOf(
            Dokumentsporing.søknad(søknad4),
            Dokumentsporing.inntektsmeldingInntekt(im)
        ), inspektør.hendelser(4.vedtaksperiode))

        assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
        assertEquals(listOf(
            søknad1 to 1.vedtaksperiode.id(ORGNUMMER),
            søknad2 to 2.vedtaksperiode.id(ORGNUMMER),
            søknad3 to 3.vedtaksperiode.id(ORGNUMMER),
            søknad4 to 4.vedtaksperiode.id(ORGNUMMER)
        ), observatør.søknadHåndtert)
        assertEquals(listOf(
            im to 1.vedtaksperiode.id(ORGNUMMER), // dager
            im to 2.vedtaksperiode.id(ORGNUMMER), // dager
            im to 3.vedtaksperiode.id(ORGNUMMER), // inntekt
            im to 4.vedtaksperiode.id(ORGNUMMER) // inntekt
        ), observatør.inntektsmeldingHåndtert)
    }

    @Test
    fun `har overlappende avslutta vedtaksperiode på annen arbeidsgiver`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a2)
        val søknad2 = håndterSøknad(Sykdom(28.januar, 28.februar, 100.prosent))
        assertEquals(
            PersonObserver.VedtaksperiodeForkastetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = 1.vedtaksperiode.id(ORGNUMMER),
                gjeldendeTilstand = TilstandType.START,
                hendelser = setOf(søknad2),
                fom = 28.januar,
                tom = 28.februar,
                forlengerPeriode = true,
                forlengerSpleisEllerInfotrygd = false,
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true
            ), observatør.forkastet(1.vedtaksperiode.id(ORGNUMMER))
        )
    }

    @Test
    fun `har periode rett før men det er en AUU`() {
        nyPeriode(1.januar til 16.januar)
        assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)

        val søknad2 = håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        val im = håndterInntektsmelding(listOf(10.januar til 26.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "FiskerMedHyre")
        assertFunksjonellFeil(RV_IM_8)
        assertSisteTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        assertEquals(
            PersonObserver.VedtaksperiodeForkastetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = 2.vedtaksperiode.id(ORGNUMMER),
                gjeldendeTilstand = TilstandType.AVVENTER_INNTEKTSMELDING,
                hendelser = setOf(søknad2),
                fom = 17.januar,
                tom = 31.januar,
                forlengerPeriode = false,
                forlengerSpleisEllerInfotrygd = true,
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true
            ), observatør.forkastet(2.vedtaksperiode.id(ORGNUMMER))
        )
        assertTrue(im in observatør.inntektsmeldingIkkeHåndtert)
    }

    @Test
    fun `har en periode rett før på annen arbeidsgiver`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a2)
        val søknad2 = håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        assertEquals(
            PersonObserver.VedtaksperiodeForkastetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = 1.vedtaksperiode.id(ORGNUMMER),
                gjeldendeTilstand = TilstandType.START,
                hendelser = setOf(søknad2),
                fom = 1.februar,
                tom = 28.februar,
                forlengerPeriode = true,
                forlengerSpleisEllerInfotrygd = false,
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true
            ), observatør.forkastet(1.vedtaksperiode.id(ORGNUMMER))
        )
    }

    @Test
    fun `har ikke overlappende vedtaksperioder`() {
        tilGodkjenning(1.januar, 31.januar, ORGNUMMER)
        håndterUtbetalingsgodkjenning(utbetalingGodkjent = false)
        val søknad2 = håndterSøknad(Sykdom(28.januar, 28.februar, 100.prosent))
        assertEquals(
            PersonObserver.VedtaksperiodeForkastetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = 2.vedtaksperiode.id(ORGNUMMER),
                gjeldendeTilstand = TilstandType.START,
                hendelser = setOf(søknad2),
                fom = 28.januar,
                tom = 28.februar,
                forlengerPeriode = false,
                forlengerSpleisEllerInfotrygd = false,
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true
            ), observatør.forkastet(2.vedtaksperiode.id(ORGNUMMER)))
    }
    @Test
    fun `har vedtaksperiode som påvirker arbeidsgiverperioden`() {
        tilGodkjenning(1.januar, 31.januar, ORGNUMMER)

        val søknad2 = håndterSøknad(Sykdom(10.februar, 28.februar, 100.prosent))
        håndterUtbetalingsgodkjenning(utbetalingGodkjent = false)
        assertEquals(
            PersonObserver.VedtaksperiodeForkastetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = 2.vedtaksperiode.id(ORGNUMMER),
                gjeldendeTilstand = TilstandType.AVVENTER_INNTEKTSMELDING,
                hendelser = setOf(søknad2),
                fom = 10.februar,
                tom = 28.februar,
                forlengerPeriode = false,
                forlengerSpleisEllerInfotrygd = false,
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true
            ), observatør.forkastet(2.vedtaksperiode.id(ORGNUMMER)))
    }

    @Test
    fun `har ikke overlappende vedtaksperiode`() {
        tilGodkjenning(1.januar, 31.januar, ORGNUMMER)
        håndterUtbetalingsgodkjenning(utbetalingGodkjent = false)

        val søknad2 = håndterSøknad(Sykdom(15.februar, 28.februar, 100.prosent))
        assertEquals(
            PersonObserver.VedtaksperiodeForkastetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = 2.vedtaksperiode.id(ORGNUMMER),
                gjeldendeTilstand = TilstandType.START,
                hendelser = setOf(søknad2),
                fom = 15.februar,
                tom = 28.februar,
                forlengerPeriode = false,
                forlengerSpleisEllerInfotrygd = false,
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true
            ), observatør.forkastet(2.vedtaksperiode.id(ORGNUMMER)))
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
                tom = 15.januar,
                forlengerPeriode = false,
                forlengerSpleisEllerInfotrygd = false,
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true
            ), observatør.forkastet(2.vedtaksperiode.id(ORGNUMMER)))
    }

    @Test
    fun `sender ut søknad håndtert for forlengelse av forkastet periode`(){
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        val søknadId1 = håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        forkastAlle(hendelselogg)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        val søknadId2 = håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        assertEquals(listOf(
            søknadId1 to 1.vedtaksperiode.id(ORGNUMMER),
            søknadId2 to 2.vedtaksperiode.id(ORGNUMMER)
        ), observatør.søknadHåndtert)
    }
}
