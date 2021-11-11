package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.november
import no.nav.helse.testhelpers.oktober
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagE2ETest : AbstractEndToEndTest() {

    @Test
    fun `Gjør ikke vilkårsprøving om vi ikke har inntekt fra inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(28.oktober(2020), 8.november(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(28.oktober(2020), 8.november(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.november(2020), 22.november(2020), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(9.november(2020), 22.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(28.oktober(2021), 8.november(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(28.oktober(2021), 8.november(2021), 100.prosent))

        håndterYtelser(2.vedtaksperiode)

        assertNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertError(2.vedtaksperiode, "Forventer minst ett sykepengegrunnlag som er fra inntektsmelding eller Infotrygd")
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
    }
}
