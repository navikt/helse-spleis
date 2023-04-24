package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class ArbeidsopplysningerKorrigertTest : AbstractEndToEndMediatorTest() {
    @Test
    fun `Sender ut arbeidsopplysninger_korrigert når arbeidsgiveropplysninger endres av en korrigerende IM`() = Toggle.Splarbeidsbros.enable {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        )
        val korrigertInntektsmelding = sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 31.januar)), førsteFraværsdag = 1.januar, beregnetInntekt = 30000.0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        val korrigerendeInntektsmelding = sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 31.januar)), førsteFraværsdag = 1.januar, beregnetInntekt = 30500.0)
        val event = testRapid.inspektør.siste("arbeidsgiveropplysninger_korrigert")

        assertEquals(korrigertInntektsmelding.first, UUID.fromString(event["korrigertInntektsmeldingId"].asText()))
        assertEquals(korrigerendeInntektsmelding.first, UUID.fromString(event["korrigerendeInntektsopplysningId"].asText()))
        assertEquals("INNTEKTSMELDING", event["korrigerendeInntektektsopplysningstype"].asText())
    }
}