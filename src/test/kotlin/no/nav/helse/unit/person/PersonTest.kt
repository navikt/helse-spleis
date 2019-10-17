package no.nav.helse.unit.person

import no.nav.helse.TestConstants.inntektsmelding
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.TestConstants.sendtSøknad
import no.nav.helse.TestConstants.sykepengehistorikk
import no.nav.helse.juli
import no.nav.helse.person.domain.*
import no.nav.helse.person.domain.SakskompleksObserver.NeedType.TRENGER_INNTEKTSOPPLYSNINGER
import no.nav.helse.person.domain.SakskompleksObserver.NeedType.TRENGER_PERSONOPPLYSNINGER
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

internal class PersonTest {

    private val aktørId = "id"
    private val orgnr = "12"
    private val observer = TestObserver()
    private val sakObserver = SakTestObserver()
    private val needObserver = TestNeedObserver()
    private val testPerson = Person(aktørId = aktørId).also {
        it.addObserver(observer)
        it.addObserver(sakObserver)
        it.addObserver(needObserver)
    }

    @Test
    internal fun `ny søknad fører til at sakskompleks trigger en sakskompleks endret hendelse`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad())
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, observer.sakskomplekstilstand)
    }

    @Test
    internal fun `sendt søknad uten sak trigger sakskompleks endret-hendelse`() {
        testPerson.also {
            it.håndterSendtSøknad(sendtSøknad())
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding uten sak trigger sakskompleks endret-hendelse`() {
        testPerson.also {
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
        testPerson.also {
            it.håndterNySøknad(nySøknad(
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = orgnr
                    )))

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
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterNySøknad(nySøknad(fom = 21.juli, tom=28.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=21.juli, tom=28.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, observer.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak trenger manuell behandling når en ny søknad overlapper sykdomstidslinjen i den eksisterende saken`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterNySøknad(nySøknad(fom = 10.juli, tom=22.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=10.juli, tom=22.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak trenger manuell behandling når vi mottar den andre sendte søknaden`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 1.juli, tom=20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 10.juli, tom=30.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=10.juli, tom=30.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }

    @Test
    internal fun `oppretter ny sak når ny søknad kommer, som ikke overlapper med eksisterende`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterNySøknad(nySøknad(fom = 21.juli, tom=30.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=21.juli, tom=30.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, observer.sakskomplekstilstand)
    }

    @Test
    internal fun `ny sak trenger manuell behandling når vi mottar den sendte søknaden først`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=9.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 10.juli, tom=30.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom=10.juli, tom=30.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak trenger manuell behandling når vi mottar den andre inntektsmeldngen`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer ="12"), søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = "12"))
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = "12"))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, observer.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding uten virksomhetsnummer kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = null))
            }
        }
    }

    @Test
    internal fun `søknad uten arbeidsgiver kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndterNySøknad(nySøknad(
                        arbeidsgiver = null
                ))
            }
        }
    }

    @Test
    internal fun `søknad uten organisasjonsnummer kaster exception`() {
        testPerson.also {
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
        testPerson.also {
            it.håndterNySøknad(nySøknad(
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = orgnr
                    )))

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

    @Test
    internal fun `sykepengehistorikk lager ikke ny sak, selv om det ikke finnes noen fra før`() {
        testPerson.also {
            it.håndterSykepengehistorikk(sykepengehistorikk(LocalDate.now()))
        }

        assertFalse(observer.wasTriggered)
        assertFalse(observer.personEndret)
    }

    @Test
    fun `komplett genererer sykepengehistorikk-needs`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom = 9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnr), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 1.juli, tom = 9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnr), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))

            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = orgnr))
        }

        assertTrue(observer.wasTriggered, "skulle ha trigget observer")
        assertTrue(observer.personEndret, "skulle endret person")
        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SAK, observer.sakskomplekstilstand)
        assertNotNull(needObserver.needEvent.find{ it.type == SakskompleksObserver.NeedType.TRENGER_SYKEPENGEHISTORIKK})
    }

    @Test
    fun `sykepengehistorikk eldre enn seks måneder fører saken videre`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnr), søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 1.juli, tom=9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnr), søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = orgnr))

            assertEquals(1, sakObserver.sakstilstander.size)
            val saksid = sakObserver.sakstilstander.keys.first()

            it.håndterSykepengehistorikk(sykepengehistorikk(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(7),
                    organisasjonsnummer = orgnr,
                    aktørId = aktørId,
                    sakskompleksId = saksid
            ))
        }

        assertTrue(needObserver.needEvent.map { it.type }.containsAll(listOf(TRENGER_PERSONOPPLYSNINGER, TRENGER_INNTEKTSOPPLYSNINGER)))
    }

    @Test
    fun `sykepengehistorikk med feil sakskompleksid skal ikke føre noen saker videre`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnr), søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 1.juli, tom=9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnr), søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = orgnr))

            it.håndterSykepengehistorikk(sykepengehistorikk(1.juli.minusMonths(7), orgnr, aktørId, UUID.randomUUID()))
        }

        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SAK, observer.sakskomplekstilstand)
    }

    @Test
    fun `sykepengehistorikk yngre enn seks måneder fører til manuell saksbehandling`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom=9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnr), søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 1.juli, tom=9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnr), søknadsperioder = listOf(SoknadsperiodeDTO(fom=1.juli, tom=9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = orgnr))

            assertEquals(1, sakObserver.sakstilstander.size)
            val saksid = sakObserver.sakstilstander.keys.first()

            it.håndterSykepengehistorikk(sykepengehistorikk(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(5),
                    organisasjonsnummer = orgnr,
                    aktørId = aktørId,
                    sakskompleksId = saksid
            ))
        }
        assertTrue(observer.wasTriggered, "skulle ha trigget observer")
        assertTrue(observer.personEndret, "skulle endret person")
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
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

    private class TestNeedObserver : PersonObserver {

        internal val needEvent: MutableList<SakskompleksObserver.NeedEvent> = mutableListOf()

        override fun sakskompleksHasNeed(event: SakskompleksObserver.NeedEvent) {
            needEvent.add(event)
        }
    }

    private class SakTestObserver: PersonObserver {
        internal val sakstilstander: MutableMap<UUID, SakskompleksObserver.StateChangeEvent> = mutableMapOf()

        override fun sakskompleksChanged(event: SakskompleksObserver.StateChangeEvent) {
            sakstilstander[event.id] = event
        }
    }

}
