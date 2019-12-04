package no.nav.helse.unit.spleis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.mockk.*
import no.nav.helse.*
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.responsFraSpole
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.søknadDTO
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProducer
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.sykepengehistorikk.Sykepengehistorikk
import no.nav.helse.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.hendelser.søknad.Sykepengesøknad
import no.nav.helse.sak.TilstandType
import no.nav.helse.spleis.*
import no.nav.helse.spleis.oppgave.GosysOppgaveProducer
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
    fun `skal lage gosys-oppgave når saken må behandles i infotrygd`() {
        beInMåBehandlesIInfotrygdState()

        verify(exactly = 1) {
            oppgaveProducer.opprettOppgave(aktørId = sendtSøknadHendelse.aktørId(), fødselsnummer = sendtSøknadHendelse.fødselsnummer())
        }
    }

    @Test
    fun `gitt en sak for godkjenning, når utbetaling er godkjent skal vi produsere et utbetalingbehov`() {
        val aktørId = "87654323421962"
        val fødselsnummer = "01017000000"
        val virksomhetsnummer = "123456789"

        sendNySøknad(aktørId, fødselsnummer, virksomhetsnummer)
        sendSøknad(aktørId, fødselsnummer, virksomhetsnummer)
        sendInntektsmelding(aktørId, fødselsnummer, virksomhetsnummer)

        assertBehov(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            virksomhetsnummer = virksomhetsnummer,
            behovsType = BehovsTyper.Sykepengehistorikk
        )

        sendSykepengehistorikk(emptyList())

        assertBehov(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            virksomhetsnummer = virksomhetsnummer,
            behovsType = BehovsTyper.GodkjenningFraSaksbehandler
        )

        sendGodkjenningFraSaksbehandler(
            saksbehandlerIdent = "en_saksbehandler_ident",
            utbetalingGodkjent = true
        )

        assertBehov(aktørId = aktørId, fødselsnummer = fødselsnummer, virksomhetsnummer = virksomhetsnummer, behovsType = BehovsTyper.Utbetaling)
    }

    @Test
    fun `gitt en komplett tidslinje, når vi mottar sykepengehistorikk mer enn 6 måneder tilbake i tid, så skal saken til Speil for godkjenning`() {
        val aktørID = "87654321962"
        val fødselsnummer = "01017000000"
        val virksomhetsnummer = "123456789"

        val søknad = sendNySøknad(aktørID, fødselsnummer, virksomhetsnummer)
        sendSøknad(aktørID, fødselsnummer, virksomhetsnummer)
        sendInntektsmelding(aktørID, fødselsnummer, virksomhetsnummer)
        sendSykepengehistorikk(
            listOf(
                SpolePeriode(
                    fom = søknad.fom!!.minusMonths(8),
                    tom = søknad.fom!!.minusMonths(7),
                    grad = "100"
                )
            )
        )

        assertBehov(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
            virksomhetsnummer = virksomhetsnummer,
            behovsType = BehovsTyper.GodkjenningFraSaksbehandler
        )
    }

    @Test
    @Disabled("Denne saken skal bli behandlet nå, i epic 3")
    fun `gitt en komplett tidslinje, når det er mer enn 16 arbeidsdager etter utbetalingsperioden, så skal den behandles manuelt av saksbehandler (17ende arbeidsdag faller på en ukedag)`() {
        val aktørID = "87654321950"
        val fødselsnummer = "01017000000"
        val virksomhetsnummer = "123456789"

        // 8 dager egenmelding - 9 sykedager - 17 arbeidsdager skal gi en ugyldig utbetalingstidslinje

        val nySøknad = søknadDTO(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer),
            status = SoknadsstatusDTO.NY,
            egenmeldinger = emptyList(),
            søknadsperioder = listOf(
                SoknadsperiodeDTO(
                    fom = 6.juli,
                    tom = 1.august,
                    sykmeldingsgrad = 100
                )
            ),
            fravær = emptyList()
        )

        val søknadMedUgyldigBetalingslinje = søknadDTO(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
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
                )
            ),
            arbeidGjenopptatt = 16.juli,
            fravær = emptyList()
        )

        val inntektsMelding = inntektsmeldingDTO(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
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

        sendNySøknad(aktørID, fødselsnummer, virksomhetsnummer, nySøknad)
        sendSøknad(aktørID, fødselsnummer, virksomhetsnummer, søknadMedUgyldigBetalingslinje)
        sendInntektsmelding(aktørID, fødselsnummer, virksomhetsnummer, inntektsMelding)

        val sykehistorikk = listOf(
            SpolePeriode(
                fom = 1.juli.minusMonths(8),
                tom = 1.juli.minusMonths(7),
                grad = "100"
            )
        )
        sendSykepengehistorikk(sykehistorikk)

        assertOpprettGosysOppgaveNøyaktigEnGang(aktørId = aktørID, fødselsnummer = fødselsnummer)
    }

    @Test
    @Disabled("Denne sakem skal behandles nå, in epic 3")
    fun `gitt en komplett tidslinje, når det er mer enn 16 arbeidsdager etter utbetalingsperioden, så skal den behandles manuelt av saksbehandler (17ende arbeidsdag faller på en søndag)`() {
        val aktørID = "87654321738"
        val fødselsnummer = "01017000000"
        val virksomhetsnummer = "123456789"

        // 8 dager egenmelding - 12 sykedager - 17 arbeidsdager skal gi en ugyldig utbetalingstidslinje

        val nySøknad = søknadDTO(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer),
            status = SoknadsstatusDTO.NY,
            egenmeldinger = emptyList(),
            søknadsperioder = listOf(
                SoknadsperiodeDTO(
                    fom = 6.juli,
                    tom = 4.august,
                    sykmeldingsgrad = 100
                )
            ),
            fravær = emptyList()
        )

        val søknadMedUgyldigBetalingslinje = søknadDTO(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
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
                )
            ),
            arbeidGjenopptatt = 19.juli,
            fravær = emptyList()
        )

        val inntektsMelding = inntektsmeldingDTO(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
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

        sendNySøknad(aktørID, fødselsnummer, virksomhetsnummer, nySøknad)
        sendSøknad(aktørID, fødselsnummer, virksomhetsnummer, søknadMedUgyldigBetalingslinje)
        sendInntektsmelding(aktørID, fødselsnummer, virksomhetsnummer, inntektsMelding)

        val sykehistorikk = listOf(
            SpolePeriode(
                fom = 1.juli.minusMonths(8),
                tom = 1.juli.minusMonths(7),
                grad = "100"
            )
        )
        sendSykepengehistorikk(sykehistorikk)

        assertOpprettGosysOppgaveNøyaktigEnGang(aktørId = aktørID, fødselsnummer = fødselsnummer)
    }

    @Test
    fun `gitt en sak for godkjenning, når utbetaling ikke er godkjent skal saken til Infotrygd`() {
        val aktørID = "8787654421962"
        val fødselsnummer = "01017000000"
        val virksomhetsnummer = "123456789"

        val søknad = sendNySøknad(aktørID, fødselsnummer, virksomhetsnummer)
        sendSøknad(aktørID, fødselsnummer, virksomhetsnummer)
        sendInntektsmelding(aktørID, fødselsnummer, virksomhetsnummer)
        sendSykepengehistorikk(
            listOf(
                SpolePeriode(
                    fom = søknad.fom!!.minusMonths(8),
                    tom = søknad.fom!!.minusMonths(7),
                    grad = "100"
                )
            )
        )
        sendGodkjenningFraSaksbehandler("en_saksbehandler_ident", false)

        assertOpprettGosysOppgaveNøyaktigEnGang(aktørId = aktørID, fødselsnummer = fødselsnummer)
    }


    @Test
    fun `gitt en komplett tidslinje, når vi mottar sykepengehistorikk mindre enn 7 måneder tilbake i tid, så skal saken til Infotrygd`() {
        val aktørID = "87654321963"
        val fødselsnummer = "01017000000"
        val virksomhetsnummer = "123456789"

        val søknad = sendNySøknad(aktørID, fødselsnummer, virksomhetsnummer)
        sendSøknad(aktørID, fødselsnummer, virksomhetsnummer)
        sendInntektsmelding(aktørID, fødselsnummer, virksomhetsnummer)
        sendSykepengehistorikk(
            listOf(
                SpolePeriode(
                    fom = søknad.fom!!.minusMonths(6),
                    tom = søknad.fom!!.minusMonths(5),
                    grad = "100"
                )
            )
        )

        assertOpprettGosysOppgaveNøyaktigEnGang(aktørId = aktørID, fødselsnummer = fødselsnummer)
    }

    private fun sendNySøknad(
        aktørId: String,
        fødselsnummer: String,
        virksomhetsnummer: String,
        søknad: SykepengesoknadDTO? = null
    ): SykepengesoknadDTO {
        return (søknad ?: søknadDTO(
            status = SoknadsstatusDTO.NY,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            arbeidsgiver = ArbeidsgiverDTO(
                orgnummer = virksomhetsnummer,
                navn = "en_arbeidsgiver"
            )
        )).also {
            sakMediator.håndter(NySøknadHendelse(Sykepengesøknad(it.toJsonNode())))
        }
    }

    private fun sendSøknad(
        aktørId: String,
        fødselsnummer: String,
        virksomhetsnummer: String,
        søknad: SykepengesoknadDTO? = null
    ): SykepengesoknadDTO {
        return (søknad ?: søknadDTO(
            status = SoknadsstatusDTO.SENDT,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            arbeidsgiver = ArbeidsgiverDTO(
                orgnummer = virksomhetsnummer,
                navn = "en_arbeidsgiver"
            )
        )).also {
            sakMediator.håndter(SendtSøknadHendelse(Sykepengesøknad(it.toJsonNode())))
        }
    }

    private fun sendInntektsmelding(
        aktørId: String,
        fødselsnummer: String,
        virksomhetsnummer: String,
        inntektsmelding: Inntektsmelding? = null
    ): Inntektsmelding {
        return (inntektsmelding ?: inntektsmeldingDTO(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
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
            it.løsBehov(
                mapOf(
                    "godkjent" to utbetalingGodkjent
                )
            )
        }
    }

    private fun løsSykepengehistorikkBehov(perioder: List<SpolePeriode>): Behov {
        return finnBehov(BehovsTyper.Sykepengehistorikk).also {
            it.løsBehov(
                responsFraSpole(
                    perioder = perioder
                )
            )
        }
    }

    private fun finnBehov(behovsType: BehovsTyper): Behov {
        return behovsliste.first { it.behovType() == behovsType.name }
    }

    private fun assertVedtaksperiodeEndretEvent(
        aktørId: String,
        fødselsnummer: String,
        virksomhetsnummer: String,
        previousState: TilstandType,
        currentState: TilstandType
    ) {
        verify(exactly = 1) {
            vedtaksperiodeEventProducer.sendEndringEvent(match {
                it.previousState == previousState
                    && it.currentState == currentState
                    && it.aktørId == aktørId
                    && it.fødselsnummer == fødselsnummer
                    && it.organisasjonsnummer == virksomhetsnummer
            })
        }
    }

    private fun assertOpprettGosysOppgaveNøyaktigEnGang(aktørId: String, fødselsnummer: String) {
        verify(exactly = 1) {
            oppgaveProducer.opprettOppgave(aktørId = aktørId, fødselsnummer = fødselsnummer)
        }
    }

    private fun assertOpprettGosysOppgave(aktørId: String, fødselsnummer: String) {
        verify(atLeast = 1) {
            oppgaveProducer.opprettOppgave(aktørId = aktørId, fødselsnummer = fødselsnummer)
        }
    }

    private fun assertBehov(
        aktørId: String,
        fødselsnummer: String,
        virksomhetsnummer: String,
        behovsType: BehovsTyper
    ) {
        assertEquals(1, behovsliste
            .filter { it.behovType() == behovsType.name }
            .filter { aktørId == it["aktørId"] }
            .filter { fødselsnummer == it["fødselsnummer"] }
            .filter { virksomhetsnummer == it["organisasjonsnummer"] }
            .size)
    }
}
