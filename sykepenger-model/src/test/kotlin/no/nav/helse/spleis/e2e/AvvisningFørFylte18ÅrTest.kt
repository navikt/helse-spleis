package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.TilstandType.*
import no.nav.helse.somFødselsnummer
import no.nav.helse.testhelpers.november
import no.nav.helse.testhelpers.oktober
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AvvisningFørFylte18ÅrTest : AbstractEndToEndTest() {
    private companion object {
        val FYLLER_18_ÅR_2_NOVEMBER = "02110075045".somFødselsnummer()
    }

    @BeforeEach
    fun setup() {
        createTestPerson(FYLLER_18_ÅR_2_NOVEMBER)
    }

    @Test
    fun `avviser sykmeldinger for person under 18 år ved søknadstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.oktober, 31.oktober, 100.prosent), mottatt = 1.november.atStartOfDay(), fnr = FYLLER_18_ÅR_2_NOVEMBER)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.oktober, 31.oktober, 100.prosent), sendtTilNav = 1.november, fnr = FYLLER_18_ÅR_2_NOVEMBER)
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertForkastetPeriodeTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
    }

    @Test
    fun `avviser ikke sykmeldinger for person som er 18 år ved søknadstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.oktober, 31.oktober, 100.prosent), mottatt = 2.november.atStartOfDay(), fnr = FYLLER_18_ÅR_2_NOVEMBER)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.oktober, 31.oktober, 100.prosent), sendtTilNav = 2.november, fnr = FYLLER_18_ÅR_2_NOVEMBER)
        assertFalse(hendelselogg.hasErrorsOrWorse())
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }
}
