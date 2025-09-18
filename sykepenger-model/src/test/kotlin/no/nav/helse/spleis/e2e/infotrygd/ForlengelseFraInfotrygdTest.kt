package no.nav.helse.spleis.e2e.infotrygd

import no.nav.helse.desember
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.arbeidsgiveropplysninger.TrengerArbeidsgiveropplysningerTest.Companion.assertEtterspurt
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class ForlengelseFraInfotrygdTest : AbstractEndToEndTest() {

    @Test
    fun `forkaster ikke førstegangsbehandling selv om det er lagret inntekter i IT`() {
        this@ForlengelseFraInfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring()
        håndterSøknad(januar)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `når en periode går Til Infotrygd avsluttes påfølgende, tilstøtende perioder også`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(februar)
        håndterSykmelding(Sykmeldingsperiode(18.mars, 31.mars))
        håndterSøknad(Sykdom(18.mars, 31.mars, 100.prosent))
        this@ForlengelseFraInfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
        observatør.assertEtterspurt(1.vedtaksperiode.id(a1), PersonObserver.Inntekt::class, PersonObserver.Refusjon::class)

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `forlenger ferieperiode i Infotrygd på samme arbeidsgiver`() {
        this@ForlengelseFraInfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(Friperiode(1.januar, 31.januar))
        nyPeriode(februar)
        assertForlengerInfotrygdperiode()
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `forlenger ferieperiode i Infotrygd på samme arbeidsgiver - reagerer på endring`() {
        nyPeriode(februar)
        this@ForlengelseFraInfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(Friperiode(1.januar, 31.januar))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `forlenger utbetaling i Infotrygd på samme arbeidsgiver`() {
        this@ForlengelseFraInfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
        )
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(februar)
        assertForlengerInfotrygdperiode()
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a1)
    }

    @Test
    fun `forlenger utbetaling i Infotrygd på annen arbeidsgiver`() {
        this@ForlengelseFraInfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a2, 1.januar, 31.januar))
        nyPeriode(februar, a1)
        assertForlengerInfotrygdperiode()
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a1)
    }

    @Test
    fun `bare ferie - etter infotrygdutbetaling`() {
        this@ForlengelseFraInfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(a1, 1.desember(2017), 31.desember(2017))
        )
        håndterSykmelding(januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(1.januar, 31.januar))
        assertForlengerInfotrygdperiode()
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    private fun assertForlengerInfotrygdperiode() {
        assertFunksjonellFeil(Varselkode.RV_IT_14, 1.vedtaksperiode.filter())
    }
}
