package no.nav.helse.bugs_showstoppers

import java.time.LocalDateTime
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertActivities
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterPåminnelse
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Feriedag
import no.nav.helse.sykdomstidslinje.Dag.FriskHelgedag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class E2EEpic3Test : AbstractEndToEndTest() {

    @Test
    fun `inntektsmelding starter etter sykmeldingsperioden`() {
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a1, 3.april(2019), 30.april(2019)),
            ArbeidsgiverUtbetalingsperiode(a1, 18.mars(2018), 2.april(2018)),
            ArbeidsgiverUtbetalingsperiode(a1, 29.november(2017), 3.desember(2017)),
            ArbeidsgiverUtbetalingsperiode(a1, 13.november(2017), 28.november(2017))
        )
        this@E2EEpic3Test.håndterUtbetalingshistorikkEtterInfotrygdendring(*utbetalinger)

        håndterSykmelding(Sykmeldingsperiode(15.januar(2020), 12.februar(2020)))
        håndterSøknad(Sykdom(15.januar(2020), 12.februar(2020), 100.prosent))
        håndterArbeidsgiveropplysninger(
            listOf(Periode(16.januar(2020), 31.januar(2020))),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@E2EEpic3Test.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `periode uten sykedager`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 9.januar))
        håndterSykmelding(Sykmeldingsperiode(15.januar, 16.januar))
        håndterSøknad(Sykdom(3.januar, 4.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(
                3.januar til 4.januar,
                8.januar til 9.januar,
                15.januar til 26.januar
            )
        )

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING
        )

        håndterSøknad(Sykdom(8.januar, 9.januar, 100.prosent))
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING
        )

        håndterSøknad(Sykdom(15.januar, 16.januar, 100.prosent))

        assertIngenFunksjonelleFeil()
        assertActivities()
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `Periode som kun er innenfor arbeidsgiverperioden der inntekten ikke gjelder venter på arbeidsgiversøknad før den går til AVSLUTTET_UTEN_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.januar))
        håndterSykmelding(Sykmeldingsperiode(4.januar, 20.januar))
        håndterInntektsmelding(
            listOf(
                1.januar til 1.januar,
                3.januar til 17.januar
            ),
            førsteFraværsdag = 3.januar
        )

        håndterSøknad(Sykdom(1.januar, 1.januar, 100.prosent))
        håndterSøknad(Sykdom(4.januar, 20.januar, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `Første periode får søknad, men ikke inntektsmelding og må nå makstid før de neste kan fortsette behandling`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2017), 30.november(2017)))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 3.januar))
        håndterSykmelding(Sykmeldingsperiode(5.januar, 22.januar))
        håndterSøknad(Sykdom(1.november(2017), 30.november(2017), 100.prosent))
        håndterSøknad(Sykdom(3.januar, 3.januar, 100.prosent))
        håndterSøknad(Sykdom(5.januar, 22.januar, 100.prosent))
        håndterArbeidsgiveropplysninger(
            listOf(
                3.januar til 3.januar,
                5.januar til 20.januar
            ),
            vedtaksperiodeIdInnhenter = 3.vedtaksperiode
        )

        assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        this@E2EEpic3Test.håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, LocalDateTime.now().minusDays(181))
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
    }

    @Test
    fun `Ikke samsvar mellom sykmeldinger og inntektsmelding - første periode får hverken søknad eller inntektsmelding og må nå makstid før de neste kan fortsette behandling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.januar))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 3.januar))
        håndterSykmelding(Sykmeldingsperiode(5.januar, 22.januar))
        håndterSøknad(Sykdom(3.januar, 3.januar, 100.prosent))
        håndterSøknad(Sykdom(5.januar, 22.januar, 100.prosent))

        håndterArbeidsgiveropplysninger(
            listOf(
                3.januar til 3.januar,
                5.januar til 20.januar
            ),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING
        )
    }

    @Test
    fun `Periode som kun er innenfor arbeidsgiverperioden avsluttes IKKE før forrige periode avsluttes`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar))
        håndterSykmelding(Sykmeldingsperiode(12.februar, 19.februar))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
        håndterSøknad(Sykdom(12.februar, 19.februar, 100.prosent))


        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `enkeltstående sykedag i arbeidsgiverperiode-gap`() {
        håndterSykmelding(Sykmeldingsperiode(10.februar(2020), 12.februar(2020)))
        håndterSykmelding(Sykmeldingsperiode(14.februar(2020), 14.februar(2020)))
        håndterSykmelding(Sykmeldingsperiode(27.februar(2020), 28.februar(2020)))


        håndterSøknad(Sykdom(10.februar(2020), 12.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(14.februar(2020), 14.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(27.februar(2020), 28.februar(2020), 100.prosent))

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                10.februar(2020) til 12.februar(2020),
                27.februar(2020) til 10.mars(2020)
            )
        )
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `person med gammel sykmelding`() {
        // OBS: Disse kastes ikke ut fordi de er for gamle. De kastes ut fordi de kommer out of order
        håndterSykmelding(Sykmeldingsperiode(13.januar(2020), 31.januar(2020)))
        håndterSøknad(Sykdom(13.januar(2020), 31.januar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.februar(2017), 15.februar(2017)), mottatt = 31.januar(2020).atStartOfDay())

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `Inntektsmelding, etter søknad, overskriver sykedager før arbeidsgiverperiode med arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(7.januar, 28.januar))
        håndterSøknad(Sykdom(7.januar, 28.januar, 100.prosent))
        // Need to extend Arbeidsdag from first Arbeidsgiverperiode to beginning of Vedtaksperiode, considering weekends
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(Periode(9.januar, 24.januar)),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(2, inspektør.sykdomshistorikk.size) // TODO
        assertEquals(22, inspektør.sykdomshistorikk.sykdomstidslinje().count())
        assertEquals(7.januar, inspektør.sykdomshistorikk.sykdomstidslinje().førsteDag())
        assertEquals(FriskHelgedag::class, inspektør.sykdomshistorikk.sykdomstidslinje()[7.januar]::class)
        assertEquals(Dag.Arbeidsdag::class, inspektør.sykdomshistorikk.sykdomstidslinje()[8.januar]::class)
        assertEquals(9.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(28.januar, inspektør.sykdomshistorikk.sykdomstidslinje().sisteDag())
    }

    @Test
    fun `Inntektsmelding, før søknad, overskriver sykedager før arbeidsgiverperiode med arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(7.januar, 28.januar))
        // Need to extend Arbeidsdag from first Arbeidsgiverperiode to beginning of Vedtaksperiode, considering weekends
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(9.januar, 24.januar)),
            førsteFraværsdag = 9.januar
        )
        håndterSøknad(Sykdom(7.januar, 28.januar, 100.prosent))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(2, inspektør.sykdomshistorikk.size)
        assertEquals(22, inspektør.sykdomshistorikk.sykdomstidslinje().count())
        assertEquals(7.januar, inspektør.sykdomshistorikk.sykdomstidslinje().førsteDag())
        assertEquals(FriskHelgedag::class, inspektør.sykdomshistorikk.sykdomstidslinje()[7.januar]::class)
        assertEquals(Dag.Arbeidsdag::class, inspektør.sykdomshistorikk.sykdomstidslinje()[8.januar]::class)
        assertEquals(9.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(28.januar, inspektør.sykdomshistorikk.sykdomstidslinje().sisteDag())
    }

    @Test
    fun `simulering av periode der tilstøtende ikke ble utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(28.januar(2020), 10.februar(2020)))
        håndterSykmelding(Sykmeldingsperiode(11.februar(2020), 21.februar(2020)))
        håndterSøknad(Sykdom(28.januar(2020), 10.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(28.januar(2020), 12.februar(2020)))
        )

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@E2EEpic3Test.håndterYtelser(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `simulering av periode der tilstøtende ble utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(17.januar(2020), 10.februar(2020)))
        håndterSykmelding(Sykmeldingsperiode(11.februar(2020), 21.februar(2020)))
        håndterSøknad(Sykdom(17.januar(2020), 10.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(Periode(17.januar(2020), 2.februar(2020))),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@E2EEpic3Test.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@E2EEpic3Test.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        this@E2EEpic3Test.håndterYtelser(2.vedtaksperiode)

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
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `helg i gap i arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar))
        håndterSøknad(Sykdom(3.januar, 10.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(3.januar til 4.januar, 9.januar til 10.januar)
        )
        inspektør.also {
            assertEquals(4, it.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(2, it.sykdomstidslinje.inspektør.dagteller[FriskHelgedag::class])
            assertEquals(2, it.sykdomstidslinje.inspektør.dagteller[Dag.Arbeidsdag::class])
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `Syk, en arbeidsdag, ferie og syk`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(1.januar(2020), 1.januar(2020)),
                Periode(3.januar(2020), 17.januar(2020))
            ),
            førsteFraværsdag = 11.januar(2020)
        )
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent), Ferie(3.januar(2020), 10.januar(2020)), sendtTilNAVEllerArbeidsgiver = 1.februar(2020))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@E2EEpic3Test.håndterYtelser(1.vedtaksperiode)

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
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `Syk, mange arbeidsdager, syk igjen på en lørdag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(1.januar(2020), 1.januar(2020)),
                Periode(11.januar(2020), 25.januar(2020))
            ),
            førsteFraværsdag = 11.januar(2020)
        )
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.februar(2020))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@E2EEpic3Test.håndterYtelser(1.vedtaksperiode)

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
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `ikke medlem avviser alle dager og legger på warning`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(INNTEKT, null, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Nei)
        assertVarsel(Varselkode.RV_MV_2, 1.vedtaksperiode.filter())
        this@E2EEpic3Test.håndterYtelser()
        this@E2EEpic3Test.håndterUtbetalingsgodkjenning()

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        assertTrue(personlogg.harVarslerEllerVerre())
        inspektør.also {
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }
    }

    @Test
    fun `opptjening ikke ok avviser ikke dager før gjeldende skjæringstidspunkt`() {
        val arbeidsforhold = listOf(Triple(a1, 1.januar(2017), 31.januar))
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(INNTEKT, null, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, arbeidsforhold = arbeidsforhold)
        this@E2EEpic3Test.håndterYtelser()
        håndterSimulering()
        this@E2EEpic3Test.håndterUtbetalingsgodkjenning()
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

        assertFalse(personlogg.harVarslerEllerVerre())
        inspektør.also {
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.navDagTeller)
            }
        }

        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterArbeidsgiveropplysninger(
            listOf(Periode(1.januar(2020), 16.januar(2020))),
            refusjon = Refusjon(INNTEKT, null, emptyList()),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode, arbeidsforhold = arbeidsforhold)
        this@E2EEpic3Test.håndterYtelser(2.vedtaksperiode)
        assertVarsler(listOf(Varselkode.RV_OV_1, Varselkode.RV_VV_1), 2.vedtaksperiode.filter())
        this@E2EEpic3Test.håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        assertTrue(personlogg.harVarslerEllerVerre())
        inspektør.also {
            it.utbetalingstidslinjer(2.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }

            it.vedtaksperioder(2.vedtaksperiode).inspektør.utbetalingstidslinje.inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)

            }
        }
    }

    @Test
    fun `bevarer avviste dager fra tidligere periode og avviser dager fra skjæringstidspunkt ved opptjening ok`() {
        val arbeidsforhold = listOf(Triple(a1, 31.desember(2017), 31.januar))
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(INNTEKT, null, emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, arbeidsforhold = arbeidsforhold)
        this@E2EEpic3Test.håndterYtelser()
        assertVarsel(Varselkode.RV_OV_1, 1.vedtaksperiode.filter())
        this@E2EEpic3Test.håndterUtbetalingsgodkjenning()

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        assertTrue(personlogg.harVarslerEllerVerre())
        inspektør.also {
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }

        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterArbeidsgiveropplysninger(
            listOf(Periode(1.januar(2020), 16.januar(2020))),
            refusjon = Refusjon(INNTEKT, null, emptyList()),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode, arbeidsforhold = arbeidsforhold)
        this@E2EEpic3Test.håndterYtelser(2.vedtaksperiode)
        assertVarsler(listOf(Varselkode.RV_OV_1, Varselkode.RV_VV_1), 2.vedtaksperiode.filter())
        this@E2EEpic3Test.håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        assertTrue(personlogg.harVarslerEllerVerre())
        inspektør.also {
            it.utbetalingstidslinjer(2.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }

            it.vedtaksperioder(2.vedtaksperiode).inspektør.utbetalingstidslinje.inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)

            }
        }
    }

    @Test
    fun `en periode med under minimum inntekt avviser ikke dager for etterfølgende periode med vilkårsgrunnlag ok`() {
        val lavInntekt = 1000.månedlig
        håndterSykmelding(januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = lavInntekt,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@E2EEpic3Test.håndterYtelser()
        this@E2EEpic3Test.håndterUtbetalingsgodkjenning()
        assertVarsel(Varselkode.RV_SV_1, 1.vedtaksperiode.filter())
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        assertTrue(personlogg.harVarslerEllerVerre())
        inspektør.also {
            it.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }

        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)))
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterArbeidsgiveropplysninger(
            listOf(Periode(1.januar(2020), 16.januar(2020))),
            refusjon = Refusjon(INNTEKT, null, emptyList()),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@E2EEpic3Test.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@E2EEpic3Test.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertTrue(personlogg.harVarslerEllerVerre())
        inspektør.also {
            it.utbetalingstidslinjer(2.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.navDagTeller)
            }

            it.vedtaksperioder(2.vedtaksperiode).inspektør.utbetalingstidslinje.inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.navDagTeller)
                assertEquals(0, tidslinjeInspektør.avvistDagTeller)

            }
        }
    }

    @Test
    fun `Forkasting skal ikke påvirke tilstanden til AVSLUTTET_UTEN_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(31.mars(2020), 13.april(2020)))
        håndterSøknad(Sykdom(31.mars(2020), 13.april(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(4.juni(2020), 11.juni(2020)))
        håndterSøknad(Sykdom(4.juni(2020), 11.juni(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(12.juni(2020), 25.juni(2020)))
        håndterSøknad(Sykdom(12.juni(2020), 25.juni(2020), 100.prosent))

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(4.juni(2020), 19.juni(2020)))
        )
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(26.juni(2020), 17.juli(2020)))
        håndterSøknad(
            Sykdom(26.juni(2020), 17.juli(2020), 100.prosent)
        )
        assertDoesNotThrow {
            this@E2EEpic3Test.håndterPåminnelse(4.vedtaksperiode, AVVENTER_INNTEKTSMELDING, LocalDateTime.now().minusMonths(2))
        }
    }

    @Test
    fun `Inntektsmelding utvider ikke perioden med arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni, 30.juni))
        håndterInntektsmelding(
            listOf(Periode(1.juni, 16.juni)),
            førsteFraværsdag = 1.juni
        )
        håndterSøknad(Sykdom(1.juni, 30.juni, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@E2EEpic3Test.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@E2EEpic3Test.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        håndterSykmelding(Sykmeldingsperiode(9.juli, 31.juli))
        håndterSøknad(Sykdom(9.juli, 31.juli, 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.juni, 16.juni)),
            førsteFraværsdag = 9.juli
        )

        inspektør.also {
            assertEquals(Periode(1.juni, 30.juni), it.vedtaksperioder(1.vedtaksperiode).periode)
            assertEquals(Periode(9.juli, 31.juli), it.vedtaksperioder(2.vedtaksperiode).periode)
        }
    }

    @Test
    fun `Avsluttet vedtaksperiode forkastes ikke ved overlapp`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 30.januar))
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 1.januar
        )
        håndterSøknad(Sykdom(1.januar, 30.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@E2EEpic3Test.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@E2EEpic3Test.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(9.januar, 31.januar))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertEquals(1, inspektør.vedtaksperiodeTeller)
    }
}
