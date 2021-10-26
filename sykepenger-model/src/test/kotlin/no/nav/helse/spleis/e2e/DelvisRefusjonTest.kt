package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsmelding.Refusjon.EndringIRefusjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.testhelpers.november
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class DelvisRefusjonTest : AbstractEndToEndTest() {

    @Test
    fun `Full refusjon til en arbeidsgiver med RefusjonPerDag på`() = Toggles.RefusjonPerDag.enable {
        nyttVedtak(1.januar, 31.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))

        assertTrue(inspektør.utbetalinger.last().arbeidsgiverOppdrag().isNotEmpty())
        inspektør.utbetalinger.last().arbeidsgiverOppdrag().forEach { assertEquals(1431, it.beløp) }
        assertTrue(inspektør.utbetalinger.last().personOppdrag().isEmpty())
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
    }

    @Test
    fun `Full refusjon til en arbeidsgiver med forlengelse og opphørsdato treffer ferie`() = Toggles.RefusjonPerDag.enable {
        nyttVedtak(1.januar, 31.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, 27.februar, emptyList()))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(27.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
        assertTrue(inspektør.utbetalinger.last().arbeidsgiverOppdrag().isNotEmpty())
        inspektør.utbetalinger.last().arbeidsgiverOppdrag().forEach { assertEquals(1431, it.beløp) }
        assertTrue(inspektør.utbetalinger.last().personOppdrag().isEmpty())
        assertUtbetalingsbeløp(2.vedtaksperiode, 1431, 1431, subset = 1.februar til 26.februar)
        assertUtbetalingsbeløp(2.vedtaksperiode, 0, 1431, subset = 27.februar til 27.februar)
        assertUtbetalingsbeløp(2.vedtaksperiode, 0, 0, subset = 28.februar til 28.februar)
    }

    @Test
    fun `Refusjonsbeløpet er forskjellig fra beregnet inntekt i inntektsmeldingen`() = Toggles.RefusjonPerDag.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 30000.månedlig,
            refusjon = Inntektsmelding.Refusjon(25000.månedlig, null, emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `arbeidsgiver refunderer ikke`() = Toggles.RefusjonPerDag.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Full refusjon til en arbeidsgiver med RefusjonPerDag av`() = Toggles.RefusjonPerDag.disable {
        nyttVedtak(1.januar, 31.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))

        assertTrue(inspektør.utbetalinger.last().arbeidsgiverOppdrag().isNotEmpty())
        inspektør.utbetalinger.last().arbeidsgiverOppdrag().forEach { assertEquals(1431, it.beløp) }
        assertTrue(inspektør.utbetalinger.last().personOppdrag().isEmpty())

        inspektør.utbetalingstidslinjer(1.vedtaksperiode).forEach {
            it.økonomi.medAvrundetData { _, arbeidsgiverRefusjonsbeløp, _, _, arbeidsgiverbeløp, personbeløp, _ ->
                val forventetArbeidsgiverbeløp = if (it is Utbetalingstidslinje.Utbetalingsdag.NavDag) 1431 else 0
                assertEquals(forventetArbeidsgiverbeløp, arbeidsgiverbeløp)
                assertEquals(null, arbeidsgiverRefusjonsbeløp)
                assertEquals(0, personbeløp)
            }
        }
    }

    @Test
    fun `Arbeidsgiverperiode tilstøter Infotrygd`() =
        Toggles.RefusjonPerDag.enable {
            håndterInntektsmelding(listOf(1.januar til 16.januar))

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterUtbetalingshistorikk(
                1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
                    Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt(1.vedtaksperiode)
            assertEquals(0, inspektør.warnings.size)
            assertFalse(inspektør.warnings.contains("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler."))
        }

    @Test
    fun `Arbeidsgiverperiode tilstøter ikke Infotrygd`() =
        Toggles.RefusjonPerDag.enable {
            håndterInntektsmelding(listOf(1.november(2017) til 16.november(2017)))

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterUtbetalingshistorikk(
                1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
                    Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt(1.vedtaksperiode)
            assertEquals(1, inspektør.warnings.size)
            assertTrue(inspektør.warnings.contains("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler."))
        }

    @Test
    fun `Finner refusjon ved forlengelse fra Infotrygd`() = Toggles.RefusjonPerDag.enable {
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        assertEquals(0, inspektør.warnings.size)
        assertFalse(inspektør.warnings.contains("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler."))
    }

    @Test
    fun `ikke kast ut vedtaksperiode når tidligere vedtaksperiode har opphør i refusjon`() = Toggles.RefusjonPerDag.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, 20.januar, emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(
            listOf(1.mars til 16.mars)
        )
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )


        assertTrue(inspektør.utbetalinger.last().arbeidsgiverOppdrag().isNotEmpty())
        inspektør.utbetalinger.last().arbeidsgiverOppdrag().forEach { assertEquals(1431, it.beløp) }
        assertTrue(inspektør.utbetalinger.last().personOppdrag().isEmpty())
        assertUtbetalingsbeløp(2.vedtaksperiode, 0, 1431, subset = 1.mars til 16.mars)
        assertUtbetalingsbeløp(2.vedtaksperiode, 1431, 1431, subset = 17.mars til 31.mars)
    }

    @Test
    fun `kast ut vedtaksperiode når refusjonopphører`() = Toggles.RefusjonPerDag.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, 20.januar, emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `kast ut vedtaksperiode ved endring i refusjon`() = Toggles.RefusjonPerDag.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, listOf(EndringIRefusjon(15000.månedlig, 20.januar)))
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `kaster ikke ut vedtaksperiode hvor endring i refusjon er etter perioden`() = Toggles.RefusjonPerDag.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, listOf(EndringIRefusjon(15000.månedlig, 1.februar)))
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
    }

    @Test
    fun `ikke kast ut vedtaksperiode ved ferie i slutten av perioden`() = Toggles.RefusjonPerDag.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(25.januar, 31.januar))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, 24.januar, emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertTrue(inspektør.utbetalinger.last().arbeidsgiverOppdrag().isNotEmpty())
        inspektør.utbetalinger.last().arbeidsgiverOppdrag().forEach { assertEquals(1431, it.beløp) }
        assertTrue(inspektør.utbetalinger.last().personOppdrag().isEmpty())

        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, 17.januar til 24.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 0, 25.januar til 31.januar)
    }

    @Test
    fun `to arbeidsgivere med ulik fom hvor den første har utbetalingsdager før arbeisdgiverperioden til den andre, ingen felles utbetalingsdager`() =
        Toggles.RefusjonPerDag.enable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(21.januar, 10.februar, 100.prosent), orgnummer = a2)

            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
            håndterSøknad(Sykdom(21.januar, 10.februar, 100.prosent), orgnummer = a2)

            håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar til 16.januar), orgnummer = a1)
            assertTilstand(orgnummer = a1, tilstand = AVVENTER_ARBEIDSGIVERE)
            assertTilstand(orgnummer = a2, tilstand = AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

            håndterInntektsmelding(arbeidsgiverperioder = listOf(21.januar til 5.februar), orgnummer = a2)
            assertTilstand(orgnummer = a1, tilstand = AVVENTER_HISTORIKK)
            assertTilstand(orgnummer = a2, tilstand = AVVENTER_ARBEIDSGIVERE)

            håndterYtelser(1.vedtaksperiode, orgnummer = a1)
            håndterVilkårsgrunnlag(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                orgnummer = a1,
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(12)),
                        sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    listOf(
                        grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3)),
                        grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3))
                    )
                )
            )
            håndterYtelser(1.vedtaksperiode, orgnummer = a1)
            håndterSimulering(1.vedtaksperiode, orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
            assertTilstand(orgnummer = a1, tilstand = AVSLUTTET)
            assertTilstand(orgnummer = a2, tilstand = AVVENTER_HISTORIKK)

            håndterYtelser(1.vedtaksperiode, orgnummer = a2)
            håndterSimulering(1.vedtaksperiode, orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
            håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)
            assertTilstand(orgnummer = a1, tilstand = AVSLUTTET)
            assertTilstand(orgnummer = a2, tilstand = AVSLUTTET)

            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 1.januar til 16.januar,
                orgnummer = a1
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1081,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 17.januar til 31.januar,
                orgnummer = a1
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 1.februar til 10.februar,
                orgnummer = a1
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                subset = 1.januar til 20.januar,
                orgnummer = a2
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 21.januar til 5.februar,
                orgnummer = a2
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1081,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 6.februar til 10.februar,
                orgnummer = a2
            )
        }

    @Test
    fun `to arbeidsgivere med ulik fom hvor den første har utbetalingsdager før arbeisdgiverperioden til den andre`() =
        Toggles.RefusjonPerDag.enable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 10.februar, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(21.januar, 10.februar, 100.prosent), orgnummer = a2)

            håndterSøknad(Sykdom(1.januar, 10.februar, 100.prosent), orgnummer = a1)
            håndterSøknad(Sykdom(21.januar, 10.februar, 100.prosent), orgnummer = a2)

            håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar til 16.januar), orgnummer = a1)
            assertTilstand(orgnummer = a1, tilstand = AVVENTER_ARBEIDSGIVERE)
            assertTilstand(orgnummer = a2, tilstand = AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

            håndterInntektsmelding(arbeidsgiverperioder = listOf(21.januar til 5.februar), orgnummer = a2)
            assertTilstand(orgnummer = a1, tilstand = AVVENTER_HISTORIKK)
            assertTilstand(orgnummer = a2, tilstand = AVVENTER_ARBEIDSGIVERE)

            håndterYtelser(1.vedtaksperiode, orgnummer = a1)
            håndterVilkårsgrunnlag(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                orgnummer = a1,
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(12)),
                        sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    listOf(
                        grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3)),
                        grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3))
                    )
                )
            )
            håndterYtelser(1.vedtaksperiode, orgnummer = a1)
            håndterSimulering(1.vedtaksperiode, orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
            assertTilstand(orgnummer = a1, tilstand = AVSLUTTET)
            assertTilstand(orgnummer = a2, tilstand = AVVENTER_HISTORIKK)

            håndterYtelser(1.vedtaksperiode, orgnummer = a2)
            håndterSimulering(1.vedtaksperiode, orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
            håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)
            assertTilstand(orgnummer = a1, tilstand = AVSLUTTET)
            assertTilstand(orgnummer = a2, tilstand = AVSLUTTET)

            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 1.januar til 16.januar,
                orgnummer = a1
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1081,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 17.januar til 10.februar,
                orgnummer = a1
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                subset = 1.januar til 20.januar,
                orgnummer = a2
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 21.januar til 5.februar,
                orgnummer = a2
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 6.februar til 10.februar,
                orgnummer = a2
            )
        }

    @Test
    fun `to arbeidsgivere med ulik fom hvor den andre har utbetalingsdager før arbeidsgiverperioden til den første`() =
        Toggles.RefusjonPerDag.enable {
            håndterSykmelding(Sykmeldingsperiode(21.januar, 10.februar, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(1.januar, 10.februar, 100.prosent), orgnummer = a2)

            håndterSøknad(Sykdom(21.januar, 10.februar, 100.prosent), orgnummer = a1)
            håndterSøknad(Sykdom(1.januar, 10.februar, 100.prosent), orgnummer = a2)

            håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar til 16.januar), orgnummer = a2)
            assertTilstand(orgnummer = a2, tilstand = AVVENTER_ARBEIDSGIVERE)
            assertTilstand(orgnummer = a1, tilstand = AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

            håndterInntektsmelding(arbeidsgiverperioder = listOf(21.januar til 5.februar), orgnummer = a1)
            assertTilstand(orgnummer = a1, tilstand = AVVENTER_HISTORIKK)
            assertTilstand(orgnummer = a2, tilstand = AVVENTER_ARBEIDSGIVERE)

            håndterYtelser(1.vedtaksperiode, orgnummer = a1)
            håndterVilkårsgrunnlag(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                orgnummer = a1,
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(12)),
                        sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    listOf(
                        grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3)),
                        grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3))
                    )
                )
            )
            håndterYtelser(1.vedtaksperiode, orgnummer = a1)
            håndterSimulering(1.vedtaksperiode, orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
            assertTilstand(orgnummer = a1, tilstand = AVSLUTTET)
            assertTilstand(orgnummer = a2, tilstand = AVVENTER_HISTORIKK)

            håndterYtelser(1.vedtaksperiode, orgnummer = a2)
            håndterSimulering(1.vedtaksperiode, orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
            håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)
            assertTilstand(orgnummer = a1, tilstand = AVSLUTTET)
            assertTilstand(orgnummer = a2, tilstand = AVSLUTTET)

            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 21.januar til 5.februar,
                orgnummer = a1
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1081,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 6.februar til 10.februar,
                orgnummer = a1
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                subset = 1.januar til 20.januar,
                orgnummer = a1
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 1.januar til 16.januar,
                orgnummer = a2
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1081,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 17.januar til 20.januar,
                orgnummer = a2
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1081,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 21.januar til 5.februar,
                orgnummer = a2
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 6.februar til 10.februar,
                orgnummer = a2
            )
        }

    @Test
    fun `gradert sykmelding med en arbeidsgiver`() = Toggles.RefusjonPerDag.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 50.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `korrigerende inntektsmelding endrer på refusjonsbeløp`() = Toggles.RefusjonPerDag.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 50.prosent))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList())
        )
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList())
        )
        håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `korrigerende inntektsmelding endrer på refusjonsbeløp med infotrygdforlengelse`() = Toggles.RefusjonPerDag.enable {
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList())
        )
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList())
        )
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 50.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 50.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 50.prosent, INNTEKT / 2),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `korrigerende inntektsmelding endrer på refusjonsbeløp med infotrygdforlengelse og gap`() = Toggles.RefusjonPerDag.enable {
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList())
        )
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList())
        )
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 50.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 50.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 18.januar, 31.januar, 50.prosent, INNTEKT / 2),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 18.januar, INNTEKT, true)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `to arbeidsgivere hvor andre arbeidsgiver har brukerutbetaling kaster ut alle perioder på personen`() = Toggles.RefusjonPerDag.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2, refusjon = Inntektsmelding.Refusjon(INNTEKT, 20.januar))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3)),
                    grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3))
                )
            ),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD,
            orgnummer = a1
        )
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERE,
            TIL_INFOTRYGD,
            orgnummer = a2
        )

    }
}
