package no.nav.helse.unit.sakskompleks.domain

import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.manuellSaksbehandlingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.sykepengehistorikkHendelse
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.juli
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.Sakskompleks.TilstandType.*
import no.nav.helse.person.domain.SakskompleksObserver
import no.nav.helse.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.serde.safelyUnwrapDate
import no.nav.helse.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.søknad.NySøknadHendelse
import no.nav.helse.søknad.SendtSøknadHendelse
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SakskompleksStateTest : SakskompleksObserver {
    private var haveObserverBeenCalled: Boolean = false
    private lateinit var lastStateEvent: SakskompleksObserver.StateChangeEvent
    private val behovsliste: MutableList<Behov> = mutableListOf()

    override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
        haveObserverBeenCalled = true
        lastStateEvent = event
    }

    override fun sakskompleksTrengerLøsning(event: Behov) {
        behovsliste.add(event)
    }

    private val aktørId = "1234567891011"
    private val organisasjonsnummer = "123456789"
    private val sakskompleksId = UUID.randomUUID()

    @BeforeEach
    fun `tilbakestill behovliste`() {
        behovsliste.clear()
    }

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
        assertEquals(TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is SendtSøknadHendelse)
    }

    @Test
    fun `motta inntektsmelding på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterInntektsmelding(inntektsmeldingHendelse())

        assertEquals(START, lastStateEvent.previousState)
        assertEquals(TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InntektsmeldingHendelse)
    }

    @Test
    fun `motta sykdomshistorikk på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikkHendelse(sisteHistoriskeSykedag = LocalDate.now(), sakskompleksId = sakskompleksId))

        assertFalse(haveObserverBeenCalled)
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
        assertEquals(TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterNySøknad(nySøknadHendelse())

        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterInntektsmelding(inntektsmeldingHendelse())

        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(KOMPLETT_SYKDOMSTIDSLINJE, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterNySøknad(nySøknadHendelse())

        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta sendt søknad etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterSendtSøknad(sendtSøknadHendelse())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(KOMPLETT_SYKDOMSTIDSLINJE, lastStateEvent.currentState)
    }

    @Test
    fun `motta ny søknad etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterNySøknad(nySøknadHendelse())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta inntektsmelding etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterInntektsmelding(inntektsmeldingHendelse())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `når saken er komplett, ber vi om sykepengehistorikk`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterSendtSøknad(sendtSøknadHendelse())

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(KOMPLETT_SYKDOMSTIDSLINJE, lastStateEvent.currentState)
        assertTrue(behovsliste.any {
            it.behovType() == BehovsTyper.Sykepengehistorikk.name
        })
    }

    @Test
    fun `motta sykepengehistorikk når saken er komplett og historikken er utenfor seks måneder`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val sakskompleks = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(7),
                sakskompleksId = sakskompleksId
        ))

        assertNotNull(sakskompleks.jsonRepresentation().utbetalingslinjer)
        assertEquals(30.74.toBigDecimal(), sakskompleks.jsonRepresentation().utbetalingslinjer?.get(0)?.get("dagsats")?.decimalValue())
        assertEquals(17.juli, sakskompleks.jsonRepresentation().utbetalingslinjer?.get(0)?.get("fom").safelyUnwrapDate())
        assertEquals(20.juli, sakskompleks.jsonRepresentation().utbetalingslinjer?.get(0)?.get("tom").safelyUnwrapDate())

        assertEquals(KOMPLETT_SYKDOMSTIDSLINJE, lastStateEvent.previousState)
        assertEquals(TIL_GODKJENNING, lastStateEvent.currentState)
    }

    @Test
    fun `motta sykepengehistorikk med siste sykedag innenfor seks måneder av denne sakens første sykedag`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val sakskompleks = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(5),
                sakskompleksId = sakskompleksId
        ))

        assertEquals(KOMPLETT_SYKDOMSTIDSLINJE, lastStateEvent.previousState)
        assertEquals(TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `gitt en komplett tidslinje, når vi mottar svar på saksbehandler-behov vi ikke trenger, skal ingenting skje`() {
        val sakskompleks = beInKomplettTidslinje()

        assertEquals(KOMPLETT_SYKDOMSTIDSLINJE, lastStateEvent.currentState)

        sakskompleks.håndterManuellSaksbehandling(manuellSaksbehandlingHendelse(
                sakskompleksId = sakskompleksId.toString(),
                utbetalingGodkjent = true
        ))

        assertEquals(KOMPLETT_SYKDOMSTIDSLINJE, lastStateEvent.currentState)
    }

    @Test
    fun `motta manuell saksbehandling med utbetaling godkjent etter klar til utbetaling`() {
        val sakskompleks = beInTilGodkjenning()

        sakskompleks.håndterManuellSaksbehandling(manuellSaksbehandlingHendelse(
                sakskompleksId = sakskompleksId.toString(),
                utbetalingGodkjent = true))

        assertEquals(TIL_GODKJENNING, lastStateEvent.previousState)
        assertEquals(TIL_UTBETALING, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is ManuellSaksbehandlingHendelse)
    }

    @Test
    fun `motta manuell saksbehandling med utbetaling ikke godkjent etter klar til utbetaling`() {
        val sakskompleks = beInTilGodkjenning()

        sakskompleks.håndterManuellSaksbehandling(manuellSaksbehandlingHendelse(
                sakskompleksId = sakskompleksId.toString(),
                utbetalingGodkjent = false))

        assertEquals(TIL_GODKJENNING, lastStateEvent.previousState)
        assertEquals(TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is ManuellSaksbehandlingHendelse)
    }

    @Test
    fun `motta sykepengehistorikk etter klar til utbetaling skal ikke endre state`() {
        val sakskompleks = beInTilGodkjenning()

        assertEquals(TIL_GODKJENNING, lastStateEvent.currentState)

        val previousState = lastStateEvent.previousState

        sakskompleks.håndterSykepengehistorikk(sykepengehistorikkHendelse(
                sakskompleksId = sakskompleksId
        ))

        assertEquals(TIL_GODKJENNING, lastStateEvent.currentState)
        assertEquals(previousState, lastStateEvent.previousState)
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

    private fun beInTilGodkjenning(sykepengehistorikkHendelse: SykepengehistorikkHendelse = sykepengehistorikkHendelse(sakskompleksId = sakskompleksId, sisteHistoriskeSykedag = LocalDate.now().minusMonths(12)),
                                   sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
                                   inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
                                   nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInKomplettTidslinje(sendtSøknadHendelse, inntektsmeldingHendelse, nySøknadHendelse).apply {
                håndterSykepengehistorikk(sykepengehistorikkHendelse)
            }
}
