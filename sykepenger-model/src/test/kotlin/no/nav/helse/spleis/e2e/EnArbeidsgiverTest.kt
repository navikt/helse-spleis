package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Utbetalingtype.REVURDERING
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class EnArbeidsgiverTest : AbstractDslTest() {

    @Test
    fun `Periode med AGP i snuten, etterfulgt av så mange arbeidsdager at det er ny AGP mot halen`() {
        a1 {
            håndterSøknad(25.juni til 5.juli)
            håndterSøknad(31.juli til 18.august)
            håndterInntektsmelding(
                listOf(25.juni til 5.juli, 8.juli til 12.juli),
                førsteFraværsdag = 1.august
            )

            assertEquals(6.juli til 18.august, inspektør.vedtaksperioder(2.vedtaksperiode).periode)
            assertEquals("ARG UUUU??? ??????? ??????? ?SSSSHH SSSSSHH SSSSSH", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals(listOf(31.juli til 15.august), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
            assertEquals(listOf(31.juli, 8.juli), inspektør.skjæringstidspunkter(2.vedtaksperiode))

            håndterInntektsmelding(
                listOf(25.juni til 5.juli, 8.juli til 12.juli),
                førsteFraværsdag = 7.august,
                begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering"
            )

            assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
            assertVarsler(listOf(Varselkode.RV_IM_3, Varselkode.RV_IM_25), 2.vedtaksperiode.filter())
            assertEquals("ARG UUUU??? ??????? ??????? ?SSSSHH SSSSSHH SSSSSH", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals(listOf(31.juli til 15.august), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
            assertEquals(31.juli, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(listOf(31.juli, 8.juli), inspektør.skjæringstidspunkter(2.vedtaksperiode))

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterOverstyrTidslinje((9.juli til 13.juli).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, 100) })
            val tidslinje = "ARG SSSSS?? ??????? ??????? ?SSSSHH SSSSSHH SSSSSH"
            assertEquals(tidslinje, inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            assertVarsler(listOf(Varselkode.RV_IV_11, Varselkode.RV_IM_3, Varselkode.RV_IM_25), 2.vedtaksperiode.filter())
            assertDoesNotThrow { håndterYtelser(2.vedtaksperiode) }
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)

            håndterOverstyrTidslinje((14.juli til 6.august).map { ManuellOverskrivingDag(it, Dagtype.ArbeidIkkeGjenopptattDag) })
            assertEquals("ARG SSSSSJJ JJJJJJJ JJJJJJJ JSSSSHH SSSSSHH SSSSSH", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals(listOf(25.juni til 5.juli, 8.juli til 12.juli), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
            assertEquals(31.juli, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(listOf(31.juli, 8.juli), inspektør.skjæringstidspunkter(2.vedtaksperiode))
            håndterYtelser(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `en sprø case som ikke lenger trekker masse penger uten at vedtaksperiodene får vite om det`() {
        a1 {
            nyttVedtak(5.desember(2017) til 5.januar)
            val korrelasjonsIdAugust2017 = inspektør.utbetaling(0).korrelasjonsId

            // Forlengelse med arbeid og ferie
            håndterSøknad(Sykdom(6.januar, 4.februar, 100.prosent), Arbeid(6.januar, 4.februar))
            håndterSøknad(Sykdom(5.februar, 24.februar, 100.prosent), Ferie(5.februar, 11.februar))
            håndterInntektsmelding(
                listOf(5.februar til 20.februar)
            )

            håndterPåminnelse(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, 1.januar.atStartOfDay(), 1.januar.plusDays(90).atStartOfDay())
            håndterSykepengegrunnlagForArbeidsgiver(2.vedtaksperiode, 6.januar, emptyList())
            assertVarsel(Varselkode.RV_IV_10, 2.vedtaksperiode.filter())
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(5.februar, inspektør.skjæringstidspunkt(3.vedtaksperiode))
            assertEquals(3, inspektør.antallUtbetalinger)
            assertEquals(5.desember(2017) til 5.januar, inspektør.utbetaling(0).periode)
            assertEquals(korrelasjonsIdAugust2017, inspektør.utbetaling(0).korrelasjonsId)
            val korrelasjonsIdFebruar2018 = inspektør.utbetaling(2).korrelasjonsId
            assertEquals(5.februar til 24.februar, inspektør.utbetaling(2).periode)
            assertNotEquals(korrelasjonsIdAugust2017, korrelasjonsIdFebruar2018)

            // Inntektsmelding som flytter arbeidsgiverperioden en uke frem
            // Utbetaling revurderes og skal trekke penger tilbake for 21.-23.februar
            håndterInntektsmelding(
                listOf(12.februar til 27.februar)
            )
            assertVarsel(Varselkode.RV_IV_7, 3.vedtaksperiode.filter())
            assertVarsel(RV_IM_24, 3.vedtaksperiode.filter())
            assertEquals(12.februar, inspektør.skjæringstidspunkt(3.vedtaksperiode))

            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

            assertEquals(4, inspektør.antallUtbetalinger)
            val utbetalingenSomTrekkerPenger = inspektør.utbetaling(3)
            assertEquals(REVURDERING, utbetalingenSomTrekkerPenger.type)
            assertEquals(korrelasjonsIdFebruar2018, utbetalingenSomTrekkerPenger.korrelasjonsId)
            assertEquals(5.februar til 24.februar, utbetalingenSomTrekkerPenger.periode)

            val opphørslinje = utbetalingenSomTrekkerPenger.arbeidsgiverOppdrag[0]
            assertEquals(21.februar, opphørslinje.inspektør.datoStatusFom)
            assertEquals("OPPH", opphørslinje.inspektør.statuskode)
            assertEquals(-4293, utbetalingenSomTrekkerPenger.nettobeløp)

            assertVarsel(Varselkode.RV_UT_23, 3.vedtaksperiode.filter())

            // Det kommer en forlengelse som skal lage en ny utbetaling som hekter seg på forrige utbetaling
            nullstillTilstandsendringer()
            håndterSøknad(25.februar til 15.mars)
            håndterYtelser(4.vedtaksperiode)

            assertEquals(5, inspektør.antallUtbetalinger)
            assertEquals(korrelasjonsIdFebruar2018, utbetalingenSomTrekkerPenger.korrelasjonsId)
            val nyUtbetaling = inspektør.utbetaling(4)
            assertEquals(1, nyUtbetaling.arbeidsgiverOppdrag.size)

            val utbetalingslinje = inspektør.utbetaling(4).arbeidsgiverOppdrag[0]
            assertEquals(28.februar, utbetalingslinje.inspektør.fom)
            assertEquals(15.mars, utbetalingslinje.inspektør.tom)

            // Utbetalingene er knyttet opp mot riktige vedtaksperioder
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertFalse(utbetalingenSomTrekkerPenger.utbetalingId in utbetalingIder(1.vedtaksperiode))
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
            assertTilstander(3.vedtaksperiode, AVSLUTTET)
            assertTrue(utbetalingenSomTrekkerPenger.utbetalingId in utbetalingIder(3.vedtaksperiode))
            assertTilstander(4.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
            assertFalse(utbetalingenSomTrekkerPenger.utbetalingId in utbetalingIder(4.vedtaksperiode))
        }
    }

    private fun utbetalingIder(vedtaksperiode: UUID) = inspektør.vedtaksperioder(vedtaksperiode).inspektør.behandlinger.flatMap { it.endringer.mapNotNull { endring -> endring.utbetaling?.inspektør?.utbetalingId } }

    @Test
    fun `Arbeid gjenopptatt i minst 16 dager fører til at vi bygger videre på feil utbetaling`()  {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(Sykdom(1.februar, 18.februar, 100.prosent), Arbeid(3.februar, 18.februar))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            håndterSøknad(19.februar til 19.mars)
            håndterArbeidsgiveropplysninger(
                listOf(19.februar til 6.mars),
                vedtaksperiodeId = 3.vedtaksperiode
            )
            assertVarsel(RV_IM_24, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)

            assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
            assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
            assertEquals(listOf(19.februar til 6.mars), inspektør.arbeidsgiverperiode(3.vedtaksperiode))

            assertEquals(4, inspektør.antallUtbetalinger)
            val januar = inspektør.utbetaling(0)
            val mars = inspektør.sisteUtbetaling()
            assertNotEquals(januar.korrelasjonsId, mars.korrelasjonsId)
        }
    }

    @Test
    fun `drawio -- misc -- oppvarming`() {
        a1 {
            håndterSykmelding(januar)
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))

            håndterSøknad(januar)
            håndterSøknad(februar)

            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)

            utbetalPeriode(1.vedtaksperiode)

            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )

            utbetalPeriodeEtterVilkårsprøving(2.vedtaksperiode)

            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `Én arbeidsgiver - førstegangsbehandling`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `Forlengelse av en avsluttet periode går til AvventerHistorikk`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(februar)

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `Førstegangsbehandling går ikke videre dersom vi har en tidligere uferdig periode`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)

            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                vedtaksperiodeId = 2.vedtaksperiode
            )

            assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE
            )
        }
    }

    @Test
    fun `Førstegangsbehandling går videre etter at en tidligere uferdig periode er ferdig`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)

            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                vedtaksperiodeId = 2.vedtaksperiode
            )

            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `Kort periode går til AvsluttetUtenUtbetaling, pusher neste periode til AvventerHistorikk`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
            håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar))
            håndterSøknad(1.januar til 16.januar)

            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)

            nullstillTilstandsendringer()
            val im = håndterInntektsmelding(
                listOf(1.januar til 16.januar)
            )

            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTrue(im in observatør.inntektsmeldingFørSøknad.map { it.inntektsmeldingId })
            assertFalse(im in observatør.inntektsmeldingHåndtert.map(Pair<InntektsmeldingId, *>::first))

            nullstillTilstandsendringer()

            håndterSøknad(17.januar til 31.januar)
            assertTrue(im in observatør.inntektsmeldingHåndtert.map(Pair<InntektsmeldingId, *>::first))

            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `Inntektsmelding kommer før søknad - vi kommer oss videre til AvventerHistorikk pga replay`() {
        a1 {
            håndterSykmelding(januar)
            håndterInntektsmelding(
                listOf(1.januar til 16.januar)
            )
            håndterSøknad(januar)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING
            )
        }
    }

    @Test
    fun `Kort periode skal ikke blokkeres av mangelende søknad`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar))
            håndterSøknad(1.januar til 20.januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)

            assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `Går til AvventerInntektsmelding ved gap`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar))
            håndterSykmelding(Sykmeldingsperiode(25.januar, 17.februar))

            håndterSøknad(1.januar til 22.januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)

            håndterSøknad(25.januar til 17.februar)

            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `drawio -- Out of order`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar)
            )

            håndterSøknad(februar)
            assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            håndterSykmelding(januar)
            håndterSøknad(januar)
            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            utbetalPeriode(2.vedtaksperiode)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)

            utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `går tilbake til AvventerTidligereEllerOverlappende dersom vi får en tilbakedatert søknad før periode i AvventerGodkjenning`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterSykmelding(januar)
            håndterSøknad(januar)

            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `går tilbake til AvventerTidligereEllerOverlappende dersom vi får en tilbakedatert søknad før periode i AvventerHistorikk`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)

            håndterSykmelding(januar)
            håndterSøknad(januar)

            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `går tilbake til AvventerTidligereEllerOverlappende dersom vi får en tilbakedatert søknad før periode i AvventerVilkårsprøving`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)

            håndterSykmelding(januar)
            håndterSøknad(januar)

            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `går tilbake til AvventerTidligereEllerOverlappende dersom vi får en tilbakedatert søknad før periode i AvventerSimulering`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            håndterSykmelding(januar)
            håndterSøknad(januar)

            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `blir i AvventerInntektsmelding dersom vi får en out-of-order søknad forran`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)

            håndterSykmelding(januar)
            håndterSøknad(januar)
            assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `blir i AvventerTidligereEllerOverlappende dersom vi får en out-of-order søknad forran`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)

            håndterSykmelding(januar)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)

            håndterSøknad(januar)
            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `hopper ikke videre fra AvventerInntektsmelding dersom vi får en out-of-order søknad foran og IM kommer på den seneste vedtaksperioden`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)

            håndterSykmelding(januar)
            håndterSøknad(januar)

            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `To perioder med gap, den siste venter på at den første skal bli ferdig - dersom den første blir forkastet skal den siste perioden forkastes`() {
        a1 {
            håndterSykmelding(januar)
            håndterSykmelding(Sykmeldingsperiode(1.mai, 31.mai))

            håndterSøknad(januar)
            håndterSøknad(mai)

            håndterArbeidsgiveropplysninger(
                listOf(1.mai til 16.mai),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterPåminnelse(
                1.vedtaksperiode,
                tilstand = AVVENTER_INNTEKTSMELDING,
                tilstandsendringstidspunkt = 5.februar.atStartOfDay()
            )

            assertTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Kort periode med en tidligere kort periode som har lagret inntekt for første fraværsdag`() {
        a1 {
            /* skal ikke gå videre til AVVENTER_HISTORIKK siden perioden ikke går forbi AGP */
            håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar))
            håndterSykmelding(Sykmeldingsperiode(10.januar, 11.januar))

            håndterSøknad(1.januar til 2.januar)
            håndterInntektsmelding(
                listOf(1.januar til 2.januar, 10.januar til 23.januar)
            )
            håndterSøknad(10.januar til 11.januar)

            assertTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `Periode skal ha utbetaling grunnet inntektsmelding vi mottok før søknad`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(11.januar, 26.januar))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar)
            )
            håndterSøknad(11.januar til 26.januar)
            assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `Skal ikke forkaste periode som mottar to identiske søknader`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)

            håndterSykmelding(januar)
            håndterSøknad(januar)

            assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `Skal ikke forkaste periode som mottar to identiske søknader for forlengelse i AvventerTidligereEllerOverlappendePerioder`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(februar)
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(februar)

            assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `ikke opprett ny vedtaksperiode dersom vi tidligere har forkastet en i samme periode`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)

            håndterAnmodningOmForkasting(1.vedtaksperiode)

            håndterSykmelding(januar)
            håndterSøknad(januar)

            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
        }
    }

    @Test
    fun `Dersom alle perioder forkastes skal ingen av dem pokes videre fra gjenopptaBehandling`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(februar)
            håndterAnmodningOmForkasting(1.vedtaksperiode, force = true)

            assertForkastetPeriodeTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Infotrygdhistorikk fører til at en senere periode ikke trenger ny AGP - må vente på infotrygdhistorikk før vi bestemmer om vi skal til AUU`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            utbetalPeriode(1.vedtaksperiode)

            håndterUtbetalingshistorikkEtterInfotrygdendring(
                utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 1.februar, 10.februar))
            )
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 20.februar
            )
            håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar))
            håndterSøknad(20.februar til 28.februar)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `overlappende sykmeldinger - første bit skal kunne gå videre selv om vi ikke har mottatt søknad for halen`() {
        a1 {
            tilGodkjenning(januar)
            håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar))
            håndterSøknad(1.februar til 20.februar)
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `korrigert sykmelding skal ikke blokkere for behandlingen av den første`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterSykmelding(januar) // dup
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            assertEquals(emptyList<Periode>(), inspektør.sykmeldingsperioder())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `tilbakedatert sykmelding som ikke overlapper skal ikke hindre at tidligere periode kan gå videre`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(februar)
            håndterSykmelding(Sykmeldingsperiode(31.januar, 31.januar))
            håndterArbeidsgiveropplysninger(listOf(1.februar til 16.februar), vedtaksperiodeId = 1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    private fun utbetalPeriodeEtterVilkårsprøving(vedtaksperiode: UUID) {
        håndterYtelser(vedtaksperiode)
        håndterSimulering(vedtaksperiode)
        håndterUtbetalingsgodkjenning(vedtaksperiode)
        håndterUtbetalt()
    }

    private fun utbetalPeriode(vedtaksperiode: UUID) {
        håndterVilkårsgrunnlag(vedtaksperiode)
        utbetalPeriodeEtterVilkårsprøving(vedtaksperiode)
    }
}
