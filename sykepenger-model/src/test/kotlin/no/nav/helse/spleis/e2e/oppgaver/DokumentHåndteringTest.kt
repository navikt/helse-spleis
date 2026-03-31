package no.nav.helse.spleis.e2e.oppgaver

import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_13
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DokumentHåndteringTest : AbstractDslTest() {

    @Test
    fun `sender ut inntektsmelding håndtert også når inntektsmelding kommer før søknad og dagene håndteres av en tidligere periode`() {
        a1 {
            nyttVedtak(januar)
            observatør.inntektsmeldingHåndtert.clear()
            val inntektsmelding = håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 10.februar
            )
            val søknadId = UUID.randomUUID()
            håndterSøknad(Sykdom(10.februar, 28.februar, 100.prosent), søknadId = søknadId)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            assertEquals(inntektsmelding to 2.vedtaksperiode, observatør.inntektsmeldingHåndtert.single())
            assertEquals(setOf(søknadId, inntektsmelding), inspektør.hendelseIder(2.vedtaksperiode))
        }
    }

    @Test
    fun `Inntektsmelding kommer mellom AUU og søknad for førstegangsbehandling`() {
        a1 {
            val søknadId1 = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), søknadId = søknadId1)
            val inntektsmelding = håndterInntektsmelding(listOf(1.januar til 16.januar))
            val søknadId2 = UUID.randomUUID()
            håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), søknadId = søknadId2)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            assertEquals(setOf(søknadId1, inntektsmelding), inspektør.hendelseIder(1.vedtaksperiode))
            assertEquals(setOf(søknadId2, inntektsmelding), inspektør.hendelseIder(2.vedtaksperiode))
            assertEquals(2.vedtaksperiode, observatør.inntektsmeldingHåndtert.single().second)
        }
    }

    @Test
    fun `Inntektsmelding kommer mellom AUU og søknad for førstegangsbehandling flere arbeidsgivere`() {
        val søknadId1A1 = UUID.randomUUID()
        val søknadId1A2 = UUID.randomUUID()
        a1 { håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), søknadId = søknadId1A1) }
        a2 { håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), søknadId = søknadId1A2) }

        val inntektsmeldingA1 = a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        val inntektsmeldingA2 = a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }

        val søknadId2A1 = UUID.randomUUID()
        val søknadId2A2 = UUID.randomUUID()
        a1 { håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), søknadId = søknadId2A1) }
        a2 { håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), søknadId = søknadId2A2) }

        a1 { håndterVilkårsgrunnlag(2.vedtaksperiode) }

        a1 {
            assertEquals(setOf(søknadId1A1, inntektsmeldingA1), inspektør.hendelseIder(1.vedtaksperiode))
            assertEquals(setOf(søknadId2A1, inntektsmeldingA1), inspektør.hendelseIder(2.vedtaksperiode))
        }
        a2 {
            assertEquals(setOf(søknadId1A2, inntektsmeldingA2), inspektør.hendelseIder(1.vedtaksperiode))
            assertEquals(setOf(søknadId2A2, inntektsmeldingA2), inspektør.hendelseIder(2.vedtaksperiode))
        }
    }

    @Test
    fun `to helt like korrigerende inntektsmeldinger`() {
        a1 {
            nyttVedtak(januar)
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT * 1.1
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()

            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
            observatør.inntektsmeldingIkkeHåndtert.clear()
            observatør.inntektsmeldingHåndtert.clear()
            val korrigertInntektsmelding2 = håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT * 1.1
            )
            assertEquals(listOf(korrigertInntektsmelding2), observatør.inntektsmeldingHåndtert.map { it.first })
            assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
        }
    }

    @Test
    fun `sender ikke ut signal om at inntektsmelding ikke er håndtert om annen vedtaksperiode har håndtert inntektsmelding før`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(Sykdom(20.februar, 20.mars, 100.prosent))
            assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
        }
    }

    @Test
    fun `Inntektsmelding før søknad`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            val id = håndterInntektsmelding(listOf(1.januar til 16.januar))
            val inntektsmeldingFørSøknadEvent = observatør.inntektsmeldingFørSøknad.single()
            assertEquals(id, inntektsmeldingFørSøknadEvent.inntektsmeldingId)
        }
    }

    @Test
    fun `Inntektsmelding før søknad med kort gap`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(20.januar, 31.januar))
            val id = håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 20.januar
            )
            val inntektsmeldingFørSøknadEvent = observatør.inntektsmeldingFørSøknad.single()
            assertEquals(id, inntektsmeldingFørSøknadEvent.inntektsmeldingId)
        }
    }

    @Test
    fun `Inntektsmelding før søknad, men vedtaksperioden er forkastet`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), utenlandskSykmelding = true)
            val id = håndterInntektsmelding(listOf(1.januar til 16.januar))
            val inntektsmelding = observatør.inntektsmeldingIkkeHåndtert.single()
            assertEquals(id, inntektsmelding)
        }
    }

    @Test
    fun `Inntektsmelding før forlengelse-søknad`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar))
            val id = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)
            val inntektsmeldingFørSøknadEvent = observatør.inntektsmeldingFørSøknad.single()
            assertEquals(id, inntektsmeldingFørSøknadEvent.inntektsmeldingId)
        }
    }

    @Test
    fun `Inntektsmelding før forlengelse-søknad - auu er litt lang`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
            val søknadId = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(17.januar, 31.januar), søknadId = søknadId)
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            val id = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)

            assertEquals(id to 1.vedtaksperiode, observatør.inntektsmeldingHåndtert.single())
            assertEquals(setOf(søknadId, id), inspektør.hendelseIder(1.vedtaksperiode))
            assertEquals(0, observatør.inntektsmeldingFørSøknad.size)
        }
    }

    @Test
    fun `Inntektsmelding ikke håndtert`() {
        a1 {
            val id = håndterInntektsmelding(listOf(1.januar til 16.januar))
            val inntektsmelding = observatør.inntektsmeldingIkkeHåndtert.single()
            assertEquals(id, inntektsmelding)
        }
    }

    @Test
    fun `Inntektsmelding ikke håndtert - lang periode mellom auu og sykmelding`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            val id = håndterInntektsmelding(listOf(1.januar til 16.januar))
            val inntektsmelding = observatør.inntektsmeldingIkkeHåndtert.single()
            assertEquals(id, inntektsmelding)
        }
    }

    @Test
    fun `Inntektsmelding bare håndtert inntekt`() {
        a1 {
            håndterSøknad(januar)
            val im1 = håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val hendelserHåndtertFør = inspektør.hendelser(1.vedtaksperiode)
            assertEquals(
                listOf(im1 to 1.vedtaksperiode),
                observatør.inntektsmeldingHåndtert
            )
            assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
            val søknadId = UUID.randomUUID()
            håndterSøknad(Sykdom(10.februar, 28.februar, 100.prosent), søknadId = søknadId)
            val søknad = MeldingsreferanseId(søknadId)
            val im = MeldingsreferanseId(håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 10.februar
            ))
            assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
            assertEquals(hendelserHåndtertFør, inspektør.hendelser(1.vedtaksperiode))
            assertEquals(
                setOf(
                    Dokumentsporing.søknad(søknad),
                    Dokumentsporing.inntektsmeldingDager(im),
                    Dokumentsporing.inntektsmeldingRefusjon(im),
                    Dokumentsporing.inntektsmeldingInntekt(im)
                ), inspektør.hendelser(2.vedtaksperiode)
            )
            assertEquals(2, observatør.inntektsmeldingHåndtert.size)
            assertEquals(im.id to 2.vedtaksperiode, observatør.inntektsmeldingHåndtert.last())
        }
    }

    @Test
    fun `lps-Inntektsmelding noen dager håndtert`() {
        a1 {
            val søknadId = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent), søknadId = søknadId)
            val im = håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertEquals(listOf(im), observatør.inntektsmeldingIkkeHåndtert)
            assertEquals(listOf(søknadId to 1.vedtaksperiode), observatør.søknadHåndtert)
            assertEquals(emptyList<Any>(), observatør.inntektsmeldingHåndtert)
        }
    }

    @Test
    fun `portal-Inntektsmelding noen dager håndtert`() {
        a1 {
            val søknadId = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent), søknadId = søknadId)
            val im = håndterArbeidsgiveropplysninger(listOf(4.januar til 19.januar))
            assertEquals(emptyList<Any>(), observatør.inntektsmeldingIkkeHåndtert)
            assertEquals(listOf(søknadId to 1.vedtaksperiode), observatør.søknadHåndtert)
            assertEquals(listOf(im to 1.vedtaksperiode), observatør.inntektsmeldingHåndtert)
        }
    }

    @Test
    fun `Inntektsmelding noen dager håndtert - IM før søknad`() {
        a1 {
            val søknadId = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent), søknadId = søknadId)
            håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar))
            val im = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)
            assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
            assertEquals(listOf(im), observatør.inntektsmeldingFørSøknad.map { it.inntektsmeldingId })
            assertEquals(listOf(søknadId to 1.vedtaksperiode), observatør.søknadHåndtert)
            assertEquals(emptyList<Any>(), observatør.inntektsmeldingHåndtert)
        }
    }

    @Test
    fun `Inntektsmelding håndteres av flere`() {
        a1 {
            val søknadId1 = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent), søknadId = søknadId1)
            val søknad1 = MeldingsreferanseId(søknadId1)
            val søknadId2 = UUID.randomUUID()
            håndterSøknad(Sykdom(11.januar, 16.januar, 100.prosent), søknadId = søknadId2)
            val søknad2 = MeldingsreferanseId(søknadId2)
            val søknadId3 = UUID.randomUUID()
            håndterSøknad(Sykdom(17.januar, 20.januar, 100.prosent), søknadId = søknadId3)
            val søknad3 = MeldingsreferanseId(søknadId3)
            val søknadId4 = UUID.randomUUID()
            håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent), søknadId = søknadId4)
            val søknad4 = MeldingsreferanseId(søknadId4)
            val im = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar)))

            assertEquals(
                setOf(
                    Dokumentsporing.søknad(søknad1),
                    Dokumentsporing.inntektsmeldingDager(im),
                    Dokumentsporing.inntektsmeldingRefusjon(im),
                ), inspektør.hendelser(1.vedtaksperiode)
            )
            assertEquals(
                setOf(
                    Dokumentsporing.søknad(søknad2),
                    Dokumentsporing.inntektsmeldingDager(im),
                    Dokumentsporing.inntektsmeldingRefusjon(im)
                ), inspektør.hendelser(2.vedtaksperiode)
            )
            assertEquals(
                setOf(
                    Dokumentsporing.søknad(søknad3),
                    Dokumentsporing.inntektsmeldingRefusjon(im),
                    Dokumentsporing.inntektsmeldingInntekt(im)
                ), inspektør.hendelser(3.vedtaksperiode)
            )
            assertEquals(
                setOf(
                    Dokumentsporing.søknad(søknad4),
                    Dokumentsporing.inntektsmeldingRefusjon(im),
                    Dokumentsporing.inntektsmeldingInntekt(im)
                ), inspektør.hendelser(4.vedtaksperiode)
            )

            assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
            assertEquals(
                listOf(
                    søknadId1 to 1.vedtaksperiode,
                    søknadId2 to 2.vedtaksperiode,
                    søknadId3 to 3.vedtaksperiode,
                    søknadId4 to 4.vedtaksperiode
                ), observatør.søknadHåndtert
            )
            assertEquals(
                listOf(im.id to 3.vedtaksperiode),
                observatør.inntektsmeldingHåndtert
            )
        }
    }

    @Test
    fun `har overlappende avslutta vedtaksperiode på annen arbeidsgiver`() {
        a2 { nyttVedtak(januar) }
        val søknad2Id = UUID.randomUUID()
        a1 { håndterSøknad(Sykdom(28.januar, 28.februar, 100.prosent), utenlandskSykmelding = true, søknadId = søknad2Id) }
        val vp1A1 = a1 { 1.vedtaksperiode }
        assertEquals(
            EventSubscription.VedtaksperiodeForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = vp1A1,
                gjeldendeTilstand = START,
                hendelser = setOf(søknad2Id),
                fom = 28.januar,
                tom = 28.februar,
                sykmeldingsperioder = listOf(28.januar til 28.februar),
                speilrelatert = true
            ), observatør.forkastet(vp1A1)
        )
    }

    @Test
    fun `har periode rett før men det er en AUU`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            val søknad2Id = UUID.randomUUID()
            håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), søknadId = søknad2Id)
            val im = håndterInntektsmelding(
                listOf(10.januar til 25.januar),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "FiskerMedHyre"
            )
            assertFunksjonellFeil(RV_IM_8, AktivitetsloggFilter.Alle)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
            val vp2 = 2.vedtaksperiode
            assertEquals(
                EventSubscription.VedtaksperiodeForkastetEvent(
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    vedtaksperiodeId = vp2,
                    gjeldendeTilstand = AVVENTER_INNTEKTSMELDING,
                    hendelser = setOf(søknad2Id),
                    fom = 17.januar,
                    tom = 31.januar,
                    sykmeldingsperioder = emptyList(),
                    speilrelatert = false
                ), observatør.forkastet(vp2)
            )
            assertTrue(im in observatør.inntektsmeldingIkkeHåndtert)
        }
    }

    @Test
    fun `har en periode rett før på annen arbeidsgiver`() {
        a2 { nyttVedtak(januar) }
        val søknad2Id = UUID.randomUUID()
        a1 { håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), utenlandskSykmelding = true, søknadId = søknad2Id) }
        val vp1A1 = a1 { 1.vedtaksperiode }
        assertEquals(
            EventSubscription.VedtaksperiodeForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = vp1A1,
                gjeldendeTilstand = START,
                hendelser = setOf(søknad2Id),
                fom = 1.februar,
                tom = 28.februar,
                sykmeldingsperioder = listOf(februar),
                speilrelatert = true
            ), observatør.forkastet(vp1A1)
        )
    }

    @Test
    fun `har ikke overlappende vedtaksperioder`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            val søknad2Id = UUID.randomUUID()
            håndterSøknad(Sykdom(28.januar, 28.februar, 100.prosent), søknadId = søknad2Id)
            val vp2 = 2.vedtaksperiode
            assertEquals(
                EventSubscription.VedtaksperiodeForkastetEvent(
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    vedtaksperiodeId = vp2,
                    gjeldendeTilstand = START,
                    hendelser = setOf(søknad2Id),
                    fom = 28.januar,
                    tom = 28.februar,
                    sykmeldingsperioder = emptyList(),
                    speilrelatert = false
                ), observatør.forkastet(vp2)
            )
        }
    }

    @Test
    fun `har vedtaksperiode som påvirker arbeidsgiverperioden`() {
        a1 {
            tilGodkjenning(januar)
            val søknad2Id = UUID.randomUUID()
            håndterSøknad(Sykdom(10.februar, 28.februar, 100.prosent), søknadId = søknad2Id)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)

            val vp2 = 2.vedtaksperiode
            assertEquals(
                EventSubscription.VedtaksperiodeForkastetEvent(
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    vedtaksperiodeId = vp2,
                    gjeldendeTilstand = AVVENTER_INNTEKTSMELDING,
                    hendelser = setOf(søknad2Id),
                    fom = 10.februar,
                    tom = 28.februar,
                    sykmeldingsperioder = emptyList(),
                    speilrelatert = false
                ), observatør.forkastet(vp2)
            )
        }
    }

    @Test
    fun `har ikke overlappende vedtaksperiode`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            val søknad2Id = UUID.randomUUID()
            håndterSøknad(Sykdom(15.februar, 28.februar, 100.prosent), søknadId = søknad2Id)
            val vp2 = 2.vedtaksperiode
            assertEquals(
                EventSubscription.VedtaksperiodeForkastetEvent(
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    vedtaksperiodeId = vp2,
                    gjeldendeTilstand = START,
                    hendelser = setOf(søknad2Id),
                    fom = 15.februar,
                    tom = 28.februar,
                    sykmeldingsperioder = listOf(januar, 15.februar til 28.februar),
                    speilrelatert = false
                ), observatør.forkastet(vp2)
            )
        }
    }

    @Test
    fun `delvis overlappende søknad`() {
        a1 {
            val søknadId1 = UUID.randomUUID()
            håndterSøknad(Sykdom(11.januar, 16.januar, 100.prosent), søknadId = søknadId1)
            nullstillTilstandsendringer()
            val søknadId2 = UUID.randomUUID()
            håndterSøknad(Sykdom(10.januar, 15.januar, 100.prosent), søknadId = søknadId2)
            assertEquals(emptyList<UUID>(), observatør.inntektsmeldingIkkeHåndtert)
            assertEquals(
                listOf(
                    søknadId1 to 1.vedtaksperiode,
                    søknadId2 to 1.vedtaksperiode
                ), observatør.søknadHåndtert
            )
            assertVarsler(listOf(RV_SØ_13), 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
            assertEquals(10.januar til 16.januar, inspektør.periode(1.vedtaksperiode))
            val vp2 = 2.vedtaksperiode
            assertEquals(
                EventSubscription.VedtaksperiodeForkastetEvent(
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    vedtaksperiodeId = vp2,
                    gjeldendeTilstand = START,
                    hendelser = setOf(søknadId2),
                    fom = 10.januar,
                    tom = 15.januar,
                    sykmeldingsperioder = emptyList(),
                    speilrelatert = false
                ), observatør.forkastet(vp2)
            )
        }
    }

    @Test
    fun `sender ut søknad håndtert for forlengelse av forkastet periode`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            val søknadId1 = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), søknadId = søknadId1)
            håndterAnmodningOmForkasting(1.vedtaksperiode)

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            val søknadId2 = UUID.randomUUID()
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), søknadId = søknadId2)
            assertEquals(
                listOf(
                    søknadId1 to 1.vedtaksperiode,
                    søknadId2 to 2.vedtaksperiode
                ), observatør.søknadHåndtert
            )
        }
    }

    @Test
    fun `sender ut inntektsmelding ikke håndtert på im med funksjonelle feil ved revurdering av dager`() {
        a1 {
            nyttVedtak(januar)
            val inntektsmeldingId = håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                opphørAvNaturalytelser = listOf(Inntektsmelding.OpphørAvNaturalytelse(1000.månedlig, 1.januar, "BIL"))
            )

            assertVarsel(RV_IM_7, 1.vedtaksperiode.filter())

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertFalse(inntektsmeldingId in observatør.inntektsmeldingIkkeHåndtert)
            assertTrue(inntektsmeldingId in observatør.inntektsmeldingHåndtert.map { it.first })
        }
    }

    @Test
    fun `inntektsmelding med første fraværsdag utenfor sykdom - ett tidligere vedtak - inntektsmelding ikke håndtert fordi inntekt håndteres ikke`() {
        a1 {
            nyttVedtak(januar)
            val im2 = håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 1.februar,
                refusjon = Inntektsmelding.Refusjon(Inntekt.INGEN, null)
            )
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTrue(im2 in observatør.inntektsmeldingHåndtert.map(Pair<UUID, *>::first))
            assertTrue(im2 in observatør.inntektsmeldingIkkeHåndtert)
        }
    }

    @Test
    fun `inntektsmelding med første fraværsdag utenfor sykdom - ingen tidligere vedtak - inntektsmelding ikke håndtert fordi inntekt håndteres ikke`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
            håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
            val im = håndterInntektsmelding(
                listOf(Periode(3.januar, 18.januar)),
                førsteFraværsdag = 27.januar
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
            assertFalse(im in observatør.inntektsmeldingHåndtert.map(Pair<UUID, *>::first))
            assertTrue(im in observatør.inntektsmeldingIkkeHåndtert)
        }
    }

    @Test
    fun `inntektsmelding med første fraværsdag utenfor sykdom - ingen tidligere vedtak - IM før søknad - inntektsmelding ikke håndtert fordi inntekt håndteres ikke`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
            håndterInntektsmelding(
                listOf(Periode(3.januar, 18.januar)),
                førsteFraværsdag = 27.januar
            )
            håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `Skal legge til hendelsesid for korrigerende inntektsmelding på alle vedtaksperioder den treffer`() {
        a1 {
            nyPeriode(1.januar til 10.januar)
            val im1 = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar)))
            nyPeriode(11.januar til 31.januar)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            val im2 = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 1.1))

            assertVarsel(Varselkode.RV_IM_4, 2.vedtaksperiode.filter())

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

            assertFalse(inspektør.hendelser(1.vedtaksperiode).contains(Dokumentsporing.inntektsmeldingInntekt(im1)))
            assertTrue(inspektør.hendelser(2.vedtaksperiode).contains(Dokumentsporing.inntektsmeldingInntekt(im1)))
            assertFalse(inspektør.hendelser(1.vedtaksperiode).contains(Dokumentsporing.inntektsmeldingInntekt(im2)))
            assertTrue(inspektør.hendelser(2.vedtaksperiode).contains(Dokumentsporing.inntektsmeldingInntekt(im2)))
        }
    }
}
