package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class MaksdatoE2ETest : AbstractEndToEndTest() {

    @Test
    fun `syk etter maksdato`() {
        var forrigePeriode = 1.januar til 31.januar
        nyttVedtak(forrigePeriode.start, forrigePeriode.endInclusive, 100.prosent)
        // setter opp vedtaksperioder frem til 182 dager etter maksdato
        repeat(17) { _ ->
            forrigePeriode = nestePeriode(forrigePeriode)
            forlengVedtak(forrigePeriode.start, forrigePeriode.endInclusive)
        }
        // oppretter forlengelse fom 182 dager etter maksdato: denne blir kastet til Infotrygd
        forrigePeriode = nyPeriodeMedYtelser(forrigePeriode)
        assertSisteTilstand(observatør.sisteVedtaksperiode(), TilstandType.TIL_INFOTRYGD) {
            "Disse periodene skal kastes ut pr nå"
        }
        forrigePeriode = nyPeriode(forrigePeriode)
        val siste = observatør.sisteVedtaksperiode()
        val inntektsmeldingId = inntektsmeldinger.keys.also { check(it.size == 1) { "forventer bare én inntektsmelding" } }.first()
        håndterInntektsmeldingReplay(inntektsmeldingId, siste.id(ORGNUMMER))
        assertSisteTilstand(siste, TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK) {
            "denne perioden skal under ingen omstendigheter utbetales fordi personen ikke har vært på arbeid etter maksdato"
        }

    }

    @Test
    fun `avviser perioder med sammenhengende sykdom etter 26 uker fra maksdato`() {
        var forrigePeriode = 1.januar til 31.januar
        nyttVedtak(forrigePeriode.start, forrigePeriode.endInclusive, 100.prosent)
        // setter opp vedtaksperioder frem til 182 dager etter maksdato
        repeat(17) { _ ->
            forrigePeriode = nestePeriode(forrigePeriode)
            forlengVedtak(forrigePeriode.start, forrigePeriode.endInclusive)
        }
        // oppretter forlengelse fom 182 dager etter maksdato
        forrigePeriode = nyPeriodeMedYtelser(forrigePeriode)
        val siste = observatør.sisteVedtaksperiode()
        assertError("Bruker er fortsatt syk 26 uker etter maksdato", siste.filter())
        assertSisteTilstand(siste, TilstandType.TIL_INFOTRYGD) {
            "Disse periodene skal kastes ut pr nå"
        }
    }

    private fun nyPeriode(forrigePeriode: Periode): Periode {
        val nestePeriode = nestePeriode(forrigePeriode)
        håndterSykmelding(Sykmeldingsperiode(nestePeriode.start, nestePeriode.endInclusive, 100.prosent))
        val id: IdInnhenter = observatør.sisteVedtaksperiode()
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(nestePeriode.start, nestePeriode.endInclusive, 100.prosent))
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
