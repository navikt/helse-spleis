package no.nav.helse.unit.spleis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.mockk.*
import no.nav.helse.SpolePeriode
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.responsFraSpole
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProducer
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.person.Sakskompleks
import no.nav.helse.person.Sakskompleks.TilstandType.*
import no.nav.helse.person.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.person.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.person.hendelser.sykepengehistorikk.Sykepengehistorikk
import no.nav.helse.person.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.person.hendelser.søknad.NySøknadHendelse
import no.nav.helse.person.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.spleis.*
import no.nav.helse.spleis.oppgave.GosysOppgaveProducer
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonMediatorTest {

    private val probe = mockk<SakskompleksProbe>(relaxed = true)
    private val oppgaveProducer = mockk<GosysOppgaveProducer>(relaxed = true)
    private val sakskompleksEventProducer = mockk<SakskompleksEventProducer>(relaxed = true)

    private val behovsliste = mutableListOf<Behov>()
    private val behovProducer = mockk<BehovProducer>(relaxed = true).also {
        every {
            it.sendNyttBehov(match {
                behovsliste.add(Behov.fromJson(it.toJson()))
                true
            })
        } just runs
    }
    private val repo = HashmapPersonRepository()
    private val utbetalingsRepo = mockk<UtbetalingsreferanseRepository>(relaxed = true)
    private val lagreUtbetalingDao = mockk<LagreUtbetalingDao>(relaxed = true)

    private val personMediator = PersonMediator(
            sakskompleksProbe = probe,
            personRepository = repo,
            lagrePersonDao = repo,
            utbetalingsreferanseRepository = utbetalingsRepo,
            lagreUtbetalingDao = lagreUtbetalingDao,
            behovProducer = behovProducer,
            gosysOppgaveProducer = oppgaveProducer,
            sakskompleksEventProducer = sakskompleksEventProducer
    )

    private val sendtSøknadHendelse = sendtSøknadHendelse()

    private companion object {
            private val objectMapper = ObjectMapper()
                    .registerModule(JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @BeforeEach
    fun `setup behov`() {
        behovsliste.clear()
    }

    @Test
    fun `skal håndtere feil ved ny søknad uten virksomhetsnummer for arbeidsgiver`() {
        personMediator.håndterNySøknad(nySøknadHendelse(arbeidsgiver = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any(), any<NySøknadHendelse>())
        }
    }

    @Test
    fun `skal håndtere feil ved sendt søknad uten virksomhetsnummer for arbeidsgiver`() {
        personMediator.håndterSendtSøknad(sendtSøknadHendelse(arbeidsgiver = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any(), any<SendtSøknadHendelse>())
        }
    }

    @Test
    fun `skal håndtere feil ved inntektsmelding uten virksomhetsnummer for arbeidsgiver`() {
        personMediator.håndterInntektsmelding(inntektsmeldingHendelse(virksomhetsnummer = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any(), any<InntektsmeldingHendelse>())
        }
    }

    @Test
    fun `skal lage gosys-oppgave når saken må behandles i infotrygd`() {
        beInMåBehandlesIInfotrygdState()

        verify(exactly = 1) {
            oppgaveProducer.opprettOppgave(aktørId = sendtSøknadHendelse.aktørId())
        }
    }

    @Test
    fun `innsendt Nysøknad, Søknad og Inntektmelding fører til at sykepengehistorikk blir etterspurt`() {
        val aktørID = "1234567890123"
        val virksomhetsnummer = "123456789"

        sendNySøknad(aktørID, virksomhetsnummer)
        assertSakskompleksEndretEvent(aktørId = aktørID, virksomhetsnummer = virksomhetsnummer, previousState = START, currentState = NY_SØKNAD_MOTTATT)

        sendSøknad(aktørID, virksomhetsnummer)
        assertSakskompleksEndretEvent(aktørId = aktørID, virksomhetsnummer = virksomhetsnummer, previousState = NY_SØKNAD_MOTTATT, currentState = SENDT_SØKNAD_MOTTATT)

        sendInntektsmelding(aktørID, virksomhetsnummer)
        assertSakskompleksEndretEvent(aktørId = aktørID, virksomhetsnummer = virksomhetsnummer, previousState = SENDT_SØKNAD_MOTTATT, currentState = KOMPLETT_SYKDOMSTIDSLINJE)

        assertBehov(aktørId = aktørID, virksomhetsnummer = virksomhetsnummer, behovsType = BehovsTyper.Sykepengehistorikk)
    }

    @Test
    fun `innsendt Nysøknad, Inntektmelding og Søknad fører til at sykepengehistorikk blir etterspurt`() {
        val aktørId = "0123456789012"
        val virksomhetsnummer = "012345678"

        sendNySøknad(aktørId, virksomhetsnummer)
        sendInntektsmelding(aktørId, virksomhetsnummer)
        sendSøknad(aktørId, virksomhetsnummer)

        assertBehov(aktørId = aktørId, virksomhetsnummer = virksomhetsnummer, behovsType = BehovsTyper.Sykepengehistorikk)
    }

    @Test
    fun `sendt søknad uten uten ny søknad først skal behandles manuelt av saksbehandler`() {
        val aktørID = "2345678901234"
        val virksomhetsnummer = "234567890"

        sendSøknad(aktørID, virksomhetsnummer)

        assertOpprettGosysOppgave(aktørId = aktørID)
    }

    @Test
    fun `gitt en sak for godkjenning, når utbetaling er godkjent skal vi produsere et utbetalingbehov`() {
        val aktørId = "87654323421962"
        val virksomhetsnummer = "123456789"

        sendNySøknad(aktørId, virksomhetsnummer)
        sendSøknad(aktørId, virksomhetsnummer)
        sendInntektsmelding(aktørId, virksomhetsnummer)

        assertBehov(aktørId = aktørId, virksomhetsnummer = virksomhetsnummer, behovsType = BehovsTyper.Sykepengehistorikk)

        sendSykepengehistorikk(emptyList())

        assertBehov(aktørId = aktørId, virksomhetsnummer = virksomhetsnummer, behovsType = BehovsTyper.GodkjenningFraSaksbehandler)

        sendManuellSaksbehandling(
                saksbehandlerIdent = "en_saksbehandler_ident",
                utbetalingGodkjent = true
        )

        assertBehov(aktørId = aktørId, virksomhetsnummer = virksomhetsnummer, behovsType = BehovsTyper.Utbetaling)
    }

    private fun sendNySøknad(aktørId: String, virksomhetsnummer: String) {
        personMediator.håndterNySøknad(nySøknadHendelse(
                aktørId = aktørId,
                arbeidsgiver = ArbeidsgiverDTO(
                        orgnummer = virksomhetsnummer,
                        navn = "en_arbeidsgiver"
                )
        ))
    }

    private fun sendSøknad(aktørId: String, virksomhetsnummer: String) {
        personMediator.håndterSendtSøknad(sendtSøknadHendelse(
                aktørId = aktørId,
                arbeidsgiver = ArbeidsgiverDTO(
                        orgnummer = virksomhetsnummer,
                        navn = "en_arbeidsgiver"
                )
        ))
    }

    private fun sendInntektsmelding(aktørId: String, virksomhetsnummer: String) {
        personMediator.håndterInntektsmelding(inntektsmeldingHendelse(
                aktørId = aktørId,
                virksomhetsnummer = virksomhetsnummer
        ))
    }

    private fun beInMåBehandlesIInfotrygdState() {
        personMediator.håndterSendtSøknad(sendtSøknadHendelse)
    }

    private fun sendSykepengehistorikk(perioder: List<SpolePeriode>) {
        Sykepengehistorikk(objectMapper.readTree(løsSykepengehistorikkBehov(perioder).toJson())).let {
            personMediator.håndterSykepengehistorikk(SykepengehistorikkHendelse(it))
        }
    }

    private fun sendManuellSaksbehandling(saksbehandlerIdent: String, utbetalingGodkjent: Boolean) {
        løsManuellSaksbehandlingBehov(
                saksbehandlerIdent = saksbehandlerIdent,
                utbetalingGodkjent = utbetalingGodkjent
        ).let {
            personMediator.håndterManuellSaksbehandling(ManuellSaksbehandlingHendelse(it))
        }
    }

    private fun løsManuellSaksbehandlingBehov(saksbehandlerIdent: String, utbetalingGodkjent: Boolean): Behov {
        return finnBehov(BehovsTyper.GodkjenningFraSaksbehandler).also {
            it["saksbehandlerIdent"] = saksbehandlerIdent
            it.løsBehov(mapOf(
                    "godkjent" to utbetalingGodkjent
            ))
        }
    }

    private fun løsSykepengehistorikkBehov(perioder: List<SpolePeriode>): Behov {
        return finnBehov(BehovsTyper.Sykepengehistorikk).also {
            it.løsBehov(responsFraSpole(
                    perioder = perioder
            ))
        }
    }

    private fun finnBehov(behovsType: BehovsTyper): Behov {
        return behovsliste.
                first { it.behovType() == behovsType.name }
    }

    private fun assertSakskompleksEndretEvent(aktørId: String, virksomhetsnummer: String, previousState: Sakskompleks.TilstandType, currentState: Sakskompleks.TilstandType) {
        verify(exactly = 1) {
            sakskompleksEventProducer.sendEndringEvent(match {
                it.previousState == previousState
                        && it.currentState == currentState
                        && it.aktørId == aktørId
                        && it.organisasjonsnummer == virksomhetsnummer
            })
        }
    }
    private fun assertOpprettGosysOppgave(aktørId: String) {
        verify(exactly = 1) {
            oppgaveProducer.opprettOppgave(aktørId = aktørId)
        }
    }

    private fun assertBehov(aktørId: String, virksomhetsnummer: String, behovsType: BehovsTyper) {
        assertEquals(1, behovsliste
                .filter { it.behovType() == behovsType.name}
                .filter { aktørId == it["aktørId"] }
                .filter { virksomhetsnummer == it["organisasjonsnummer"] }
                .size)
    }
}
