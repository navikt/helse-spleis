package no.nav.helse.spleis.e2e.brukerutbetaling

import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsmelding.Refusjon.EndringIRefusjon
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertInntektshistorikkForDato
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertUtbetalingsbeløp
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DelvisRefusjonTest : AbstractEndToEndTest() {

    @Test
    fun `Full refusjon til en arbeidsgiver med RefusjonPerDag på`() {
        nyttVedtak(januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))

        assertTrue(inspektør.sisteUtbetaling().arbeidsgiverOppdrag.isNotEmpty())
        inspektør.sisteUtbetaling().arbeidsgiverOppdrag.forEach { assertEquals(1431, it.beløp) }
        assertTrue(inspektør.sisteUtbetaling().personOppdrag.isEmpty())
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
    }

    @Test
    fun `Full refusjon til en arbeidsgiver med forlengelse og opphørsdato treffer ferie`() {
        nyttVedtak(januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, 27.februar, emptyList()))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(27.februar, 28.februar))
        this@DelvisRefusjonTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
        assertTrue(inspektør.sisteUtbetaling().arbeidsgiverOppdrag.isNotEmpty())
        inspektør.sisteUtbetaling().arbeidsgiverOppdrag.forEach { assertEquals(1431, it.beløp) }
        assertTrue(inspektør.sisteUtbetaling().personOppdrag.isEmpty())
        assertUtbetalingsbeløp(2.vedtaksperiode, 1431, 1431, subset = 1.februar til 26.februar)
        assertUtbetalingsbeløp(2.vedtaksperiode, 0, 1431, subset = 27.februar til 27.februar)
        assertUtbetalingsbeløp(2.vedtaksperiode, 0, 0, subset = 28.februar til 28.februar)
    }

    @Test
    fun `Refusjonsbeløpet er forskjellig fra beregnet inntekt i inntektsmeldingen`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 30000.månedlig,
            refusjon = Inntektsmelding.Refusjon(25000.månedlig, null, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `arbeidsgiver refunderer ikke`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `tidligere vedtaksperiode har opphør i refusjon`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, 20.januar, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
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

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(mars)
        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(2.vedtaksperiode)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )


        assertTrue(inspektør.sisteUtbetaling().arbeidsgiverOppdrag.isNotEmpty())
        inspektør.sisteUtbetaling().arbeidsgiverOppdrag.forEach { assertEquals(1431, it.beløp) }
        assertTrue(inspektør.sisteUtbetaling().personOppdrag.isEmpty())
        assertUtbetalingsbeløp(2.vedtaksperiode, 0, 1431, subset = 1.mars til 16.mars)
        assertUtbetalingsbeløp(2.vedtaksperiode, 1431, 1431, subset = 17.mars til 31.mars)
    }

    @Test
    fun `kaster ikke ut vedtaksperiode når refusjonopphører`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, 20.januar, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `ikke kast ut vedtaksperiode ved endring i refusjon`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, listOf(EndringIRefusjon(15000.månedlig, 20.januar))),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `kaster ikke ut vedtaksperiode hvor endring i refusjon er etter perioden`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, listOf(EndringIRefusjon(15000.månedlig, 1.februar))),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
    }

    @Test
    fun `ikke kast ut vedtaksperiode ved ferie i slutten av perioden`() {
        håndterSykmelding(januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(25.januar, 31.januar))
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, 24.januar, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertTrue(inspektør.sisteUtbetaling().arbeidsgiverOppdrag.isNotEmpty())
        inspektør.sisteUtbetaling().arbeidsgiverOppdrag.forEach { assertEquals(1431, it.beløp) }
        assertTrue(inspektør.sisteUtbetaling().personOppdrag.isEmpty())

        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, 17.januar til 24.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 0, 25.januar til 31.januar)
    }

    @Test
    fun `to arbeidsgivere med ulik fom hvor den første har utbetalingsdager før arbeisdgiverperioden til den andre, ingen felles utbetalingsdager`() {
        håndterSykmelding(januar, orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(21.januar, 10.februar), orgnummer = a2)

        håndterSøknad(januar, orgnummer = a1)
        håndterSøknad(Sykdom(21.januar, 10.februar, 100.prosent), orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(21.januar til 5.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, a1, a2, orgnummer = a1)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)

        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 0,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 1.januar til 16.januar,
            orgnummer = a1
        )
        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1080,
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
            forventetArbeidsgiverbeløp = 1080,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 6.februar til 10.februar,
            orgnummer = a2
        )
    }

    @Test
    fun `to arbeidsgivere med ulik fom hvor den første har utbetalingsdager før arbeisdgiverperioden til den andre`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(21.januar, 10.februar), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 10.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(21.januar, 10.februar, 100.prosent), orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(21.januar til 5.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, a1, a2, orgnummer = a1)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)

        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 0,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 1.januar til 16.januar,
            orgnummer = a1
        )
        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1080,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 17.januar til 5.februar,
            orgnummer = a1
        )
        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1080,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 6.februar til 10.februar,
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
    fun `to arbeidsgivere med ulik fom hvor den andre har utbetalingsdager før arbeidsgiverperioden til den første`() {
        håndterSykmelding(Sykmeldingsperiode(21.januar, 10.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.februar), orgnummer = a2)

        håndterSøknad(Sykdom(21.januar, 10.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 10.februar, 100.prosent), orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a1)

        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(21.januar til 5.februar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, orgnummer = a2)
        håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, a1, a2, orgnummer = a2)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 0,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 21.januar til 5.februar,
            orgnummer = a1
        )
        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1080,
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
            forventetArbeidsgiverbeløp = 1080,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 17.januar til 20.januar,
            orgnummer = a2
        )
        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1080,
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
    fun `gradert sykmelding med en arbeidsgiver`() {
        håndterSykmelding(januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `korrigerende inntektsmelding endrer på refusjonsbeløp`()  {
        håndterSykmelding(januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        val im2 = håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT + 100.månedlig,
            refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList())
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT + 100.månedlig)
        }
        assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT / 2, im2.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
    }

    @Test
    fun `to arbeidsgivere hvor andre arbeidsgiver har delvis refusjon`() {
        håndterSykmelding(januar, orgnummer = a1)
        håndterSykmelding(januar, orgnummer = a2)
        håndterSøknad(januar, orgnummer = a1)
        håndterSøknad(januar, orgnummer = a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(
                INNTEKT, 20.januar
            ),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)

        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertEquals(1, inspektør(a2).antallUtbetalinger)
        inspektør(a1).utbetaling(0).also { utbetaling ->
            assertEquals(2, utbetaling.arbeidsgiverOppdrag.size)
            utbetaling.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(1080, linje.beløp)
                assertEquals(17.januar til 21.januar, linje.fom til linje.tom)
            }
            utbetaling.arbeidsgiverOppdrag[1].inspektør.also { linje ->
                assertEquals(1431, linje.beløp)
                assertEquals(22.januar til 31.januar, linje.fom til linje.tom)
            }
            assertTrue(utbetaling.personOppdrag.isEmpty())
        }
        assertEquals(1, inspektør(a2).antallUtbetalinger)
        inspektør(a2).utbetaling(0).also { utbetaling ->
            assertEquals(1, utbetaling.arbeidsgiverOppdrag.size)
            utbetaling.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(1080, linje.beløp)
                assertEquals(17.januar til 19.januar, linje.fom til linje.tom)
            }

            assertEquals(1, utbetaling.personOppdrag.size)
            utbetaling.personOppdrag[0].inspektør.also { linje ->
                assertEquals(730, linje.beløp)
                assertEquals(22.januar til 31.januar, linje.fom til linje.tom)
            }
        }
    }

    @Test
    fun `Første utbetalte dag er før første fraværsdag`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        val inntektsmeldingId = håndterInntektsmelding(
            listOf(),
            førsteFraværsdag = 17.januar
        )

        assertInntektshistorikkForDato(INNTEKT, 1.januar, inspektør)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT, inntektsmeldingId.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
    }

    @Test
    fun `Korrigerende inntektsmelding med feil skjæringstidspunkt går til manuell behandling på grunn av warning`()  {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)

        håndterInntektsmelding(emptyList(), førsteFraværsdag = 1.januar)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
    }

    @Test
    fun `arbeidsgiver sender unødvendig inntektsmelding ved forlengelse før sykmelding`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterInntektsmelding(
            emptyList(),
            førsteFraværsdag = 1.februar
        )
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(februar)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `to arbeidsgivere, hvor den andre har opphør i refusjon`() {
        håndterSykmelding(januar, orgnummer = a1)
        håndterSykmelding(januar, orgnummer = a2)
        håndterSøknad(januar, orgnummer = a1)
        håndterSøknad(januar, orgnummer = a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(
                INNTEKT, 15.januar, emptyList()
            ),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        this@DelvisRefusjonTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@DelvisRefusjonTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertEquals(1, inspektør(a1).antallUtbetalinger)
        inspektør(a1).utbetaling(0).also { utbetaling ->
            val linje = utbetaling.arbeidsgiverOppdrag[0].inspektør
            assertEquals(1431, linje.beløp)
            assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
            assertTrue(utbetaling.personOppdrag.isEmpty())
        }
        assertEquals(1, inspektør(a2).antallUtbetalinger)
        inspektør(a2).utbetaling(0).also { utbetaling ->
            val linje = utbetaling.personOppdrag[0].inspektør
            assertEquals(730, linje.beløp)
            assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
            assertTrue(utbetaling.arbeidsgiverOppdrag.isEmpty())
        }
    }
}
