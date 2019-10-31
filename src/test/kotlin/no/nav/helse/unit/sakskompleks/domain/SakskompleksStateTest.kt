package no.nav.helse.unit.sakskompleks.domain

import no.nav.helse.TestConstants.inngangsvilkårHendelse
import no.nav.helse.TestConstants.inntektshistorikkHendelse
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.manuellSaksbehandlingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.sykepengehistorikkHendelse
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.inngangsvilkar.InngangsvilkårHendelse
import no.nav.helse.inntektshistorikk.InntektshistorikkHendelse
import no.nav.helse.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.juli
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.Sakskompleks.TilstandType.*
import no.nav.helse.person.domain.SakskompleksObserver
import no.nav.helse.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.søknad.NySøknadHendelse
import no.nav.helse.søknad.SendtSøknadHendelse
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SakskompleksStateTest : SakskompleksObserver {
    private lateinit var lastStateEvent: SakskompleksObserver.StateChangeEvent
    private val behovsliste: MutableList<Behov> = mutableListOf()

    override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
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
    fun `motta inngangsvilkår på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterInngangsvilkår(inngangsvilkårHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(START, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InngangsvilkårHendelse)
    }

    @Test
    fun `motta inntektshistorikk på feil tidspunkt`() {
        val sakskompleks = beInStartTilstand()

        sakskompleks.håndterInntektshistorikk(inntektshistorikkHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(START, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InntektshistorikkHendelse)
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
    fun `motta inngangsvilkår etter ny søknad`() {
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterInngangsvilkår(inngangsvilkårHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(NY_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InngangsvilkårHendelse)
    }

    @Test
    fun `motta inntektshistorikk etter ny søknad`() {
        val sakskompleks = beInNySøknad()

        sakskompleks.håndterInntektshistorikk(inntektshistorikkHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(NY_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InntektshistorikkHendelse)
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
    fun `motta inngangsvilkår etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterInngangsvilkår(inngangsvilkårHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InngangsvilkårHendelse)
    }

    @Test
    fun `motta inntektshistorikk etter sendt søknad`() {
        val sakskompleks = beInSendtSøknad()

        sakskompleks.håndterInntektshistorikk(inntektshistorikkHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(SENDT_SØKNAD_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InntektshistorikkHendelse)
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
        assertTrue(behovsliste.any {
            it.behovType() == BehovsTyper.Sykepengehistorikk.name
        })
        assertTrue(behovsliste.any {
            it.behovType() == BehovsTyper.Inngangsvilkår.name
        })
    }

    @Test
    fun `motta inngangsvilkår etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterInngangsvilkår(inngangsvilkårHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InngangsvilkårHendelse)
    }

    @Test
    fun `motta inntektshistorikk etter inntektsmelding`() {
        val sakskompleks = beInMottattInntektsmelding()

        sakskompleks.håndterInntektshistorikk(inntektshistorikkHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(INNTEKTSMELDING_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InntektshistorikkHendelse)
    }

    @Test
    fun `motta inngangsvilkår etter komplett tidslinje`() {
        val sakskompleks = beInKomplettTidslinje()

        sakskompleks.håndterInngangsvilkår(inngangsvilkårHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(KOMPLETT_SAK, lastStateEvent.previousState)
        assertEquals(INNGANGSVILKÅR_MOTTATT, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InngangsvilkårHendelse)
    }

    @Test
    fun `motta sykepengehistorikk når saken er komplett men historikken er utenfor seks måneder`() {
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
                sakskompleksId = sakskompleksId))

        assertEquals(KOMPLETT_SAK, lastStateEvent.previousState)
        assertEquals(SYKEPENGEHISTORIKK_MOTTATT, lastStateEvent.currentState)
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
                sakskompleksId = sakskompleksId))

        assertEquals(KOMPLETT_SAK, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
    }

    @Test
    fun `motta inngangsvilkår etter sykepengehistorikk`() {
        val sakskompleks = beInMottattSykepengehistorikk()

        sakskompleks.håndterInngangsvilkår(inngangsvilkårHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(SYKEPENGEHISTORIKK_MOTTATT, lastStateEvent.previousState)
        assertEquals(BEREGN_UTBETALING, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InngangsvilkårHendelse)
        assertTrue(behovsliste.any {
            it.behovType() == BehovsTyper.Inntektshistorikk.name
        })
    }

    @Test
    fun `motta inntektshistorikk etter sykepengehistorikk`() {
        val sakskompleks = beInMottattSykepengehistorikk()

        sakskompleks.håndterInntektshistorikk(inntektshistorikkHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(SYKEPENGEHISTORIKK_MOTTATT, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InntektshistorikkHendelse)
    }

    @Test
    fun `motta inntektshistorikk uten avvik siste tre måneder etter beregn utbetaling`() {
        val sakskompleks = beInBeregnUtbetaling()

        sakskompleks.håndterInntektshistorikk(inntektshistorikkHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString()
        ))

        assertEquals(BEREGN_UTBETALING, lastStateEvent.previousState)
        assertEquals(KLAR_TIL_UTBETALING, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InntektshistorikkHendelse)

        assertTrue(behovsliste.any {
            it.behovType() == BehovsTyper.GodkjenningFraSaksbehandler.name
        })
    }

    @Test
    fun `motta inntektshistorikk med avvik siste tre måneder etter beregn utbetaling`() {
        val sakskompleks = beInBeregnUtbetaling()

        sakskompleks.håndterInntektshistorikk(inntektshistorikkHendelse(
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                sakskompleksId = sakskompleksId.toString(),
                avvikSisteTreMåneder = true
        ))

        assertEquals(BEREGN_UTBETALING, lastStateEvent.previousState)
        assertEquals(SKAL_TIL_INFOTRYGD, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is InntektshistorikkHendelse)
    }

    @Test
    fun `motta manuell saksbehandling med utbetaling godkjent etter klar til utbetaling`() {
        val sakskompleks = beInKlarTilUtbetaling()

        sakskompleks.håndterManuellSaksbehandling(manuellSaksbehandlingHendelse(
                sakskompleksId = sakskompleksId.toString(),
                utbetalingGodkjent = true))

        assertEquals(KLAR_TIL_UTBETALING, lastStateEvent.previousState)
        assertEquals(UTBETALING_GODKJENT, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is ManuellSaksbehandlingHendelse)
    }

    @Test
    fun `motta manuell saksbehandling med utbetaling ikke godkjent etter klar til utbetaling`() {
        val sakskompleks = beInKlarTilUtbetaling()

        sakskompleks.håndterManuellSaksbehandling(manuellSaksbehandlingHendelse(
                sakskompleksId = sakskompleksId.toString(),
                utbetalingGodkjent = false))

        assertEquals(KLAR_TIL_UTBETALING, lastStateEvent.previousState)
        assertEquals(UTBETALING_IKKE_GODKJENT, lastStateEvent.currentState)
        assertTrue(lastStateEvent.sykdomshendelse is ManuellSaksbehandlingHendelse)
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

    private fun beInMottattSykepengehistorikk(sykepengehistorikkHendelse: SykepengehistorikkHendelse = sykepengehistorikkHendelse(sakskompleksId = sakskompleksId, sisteHistoriskeSykedag = LocalDate.now().minusMonths(12)),
                                              sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
                                              inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
                                              nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInKomplettTidslinje(sendtSøknadHendelse, inntektsmeldingHendelse, nySøknadHendelse).apply {
                håndterSykepengehistorikk(sykepengehistorikkHendelse)
            }

    private fun beInBeregnUtbetaling(inngangsvilkårHendelse: InngangsvilkårHendelse = inngangsvilkårHendelse(aktørId = aktørId, organisasjonsnummer = organisasjonsnummer, sakskompleksId = sakskompleksId.toString()),
                                     sykepengehistorikkHendelse: SykepengehistorikkHendelse = sykepengehistorikkHendelse(sakskompleksId = sakskompleksId, sisteHistoriskeSykedag = LocalDate.now().minusMonths(12)),
                                     sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
                                     inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
                                     nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInMottattSykepengehistorikk(sykepengehistorikkHendelse, sendtSøknadHendelse, inntektsmeldingHendelse, nySøknadHendelse).apply {
                håndterInngangsvilkår(inngangsvilkårHendelse)
            }

    private fun beInKlarTilUtbetaling(inntektshistorikkHendelse: InntektshistorikkHendelse = inntektshistorikkHendelse(aktørId = aktørId, organisasjonsnummer = organisasjonsnummer, sakskompleksId = sakskompleksId.toString()),
                                      inngangsvilkårHendelse: InngangsvilkårHendelse = inngangsvilkårHendelse(aktørId = aktørId, organisasjonsnummer = organisasjonsnummer, sakskompleksId = sakskompleksId.toString()),
                                      sykepengehistorikkHendelse: SykepengehistorikkHendelse = sykepengehistorikkHendelse(sakskompleksId = sakskompleksId, sisteHistoriskeSykedag = LocalDate.now().minusMonths(12)),
                                      sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
                                      inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
                                      nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInBeregnUtbetaling(inngangsvilkårHendelse, sykepengehistorikkHendelse, sendtSøknadHendelse, inntektsmeldingHendelse, nySøknadHendelse).apply {
                håndterInntektshistorikk(inntektshistorikkHendelse)
            }
}
