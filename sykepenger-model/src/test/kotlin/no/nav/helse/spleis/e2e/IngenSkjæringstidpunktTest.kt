package no.nav.helse.spleis.e2e

import java.util.*
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Utbetalingtype.REVURDERING
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class IngenSkjæringstidpunktTest : AbstractDslTest() {


    @Test
    fun `Bruker skatteinntekter ved kort gap til periode med kun ferie`() {
        a1 {
            nyttVedtak(januar)
            håndterSykmelding(Sykmeldingsperiode(10.februar, 28.februar))
            håndterSøknad(Sykdom(10.februar, 28.februar, 100.prosent), Ferie(10.februar, 28.februar))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertUtbetalingsbeløp(2.vedtaksperiode, 0, 0)
            assertEquals(2, observatør.avsluttetMedVedtakEventer.size)
        }
    }

    @Test
    fun `Bruker skatteinntekter ved forlengelse utenfor arbeidsgiverperioden dersom det kun er friskmelding`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(17.januar, 25.januar))
            håndterSøknad(Sykdom(17.januar, 25.januar, 100.prosent), Arbeid(17.januar, 25.januar))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `Bruker skatteinntekter ved forlengelse utenfor arbeidsgiverperioden dersom det kun er ferie og friskmelding`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Ferie(16.januar, 20.januar))
            håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar))
            håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent), Arbeid(21.januar, 25.januar))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `Bruker skatteinntekter ved forlengelse utenfor arbeidsgiverperioden dersom det kun er friskmelding - etter utbetaling`() {
        a1 {
            nyttVedtak(1.januar til 23.januar)
            håndterSykmelding(Sykmeldingsperiode(24.januar, 25.januar))
            håndterSøknad(Sykdom(24.januar, 25.januar, 100.prosent), Arbeid(24.januar, 25.januar))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
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
            håndterVilkårsgrunnlag(2.vedtaksperiode, skatteinntekt = 0.daglig)
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_SV_1, 2.vedtaksperiode.filter())
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
            håndterVilkårsgrunnlag(3.vedtaksperiode)
            assertVarsel(RV_IM_24, 2.vedtaksperiode.filter())
            assertVarsel(RV_IM_24, 3.vedtaksperiode.filter())
            assertEquals(12.februar, inspektør.skjæringstidspunkt(3.vedtaksperiode))

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


    @Test
    fun `bare ferie (sykdomsforlengelse) - etter tilbakevennende sykdom`() {
        a1 {
            nyttVedtak(januar)
            håndterSykmelding(Sykmeldingsperiode(5.februar, 23.februar))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 5.februar
            )
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(5.februar, 23.februar, 100.prosent), Ferie(5.februar, 23.februar))
            håndterSøknad(Sykdom(24.februar, 28.februar, 100.prosent))

            assertEquals(januar, inspektør.periode(1.vedtaksperiode))
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))

            assertEquals(5.februar til 23.februar, inspektør.periode(2.vedtaksperiode))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 5.februar, listOf(1.januar til 16.januar))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)

            assertEquals(24.februar til 28.februar, inspektør.periode(3.vedtaksperiode))
            assertSkjæringstidspunktOgVenteperiode(3.vedtaksperiode, 24.februar, listOf(24.februar til 28.februar))
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `periode med ferie kant-i-kant med en periode med utbetalingsdag`() {
        a1 {
            nyttVedtak(januar)
            håndterSykmelding(Sykmeldingsperiode(5.februar, 23.februar))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 5.februar
            )
            håndterSykmelding(Sykmeldingsperiode(24.februar, 12.mars))
            håndterSøknad(Sykdom(5.februar, 23.februar, 100.prosent), Ferie(5.februar, 23.februar))
            håndterSøknad(Sykdom(24.februar, 12.mars, 100.prosent))

            assertEquals(januar, inspektør.periode(1.vedtaksperiode))
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))

            assertEquals(5.februar til 23.februar, inspektør.periode(2.vedtaksperiode))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 5.februar, listOf(1.januar til 16.januar))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)

            assertEquals(24.februar til 12.mars, inspektør.periode(3.vedtaksperiode))
            assertSkjæringstidspunktOgVenteperiode(3.vedtaksperiode, 24.februar, listOf(24.februar til 11.mars))

            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `bare ferie (forlengelse) - etter tilbakevennende sykdom`() {
        a1 {
            nyttVedtak(januar)
            håndterSykmelding(Sykmeldingsperiode(5.februar, 23.februar))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 5.februar
            )
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(5.februar, 23.februar, 100.prosent), Ferie(5.februar, 23.februar))
            håndterSøknad(Sykdom(24.februar, 28.februar, 100.prosent), Ferie(24.februar, 28.februar))

            assertEquals(januar, inspektør.periode(1.vedtaksperiode))
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))

            assertEquals(5.februar til 23.februar, inspektør.periode(2.vedtaksperiode))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 5.februar, listOf(1.januar til 16.januar))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)

            assertEquals(24.februar til 28.februar, inspektør.periode(3.vedtaksperiode))
            assertSkjæringstidspunktOgVenteperiode(3.vedtaksperiode, 24.februar, emptyList())
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `ferie med gap til forrige, men samme skjæringstidspunkt`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar), orgnummer = a2)
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(5.februar, 20.februar))
            håndterSøknad(Sykdom(5.februar, 20.februar, 100.prosent), Ferie(5.februar, 20.februar))

            nullstillTilstandsendringer()
            assertVarsler(listOf(Varselkode.RV_VV_2), 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent))
            håndterArbeidsgiveropplysninger(vedtaksperiodeId = 1.vedtaksperiode, Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT, emptyList()), Arbeidsgiveropplysning.OppgittArbeidgiverperiode(listOf(1.februar til 16.februar)))

            assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
        }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
    }

    private fun utbetalingIder(vedtaksperiode: UUID) = inspektør.vedtaksperioder(vedtaksperiode).inspektør.behandlinger.flatMap { it.endringer.mapNotNull { endring -> endring.utbetaling?.inspektør?.utbetalingId } }

}

