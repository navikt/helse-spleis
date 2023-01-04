package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.rapids_rivers.asLocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow


internal class TrengerArbeidsgiveropplysningerTest : AbstractEndToEndMediatorTest() {
    @Test
    fun `Sender ut forespørsel om opplysninger fra arbeidsgiver med riktig format`() = Toggle.Splarbeidsbros.enable {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))

        val event = testRapid.inspektør.siste("trenger_opplysninger_fra_arbeidsgiver")

        assertDoesNotThrow { UUID.fromString(event["@id"].asText()) }
        assertDoesNotThrow { UUID.fromString(event["vedtaksperiodeId"].asText()) }
        assertEquals(UNG_PERSON_FNR_2018, event["fødselsnummer"].asText())
        assertEquals(ORGNUMMER, event["organisasjonsnummer"].asText())

        assertEquals(1.januar, event["sykmeldingsperioder"][0]["fom"].asLocalDate())
        assertEquals(31.januar, event["sykmeldingsperioder"][0]["tom"].asLocalDate())

        val forespurteOpplysninger = event["forespurteOpplysninger"].asIterable().toList()
        assertEquals("Inntekt", forespurteOpplysninger[0]["opplysningstype"].asText())
        assertEquals("Refusjon", forespurteOpplysninger[1]["opplysningstype"].asText())

        val arbeidsgiverperiode = forespurteOpplysninger[2]
        assertEquals("Arbeidsgiverperiode", arbeidsgiverperiode["opplysningstype"].asText())
        assertEquals(1.januar, arbeidsgiverperiode["forslag"][0]["fom"].asLocalDate())
        assertEquals(16.januar, arbeidsgiverperiode["forslag"][0]["tom"].asLocalDate())
    }
}