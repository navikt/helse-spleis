package no.nav.helse.spleis.e2e.oppgaver

import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_2
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.forkastAlle
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSkjønnsmessigFastsettelse
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DokumentHåndteringTest : AbstractEndToEndTest() {

    @Test
    fun `Inntektsmelding kommer mellom AUU og søknad for førstegangsbehandling`() {
        val søknad1 = håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        val inntektsmelding = håndterInntektsmelding(listOf(1.januar til 16.januar))
        val søknad2 = håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        assertEquals(setOf(søknad1, inntektsmelding), inspektør.hendelseIder(1.vedtaksperiode))

        assertEquals(setOf(søknad2, inntektsmelding), inspektør.hendelseIder(2.vedtaksperiode))
    }

    @Test
    fun `Inntektsmelding kommer mellom AUU og søknad for førstegangsbehandling flere arbeidsgivere`() {
        val søknad1A1 = håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        val søknad1A2 = håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a2)

        val inntektsmeldingA1 = håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        val inntektsmeldingA2 = håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        val søknad2A1 = håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a1)
        val søknad2A2 = håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterVilkårsgrunnlag(2.vedtaksperiode, orgnummer = a1)

        assertEquals(setOf(søknad1A1, inntektsmeldingA1), inspektør(a1).hendelseIder(1.vedtaksperiode))
        assertEquals(setOf(søknad1A2, inntektsmeldingA2), inspektør(a2).hendelseIder(1.vedtaksperiode))

        assertEquals(setOf(søknad2A1, inntektsmeldingA1), inspektør(a1).hendelseIder(2.vedtaksperiode))
        assertEquals(setOf(søknad2A2, inntektsmeldingA2), inspektør(a2).hendelseIder(2.vedtaksperiode))
    }

    @Test
    fun `Alle vedtaksperioder på skjæringstidspunktet skal få referanse til overstyring på tvers av arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)

        val januarA1DokumentIderFør = inspektør(a1).hendelseIder(1.vedtaksperiode)
        val februarA1DokumentIderFør = inspektør(a1).hendelseIder(2.vedtaksperiode)
        val januarA2DokumentIderFør = inspektør(a2).hendelseIder(1.vedtaksperiode)
        val februarA2DokumentIderFør = inspektør(a2).hendelseIder(2.vedtaksperiode)

        val overstyringId = håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = 21000.månedlig),
            OverstyrtArbeidsgiveropplysning(orgnummer = a2, inntekt = 25500.månedlig)
        ))
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        val januarA1DokumentIderEtter = inspektør(a1).hendelseIder(1.vedtaksperiode)
        val februarA1DokumentIderEtter = inspektør(a1).hendelseIder(2.vedtaksperiode)
        val januarA2DokumentIderEtter = inspektør(a2).hendelseIder(1.vedtaksperiode)
        val februarA2DokumentIderEtter = inspektør(a2).hendelseIder(2.vedtaksperiode)

        assertTrue(inspektør(a1).hendelser(1.vedtaksperiode).contains(Dokumentsporing.overstyrArbeidsgiveropplysninger(overstyringId)))

        assertEquals(januarA1DokumentIderFør.plus(overstyringId), januarA1DokumentIderEtter)
        assertEquals(februarA1DokumentIderFør.plus(overstyringId), februarA1DokumentIderEtter)

        assertEquals(januarA2DokumentIderFør.plus(overstyringId), januarA2DokumentIderEtter)
        assertEquals(februarA2DokumentIderFør.plus(overstyringId), februarA2DokumentIderEtter)
    }

    @Test
    fun `to helt like korrigerende inntektsmeldinger`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent, beregnetInntekt = INNTEKT)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 1.1)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()
        observatør.inntektsmeldingIkkeHåndtert.clear()
        observatør.inntektsmeldingHåndtert.clear()
        val korrigertInntektsmelding2 = håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 1.1)
        assertEquals(listOf(korrigertInntektsmelding2, korrigertInntektsmelding2), observatør.inntektsmeldingHåndtert.map { it.first })
        assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
    }

    @Test
    fun `to helt like korrigerende inntektsmeldinger med toggle disabled`() = Toggle.RevurdereAgpFraIm.disable {
        nyttVedtak(1.januar, 31.januar, 100.prosent, beregnetInntekt = INNTEKT)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 1.1)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()
        observatør.inntektsmeldingIkkeHåndtert.clear()
        observatør.inntektsmeldingHåndtert.clear()
        val korrigertInntektsmelding2 = håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 1.1)
        assertEquals(emptyList<UUID>(), observatør.inntektsmeldingHåndtert.map { it.first })
        assertEquals(listOf(korrigertInntektsmelding2), observatør.inntektsmeldingIkkeHåndtert)
    }

    @Test
    fun `inntektsmelding med korrigerende inntekt på én av to arbeidsgivere på ett av to skjæringstidspunkt`() = Toggle.TjuefemprosentAvvik.enable {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        nyeVedtak(1.april, 30.april, a1, a2)

        val januarA1DokumentIderFør = inspektør(a1).hendelseIder(1.vedtaksperiode)
        val februarA1DokumentIderFør = inspektør(a1).hendelseIder(2.vedtaksperiode)
        val aprilA1DokumentIderFør = inspektør(a1).hendelseIder(3.vedtaksperiode)
        val januarA2DokumentIderFør = inspektør(a2).hendelseIder(1.vedtaksperiode)
        val februarA2DokumentIderFør = inspektør(a2).hendelseIder(2.vedtaksperiode)
        val aprilA2DokumentIderFør = inspektør(a2).hendelseIder(3.vedtaksperiode)

        val a1KorrigertInntektsmelding = håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 1.1, orgnummer = a1)
        assertVarsel(RV_IV_2)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING)
        val skjønnsmessigFastsettelse = håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.1), OverstyrtArbeidsgiveropplysning(a2, INNTEKT)))

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(3.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, orgnummer = a1)

        håndterYtelser(3.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        val januarA1DokumentIderEtter = inspektør(a1).hendelseIder(1.vedtaksperiode)
        val februarA1DokumentIderEtter = inspektør(a1).hendelseIder(2.vedtaksperiode)
        val aprilA1DokumentIderEtter = inspektør(a1).hendelseIder(3.vedtaksperiode)
        val januarA2DokumentIderEtter = inspektør(a2).hendelseIder(1.vedtaksperiode)
        val februarA2DokumentIderEtter = inspektør(a2).hendelseIder(2.vedtaksperiode)
        val aprilA2DokumentIderEtter = inspektør(a2).hendelseIder(3.vedtaksperiode)

        assertEquals(januarA2DokumentIderFør.plus(skjønnsmessigFastsettelse), januarA2DokumentIderEtter)
        assertEquals(februarA2DokumentIderFør.plus(skjønnsmessigFastsettelse), februarA2DokumentIderEtter)
        assertEquals(aprilA1DokumentIderFør, aprilA1DokumentIderEtter)
        assertEquals(aprilA2DokumentIderFør, aprilA2DokumentIderEtter)

        assertEquals(januarA1DokumentIderFør.plus(a1KorrigertInntektsmelding).plus(skjønnsmessigFastsettelse), januarA1DokumentIderEtter)
        assertEquals(februarA1DokumentIderFør.plus(a1KorrigertInntektsmelding).plus(skjønnsmessigFastsettelse), februarA1DokumentIderEtter)
    }

    @Test
    fun `sender ikke ut signal om at inntektsmelding ikke er håndtert om annen vedtaksperiode har håndtert inntektsmelding før`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSøknad(Sykdom(20.februar, 20.mars, 100.prosent))
        assertEquals(emptyList<UUID>(),  observatør.inntektsmeldingIkkeHåndtert)
    }

    @Test
    fun `sender ut signal om at inntektsmelding ikke er håndtert på nytt om ingen andre har håndtert den tidligere`() {
        val inntektsmelding = håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertEquals(listOf(inntektsmelding), observatør.inntektsmeldingIkkeHåndtert)
        håndterSøknad(Sykdom(20.februar, 20.mars, 100.prosent))
        assertEquals(listOf(inntektsmelding, inntektsmelding), observatør.inntektsmeldingIkkeHåndtert)
    }

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
        assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
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
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true,
                sykmeldingsperioder = listOf(28.januar til 28.februar)
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
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true,
                sykmeldingsperioder = listOf(1.januar til 16.januar, 17.januar til 31.januar)
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
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true,
                sykmeldingsperioder = listOf(1.februar til 28.februar)
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
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true,
                sykmeldingsperioder = listOf(1.januar til 31.januar, 28.januar til 28.februar)
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
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true,
                sykmeldingsperioder = listOf(1.januar til 31.januar, 10.februar til 28.februar)
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
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = true,
                sykmeldingsperioder = listOf(1.januar til 31.januar, 15.februar til 28.februar)
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
                harPeriodeInnenfor16Dager = false,
                trengerArbeidsgiveropplysninger = false,
                sykmeldingsperioder = listOf(11.januar til 16.januar, 10.januar til 15.januar)
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

    @Test
    fun `sender ut inntektsmelding ikke håndtert på im med funksjonelle feil ved revurdering av dager`() {
        nyttVedtak(1.januar, 31.januar)
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, harOpphørAvNaturalytelser = true)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertTrue(inntektsmeldingId in observatør.inntektsmeldingIkkeHåndtert)
    }
}
