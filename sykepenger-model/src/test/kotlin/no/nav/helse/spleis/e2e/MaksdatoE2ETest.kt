package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MaksdatoE2ETest : AbstractDslTest() {

    @Test
    fun `hensyntar tidligere arbeidsgivere fra IT`() {
        a2 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a2, 1.januar, 31.januar))
        }
        a1 {
            nyPeriode(mars, a1)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)
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
    }

    @Test
    fun `hensyntar ikke senere arbeidsgivere fra IT`() {
        a2 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a2, 1.april, 30.april))
        }
        a1 {
            nyPeriode(mars, a1)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_IT_1, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            inspektør(a1).sisteMaksdato(1.vedtaksperiode).also {
                assertEquals(10, it.antallForbrukteDager)
                assertEquals(238, it.gjenståendeDager)
                assertEquals(27.februar(2019), it.maksdato)
            }
        }
    }

    @Test
    fun `avviser perioder med sammenhengende sykdom etter 26 uker fra maksdato`() {
        a1 {
            var forrigePeriode = januar
            nyttVedtak(forrigePeriode, 100.prosent)
            // setter opp vedtaksperioder frem til maksdato
            repeat(11) { _ ->
                forrigePeriode = nestePeriode(forrigePeriode)
                forlengVedtak(forrigePeriode)
            }
            // setter opp vedtaksperioder 182 dager etter maksdato
            repeat(6) { _ ->
                forrigePeriode = nestePeriode(forrigePeriode)
                val vedtaksperiode = nyPeriode(forrigePeriode, 100.prosent)
                håndterYtelser(vedtaksperiode)
                håndterUtbetalingsgodkjenning(vedtaksperiode)
            }

            // oppretter forlengelse fom 182 dager etter maksdato
            nyPeriodeMedYtelser(forrigePeriode)
            val siste = observatør.sisteVedtaksperiodeId(a1)
            assertFunksjonellFeil(Varselkode.RV_VV_9, siste.filter())
            assertSisteTilstand(siste, TIL_INFOTRYGD) {
                "Disse periodene skal kastes ut pr nå"
            }
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
        val id = observatør.sisteVedtaksperiodeId(a1)
        håndterYtelser(id)
        return nestePeriode
    }

    private fun nestePeriode(forrigePeriode: Periode): Periode {
        val nesteMåned = forrigePeriode.start.plusMonths(1)
        return nesteMåned til nesteMåned.plusDays(nesteMåned.lengthOfMonth().toLong() - 1)
    }
}
