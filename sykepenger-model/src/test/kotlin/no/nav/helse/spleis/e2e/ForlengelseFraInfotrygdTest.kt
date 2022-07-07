package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.etterspurteBehov
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.inspectors.søppelbøtte
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Punktum.Companion.punktum
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.september
import no.nav.helse.serde.api.speil.builders.OppsamletSammenligningsgrunnlagBuilder
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

@Disabled
internal class ForlengelseFraInfotrygdTest : AbstractEndToEndTest() {

    @Test
    fun `tillater ikke å overta brukerutbetaling-saker fra Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        val im = håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INGEN, null))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(im, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, false)
        ), statslønn = true)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `Lager ikke utbetalinger for vedtaksperioder hos andre arbeidsgivere som ligger senere i tid enn den som er først totalt sett`() {
        val periode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 1.januar(2021), INNTEKT, true)
        )
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar(2021), 31.januar(2021), 100.prosent, INNTEKT)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1, inntektshistorikk = inntektshistorikk)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        val periode2 = 1.mars(2021) til 31.mars(2021)
        val a2Periode = 2.april(2021) til 30.april(2021)
        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(a2Periode.start, a2Periode.endInclusive, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1, inntektshistorikk = inntektshistorikk)

        assertEquals(0, inspektør(a2).ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
        assertEquals(0, inspektør(a2).avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
    }



    @Test
    fun `oppdager forlengelse`()  {
        nyPeriode(1.januar til 31.januar)
        person.invaliderAllePerioder(hendelselogg, null)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 19.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.februar, 25.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(26.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 19.februar, 100.prosent))
        håndterSøknad(Sykdom(20.februar, 25.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)
        ))
        håndterYtelser(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `forlenger vedtaksperiode som har gått til infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 3.januar, INNTEKT, true))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser(2.vedtaksperiode)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertEquals(3.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(Periodetype.OVERGANG_FRA_IT, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `forlenger ikke vedtaksperiode som har gått til infotrygd, der utbetaling ikke er gjort`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 19.januar, 25.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 19.januar(2018), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)  // <-- TIL_INFOTRYGD
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 23.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 29.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `avdekker tilstøtende periode i Infotrygd`() {
        nyPeriode(3.januar til 26.januar)
        person.invaliderAllePerioder(hendelselogg, null)
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 23.februar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntekter = listOf(
            Inntektsopplysning(
                ORGNUMMER,
                3.januar,
                INNTEKT,
                true
            )
        )
        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(
            2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING
        )
        assertEquals(3.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
    }

    @Test
    fun `når en periode går Til Infotrygd avsluttes påfølgende, tilstøtende perioder også`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(18.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        person.søppelbøtte(hendelselogg, 1.januar til 31.januar)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, 1000.daglig)
        )
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TIL_INFOTRYGD
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `tidligere utbetalinger i spleis som er forkastet blir tatt med som en del av utbetalingshistorikken`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
            assertEquals(1, it.size)
            assertEquals(Periode(1.januar, 31.januar), it.first())
        }

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)
        inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
            assertEquals(1, it.size)
            assertEquals(Periode(1.januar, 31.januar), it.first())
        }

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            3.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true))
        )
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
            assertEquals(2, it.size)
            assertEquals(Periode(1.januar, 31.januar), it[0])
            assertEquals(Periode(1.mars, 31.mars), it[1])
        }

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertEquals(28.desember, inspektør.sisteMaksdato(1.vedtaksperiode))
        assertEquals(inspektør.sisteMaksdato(3.vedtaksperiode), inspektør.sisteMaksdato(1.vedtaksperiode))
    }

    @Test
    fun `lager ikke ny arbeidsgiverperiode når det er tilstøtende historikk`() {
        håndterSykmelding(Sykmeldingsperiode(18.februar(2020), 3.mars(2020), 100.prosent))
        håndterSøknad(Sykdom(18.februar(2020), 3.mars(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(4.mars(2020), 17.mars(2020), 100.prosent))
        håndterSøknad(Sykdom(4.mars(2020), 17.mars(2020), 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(18.mars(2020), 15.april(2020), 70.prosent))
        håndterSøknad(Sykdom(18.mars(2020), 15.april(2020), 70.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterInntektsmelding(listOf(Periode(18.februar(2020), 4.mars(2020))), 18.februar(2020))

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.februar(2019) til 1.januar(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, AVVENTER_GODKJENNING, LocalDateTime.now().minusDays(110)) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(16.april(2020), 7.mai(2020), 50.prosent))
        håndterSøknad(Sykdom(16.april(2020), 7.mai(2020), 50.prosent))

        håndterUtbetalingshistorikk(
            4.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.mars(2020), 17.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 18.mars(2020), 15.april(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    ORGNUMMER,
                    5.mars(2020), INNTEKT, true
                )
            )
        )
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            TIL_INFOTRYGD
        )
        assertTilstander(
            4.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        inspektør.utbetalinger.aktive().also { utbetalinger ->
            assertEquals(1, utbetalinger.size)
            utbetalinger.first().utbetalingstidslinje().inspektør.also {
                assertEquals(15, it.arbeidsgiverperiodeDagTeller)
                assertEquals(16, it.navDagTeller)
            }
        }
    }

    @Test
    fun `Forlengelse av søknad med utbetaling med opphold betalt i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(10.mai(2020), 2.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(10.mai(2020), 2.juni(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(10.mai(2020), 25.mai(2020))), førsteFraværsdag = 10.mai(2020))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.mai(2019) til 1.april(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        nyPeriode(3.juni(2020) til 21.juni(2020))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            3.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.juni(2020), 21.juni(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 3.juni(2020), INNTEKT, true))
        )
        håndterYtelser(3.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertFalse(inspektør.periodeErForkastet(3.vedtaksperiode))
        assertTrue(person.personLogg.etterspurteBehov(1.vedtaksperiode, Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering))
        inspektør.apply {
            assertTrue(
                utbetalingstidslinjer(3.vedtaksperiode)
                    .filterIsInstance<ArbeidsgiverperiodeDag>().isEmpty()
            )
            assertTrue(
                utbetalingstidslinjer(1.vedtaksperiode)
                    .filterIsInstance<ArbeidsgiverperiodeDag>().isNotEmpty()
            )
        }
    }

    @Test
    fun `periode utvides ikke tilbake til arbeidsgiverperiode dersom det er gap mellom`() {
        nyPeriode(17.januar til 31.januar)
        person.invaliderAllePerioder(hendelselogg, null)
        håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(2.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 2.februar)
        val historikk = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, 1000.daglig))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar(2018), 1000.daglig, true))
        håndterYtelser(2.vedtaksperiode, *historikk, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertEquals(1, inspektør.utbetalinger.size)
        assertEquals(2.februar til 28.februar, inspektør.periode(2.vedtaksperiode))
        inspektør.utbetalinger.first().utbetalingstidslinje().also { utbetalingstidslinje ->
            assertAlleDager(utbetalingstidslinje, 1.januar til 16.januar, ArbeidsgiverperiodeDag::class)
            assertAlleDager(utbetalingstidslinje, 17.januar til 1.februar, UkjentDag::class, Arbeidsdag::class)
            assertAlleDager(utbetalingstidslinje, 2.februar til 28.februar, NavDag::class, NavHelgDag::class)
        }
    }

    private fun assertAlleDager(utbetalingstidslinje: Utbetalingstidslinje, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        utbetalingstidslinje.subset(periode).also { tidslinje ->
            assertTrue(tidslinje.all { it::class in dager }) {
                val ulikeDager = tidslinje.filter { it::class !in dager }
                "Forventet at alle dager skal være en av: ${dager.joinToString { it.simpleName ?: "UKJENT" }}.\n" +
                    ulikeDager.joinToString(prefix = "  - ", separator = "\n  - ", postfix = "\n") {
                        "${it.dato} er ${it::class.simpleName}"
                    } + "\nUtbetalingstidslinje:\n" + tidslinje.toString() + "\n"
            }
        }
    }

    @Test
    fun `maksdato blir riktig i ping-pong-perioder`() {
        val historikk1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 20.november(2019), 3.januar(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 4.januar(2020), 31.januar(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar(2020), 14.februar(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 15.februar(2020), 3.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 4.mars(2020), 20.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 21.mars(2020), 17.april(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 18.april(2020), 8.mai(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 9.mai(2020), 29.mai(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning1 = listOf(
            Inntektsopplysning(ORGNUMMER, 20.november(2019), INNTEKT, true)
        )

        håndterSykmelding(Sykmeldingsperiode(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            *historikk1.toTypedArray(),
            inntektshistorikk = inntektsopplysning1,
            besvart = LocalDate.EPOCH.atStartOfDay()
        )
        håndterYtelser(
            1.vedtaksperiode,
            utbetalinger = historikk1.toTypedArray(),
            inntektshistorikk = inntektsopplysning1,
            besvart = LocalDate.EPOCH.atStartOfDay()
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        val historikk2 = historikk1 + listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 22.juni(2020), 9.juli(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juli(2020), 31.juli(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.august(2020), 17.august(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning2 = inntektsopplysning1 + listOf(
            Inntektsopplysning(ORGNUMMER, 22.juni(2020), INNTEKT, true)
        )

        håndterSykmelding(Sykmeldingsperiode(18.august(2020), 2.september(2020), 100.prosent))
        håndterSøknad(Sykdom(18.august(2020), 2.september(2020), 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historikk2.toTypedArray(), inntektshistorikk = inntektsopplysning2)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)


        håndterSykmelding(Sykmeldingsperiode(3.september(2020), 30.september(2020), 100.prosent))
        håndterSøknad(Sykdom(3.september(2020), 30.september(2020), 100.prosent))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(1.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(2.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(3.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(3.vedtaksperiode))
    }

    @Test
    fun `Person uten refusjon til arbeidsgiver blir ikke behandlet i Spleis`() { // TODO
        nyPeriode(7.oktober(2019) til 22.oktober(2020))
        person.invaliderAllePerioder(hendelselogg, null)
        // seeder personen med historisk refusjonsopplysning
        håndterInntektsmelding(listOf(7.oktober(2019) til 23.oktober(2019)), refusjon = Inntektsmelding.Refusjon(null, null))

        håndterSykmelding(Sykmeldingsperiode(23.oktober(2020), 18.november(2020), 100.prosent))
        håndterSøknad(Sykdom(23.oktober(2020), 18.november(2020), 100.prosent))
        val historikk = arrayOf(
            PersonUtbetalingsperiode(ORGNUMMER, 7.oktober(2019), 1.juli(2020), 100.prosent, 1000.daglig),
            Friperiode(2.juli(2020), 2.september(2020)),
            PersonUtbetalingsperiode(ORGNUMMER, 3.september(2020), 22.oktober(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning = listOf(
            Inntektsopplysning(ORGNUMMER, 7.oktober(2019), INNTEKT, false)
        )
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historikk, inntektshistorikk = inntektsopplysning)
        håndterYtelser(2.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `ping-pong hvor infotrygd perioden slutter på maksdato skal ikke føre til en automatisk annullering`() {
        nyPeriode(1.januar til 31.mai)
        person.invaliderAllePerioder(hendelselogg, null)
        val fom1 = 1.juni
        val tom1 = 30.juni
        håndterSykmelding(Sykmeldingsperiode(fom1, tom1, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(fom1, tom1, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.mai, 100.prosent, 1200.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1200.daglig, true, null))
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        nyPeriode(1.juli til 12.desember)
        person.invaliderAllePerioder(hendelselogg, null)
        val fom2 = 13.desember
        val tom2 = 31.desember
        håndterSykmelding(Sykmeldingsperiode(fom2, tom2, 100.prosent))
        håndterSøknadMedValidering(4.vedtaksperiode, Sykdom(fom2, tom2, 100.prosent))
        håndterUtbetalingshistorikk(
            4.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.mai, 100.prosent, 1200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.juli, 12.desember, 100.prosent, 1200.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1200.daglig, true, null),
                Inntektsopplysning(ORGNUMMER, 1.juli, 1200.daglig, true, null)
            )
        )
        håndterYtelser(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode)

        assertTilstander(
            4.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
        assertEquals(Utbetaling.GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(1))
    }

    @Test
    fun `Ping-pong med ferie mellom Infotrygd-perioder skal ikke beregne nye skjæringstidspunkt etter ferien`() {
        nyPeriode(10.juni(2020) til 25.juni(2020))
        person.invaliderAllePerioder(hendelselogg, null)
        håndterSykmelding(Sykmeldingsperiode(26.juni(2020), 26.juli(2020), 60.prosent))
        håndterSøknad(Sykdom(26.juni(2020), 26.juli(2020), 60.prosent))
        val historikk1 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juni(2020), 25.juni(2020), 100.prosent, 1200.daglig),
        )
        val inntektsopplysning1 = listOf(
            Inntektsopplysning(ORGNUMMER, 10.juni(2020), INNTEKT, true)
        )
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historikk1, inntektshistorikk = inntektsopplysning1)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(12.oktober(2020), 8.november(2020), 50.prosent))
        håndterSøknad(Sykdom(12.oktober(2020), 8.november(2020), 50.prosent))

        val historikk2 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juni(2020), 25.juni(2020), 60.prosent, 1200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 27.juli(2020), 6.september(2020), 60.prosent, 1200.daglig),
            Friperiode(7.september(2020), 8.september(2020)),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 9.september(2020), 13.september(2020), 60.prosent, 1200.daglig),
            Friperiode(14.september(2020), 15.september(2020)),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 16.september(2020), 11.oktober(2020), 60.prosent, 1200.daglig)
        )
        val inntektsopplysning2 = listOf(
            Inntektsopplysning(ORGNUMMER, 10.juni(2020), INNTEKT, true),
            Inntektsopplysning(ORGNUMMER, 27.juli(2020), INNTEKT, true)
        )
        håndterUtbetalingshistorikk(3.vedtaksperiode, *historikk2, inntektshistorikk = inntektsopplysning2)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(9.november(2020), 6.desember(2020), 60.prosent))
        håndterSøknad(Sykdom(9.november(2020), 6.desember(2020), 60.prosent))
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode)
        håndterUtbetalt()
        assertTilstander(
            4.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Sender perioden til Infotrygd hvis inntekt mangler ved bygging av utbetalingstidslinje`() {
        nyPeriode(10.januar(2020) til 25.januar(2020))
        nyPeriode(10.juni(2020) til 25.juni(2020))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(26.juni(2020), 26.juli(2020), 60.prosent))
        håndterSøknad(Sykdom(26.juni(2020), 26.juli(2020), 60.prosent))
        val historikk = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.januar(2020), 25.januar(2020), 100.prosent, 1200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juni(2020), 25.juni(2020), 100.prosent, 1200.daglig)
        )
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 10.juni(2020), INNTEKT, true))
        håndterUtbetalingshistorikk(3.vedtaksperiode, *historikk, inntektshistorikk = inntektshistorikk)

        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Forlengelse oppdager ferie fra infotrygd`() {
        /* Vi ser at hvis vi oppdager ferie i infotrygd ender vi opp med ukjente dager i utbetalingstidslinja.
           Dette fører til en annullering i oppdraget. */
        nyPeriode(3.januar til 26.januar)
        person.invaliderAllePerioder(hendelselogg, null)
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 3.januar, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent), Ferie(16.februar, 17.februar))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk, besvart = LocalDateTime.now().minusHours(24))
        håndterYtelser(2.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk, besvart = LocalDateTime.now().minusHours(24))
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertTilstander(
            2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(24.februar, 15.mars, 100.prosent))
        håndterSøknadMedValidering(3.vedtaksperiode, Sykdom(24.februar, 15.mars, 100.prosent))
        håndterYtelser(3.vedtaksperiode, historikk, Friperiode(16.februar, 17.februar), inntektshistorikk = inntektshistorikk)
        håndterSimulering(3.vedtaksperiode)
        assertEquals(2, inspektør.arbeidsgiverOppdrag[1].linjerUtenOpphør().size)
    }

    @Test
    fun `infotrygd overtar periode med arbeidsgiverperiode 2`() {
        håndterSykmelding(Sykmeldingsperiode(22.januar, 28.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 6.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 6.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(7.februar, 7.februar, 100.prosent))
        håndterSøknad(Sykdom(7.februar, 7.februar, 100.prosent))
        person.søppelbøtte(hendelselogg, 7.februar til 7.februar) // perioden fikk en error som trigget utkastelse
        håndterSykmelding(Sykmeldingsperiode(8.februar, 25.februar, 100.prosent))
        håndterSøknad(Sykdom(8.februar, 25.februar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 7.februar, 7.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 7.februar, INNTEKT, true)
        ))
        håndterYtelser(3.vedtaksperiode)
        val utbetaling = inspektør.utbetaling(0).inspektør
        assertEquals(8.februar til 25.februar, utbetaling.periode)
        assertTrue(utbetaling.utbetalingstidslinje[8.februar] is NavDag)
    }

    @Test
    fun `infotrygd overtar periode med arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 18.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 18.januar, 100.prosent))
        person.søppelbøtte(hendelselogg, 11.januar til 18.januar) // perioden fikk en error som trigget utkastelse
        håndterSykmelding(Sykmeldingsperiode(19.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(19.januar, 25.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 18.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
        ))
        håndterYtelser(3.vedtaksperiode)
        val utbetaling = inspektør.utbetaling(0).inspektør
        assertEquals(19.januar til 25.januar, utbetaling.periode)
        assertTrue(utbetaling.utbetalingstidslinje[19.januar] is NavDag)
    }

    @Test
    fun `infotrygd overtar periode med arbeidsgiverperiode med opphold`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 22.januar, 100.prosent))
        person.søppelbøtte(hendelselogg, 18.januar til 22.januar) // perioden fikk en error som trigget utkastelse
        håndterSykmelding(Sykmeldingsperiode(23.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(23.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 22.januar, 22.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 22.januar, INNTEKT, true)
        ))
        håndterYtelser(3.vedtaksperiode)
        val utbetaling = inspektør.utbetaling(0).inspektør
        assertEquals(23.januar til 31.januar, utbetaling.periode)
        assertTrue(utbetaling.utbetalingstidslinje[23.januar] is NavDag)
    }

    @Test
    fun `infotrygd forlengelse med skjæringstidspunkt vi allerede har vilkårsvurdert burde bruke vilkårsgrunnlag fra infotrygd`() {
        tilGodkjenning(fom = 1.januar, tom = 31.januar, grad = 100.prosent, førsteFraværsdag = 1.januar)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertInstanceOf(VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag::class.java, person.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `IT forlengelse hvor arbeidsgiver har endret orgnummer og vi har fått nye inntektsopplysninger fra IT ved skjæringstidspunktet`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(a1, 1.januar, INNTEKT, true)),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        val vilkårsgrunnlag = person.vilkårsgrunnlagFor(1.januar)
        håndterAnnullerUtbetaling(a1, inspektør.fagsystemId(1.vedtaksperiode))
        håndterUtbetalt(orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a2, 1.januar, 31.januar, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 1.februar, 28.februar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(a2, 1.januar, INNTEKT, true)),
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertNotSame(vilkårsgrunnlag, person.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `overskriver ikke spleis vilkårsgrunnlag pga inntekt fra IT dersom vi allerede har en utbetalt periode i spleis`()  {
        nyPeriode(1.januar til 31.januar, a1)
        person.invaliderAllePerioder(hendelselogg, null)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(a1, 1.januar, INNTEKT, true)),
            orgnummer = a1
        )
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        val vilkårsgrunnlag = person.vilkårsgrunnlagFor(1.januar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a2, 1.januar, 31.januar, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 1.februar, 28.februar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(a2, 1.januar, INNTEKT, true)),
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertSame(vilkårsgrunnlag, person.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `lagrer ikke inntekt fra infotrygd uten utbetaling som vilkårsgrunnlag i spleis`() {
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = inntektshistorikk)
        håndterInntektsmelding(listOf(1.januar til 31.januar))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = inntektshistorikk)
        assertTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `Teller ikke med dager fra opphørte utbetalingslinjer`() {
        byggPersonMedOpphør()
        val beregner = Feriepengeberegner(UNG_PERSON_FNR_2018.alder(), Year.of(2018), utbetalingshistorikkForFeriepenger(), person)
        assertEquals((16.januar til 28.februar).filterNot { it.erHelg() }, beregner.feriepengedatoer())
    }

    @Test
    fun `forlengelse fra IT skal ikke kobles med periode i AvsluttetUtenUtbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        person.invaliderAllePerioder(Aktivitetslogg(), null)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
        ))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))
        assertEquals(Periodetype.OVERGANG_FRA_IT, inspektør.periodetype(3.vedtaksperiode))
    }

    @Test
    fun `forlengelse fra infotrygd hvor arbeidsgiverdager blir igjen fra IM etter tidligere forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent))
        håndterInntektsmelding(listOf(9.januar til 24.januar))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        /*
        Dette vil kaste ut den første perioden, men siden vi har en periode etter tom-datoen til denne perioden vil beholde arbeidsgiverperiodedager fra IM
        Perioden overlapper med en dag(22.januar), dagene 23. og 24. januar blir igjen fra IM i arbeidsgiverens sykdomstidslinje
         */
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))

        håndterInntektsmelding(listOf(1.februar til 16.februar))

        // Perioden i infotrygd trenger ikke nødvendigvis å tilstøte, men det _må_ være arbeidsgiverperiodedager før første utbetalte dag i IT
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 25.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 25.januar, INNTEKT, true))
        )
        håndterYtelser(2.vedtaksperiode)

        assertEquals(9.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `Ny utbetalingsbuilder feiler ikke når sykdomshistorikk inneholder arbeidsgiverperiode som hører til infotrygdperiode`() {
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.september(2020), 7.september(2020), 30.prosent, 200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 8.september(2020), 27.september(2020), 30.prosent, 200.daglig)
        )
        val inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 3.september(2020), 20000.månedlig, true)
        )
        håndterSykmelding(Sykmeldingsperiode(28.september(2020), 14.oktober(2020), 30.prosent))
        håndterSøknad(Sykdom(28.september(2020), 14.oktober(2020), 30.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(15.oktober(2020), 30.oktober(2020), 30.prosent))
        håndterSøknad(Sykdom(15.oktober(2020), 30.oktober(2020), 30.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(11.november(2020), 11.november(2020), 100.prosent))
        håndterSøknad(Sykdom(11.november(2020), 11.november(2020), 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(18.august(2020), 2.september(2020))), førsteFraværsdag = 11.november(2020))
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        Assertions.assertDoesNotThrow { håndterYtelser(3.vedtaksperiode) }
    }


    @Test
    fun `førstegangsbehandling skal ikke hoppe videre dersom det kun er inntekt i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
        ))
        assertTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `fortsettelse av Infotrygd-perioder skal ikke generere utbetalingslinjer for Infotrygd-periode`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 2.januar, 100.prosent, INNTEKT)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historie, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)

        inspektør.also {
            assertEquals(3.januar, it.arbeidsgiverOppdrag[0][0].fom)
        }
    }

    // TODO: er denne testen relevant? Vi sender ikke med arbeidsgiverinfo for forlengelser
    @Test
    fun `fremtidig test av utbetalingstidslinjeBuilder, historikk fra flere arbeidsgivere`() {
        håndterInntektsmelding(listOf(11.juli(2020) til 26.juli(2020)), refusjon = Inntektsmelding.Refusjon(
            2077.daglig,
            null
        )
        )
        håndterSykmelding(Sykmeldingsperiode(20.september(2020), 19.oktober(2020), 100.prosent))
        håndterSøknadMedValidering(
            1.vedtaksperiode,
            Sykdom(20.september(2020), 19.oktober(2020), 100.prosent)
        )

        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 27.juli(2020), 20.august(2020), 100.prosent, 2077.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 21.august(2020), 19.september(2020), 100.prosent, 2077.daglig),
            ArbeidsgiverUtbetalingsperiode("12345789", 21.august(2019), 19.september(2019), 100.prosent, 1043.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 27.juli(2020), 45000.månedlig, true),
                Inntektsopplysning("12345789", 21.august(2019), 22600.månedlig, true)
            )
        )
        håndterYtelser(1.vedtaksperiode)

        inspektør.also {
            assertNoErrors(1.vedtaksperiode.filter())
            assertNoWarnings(1.vedtaksperiode.filter())
            assertActivities(person)
            assertInntektForDato(45000.månedlig, 27.juli(2020), inspektør = it)
            assertEquals(1, it.sykdomshistorikk.size)
            assertEquals(21, it.sykdomstidslinje.inspektør.dagteller[Dag.Sykedag::class])
            assertEquals(9, it.sykdomstidslinje.inspektør.dagteller[Dag.SykHelgedag::class])
            assertEquals(43617, it.nettoBeløp[0])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `Perioder hvor vedtaksperiode 1 avsluttes med permisjon skal ikke regnes som gap til påfølgende vedtaksperiode`() {
        // Første periode slutter på arbeidsdager, og neste periode blir feilaktig markert som en forlengelse.
        // Dette skyldes for at vi ikke sjekker for følgende arbeidsdager/permisjon i slutten av forrige periode (som gjør at det egentlig skal være gap)
        håndterSykmelding(Sykmeldingsperiode(9.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(9.juni, 30.juni, 100.prosent), Søknad.Søknadsperiode.Permisjon(28.juni, 30.juni))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.juni, 8.juni, 100.prosent, 15000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 3.juni(2018), 15000.daglig, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.juli, 31.juli, 100.prosent))
        håndterSøknad(Sykdom(1.juli, 31.juli, 100.prosent))

        assertNoErrors()
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE
        )
    }

    @Test
    fun `Perioder hvor vedtaksperiode 1 avsluttes med ferie skal ikke regnes som gap til påfølgende vedtaksperiode`() {
        // Første periode slutter på arbeidsdager, og neste periode blir feilaktig markert som en forlengelse.
        // Dette skyldes for at vi ikke sjekker for følgende arbeidsdager/ferie i slutten av forrige periode (som gjør at det egentlig skal være gap)
        håndterSykmelding(Sykmeldingsperiode(9.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(9.juni, 30.juni, 100.prosent), Ferie(28.juni, 30.juni))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.juni, 8.juni, 100.prosent, 15000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 3.juni(2018), 15000.daglig, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.juli, 31.juli, 100.prosent))
        håndterSøknad(Sykdom(1.juli, 31.juli, 100.prosent))

        assertNoErrors()
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE
        )
    }


    @Test
    fun `To forlengelser som forlenger utbetaling fra infotrygd skal ha samme maksdato`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 2.januar, 100.prosent, INNTEKT)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar(2018), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historie, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 23.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertEquals(inspektør.sisteMaksdato(1.vedtaksperiode), inspektør.sisteMaksdato(2.vedtaksperiode))
    }

    @Test
    fun `Tillater førstegangsbehandling hos annen arbeidsgiver, hvis gap til foregående`() {
        val periode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 1.januar(2021), INNTEKT, true)
        )
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar(2021), 31.januar(2021), 100.prosent, INNTEKT)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1, inntektshistorikk = inntektshistorikk)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        val periode2 = 1.mars(2021) til 31.mars(2021)
        val a2Periode = 2.april(2021) til 30.april(2021)
        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(a2Periode.start, a2Periode.endInclusive, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1, inntektshistorikk = inntektshistorikk)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSøknad(Sykdom(a2Periode.start, a2Periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(2.april(2021) til 17.april(2021)),
            førsteFraværsdag = 2.april(2021),
            orgnummer = a2
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a2, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a2, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.april(2020) til 1.mars(2021) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt Inntekt.INGEN
                }
            }
        ))
        assertNoWarnings(1.vedtaksperiode.filter(a2))

        håndterYtelser(1.vedtaksperiode, orgnummer = a2, inntektshistorikk = inntektshistorikk)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
    }

    @Test
    fun `forlengelse av infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 23.februar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar(2018), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `Tidligere periode fra gammel arbeidsgiver, deretter en infotrygdforlengelse fra nåværende arbeidsgiver, kan utbetales uten warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                listOf(sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(12)))
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(3)))
                , arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(29.januar til 13.februar),
            beregnetInntekt = 10000.månedlig,
            orgnummer = a2
        )

        val utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a2, 14.februar, 28.februar, 100.prosent, 10000.månedlig))
        val inntektshistorikk = listOf(Inntektsopplysning(a2, 14.februar, 10000.månedlig, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = utbetalinger, inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertNoWarnings(1.vedtaksperiode.filter(a1))
        assertNoErrors(1.vedtaksperiode.filter(a1))
        assertNoWarnings(1.vedtaksperiode.filter(a2))
        assertNoErrors(1.vedtaksperiode.filter(a2))
    }

    @Test
    fun `Infotrygdforlengelse av arbeidsgiver som ikke finnes i aareg, kan utbetales uten warning`() {
        håndterInntektsmelding(listOf(17.januar til 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        val utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 1.februar, 28.februar, 100.prosent, 10000.månedlig))
        val inntektshistorikk = listOf(Inntektsopplysning(a1, 1.februar, 10000.månedlig, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = utbetalinger, inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertNoWarnings(1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `revurdering ved skjæringstidspunkt hos infotrygd`() {
        val historikk = listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, 1000.daglig))
        val inntektsopplysning = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historikk.toTypedArray(), inntektshistorikk = inntektsopplysning)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET
        )
        assertErrors()
    }

    @Test
    fun `periode etter en periode med ferie - opphav i Infotrygd - Avsluttes via godkjenningsbehov`() {
        håndterInntektsmelding(listOf(15.november(2017) til 30.november(2017)))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.desember(2017), 31.desember(2017), 100.prosent, INNTEKT)
        val inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.desember(2017), INNTEKT, true)
        )
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterYtelser(3.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
        assertTilstander(3.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `periode etter en periode med permisjon (gir warning) - opphav i Infotrygd`() {
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.desember(2017), 31.desember(2017), 100.prosent, INNTEKT)
        val inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.desember(2017), INNTEKT, true)
        )
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent),
            Søknad.Søknadsperiode.Permisjon(1.februar, 28.februar)
        )
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterYtelser(3.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(2.vedtaksperiode, START, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `Forlengelse av en infotrygdforlengelse - skal ikke vente på inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)),
            besvart = LocalDate.EPOCH.atStartOfDay()
        )
        håndterYtelser(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)),
            besvart = LocalDate.EPOCH.atStartOfDay()
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `enkel infotrygdforlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `annen arbeidsgiver forlenger infotrygdforlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)),
            besvart = LocalDate.EPOCH.atStartOfDay(),
            orgnummer = a1
        )
        håndterYtelser(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)),
            besvart = LocalDate.EPOCH.atStartOfDay(),
            orgnummer = a1
        )
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a2)
        val vilkårsgrunnlag = inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)
        assertNotNull(vilkårsgrunnlag)
        assertFalse(vilkårsgrunnlag.inspektør.vurdertOk)
        assertError("Bruker mangler nødvendig inntekt ved validering av Vilkårsgrunnlag", 1.vedtaksperiode.filter(a2))
        assertTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
    }

    @Test
    fun `Legger på warning hvis første utbetalingsdag i infotrygdforlengelse er mellom første og sekstende mai 2021 og sykepengegrunnlag har blitt begrenset av gammelt G-beløp`() {
        håndterSykmelding(Sykmeldingsperiode(12.juli(2021), 1.august(2021), 100.prosent))
        håndterSøknad(Sykdom(12.juli(2021), 1.august(2021), 100.prosent))

        val inntektHøyereEllerLikGammeltGBeløp = Grunnbeløp.`6G`.beløp(LocalDate.of(2021, 4, 30))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 2.mai(2021), inntektHøyereEllerLikGammeltGBeløp, true))
        val utbetlinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 2.mai(2021), 21.juni(2021), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 22.juni(2021), 11.juli(2021), 100.prosent, 1000.daglig)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = utbetlinger, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertTrue(person.personLogg.warn().toString().contains("Første utbetalingsdag er i Infotrygd og mellom 1. og 16. mai. Kontroller at riktig grunnbeløp er brukt."))
    }

    @Test
    fun `en periode med ikke medlem, en infotrygdperiode, en etterfølgende periode med vilkårsgrunnlag ok`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(
                Periode(1.januar, 16.januar)
            ), førsteFraværsdag = 1.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            INNTEKT,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Nei

        )
        håndterYtelser()
        håndterUtbetalingsgodkjenning()

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        assertTrue(person.personLogg.hasWarningsOrWorse())
        inspektør.also {
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }

        håndterSykmelding(Sykmeldingsperiode(1.januar(2019), 31.januar(2019), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2019), 31.januar(2019), 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.desember, 31.desember, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.desember, INNTEKT, true))
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(
            3.vedtaksperiode, listOf(
                Periode(1.januar(2020), 16.januar(2020))
            ), førsteFraværsdag = 1.januar(2020), refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList())
        )
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(
            3.vedtaksperiode,
            INNTEKT,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2019) til 1.desember(2019) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                }
            )
        )
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertTrue(person.personLogg.hasWarningsOrWorse())
        inspektør.also {
            it.utbetalingstidslinjer(3.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.navDagTeller)
            }

            it.utbetaling(2).utbetalingstidslinje().inspektør.also { tidslinjeInspektør ->
                assertEquals(32, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(16, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(34, tidslinjeInspektør.navDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)

            }
        }
    }

    @Test
    fun `Periode som forlenger annen arbeidsgiver, men ikke seg selv, kastes ut fordi den mangler inntekt på skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar(2021), 28.februar(2021), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar(2021), 28.februar(2021), 100.prosent), orgnummer = a1)
        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 1.januar(2021), INNTEKT, true)
        )
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar(2021), 31.januar(2021), 100.prosent, INNTEKT)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars(2021), 31.mars(2021), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars(2021), 31.mars(2021), 100.prosent), orgnummer = a1)

        håndterUtbetalingshistorikk(2.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.april(2021), 30.april(2021), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.april(2021), 30.april(2021), 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.april(2021) til 30.april(2021)), førsteFraværsdag = 1.april(2021), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
    }

    /*
   starter i IT.
   samme arbeidsgiverperiode.
   [infotrygd][spleis][infotrygd][spleis]
                 ^ ny fagsystemID   ^ ny fagsystemID
*/
    @Test
    fun `får ny fagsystemId når perioden er innom infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(30.mai, 23.juni, 100.prosent))
        håndterSøknad(Sykdom(30.mai, 23.juni, 100.prosent))
        val historie1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 19.mai,  29.mai, 100.prosent, 1000.daglig)
        )
        val inntekter1 = listOf(Inntektsopplysning(ORGNUMMER, 19.mai(2018), 1000.daglig, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historie1.toTypedArray(), inntektshistorikk = inntekter1)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        val historie2 = historie1 + listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 24.juni,  12.juli, 100.prosent, 1000.daglig),
        )

        val inntekter2 = inntekter1 + listOf(
            Inntektsopplysning(ORGNUMMER, 24.juni(2018), 1000.daglig, true)
        )
        håndterSykmelding(Sykmeldingsperiode(13.juli, 31.juli, 100.prosent))
        håndterSøknad(Sykdom(13.juli, 31.juli, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historie2.toTypedArray(), inntektshistorikk = inntekter2)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertEquals(2, inspektør.utbetalinger.size)
        val første = inspektør.utbetalinger.first().inspektør.arbeidsgiverOppdrag
        val siste = inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag
        Assertions.assertNotEquals(første.fagsystemId(), siste.fagsystemId())
        første.linjerUtenOpphør().also { linjer ->
            assertEquals(1, linjer.size)
            assertEquals(30.mai, linjer.first().fom)
            assertEquals(22.juni, linjer.first().tom)
        }
        siste.linjerUtenOpphør().also { linjer ->
            assertEquals(1, linjer.size)
            assertEquals(13.juli, linjer.last().fom)
            assertEquals(31.juli, linjer.last().tom)
        }
    }

    /*
   starter i IT.
   ikke samme arbeidsgiverperiode.
   [infotrygd][spleis][infotrygd]                         [infotrygd][spleis]
                ^ ny fagsystemID   ^-egentlig ny AGP her               ^ ny fagsystemID
*/
    @Test
    fun `kort infotrygdperiode etter ny arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        val historie1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  31.januar, 100.prosent, 1000.daglig)
        )
        val inntekter1 = listOf(Inntektsopplysning(ORGNUMMER, 1.januar(2018), 1000.daglig, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historie1.toTypedArray(), inntektshistorikk = inntekter1)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        val historie2 = historie1 + listOf(
            // [ nok gap til ny arbeidsgiverperiode ]
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.april,  10.april, 100.prosent, 1000.daglig)
        )
        håndterSykmelding(Sykmeldingsperiode(11.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(11.april, 30.april, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historie2.toTypedArray(), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 5.april(2018), 1000.daglig, true),
            Inntektsopplysning(ORGNUMMER, 1.januar(2018), 1000.daglig, true)
        ))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        val siste = inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag
        siste.linjerUtenOpphør().also { linjer ->
            assertEquals(1, linjer.size)
            assertEquals(11.april, linjer.last().fom)
            assertEquals(30.april, linjer.last().tom)
        }
    }

    /*
   starter i IT.
   samme arbeidsgiverperiode.
   [infotrygd][spleis][infotrygd][      spleis     ][spleis]
                 ^ ny fagsystemID   ^ ny fagsystemID   ^ samme fagsystemID
*/
    @Test
    fun `bruker samme fagsystemID når forrige er spleisperiode`() {
        håndterSykmelding(Sykmeldingsperiode(30.mai, 23.juni, 100.prosent))
        håndterSøknad(Sykdom(30.mai, 23.juni, 100.prosent))
        val historie1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 19.mai,  29.mai, 100.prosent, 1000.daglig)
        )
        val inntekter1 = listOf(
            Inntektsopplysning(ORGNUMMER, 19.mai(2018), 1000.daglig, true)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historie1.toTypedArray(), inntektshistorikk = inntekter1)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        val historie2 = historie1 + listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 24.juni,  12.juli, 100.prosent, 1000.daglig),
        )
        val inntekter2 = listOf(
            Inntektsopplysning(ORGNUMMER, 24.juni(2018), 1000.daglig, true),
            Inntektsopplysning(ORGNUMMER, 19.mai(2018), 1000.daglig, true)
        )
        håndterSykmelding(Sykmeldingsperiode(13.juli, 31.juli, 100.prosent))
        håndterSøknad(Sykdom(13.juli, 31.juli, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historie2.toTypedArray(), inntektshistorikk = inntekter2)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.august, 31.august, 100.prosent))
        håndterSøknad(Sykdom(1.august, 31.august, 100.prosent))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertEquals(3, inspektør.utbetalinger.size)
        val første = inspektør.utbetalinger.first().inspektør.arbeidsgiverOppdrag
        val andre = inspektør.utbetalinger[1].inspektør.arbeidsgiverOppdrag
        val siste = inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag
        Assertions.assertNotEquals(første.fagsystemId(), siste.fagsystemId())
        assertEquals(andre.fagsystemId(), siste.fagsystemId())
        siste.linjerUtenOpphør().also { linjer ->
            assertEquals(1, linjer.size)
            assertEquals(13.juli, linjer.last().fom)
            assertEquals(31.august, linjer.last().tom)
        }
    }

    /*
   starter i IT.
   ikke samme arbeidsgiverperiode.
   [infotrygd][spleis][infotrygd]   [infotrygd][spleis]
                ^ ny fagsystemID                  ^ ny fagsystemID
*/
    @Test
    fun `bruker ny fagsystemID når det er gap i Infortrygd i mellomtiden`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        val historie1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  31.januar, 100.prosent, 1000.daglig)
        )
        val inntekter1 = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar(2018), 1000.daglig, true)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historie1.toTypedArray(), inntektshistorikk = inntekter1)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        val historie2 = historie1 + listOf(
            // [ nok gap til ny arbeidsgiverperiode ]
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.april,  30.april, 100.prosent, 1000.daglig)
        )
        val inntekter2 = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar(2018), 1000.daglig, true),
            Inntektsopplysning(ORGNUMMER, 5.april(2018), 1000.daglig, true)
        )
        håndterSykmelding(Sykmeldingsperiode(1.mai, 31.mai, 100.prosent))
        håndterSøknad(Sykdom(1.mai, 31.mai, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historie2.toTypedArray(), inntektshistorikk = inntekter2)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertEquals(2, inspektør.utbetalinger.size)
        val første = inspektør.utbetalinger.first().inspektør.arbeidsgiverOppdrag
        val siste = inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag
        Assertions.assertNotEquals(første.fagsystemId(), siste.fagsystemId())
        første.linjerUtenOpphør().also { linjer ->
            assertEquals(1, linjer.size)
            assertEquals(1.februar, linjer.first().fom)
            assertEquals(28.februar, linjer.first().tom)
        }
        siste.linjerUtenOpphør().also { linjer ->
            assertEquals(1, linjer.size)
            assertEquals(1.mai, linjer.last().fom)
            assertEquals(31.mai, linjer.last().tom)
        }
    }

    @Test
    fun `periodetype er forlengelse fra Infotrygd hvis førstegangsbehandlingen skjedde i Infotrygd`() {
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 3.januar(2018), 1000.daglig, true))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        håndterSykmelding(Sykmeldingsperiode(26.februar, 15.april, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(26.februar, 15.april, 100.prosent))
        assertEquals(Periodetype.OVERGANG_FRA_IT, inspektør.periodetype(1.vedtaksperiode))
        assertEquals(Periodetype.INFOTRYGDFORLENGELSE, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `Forlengelser av infotrygd overgang har samme maksdato som forrige`() {
        val historikk1 = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 20.november(2019),  29.mai(2020), 100.prosent, 1145.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 20.november(2019), 1000.daglig, true))
        håndterSykmelding(Sykmeldingsperiode(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk1, inntektshistorikk = inntekter, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterYtelser(1.vedtaksperiode, historikk1, inntektshistorikk = inntekter, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 9.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(22.juni(2020), 9.juli(2020), 100.prosent))
        håndterYtelser(2.vedtaksperiode, historikk1, inntektshistorikk = inntekter, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterSimulering(2.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, AVVENTER_GODKJENNING, LocalDateTime.now().minusDays(110))

        val historikk2 = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 22.juni(2020),  17.august(2020), 100.prosent, 1145.daglig)
        val inntekter2 = listOf(
            Inntektsopplysning(ORGNUMMER, 20.november(2019), 1000.daglig, true),
            Inntektsopplysning(ORGNUMMER, 22.juni(2020), 1000.daglig, true)
        )
        håndterSykmelding(Sykmeldingsperiode(18.august(2020), 2.september(2020), 100.prosent))
        håndterSøknad(Sykdom(18.august(2020), 2.september(2020), 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, historikk1, historikk2, inntektshistorikk = inntekter2, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterYtelser(3.vedtaksperiode, historikk1, historikk2, inntektshistorikk = inntekter2)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(3.vedtaksperiode))
        assertEquals(inspektør.sisteMaksdato(1.vedtaksperiode), inspektør.sisteMaksdato(3.vedtaksperiode))
    }

    @Test
    fun `infotrygdforlengelse med spleis-utbetaling mellom`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent))
        val søknadId = håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.mars, 31.mars, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true),
                Inntektsopplysning(ORGNUMMER, 1.mars, INNTEKT, true)
            )
        )

        person.søppelbøtte(hendelselogg, 1.april til 30.april)
        assertTrue(observatør.opprettOppgaveEvent().any { søknadId in it.hendelser })
        assertFalse(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadId in it.hendelser })
    }

    @Test
    fun `ingen vilkårsgrunnlag når perioden har opphav i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Arbeid(25.februar, 28.februar))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, 15000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 17.januar(2018), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.mars)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertTrue(inspektør.vilkårsgrunnlag(1.vedtaksperiode) is VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag)
        assertTrue(inspektør.vilkårsgrunnlag(2.vedtaksperiode) is VilkårsgrunnlagHistorikk.Grunnlagsdata)
        assertEquals(Periodetype.OVERGANG_FRA_IT, inspektør.periodetype(1.vedtaksperiode))
        assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `gjenbruker ikke vilkårsprøving når førstegangsbehandlingen kastes ut`() = Toggle.IkkeForlengInfotrygdperioder.disable {
        val INNTEKT_FRA_IT = INNTEKT/2

        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterYtelser(1.vedtaksperiode)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            INNTEKT,
            inntektsvurdering = Inntektsvurdering(
                inntekter = listOf(sammenligningsgrunnlag(a1, 1.januar, (INNTEKT / 2).repeat(12))),
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(grunnlag(a1, 1.januar, INNTEKT.repeat(3))),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(a1, 1.desember(2017)))
        )
        assertError("Har mer enn 25 % avvik", 1.vedtaksperiode.filter())

        håndterSykmelding(Sykmeldingsperiode(18.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 17.januar, 100.prosent, INNTEKT_FRA_IT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT_FRA_IT, true)
        ))
        håndterYtelser(2.vedtaksperiode)
        val vilkårsgrunnlag = inspektør.vilkårsgrunnlag(2.vedtaksperiode)
        assertNotNull(vilkårsgrunnlag)
        val grunnlagsdataInspektør = vilkårsgrunnlag.inspektør
        assertEquals(INNTEKT_FRA_IT, grunnlagsdataInspektør.sykepengegrunnlag.inspektør.sykepengegrunnlag)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
    }

    @Test
    fun `Overgang fra infotrygd skal ikke få ghost warning selv om vi har lagret skatteinntekter i en gammel versjon av spleis`() {
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        val utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 31.januar, 100.prosent, INNTEKT))
        val inntektshistorikk = listOf(Inntektsopplysning(a1, 17.januar, INNTEKT, true))

        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1, utbetalinger = utbetalinger, inntektshistorikk = inntektshistorikk)

        person.lagreArbeidsforhold(a1, listOf(Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH)), person.aktivitetslogg, 17.januar)
        person.lagreArbeidsforhold(a2, listOf(Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH)), person.aktivitetslogg, 17.januar)

        inspektør(a2).inntektInspektør.inntektshistorikk.append {
            val hendelseId = UUID.randomUUID()
            addSkattSykepengegrunnlag(17.januar, hendelseId, INNTEKT, YearMonth.of(2017, 12),
                Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
            addSkattSykepengegrunnlag(17.januar, hendelseId, INNTEKT, YearMonth.of(2017, 11),
                Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
            addSkattSykepengegrunnlag(17.januar, hendelseId, INNTEKT, YearMonth.of(2017, 10),
                Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT, "fordel", "beskrivelse")
        }

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertNoWarnings(1.vedtaksperiode.filter(a1))
        assertNoErrors(1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `Varsel om flere inntektsmeldinger hvis vi forlenger en avsluttet periode uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(1.mars, 20.mars, 50.prosent))
        håndterSøknad(Sykdom(1.mars, 20.mars, 50.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars)

        assertNoWarning("Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.", 1.vedtaksperiode.filter())
        assertWarning("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler.", 1.vedtaksperiode.filter())
        assertWarning("Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.", 2.vedtaksperiode.filter())
        assertNoWarning("Vi har mottatt en inntektsmelding i en løpende sykmeldingsperiode med oppgitt første/bestemmende fraværsdag som er ulik tidligere fastsatt skjæringstidspunkt.", 2.vedtaksperiode.filter())
        assertNoWarning("Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode.", 2.vedtaksperiode.filter())
    }

    @Test
    fun `Arbeidsgiverperiode tilstøter ikke Infotrygd`() {
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
        håndterUtbetalt()
        assertWarning("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler.", 1.vedtaksperiode.filter())
    }

    @Test
    fun `Arbeidsgiverperiode tilstøter Infotrygd`() {
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
        håndterUtbetalt()
        assertNoWarnings(1.vedtaksperiode.filter())
    }

    @Test
    fun `Finner refusjon fra feil inntektsmelding ved Infotrygdforlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        // Inntektsmelding blir ikke brukt ettersom det er forlengelse fra Infotrygd.
        // Når vi ser etter refusjon for Infotrygdperioden finner vi alikevel frem til denne inntektsmeldingen og forsøker å finne
        // refusjonsbeløp på 1-5.Januar som er før arbeidsgiverperioden
        håndterInntektsmelding(listOf(Periode(6.januar, 21.januar)))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 21.januar, 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertInfo("Refusjon gjelder ikke for hele utbetalingsperioden", 1.vedtaksperiode.filter())
    }

    @Test
    fun `Finner refusjon ved forlengelse fra Infotrygd`() {
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
        håndterUtbetalt()
        assertNoWarnings(1.vedtaksperiode.filter())
    }

    @Test
    fun `en forlengelse av overgang fra Infotrygd uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 10.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 10.februar, 100.prosent))

        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            inntektshistorikk = listOf(
                Inntektsopplysning(orgnummer = ORGNUMMER, sykepengerFom = 17.januar, inntekt = INNTEKT, refusjonTilArbeidsgiver = true)
            ),
            utbetalinger = arrayOf(
                ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT)
            )
        )

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        forlengVedtak(11.februar, 28.februar)

        assertWarning("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler.", 1.vedtaksperiode.filter())
        assertWarning("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler.", 2.vedtaksperiode.filter())
    }

    @Test
    fun `en overgang fra Infotrygd uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 10.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 10.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            inntektshistorikk = listOf(
                Inntektsopplysning(orgnummer = ORGNUMMER, sykepengerFom = 17.januar, inntekt = INNTEKT, refusjonTilArbeidsgiver = true)
            ),
            utbetalinger = arrayOf(
                ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT)
            )
        )


        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertWarning("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler.", 1.vedtaksperiode.filter())
    }

    @Test
    fun `førstegangsbehandling i Spleis etter en overgang fra Infotrygd uten inntektsmelding `() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 10.februar, 100.prosent))

        håndterSøknad(Sykdom(1.februar, 10.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            inntektshistorikk = listOf(
                Inntektsopplysning(orgnummer = ORGNUMMER, sykepengerFom = 17.januar, inntekt = INNTEKT, refusjonTilArbeidsgiver = true)
            ),
            utbetalinger = arrayOf(
                ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT)
            )
        )

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        nyttVedtak(1.mars, 31.mars)

        assertWarning("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler.", 1.vedtaksperiode.filter())
        assertNoWarnings(2.vedtaksperiode.filter(ORGNUMMER))
    }

    @Test
    fun `bruker laveste registrerte inntekt i infotrygd om det er fler inntekter samme dato for samme arbeidsgiver`() {
        val inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, 25000.månedlig, true, null),
            Inntektsopplysning(ORGNUMMER, 1.januar, 23000.månedlig, false, null),
            Inntektsopplysning(ORGNUMMER, 1.januar, 24000.månedlig, true, 1.januar)
        )
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = inntektshistorikk)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        assertInntektForDato(23000.månedlig, 1.januar, inspektør = inspektør)
        assertInfo("Det er lagt inn flere inntekter i Infotrygd med samme fom-dato.")
    }

    @Test
    fun `infotrygd endrer refusjon til brukerubetaling uten at det foreligger inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(1.april, 28.april, 100.prosent))
        håndterSøknad(Sykdom(1.april, 28.april, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT),
            PersonUtbetalingsperiode(ORGNUMMER, 1.mars, 31.mars, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true),
                Inntektsopplysning(ORGNUMMER, 1.mars, INNTEKT, false)
            )
        )
        håndterYtelser(2.vedtaksperiode)
        assertErrors(2.vedtaksperiode.filter(ORGNUMMER))
        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
    }

    private fun byggPersonMedOpphør(
        arbeidsgiverperiode: Periode = 1.januar til 16.januar,
        syktil: LocalDate = 31.januar,
        orgnummer: String = ORGNUMMER
    ) {
        håndterSykmelding(Sykmeldingsperiode(arbeidsgiverperiode.start, syktil, 100.prosent), orgnummer = orgnummer)
        håndterSøknadMedValidering(
            observatør.sisteVedtaksperiode(),
            Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent),
            orgnummer = orgnummer
        )
        håndterUtbetalingshistorikk(
            observatør.sisteVedtaksperiode(),
            orgnummer = orgnummer,
            inntektshistorikk = listOf(Inntektsopplysning(orgnummer, 1.desember(2017), INNTEKT, true)),
            besvart = LocalDateTime.now().minusMonths(1),
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(orgnummer, 1.desember(2017), 31.desember(2017), 100.prosent, INNTEKT))
        )
        håndterYtelser(
            observatør.sisteVedtaksperiode(),
            orgnummer = orgnummer,
            inntektshistorikk = listOf(Inntektsopplysning(orgnummer, 1.desember(2017), INNTEKT, true)),
            besvart = LocalDateTime.now().minusMonths(1),
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(orgnummer, 1.desember(2017), 31.desember(2017), 100.prosent, INNTEKT))
        )

        håndterSimulering(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalt(orgnummer = orgnummer)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 60.prosent), orgnummer = orgnummer)
        håndterSøknadMedValidering(
            observatør.sisteVedtaksperiode(),
            Sykdom(1.februar, 28.februar, 60.prosent),
            orgnummer = orgnummer
        )
        håndterYtelser(
            observatør.sisteVedtaksperiode(),
            orgnummer = orgnummer,
            inntektshistorikk = listOf(Inntektsopplysning(orgnummer, 1.desember(2017), 40000.månedlig, true)),
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(orgnummer, 1.desember(2017), 15.januar, 50.prosent, 40000.månedlig))
        )

        håndterSimulering(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalt(orgnummer = orgnummer)
    }

    @Test
    fun `§ 8-10 ledd 2 punktum 1 - vurderes ved overgang fra Infotrygd`() {
        val maksimumSykepengegrunnlag2018 = (93634 * 6).årlig // 6G
        val inntekt = maksimumSykepengegrunnlag2018.plus(1.årlig)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, inntekt)),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, inntekt, true)
            )
        )
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()
        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = Paragraf.PARAGRAF_8_10,
            ledd = Ledd.LEDD_2,
            punktum = 1.punktum,
            versjon = 1.januar(2020),
            input = mapOf(
                "maksimaltSykepengegrunnlag" to 561804.0,
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 561805.0
            ),
            output = mapOf(
                "erBegrenset" to false // Infotrygd-inntekter er allerede begrenset til 6G
            )
        )
    }

    @Test
    fun `har ikke sammenligningsgrunnlag etter overgang fra Infotrygd`() {
        val skjæringstidspunkt = 1.desember(2017)
        val infotrygdperioder = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, skjæringstidspunkt, 31.desember(2017), 100.prosent, INNTEKT))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, skjæringstidspunkt, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *infotrygdperioder, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(null, OppsamletSammenligningsgrunnlagBuilder(person).sammenligningsgrunnlag(ORGNUMMER, skjæringstidspunkt))
    }


    private fun utbetalingshistorikkForFeriepenger(
        utbetalinger: List<UtbetalingshistorikkForFeriepenger.Utbetalingsperiode> = emptyList(),
        arbeidskategorikoder: UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
            listOf(
                UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode(
                    periode = LocalDate.MIN til LocalDate.MAX,
                    arbeidskategorikode = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.Arbeidstaker
                )
            )
        ),
        skalBeregnesManuelt: Boolean = false
    ) =
        UtbetalingshistorikkForFeriepenger(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            utbetalinger = utbetalinger,
            feriepengehistorikk = emptyList(),
            arbeidskategorikoder = arbeidskategorikoder,
            opptjeningsår = Year.of(2020),
            skalBeregnesManuelt = skalBeregnesManuelt,
        )



}
