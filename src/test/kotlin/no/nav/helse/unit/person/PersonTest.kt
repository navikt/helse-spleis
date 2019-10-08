package no.nav.helse.unit.person

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants.søknad
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.person.domain.*
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import no.nav.inntektsmeldingkontrakt.Inntektsmelding as Inntektsmeldingkontrakt

internal class PersonTest {

    @Test internal fun `ny søknad fører til at sakskompleks trigger en sakskompleks endret hendelse`() {
        val observer = TestObserver()
        Person().also {
            it.addObserver(observer)
            it.håndterNySøknad(søknad(status = SoknadsstatusDTO.NY))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, observer.sakskomplekstilstand)
    }

    @Test internal fun `sendt søknad uten sak trigger sakskompleks endret-hendelse`() {
        val observer = TestObserver()
        Person().also {
            it.addObserver(observer)
            it.håndterSendtSøknad(søknad(status = SoknadsstatusDTO.SENDT))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }

    @Test internal fun `inntektsmelding uten sak trigger sakskompleks endret-hendelse`() {
        val observer = TestObserver()
        Person().also {
            it.addObserver(observer)
            it.håndterInntektsmelding(inntektsmelding(
                    virksomhetsnummer = "123456789"
            ))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }

    @Test internal fun `inntektsmelding med sak trigger sakskompleks endret-hendelse`() {
        val orgnr = "123456789"
        val observer = TestObserver()
        Person().also {
            it.håndterNySøknad(søknad(
                    status = SoknadsstatusDTO.NY,
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = orgnr
                    )))

            it.addObserver(observer)
            it.håndterInntektsmelding(inntektsmelding(
                    virksomhetsnummer = orgnr
            ))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, observer.sakskomplekstilstand)
    }

    @Test internal fun `inntektsmelding uten virksomhetsnummer kaster exception`() {
        val observer = TestObserver()
        Person().also {
            it.addObserver(observer)
            assertThrows<UtenforOmfangException> {
                it.håndterInntektsmelding(inntektsmelding())
            }
        }
    }

    @Test internal fun `sendt søknad trigger sakskompleks endret-hendelse`() {
        val orgnr = "123456789"
        val observer = TestObserver()
        Person().also {
            it.håndterNySøknad(søknad(
                    status = SoknadsstatusDTO.NY,
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = orgnr
                    )))

            it.addObserver(observer)
            it.håndterSendtSøknad(søknad(
                    status = SoknadsstatusDTO.SENDT,
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = orgnr
                    )
            ))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, observer.sakskomplekstilstand)
    }

    private class TestObserver: PersonObserver {
        internal var wasTriggered = false
        internal var personEndret = false
        internal var sakskomplekstilstand: Sakskompleks.TilstandType? = null

        override fun personEndret(person: Person) {
            personEndret = true
        }

        override fun sakskompleksChanged(event: SakskompleksObserver.StateChangeEvent) {
            wasTriggered = true
            sakskomplekstilstand = event.currentState
        }
    }

    private fun inntektsmeldingMottattTilstand() : Sakskompleks {
        TODO()
    }

   private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private fun inntektsmelding(virksomhetsnummer: String? = null) = Inntektsmelding(objectMapper.valueToTree(Inntektsmeldingkontrakt(
            inntektsmeldingId = "",
            arbeidstakerFnr = "",
            arbeidstakerAktorId = "",
            virksomhetsnummer = virksomhetsnummer,
            arbeidsgiverFnr = null,
            arbeidsgiverAktorId = null,
            arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
            arbeidsforholdId = null,
            beregnetInntekt = null,
            refusjon = Refusjon(
                    beloepPrMnd = null,
                    opphoersdato = null
            ),
            endringIRefusjoner = emptyList(),
            opphoerAvNaturalytelser = emptyList(),
            gjenopptakelseNaturalytelser = emptyList(),
            arbeidsgiverperioder = emptyList(),
            status = Status.GYLDIG,
            arkivreferanse = ""
    )))
}
