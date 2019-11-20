package no.nav.helse.unit.spleis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.mockk.*
import no.nav.helse.*
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.responsFraSpole
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.søknadDTO
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProducer
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.sak.Vedtaksperiode
import no.nav.helse.sak.Vedtaksperiode.TilstandType.*
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.sykepengehistorikk.Sykepengehistorikk
import no.nav.helse.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.hendelser.søknad.Sykepengesøknad
import no.nav.helse.spleis.*
import no.nav.helse.spleis.oppgave.GosysOppgaveProducer
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SakMediatorTest {

    private val probe = mockk<VedtaksperiodeProbe>(relaxed = true)
    private val oppgaveProducer = mockk<GosysOppgaveProducer>(relaxed = true)
    private val vedtaksperiodeEventProducer = mockk<VedtaksperiodeEventProducer>(relaxed = true)

    private val behovsliste = mutableListOf<Behov>()
    private val behovProducer = mockk<BehovProducer>(relaxed = true).also {
        every {
            it.sendNyttBehov(match {
                behovsliste.add(Behov.fromJson(it.toJson()))
                true
            })
        } just runs
    }
    private val repo = HashmapSakRepository()
    private val utbetalingsRepo = mockk<UtbetalingsreferanseRepository>(relaxed = true)
    private val lagreUtbetalingDao = mockk<LagreUtbetalingDao>(relaxed = true)

    private val sakMediator = SakMediator(
            vedtaksperiodeProbe = probe,
            sakRepository = repo,
            lagreSakDao = repo,
            utbetalingsreferanseRepository = utbetalingsRepo,
            lagreUtbetalingDao = lagreUtbetalingDao,
            behovProducer = behovProducer,
            gosysOppgaveProducer = oppgaveProducer,
            vedtaksperiodeEventProducer = vedtaksperiodeEventProducer
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
        sakMediator.håndter(nySøknadHendelse(arbeidsgiver = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any<NySøknadHendelse>())
        }
    }

    @Test
    fun `skal håndtere feil ved sendt søknad uten virksomhetsnummer for arbeidsgiver`() {
        sakMediator.håndter(sendtSøknadHendelse(arbeidsgiver = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any<SendtSøknadHendelse>())
        }
    }

    @Test
    fun `skal håndtere feil ved inntektsmelding uten virksomhetsnummer for arbeidsgiver`() {
        sakMediator.håndter(inntektsmeldingHendelse(virksomhetsnummer = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any<InntektsmeldingHendelse>())
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
        assertVedtaksperiodeEndretEvent(aktørId = aktørID, virksomhetsnummer = virksomhetsnummer, previousState = START, currentState = NY_SØKNAD_MOTTATT)

        sendSøknad(aktørID, virksomhetsnummer)
        assertVedtaksperiodeEndretEvent(aktørId = aktørID, virksomhetsnummer = virksomhetsnummer, previousState = NY_SØKNAD_MOTTATT, currentState = SENDT_SØKNAD_MOTTATT)

        sendInntektsmelding(aktørID, virksomhetsnummer)
        assertVedtaksperiodeEndretEvent(aktørId = aktørID, virksomhetsnummer = virksomhetsnummer, previousState = SENDT_SØKNAD_MOTTATT, currentState = KOMPLETT_SYKDOMSTIDSLINJE)

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

        assertOpprettGosysOppgaveNøyaktigEnGang(aktørId = aktørID)
    }

    @Test
    fun `gitt en komplett sykdomstidslinje, når det kommer en til sendt søknad med en annen arbeidsgiver, så skal begge søknadene behandles manuelt av saksbehandler`() {
        val aktørID = "2345678901234"
        val virksomhetsnummer_a = "234567890"
        val virksomhetsnummer_b = "098765432"

        sendNySøknad(aktørID, virksomhetsnummer_a)
        sendInntektsmelding(aktørID, virksomhetsnummer_a)
        sendSøknad(aktørID, virksomhetsnummer_a)
        assertVedtaksperiodeEndretEvent(
            aktørId = aktørID,
            virksomhetsnummer = virksomhetsnummer_a,
            previousState = INNTEKTSMELDING_MOTTATT,
            currentState = KOMPLETT_SYKDOMSTIDSLINJE
        )

        sendNySøknad(aktørID, virksomhetsnummer_b)
        sendSøknad(aktørID, virksomhetsnummer_b)
        assertVedtaksperiodeEndretEvent(
            aktørId = aktørID,
            virksomhetsnummer = virksomhetsnummer_a,
            previousState = KOMPLETT_SYKDOMSTIDSLINJE,
            currentState = TIL_INFOTRYGD
        )
        assertVedtaksperiodeEndretEvent(
            aktørId = aktørID,
            virksomhetsnummer = virksomhetsnummer_b,
            previousState = NY_SØKNAD_MOTTATT,
            currentState = TIL_INFOTRYGD
        )
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

        sendGodkjenningFraSaksbehandler(
                saksbehandlerIdent = "en_saksbehandler_ident",
                utbetalingGodkjent = true
        )

        assertBehov(aktørId = aktørId, virksomhetsnummer = virksomhetsnummer, behovsType = BehovsTyper.Utbetaling)
    }

    @Test
    fun `gitt en komplett tidslinje, når vi mottar sykepengehistorikk mer enn 6 måneder tilbake i tid, så skal saken til Speil for godkjenning`() {
        val aktørID = "87654321962"
        val virksomhetsnummer = "123456789"

        val søknad = sendNySøknad(aktørID, virksomhetsnummer)
        sendSøknad(aktørID, virksomhetsnummer)
        sendInntektsmelding(aktørID, virksomhetsnummer)
        sendSykepengehistorikk(listOf(SpolePeriode(
                fom = søknad.fom!!.minusMonths(8),
                tom = søknad.fom!!.minusMonths(7),
                grad = "100"
        )))

        assertBehov(aktørId = aktørID, virksomhetsnummer = virksomhetsnummer, behovsType = BehovsTyper.GodkjenningFraSaksbehandler)
    }

    @Test
    fun `gitt en komplett tidslinje, når det er mer enn 16 arbeidsdager etter utbetalingsperioden, så skal den behandles manuelt av saksbehandler (17ende arbeidsdag faller på en ukedag)`() {
        val aktørID = "87654321950"
        val virksomhetsnummer = "123456789"

        // 8 dager egenmelding - 9 sykedager - 17 arbeidsdager skal gi en ugyldig utbetalingstidslinje

        val nySøknad = søknadDTO(
                aktørId = aktørID,
                arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer),
                status = SoknadsstatusDTO.NY,
                egenmeldinger = emptyList(),
                søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                                fom = 6.juli,
                                tom = 1.august,
                                sykmeldingsgrad = 100
                        )),
                fravær = emptyList()
        )

        val søknadMedUgyldigBetalingslinje = søknadDTO(
                aktørId = aktørID,
                arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer),
                status = SoknadsstatusDTO.SENDT,
                egenmeldinger = listOf(
                        PeriodeDTO(28.juni, 5.juli)
                ),
                søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                                fom = 6.juli,
                                tom = 1.august,
                                sykmeldingsgrad = 100
                        )),
                arbeidGjenopptatt = 16.juli,
                fravær = emptyList()
        )

        val inntektsMelding = inntektsmeldingDTO(
                aktørId = aktørID,
                virksomhetsnummer = virksomhetsnummer,
                førsteFraværsdag = 1.juli,
                arbeidsgiverperioder = listOf(
                        Periode(28.juni, 13.juli)
                ),
                feriePerioder = emptyList(),
                refusjon = Refusjon(
                        beloepPrMnd = 666.toBigDecimal(),
                        opphoersdato = null
                ),
                endringerIRefusjoner = emptyList(),
                beregnetInntekt = 666.toBigDecimal()
        )

        sendNySøknad(aktørID, virksomhetsnummer, nySøknad)
        sendSøknad(aktørID, virksomhetsnummer, søknadMedUgyldigBetalingslinje)
        sendInntektsmelding(aktørID, virksomhetsnummer, inntektsMelding)

        val sykehistorikk = listOf(SpolePeriode(
                fom = 1.juli.minusMonths(8),
                tom = 1.juli.minusMonths(7),
                grad = "100"
        ))
        sendSykepengehistorikk(sykehistorikk)

        assertOpprettGosysOppgaveNøyaktigEnGang(aktørId = aktørID)
    }

    @Test
    fun `gitt en komplett tidslinje, når det er mer enn 16 arbeidsdager etter utbetalingsperioden, så skal den behandles manuelt av saksbehandler (17ende arbeidsdag faller på en søndag)`() {
        val aktørID = "87654321738"
        val virksomhetsnummer = "123456789"

        // 8 dager egenmelding - 12 sykedager - 17 arbeidsdager skal gi en ugyldig utbetalingstidslinje

        val nySøknad = søknadDTO(
                aktørId = aktørID,
                arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer),
                status = SoknadsstatusDTO.NY,
                egenmeldinger = emptyList(),
                søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                                fom = 6.juli,
                                tom = 4.august,
                                sykmeldingsgrad = 100
                        )),
                fravær = emptyList()
        )

        val søknadMedUgyldigBetalingslinje = søknadDTO(
                aktørId = aktørID,
                arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer),
                status = SoknadsstatusDTO.SENDT,
                egenmeldinger = listOf(
                        PeriodeDTO(28.juni, 5.juli)
                ),
                søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                                fom = 6.juli,
                                tom = 4.august,
                                sykmeldingsgrad = 100
                        )),
                arbeidGjenopptatt = 19.juli,
                fravær = emptyList()
        )

        val inntektsMelding = inntektsmeldingDTO(
                aktørId = aktørID,
                virksomhetsnummer = virksomhetsnummer,
                førsteFraværsdag = 1.juli,
                arbeidsgiverperioder = listOf(
                        Periode(28.juni, 13.juli)
                ),
                feriePerioder = emptyList(),
                refusjon = Refusjon(
                        beloepPrMnd = 666.toBigDecimal(),
                        opphoersdato = null
                ),
                endringerIRefusjoner = emptyList(),
                beregnetInntekt = 666.toBigDecimal()
        )

        sendNySøknad(aktørID, virksomhetsnummer, nySøknad)
        sendSøknad(aktørID, virksomhetsnummer, søknadMedUgyldigBetalingslinje)
        sendInntektsmelding(aktørID, virksomhetsnummer, inntektsMelding)

        val sykehistorikk = listOf(SpolePeriode(
                fom = 1.juli.minusMonths(8),
                tom = 1.juli.minusMonths(7),
                grad = "100"
        ))
        sendSykepengehistorikk(sykehistorikk)

        assertOpprettGosysOppgaveNøyaktigEnGang(aktørId = aktørID)
    }

    @Test
    fun `gitt en sak for godkjenning, når utbetaling ikke er godkjent skal saken til Infotrygd`() {
        val aktørID = "8787654421962"
        val virksomhetsnummer = "123456789"

        val søknad = sendNySøknad(aktørID, virksomhetsnummer)
        sendSøknad(aktørID, virksomhetsnummer)
        sendInntektsmelding(aktørID, virksomhetsnummer)
        sendSykepengehistorikk(listOf(SpolePeriode(
                fom = søknad.fom!!.minusMonths(8),
                tom = søknad.fom!!.minusMonths(7),
                grad = "100"
        )))
        sendGodkjenningFraSaksbehandler("en_saksbehandler_ident", false)

        assertOpprettGosysOppgaveNøyaktigEnGang(aktørId = aktørID)
    }


    @Test
    fun `gitt en komplett tidslinje, når vi mottar sykepengehistorikk mindre enn 7 måneder tilbake i tid, så skal saken til Infotrygd`() {
        val aktørID = "87654321963"
        val virksomhetsnummer = "123456789"

        val søknad = sendNySøknad(aktørID, virksomhetsnummer)
        sendSøknad(aktørID, virksomhetsnummer)
        sendInntektsmelding(aktørID, virksomhetsnummer)
        sendSykepengehistorikk(listOf(SpolePeriode(
                fom = søknad.fom!!.minusMonths(6),
                tom = søknad.fom!!.minusMonths(5),
                grad = "100"
        )))

        assertOpprettGosysOppgaveNøyaktigEnGang(aktørId = aktørID)
    }

    private fun sendNySøknad(aktørId: String, virksomhetsnummer: String, søknad: SykepengesoknadDTO? = null): SykepengesoknadDTO {
        return (søknad ?: søknadDTO(
                status = SoknadsstatusDTO.NY,
                aktørId = aktørId,
                arbeidsgiver = ArbeidsgiverDTO(
                        orgnummer = virksomhetsnummer,
                        navn = "en_arbeidsgiver"
                )
        )).also {
            sakMediator.håndter(NySøknadHendelse(Sykepengesøknad(it.toJsonNode())))
        }
    }

    private fun sendSøknad(aktørId: String, virksomhetsnummer: String, søknad: SykepengesoknadDTO? = null): SykepengesoknadDTO {
        return (søknad ?: søknadDTO(
                status = SoknadsstatusDTO.SENDT,
                aktørId = aktørId,
                arbeidsgiver = ArbeidsgiverDTO(
                        orgnummer = virksomhetsnummer,
                        navn = "en_arbeidsgiver"
                )
        )).also {
            sakMediator.håndter(SendtSøknadHendelse(Sykepengesøknad(it.toJsonNode())))
        }
    }

    private fun sendInntektsmelding(aktørId: String, virksomhetsnummer: String, inntektsmelding: Inntektsmelding? = null): Inntektsmelding {
        return (inntektsmelding ?: inntektsmeldingDTO(
                aktørId = aktørId,
                virksomhetsnummer = virksomhetsnummer
        )).also {
            sakMediator.håndter(InntektsmeldingHendelse(no.nav.helse.hendelser.inntektsmelding.Inntektsmelding(it.toJsonNode())))
        }
    }

    private fun beInMåBehandlesIInfotrygdState() {
        sakMediator.håndter(sendtSøknadHendelse)
    }

    private fun sendSykepengehistorikk(perioder: List<SpolePeriode>) {
        Sykepengehistorikk(objectMapper.readTree(løsSykepengehistorikkBehov(perioder).toJson())).let {
            sakMediator.håndter(SykepengehistorikkHendelse(it))
        }
    }

    private fun sendGodkjenningFraSaksbehandler(saksbehandlerIdent: String, utbetalingGodkjent: Boolean) {
        løsManuellSaksbehandlingBehov(
                saksbehandlerIdent = saksbehandlerIdent,
                utbetalingGodkjent = utbetalingGodkjent
        ).let {
            sakMediator.håndter(ManuellSaksbehandlingHendelse(it))
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

    private fun assertVedtaksperiodeEndretEvent(aktørId: String, virksomhetsnummer: String, previousState: Vedtaksperiode.TilstandType, currentState: Vedtaksperiode.TilstandType) {
        verify(exactly = 1) {
            vedtaksperiodeEventProducer.sendEndringEvent(match {
                it.previousState == previousState
                        && it.currentState == currentState
                        && it.aktørId == aktørId
                        && it.organisasjonsnummer == virksomhetsnummer
            })
        }
    }

    private fun assertOpprettGosysOppgaveNøyaktigEnGang(aktørId: String) {
        verify(exactly = 1) {
            oppgaveProducer.opprettOppgave(aktørId = aktørId)
        }
    }

    private fun assertOpprettGosysOppgave(aktørId: String) {
        verify(atLeast = 1) {
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
