package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class MaksdatoE2ETest : AbstractEndToEndTest() {

    @Test
    fun `hensyntar tidligere arbeidsgivere fra IT`() {
        håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(
                a2,
                1.januar,
                31.januar,
                100.prosent,
                INNTEKT
            )
        )
        nyPeriode(mars, a1)
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        inspektør(a1).sisteMaksdato(1.vedtaksperiode).also {
            assertEquals(33, it.antallForbrukteDager)
            assertEquals(215, it.gjenståendeDager)
            assertEquals(25.januar(2019), it.maksdato)
        }
    }

    @Test
    fun `hensyntar ikke senere arbeidsgivere fra IT`() {
        håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(
                a2,
                1.april,
                30.april,
                100.prosent,
                INNTEKT
            )
        )
        nyPeriode(mars, a1)
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        inspektør(a1).sisteMaksdato(1.vedtaksperiode).also {
            assertEquals(10, it.antallForbrukteDager)
            assertEquals(238, it.gjenståendeDager)
            assertEquals(27.februar(2019), it.maksdato)
        }
    }

    @Test
    fun `syk etter maksdato`() {
        var forrigePeriode = januar
        nyttVedtak(forrigePeriode, 100.prosent)
        // setter opp vedtaksperioder frem til 182 dager etter maksdato
        repeat(17) { _ ->
            forrigePeriode = nestePeriode(forrigePeriode)
            forlengVedtak(forrigePeriode)
        }
        // oppretter forlengelse fom 182 dager etter maksdato: denne blir kastet til Infotrygd
        forrigePeriode = nyPeriodeMedYtelser(forrigePeriode)
        val nestSiste = observatør.sisteVedtaksperiode()
        assertSisteTilstand(nestSiste, TIL_INFOTRYGD) {
            "Disse periodene skal kastes ut pr nå"
        }
        forrigePeriode = nyPeriode(forrigePeriode)
        val siste = observatør.sisteVedtaksperiode()
        assertNotEquals(nestSiste, siste)
        assertForkastetPeriodeTilstander(siste, START, TIL_INFOTRYGD)
    }

    @Test
    fun `avviser perioder med sammenhengende sykdom etter 26 uker fra maksdato`() {
        var forrigePeriode = januar
        nyttVedtak(forrigePeriode, 100.prosent)
        // setter opp vedtaksperioder frem til 182 dager etter maksdato
        repeat(17) { _ ->
            forrigePeriode = nestePeriode(forrigePeriode)
            forlengVedtak(forrigePeriode)
        }
        // oppretter forlengelse fom 182 dager etter maksdato
        forrigePeriode = nyPeriodeMedYtelser(forrigePeriode)
        val siste = observatør.sisteVedtaksperiode()
        assertFunksjonellFeil("Bruker er fortsatt syk 26 uker etter maksdato", siste.filter())
        assertSisteTilstand(siste, TIL_INFOTRYGD) {
            "Disse periodene skal kastes ut pr nå"
        }
    }

    private fun nyPeriode(forrigePeriode: Periode): Periode {
        val nestePeriode = nestePeriode(forrigePeriode)
        håndterSykmelding(Sykmeldingsperiode(nestePeriode.start, nestePeriode.endInclusive))
        håndterSøknad(nestePeriode)
        return nestePeriode
    }

    private fun nyPeriodeMedYtelser(forrigePeriode: Periode): Periode {
        val nestePeriode = nyPeriode(forrigePeriode)
        val id: IdInnhenter = observatør.sisteVedtaksperiode()
        håndterYtelser(id)
        return nestePeriode
    }

    private fun nestePeriode(forrigePeriode: Periode): Periode {
        val nesteMåned = forrigePeriode.start.plusMonths(1)
        return nesteMåned til nesteMåned.plusDays(nesteMåned.lengthOfMonth().toLong() - 1)
    }
}
