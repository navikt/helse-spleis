package no.nav.helse.unit.person

import no.nav.helse.TestConstants.inntektsmelding
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.TestConstants.sendtSøknad
import no.nav.helse.TestConstants.sykepengehistorikk
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.juli
import no.nav.helse.person.domain.*
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

internal class PersonTest {

    private val aktørId = "id"
    private val organisasjonsnummer = "12"
    private val tilstandsflytObserver = TilstandsflytObserver()
    private val sakstilstandObserver = SakstilstandObserver()
    private val needObserver = NeedObserver()

    private val testPerson = Person(aktørId = aktørId).also {
        it.addObserver(tilstandsflytObserver)
        it.addObserver(sakstilstandObserver)
        it.addObserver(needObserver)
    }

    @Test
    internal fun `ny søknad fører til at sakskompleks trigger en sakskompleks endret hendelse`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad())
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `sendt søknad uten sak trigger sakskompleks endret-hendelse`() {
        testPerson.also {
            it.håndterSendtSøknad(sendtSøknad())
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.SKAL_TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding uten sak trigger sakskompleks endret-hendelse`() {
        testPerson.also {
            it.håndterInntektsmelding(inntektsmelding(
                    virksomhetsnummer = "123456789"
            ))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.SKAL_TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding med sak trigger sakskompleks endret-hendelse`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = organisasjonsnummer
                    )))

            it.håndterInntektsmelding(inntektsmelding(
                    virksomhetsnummer = organisasjonsnummer
            ))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `ny sak blir opprettet når en ny søknad som ikke overlapper saken personen har fra før blir sendt inn`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom = 20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterNySøknad(nySøknad(fom = 21.juli, tom = 28.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom = 21.juli, tom = 28.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak må behandles i infotrygd når en ny søknad overlapper sykdomstidslinjen i den eksisterende saken`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom = 20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterNySøknad(nySøknad(fom = 10.juli, tom = 22.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom = 10.juli, tom = 22.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.SKAL_TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak må behandles i infotrygd når vi mottar den andre sendte søknaden`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom = 20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 1.juli, tom = 20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 10.juli, tom = 30.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom = 10.juli, tom = 30.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.SKAL_TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `oppretter ny sak når ny søknad kommer, som ikke overlapper med eksisterende`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom = 20.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterNySøknad(nySøknad(fom = 21.juli, tom = 30.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom = 21.juli, tom = 30.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `ny sak må behandles i infotrygd når vi mottar den sendte søknaden først`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom = 9.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 10.juli, tom = 30.juli, søknadsperioder = listOf(SoknadsperiodeDTO(fom = 10.juli, tom = 30.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.SKAL_TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak må behandles i infotrygd når vi mottar den andre inntektsmeldngen`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom = 9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = "12"), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = "12"))
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = "12"))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(Sakskompleks.TilstandType.SKAL_TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
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
                            orgnummer = organisasjonsnummer
                    )))

            it.håndterSendtSøknad(sendtSøknad(
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = organisasjonsnummer
                    )
            ))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `sykepengehistorikk lager ikke ny sak, selv om det ikke finnes noen fra før`() {
        testPerson.also {
            it.håndterSykepengehistorikk(sykepengehistorikk(LocalDate.now()))
        }

        assertFalse(tilstandsflytObserver.wasTriggered)
        assertFalse(tilstandsflytObserver.personEndret)
    }

    @Test
    fun `komplett genererer sykepengehistorikk-needs`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom = 9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 1.juli, tom = 9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))

            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = organisasjonsnummer))
        }

        assertTrue(tilstandsflytObserver.wasTriggered, "skulle ha trigget observer")
        assertTrue(tilstandsflytObserver.personEndret, "skulle endret person")
        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SAK, tilstandsflytObserver.sakskomplekstilstand)
        assertNotNull(needObserver.needEvent.find { it.behovType() == BehovsTyper.Sykepengehistorikk.name })
    }

    @Test
    fun `sykepengehistorikk eldre enn seks måneder fører saken videre`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom = 9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 1.juli, tom = 9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = organisasjonsnummer))

            assertEquals(1, sakstilstandObserver.sakstilstander.size)
            val saksid = sakstilstandObserver.sakstilstander.keys.first()

            it.håndterSykepengehistorikk(sykepengehistorikk(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(7),
                    organisasjonsnummer = organisasjonsnummer,
                    aktørId = aktørId,
                    sakskompleksId = saksid
            ))
        }

        assertEquals(Sakskompleks.TilstandType.SYKEPENGEHISTORIKK_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    fun `sykepengehistorikk med feil sakskompleksid skal ikke føre noen saker videre`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom = 9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 1.juli, tom = 9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = organisasjonsnummer))

            it.håndterSykepengehistorikk(sykepengehistorikk(1.juli.minusMonths(7), organisasjonsnummer, aktørId, UUID.randomUUID()))
        }

        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SAK, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    fun `sykepengehistorikk yngre enn seks måneder fører til at saken må behandles i infotrygd`() {
        testPerson.also {
            it.håndterNySøknad(nySøknad(fom = 1.juli, tom = 9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknad(fom = 1.juli, tom = 9.juli, arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmelding(virksomhetsnummer = organisasjonsnummer))

            assertEquals(1, sakstilstandObserver.sakstilstander.size)
            val saksid = sakstilstandObserver.sakstilstander.keys.first()

            it.håndterSykepengehistorikk(sykepengehistorikk(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(5),
                    organisasjonsnummer = organisasjonsnummer,
                    aktørId = aktørId,
                    sakskompleksId = saksid
            ))
        }
        assertTrue(tilstandsflytObserver.wasTriggered, "skulle ha trigget observer")
        assertTrue(tilstandsflytObserver.personEndret, "skulle endret person")
        assertEquals(Sakskompleks.TilstandType.SKAL_TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    private class TilstandsflytObserver : PersonObserver {

        internal var wasTriggered = false
        internal var personEndret = false
        internal var forrigeSakskomplekstilstand: Sakskompleks.TilstandType? = null
        internal var sakskomplekstilstand: Sakskompleks.TilstandType? = null

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
            personEndret = true
        }

        override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
            wasTriggered = true
            forrigeSakskomplekstilstand = event.previousState
            sakskomplekstilstand = event.currentState
        }

    }

    private class NeedObserver : PersonObserver {
        internal val needEvent: MutableList<Behov> = mutableListOf()

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
        }

        override fun sakskompleksTrengerLøsning(event: Behov) {
            needEvent.add(event)
        }
    }

    private class SakstilstandObserver : PersonObserver {
        internal val sakstilstander: MutableMap<UUID, SakskompleksObserver.StateChangeEvent> = mutableMapOf()

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {

        }

        override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
            sakstilstander[event.id] = event
        }
    }

}
