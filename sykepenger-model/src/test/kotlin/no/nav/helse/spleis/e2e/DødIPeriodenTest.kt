package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.testhelpers.UtbetalingstidslinjeInspektør
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DødIPeriodenTest : AbstractEndToEndTest() {

    @Test
    fun `Dager etter dødsdato avvises`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, dødsdato = 18.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, dødsdato = 18.januar)
        UtbetalingstidslinjeInspektør(inspektør.utbetalinger.first().utbetalingstidslinje()).also {
            assertEquals(13, it.avvistDagTeller)
        }
    }

    @Test
    fun `Ingen dager avvises når dødsdato er etter perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, dødsdato = 1.februar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, dødsdato = 1.februar)
        UtbetalingstidslinjeInspektør(inspektør.utbetalinger.first().utbetalingstidslinje()).also {
            assertEquals(0, it.avvistDagTeller)
        }
    }

    @Test
    fun `Alle dager avvises når dødsdato er før perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, dødsdato = 31.desember(2017))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, dødsdato = 31.desember(2017))
        UtbetalingstidslinjeInspektør(inspektør.utbetalinger.first().utbetalingstidslinje()).also {
            assertEquals(15, it.avvistDagTeller)
            assertEquals(16, it.arbeidsgiverperiodeDagTeller)
            assertEquals(0, it.navHelgDagTeller)
        }
    }

}
