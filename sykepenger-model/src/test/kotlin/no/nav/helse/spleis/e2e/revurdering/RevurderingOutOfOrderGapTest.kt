package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.TilstandType.UTBETALING_FEILET
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Mottatt søknad out of order`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Mottatt søknad out of order innenfor 18 dager`
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OO_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OO_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_13
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertInfo
import no.nav.helse.spleis.e2e.assertIngenInfo
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertSisteForkastetPeriodeTilstand
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertUtbetalingsbeløp
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.forlengTilGodkjenning
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.forlengelseTilGodkjenning
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterPåminnelse
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.spleis.e2e.tilGodkjent
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.utbetalingslinjer.Endringskode.NY
import no.nav.helse.utbetalingslinjer.Endringskode.UEND
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class RevurderingOutOfOrderGapTest : AbstractEndToEndTest() {

    @Test
    fun `out of order med nyere periode til godkjenning revurdering`() {
        nyttVedtak(1.mars, 31.mars)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(30.mars, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        nullstillTilstandsendringer()
        nyPeriode(1.januar til 31.januar)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
    }

    @Test
    fun `out of order periode i helg mellom to andre perioder`() {
        nyttVedtak(1.januar, 26.januar)
        forlengVedtak(29.januar, 11.februar)
        nullstillTilstandsendringer()
        nyPeriode(27.januar til 28.januar)

        assertForventetFeil(
            nå = {
                assertFunksjonellFeil(`Mottatt søknad out of order`, 2.vedtaksperiode.filter())
                assertSisteForkastetPeriodeTilstand(ORGNUMMER, 3.vedtaksperiode, TIL_INFOTRYGD)
            },
            ønsket = {
                håndterYtelser(3.vedtaksperiode)

                val andreUtbetaling = inspektør.utbetaling(1).inspektør
                val outOfOrderUtbetaling = inspektør.utbetaling(2).inspektør

                assertEquals(andreUtbetaling.korrelasjonsId, outOfOrderUtbetaling.korrelasjonsId)
                val arbeidsgiverOppdrag = outOfOrderUtbetaling.arbeidsgiverOppdrag
                assertEquals(1, arbeidsgiverOppdrag.size)
                arbeidsgiverOppdrag[0].inspektør.also { linje ->
                    assertEquals(NY, linje.endringskode)
                    assertEquals(17.januar, linje.fom)
                    assertEquals(26.januar, linje.tom)
                    assertEquals(3, linje.delytelseId)
                    assertEquals(2, linje.refDelytelseId)
                    assertNull(linje.datoStatusFom)
                }

                assertTilstander(1.vedtaksperiode, AVSLUTTET)
                assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
                assertTilstander(3.vedtaksperiode, START, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)

            }
        )
    }

    @Test
    fun `hører til samme arbeidsgiverperiode som forrige - har en fremtidig utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.april, 30.april)

        nyPeriode(10.februar til 28.februar)
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar), 10.februar)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_GODKJENNING)

        val utbetaling1 = inspektør.utbetaling(0).inspektør

        inspektør.utbetaling(2).inspektør.also { inspektør ->
            assertEquals(inspektør.korrelasjonsId, utbetaling1.korrelasjonsId)
            assertEquals(inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), utbetaling1.arbeidsgiverOppdrag.inspektør.fagsystemId())
            assertEquals(2, inspektør.arbeidsgiverOppdrag.size)
            assertEquals(UEND, inspektør.arbeidsgiverOppdrag[0].inspektør.endringskode)
            assertEquals(17.januar, inspektør.arbeidsgiverOppdrag[0].inspektør.fom)
            assertEquals(31.januar, inspektør.arbeidsgiverOppdrag[0].inspektør.tom)
            assertEquals(NY, inspektør.arbeidsgiverOppdrag[1].inspektør.endringskode)
            assertEquals(10.februar, inspektør.arbeidsgiverOppdrag[1].inspektør.fom)
            assertEquals(28.februar, inspektør.arbeidsgiverOppdrag[1].inspektør.tom)
        }
    }

    @Test
    fun `out-of-order søknad medfører revurdering -- AvventerVilksprøvingRevurdering`() {
        nyPeriode(1.mars til 10.mars)
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterInntektsmelding(arbeidsgiverperioder = listOf(20.februar til 7.mars), førsteFraværsdag = 20.februar)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

        nyPeriode(1.januar til 18.januar)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertUtbetalingsbeløp(
            1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 5.februar til 10.februar
        )
    }

    @Test
    fun `out of order periode med kort gap - utbetalingen på revurderingen får korrekt beløp`() {
        nyttVedtak(1.februar, 28.februar)
        assertForventetFeil(
            forklaring = "Betaler for januar dobbelt opp fordi også revurderingen hensyntar denne perioden",
            nå = {
                nyPeriode(1.januar til 25.januar)
                assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
            },
            ønsket = {
                nyttVedtak(1.januar, 25.januar)
                håndterYtelser(1.vedtaksperiode)

                assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
                assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

                assertEquals(3, inspektør.utbetalinger.size)
                val nettoBeløpForFebruarMedStandardInntektOgAgp = inspektør.utbetalinger.first().inspektør.nettobeløp
                val antallSykedagerIFebruar2018 = 20
                val nettoBeløpForFebruarMedStandardInntektUtenAgp = 1431 * antallSykedagerIFebruar2018
                val revurdering = inspektør.utbetalinger.last()
                assertEquals(nettoBeløpForFebruarMedStandardInntektUtenAgp -nettoBeløpForFebruarMedStandardInntektOgAgp, revurdering.inspektør.nettobeløp)
            }
        )
    }

    @Test
    fun `out of order periode med langt gap - utbetalingen på revurderingen får korrekt beløp`() {
        nyttVedtak(1.mars, 31.mars)
        nyttVedtak(1.januar, 25.januar)
        håndterYtelser(1.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        assertEquals(3, inspektør.utbetalinger.size)
        val revurdering = inspektør.utbetalinger.last()
        assertEquals(0, revurdering.inspektør.nettobeløp)
    }

    @Test
    fun `out of order periode med 18 dagers gap - revurderingen er uten endringer`() {
        nyttVedtak(19.februar, 15.mars)
        nyttVedtak(1.januar, 31.januar)
        håndterYtelser(1.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        assertEquals(3, inspektør.utbetalinger.size)
        val førsteUtbetaling = inspektør.utbetaling(0).inspektør
        val andreUtbetaling = inspektør.utbetaling(1).inspektør
        val revurdering = inspektør.utbetaling(2).inspektør

        assertNotEquals(førsteUtbetaling.korrelasjonsId, andreUtbetaling.korrelasjonsId)
        assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
        assertEquals(0, revurdering.nettobeløp)
        assertEquals(UEND, revurdering.arbeidsgiverOppdrag.inspektør.endringskode)
    }
    @Test
    fun `out of order periode med 15 dagers gap - støttes ikke`() {
        nyttVedtak(16.februar, 15.mars)
        nyttVedtak(16.april, 15.mai)
        nyPeriode(1.januar til 31.januar)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)
        assertFunksjonellFeil(`Mottatt søknad out of order innenfor 18 dager`)
    }

    @Test
    fun `out of order periode rett før - påvirker arbeidsgiverperioden - støttes ikke`() {
        nyttVedtak(29.januar, 28.februar)
        nyPeriode(1.januar til 28.januar)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        assertFunksjonellFeil(`Mottatt søknad out of order`)
    }

    @Test
    fun `out of order periode med 15 dagers gap - mellom to perioder`() {
        nyPeriode(1.januar til 15.januar)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyttVedtak(29.januar, 15.februar)
        nyPeriode(17.januar til 25.januar)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)
        assertFunksjonellFeil(`Mottatt søknad out of order innenfor 18 dager`)
    }

    @Test
    fun `out of order periode rett før - mellom to perioder - arbeidsgiverperioden slutter tidligere`() {
        nyPeriode(1.januar til 15.januar)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyPeriode(29.januar til 15.februar)
        håndterInntektsmelding(listOf(1.januar til 15.januar, 29.januar til 29.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        nyPeriode(17.januar til 28.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertFunksjonellFeil(`Mottatt søknad out of order`, 2.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 3.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `out of order periode rett før - mellom to perioder - arbeidsgiverperioden var ferdig`() {
        nyPeriode(1.januar til 16.januar)
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        nyPeriode(29.januar til 15.februar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 29.januar)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        nyPeriode(17.januar til 28.januar)

        assertFunksjonellFeil(`Mottatt søknad out of order`, 2.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 3.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `out of order periode trigger revurdering`() {
        nyttVedtak(1.mai, 31.mai)
        forlengVedtak(1.juni, 30.juni)
        nullstillTilstandsendringer()
        nyttVedtak(1.januar, 31.januar)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )
    }

    @Test
    fun `out of order periode uten utbetaling trigger ikke revurdering`() {
        nyttVedtak(1.mai, 31.mai)
        forlengVedtak(1.juni, 30.juni)
        nullstillTilstandsendringer()
        nyPeriode(1.januar til 15.januar)
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET
        )
    }

    @Test
    fun `out of order periode uten utbetaling trigger ikke revurdering -- flere ag`() {
        nyeVedtak(1.mai, 31.mai, a1, a2)
        nullstillTilstandsendringer()
        nyPeriode(1.januar til 15.januar, a1)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            orgnummer = a1
        )
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            orgnummer = a1
        )
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            orgnummer = a2
        )
    }

    @Test
    fun `Burde revurdere utbetalt periode dersom det kommer en eldre periode fra en annen AG`() {
        nyttVedtak(1.mars, 31.mars, orgnummer = a2)
        nyPeriode(1.januar til 31.januar, a1)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a1)
    }

    @Test
    fun `out of order som overlapper med eksisterende -- flere ag`() {
        nyttVedtak(1.mars, 31.mars, orgnummer = a2)
        nyPeriode(20.februar til 15.mars, a1)
        håndterInntektsmelding(listOf(20.februar til 7.mars), orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.februar(2017) til 1.januar(2018) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.november(2017) til 1.januar(2018) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }, arbeidsforhold = listOf()),
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, 1.januar(2017)),
                Arbeidsforhold(a2, 1.januar(2017))
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a1Utbetaling = inspektør(a1).utbetaling(0).inspektør
        val a2FørsteUtbetaling = inspektør(a2).utbetaling(0).inspektør
        val a2AndreUtbetaling = inspektør(a2).utbetaling(2).inspektør

        assertEquals(a1Utbetaling.tilstand, Utbetaling.Utbetalt)
        assertEquals(a2FørsteUtbetaling.tilstand, Utbetaling.Utbetalt)
        assertEquals(a2AndreUtbetaling.tilstand, Utbetaling.Utbetalt)
        assertEquals(a2FørsteUtbetaling.korrelasjonsId, a2AndreUtbetaling.korrelasjonsId)
        a2FørsteUtbetaling.arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(1, oppdrag.size)
            val linje = oppdrag.single().inspektør
            assertEquals(17.mars, linje.fom)
            assertEquals(30.mars, linje.tom)
            assertEquals(1431, linje.beløp)
        }
        a2AndreUtbetaling.arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(1, oppdrag.size)
            val linje = oppdrag.single().inspektør
            assertEquals(17.mars, linje.fom)
            assertEquals(30.mars, linje.tom)
            assertEquals(1080, linje.beløp)
        }
        a1Utbetaling.arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(1, oppdrag.size)
            val linje = oppdrag.single().inspektør
            assertEquals(8.mars, linje.fom)
            assertEquals(15.mars, linje.tom)
            assertEquals(1080, linje.beløp)
        }

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
    }

    @Test
    fun `out of order som overlapper med eksisterende`() {
        nyttVedtak(1.mars, 31.mars, orgnummer = a1)
        nyPeriode(20.februar til 15.mars, a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertFunksjonellFeil(RV_SØ_13, 1.vedtaksperiode.filter())
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `To arbeidsgivere gikk inn i en bar - og første arbeidsgiver ble ferdig behandlet før vi mottok sykmelding på neste arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 30000.månedlig, orgnummer = a1)

        val inntektsvurdering = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
            )
        )
        val ivForSykepengegrunnlag = InntektForSykepengegrunnlag(
            inntekter = listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
            )
            , arbeidsforhold = emptyList()
        )
        val arbeidsforhold = listOf(Arbeidsforhold(a1, LocalDate.EPOCH), Arbeidsforhold(a2, LocalDate.EPOCH))

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = inntektsvurdering,
            inntektsvurderingForSykepengegrunnlag = ivForSykepengegrunnlag,
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertUtbetalingsbeløp(
            1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 997,
            forventetArbeidsgiverRefusjonsbeløp = 1385,
            orgnummer = a1,
            subset = 17.januar til 31.januar
        )

        assertEquals(1, inspektør(a1).arbeidsgiverOppdrag.size)
        assertEquals(0, inspektør(a2).arbeidsgiverOppdrag.size)

        nullstillTilstandsendringer()
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 30000.månedlig, orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            orgnummer = a2
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertUtbetalingsbeløp(
            1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1081,
            forventetArbeidsgiverRefusjonsbeløp = 1385,
            orgnummer = a1,
            subset = 17.januar til 31.januar
        )
        assertUtbetalingsbeløp(
            1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1080,
            forventetArbeidsgiverRefusjonsbeløp = 1385,
            orgnummer = a2,
            subset = 17.januar til 31.januar
        )
        assertEquals(2, inspektør(a1).utbetalinger.size)
        assertEquals(1, inspektør(a2).utbetalinger.size)
    }

    @Test
    fun `revurdering av senere frittstående periode hos ag3 mens overlappende out of order hos ag1 og ag2 utbetales`() {
        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)
        val inntekt = 20000.månedlig
        håndterInntektsmelding(listOf(1.april til 16.april), beregnetInntekt = inntekt, orgnummer = a1)
        val sammenligningsgrunnlag = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag,
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()


        nyPeriode(1.februar til 28.februar, a2, a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a3)

        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a3)

        val sammenligningsgrunnlag2 = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag2 = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold2 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag2,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag2,
            arbeidsforhold = arbeidsforhold2,
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a3)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a3)
    }

    @Test
    fun `revurdering av senere frittstående periode hos ag3 mens overlappende out of order hos ag1 og ag2 utbetales -- forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)
        val inntekt = 20000.månedlig
        håndterInntektsmelding(listOf(1.april til 16.april), beregnetInntekt = inntekt, orgnummer = a1)

        val sammenligningsgrunnlag = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag,
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        nyPeriode(1.februar til 28.februar, a2, a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a3)

        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a3)

        val sammenligningsgrunnlag2 = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag2 = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold2 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag2,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag2,
            arbeidsforhold = arbeidsforhold2,
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)

        forlengelseTilGodkjenning(1.mars, 15.mars, a2, a3)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)
        assertSisteTilstand(2.vedtaksperiode, TIL_UTBETALING, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a3)
    }

    @Test
    fun `out of order periode mens senere periode revurderes til utbetaling`() {
        nyttVedtak(1.mai, 31.mai)
        forlengTilGodkjenning(1.juni, 30.juni)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        nullstillTilstandsendringer()
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)

        nullstillTilstandsendringer()
        håndterUtbetalt()

        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        håndterYtelser(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        assertTilstander(
            3.vedtaksperiode,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertNotNull(observatør.vedtakFattetEvent[2.vedtaksperiode.id(ORGNUMMER)])
    }

    @Test
    fun `første periode i til utbetaling når det dukker opp en out of order-periode`() {
        tilGodkjent(1.mars, 31.mars, 100.prosent, 1.mars)
        nullstillTilstandsendringer()
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertNotNull(observatør.vedtakFattetEvent[1.vedtaksperiode.id(ORGNUMMER)])

        nullstillTilstandsendringer()

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            1.vedtaksperiode,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVSLUTTET
        )
    }

    @Test
    fun `første periode i til utbetaling når det dukker opp en out of order-periode - utbetalingen feiler`() {
        tilGodkjent(1.mars, 31.mars, 100.prosent, 1.mars)
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        nullstillTilstandsendringer()
        håndterUtbetalt(status = Oppdragstatus.AVVIST)

        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, UTBETALING_FEILET)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertNull(observatør.vedtakFattetEvent[1.vedtaksperiode.id(ORGNUMMER)])
    }

    @Test
    fun `kort periode, lang periode kommer out of order - kort periode trenger ikke å sendes til saksbehandler`() {
        nyPeriode(1.mars til 16.mars)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyPeriode(1.januar til 31.januar)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        assertIngenInfo("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding", 1.vedtaksperiode.filter())

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `kort periode, lang periode kommer out of order og fører til utbetaling på kort periode som nå trenger IM`() {
        nyPeriode(1.mars til 16.mars)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyPeriode(1.februar til 25.februar)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertInfo("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding", 1.vedtaksperiode.filter())
        håndterInntektsmelding(listOf(1.februar til 16.februar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterInntektsmelding(listOf(1.februar til 16.februar), førsteFraværsdag = 1.mars)
        håndterYtelser(1.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `out-of-order med error skal ikke medføre revurdering`() {
        nyttVedtak(1.mars, 31.mars)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = true)
        assertFunksjonellFeil(RV_SØ_10.varseltekst, 2.vedtaksperiode.filter())
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `out-of-order som fører til nådd maksdato skal avslå riktige dager`() {
        createKorttidsPerson(UNG_PERSON_FNR_2018, 1.januar(1992), maksSykedager = 16)

        nyttVedtak(1.januar, 30.januar)
        assertEquals(6, inspektør.gjenståendeSykedager(1.vedtaksperiode))

        nyttVedtak(1.mai, 24.mai)
        assertEquals(0, inspektør.gjenståendeSykedager(2.vedtaksperiode))

        nyttVedtak(1.mars, 26.mars)
        håndterYtelser(2.vedtaksperiode)

        //Når out-of-order perioden for mars kommer inn, så er det dager i mai som skal bli avvist pga maksdato
        assertEquals(0, inspektør.gjenståendeSykedager(3.vedtaksperiode))
        assertEquals(0, inspektør.utbetalingstidslinjer(3.vedtaksperiode).inspektør.avvistDagTeller)
        assertEquals(6, inspektør.utbetalingstidslinjer(2.vedtaksperiode).inspektør.avvistDagTeller)
    }

    @Test
    fun `Warning ved out-of-order - én warning for perioden som trigger out-of-order, én warning for de som blir påvirket av out-of-order`() {
        nyttVedtak(1.mars, 31.mars)
        forlengVedtak(1.april, 30.april)
        forlengVedtak(1.mai, 31.mai)

        nyttVedtak(1.januar, 31.januar)

        assertVarsel(RV_OO_1, 4.vedtaksperiode.filter())
        assertIngenVarsel(RV_OO_2, 4.vedtaksperiode.filter())
        assertVarsel(RV_OO_2, 1.vedtaksperiode.filter())
        assertVarsel(RV_OO_2, 2.vedtaksperiode.filter())
        assertVarsel(RV_OO_2, 3.vedtaksperiode.filter())
    }

    @Test
    fun `Warning ved out-of-order - dukker ikke opp i revurderinger som ikke er out-of-order`() {
        nyttVedtak(1.mars, 31.mars)
        forlengVedtak(1.april, 30.april)
        forlengVedtak(1.mai, 31.mai)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.mars, Dagtype.Sykedag, 50)))

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        assertIngenVarsel(RV_OO_1, 1.vedtaksperiode.filter())
        assertIngenVarsel(RV_OO_1, 2.vedtaksperiode.filter())
        assertIngenVarsel(RV_OO_1, 3.vedtaksperiode.filter())
    }

    @Test
    fun `Out of order kastes ut når det finnes en forkastet periode senere i tid`() {
        tilGodkjenning(1.februar, 25.februar, ORGNUMMER)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, false)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

        nyPeriode(1.januar til 25.januar)
        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `Korte perioder skal ikke revurderes dersom de forblir innenfor AGP`() {
        nyPeriode(1.mars til 10.mars)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyPeriode(11.mars til 16.mars)
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyttVedtak(1.januar, 31.januar)

        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        assertVarsel(RV_OO_1, 3.vedtaksperiode.filter())
        assertIngenVarsel(RV_OO_1, 2.vedtaksperiode.filter())
        assertIngenVarsel(RV_OO_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Out of order gjør at AUU revurderes fordi de ikke lenger er innen AGP - ber om inntektsmelding`() {
        nyPeriode(1.mars til 10.mars)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyPeriode(11.mars til 16.mars)
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyttVedtak(1.februar, 25.februar)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size) // For 1. februar og 1.mars
        assertInfo("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding", 1.vedtaksperiode.filter())

        håndterInntektsmelding(listOf(1.februar til 16.februar), førsteFraværsdag = 1.mars)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `Out of order gjør at senere periode revurderes - ber ikke om ny inntektsmelding`() {
        nyttVedtak(1.mars, 31.mars)

        nyPeriode(1.februar til 25.februar)

        assertForventetFeil(
            forklaring = "støtter ikke out-of-order med mindre enn 16 dagers gap",
            nå = {
                assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
                assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
            },
            ønsket = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
                assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
                assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size)
                assertIngenInfo("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding", 1.vedtaksperiode.filter())
            }
        )
    }

    @Test
    fun `Ber om inntektsmelding ved påminnelse i AvventerRevurdering ved manglende IM`() {
        nyPeriode(1.mars til 15.mars)
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        nyttVedtak(1.februar, 20.februar)
        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size)
        assertEquals(2, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertEquals(3, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Ber ikke om inntektsmelding ved påminnelse i AvventerRevurdering når vi har nødvendig IM`() {
        nyttVedtak(1.mars, 31.mars)

        nyPeriode(1.januar til 31.januar)
        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size)
        assertEquals(1, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Out of order som overlapper med annen AG og flytter skjæringstidspunktet - nærmere enn 18 dager fra neste - støttes ikke`() {
        nyPeriode(1.mars til 31.mars, a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)
        nyPeriode(20.mars til 20.april, a2)

        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a1)
        håndterInntektsmelding(listOf(20.mars til 4.april), orgnummer = a2)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.mars(2017) til 1.februar inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt INNTEKT
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.mars, INNTEKT.repeat(3)),
                    grunnlag(a2, 1.mars, INNTEKT.repeat(3))
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        // siden perioden slutter på en fredag starter ikke oppholdstelling i arbeidsgiverperioden før mandagen.
        // 10.februar-2.mars hører derfor til samme arbeidsgiverperioden som 20.mars-4.april, ettersom avstanden mellom
        // 5.mars (påfølgende mandag)-20.mars er akkurat 16 dager
        nyPeriode(10.februar til 2.mars, a2)

        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD, a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertFunksjonellFeil(`Mottatt søknad out of order innenfor 18 dager`)
    }
}