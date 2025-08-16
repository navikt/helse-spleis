package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
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
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_4
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SamletSykdomsgradE2ETest : AbstractDslTest() {

    @Test
    fun `hele perioden avvises`() {
        (a1 og a2).nyeVedtak(1.januar til 4.februar)
        a1 { nyPeriode(5.februar til 9.februar, 10.prosent) }
        a2 { nyPeriode(5.februar til 9.februar, 10.prosent) }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(RV_VV_4, 2.vedtaksperiode.filter())
            assertEquals(listOf(5.februar, 6.februar, 7.februar, 8.februar, 9.februar), inspektør.utbetalingstidslinjer(2.vedtaksperiode).inspektør.avvistedatoer)
        }
    }

    @Test
    fun `avviser dager under 20 prosent`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSøknad(Sykdom(1.januar, 20.januar, 19.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

            assertVarsel(RV_VV_4, 1.vedtaksperiode.filter())
            assertEquals(Utbetalingstatus.GODKJENT_UTEN_UTBETALING, inspektør.utbetalingtilstand(0))
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            assertTrue(utbetalingstidslinje[17.januar] is Utbetalingsdag.AvvistDag)
            assertTrue(utbetalingstidslinje[18.januar] is Utbetalingsdag.AvvistDag)
            assertTrue(utbetalingstidslinje[19.januar] is Utbetalingsdag.AvvistDag)
            assertEquals(3, utbetalingstidslinje.inspektør.avvistDagTeller)
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
        }
    }

    @Test
    fun `avviser dager under 20 prosent på forlengelser`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSøknad(Sykdom(1.januar, 20.januar, 19.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            assertVarsel(RV_VV_4, 1.vedtaksperiode.filter())

            håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar))
            håndterSøknad(21.januar til 31.januar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            assertTrue(utbetalingstidslinje[17.januar] is Utbetalingsdag.AvvistDag)
            assertTrue(utbetalingstidslinje[18.januar] is Utbetalingsdag.AvvistDag)
            assertTrue(utbetalingstidslinje[19.januar] is Utbetalingsdag.AvvistDag)
            assertEquals(3, utbetalingstidslinje.inspektør.avvistDagTeller)
            assertEquals(Utbetalingstatus.OVERFØRT, inspektør.utbetalingtilstand(1))
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING
            )
        }
    }

    @Test
    fun `ny periode med egen arbeidsgiverperiode skal ikke ha warning pga sykdomsgrad som gjelder forrige periode`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSøknad(Sykdom(1.januar, 20.januar, 19.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            håndterSykmelding(Sykmeldingsperiode(1.mars, 20.mars))
            håndterSøknad(1.mars til 20.mars)
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertVarsler(listOf(RV_VV_4), 1.vedtaksperiode.filter())
            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        }
    }
}
