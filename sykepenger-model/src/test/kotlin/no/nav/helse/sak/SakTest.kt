package no.nav.helse.sak

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.sykepengehistorikkHendelse
import no.nav.helse.Uke
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.sak.Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT
import no.nav.helse.sak.Sakskompleks.TilstandType.TIL_INFOTRYGD
import no.nav.helse.hendelser.inntektsmelding.Inntektsmelding
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*
import kotlin.collections.set

internal class SakTest {

    private val aktørId = "id"
    private val organisasjonsnummer = "12"
    private val tilstandsflytObserver = TilstandsflytObserver()
    private val sakstilstandObserver = SakstilstandObserver()
    private val needObserver = NeedObserver()

    private val testSak = Sak(aktørId = aktørId).also {
        it.addObserver(tilstandsflytObserver)
        it.addObserver(sakstilstandObserver)
        it.addObserver(needObserver)
    }

    @Test
    internal fun `ny søknad fører til at sakskompleks trigger en sakskompleks endret hendelse`() {
        testSak.also {
            it.håndter(nySøknadHendelse())
        }
        assertTrue(tilstandsflytObserver.sakEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(NY_SØKNAD_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `sendt søknad uten sak trigger sakskompleks endret-hendelse`() {
        testSak.also {
            it.håndter(sendtSøknadHendelse())
        }
        assertTrue(tilstandsflytObserver.sakEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding uten sak trigger sakskompleks endret-hendelse`() {
        testSak.also {
            it.håndter(inntektsmeldingHendelse(
                    virksomhetsnummer = "123456789"
            ))
        }
        assertTrue(tilstandsflytObserver.sakEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding med sak trigger sakskompleks endret-hendelse`() {
        testSak.also {
            it.håndter(nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = organisasjonsnummer
                    )))

            it.håndter(inntektsmeldingHendelse(
                    virksomhetsnummer = organisasjonsnummer
            ))
        }
        assertTrue(tilstandsflytObserver.sakEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `ny sak blir opprettet når en ny søknad som ikke overlapper saken personen har fra før blir sendt inn`() {
        testSak.also {
            it.håndter(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 21.juli, tom = 28.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.sakEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(NY_SØKNAD_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak må behandles i infotrygd når en ny søknad overlapper sykdomstidslinjen i den eksisterende saken`() {
        testSak.also {
            it.håndter(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 10.juli, tom = 22.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.sakEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(NY_SØKNAD_MOTTATT, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak må behandles i infotrygd når vi mottar den andre sendte søknaden`() {
        testSak.also {
            it.håndter(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 10.juli, tom = 30.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.sakEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `oppretter ny sak når ny søknad kommer, som ikke overlapper med eksisterende`() {
        testSak.also {
            it.håndter(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 21.juli, tom = 30.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.sakEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(NY_SØKNAD_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `ny sak må behandles i infotrygd når vi mottar den sendte søknaden først`() {
        testSak.also {
            it.håndter(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 10.juli, tom = 30.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
        }
        assertTrue(tilstandsflytObserver.sakEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.START, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }


    @Test
    internal fun `eksisterende sak må behandles i infotrygd når vi mottar den andre inntektsmeldngen`() {
        testSak.also {
            it.håndter(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = "12"), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(inntektsmeldingHendelse(virksomhetsnummer = "12", førsteFraværsdag = 1.juli, arbeidsgiverperioder = listOf(Periode(1.juli, 1.juli.plusDays(16)))))
            it.håndter(inntektsmeldingHendelse(virksomhetsnummer = "12", førsteFraværsdag = 1.juli, arbeidsgiverperioder = listOf(Periode(1.juli, 1.juli.plusDays(16)))))
        }
        assertTrue(tilstandsflytObserver.sakEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.INNTEKTSMELDING_MOTTATT, tilstandsflytObserver.forrigeSakskomplekstilstand)
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `inntektsmelding uten virksomhetsnummer kaster exception`() {
        testSak.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(inntektsmeldingHendelse(virksomhetsnummer = null))
            }
        }
    }

    @Test
    internal fun `ny søknad med periode som ikke er 100 % kaster exception`() {
        testSak.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(nySøknadHendelse(
                        søknadsperioder = listOf(
                                SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 60),
                                SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100)
                        )
                ))
            }
        }
    }

    @Test
    internal fun `sendt søknad kan ikke være sendt mer enn 3 måneder etter perioden`() {
        testSak.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(sendtSøknadHendelse(
                        søknadsperioder = listOf(
                                SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 100)
                        ),
                        sendtNav = Uke(1).mandag.plusMonths(4).atStartOfDay()
                ))
            }
        }
    }

    @Test
    internal fun `sendt søknad med periode som ikke er 100 % kaster exception`() {
        testSak.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(sendtSøknadHendelse(
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
        testSak.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(nySøknadHendelse(
                        arbeidsgiver = null
                ))
            }
        }
    }

    @Test
    internal fun `ny søknad uten organisasjonsnummer kaster exception`() {
        testSak.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(nySøknadHendelse(
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
        testSak.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(sendtSøknadHendelse(
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
        testSak.also {
            it.håndter(nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = organisasjonsnummer
                    )))

            it.håndter(sendtSøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(
                            orgnummer = organisasjonsnummer
                    )
            ))
        }
        assertTrue(tilstandsflytObserver.sakEndret)
        assertTrue(tilstandsflytObserver.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    internal fun `sykepengehistorikk lager ikke ny sak, selv om det ikke finnes noen fra før`() {
        testSak.also {
            it.håndter(sykepengehistorikkHendelse(LocalDate.now()))
        }

        assertFalse(tilstandsflytObserver.wasTriggered)
        assertFalse(tilstandsflytObserver.sakEndret)
    }

    @Test
    fun `komplett genererer sykepengehistorikk-needs`() {
        testSak.also {
            it.håndter(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(sendtSøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))

            it.håndter(inntektsmeldingHendelse(virksomhetsnummer = organisasjonsnummer, arbeidsgiverperioder = listOf(Periode(1. juli, 9. juli))))
        }

        assertTrue(tilstandsflytObserver.wasTriggered, "skulle ha trigget observer")
        assertTrue(tilstandsflytObserver.sakEndret, "skulle endret sak")
        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SYKDOMSTIDSLINJE, tilstandsflytObserver.sakskomplekstilstand)
        assertNotNull(needObserver.needEvent.find { it.behovType() == BehovsTyper.Sykepengehistorikk.name })
    }

    @Test
    fun `sykepengehistorikk eldre enn seks måneder fører saken videre`() {
        testSak.also {
            it.håndter(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(sendtSøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(inntektsmeldingHendelse(virksomhetsnummer = organisasjonsnummer, arbeidsgiverperioder = listOf(Periode(30.juni, 5.juli))))

            assertEquals(1, sakstilstandObserver.sakstilstander.size)
            val saksid = sakstilstandObserver.sakstilstander.keys.first()

            it.håndter(sykepengehistorikkHendelse(
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
        testSak.also {
            it.håndter(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(sendtSøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(inntektsmeldingHendelse(virksomhetsnummer = organisasjonsnummer, arbeidsgiverperioder = listOf(Periode(1.juli, 9.juli))))

            it.håndter(sykepengehistorikkHendelse(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(7),
                    organisasjonsnummer = organisasjonsnummer,
                    aktørId = aktørId,
                    sakskompleksId = UUID.randomUUID()
            ))
        }

        assertEquals(Sakskompleks.TilstandType.KOMPLETT_SYKDOMSTIDSLINJE, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    fun `sykepengehistorikk yngre enn seks måneder fører til at saken må behandles i infotrygd`() {
        testSak.also {
            it.håndter(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(sendtSøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))
            it.håndter(inntektsmeldingHendelse(virksomhetsnummer = organisasjonsnummer, arbeidsgiverperioder = listOf(Periode(1.juli, 9.juli))))

            assertEquals(1, sakstilstandObserver.sakstilstander.size)
            val saksid = sakstilstandObserver.sakstilstander.keys.first()

            it.håndter(sykepengehistorikkHendelse(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(5),
                    organisasjonsnummer = organisasjonsnummer,
                    aktørId = aktørId,
                    sakskompleksId = saksid
            ))
        }
        assertTrue(tilstandsflytObserver.wasTriggered, "skulle ha trigget observer")
        assertTrue(tilstandsflytObserver.sakEndret, "skulle endret sak")
        assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
    }

    @Test
    fun `motta en inntektsmelding som ikke kan behandles etter ny søknad`() {
        testSak.also {
            it.håndter(nySøknadHendelse(arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer), søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)), egenmeldinger = emptyList(), fravær = emptyList()))

            val inntektsmeldingJson = inntektsmeldingDTO().toJsonNode().also {
                (it as ObjectNode).remove("virksomhetsnummer")
            }
            val inntektsmeldingHendelse = InntektsmeldingHendelse(Inntektsmelding(inntektsmeldingJson))

            assertThrows<UtenforOmfangException> {
                it.håndter(inntektsmeldingHendelse)
            }

            assertTrue(tilstandsflytObserver.wasTriggered, "skulle ha trigget observer")
            assertTrue(tilstandsflytObserver.sakEndret, "skulle endret sak")

            assertEquals(NY_SØKNAD_MOTTATT, tilstandsflytObserver.forrigeSakskomplekstilstand)
            assertEquals(TIL_INFOTRYGD, tilstandsflytObserver.sakskomplekstilstand)
        }
    }

    private class TilstandsflytObserver : SakObserver {

        internal var wasTriggered = false
        internal var sakEndret = false
        internal var forrigeSakskomplekstilstand: Sakskompleks.TilstandType? = null
        internal var sakskomplekstilstand: Sakskompleks.TilstandType? = null

        override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {
            sakEndret = true
        }

        override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
            wasTriggered = true
            forrigeSakskomplekstilstand = event.previousState
            sakskomplekstilstand = event.currentState
        }

    }

    private class NeedObserver : SakObserver {
        internal val needEvent: MutableList<Behov> = mutableListOf()

        override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {
        }

        override fun sakskompleksTrengerLøsning(event: Behov) {
            needEvent.add(event)
        }
    }

    private class SakstilstandObserver : SakObserver {
        internal val sakstilstander: MutableMap<UUID, SakskompleksObserver.StateChangeEvent> = mutableMapOf()

        override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {

        }

        override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
            sakstilstander[event.id] = event
        }
    }

}
