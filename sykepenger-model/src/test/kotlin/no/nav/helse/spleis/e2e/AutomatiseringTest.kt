package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AutomatiseringTest: AbstractEndToEndTest() {

    @Test
    fun `inntektsmelding med ny førsteFraværsdag i påfølgende periode får warning`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(3.januar, 18.januar)),
            3.januar
        )
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        assertNoWarnings(inspektør)

        håndterSykmelding(Sykmeldingsperiode(27.januar, 7.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(27.januar, 7.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(Periode(3.januar, 18.januar)),
            27.januar // Ikke 3.jan
        )

        assertTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_UTBETALINGSGRUNNLAG,
        )
        assertTilstander(
            2.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_UFERDIG_FORLENGELSE
        )
        assertTrue(inspektør.personLogg.toString().contains("Første fraværsdag i inntektsmeldingen er forskjellig fra foregående tilstøtende periode"))
    }
}
