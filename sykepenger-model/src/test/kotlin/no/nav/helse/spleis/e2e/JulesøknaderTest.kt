package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.mars
import no.nav.helse.testhelpers.november
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class JulesøknaderTest : AbstractEndToEndTest() {

    @Test
    fun `julesøknader for 2021 fungerer`() {
        håndterSykmelding(Sykmeldingsperiode(15.november(2021), 15.desember(2021), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(15.november(2021), 15.desember(2021), 100.prosent), sendtTilNav = 17.november(2021))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(15.november(2021), 1.desember(2021))),
            førsteFraværsdag = 15.november(2021),
            refusjon = Inntektsmelding.Refusjon(
                31000.månedlig,
                1.mars(2022),
                emptyList()
            )
        )

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TilstandType.TIL_INFOTRYGD
        )
    }
}
