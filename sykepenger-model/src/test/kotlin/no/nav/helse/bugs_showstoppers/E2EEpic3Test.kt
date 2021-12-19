package no.nav.helse.bugs_showstoppers

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.*
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.e2e.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class E2EEpic3Test : AbstractEndToEndTest() {

    @Test
    fun `gradert sykmelding først`() {
        // ugyldig sykmelding lager en tom vedtaksperiode uten tidslinje, som overlapper med alt
        håndterSykmelding(Sykmeldingsperiode(3.januar(2020), 3.januar(2020), 50.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        håndterSykmelding(Sykmeldingsperiode(13.januar(2020), 17.januar(2020), 100.prosent))
        håndterSøknad(Sykdom(13.januar(2020), 17.januar(2020), 100.prosent))
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
    }

    @Test
    fun `Ingen sykedager i tidslinjen - skjæringstidspunkt-bug`() {
        håndterSykmelding(Sykmeldingsperiode(6.januar(2020), 7.januar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar(2020), 10.januar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(27.januar(2020), 28.januar(2020), 100.prosent))

        håndterInntektsmelding(
            listOf(
                Periode(18.november(2019), 23.november(2019)),
                Periode(14.oktober(2019), 18.oktober(2019)),
                Periode(1.november(2019), 5.november(2019))
            ),
            18.november(2019)
        )

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    fun `inntektsmelding starter etter sykmeldingsperioden`() {
        håndterSykmelding(Sykmeldingsperiode(15.januar(2020), 12.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(15.januar(2020), 12.februar(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(16.januar(2020), 31.januar(2020))), 16.januar(2020))

        val inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER.toString(), 3.april(2019), INNTEKT, true),
            Inntektsopplysning(ORGNUMMER.toString(), 18.mars(2018), INNTEKT, true),
            Inntektsopplysning(ORGNUMMER.toString(), 13.november(2017), INNTEKT, true)
        )
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 3.april(2019), 30.april(2019), 100.prosent, 100.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 18.mars(2018), 2.april(2018), 100.prosent, 100.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 29.november(2017), 3.desember(2017), 100.prosent, 100.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 13.november(2017), 28.november(2017), 100.prosent, 100.daglig)
        )
        håndterYtelser(1.vedtaksperiode, *utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `periode uten sykedager`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 9.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(15.januar, 16.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(
                3.januar til 4.januar,
                8.januar til 9.januar,
                15.januar til 26.januar
            ),
            15.januar
        )

        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 4.januar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )

        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(8.januar, 9.januar, 100.prosent))

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP
        )
    }

    @Test
    fun `Periode som kun er innenfor arbeidsgiverperioden der inntekten ikke gjelder venter på arbeidsgiversøknad før den går til AVSLUTTET_UTEN_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(4.januar, 20.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(
                1.januar til 1.januar,
                3.januar til 17.januar
            ),
            3.januar
        )

        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 1.januar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(4.januar, 20.januar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Periode som kun er innenfor arbeidsgiverperioden der inntekten ikke gjelder går til AVSLUTTET_UTEN_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(4.januar, 20.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 1.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(
                1.januar til 1.januar,
                3.januar til 17.januar
            ),
            3.januar
        )

        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(4.januar, 20.januar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Inntektsmelding for to perioder som utvider periode 2 slik at de blir tilstøtende sender ikke periode 2 til AVSLUTTET_UTEN_UTBETALING - inntektsmelding først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(5.januar, 20.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar), 1.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 1.januar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(5.januar, 20.januar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Inntektsmelding for to perioder som utvider periode 2 slik at de blir tilstøtende sender ikke periode 2 til AVSLUTTET_UTEN_UTBETALING - søknad først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(5.januar, 20.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 1.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(5.januar, 20.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar), 1.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_UFERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Ikke samsvar mellom sykmeldinger og inntektsmelding - første periode får søknad, men ikke inntektsmelding og må nå makstid før de neste kan fortsette behandling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 3.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(5.januar, 22.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 1.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(3.januar, 3.januar, 100.prosent))
        håndterSøknadMedValidering(3.vedtaksperiode, Sykdom(5.januar, 22.januar, 100.prosent))

        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(
                3.januar til 3.januar,
                5.januar til 20.januar
            ),
            5.januar
        )

        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, LocalDateTime.now().minusDays(111))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_UFERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Ikke samsvar mellom sykmeldinger og inntektsmelding - første periode får hverken søknad eller inntektsmelding og må nå makstid før de neste kan fortsette behandling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 3.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(5.januar, 22.januar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(3.januar, 3.januar, 100.prosent))
        håndterSøknadMedValidering(3.vedtaksperiode, Sykdom(5.januar, 22.januar, 100.prosent))

        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(
                3.januar til 3.januar,
                5.januar til 20.januar
            ),
            5.januar
        )

        håndterPåminnelse(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP, LocalDateTime.now().minusMonths(13))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            TIL_INFOTRYGD
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_UFERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Periode som kun er innenfor arbeidsgiverperioden går til UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP før forrige periode avsluttes`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(12.februar, 19.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 17.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(12.februar, 19.februar, 100.prosent))

        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(
                1.januar til 17.januar
            ),
            1.januar
        )

        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(
                12.februar til 19.februar,
                21.februar til 28.februar
            ),
            21.februar
        )

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
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

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `enkeltstående sykedag i arbeidsgiverperiode-gap`() {
        håndterSykmelding(Sykmeldingsperiode(10.februar(2020), 12.februar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(14.februar(2020), 14.februar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(27.februar(2020), 28.februar(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                10.februar(2020) til 12.februar(2020),
                27.februar(2020) til 10.mars(2020)
            ),
            førsteFraværsdag = 27.februar(2020)
        )
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP
        )
    }

    @Test
    fun `ignorerer egenmeldingsdag i søknaden langt tilbake i tid`() {
        håndterSykmelding(Sykmeldingsperiode(6.januar(2020), 23.januar(2020), 100.prosent))
        håndterSøknad(
            Egenmelding(
                24.september(2019),
                24.september(2019)
            ), // ignored because it's too long ago relative to 6.januar
            Sykdom(6.januar(2020), 23.januar(2020), 100.prosent)
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(24.september(2019), 24.september(2019)),
                Periode(27.september(2019), 6.oktober(2019)),
                Periode(14.oktober(2019), 18.oktober(2019))
            ),
            førsteFraværsdag = 24.september(2019)
        )
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `person med gammel sykmelding`() {
        // OBS: Disse kastes ikke ut fordi de er for gamle. De kastes ut fordi de kommer out of order
        håndterSykmelding(Sykmeldingsperiode(13.januar(2020), 31.januar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.februar(2017), 15.februar(2017), 100.prosent), mottatt = 31.januar(2020).atStartOfDay())

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `periode som begynner på siste dag i arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.februar(2020), 17.februar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.februar(2020), 1.mars(2020), 100.prosent))
        håndterInntektsmelding(listOf(3.februar(2020) til 18.februar(2020)), førsteFraværsdag = 3.januar(2020))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_SØKNAD_UFERDIG_FORLENGELSE)
    }

    @Test
    fun `sykmeldinger som overlapper`() {
        håndterSykmelding(Sykmeldingsperiode(15.januar(2020), 30.januar(2020), 100.prosent)) // sykmelding A, part 1
        håndterSykmelding(Sykmeldingsperiode(31.januar(2020), 15.februar(2020), 100.prosent)) // sykmelding A, part 2
        håndterSykmelding(Sykmeldingsperiode(16.januar(2020), 31.januar(2020), 100.prosent)) // sykmelding B
        håndterSykmelding(Sykmeldingsperiode(1.februar(2020), 16.februar(2020), 100.prosent)) // sykmelding C
        håndterSøknad(Sykdom(16.januar(2020), 31.januar(2020), 100.prosent)) // -> sykmelding B
        håndterSøknad(Sykdom(1.februar(2020), 16.februar(2020), 100.prosent)) // sykmelding C
        håndterSøknad(Sykdom(31.januar(2020), 15.februar(2020), 100.prosent)) // sykmelding A, part 2
        håndterSykmelding(Sykmeldingsperiode(18.februar(2020), 8.mars(2020), 100.prosent)) // sykmelding D
        assertEquals(3, inspektør.vedtaksperiodeTeller)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, TIL_INFOTRYGD)
        håndterInntektsmelding(listOf(15.januar(2020) til 30.januar(2020)), førsteFraværsdag = 15.januar(2020)) // does not currently affect anything, that should change with revurdering
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, TIL_INFOTRYGD)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `overlapp i arbeidsgivertidslinjer`() {
        håndterSykmelding(Sykmeldingsperiode(7.januar(2020), 13.januar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(14.januar(2020), 24.januar(2020), 100.prosent))
        håndterSøknad(Sykdom(7.januar(2020), 13.januar(2020), 100.prosent))
        håndterSøknad(
            Egenmelding(6.januar(2020), 6.januar(2020)),
            Sykdom(14.januar(2020), 24.januar(2020), 100.prosent)
        )
        håndterSykmelding(Sykmeldingsperiode(25.januar(2020), 7.februar(2020), 80.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.februar(2020), 28.februar(2020), 80.prosent))
        håndterSøknad(Sykdom(25.januar(2020), 7.februar(2020), 80.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(6.januar(2020) til 21.januar(2020)), førsteFraværsdag = 6.januar(2020))
        håndterSykmelding(Sykmeldingsperiode(29.februar(2020), 11.mars(2020), 80.prosent))

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(2.vedtaksperiode)

        assertEquals(5, inspektør.vedtaksperiodeTeller)
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(5.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
    }

    @Test
    fun `ferie inni arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(21.desember(2019), 5.januar(2020), 80.prosent))
        håndterSøknad(
            Egenmelding(18.september(2019), 20.september(2019)),
            Sykdom(21.desember(2019), 8.januar(2020), 80.prosent),
            Ferie(21.desember(2019), 23.desember(2019)),
            Ferie(27.desember(2019), 27.desember(2019)),
            Ferie(30.desember(2019), 30.desember(2019))
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                24.desember(2019) til 26.desember(2019),
                28.desember(2019) til 29.desember(2019),
                31.desember(2019) til 8.januar(2020)
            ),
            førsteFraværsdag = 31.desember(2019)
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.desember(2018) til 1.november(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)

        assertEquals(31.desember(2019), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTrue(inspektør.utbetalingslinjer(0).isEmpty())
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `Inntektsmelding, etter søknad, overskriver sykedager før arbeidsgiverperiode med arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(7.januar, 28.januar, 100.prosent))
        håndterSøknad(Sykdom(7.januar, 28.januar, 100.prosent))
        // Need to extend Arbeidsdag from first Arbeidsgiverperiode to beginning of Vedtaksperiode, considering weekends
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(9.januar, 24.januar)),
            førsteFraværsdag = 9.januar
        )
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(3, inspektør.sykdomshistorikk.size)
        assertEquals(22, inspektør.sykdomshistorikk.sykdomstidslinje().count())
        assertEquals(7.januar, inspektør.sykdomshistorikk.sykdomstidslinje().førsteDag())
        assertEquals(FriskHelgedag::class, inspektør.sykdomshistorikk.sykdomstidslinje()[7.januar]::class)
        assertEquals(Dag.Arbeidsdag::class, inspektør.sykdomshistorikk.sykdomstidslinje()[8.januar]::class)
        assertEquals(9.januar, inspektør.sykdomshistorikk.sykdomstidslinje().sisteSkjæringstidspunkt())
        assertEquals(28.januar, inspektør.sykdomshistorikk.sykdomstidslinje().sisteDag())
    }

    @Test
    fun `Inntektsmelding, før søknad, overskriver sykedager før arbeidsgiverperiode med arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(7.januar, 28.januar, 100.prosent))
        // Need to extend Arbeidsdag from first Arbeidsgiverperiode to beginning of Vedtaksperiode, considering weekends
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(9.januar, 24.januar)),
            førsteFraværsdag = 9.januar
        )
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(2, inspektør.sykdomshistorikk.size)
        assertEquals(22, inspektør.sykdomshistorikk.sykdomstidslinje().count())
        assertEquals(7.januar, inspektør.sykdomshistorikk.sykdomstidslinje().førsteDag())
        assertEquals(FriskHelgedag::class, inspektør.sykdomshistorikk.sykdomstidslinje()[7.januar]::class)
        assertEquals(Dag.Arbeidsdag::class, inspektør.sykdomshistorikk.sykdomstidslinje()[8.januar]::class)
        assertEquals(9.januar, inspektør.sykdomshistorikk.sykdomstidslinje().sisteSkjæringstidspunkt())
        assertEquals(28.januar, inspektør.sykdomshistorikk.sykdomstidslinje().sisteDag())
    }

    @Test
    fun `andre vedtaksperiode utbetalingslinjer dekker to perioder`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar(2020), 16.januar(2020))),
            førsteFraværsdag = 1.januar(2020)
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.februar(2020), 28.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(1.februar(2020), 28.februar(2020), 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))

        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )

        inspektør.also {
            assertEquals(1, it.arbeidsgiverOppdrag[0].size)
            assertEquals(17.januar(2020), it.arbeidsgiverOppdrag[0].first().fom)
            assertEquals(31.januar(2020), it.arbeidsgiverOppdrag[0].first().tom)
            assertEquals(1, it.arbeidsgiverOppdrag[1].size)
            assertEquals(17.januar(2020), it.arbeidsgiverOppdrag[1].first().fom)
            assertEquals(28.februar(2020), it.arbeidsgiverOppdrag[1].first().tom)
        }
    }

    @Test
    fun `simulering av periode der tilstøtende ikke ble utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(28.januar(2020), 10.februar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterSøknadArbeidsgiver(Sykdom(28.januar(2020), 10.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(28.januar(2020), 12.februar(2020))),
            førsteFraværsdag = 28.januar(2020)
        )

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(2.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `simulering av periode der tilstøtende ble utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(17.januar(2020), 10.februar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(17.januar(2020), 10.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(17.januar(2020), 2.februar(2020))),
            førsteFraværsdag = 17.januar(2020)
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)
        håndterYtelser(2.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
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
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `dobbeltbehandling av første periode aborterer behandling av andre periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar(2020), 16.januar(2020))),
            førsteFraværsdag = 1.januar(2020)
        )
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.februar(2020), 28.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(1.februar(2020), 28.februar(2020), 100.prosent))
        håndterYtelser(
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 17.januar(2020), 31.januar(2020), 100.prosent, 1400.daglig)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 17.januar(2020), 1400.daglig, true))
        )

        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )

        assertTilstander(
            2.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING
        )
    }

    @Test
    fun `helg i gap i arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 4.januar, 9.januar til 10.januar), 3.januar)
        håndterSøknad(Sykdom(3.januar, 10.januar, 100.prosent))
        assertTrue(inspektør.personLogg.hasWarningsOrWorse())
        inspektør.also {
            assertEquals(4, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(2, it.sykdomstidslinje.inspektør.dagteller[FriskHelgedag::class])
            assertEquals(2, it.sykdomstidslinje.inspektør.dagteller[Dag.Arbeidsdag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `Egenmelding i søknad overstyres av inntektsmelding når IM mottas først`() {
        håndterSykmelding(Sykmeldingsperiode(20.februar(2020), 8.mars(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(20.februar(2020), 5.mars(2020))), 20.februar(2020))
        håndterSøknad(Egenmelding(17.februar(2020), 19.februar(2020)), Sykdom(20.februar(2020), 8.mars(2020), 100.prosent))

        inspektør.also {
            assertEquals(20.februar(2020), it.sykdomstidslinje.førsteDag())
        }
    }

    @Test
    fun `Egenmelding i søknad overstyres av inntektsmelding når IM mottas sist`() {
        håndterSykmelding(Sykmeldingsperiode(20.februar(2020), 8.mars(2020), 100.prosent))
        håndterSøknad(Egenmelding(17.februar(2020), 19.februar(2020)), Sykdom(20.februar(2020), 8.mars(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(20.februar(2020), 5.mars(2020))), 20.februar(2020))

        inspektør.also {
            assertNull(it.sykdomstidslinje.inspektør.dagteller[Arbeidsgiverdag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertEquals(12, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(20.februar(2020), it.sykdomstidslinje.førsteDag())
        }
    }

    @Test
    fun `Syk, en arbeidsdag, ferie og syk`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(1.januar(2020), 1.januar(2020)),
                Periode(3.januar(2020), 17.januar(2020))
            ),
            førsteFraværsdag = 11.januar(2020)
        )
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent), Ferie(3.januar(2020), 10.januar(2020)), sendtTilNav = 1.februar(2020))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)

        inspektør.also {
            assertEquals(16, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertEquals(null, it.sykdomstidslinje.inspektør.dagteller[Feriedag::class])
            assertEquals(1, it.sykdomstidslinje.inspektør.dagteller[Dag.Arbeidsdag::class])
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(0, tidslinjeInspektør.fridagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(10, tidslinjeInspektør.navDagTeller)
                assertEquals(1, tidslinjeInspektør.arbeidsdagTeller)
            }
        }
        assertEquals(3.januar(2020), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `Syk, mange arbeidsdager, syk igjen på en lørdag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(1.januar(2020), 1.januar(2020)),
                Periode(11.januar(2020), 25.januar(2020))
            ),
            førsteFraværsdag = 11.januar(2020)
        )
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent), sendtTilNav = 1.februar(2020))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)

        inspektør.also {
            assertEquals(16, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(6, it.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertEquals(7, it.sykdomstidslinje.inspektør.dagteller[Dag.Arbeidsdag::class])
            assertEquals(2, it.sykdomstidslinje.inspektør.dagteller[FriskHelgedag::class])
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(1, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(5, tidslinjeInspektør.navDagTeller)
                assertEquals(9, tidslinjeInspektør.arbeidsdagTeller)
            }
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `Utbetaling med forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.juni(2020), 16.juni(2020))))

        håndterSykmelding(Sykmeldingsperiode(1.juli(2020), 31.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(1.juli(2020), 31.juli(2020), 100.prosent))

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertEquals(it.arbeidsgiverOppdrag[0].fagsystemId(), it.arbeidsgiverOppdrag[1].fagsystemId())
        }
    }

    @Test
    fun `Grad endrer tredje periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.juni(2020), 16.juni(2020))))

        håndterSykmelding(Sykmeldingsperiode(1.juli(2020), 31.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(1.juli(2020), 31.juli(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.august(2020), 31.august(2020), 50.prosent))
        håndterSøknad(Sykdom(1.august(2020), 31.august(2020), 50.prosent))

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(3.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertEquals(it.arbeidsgiverOppdrag[0].fagsystemId(), it.arbeidsgiverOppdrag[1].fagsystemId())
            assertEquals(it.arbeidsgiverOppdrag[1].fagsystemId(), it.arbeidsgiverOppdrag[2].fagsystemId())
        }
    }

    @Test
    fun `ikke medlem avviser alle dager og legger på warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(
                Periode(1.januar, 16.januar)
            ), førsteFraværsdag = 1.januar, refusjon = Refusjon(INNTEKT, null, emptyList())
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
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }
    }

    @Test
    fun `opptjening ikke ok avviser ikke dager før gjeldende skjæringstidspunkt`() {
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER.toString(), 1.januar(2017), 31.januar))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(
                Periode(1.januar, 16.januar)
            ), førsteFraværsdag = 1.januar, refusjon = Refusjon(INNTEKT, null, emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            INNTEKT,
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        assertTilstander(
            1.vedtaksperiode,
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

        inspektør.also {
            assertFalse(it.personLogg.hasWarningsOrWorse())
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.navDagTeller)
            }
        }

        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode, listOf(
                Periode(1.januar(2020), 16.januar(2020))
            ), førsteFraværsdag = 1.januar(2020), refusjon = Refusjon(INNTEKT, null, emptyList())
        )
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(
            2.vedtaksperiode,
            INNTEKT,
            arbeidsforhold = arbeidsforhold,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2019) til 1.desember(2019) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                }
            )
        )
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            it.utbetalingstidslinjer(2.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }

            it.utbetaling(1).utbetalingstidslinje().inspektør.also { tidslinjeInspektør ->
                assertEquals(32, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(8, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)

            }
        }
    }

    @Test
    fun `bevarer avviste dager fra tidligere periode og avviser dager fra skjæringstidspunkt ved opptjening ok`() {
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER.toString(), 31.desember(2017), 31.januar))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(
                Periode(1.januar, 16.januar)
            ), førsteFraværsdag = 1.januar, refusjon = Refusjon(INNTEKT, null, emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            INNTEKT,
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser()
        håndterUtbetalingsgodkjenning()

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }

        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode, listOf(
                Periode(1.januar(2020), 16.januar(2020))
            ), førsteFraværsdag = 1.januar(2020), refusjon = Refusjon(INNTEKT, null, emptyList())
        )
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(
            2.vedtaksperiode,
            INNTEKT,
            arbeidsforhold = arbeidsforhold,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2019) til 1.desember(2019) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                }
            )
        )
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            it.utbetalingstidslinjer(2.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }

            it.utbetaling(1).utbetalingstidslinje().inspektør.also { tidslinjeInspektør ->
                assertEquals(32, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(8, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(22, tidslinjeInspektør.avvistDagTeller)

            }
        }
    }

    @Test
    fun `en periode med under minimum inntekt avviser ikke dager for etterfølgende periode med vilkårsgrunnlag ok`() {
        val lavInntekt = 1000.månedlig
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = lavInntekt
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            lavInntekt,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt lavInntekt
                    }
                }
            )

        )
        håndterYtelser()
        håndterUtbetalingsgodkjenning()

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }

        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode, listOf(
                Periode(1.januar(2020), 16.januar(2020))
            ), førsteFraværsdag = 1.januar(2020), refusjon = Refusjon(INNTEKT, null, emptyList())
        )
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(
            2.vedtaksperiode,
            INNTEKT,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2019) til 1.desember(2019) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                }
            )
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
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

        inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            it.utbetalingstidslinjer(2.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.navDagTeller)
            }

            it.utbetaling(1).utbetalingstidslinje().inspektør.also { tidslinjeInspektør ->
                assertEquals(32, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(8, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.navDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)

            }
        }
    }

    @Test
    fun `en periode med ikke medlem, en infotrygdperiode, en etterfølgende periode med vilkårsgrunnlag ok`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(
                Periode(1.januar, 16.januar)
            ), førsteFraværsdag = 1.januar, refusjon = Refusjon(INNTEKT, null, emptyList())
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
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
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
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.desember, 31.desember, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.desember, INNTEKT, true))
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
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
            ), førsteFraværsdag = 1.januar(2020), refusjon = Refusjon(INNTEKT, null, emptyList())
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
        håndterUtbetalt(3.vedtaksperiode)

        assertTilstander(
            3.vedtaksperiode,
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

        inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
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
    fun `periode som begynner på søndag skal ikke gi warning på krav om minimuminntekt`() {
        håndterSykmelding(Sykmeldingsperiode(15.mars(2020), 8.april(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(16.mars(2020), 31.mars(2020))),
            førsteFraværsdag = 16.mars(2020),
            refusjon = Refusjon(INNTEKT, null, emptyList())
        )
        håndterSøknad(Sykdom(15.mars(2020), 8.april(2020), 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.mars(2019) til 1.februar(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        assertEquals(ForlengelseFraInfotrygd.NEI, inspektør.forlengelseFraInfotrygd(1.vedtaksperiode))
        assertFalse(inspektør.personLogg.hasWarningsOrWorse())
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `Forkasting skal ikke påvirke tilstanden til AVSLUTTET_UTEN_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(31.mars(2020), 13.april(2020), 100.prosent))
        håndterSøknadArbeidsgiver(Sykdom(31.mars(2020), 13.april(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(4.juni(2020), 11.juni(2020), 100.prosent))
        håndterSøknadArbeidsgiver(Sykdom(4.juni(2020), 11.juni(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(12.juni(2020), 25.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(12.juni(2020), 25.juni(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(4.juni(2020), 19.juni(2020))),
            førsteFraværsdag = 4.juni(2020)
        )
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode, INNTEKT)
        håndterSykmelding(Sykmeldingsperiode(26.juni(2020), 17.juli(2020), 100.prosent))
        håndterSøknad(
            Sykdom(26.juni(2020), 17.juli(2020), 100.prosent)
        )
        assertDoesNotThrow {
            håndterPåminnelse(4.vedtaksperiode, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, LocalDateTime.now().minusMonths(2))
        }
    }

    @Test
    fun `sykdomstidslinje tømmes helt når perioder blir forkastet, dersom det ikke finnes noen perioder igjen`() {
        håndterSykmelding(Sykmeldingsperiode(7.april(2020), 30.april(2020), 100.prosent)) // (1.vedtaksperiode)
        håndterSøknad(Sykdom(7.april(2020), 30.april(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.juni(2020), 21.juni(2020), 100.prosent)) // (2.vedtaksperiode)
        håndterSøknad(Sykdom(8.juni(2020), 21.juni(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(8.juni(2020), 23.juni(2020))), førsteFraværsdag = 8.juni(2020))

        håndterSykmelding(Sykmeldingsperiode(21.juni(2020), 11.juli(2020), 100.prosent))
        assertNull(inspektør.sykdomstidslinje.periode(), "Sykdomstidslinja burde ikke ha en periode her, for alle vedtaksperioder skal være forkastet pga overlappende sykmelding")
    }

    @Test
    fun `Inntektsmelding utvider ikke perioden med arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni, 30.juni, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.juni, 16.juni)), førsteFraværsdag = 1.juni)
        håndterSøknad(Sykdom(1.juni, 30.juni, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2017) til 1.mai(2018) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        håndterSykmelding(Sykmeldingsperiode(9.juli, 31.juli, 100.prosent))
        håndterSøknad(Sykdom(9.juli, 31.juli, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.juni, 16.juni)), førsteFraværsdag = 9.juli)

        inspektør.also {
            assertEquals(Periode(1.juni, 30.juni), it.vedtaksperioder(1.vedtaksperiode).periode())
            assertEquals(Periode(9.juli, 31.juli), it.vedtaksperioder(2.vedtaksperiode).periode())
        }
    }

    @Test
    fun `uønskede ukjente dager`() {
        håndterSykmelding(Sykmeldingsperiode(18.august(2020), 6.september(2020), 100.prosent))
        håndterSøknad(Sykdom(18.august(2020), 6.september(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(20.august(2020), 13.september(2020), 100.prosent)) // Denne blir ignorert
        håndterSøknad(Sykdom(20.august(2020), 13.september(2020), 100.prosent))

        håndterSykmelding(
            Sykmeldingsperiode(
                14.september(2020),
                20.september(2020),
                100.prosent
            )
        ) // Dette fører til ukjent-dager i sykdomshistorikken mellom 6.9. og 14.9.
        håndterSøknad(Sykdom(14.september(2020), 20.september(2020), 100.prosent))

        person =
            SerialisertPerson(person.serialize().json).deserialize() // Må gjøre serde for å få gjenskapt at ukjent-dager blir *instansiert* i sykdomshistorikken

        håndterPåminnelse(
            1.vedtaksperiode,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            LocalDateTime.now().minusDays(200)
        ) // Etter forkast ble det liggende igjen ukjent-dager forrest i sykdomstidslinjen

        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 27.juli(2020), 13.september(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 27.juli(2020), INNTEKT, true))
        )
        håndterYtelser(2.vedtaksperiode)

        assertEquals(40, 248 - inspektør.gjenståendeSykedager(2.vedtaksperiode))
    }

    @Test
    fun `skal ikke lage ny arbeidsgiverperiode ved forkasting`() {
        håndterSykmelding(Sykmeldingsperiode(30.juni(2020), 14.august(2020), 100.prosent))
        håndterSøknadArbeidsgiver(Sykdom(30.juni(2020), 14.august(2020), 100.prosent))
        håndterSøknad(Sykdom(30.juni(2020), 14.august(2020), 100.prosent))

        håndterInntektsmelding(listOf(Periode(30.juni(2020), 14.august(2020))), førsteFraværsdag = 30.juni(2020))

        håndterSykmelding(Sykmeldingsperiode(30.juni(2020), 22.august(2020), 100.prosent))
        håndterSøknad(Sykdom(30.juni(2020), 22.august(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(23.august(2020), 14.september(2020), 100.prosent))
        håndterSøknad(Sykdom(23.august(2020), 14.september(2020), 100.prosent))

        person = SerialisertPerson(person.serialize().json).deserialize()

        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 17.august(2020), 22.august(2020), 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER.toString(), 17.august(2020), INNTEKT, true))

        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(2.vedtaksperiode)

        inspektør.also {
            it.utbetalingstidslinjer(2.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(0, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
            }
        }
    }

    @Test
    fun `Avsluttet vedtaksperiode forkastes ikke ved overlapp`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 30.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterSøknad(Sykdom(1.januar, 30.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(9.januar, 31.januar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertEquals(1, inspektør.vedtaksperiodeTeller)
    }

    @Test
    fun `Kan ta inn ferie fra IT som overlapper med perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 28.februar))

        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.januar, 31.januar, 100.prosent, 1000.daglig),
            Friperiode(1.februar, 28.februar),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.januar, INNTEKT, true))
        )

        inspektør.also {
            assertEquals(1.februar, it.sykdomstidslinje.førsteDag())
            assertEquals(28.februar, it.sykdomstidslinje.sisteDag())
            assertEquals(28, it.sykdomstidslinje.inspektør.dagteller[Feriedag::class])
        }
    }

    @Test
    fun `'arbeidGjenopptatt' i løpet av arbeidsgiverperioden i arbeidsgiversøknad medfører ikke NavDager og påvirker derfor ikke telling av 26 uker opphold`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 21.mars, 100.prosent))
        håndterSøknadArbeidsgiver(
            Sykdom(1.mars, 21.mars, 100.prosent),
            arbeidsperiode = Arbeid(12.mars, 21.mars)
        )
        håndterInntektsmelding(listOf(1.mars til 16.mars))

        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        val sykdomstidslinjedagerForAndrePeriode = inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).inspektør.dagteller
        assertEquals(21, sykdomstidslinjedagerForAndrePeriode.values.reduce { acc, i -> acc + i })
        assertEquals(7, sykdomstidslinjedagerForAndrePeriode[Sykedag::class])
        assertEquals(4, sykdomstidslinjedagerForAndrePeriode[SykHelgedag::class])
        assertEquals(2, sykdomstidslinjedagerForAndrePeriode[FriskHelgedag::class])
        assertEquals(3, sykdomstidslinjedagerForAndrePeriode[Dag.Arbeidsdag::class])
        assertEquals(5, sykdomstidslinjedagerForAndrePeriode[Arbeidsgiverdag::class])

        val maksdatoFør26UkerOpphold = LocalDate.of(2018, 12, 28)
        assertEquals(maksdatoFør26UkerOpphold, inspektør.maksdatoVedSisteVedtak())

        nyttVedtak(1.august, 21.august)

        val maksdatoEtter26UkerOpphold = LocalDate.of(2019, 7, 30)
        assertEquals(maksdatoEtter26UkerOpphold, inspektør.maksdatoVedSisteVedtak())
        assertEquals(3, inspektør.forbrukteSykedager(1))
        assertEquals(245, inspektør.gjenståendeSykedager(3.vedtaksperiode))
    }

    @Test
    fun `'arbeidGjenopptatt' i løpet av arbeidsgiverperioden i arbeidsgiversøknad medfører ikke forbrukte sykedager`() {
        nyttVedtak(1.januar, 31.januar)
        assertEquals(28.desember, inspektør.maksdatoVedSisteVedtak())
        håndterSykmelding(Sykmeldingsperiode(1.mars, 21.mars, 100.prosent))
        håndterSøknadArbeidsgiver(
            Sykdom(1.mars, 21.mars, 100.prosent),
            arbeidsperiode = Arbeid(12.mars, 21.mars)
        )
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        assertEquals(28.desember, inspektør.maksdatoVedSisteVedtak())
        nyttVedtak(1.mai, 21.mai)
        assertEquals(12.april(2019), inspektør.maksdatoVedSisteVedtak())
    }

    @Test
    fun `går til Infortrygd dersom det finnes en utbetalt periode i infotrygdhistorikken som starter etter denne perioden`() {
        håndterSykmelding(Sykmeldingsperiode(12.juli(2021), 1.august(2021), 100.prosent))
        håndterSøknad(Sykdom(12.juli(2021), 1.august(2021), 100.prosent), Ferie(12.juli(2021), 1.august(2021)))

        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 2.august(2021), INNTEKT, true), Inntektsopplysning(ORGNUMMER.toString(), 22.juni(2021), INNTEKT, true))
        val utbetlinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 2.august(2021), 28.september(2021), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 22.juni(2021), 11.juli(2021), 100.prosent, 1000.daglig)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = utbetlinger, inntektshistorikk = inntektshistorikk)
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
    fun `Legger på warning hvis første utbetalingsdag i infotrygdforlengelse er mellom første og sekstende mai 2021 og sykepengegrunnlag har blitt begrenset av gammelt G-beløp`() {
        håndterSykmelding(Sykmeldingsperiode(12.juli(2021), 1.august(2021), 100.prosent))
        håndterSøknad(Sykdom(12.juli(2021), 1.august(2021), 100.prosent))

        val inntektHøyereEllerLikGammeltGBeløp = Grunnbeløp.`6G`.beløp(LocalDate.of(2021, 4, 30))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 2.mai(2021), inntektHøyereEllerLikGammeltGBeløp, true))
        val utbetlinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 2.mai(2021), 21.juni(2021), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 22.juni(2021), 11.juli(2021), 100.prosent, 1000.daglig)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = utbetlinger, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertTrue(inspektør.personLogg.warn().toString().contains("Første utbetalingsdag er i Infotrygd og mellom 1. og 16. mai. Kontroller at riktig grunnbeløp er brukt."))
    }

    @Test
    fun `Legger ikke på warning hvis inntekt ikke blir begrenset av gammel 6G`() {
        håndterSykmelding(Sykmeldingsperiode(12.juli(2021), 1.august(2021), 100.prosent))
        håndterSøknad(Sykdom(12.juli(2021), 1.august(2021), 100.prosent))

        val inntektLavereEnnGammeltGBeløp = Grunnbeløp.`6G`.beløp(LocalDate.of(2021, 4, 30)).minus(1.00.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 2.mai(2021), inntektLavereEnnGammeltGBeløp, true))
        val utbetlinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 2.mai(2021), 21.juni(2021), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 22.juni(2021), 11.juli(2021), 100.prosent, 1000.daglig)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = utbetlinger, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)

        assertFalse(inspektør.personLogg.warn().toString().contains("Første utbetalingsdag er i Infotrygd og mellom 1. og 16. mai. Kontroller at riktig grunnbeløp er brukt."))
    }
}
