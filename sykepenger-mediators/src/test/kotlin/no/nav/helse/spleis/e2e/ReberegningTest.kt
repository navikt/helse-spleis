package no.nav.helse.spleis.e2e

import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.FravarDTO
import no.nav.syfo.kafka.felles.FravarstypeDTO
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class ReberegningTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `vedtaksperiode_reberegnet event`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendKorrigerendeSøknad(
            listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            listOf(FravarDTO(31.januar, 31.januar, FravarstypeDTO.FERIE))
        )

        val event = testRapid.inspektør.siste("vedtaksperiode_reberegnet")
        assertNotNull(event)
        assertEquals(testRapid.inspektør.vedtaksperiodeId(0).toString(), event["vedtaksperiodeId"].asText())
        assertEquals(UNG_PERSON_FNR_2018, event["fødselsnummer"].asText())
        assertEquals(AKTØRID, event["aktørId"].asText())
        assertEquals(ORGNUMMER, event["organisasjonsnummer"].asText())
    }
}
