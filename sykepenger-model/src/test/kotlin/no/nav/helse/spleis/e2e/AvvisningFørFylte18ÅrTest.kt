package no.nav.helse.spleis.e2e

import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AvvisningFørFylte18ÅrTest : AbstractEndToEndTest() {
    private companion object {
        val FYLLER_18_ÅR_2_NOVEMBER = Personidentifikator("02110075045")
    }

    @BeforeEach
    fun setup() {
        createTestPerson(FYLLER_18_ÅR_2_NOVEMBER, 2.november(2000))
    }

    @Test
    fun `avviser sykmeldinger for person under 18 år ved søknadstidspunkt`() {
        håndterSykmelding(
            Sykmeldingsperiode(1.oktober, 31.oktober),
            mottatt = 1.november.atStartOfDay(),
            fnr = FYLLER_18_ÅR_2_NOVEMBER
        )
        håndterSøknad(
            oktober,
            sendtTilNAVEllerArbeidsgiver = 1.november,
            fnr = FYLLER_18_ÅR_2_NOVEMBER
        )
        assertTrue(hendelselogg.harFunksjonelleFeilEllerVerre())
        assertForkastetPeriodeTilstander(1, START, TIL_INFOTRYGD)
    }

    @Test
    fun `avviser ikke sykmeldinger for person som er 18 år ved søknadstidspunkt`() {
        håndterSykmelding(
            Sykmeldingsperiode(1.oktober, 31.oktober),
            mottatt = 2.november.atStartOfDay(),
            fnr = FYLLER_18_ÅR_2_NOVEMBER
        )
        håndterSøknad(
            oktober,
            sendtTilNAVEllerArbeidsgiver = 2.november,
            fnr = FYLLER_18_ÅR_2_NOVEMBER
        )
        assertFalse(hendelselogg.harFunksjonelleFeilEllerVerre())
        assertTilstander(0, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
    }
}
