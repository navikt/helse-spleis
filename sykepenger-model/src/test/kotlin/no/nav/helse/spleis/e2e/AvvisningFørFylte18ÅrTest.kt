package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.testhelpers.november
import no.nav.helse.testhelpers.oktober
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AvvisningFørFylte18ÅrTest : AbstractEndToEndTest() {
    private companion object {
        const val FYLLER_18_ÅR_2_NOVEMBER = "02110075045"
    }

    @BeforeEach
    fun setup() {
        createTestPerson(FYLLER_18_ÅR_2_NOVEMBER)
    }

    @Test
    fun `avviser sykmeldinger for person under 18 år ved søknadstidspunkt`() {
        val meldingsreferanse = håndterSykmelding(Sykmeldingsperiode(1.oktober, 31.oktober, 100.prosent), mottatt = 1.november.atStartOfDay(), fnr = FYLLER_18_ÅR_2_NOVEMBER)
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertTrue(Sykmelding.AVSLAGSTEKST_PERSON_UNDER_18_ÅR in observatør.hendelseIkkeHåndtert(meldingsreferanse).årsaker)
    }

    @Test
    fun `avviser ikke sykmeldinger for person som er 18 år ved søknadstidspunkt`() {
        val meldingsreferanse = håndterSykmelding(Sykmeldingsperiode(1.oktober, 31.oktober, 100.prosent), mottatt = 2.november.atStartOfDay(), fnr = FYLLER_18_ÅR_2_NOVEMBER)
        assertFalse(hendelselogg.hasErrorsOrWorse())
        assertFalse(observatør.hendelseIkkeHåndtertEventer.containsKey(meldingsreferanse))
    }
}
