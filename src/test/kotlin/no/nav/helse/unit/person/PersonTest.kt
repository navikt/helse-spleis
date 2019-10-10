package no.nav.helse.unit.person

import no.nav.helse.TestConstants.inntektsmelding
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.TestConstants.sendtSøknad
import no.nav.helse.juli
import no.nav.helse.person.domain.*
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
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
    internal fun `ny sak blir opprettet når en ny søknad som ikke overlapper saken personen har fra før blir sendt inn`() {
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.addObserver(observer)
            it.håndterNySøknad(nySøknad(fom = 21.juli, tom=28.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=21.juli, tom=28.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, observer.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak trenger manuell behandling når en ny søknad overlapper sykdomstidslinjen i den eksisterende saken`() {
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.addObserver(observer)
            it.håndterNySøknad(nySøknad(fom = 10.juli, tom=22.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=10.juli, tom=22.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak trenger manuell behandling når vi mottar den andre sendte søknaden`() {
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 1.juli, tom=20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.addObserver(observer)
            it.håndterSendtSøknad(sendtSøknad(fom = 10.juli, tom=30.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=10.juli, tom=30.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }

    @Test
    internal fun `ny sak trenger manuell behandling når vi mottar den sendte søknaden først`() {
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=9.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.addObserver(observer)
            it.håndterSendtSøknad(sendtSøknad(fom = 10.juli, tom=30.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=10.juli, tom=30.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak trenger manuell behandling når vi mottar den andre inntektsmeldngen`() {
        val observer = TestObserver()
        Person(aktørId = "id").also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer ="12"), søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = "12"))
            it.addObserver(observer)
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = "12"))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
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
        internal var forrigeSakskomplekstilstand: Sakskompleks.TilstandType? = null
        internal var sakskomplekstilstand: Sakskompleks.TilstandType? = null

        override fun personEndret(person: Person) {
            personEndret = true
        }

        override fun sakskompleksChanged(event: SakskompleksObserver.StateChangeEvent) {
            wasTriggered = true
            forrigeSakskomplekstilstand = event.previousState
            sakskomplekstilstand = event.currentState
        }
    }

    private fun inntektsmeldingMottattTilstand(): Sakskompleks {
        TODO()
    }


}
