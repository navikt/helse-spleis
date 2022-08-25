package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ForeldetSøknadGateTest : AbstractEndToEndTest() {

    @Test
    fun `Kort ikke-foreldet periode skal ikke logge at den må til manuell`(){
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        assertFalse(inspektør.vedtaksperioder(1.vedtaksperiode).skalLoggeDersomPeriodenSkalTilManuellGrunnetForeldelse())
    }

    @Test
    fun `Kort foreldet periode skal ikke logge at den må til manuell`(){
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.februar(2019))
        assertFalse(inspektør.vedtaksperioder(1.vedtaksperiode).skalLoggeDersomPeriodenSkalTilManuellGrunnetForeldelse())
    }

    @Test
    fun `Ferie utenfor agp skal ikke logge at den må til manuell`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(17.januar, 31.januar))
        assertFalse(inspektør.vedtaksperioder(1.vedtaksperiode).skalLoggeDersomPeriodenSkalTilManuellGrunnetForeldelse())
    }

    @Test
    fun `Bare ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 31.januar))
        val vedtaksperiode = inspektør.vedtaksperioder(1.vedtaksperiode)
        assertFalse(vedtaksperiode.skalLoggeDersomPeriodenSkalTilManuellGrunnetForeldelse())
    }

    @Test
    fun `Foreldet dag innenfor agp skal ikke logge at den må til manuell`() {
        håndterSykmelding(Sykmeldingsperiode(16.januar, 16.februar, 100.prosent))
        håndterSøknad(
            Sykdom(16.januar, 16.februar, 100.prosent),
            Ferie(1.februar, 16.februar),
            sendtTilNAVEllerArbeidsgiver = 1.mai
        )
        assertFalse(inspektør.vedtaksperioder(1.vedtaksperiode).skalLoggeDersomPeriodenSkalTilManuellGrunnetForeldelse())
    }

    @Test
    fun `Foreldet dag utenfor agp skal logge at den må til manuell`() {
        håndterSykmelding(Sykmeldingsperiode(15.januar, 16.februar, 100.prosent))
        håndterSøknad(
            Sykdom(15.januar, 16.februar, 100.prosent),
            Ferie(1.februar, 16.februar),
            sendtTilNAVEllerArbeidsgiver = 1.mai
        )
        assertTrue(inspektør.vedtaksperioder(1.vedtaksperiode).skalLoggeDersomPeriodenSkalTilManuellGrunnetForeldelse())
    }

    @Test
    fun `Foreldet forlengelse innenfor agp skal ikke logge at den må til manuell`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(6.januar, 10.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
        assertFalse(inspektør.vedtaksperioder(1.vedtaksperiode).skalLoggeDersomPeriodenSkalTilManuellGrunnetForeldelse())
        assertFalse(inspektør.vedtaksperioder(2.vedtaksperiode).skalLoggeDersomPeriodenSkalTilManuellGrunnetForeldelse())
    }

    @Test
    fun `Foreldet forlengelse utenfor agp skal logge at den må til manuell`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(6.januar, 17.januar, 100.prosent))
        håndterSøknad(Sykdom(6.januar, 17.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
        assertFalse(inspektør.vedtaksperioder(1.vedtaksperiode).skalLoggeDersomPeriodenSkalTilManuellGrunnetForeldelse())
        assertTrue(inspektør.vedtaksperioder(2.vedtaksperiode).skalLoggeDersomPeriodenSkalTilManuellGrunnetForeldelse())
    }

}
