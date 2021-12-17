package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.november
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class JulesøknaderTest : AbstractEndToEndTest() {

    @Test
    fun `julesøknader for 2021 fungerer`() {
        håndterSykmelding(Sykmeldingsperiode(15.november(2021), 15.desember(2021), 100.prosent))
        håndterSøknad(Sykdom(15.november(2021), 15.desember(2021), 100.prosent), sendtTilNav = 17.november(2021))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(15.november(2021), 1.desember(2021))),
            førsteFraværsdag = 15.november(2021),
            refusjon = Inntektsmelding.Refusjon(
                INGEN,
                null,
                emptyList()
            )
        )

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        assertSisteForkastetPeriodeTilstand(
            ORGNUMMER,
            1.vedtaksperiode,
            TilstandType.TIL_INFOTRYGD
        )
    }
}
