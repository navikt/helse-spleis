package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class ArbeidskategoriKodeTest : AbstractEndToEndTest() {

    @Test
    fun `kaster periode til Infotrygd ved avvikende arbeidskategoriKode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.desember(2017), 31.desember(2017), 100.prosent, 15000.daglig),
            arbeidskategorikoder = mapOf("05" to 1.desember(2017))
        )
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK, TIL_INFOTRYGD
        )
    }

    @Test
    fun `kaster ikke periode til Infotrygd ved avvikende arbeidskategoriKode når skjæringstidspunktet ligger i Spleis`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        val arbeidskategorier = mapOf("05" to 1.desember(2017))
        val historikk = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.desember(2017), 28.desember(2017), 100.prosent, 15000.daglig))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, *historikk, arbeidskategorikoder = arbeidskategorier)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG, AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING
        )
    }
}
