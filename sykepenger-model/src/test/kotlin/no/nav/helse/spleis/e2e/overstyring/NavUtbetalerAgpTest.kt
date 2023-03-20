package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class NavUtbetalerAgpTest: AbstractEndToEndTest() {

    @Test
    fun `Overstyrer agp til sykedagNav - ingen refusjon`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(Inntekt.INGEN, null, emptyList()))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.SykedagNav, 100) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val utbetalinger = inspektør.utbetalinger
        Assertions.assertEquals(2, utbetalinger.size)
        utbetalinger.last().inspektør.also { overstyringen ->
            Assertions.assertEquals(1, overstyringen.personOppdrag.size)
            Assertions.assertEquals(0, overstyringen.arbeidsgiverOppdrag.size)
            overstyringen.personOppdrag[0].inspektør.also { linje ->
                Assertions.assertEquals(1.januar til 31.januar, linje.fom til linje.tom)
                Assertions.assertEquals(1431, linje.beløp)
            }
        }
    }

    @Test
    fun `Overstyrer agp til sykedagNav - refusjon`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.SykedagNav, 100) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val utbetalinger = inspektør.utbetalinger
        Assertions.assertEquals(2, utbetalinger.size)
        utbetalinger.last().inspektør.also { overstyringen ->
            Assertions.assertEquals(0, overstyringen.personOppdrag.size)
            Assertions.assertEquals(1, overstyringen.arbeidsgiverOppdrag.size)
            overstyringen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                Assertions.assertEquals(1.januar til 31.januar, linje.fom til linje.tom)
                Assertions.assertEquals(1431, linje.beløp)
            }
        }
    }
}