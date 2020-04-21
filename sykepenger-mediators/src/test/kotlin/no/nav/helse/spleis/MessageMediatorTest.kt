package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.mockk
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.meldinger.TestRapid
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import no.nav.syfo.kafka.felles.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import no.nav.inntektsmeldingkontrakt.Inntektsmelding as Inntektsmeldingkontrakt

internal class MessageMediatorTest {

    @Test
    internal fun `leser søknader`() {
        sendNySøknad()
        assertTrue(hendelseMediator.lestNySøknad)

        sendSøknad()
        assertTrue(hendelseMediator.lestSendtSøknad)
    }

    @Test
    internal fun `leser inntektsmeldinger`() {
        sendInnteksmelding()
        assertTrue(hendelseMediator.lestInntektsmelding)
    }

    @Test
    internal fun `leser påminnelser`() {
        sendNyPåminnelse()
        assertTrue(hendelseMediator.lestPåminnelse)
    }

    @Test
    internal fun `leser behov`() {
        sendVilkårsgrunnlag()
        assertTrue(hendelseMediator.lestVilkårsgrunnlag)

        sendSimulering()
        assertTrue(hendelseMediator.lestSimulering)

        sendYtelser()
        assertTrue(hendelseMediator.lestYtelser)

        sendManuellSaksbehandling()
        assertTrue(hendelseMediator.lestManuellSaksbehandling)

        sendUtbetaling()
        assertTrue(hendelseMediator.lestUtbetaling)
    }

    @BeforeEach
    internal fun reset() {
        testRapid.reset()
        hendelseMediator.reset()
    }

    private companion object {
        private val defaultAktørId = UUID.randomUUID().toString()
        private val defaultFødselsnummer = UUID.randomUUID().toString()
        private val defaultOrganisasjonsnummer = UUID.randomUUID().toString()

        private val testRapid = TestRapid()
        private val hendelseMediator = TestHendelseMediator()

        init {
            MessageMediator(
                rapidsConnection = testRapid,
                hendelseRecorder = mockk(relaxed = true),
                hendelseMediator = hendelseMediator
            )
        }

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private fun sendGeneriskBehov(
            aktørId: String,
            fødselsnummer: String,
            organisasjonsnummer: String,
            behov: List<String> = listOf(),
            vedtaksperiodeId: UUID = UUID.randomUUID(),
            løsninger: Map<String, Any> = emptyMap(),
            ekstraFelter: Map<String, Any> = emptyMap()
        ) = testRapid.sendTestMessage(
            objectMapper.writeValueAsString(
                ekstraFelter + mapOf(
                    "@id" to UUID.randomUUID().toString(),
                    "@opprettet" to LocalDateTime.now(),
                    "@event_name" to "behov",
                    "@behov" to behov,
                    "tilstand" to "EN_TILSTAND",
                    "aktørId" to aktørId,
                    "fødselsnummer" to fødselsnummer,
                    "organisasjonsnummer" to organisasjonsnummer,
                    "vedtaksperiodeId" to vedtaksperiodeId.toString(),
                    "@løsning" to løsninger,
                    "@final" to true,
                    "@besvart" to LocalDateTime.now()
                )
            )
        )

        private fun sendNyPåminnelse(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ) {
            objectMapper.writeValueAsString(
                mapOf(
                    "@id" to UUID.randomUUID().toString(),
                    "@opprettet" to LocalDateTime.now(),
                    "@event_name" to "påminnelse",
                    "aktørId" to aktørId,
                    "fødselsnummer" to fødselsnummer,
                    "organisasjonsnummer" to organisasjonsnummer,
                    "vedtaksperiodeId" to UUID.randomUUID().toString(),
                    "tilstand" to TilstandType.START.name,
                    "antallGangerPåminnet" to 0,
                    "tilstandsendringstidspunkt" to LocalDateTime.now().toString(),
                    "påminnelsestidspunkt" to LocalDateTime.now().toString(),
                    "nestePåminnelsestidspunkt" to LocalDateTime.now().toString()
                )
            ).also { testRapid.sendTestMessage(it) }
        }

        private fun sendManuellSaksbehandling(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ) {
            sendGeneriskBehov(
                behov = listOf("Godkjenning"),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                ekstraFelter = mapOf(
                    "saksbehandlerIdent" to "en_saksbehandler",
                    "godkjenttidspunkt" to LocalDateTime.now()
                ),
                løsninger = mapOf(
                    "Godkjenning" to mapOf(
                        "godkjent" to true
                    )
                )
            )
        }

        private fun sendYtelser(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ) {
            sendGeneriskBehov(
                behov = listOf("Sykepengehistorikk", "Foreldrepenger"),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                løsninger = mapOf(
                    "Sykepengehistorikk" to emptyList<Any>(),
                    "Foreldrepenger" to emptyMap<String, String>()
                )
            )
        }

        private fun sendVilkårsgrunnlag(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer,
            egenAnsatt: Boolean = false
        ) {
            sendGeneriskBehov(
                behov = listOf("Inntektsberegning", "EgenAnsatt", "Opptjening", "Dagpenger", "Arbeidsavklaringspenger"),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                løsninger = mapOf(
                    "EgenAnsatt" to egenAnsatt,
                    "Inntektsberegning" to listOf(
                        mapOf(
                            "årMåned" to YearMonth.now().toString(),
                            "inntektsliste" to listOf(
                                mapOf(
                                    "beløp" to 1000.0,
                                    "inntektstype" to "LOENNSINNTEKT",
                                    "orgnummer" to "123456789"
                                )
                            )
                        )
                    ),
                    "Opptjening" to emptyList<Any>(),
                    "Dagpenger" to emptyList<Any>(),
                    "Arbeidsavklaringspenger" to emptyList<Any>()
                )
            )
        }

        private fun sendSimulering(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ) {
            sendGeneriskBehov(
                behov = listOf("Simulering"),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                løsninger = mapOf(
                    "Simulering" to mapOf(
                        "status" to "OK",
                        "feilmelding" to "",
                        "simulering" to mapOf(
                            "gjelderId" to fødselsnummer,
                            "gjelderNavn" to "Korona",
                            "datoBeregnet" to "2020-01-01",
                            "totalBelop" to 9999,
                            "periodeList" to listOf(
                                mapOf(
                                    "fom" to "2020-01-01",
                                    "tom" to "2020-01-02",
                                    "utbetaling" to listOf(
                                        mapOf(
                                            "fagSystemId" to "1231203123123",
                                            "utbetalesTilId" to organisasjonsnummer,
                                            "utbetalesTilNavn" to "Koronavirus",
                                            "forfall" to "2020-01-03",
                                            "feilkonto" to true,
                                            "detaljer" to listOf(
                                                mapOf(
                                                    "faktiskFom" to "2020-01-01",
                                                    "faktiskTom" to "2020-01-02",
                                                    "konto" to "12345678910og1112",
                                                    "belop" to 9999,
                                                    "tilbakeforing" to false,
                                                    "sats" to 1111,
                                                    "typeSats" to "DAGLIG",
                                                    "antallSats" to 9,
                                                    "uforegrad" to 100,
                                                    "klassekode" to "SPREFAG-IOP",
                                                    "klassekodeBeskrivelse" to "Sykepenger, Refusjon arbeidsgiver",
                                                    "utbetalingsType" to "YTELSE",
                                                    "refunderesOrgNr" to organisasjonsnummer
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }

        private fun sendUtbetaling(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer,
            utbetalingOK: Boolean = true
        ) {
            sendGeneriskBehov(
                behov = listOf("Utbetaling"),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                løsninger = mapOf(
                    "Utbetaling" to mapOf(
                        "status" to
                            if (utbetalingOK) UtbetalingHendelse.Oppdragstatus.AKSEPTERT.name
                            else UtbetalingHendelse.Oppdragstatus.AVVIST.name,
                        "beskrivelse" to if (!utbetalingOK) "FEIL fra Spenn" else ""
                    )
                ),
                ekstraFelter = mapOf(
                    "fagsystemId" to "123456789"
                )
            )
        }

        private fun sendInnteksmelding(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ): Inntektsmeldingkontrakt {
            val inntektsmelding = Inntektsmeldingkontrakt(
                inntektsmeldingId = UUID.randomUUID().toString(),
                arbeidstakerFnr = fødselsnummer,
                arbeidstakerAktorId = aktørId,
                virksomhetsnummer = organisasjonsnummer,
                arbeidsgiverFnr = null,
                arbeidsgiverAktorId = null,
                arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
                arbeidsforholdId = null,
                beregnetInntekt = BigDecimal.ONE,
                refusjon = Refusjon(BigDecimal.ONE, LocalDate.now()),
                endringIRefusjoner = emptyList(),
                opphoerAvNaturalytelser = emptyList(),
                gjenopptakelseNaturalytelser = emptyList(),
                arbeidsgiverperioder = emptyList(),
                status = Status.GYLDIG,
                arkivreferanse = "",
                ferieperioder = emptyList(),
                foersteFravaersdag = LocalDate.now(),
                mottattDato = LocalDateTime.now()
            )
            testRapid.sendTestMessage(inntektsmelding.toJsonNode().apply {
                this as ObjectNode
                put("@id", UUID.randomUUID().toString())
                put("@event_name", "inntektsmelding")
                put("@opprettet", LocalDateTime.now().toString())
            }.toString())
            return inntektsmelding
        }

        private fun sendSøknad(
            id: UUID = UUID.randomUUID(),
            aktørId: String = defaultAktørId,
            fødselsnummer: String = "fødselsnummer",
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ) {
            val sendtSøknad = SykepengesoknadDTO(
                status = SoknadsstatusDTO.SENDT,
                id = id.toString(),
                aktorId = aktørId,
                fodselsnummer = SkjultVerdi(fødselsnummer),
                arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                startSyketilfelle = LocalDate.now(),
                sendtNav = LocalDateTime.now(),
                egenmeldinger = emptyList(),
                fravar = emptyList(),
                soknadsperioder = listOf(SoknadsperiodeDTO(LocalDate.now(), LocalDate.now(), 100)),
                opprettet = LocalDateTime.now()
            )
            testRapid.sendTestMessage(sendtSøknad.toJsonNode().apply {
                this as ObjectNode
                put("@id", UUID.randomUUID().toString())
                put("@event_name", "sendt_søknad_nav")
                put("@opprettet", LocalDateTime.now().toString())
            }.toString())
        }

        private fun sendNySøknad(
            id: UUID = UUID.randomUUID(),
            aktørId: String = defaultAktørId,
            fødselsnummer: String = "fødselsnummer",
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ): SykepengesoknadDTO {
            val nySøknad = SykepengesoknadDTO(
                status = SoknadsstatusDTO.NY,
                id = id.toString(),
                sykmeldingId = UUID.randomUUID().toString(),
                aktorId = aktørId,
                fodselsnummer = SkjultVerdi(fødselsnummer),
                arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                startSyketilfelle = LocalDate.now(),
                sendtNav = null,
                egenmeldinger = emptyList(),
                fravar = emptyList(),
                soknadsperioder = listOf(
                    SoknadsperiodeDTO(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        sykmeldingsgrad = 100
                    )
                ),
                opprettet = LocalDateTime.now()
            )
            testRapid.sendTestMessage(nySøknad.toJsonNode().apply {
                this as ObjectNode
                put("@id", UUID.randomUUID().toString())
                put("@event_name", "ny_søknad")
                put("@opprettet", LocalDateTime.now().toString())
            }.toString())
            return nySøknad
        }
    }
}
