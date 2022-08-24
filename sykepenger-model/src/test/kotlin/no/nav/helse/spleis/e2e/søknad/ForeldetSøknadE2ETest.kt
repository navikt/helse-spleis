package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class ForeldetSøknadE2ETest : AbstractEndToEndTest() {
    @Test
    fun `forledet søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), mottatt = 1.januar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2019))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertVarsel("Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd")
        assertForventetFeil(
            forklaring = "foreldet søknad skal ikke lukkes, skal sendes til manuell",
            nå = {
                assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            },
            ønsket = {
                assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            }
        )
    }

    @Test
    fun `forledet søknad med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), mottatt = 1.januar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2019))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertVarsel("Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd")
        assertForventetFeil(
            forklaring = "foreldet søknad skal ikke lukkes, skal sendes til manuell",
            nå = {
                assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            },
            ønsket = {
                assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            }
        )
    }

    @Test
    fun `forledet søknad ved forlengelse`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), mottatt = 1.februar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.februar(2019))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        assertVarsel("Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd")
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
    }
}
