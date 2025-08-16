package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ForkastSykmeldingsperioderTest : AbstractDslTest() {

    @Test
    fun `Forkaster sykmeldingsperioder slik at den andre arbeidsgiveren kan behandles`() {
        a1 {
            håndterSykmelding(januar)
        }
        a2 {
            håndterSykmelding(januar)
        }

        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            val sisteVedtaksperiodeventer = observatør.vedtaksperiodeVenter.last()
            assertEquals(1.vedtaksperiode, sisteVedtaksperiodeventer.vedtaksperiodeId)
            assertEquals(1.vedtaksperiode, sisteVedtaksperiodeventer.venterPå.vedtaksperiodeId)
            assertEquals("SØKNAD", sisteVedtaksperiodeventer.venterPå.venteårsak.hva)
        }

        a2 {
            håndterForkastSykmeldingsperioder(31.januar til 31.januar)
        }

        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `Forkaster sykmeldingsperioder slik at den andre arbeidsgiveren gjenopptar behandling`() {
        a1 {
            håndterSykmelding(januar)
        }
        a2 {
            håndterSykmelding(januar)
        }

        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 31.januar))
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            val sisteVedtaksperiodeventer = observatør.vedtaksperiodeVenter.last()
            assertEquals(1.vedtaksperiode, sisteVedtaksperiodeventer.vedtaksperiodeId)
            assertEquals(1.vedtaksperiode, sisteVedtaksperiodeventer.venterPå.vedtaksperiodeId)
            assertEquals("SØKNAD", sisteVedtaksperiodeventer.venterPå.venteårsak.hva)

        }

        a2 {
            håndterForkastSykmeldingsperioder(31.januar til 31.januar)
        }

        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
        }
    }
}
