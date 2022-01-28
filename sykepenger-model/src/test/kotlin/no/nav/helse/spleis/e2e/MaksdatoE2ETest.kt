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
        // oppretter forlengelse fom 182 dager etter maksdato
        forrigePeriode = nyPeriodeMedYtelser(forrigePeriode)
        person.søppelbøtte(hendelselogg, forrigePeriode) // simulerer at perioden forkastes; f.eks. fordi spesialtilfellet med fortsatt syk etter 182 dager må håndteres manuelt i IT
        forrigePeriode = nyPeriode(forrigePeriode)
        val siste = observatør.sisteVedtaksperiode()
        val inntektsmeldingId = inntektsmeldinger.keys.also { check(it.size == 1) { "forventer bare én inntektsmelding" } }.first()
        håndterInntektsmeldingReplay(inntektsmeldingId, siste.id(ORGNUMMER))
        assertSisteTilstand(siste, TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP) {
            "denne perioden skal under ingen omstendigheter utbetales fordi personen ikke har vært på arbeid etter maksdato"
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
