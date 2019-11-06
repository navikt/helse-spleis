package no.nav.helse.unit.person

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.sykepengehistorikkHendelse
import no.nav.helse.Uke
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.inntektsmelding.Inntektsmelding
import no.nav.helse.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.person.domain.*
import no.nav.helse.person.domain.Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT
import no.nav.helse.person.domain.Sakskompleks.TilstandType.TIL_INFOTRYGD
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.emptyList
import kotlin.collections.find
import kotlin.collections.first
import kotlin.collections.listOf
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set

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
            it.håndterNySøknad(nySøknadHendelse())
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(NY_SØKNAD_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `sendt søknad uten sak trigger sakskompleks endret-hendelse`() {
        testPerson.also {
            it.håndterSendtSøknad(sendtSøknadHendelse())
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding uten sak trigger sakskompleks endret-hendelse`() {
        testPerson.also {
            it.håndterInntektsmelding(inntektsmeldingHendelse(
                    virksomhetsnummer = "123456789"
            ))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding med sak trigger sakskompleks endret-hendelse`() {
        testPerson.also {
            it.håndterNySøknad(nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = organisasjonsnummer
                    )))

            it.håndterInntektsmelding(inntektsmeldingHendelse(
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
            it.håndterNySøknad(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterNySøknad(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 21.juli, tom = 28.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(NY_SØKNAD_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak må behandles i infotrygd når en ny søknad overlapper sykdomstidslinjen i den eksisterende saken`() {
        testPerson.also {
            it.håndterNySøknad(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterNySøknad(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 10.juli, tom = 22.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(NY_SØKNAD_MOTTATT, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak må behandles i infotrygd når vi mottar den andre sendte søknaden`() {
        testPerson.also {
            it.håndterNySøknad(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 10.juli, tom = 30.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `oppretter ny sak når ny søknad kommer, som ikke overlapper med eksisterende`() {
        testPerson.also {
            it.håndterNySøknad(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterNySøknad(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 21.juli, tom = 30.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(NY_SØKNAD_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `ny sak må behandles i infotrygd når vi mottar den sendte søknaden først`() {
        testPerson.also {
            it.håndterNySøknad(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 10.juli, tom = 30.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak må behandles i infotrygd når vi mottar den andre inntektsmeldngen`() {
        testPerson.also {
            it.håndterNySøknad(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = "12"), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmeldingHendelse(virksomhetsnummer = "12", førsteFraværsdag = 1.juli, arbeidsgiverperioder = listOf(Periode(1.juli, 1.juli.plusDays(16)))))
            it.håndterInntektsmelding(inntektsmeldingHendelse(virksomhetsnummer = "12", førsteFraværsdag = 1.juli, arbeidsgiverperioder = listOf(Periode(1.juli, 1.juli.plusDays(16)))))
        }
        assertTrue(tilstandsflytObserver.personEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding uten virksomhetsnummer kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndterInntektsmelding(inntektsmeldingHendelse(virksomhetsnummer = null))
            }
        }
    }

    @Test
    internal fun `ny søknad med periode som ikke er 100 % kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndterNySøknad(nySøknadHendelse(
                        søknadsperioder = listOf(
                                SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 60),
                                SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100)
                        )
                ))
            }
        }
    }

    @Test
    internal fun `sendt søknad med periode som ikke er 100 % kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndterSendtSøknad(sendtSøknadHendelse(
                        søknadsperioder = listOf(
                                SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 100),
                                SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100, faktiskGrad = 90)
                        )
                ))
            }
        }
    }

    @Test
    internal fun `søknad uten arbeidsgiver kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndterNySøknad(nySøknadHendelse(
                        arbeidsgiver = null
                ))
            }
        }
    }

    @Test
    internal fun `ny søknad uten organisasjonsnummer kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndterNySøknad(nySøknadHendelse(
                        arbeidsgiver = ArbeidsgiverDTO(
                                navn = "En arbeidsgiver",
                                orgnummer = null
                        )
                ))
            }
        }
    }

    @Test
    internal fun `sendt søknad uten organisasjonsnummer kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndterSendtSøknad(sendtSøknadHendelse(
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
            it.håndterNySøknad(nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = organisasjonsnummer
                    )))

            it.håndterSendtSøknad(sendtSøknadHendelse(
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
            it.håndterSykepengehistorikk(sykepengehistorikkHendelse(LocalDate.now()))
        }

        assertFalse(tilstandsflytObserver.wasTriggered)
        assertFalse(tilstandsflytObserver.personEndret)
    }

    @Test
    fun `komplett genererer sykepengehistorikk-needs`() {
        testPerson.also {
            it.håndterNySøknad(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))

            it.håndterInntektsmelding(inntektsmeldingHendelse(virksomhetsnummer = organisasjonsnummer, arbeidsgiverperioder = listOf(Periode(1. juli, 9. juli))))
        }

        assertTrue(tilstandsflytObserver.wasTriggered, "skulle ha trigget observer")
        assertTrue(tilstandsflytObserver.personEndret, "skulle endret person")
        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SYKDOMSTIDSLINJE, tilstandsflytObserver.sakskomplekstilstand)
        assertNotNull(needObserver.needEvent.find { it.behovType() == BehovsTyper.Sykepengehistorikk.name })
    }

    @Test
    fun `sykepengehistorikk eldre enn seks måneder fører saken videre`() {
        testPerson.also {
            it.håndterNySøknad(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmeldingHendelse(virksomhetsnummer = organisasjonsnummer, arbeidsgiverperioder = listOf(Periode(30.juni, 5.juli))))

            assertEquals(1, sakstilstandObserver.sakstilstander.size)
            val saksid = sakstilstandObserver.sakstilstander.keys.first()

            it.håndterSykepengehistorikk(sykepengehistorikkHendelse(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(7),
                    organisasjonsnummer = organisasjonsnummer,
                    aktørId = aktørId,
                    sakskompleksId = saksid
            ))
        }

        assertEquals(Sakskompleks.TilstandType.TIL_GODKJENNING, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    fun `sykepengehistorikk med feil sakskompleksid skal ikke føre noen saker videre`() {
        testPerson.also {
            it.håndterNySøknad(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmeldingHendelse(virksomhetsnummer = organisasjonsnummer, arbeidsgiverperioder = listOf(Periode(1.juli, 9.juli))))

            it.håndterSykepengehistorikk(sykepengehistorikkHendelse(1.juli.minusMonths(7), organisasjonsnummer, aktørId, UUID.randomUUID()))
        }

        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SYKDOMSTIDSLINJE, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    fun `sykepengehistorikk yngre enn seks måneder fører til at saken må behandles i infotrygd`() {
        testPerson.also {
            it.håndterNySøknad(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterSendtSøknad(sendtSøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndterInntektsmelding(inntektsmeldingHendelse(virksomhetsnummer = organisasjonsnummer, arbeidsgiverperioder = listOf(Periode(1.juli, 9.juli))))

            assertEquals(1, sakstilstandObserver.sakstilstander.size)
            val saksid = sakstilstandObserver.sakstilstander.keys.first()

            it.håndterSykepengehistorikk(sykepengehistorikkHendelse(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(5),
                    organisasjonsnummer = organisasjonsnummer,
                    aktørId = aktørId,
                    sakskompleksId = saksid
            ))
        }
        assertTrue(tilstandsflytObserver.wasTriggered, "skulle ha trigget observer")
        assertTrue(tilstandsflytObserver.personEndret, "skulle endret person")
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    fun `motta en inntektsmelding som ikke kan behandles etter ny søknad`() {
        testPerson.also {
            it.håndterNySøknad(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))

            val inntektsmeldingJson = inntektsmeldingDTO().toJsonNode().also {
                (it as ObjectNode).remove("virksomhetsnummer")
            }
            val inntektsmeldingHendelse = InntektsmeldingHendelse(Inntektsmelding(inntektsmeldingJson))

            assertThrows<UtenforOmfangException> {
                it.håndterInntektsmelding(inntektsmeldingHendelse)
            }

            assertTrue(tilstandsflytObserver.wasTriggered, "skulle ha trigget observer")
            assertTrue(tilstandsflytObserver.personEndret, "skulle endret person")

            assertEquals(NY_SØKNAD_MOTTATT, tilstandsflytObserver.forrigeSakskomplekstilstand)
            assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
        }
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
