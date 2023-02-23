package no.nav.helse.spleis.e2e.infotrygd

import java.time.LocalDate
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertEtterspurt
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteForkastetPeriodeTilstand
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.håndterPåminnelse
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class InfotrygdTest : AbstractEndToEndTest() {

    @Test
    fun `Infotrygdhistorikk som ikke medfører forlengelse`() {
        håndterUtbetalingshistorikkEtterInfotrygdendring(
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 30.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
    }

    @Test
    fun `eksisterende infotrygdforlengelse`() {
        createOvergangFraInfotrygdPerson()
        nyPeriode(1.mars til 31.mars)
        håndterYtelser(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
        assertIngenFunksjonelleFeil()
    }
}