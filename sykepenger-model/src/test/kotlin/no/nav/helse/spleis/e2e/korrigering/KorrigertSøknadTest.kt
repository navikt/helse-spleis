package no.nav.helse.spleis.e2e.korrigering

import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.*
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.person.TilstandType.*
import no.nav.helse.spleis.e2e.*
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class KorrigertSøknadTest : AbstractEndToEndTest() {

    @Test
    fun `Arbeidsdag i søknad nr 2 kaster ut perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 30.januar, 100.prosent), Arbeid(31.januar, 31.januar))
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Søknad som er lengre tilbake støttes ikke`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Søknad som er lengre frem støttes ikke`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 1.februar, 100.prosent))
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Støtter ikke korrigerende søknad på utbetalt vedtaksperiode`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(31.januar, 31.januar))
        assertErrors(inspektør)
    }

    @Test
    fun `Korrigerer fridager til sykedag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Permisjon(30.januar, 30.januar), Ferie(31.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[30.januar] is Sykedag)
            assertTrue(it[31.januar] is Sykedag)
        }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertEquals(2, inspektør.personLogg.warn().size)
    }

    @Test
    fun `Korrigerer sykedag til feriedag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Permisjon(30.januar, 30.januar), Ferie(31.januar, 31.januar))
        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[30.januar] is Permisjonsdag)
            assertTrue(it[31.januar] is Feriedag)
        }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertEquals(2, inspektør.personLogg.warn().size)
    }

    @Test
    fun `Korrigerer grad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        object : OppdragVisitor {
            var grad: Int? = null
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
                this.grad = grad
            }
        }.also {
            inspektør.arbeidsgiverOppdrag.first().accept(it)
            assertEquals(50, it.grad)
        }
    }

    @Test
    fun `Korrigerer feriedag til sykedag i forlengelse`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(28.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[28.februar] is Sykedag)
        }
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(
            2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK
        )
        assertEquals(1, inspektør.personLogg.warn().size)
    }

    @Test
    fun `Blir værende i nåværende tilstand dersom søknad kommer inn i AVVENTER_INNTEKTSMELDING_UFERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(2.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(2.februar, 28.februar, 100.prosent), Ferie(28.februar, 28.februar))
        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[28.februar] is Feriedag)
        }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
        assertEquals(1, inspektør.personLogg.warn().size)
    }

    @Test
    fun `Blir værende i nåværende tilstand dersom søknad kommer inn i AVVENTER_UFERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(2.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 2.februar)
        håndterSøknad(Sykdom(2.februar, 28.februar, 100.prosent), Ferie(28.februar, 28.februar))
        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[28.februar] is Feriedag)
        }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP, AVVENTER_UFERDIG_GAP)
        assertEquals(1, inspektør.personLogg.warn().size)
    }

    @Test
    fun `Blir værende i nåværende tilstand dersom søknad kommer inn i AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(28.februar, 28.februar))

        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[28.februar] is Feriedag)
        }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
        assertEquals(1, inspektør.personLogg.warn().size)
    }

    @Test
    fun `Blir værende i nåværende tilstand dersom søknad kommer inn i AVVENTER_UFERDIG_FORLENGELSE`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(11.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent), Ferie(31.januar, 31.januar))

        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[31.januar] is Feriedag)
        }
        assertTilstander(
            1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
        assertEquals(1, inspektør.personLogg.warn().size)
    }

    @Test
    fun `Blir værende i nåværende tilstand dersom søknad kommer inn i AVVENTER_INNTEKTSMELDING_FERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(31.januar, 31.januar))

        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[31.januar] is Feriedag)
        }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertEquals(1, inspektør.personLogg.warn().size)
    }

    @Test
    fun `Blir værende i nåværende tilstand dersom søknad kommer inn i AVVENTER_VILKÅRSPRØVING`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(31.januar, 31.januar))

        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[31.januar] is Feriedag)
        }
        assertTilstander(
            1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING
        )
        assertEquals(1, inspektør.personLogg.warn().size)
    }

    @Test
    fun `Blir værende i nåværende tilstand dersom søknad kommer inn i AVVENTER_HISTORIKK`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(31.januar, 31.januar))

        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[31.januar] is Feriedag)
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK
        )
        assertEquals(1, inspektør.personLogg.warn().size)
    }

    @Test
    fun `Går tilbake til AVVENTER_HISTORIKK når søknaden kommer inn i AVVENTER_SIMULERING`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(31.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[31.januar] is Sykedag)
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_HISTORIKK
        )
        assertEquals(1, inspektør.personLogg.warn().size)
    }

    @Test
    fun `Går tilbake til AVVENTER_HISTORIKK når søknaden kommer inn i AVVENTER_GODKJENNING`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(31.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        inspektør.sykdomstidslinje.inspektør.also {
            assertTrue(it[31.januar] is Sykedag)
        }
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
            AVVENTER_HISTORIKK
        )
        assertEquals(1, inspektør.personLogg.warn().size)
        assertTrue(observatør.reberegnedeVedtaksperioder.contains(1.vedtaksperiode(ORGNUMMER)))
    }
}
