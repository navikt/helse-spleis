package no.nav.helse.spleis.e2e

import no.nav.helse.desember
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class ArbeidskategoriKodeTest : AbstractEndToEndTest() {

    @Test
    fun `kaster ikke periode til Infotrygd ved avvikende arbeidskategoriKode når skjæringstidspunktet ligger i Spleis`() {
        val arbeidskategorier = mapOf("05" to 1.desember(2017))
        val historikk = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.desember(2017), 28.desember(2017), 100.prosent, 15000.daglig))
        håndterUtbetalingshistorikkEtterInfotrygdendring(
            *historikk, arbeidskategorikoder = arbeidskategorier, inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.desember(2017), INNTEKT, true))
        )

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }
}
