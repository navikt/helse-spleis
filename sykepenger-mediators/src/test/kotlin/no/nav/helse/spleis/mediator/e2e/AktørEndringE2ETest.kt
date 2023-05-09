package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class AktørEndringE2ETest : AbstractEndToEndMediatorTest() {
    private companion object {
        private const val FNR1 = "12029240045"
        private const val FNR2 = "12029277777"
    }

    @Test
    fun `person får nytt fnr - behandling fortsetter på samme personjson`() {
        sendSøknad(fnr = FNR1, perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendSøknad(fnr = FNR2, perioder = listOf(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100)), historiskeFolkeregisteridenter = listOf(FNR1))

        assertEquals(1, antallUnikePersoner())
        assertEquals(1, antallPersoner())
        assertEquals(2, antallPersonalias())
        val meldinger = testRapid.inspektør.meldinger("vedtaksperiode_endret")
        assertEquals(4, meldinger.size)
        assertEquals(FNR1, meldinger[0].path("fødselsnummer").asText())
        assertEquals(FNR1, meldinger[1].path("fødselsnummer").asText())
        assertEquals(FNR2, meldinger[2].path("fødselsnummer").asText())
        assertEquals(FNR2, meldinger[3].path("fødselsnummer").asText())
    }

    @Test
    fun `to ulike personer - finner sammenheng etterpå`() {
        sendSøknad(fnr = FNR1, perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendSøknad(fnr = FNR2, perioder = listOf(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100)))

        sendSøknad(fnr = FNR2, perioder = listOf(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100)), historiskeFolkeregisteridenter = listOf(FNR1))

        assertEquals(2, antallUnikePersoner())
        assertEquals(2, antallPersoner())
        assertEquals(2, antallPersonalias())
        assertEquals(1, antallPersonalias(FNR1))
        assertEquals(1, antallPersonalias(FNR2))
        val meldinger = testRapid.inspektør.meldinger("vedtaksperiode_endret")
        assertEquals(4, meldinger.size)
        assertEquals(FNR1, meldinger[0].path("fødselsnummer").asText())
        assertEquals(FNR1, meldinger[1].path("fødselsnummer").asText())
        assertEquals(FNR2, meldinger[2].path("fødselsnummer").asText())
        assertEquals(FNR2, meldinger[3].path("fødselsnummer").asText())

        testRapid.inspektør.siste("vedtaksperiode_forkastet").also { melding ->
            assertEquals(FNR2, melding.path("fødselsnummer").asText())
        }

        testRapid.inspektør.siste("aktivitetslogg_ny_aktivitet").also { melding ->
            val error = melding.path("aktiviteter").first { it.path("melding").asText() == "Personen har blitt behandlet på en tidligere ident" }
            assertEquals("FUNKSJONELL_FEIL", error.path("nivå").asText())
        }
    }
}
