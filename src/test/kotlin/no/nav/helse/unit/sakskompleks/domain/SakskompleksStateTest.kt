package no.nav.helse.unit.sakskompleks.domain

import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.sykepengehistorikkHendelse
import no.nav.helse.behov.Behov
import no.nav.helse.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.juli
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.Sakskompleks.TilstandType.*
import no.nav.helse.person.domain.SakskompleksObserver
import no.nav.helse.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.søknad.NySøknadHendelse
import no.nav.helse.søknad.SendtSøknadHendelse
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SakskompleksStateTest : SakskompleksObserver {
    private lateinit var lastStateEvent: SakskompleksObserver.StateChangeEvent
    private lateinit var lastNeedEvent: Behov

    override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
        lastStateEvent = event
    }

    override fun sakskompleksTrengerLøsning(event: Behov) {
        lastNeedEvent = event
    }

    private val aktørId = "1234567891011"
    private val organisasjonsnummer = "123456789"
    private val sakskompleksId = UUID.randomUUID()

    @Test
    fun `motta ny søknad`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterNySøknad(nySøknadHendelse())

        assertEquals(START, lastStateEvent.previousState)
        assertEquals(NY_SØKNAD_MOTTATT, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is NySøknadHendelse)
    }

    @Test
    fun `motta sendt søknad på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterSendtSøknad(sendtSøknadHendelse())

        assertEquals(START, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is SendtSøknadHendelse)
    }

    @Test
    fun `motta inntektsmelding på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterInntektsmelding(inntektsmeldingHendelse())

        assertEquals(START, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InntektsmeldingHendelse)
    }

    @Test
    fun `motta sykdomshistorikk på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikkHendelse(sisteHistoriskeSykedag = LocalDate.now(), sakskompleksId = sakskompleksId))

        assertEquals(START, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is SykepengehistorikkHendelse)
    }

    @Test
    fun `motta sendt søknad etter ny søknad`() {
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterSendtSøknad(sendtSøknadHendelse())

        assertEquals(NY_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter ny søknad`() {
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterInntektsmelding(inntektsmeldingHendelse())

        assertEquals(NY_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter ny søknad`() {
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterNySøknad(nySøknadHendelse())

        assertEquals(NY_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterNySøknad(nySøknadHendelse())

        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterInntektsmelding(inntektsmeldingHendelse())

        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(KOMPLETT_SAK, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterNySøknad(nySøknadHendelse())

        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta sendt søknad etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterSendtSøknad(sendtSøknadHendelse())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(KOMPLETT_SAK, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterNySøknad(nySøknadHendelse())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterInntektsmelding(inntektsmeldingHendelse())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `når saken er komplett, ber vi om sykepengehistorikk`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterSendtSøknad(sendtSøknadHendelse())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(KOMPLETT_SAK, lastStateEvent.currentState)
        assertNotNull(lastNeedEvent)
    }

    @Test
    fun `motta sykepengehistorikk når saken er komplett men historikken er utenfor seks måneder`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(fom = periodeFom, tom = periodeTom, søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(fom = periodeFom, tom = periodeTom, søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())

        val sakskompleks = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse)

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(7),
                sakskompleksId = sakskompleksId))

        assertEquals(KOMPLETT_SAK, lastStateEvent.previousState)
        assertEquals(SYKEPENGEHISTORIKK_MOTTATT, lastStateEvent.currentState)
    }

    @Test
    fun `motta sykepengehistorikk med siste sykedag innenfor seks måneder av denne sakens første sykedag`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(fom = periodeFom, tom = periodeTom, søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(fom = periodeFom, tom = periodeTom, søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())

        val sakskompleks = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse)

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(5),
                sakskompleksId = sakskompleksId))

        assertEquals(KOMPLETT_SAK, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    private fun beInStartTilstand(): Sakskompleks {
        return Sakskompleks(
                aktørId = aktørId,
                id = sakskompleksId,
                organisasjonsnummer = organisasjonsnummer
        ).apply {
            addSakskompleksObserver(this@SakskompleksStateTest)
        }
    }

    private fun beInNySøknad(nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInStartTilstand().apply {
                håndterNySøknad(nySøknadHendelse)
            }

    private fun beInSendtSøknad(sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
                                nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInNySøknad(nySøknadHendelse).apply {
                håndterSendtSøknad(sendtSøknadHendelse)
            }

    private fun beInMottattInntektsmelding(inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
                                           nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInNySøknad(nySøknadHendelse).apply {
                håndterInntektsmelding(inntektsmeldingHendelse)
            }

    private fun beInKomplettTidslinje(sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
                                      inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
                                      nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInMottattInntektsmelding(inntektsmeldingHendelse, nySøknadHendelse).apply {
                håndterSendtSøknad(sendtSøknadHendelse)
            }
}
