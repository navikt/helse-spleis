package no.nav.helse.spleis.e2e

import no.nav.helse.*
import no.nav.helse.ForventetFeil
import no.nav.helse.Toggle.Companion.disable
import no.nav.helse.Toggle.Companion.enable
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.inspectors.Kilde
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class RevurderInntektTest : AbstractEndToEndTest() {

    @Test
    fun `revurder inntekt happy case`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)

        val tidligereInntektInnslagId = inspektør.inntektInspektør.sisteInnslag?.innslagId

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 32000.månedlig, refusjon = Refusjon(32000.månedlig, null, emptyList()))
        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        val grunnlagsdataInspektør = GrunnlagsdataInspektør(inspektør.vilkårsgrunnlagHistorikk[0].second)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_VILKÅRSPRØVING_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertEquals(15741, inspektør.utbetalinger.first().inspektør.arbeidsgiverOppdrag.nettoBeløp())
        assertEquals(506, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.nettoBeløp())

        assertEquals(2, inspektør.vilkårsgrunnlagHistorikk.size)
        assertEquals(3, grunnlagsdataInspektør.avviksprosent?.roundToInt())

        val tidligereBeregning = inspektør.utbetalingstidslinjeberegningData.first()
        assertEquals(tidligereBeregning.inntektshistorikkInnslagId, tidligereInntektInnslagId)

        val beregning = inspektør.utbetalingstidslinjeberegningData.last()
        assertEquals(beregning.inntektshistorikkInnslagId, inspektør.inntektInspektør.sisteInnslag?.innslagId)

        assertEquals(beregning.vilkårsgrunnlagHistorikkInnslagId, person.nyesteIdForVilkårsgrunnlagHistorikk())

        assertTrue(inspektør.inntektInspektør.sisteInnslag?.opplysninger?.any { it.kilde == Kilde.SAKSBEHANDLER } ?: false)
    }

    @Test
    fun `revurder inntekt flere ganger`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 32000.månedlig, refusjon = Refusjon(32000.månedlig, null, emptyList()))
        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 31000.månedlig, refusjon = Refusjon(31000.månedlig, null, emptyList()))
        håndterOverstyrInntekt(inntekt = 31000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        assertEquals(15741, inspektør.utbetalinger[0].inspektør.arbeidsgiverOppdrag.nettoBeløp())
        assertEquals(506, inspektør.utbetalinger[1].inspektør.arbeidsgiverOppdrag.nettoBeløp())
        assertEquals(-506, inspektør.utbetalinger[2].inspektør.arbeidsgiverOppdrag.nettoBeløp())

        val inntektFraSaksbehandler = inspektør.inntektInspektør.sisteInnslag?.opplysninger?.filter { it.kilde == Kilde.SAKSBEHANDLER }!!
        assertEquals(1, inntektFraSaksbehandler.size)
        assertEquals(31000.månedlig, inntektFraSaksbehandler.first().sykepengegrunnlag)
    }

    @Test
    fun `revurder inntekt ukjent skjæringstidspunkt`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 2.januar)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertEquals(1, inspektør.utbetalinger.size)
    }

    @Test
    fun `revurder inntekt tidligere skjæringstidspunkt`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        nyttVedtak(1.mars, 31.mars, 100.prosent)

        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertEquals(2, inspektør.utbetalinger.size)
    }

    @Test
    @ForventetFeil("Denne er skrudd av i påvente av at vi skal støtte revurdering over skjæringstidspunkt")
    fun `overstyr inntekt to vedtak med kort opphold`() {
        nyttVedtak(1.januar, 26.januar, 100.prosent)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 14.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 14.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.februar)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)

        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_VILKÅRSPRØVING_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVSLUTTET,

            )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertEquals(3, inspektør.utbetalinger.filter { it.inspektør.erUtbetalt }.size)
    }

    @Test
    fun `kan ikke revurdere inntekt på vedtak med opphold og nytt skjæringstidspunkt etterpå`() {
        nyttVedtak(1.januar, 26.januar, 100.prosent)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 14.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 14.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.februar)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertErrorTekst(inspektør, "Kan kun revurdere siste skjæringstidspunkt")
    }

    @Test
    fun `revurder inntekt avvik over 25 prosent reduksjon`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterOverstyrInntekt(inntekt = 7000.månedlig, skjæringstidspunkt = 1.januar)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_VILKÅRSPRØVING_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )

        assertWarningTekst(inspektør, "Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene.")
        assertEquals(1, inspektør.utbetalinger.size)
    }

    @Test
    fun `revurder inntekt avvik over 25 prosent økning`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterOverstyrInntekt(inntekt = 70000.månedlig, skjæringstidspunkt = 1.januar)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_VILKÅRSPRØVING_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )

        assertWarningTekst(inspektør, "Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene.")
        assertEquals(1, inspektør.utbetalinger.size)
    }

    @Test
    fun `revurder inntekt ny inntekt under en halv G`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterOverstyrInntekt(inntekt = 3000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_VILKÅRSPRØVING_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )

        val utbetalingTilRevurdering = inspektør.utbetalinger.last()
        assertEquals(2, inspektør.utbetalinger.size)
        assertEquals(-15741, utbetalingTilRevurdering.inspektør.arbeidsgiverOppdrag.nettoBeløp())

        assertWarningTekst(
            inspektør,
            "Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene.",
            "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag"
        )
        assertFalse(utbetalingTilRevurdering.utbetalingstidslinje().harUtbetalinger())
    }

    @Test
    fun `revurdering ved skjæringstidspunkt hos infotrygd`() {
        val historikk1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 29.januar(2018), 18.februar(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 19.februar(2018), 18.mars(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 19.mars(2018), 2.april(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 3.april(2018), 14.mai(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 15.mai(2018), 3.juni(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 4.juni(2018), 22.juni(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 18.mars(2020), 31.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.april(2020), 30.april(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.mai(2020), 31.mai(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning1 = listOf(
            Inntektsopplysning(ORGNUMMER.toString(), 18.mars(2020), INNTEKT, true),
            Inntektsopplysning(ORGNUMMER.toString(), 29.januar(2018), INNTEKT, true)
        )

        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historikk1.toTypedArray(), inntektshistorikk = inntektsopplysning1)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        forlengVedtak(1.juli(2020), 30.juli(2020))
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 18.mars(2020))

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
        )
        assertErrors(inspektør)
    }

    @Test
    fun `utbetaling_utbetalt tar med vedtaksperiode-ider for ett enkelt vedtak`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)

        val utbetalingEvent = observatør.utbetalingMedUtbetalingEventer.first()

        assertEquals(1, utbetalingEvent.vedtaksperiodeIder.size)
        assertEquals(1.vedtaksperiode.id(ORGNUMMER), utbetalingEvent.vedtaksperiodeIder.first())
    }

    @Test
    fun `utbetaling_utbetalt tar med vedtaksperiode-ider for flere vedtak`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        forlengVedtak(1.februar, 28.februar)

        val førsteEvent = observatør.utbetalingMedUtbetalingEventer.first()
        val andreEvent = observatør.utbetalingMedUtbetalingEventer.last()

        assertEquals(1, førsteEvent.vedtaksperiodeIder.size)
        assertEquals(1.vedtaksperiode.id(ORGNUMMER), førsteEvent.vedtaksperiodeIder.first())
        assertEquals(1, andreEvent.vedtaksperiodeIder.size)
        assertEquals(2.vedtaksperiode.id(ORGNUMMER), andreEvent.vedtaksperiodeIder.first())
    }

    @Test
    fun `utbetaling_utbetalt tar med vedtaksperiode-ider for revurdering over flere perioder`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        forlengVedtak(1.februar, 28.februar)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 32000.månedlig, refusjon = Refusjon(32000.månedlig, null, emptyList()))
        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)

        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode)

        val utbetalingsevent = observatør.utbetalingMedUtbetalingEventer.last()

        assertEquals(2, utbetalingsevent.vedtaksperiodeIder.size)
        assertTrue(utbetalingsevent.vedtaksperiodeIder.contains(1.vedtaksperiode.id(ORGNUMMER)))
        assertTrue(utbetalingsevent.vedtaksperiodeIder.contains(2.vedtaksperiode.id(ORGNUMMER)))
    }

    @Test
    fun `utbetaling_utbetalt tar med vedtaksperiode-ider for forkastede perioder`() {
        tilGodkjent(1.januar, 31.januar, 100.prosent, 1.januar)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), andreInntektskilder = listOf(Søknad.Inntektskilde(true, "FRILANSER")))
        håndterUtbetalt(1.vedtaksperiode)

        val utbetalingEvent = observatør.utbetalingMedUtbetalingEventer.first()

        assertEquals(1, utbetalingEvent.vedtaksperiodeIder.size)
        assertEquals(1.vedtaksperiode.id(ORGNUMMER), utbetalingEvent.vedtaksperiodeIder.first())
    }


    @Test
    fun `avviser revurdering av inntekt for saker med flere arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        håndterOverstyrInntekt(inntekt = 32000.månedlig, a1, 1.januar)

        assertEquals(1, observatør.avvisteRevurderinger.size)
        assertErrorTekst(inspektør, "Forespurt overstyring av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
    }

    @Test
    fun `avviser revurdering av inntekt for saker med 1 arbeidsgiver og ghost`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1.toString(), LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2.toString(), LocalDate.EPOCH, null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        håndterOverstyrInntekt(32000.månedlig, a1, 1.januar)
        assertEquals(1, observatør.avvisteRevurderinger.size)
        assertErrorTekst(inspektør, "Forespurt overstyring av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
    }

    @Test
    fun `Ved revurdering av inntekt til under krav til minste sykepengegrunnlag skal utbetaling opphøres`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, beregnetInntekt = 50000.årlig)
        håndterYtelser(1.vedtaksperiode)
        val inntekter = listOf(grunnlag(ORGNUMMER, 1.januar, 50000.årlig.repeat(3)))
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(ORGNUMMER, 1.januar, 50000.årlig.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterOverstyrInntekt(46000.årlig, skjæringstidspunkt = 1.januar) // da havner vi under greia
        håndterYtelser(1.vedtaksperiode)

        val utbetalinger = inspektør.utbetalinger
        assertEquals(1, utbetalinger.map { it.inspektør.arbeidsgiverOppdrag.fagsystemId() }.toSet().size)
        assertEquals(utbetalinger.first().inspektør.arbeidsgiverOppdrag.nettoBeløp(), -1 * utbetalinger.last().inspektør.arbeidsgiverOppdrag.nettoBeløp())
        assertEquals(2, utbetalinger.size)
    }

    @Test
    fun `revurder inntekt til under krav til minste sykepengegrunnlag slik at utbetaling opphører, og så revurder igjen til over krav til minste sykepengegrunnlag`() {
        val OverMinstegrense = 50000.årlig
        val UnderMinstegrense = 46000.årlig

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, beregnetInntekt = OverMinstegrense)
        håndterYtelser(1.vedtaksperiode)
        val inntekter = listOf(grunnlag(ORGNUMMER, 1.januar, OverMinstegrense.repeat(3)))
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(ORGNUMMER, 1.januar, OverMinstegrense.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterOverstyrInntekt(UnderMinstegrense, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterOverstyrInntekt(OverMinstegrense, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)

        val utbetalinger = inspektør.utbetalinger
        var opprinneligFagsystemId: String?
        utbetalinger[0].inspektør.arbeidsgiverOppdrag.apply {
            skalHaEndringskode(Endringskode.NY)
            opprinneligFagsystemId = fagsystemId()
            assertEquals(1, size)
            first().assertUtbetalingslinje(Endringskode.NY, 1, null, null)
        }
        utbetalinger[1].inspektør.arbeidsgiverOppdrag.apply {
            skalHaEndringskode(Endringskode.ENDR)
            assertEquals(opprinneligFagsystemId, fagsystemId())
            assertEquals(1, size)
            first().assertUtbetalingslinje(Endringskode.ENDR, 1, null, null, ønsketDatoStatusFom = 17.januar)
        }
        utbetalinger[2].inspektør.arbeidsgiverOppdrag.apply {
            skalHaEndringskode(Endringskode.ENDR)
            assertEquals(opprinneligFagsystemId, fagsystemId())
            assertEquals(1, size)
            first().assertUtbetalingslinje(Endringskode.NY, 2, 1, fagsystemId())
        }
        assertWarningTekst(inspektør, WARN_FORLENGER_OPPHØRT_OPPDRAG)
    }

    @Test
    fun `revurdering av inntekt delegeres til den første perioden som har en utbetalingstidslinje - arbeidsgiversøknad først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 15.februar, 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar))
        )
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        håndterOverstyrInntekt(skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_VILKÅRSPRØVING_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVSLUTTET
        )
    }

    @Test
    fun `revurdering av inntekt delegeres til den første perioden som har en utbetalingstidslinje - periode uten utbetaling først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 31.januar))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        håndterOverstyrInntekt(skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertEquals(Utbetaling.GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(0))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVSLUTTET,
            AVVENTER_VILKÅRSPRØVING_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVSLUTTET
        )
    }

    @Test
    fun `Alle perioder med aktuelt skjæringstidspunkt skal være stemplet med hendelseId`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        val overstyrInntektHendelseId = UUID.randomUUID()
        håndterOverstyrInntekt(skjæringstidspunkt = 1.januar, meldingsreferanseId = overstyrInntektHendelseId)
        assertHarHendelseIder(1.vedtaksperiode, overstyrInntektHendelseId)
        assertHarHendelseIder(2.vedtaksperiode, overstyrInntektHendelseId)
    }

    @Test
    fun `Kun perioder med aktuelt skjæringstidspunkt skal være stemplet med hendelseId`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        forlengVedtak(1.april, 30.april)
        val overstyrInntektHendelseId = UUID.randomUUID()
        håndterOverstyrInntekt(skjæringstidspunkt = 1.mars, meldingsreferanseId = overstyrInntektHendelseId)

        assertHarIkkeHendelseIder(1.vedtaksperiode, overstyrInntektHendelseId)
        assertHarHendelseIder(2.vedtaksperiode, overstyrInntektHendelseId)
        assertHarHendelseIder(3.vedtaksperiode, overstyrInntektHendelseId)
    }

    @Test
    fun `revurdere inntekt slik at det blir brukerutbetaling`() = Toggle.LageBrukerutbetaling.enable {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
    }

    @Test
    fun `revurdere inntekt slik at det blir brukerutbetaling uten å ha skrudd på brukerutbetaling `() =
        Toggle.LageBrukerutbetaling.disable {
            nyttVedtak(1.januar, 31.januar)
            håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, REVURDERING_FEILET)
        }

    @Test
    fun `revurdere inntekt slik at det blir delvis refusjon`() = listOf(Toggle.LageBrukerutbetaling, Toggle.DelvisRefusjon).enable {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), refusjon = Refusjon(25000.månedlig, null, emptyList()))
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
    }

    @Test
    fun `revurdere inntekt slik at det blir delvis refusjon uten å skrudd på brukerutbetaling eller delvis refusjon`() =
        listOf(Toggle.LageBrukerutbetaling, Toggle.DelvisRefusjon).disable {
            nyttVedtak(1.januar, 31.januar)
            håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), refusjon = Refusjon(25000.månedlig, null, emptyList()))
            håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, REVURDERING_FEILET)
        }

    @Test
    fun `revurdere inntekt slik at det blir delvis refusjon uten å skrudd på delvis refusjon`() =
        Toggle.LageBrukerutbetaling.enable {
            Toggle.DelvisRefusjon.disable {
                nyttVedtak(1.januar, 31.januar)
                håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), refusjon = Refusjon(25000.månedlig, null, emptyList()))
                håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
                håndterYtelser(1.vedtaksperiode)
                assertSisteTilstand(1.vedtaksperiode, REVURDERING_FEILET)
            }
        }

    @Test
    fun `revurdere mens en periode er til utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        forlengTilGodkjentVedtak(1.februar, 28.februar)
        håndterOverstyrInntekt(INNTEKT/2, skjæringstidspunkt = 1.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING)
    }

    @Test
    fun `revurdere mens en periode har feilet i utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        forlengTilGodkjentVedtak(1.februar, 28.februar)
        håndterUtbetalt(2.vedtaksperiode, status = Oppdragstatus.FEIL)
        håndterOverstyrInntekt(INNTEKT/2, skjæringstidspunkt = 1.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, UTBETALING_FEILET)
    }
}

private fun Oppdrag.skalHaEndringskode(kode: Endringskode, message: String = "") = accept(UtbetalingSkalHaEndringskode(kode, message))

private class UtbetalingSkalHaEndringskode(private val ønsketEndringskode: Endringskode, private val message: String = "") : OppdragVisitor {
    override fun preVisitOppdrag(
        oppdrag: Oppdrag,
        fagområde: Fagområde,
        fagsystemId: String,
        mottaker: String,
        førstedato: LocalDate,
        sistedato: LocalDate,
        sisteArbeidsgiverdag: LocalDate?,
        stønadsdager: Int,
        totalBeløp: Int,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime,
        endringskode: Endringskode,
        avstemmingsnøkkel: Long?,
        status: Oppdragstatus?,
        overføringstidspunkt: LocalDateTime?,
        erSimulert: Boolean,
        simuleringsResultat: Simulering.SimuleringResultat?
    ) {
        assertEquals(ønsketEndringskode, endringskode, message)
    }
}

private fun Utbetalingslinje.assertUtbetalingslinje(
    ønsketEndringskode: Endringskode,
    ønsketDelytelseId: Int,
    ønsketRefDelytelseId: Int? = null,
    ønsketRefFagsystemId: String? = null,
    ønsketDatoStatusFom: LocalDate? = null
) {
    val visitor = object : OppdragVisitor {
        override fun visitUtbetalingslinje(
            linje: Utbetalingslinje,
            fom: LocalDate,
            tom: LocalDate,
            stønadsdager: Int,
            totalbeløp: Int,
            satstype: Satstype,
            beløp: Int?,
            aktuellDagsinntekt: Int?,
            grad: Int?,
            delytelseId: Int,
            refDelytelseId: Int?,
            refFagsystemId: String?,
            endringskode: Endringskode,
            datoStatusFom: LocalDate?,
            statuskode: String?,
            klassekode: Klassekode
        ) {
            assertEquals(ønsketEndringskode, endringskode)
            assertEquals(ønsketDelytelseId, delytelseId)
            assertEquals(ønsketRefDelytelseId, refDelytelseId)
            assertEquals(ønsketRefFagsystemId, refFagsystemId)
            assertEquals(ønsketDatoStatusFom, datoStatusFom)
        }
    }
    accept(visitor)
}
