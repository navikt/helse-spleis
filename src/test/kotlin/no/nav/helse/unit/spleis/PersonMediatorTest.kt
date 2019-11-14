package no.nav.helse.unit.spleis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.mockk.*
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.responsFraSpole
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProducer
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.person.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.person.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.person.hendelser.sykepengehistorikk.Sykepengehistorikk
import no.nav.helse.person.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.person.hendelser.søknad.NySøknadHendelse
import no.nav.helse.person.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.spleis.LagreUtbetalingDao
import no.nav.helse.spleis.PersonMediator
import no.nav.helse.spleis.SakskompleksProbe
import no.nav.helse.spleis.UtbetalingsreferanseRepository
import no.nav.helse.spleis.oppgave.GosysOppgaveProducer
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonMediatorTest {

    private val probe = mockk<SakskompleksProbe>(relaxed = true)
    private val oppgaveProducer = mockk<GosysOppgaveProducer>(relaxed = true)

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
            sakskompleksEventProducer = mockk(relaxed = true)
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
    fun `gitt en sak for godkjenning, når utbetaling er godkjent skal vi produsere et utbetalingbehov`() {
        val aktørId = "87654323421962"
        val virksomhetsnummer = "123456789"

        personMediator.håndterNySøknad(nySøknadHendelse(
                aktørId = aktørId,
                arbeidsgiver = ArbeidsgiverDTO(
                        orgnummer = virksomhetsnummer,
                        navn = "en_arbeidsgiver"
                )

        ))

        personMediator.håndterSendtSøknad(sendtSøknadHendelse(
                aktørId = aktørId,
                arbeidsgiver = ArbeidsgiverDTO(
                        orgnummer = virksomhetsnummer,
                        navn = "en_arbeidsgiver"
                )
        ))

        personMediator.håndterInntektsmelding(inntektsmeldingHendelse(
                aktørId = aktørId,
                virksomhetsnummer = virksomhetsnummer
        ))

        assertEquals(1, behovsliste.filter { it.behovType() == BehovsTyper.Sykepengehistorikk.name}.size)

        val sykepengehistorikkløsning = behovsliste.
                first { it.behovType() == BehovsTyper.Sykepengehistorikk.name }
                .also {
                    it.løsBehov(responsFraSpole(
                            perioder = emptyList()
                    ))
                }.let {
                    Sykepengehistorikk(objectMapper.readTree(it.toJson()))
                }

        personMediator.håndterSykepengehistorikk(SykepengehistorikkHendelse(sykepengehistorikkløsning))

        assertEquals(1, behovsliste.filter { it.behovType() == BehovsTyper.GodkjenningFraSaksbehandler.name}.size)

        val manuellSaksbehandlingløsning = behovsliste.
                first { it.behovType() == BehovsTyper.GodkjenningFraSaksbehandler.name }
                .also {
                    it["saksbehandlerIdent"] = "en_saksbehandler_ident"
                    it.løsBehov(mapOf(
                            "godkjent" to true
                    ))
                }

        personMediator.håndterManuellSaksbehandling(ManuellSaksbehandlingHendelse(manuellSaksbehandlingløsning))

        assertEquals(1, behovsliste.filter { it.behovType() == BehovsTyper.Utbetaling.name}.size)
    }

    fun beInMåBehandlesIInfotrygdState() {
        personMediator.håndterSendtSøknad(sendtSøknadHendelse)
    }
}
