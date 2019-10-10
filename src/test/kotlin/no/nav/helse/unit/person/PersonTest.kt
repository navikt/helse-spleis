package no.nav.helse.unit.person

import no.nav.helse.TestConstants.inntektsmelding
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.TestConstants.sendtSøknad
import no.nav.helse.person.domain.*
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PersonTest {

    @Test
    internal fun `ny søknad fører til at sakskompleks trigger en sakskompleks endret hendelse`() {
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.addObserver(observer)
            it.håndterNySøknad(nySøknad())
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, observer.sakskomplekstilstand)
    }

    @Test
    internal fun `sendt søknad uten sak trigger sakskompleks endret-hendelse`() {
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.addObserver(observer)
            it.håndterSendtSøknad(sendtSøknad())
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding uten sak trigger sakskompleks endret-hendelse`() {
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.addObserver(observer)
            it.håndterInntektsmelding(inntektsmelding(
                    virksomhetsnummer = "123456789"
            ))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding med sak trigger sakskompleks endret-hendelse`() {
        val orgnr = "123456789"
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.håndterNySøknad(nySøknad(
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

    @Test
    internal fun `inntektsmelding uten virksomhetsnummer kaster exception`() {
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.addObserver(observer)
            assertThrows<UtenforOmfangException> {
                it.håndterInntektsmelding(inntektsmelding())
            }
        }
    }

    @Test
    internal fun `søknad uten arbeidsgiver kaster exception`() {
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.addObserver(observer)
            assertThrows<UtenforOmfangException> {
                it.håndterNySøknad(nySøknad(
                        arbeidsgiver = null
                ))
            }
        }
    }

    @Test
    internal fun `søknad uten organisasjonsnummer kaster exception`() {
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.addObserver(observer)
            assertThrows<UtenforOmfangException> {
                it.håndterNySøknad(nySøknad(
                        arbeidsgiver = ArbeidsgiverDTO(
                                navn = "En arbeidsgiver",
                                orgnummer = null
                        )
                ))
            }
        }
    }

    @Test
    internal fun `sendt søknad trigger sakskompleks endret-hendelse`() {
        val orgnr = "123456789"
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.håndterNySøknad(nySøknad(
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = orgnr
                    )))

            it.addObserver(observer)
            it.håndterSendtSøknad(sendtSøknad(
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = orgnr
                    )
            ))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, observer.sakskomplekstilstand)
    }

    private class TestObserver : PersonObserver {
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

    private fun inntektsmeldingMottattTilstand(): Sakskompleks {
        TODO()
    }
}
