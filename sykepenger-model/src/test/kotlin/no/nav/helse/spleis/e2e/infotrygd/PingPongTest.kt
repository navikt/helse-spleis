package no.nav.helse.spleis.e2e.infotrygd

import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertSisteForkastetPeriodeTilstand
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class PingPongTest : AbstractEndToEndTest() {

    @Test
    fun `skjæringstidspunktet endres som følge av historikk fra IT`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(10.februar, 28.februar)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.februar, 9.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 5.februar, INNTEKT, true)
        ))
        håndterYtelser(3.vedtaksperiode)
        assertFunksjonellFeil(Varselkode.RV_IT_33, 3.vedtaksperiode.filter(ORGNUMMER))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 3.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `Kaster ut alt om vi oppdager at en senere periode er utbetalt i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
        // Ingen søknad for første sykmelding - den sykmeldte sender den ikke inn eller vi er i et out of order-scenario

        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar))
        håndterInntektsmelding(listOf(1.februar til 16.februar))
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent))

        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(a1, 17.februar, 20.februar, 100.prosent, INNTEKT))
        assertSisteForkastetPeriodeTilstand(a1, 1.vedtaksperiode, TIL_INFOTRYGD)
    }
}
