package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.DisableToggle
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.Kilde
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingslinjer.WARN_FORLENGER_OPPHØRT_OPPDRAG
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisableToggle(Toggle.NyRevurdering::class)
internal class OldRevurderInntektTest : AbstractEndToEndTest() {

    @Test
    fun `revurder inntekt happy case`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)

        val tidligereInntektInnslagId = inspektør.inntektInspektør.sisteInnslag?.innslagId

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 32000.månedlig, refusjon = Inntektsmelding.Refusjon(
            32000.månedlig,
            null,
            emptyList()
        )
        )
        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_SIMULERING_REVURDERING,
            TilstandType.AVVENTER_GODKJENNING_REVURDERING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
        )
        Assertions.assertEquals(15741, inspektør.utbetalinger.first().inspektør.arbeidsgiverOppdrag.nettoBeløp())
        Assertions.assertEquals(506, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.nettoBeløp())

        val grunnlagsdataInspektør = person.inspektør.grunnlagsdata(0).inspektør
        Assertions.assertEquals(2, person.inspektør.antallGrunnlagsdata())
        Assertions.assertEquals(3, grunnlagsdataInspektør.avviksprosent?.roundToInt())

        val tidligereBeregning = inspektør.utbetalingstidslinjeberegningData.first()
        Assertions.assertEquals(tidligereBeregning.inntektshistorikkInnslagId, tidligereInntektInnslagId)

        val beregning = inspektør.utbetalingstidslinjeberegningData.last()
        Assertions.assertEquals(
            beregning.inntektshistorikkInnslagId,
            inspektør.inntektInspektør.sisteInnslag?.innslagId
        )

        Assertions.assertEquals(
            beregning.vilkårsgrunnlagHistorikkInnslagId,
            person.nyesteIdForVilkårsgrunnlagHistorikk()
        )

        Assertions.assertTrue(inspektør.inntektInspektør.sisteInnslag?.opplysninger?.any { it.kilde == Kilde.SAKSBEHANDLER }
            ?: false)
    }

    @Test
    fun `revurder inntekt flere ganger`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 32000.månedlig, refusjon = Inntektsmelding.Refusjon(
            32000.månedlig,
            null,
            emptyList()
        )
        )
        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 31000.månedlig, refusjon = Inntektsmelding.Refusjon(
            31000.månedlig,
            null,
            emptyList()
        )
        )
        håndterOverstyrInntekt(inntekt = 31000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        Assertions.assertEquals(15741, inspektør.utbetalinger[0].inspektør.arbeidsgiverOppdrag.nettoBeløp())
        Assertions.assertEquals(506, inspektør.utbetalinger[1].inspektør.arbeidsgiverOppdrag.nettoBeløp())
        Assertions.assertEquals(-506, inspektør.utbetalinger[2].inspektør.arbeidsgiverOppdrag.nettoBeløp())

        val inntektFraSaksbehandler = inspektør.inntektInspektør.sisteInnslag?.opplysninger?.filter { it.kilde == Kilde.SAKSBEHANDLER }!!
        Assertions.assertEquals(1, inntektFraSaksbehandler.size)
        Assertions.assertEquals(31000.månedlig, inntektFraSaksbehandler.first().sykepengegrunnlag)
    }

    @Test
    fun `revurder inntekt ukjent skjæringstidspunkt`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        assertThrows<Aktivitetslogg.AktivitetException> {
            håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 2.januar)
        }
        assertSevere("Kan ikke overstyre inntekt hvis vi ikke har en arbeidsgiver med sykdom for skjæringstidspunktet", AktivitetsloggFilter.person())

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
        )

        Assertions.assertEquals(1, inspektør.utbetalinger.size)
    }

    @Test
    fun `revurder inntekt tidligere skjæringstidspunkt`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        nyttVedtak(1.mars, 31.mars, 100.prosent)

        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
        )

        assertTilstander(
            1,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
        )

        Assertions.assertEquals(2, inspektør.utbetalinger.size)
    }

    @Test
    fun `overstyr inntekt to vedtak med kort opphold`() {
        nyttVedtak(1.januar, 26.januar, 100.prosent)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 14.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 14.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.februar)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)

        assertForventetFeil(
            forklaring = "Denne er skrudd av i påvente av at vi skal støtte revurdering over skjæringstidspunkt",
            nå = {
                assertTilstander(
                    1.vedtaksperiode,
                    TilstandType.START,
                    TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                    TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
                    TilstandType.AVVENTER_HISTORIKK,
                    TilstandType.AVVENTER_VILKÅRSPRØVING,
                    TilstandType.AVVENTER_HISTORIKK,
                    TilstandType.AVVENTER_SIMULERING,
                    TilstandType.AVVENTER_GODKJENNING,
                    TilstandType.TIL_UTBETALING,
                    TilstandType.AVSLUTTET
                )

                assertTilstander(
                    2.vedtaksperiode,
                    TilstandType.START,
                    TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                    TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
                    TilstandType.AVVENTER_HISTORIKK,
                    TilstandType.AVVENTER_VILKÅRSPRØVING,
                    TilstandType.AVVENTER_HISTORIKK,
                    TilstandType.AVVENTER_SIMULERING,
                    TilstandType.AVVENTER_GODKJENNING,
                    TilstandType.TIL_UTBETALING,
                    TilstandType.AVSLUTTET
                )

                Assertions.assertEquals(2, inspektør.utbetalinger.filter { it.inspektør.erUtbetalt }.size)
            },
            ønsket = {
                håndterYtelser(1.vedtaksperiode)
                håndterYtelser(2.vedtaksperiode)
                håndterSimulering(2.vedtaksperiode)
                håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
                håndterUtbetalt()
                assertTilstander(
                    1.vedtaksperiode,
                    TilstandType.START,
                    TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                    TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
                    TilstandType.AVVENTER_HISTORIKK,
                    TilstandType.AVVENTER_VILKÅRSPRØVING,
                    TilstandType.AVVENTER_HISTORIKK,
                    TilstandType.AVVENTER_SIMULERING,
                    TilstandType.AVVENTER_GODKJENNING,
                    TilstandType.TIL_UTBETALING,
                    TilstandType.AVSLUTTET,
                    TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
                    TilstandType.AVVENTER_HISTORIKK_REVURDERING,
                    TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
                    TilstandType.AVSLUTTET
                )

                assertTilstander(
                    2.vedtaksperiode,
                    TilstandType.START,
                    TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                    TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
                    TilstandType.AVVENTER_HISTORIKK,
                    TilstandType.AVVENTER_VILKÅRSPRØVING,
                    TilstandType.AVVENTER_HISTORIKK,
                    TilstandType.AVVENTER_SIMULERING,
                    TilstandType.AVVENTER_GODKJENNING,
                    TilstandType.TIL_UTBETALING,
                    TilstandType.AVSLUTTET,
                    TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
                    TilstandType.AVVENTER_HISTORIKK_REVURDERING,
                    TilstandType.AVVENTER_SIMULERING_REVURDERING,
                    TilstandType.AVVENTER_GODKJENNING_REVURDERING,
                    TilstandType.TIL_UTBETALING,
                    TilstandType.AVSLUTTET
                )

                Assertions.assertEquals(3, inspektør.utbetalinger.filter { it.inspektør.erUtbetalt }.size)
            }
        )
    }

    @Test
    fun `kan ikke revurdere inntekt på vedtak med opphold og nytt skjæringstidspunkt etterpå`() {
        nyttVedtak(1.januar, 26.januar, 100.prosent)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 14.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 14.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.februar)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET
        )

        assertTilstander(
            1,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET
        )
        assertError("Kan kun revurdere siste skjæringstidspunkt")
    }

    @Test
    fun `revurder inntekt avvik over 25 prosent reduksjon`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterOverstyrInntekt(inntekt = 7000.månedlig, skjæringstidspunkt = 1.januar)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING
        )

        assertWarning("Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene.",
            AktivitetsloggFilter.person()
        )
        Assertions.assertEquals(1, inspektør.utbetalinger.size)
    }

    @Test
    fun `revurder inntekt avvik over 25 prosent økning`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterOverstyrInntekt(inntekt = 70000.månedlig, skjæringstidspunkt = 1.januar)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING
        )

        assertWarning("Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene.",
            AktivitetsloggFilter.person()
        )
        Assertions.assertEquals(1, inspektør.utbetalinger.size)
    }

    @Test
    fun `revurder inntekt ny inntekt under en halv G`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterOverstyrInntekt(inntekt = 3000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_SIMULERING_REVURDERING,
            TilstandType.AVVENTER_GODKJENNING_REVURDERING
        )

        val utbetalingTilRevurdering = inspektør.utbetalinger.last()
        Assertions.assertEquals(2, inspektør.utbetalinger.size)
        Assertions.assertEquals(-15741, utbetalingTilRevurdering.inspektør.arbeidsgiverOppdrag.nettoBeløp())

        assertWarning(
            "Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene.",
            AktivitetsloggFilter.person()
        )
        assertWarning(
            "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag",
            AktivitetsloggFilter.person()
        )
        Assertions.assertFalse(utbetalingTilRevurdering.utbetalingstidslinje().harUtbetalinger())
    }

    @Test
    fun `utbetaling_utbetalt tar med vedtaksperiode-ider for ett enkelt vedtak`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)

        val utbetalingEvent = observatør.utbetalingMedUtbetalingEventer.first()

        Assertions.assertEquals(1, utbetalingEvent.vedtaksperiodeIder.size)
        Assertions.assertEquals(1.vedtaksperiode.id(ORGNUMMER), utbetalingEvent.vedtaksperiodeIder.first())
    }

    @Test
    fun `utbetaling_utbetalt tar med vedtaksperiode-ider for flere vedtak`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        forlengVedtak(1.februar, 28.februar)

        val førsteEvent = observatør.utbetalingMedUtbetalingEventer.first()
        val andreEvent = observatør.utbetalingMedUtbetalingEventer.last()

        Assertions.assertEquals(1, førsteEvent.vedtaksperiodeIder.size)
        Assertions.assertEquals(1.vedtaksperiode.id(ORGNUMMER), førsteEvent.vedtaksperiodeIder.first())
        Assertions.assertEquals(1, andreEvent.vedtaksperiodeIder.size)
        Assertions.assertEquals(2.vedtaksperiode.id(ORGNUMMER), andreEvent.vedtaksperiodeIder.first())
    }

    @Test
    fun `utbetaling_utbetalt tar med vedtaksperiode-ider for revurdering over flere perioder`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        forlengVedtak(1.februar, 28.februar)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 32000.månedlig, refusjon = Inntektsmelding.Refusjon(
            32000.månedlig,
            null,
            emptyList()
        )
        )
        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)

        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt()

        val utbetalingsevent = observatør.utbetalingMedUtbetalingEventer.last()

        Assertions.assertEquals(2, utbetalingsevent.vedtaksperiodeIder.size)
        Assertions.assertTrue(utbetalingsevent.vedtaksperiodeIder.contains(1.vedtaksperiode.id(ORGNUMMER)))
        Assertions.assertTrue(utbetalingsevent.vedtaksperiodeIder.contains(2.vedtaksperiode.id(ORGNUMMER)))
    }

    @Test
    fun `utbetaling_utbetalt tar med vedtaksperiode-ider for forkastede perioder`() {
        tilGodkjent(1.januar, 31.januar, 100.prosent, 1.januar)
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), andreInntektskilder = listOf(
                Søknad.Inntektskilde(true, "FRILANSER")
            ))
        håndterUtbetalt()

        val utbetalingEvent = observatør.utbetalingMedUtbetalingEventer.first()

        Assertions.assertEquals(1, utbetalingEvent.vedtaksperiodeIder.size)
        Assertions.assertEquals(1.vedtaksperiode.id(ORGNUMMER), utbetalingEvent.vedtaksperiodeIder.first())
    }


    @Test
    fun `avviser revurdering av inntekt for saker med flere arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        håndterOverstyrInntekt(inntekt = 32000.månedlig, a1, 1.januar)

        Assertions.assertEquals(1, observatør.avvisteRevurderinger.size)
        assertError("Forespurt overstyring av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
    }

    @Test
    fun `avviser revurdering av inntekt for saker med 1 arbeidsgiver og ghost`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
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
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterOverstyrInntekt(32000.månedlig, a1, 1.januar)
        Assertions.assertEquals(1, observatør.avvisteRevurderinger.size)
        assertError("Forespurt overstyring av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
    }

    @Test
    fun `Ved revurdering av inntekt til under krav til minste sykepengegrunnlag skal utbetaling opphøres`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, beregnetInntekt = 50000.årlig)
        håndterYtelser(1.vedtaksperiode)
        val inntekter = listOf(grunnlag(ORGNUMMER, 1.januar, 50000.årlig.repeat(3)))
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(ORGNUMMER, 1.januar, 50000.årlig.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(46000.årlig, skjæringstidspunkt = 1.januar) // da havner vi under greia
        håndterYtelser(1.vedtaksperiode)

        val utbetalinger = inspektør.utbetalinger
        Assertions.assertEquals(1, utbetalinger.map { it.inspektør.arbeidsgiverOppdrag.fagsystemId() }.toSet().size)
        Assertions.assertEquals(
            utbetalinger.first().inspektør.arbeidsgiverOppdrag.nettoBeløp(),
            -1 * utbetalinger.last().inspektør.arbeidsgiverOppdrag.nettoBeløp()
        )
        Assertions.assertEquals(2, utbetalinger.size)
    }

    @Test
    fun `revurder inntekt til under krav til minste sykepengegrunnlag slik at utbetaling opphører, og så revurder igjen til over krav til minste sykepengegrunnlag`() {
        val OverMinstegrense = 50000.årlig
        val UnderMinstegrense = 46000.årlig

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, beregnetInntekt = OverMinstegrense)
        håndterYtelser(1.vedtaksperiode)
        val inntekter = listOf(grunnlag(ORGNUMMER, 1.januar, OverMinstegrense.repeat(3)))
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(ORGNUMMER, 1.januar, OverMinstegrense.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(UnderMinstegrense, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(OverMinstegrense, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)

        val utbetalinger = inspektør.utbetalinger
        var opprinneligFagsystemId: String?
        utbetalinger[0].inspektør.arbeidsgiverOppdrag.apply {
            skalHaEndringskode(Endringskode.NY)
            opprinneligFagsystemId = fagsystemId()
            Assertions.assertEquals(1, size)
            first().assertUtbetalingslinje(Endringskode.NY, 1, null, null)
        }
        utbetalinger[1].inspektør.arbeidsgiverOppdrag.apply {
            skalHaEndringskode(Endringskode.ENDR)
            Assertions.assertEquals(opprinneligFagsystemId, fagsystemId())
            Assertions.assertEquals(1, size)
            first().assertUtbetalingslinje(Endringskode.ENDR, 1, null, null, ønsketDatoStatusFom = 17.januar)
        }
        utbetalinger[2].inspektør.arbeidsgiverOppdrag.apply {
            skalHaEndringskode(Endringskode.ENDR)
            Assertions.assertEquals(opprinneligFagsystemId, fagsystemId())
            Assertions.assertEquals(1, size)
            first().assertUtbetalingslinje(Endringskode.NY, 2, 1, fagsystemId())
        }
        assertWarning(WARN_FORLENGER_OPPHØRT_OPPDRAG, AktivitetsloggFilter.person())
    }

    @Test
    fun `revurdering av inntekt delegeres til den første perioden som har en utbetalingstidslinje - arbeidsgiversøknad først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(16.januar, 15.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(16.januar, 15.februar, 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar))
        )
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVSLUTTET_UTEN_UTBETALING,
            TilstandType.AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_GODKJENNING_REVURDERING,
            TilstandType.AVSLUTTET
        )
    }

    @Test
    fun `revurdering av inntekt delegeres til den første perioden som har en utbetalingstidslinje - periode uten utbetaling først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            Søknad.Søknadsperiode.Ferie(1.januar, 31.januar)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        Assertions.assertEquals(2, inspektør.utbetalinger.size)
        Assertions.assertEquals(0, inspektør.utbetalinger(1.vedtaksperiode).size)
        Assertions.assertEquals(2, inspektør.utbetalinger(2.vedtaksperiode).size)
        Assertions.assertEquals(Utbetaling.Utbetalt, inspektør.utbetalingtilstand(0))
        Assertions.assertEquals(Utbetaling.GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(1))
        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVSLUTTET_UTEN_UTBETALING,
            TilstandType.AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_GODKJENNING_REVURDERING,
            TilstandType.AVSLUTTET
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
    fun `revurdere inntekt slik at det blir brukerutbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
    }

    @Test
    fun `revurdere inntekt slik at det blir brukerutbetaling `() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `revurdere inntekt slik at det blir delvis refusjon`() {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), refusjon = Inntektsmelding.Refusjon(
            25000.månedlig,
            null,
            emptyList()
        )
        )
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `revurdere mens en periode er til utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        forlengTilGodkjentVedtak(1.februar, 28.februar)
        håndterOverstyrInntekt(INNTEKT /2, skjæringstidspunkt = 1.januar)
        assertTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET
        )
        assertTilstander(2.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING
        )
    }

    @Test
    fun `revurdere mens en periode har feilet i utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        forlengTilGodkjentVedtak(1.februar, 28.februar)
        håndterUtbetalt(status = Oppdragstatus.FEIL)
        håndterOverstyrInntekt(INNTEKT /2, skjæringstidspunkt = 1.januar)
        assertTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET
        )
        assertTilstander(2.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.UTBETALING_FEILET
        )
    }

    private fun Oppdrag.skalHaEndringskode(kode: Endringskode, message: String = "") = accept(UtbetalingSkalHaEndringskode(kode, message))

    private class UtbetalingSkalHaEndringskode(private val ønsketEndringskode: Endringskode, private val message: String = "") :
        OppdragVisitor {
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
            Assertions.assertEquals(ønsketEndringskode, endringskode, message)
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
                Assertions.assertEquals(ønsketEndringskode, endringskode)
                Assertions.assertEquals(ønsketDelytelseId, delytelseId)
                Assertions.assertEquals(ønsketRefDelytelseId, refDelytelseId)
                Assertions.assertEquals(ønsketRefFagsystemId, refFagsystemId)
                Assertions.assertEquals(ønsketDatoStatusFom, datoStatusFom)
            }
        }
        accept(visitor)
    }
}