package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.forlengTilGodkjentVedtak
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjent
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class RevurderInntektTest : AbstractEndToEndTest() {

    @Test
    fun `revurder inntekt happy case`() {
        nyttVedtak(januar, 100.prosent)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 32000.månedlig,
            refusjon = Refusjon(32000.månedlig, null, emptyList())
        )
        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderInntektTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertVarsler(listOf(Varselkode.RV_IM_4), 1.vedtaksperiode.filter())
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
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )
        assertEquals(15741, inspektør.utbetaling(0).arbeidsgiverOppdrag.nettoBeløp())
        assertEquals(506, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.nettoBeløp())

        val vilkårgrunnlagsinspektør = person.inspektør.vilkårsgrunnlagHistorikk
        assertEquals(2, vilkårgrunnlagsinspektør.antallGrunnlagsdata())

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, 32000.månedlig)
        }
    }

    @Test
    fun `revurder inntekt flere ganger`() {
        nyttVedtak(januar, 100.prosent)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 32000.månedlig,
            refusjon = Refusjon(32000.månedlig, null, emptyList())
        )

        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderInntektTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 31000.månedlig,
            refusjon = Refusjon(31000.månedlig, null, emptyList())
        )
        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)
        assertVarsler(listOf(Varselkode.RV_IM_4, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderInntektTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertEquals(15741, inspektør.utbetaling(0).arbeidsgiverOppdrag.nettoBeløp())
        assertEquals(506, inspektør.utbetaling(1).arbeidsgiverOppdrag.nettoBeløp())
        assertEquals(-506, inspektør.utbetaling(2).arbeidsgiverOppdrag.nettoBeløp())

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT)
        }
    }

    @Test
    fun `revurder inntekt tidligere skjæringstidspunkt`() {
        nyttVedtak(januar, 100.prosent)
        nyttVedtak(mars, 100.prosent, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)

        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)

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
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )

        assertTilstander(
            1,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING
        )

        assertEquals(2, inspektør.antallUtbetalinger)
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `revurder inntekt ny inntekt under en halv G`() {
        nyttVedtak(januar, 100.prosent)
        håndterOverstyrInntekt(inntekt = 3000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertVarsler(listOf(Varselkode.RV_SV_1, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
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
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )

        val utbetalingTilRevurdering = inspektør.sisteUtbetaling()
        assertEquals(2, inspektør.antallUtbetalinger)
        assertEquals(-15741, utbetalingTilRevurdering.arbeidsgiverOppdrag.nettoBeløp())

        assertFalse(inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje.harUtbetalingsdager())
    }

    @Test
    fun `Ved revurdering av inntekt til under krav til minste sykepengegrunnlag skal utbetaling opphøres`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 50000.årlig,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderInntektTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(46000.årlig, skjæringstidspunkt = 1.januar) // da havner vi under greia
        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)

        assertVarsler(listOf(Varselkode.RV_SV_1, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
        assertEquals(inspektør.utbetaling(0).arbeidsgiverOppdrag.fagsystemId, inspektør.utbetaling(1).arbeidsgiverOppdrag.fagsystemId)
        assertEquals(inspektør.utbetaling(0).arbeidsgiverOppdrag.nettoBeløp(), -1 * inspektør.utbetaling(1).arbeidsgiverOppdrag.nettoBeløp())
    }

    @Test
    fun `revurder inntekt til under krav til minste sykepengegrunnlag slik at utbetaling opphører, og så revurder igjen til over krav til minste sykepengegrunnlag`() {
        val OverMinstegrense = 50000.årlig
        val UnderMinstegrense = 46000.årlig

        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = OverMinstegrense,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderInntektTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(UnderMinstegrense, skjæringstidspunkt = 1.januar)
        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)
        assertVarsler(listOf(Varselkode.RV_SV_1, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderInntektTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(OverMinstegrense, skjæringstidspunkt = 1.januar)
        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)

        var opprinneligFagsystemId: String?
        inspektør.utbetaling(0).arbeidsgiverOppdrag.apply {
            skalHaEndringskode(Endringskode.NY)
            opprinneligFagsystemId = fagsystemId
            assertEquals(1, size)
            first().assertUtbetalingslinje(Endringskode.NY, 1, null, null)
        }
        inspektør.utbetaling(1).arbeidsgiverOppdrag.apply {
            skalHaEndringskode(Endringskode.ENDR)
            assertEquals(opprinneligFagsystemId, fagsystemId)
            assertEquals(1, size)
            first().assertUtbetalingslinje(Endringskode.ENDR, 1, null, null, ønsketDatoStatusFom = 17.januar)
        }
        inspektør.utbetaling(2).arbeidsgiverOppdrag.apply {
            skalHaEndringskode(Endringskode.ENDR)
            assertEquals(opprinneligFagsystemId, fagsystemId)
            assertEquals(1, size)
            first().assertUtbetalingslinje(Endringskode.NY, 2, 1, fagsystemId)
        }
    }

    @Test
    fun `revurdere inntekt slik at det blir brukerutbetaling`() {
        nyttVedtak(januar)
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderInntektTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
    }

    @Test
    fun `revurdere inntekt slik at det blir brukerutbetaling `() {
        nyttVedtak(januar)
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `revurdere inntekt slik at det blir delvis refusjon`() {
        nyttVedtak(januar)
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(25000.månedlig, null, emptyList())
        )
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderInntektTest.håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `revurdere mens en forlengelse er til utbetaling`() {
        nyttVedtak(januar)
        forlengTilGodkjentVedtak(februar)
        nullstillTilstandsendringer()
        håndterOverstyrInntekt(INNTEKT * 1.05, skjæringstidspunkt = 1.januar)
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
        assertNotNull(observatør.avsluttetMedVedtakEvent[2.vedtaksperiode.id(a1)])
    }

    @Test
    fun `revurdere mens en førstegangsbehandlingen er til utbetaling`() {
        tilGodkjent(januar, 100.prosent)
        nullstillTilstandsendringer()
        håndterOverstyrInntekt(INNTEKT * 1.05, skjæringstidspunkt = 1.januar)
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertNotNull(observatør.avsluttetMedVedtakEvent[1.vedtaksperiode.id(a1)])
    }

    @Test
    fun `revurdere mens en førstegangsbehandlingen er til utbetaling - utbetalingen feiler`() {
        tilGodkjent(januar, 100.prosent)
        håndterOverstyrInntekt(INNTEKT / 2, skjæringstidspunkt = 1.januar)
        nullstillTilstandsendringer()
        håndterUtbetalt(status = Oppdragstatus.AVVIST)
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertNull(observatør.avsluttetMedVedtakEvent[1.vedtaksperiode.id(a1)])
    }

    @Test
    fun `revurdere mens en periode har feilet i utbetaling`() {
        nyttVedtak(januar)
        forlengTilGodkjentVedtak(februar)
        håndterUtbetalt(status = Oppdragstatus.FEIL)
        håndterOverstyrInntekt(INNTEKT / 2, skjæringstidspunkt = 1.januar)
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
            AVSLUTTET,
            AVVENTER_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVVENTER_REVURDERING
        )
    }

    private fun Oppdrag.skalHaEndringskode(kode: Endringskode, message: String = "") {
        assertEquals(kode, endringskode, message)
    }

    private fun Utbetalingslinje.assertUtbetalingslinje(
        ønsketEndringskode: Endringskode,
        ønsketDelytelseId: Int,
        ønsketRefDelytelseId: Int? = null,
        ønsketRefFagsystemId: String? = null,
        ønsketDatoStatusFom: LocalDate? = null
    ) {
        assertEquals(ønsketEndringskode, endringskode)
        assertEquals(ønsketDelytelseId, delytelseId)
        assertEquals(ønsketRefDelytelseId, refDelytelseId)
        assertEquals(ønsketRefFagsystemId, refFagsystemId)
        assertEquals(ønsketDatoStatusFom, datoStatusFom)
    }
}
