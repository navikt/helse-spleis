package no.nav.helse.spleis.mediator

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidsgiverDTO
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.FravarDTO
import no.nav.helse.flex.sykepengesoknad.kafka.InntektskildeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.MerknadDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SporsmalDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SvarDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.mediator.TestMessageFactory.UtbetalingshistorikkTestdata.Companion.toJson
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.AvsenderSystem
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.OpphoerAvNaturalytelse
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status

internal class TestMessageFactory(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val organisasjonsnummer: String,
    private val inntekt: Double,
    private val fødselsdato: LocalDate
) {

    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private fun SykepengesoknadDTO.toMapMedFelterFraSpedisjon(fødselsdato: LocalDate, aktørId: String, historiskeFolkeregisteridenter: List<String>): Map<String, Any> =
            objectMapper
                .convertValue<Map<String, Any>>(this)
                .plus("fødselsdato" to "$fødselsdato")
                .plus("aktorId" to aktørId)
                .plus("historiskeFolkeregisteridenter" to historiskeFolkeregisteridenter)
        private fun Inntektsmelding.toMapMedFelterFraSpedisjon(fødselsdato: LocalDate, aktørId: String): Map<String, Any> =
            objectMapper
                .convertValue<Map<String, Any>>(this)
                .plus("fødselsdato" to "$fødselsdato")
                .plus("aktorId" to aktørId)
    }

    fun lagNySøknad(
        vararg perioder: SoknadsperiodeDTO,
        opprettet: LocalDateTime = perioder.minOfOrNull { it.fom!! }!!.atStartOfDay(),
        orgnummer: String = organisasjonsnummer,
        historiskeFolkeregisteridenter: List<String> = emptyList(),
        fnr: String = fødselsnummer
    ): Pair<String, String> {
        val fom = perioder.minOfOrNull { it.fom!! }!!
        val nySøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.NY,
            id = UUID.randomUUID().toString(),
            sykmeldingId = UUID.randomUUID().toString(),
            fnr = fnr,
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnummer),
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            startSyketilfelle = LocalDate.now(),
            sendtNav = null,
            egenmeldinger = emptyList(),
            fravar = emptyList(),
            soknadsperioder = perioder.toList(),
            opprettet = opprettet,
            sykmeldingSkrevet = fom.atStartOfDay()
        )
        return nyHendelse("ny_søknad", nySøknad.toMapMedFelterFraSpedisjon(fødselsdato, aktørId, historiskeFolkeregisteridenter))
    }

    fun lagNySøknadFrilanser(
        vararg perioder: SoknadsperiodeDTO,
        opprettet: LocalDateTime = perioder.minOfOrNull { it.fom!! }!!.atStartOfDay(),
        historiskeFolkeregisteridenter: List<String> = emptyList(),
        fnr: String = fødselsnummer
    ): Pair<String, String> {
        val fom = perioder.minOfOrNull { it.fom!! }!!
        val nySøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.NY,
            id = UUID.randomUUID().toString(),
            sykmeldingId = UUID.randomUUID().toString(),
            fnr = fnr,
            arbeidsgiver = null,
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
            arbeidssituasjon = ArbeidssituasjonDTO.FRILANSER,
            startSyketilfelle = LocalDate.now(),
            sendtNav = null,
            egenmeldinger = null,
            fravar = null,
            soknadsperioder = perioder.toList(),
            opprettet = opprettet,
            sykmeldingSkrevet = fom.atStartOfDay()
        )
        return nyHendelse("ny_søknad_frilans", nySøknad.toMapMedFelterFraSpedisjon(fødselsdato, aktørId, historiskeFolkeregisteridenter))
    }

    fun lagNySøknadSelvstendig(
        vararg perioder: SoknadsperiodeDTO,
        opprettet: LocalDateTime = perioder.minOfOrNull { it.fom!! }!!.atStartOfDay(),
        historiskeFolkeregisteridenter: List<String> = emptyList(),
        fnr: String = fødselsnummer
    ): Pair<String, String> {
        val fom = perioder.minOfOrNull { it.fom!! }!!
        val nySøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.NY,
            id = UUID.randomUUID().toString(),
            sykmeldingId = UUID.randomUUID().toString(),
            fnr = fnr,
            arbeidsgiver = null,
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
            startSyketilfelle = LocalDate.now(),
            sendtNav = null,
            egenmeldinger = null,
            fravar = null,
            soknadsperioder = perioder.toList(),
            opprettet = opprettet,
            sykmeldingSkrevet = fom.atStartOfDay()
        )
        return nyHendelse("ny_søknad_selvstendig", nySøknad.toMapMedFelterFraSpedisjon(fødselsdato, aktørId, historiskeFolkeregisteridenter))
    }
    fun lagNySøknadArbeidsledig(
        vararg perioder: SoknadsperiodeDTO,
        opprettet: LocalDateTime = perioder.minOfOrNull { it.fom!! }!!.atStartOfDay(),
        historiskeFolkeregisteridenter: List<String> = emptyList(),
        fnr: String = fødselsnummer,
        tidligereArbeidsgiverOrgnummer: String? = null
    ): Pair<String, String> {
        val fom = perioder.minOfOrNull { it.fom!! }!!
        val nySøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.NY,
            id = UUID.randomUUID().toString(),
            sykmeldingId = UUID.randomUUID().toString(),
            fnr = fnr,
            arbeidsgiver = null,
            tidligereArbeidsgiverOrgnummer = tidligereArbeidsgiverOrgnummer,
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.ARBEIDSLEDIG,
            arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSLEDIG,
            startSyketilfelle = LocalDate.now(),
            sendtNav = null,
            egenmeldinger = null,
            fravar = null,
            soknadsperioder = perioder.toList(),
            opprettet = opprettet,
            sykmeldingSkrevet = fom.atStartOfDay()
        )
        return nyHendelse("ny_søknad_arbeidsledig", nySøknad.toMapMedFelterFraSpedisjon(fødselsdato, aktørId, historiskeFolkeregisteridenter))
    }

    fun lagSøknadArbeidsgiver(
        perioder: List<SoknadsperiodeDTO>,
        historiskeFolkeregisteridenter: List<String> = emptyList()
    ): Pair<String, String> {
        val fom = perioder.minOfOrNull { it.fom!! }!!
        val sendtSøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.SENDT,
            id = UUID.randomUUID().toString(),
            fnr = fødselsnummer,
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            startSyketilfelle = LocalDate.now(),
            sendtArbeidsgiver = perioder.maxOfOrNull { it.tom!! }?.atStartOfDay(),
            papirsykmeldinger = emptyList(),
            egenmeldinger = emptyList(),
            fravar = emptyList(),
            soknadsperioder = perioder.toList(),
            opprettet = LocalDateTime.now(),
            sykmeldingSkrevet = fom.atStartOfDay()
        )
        return nyHendelse("sendt_søknad_arbeidsgiver", sendtSøknad.toMapMedFelterFraSpedisjon(fødselsdato, aktørId, historiskeFolkeregisteridenter))
    }

    fun lagSøknadNav(
        fnr: String = fødselsnummer,
        perioder: List<SoknadsperiodeDTO>,
        fravær: List<FravarDTO> = emptyList(),
        andreInntektskilder: List<InntektskildeDTO>? = null,
        ikkeJobbetIDetSisteFraAnnetArbeidsforhold: Boolean = false,
        sendtNav: LocalDateTime? = perioder.maxOfOrNull { it.tom!! }?.atStartOfDay(),
        orgnummer: String = organisasjonsnummer,
        korrigerer: UUID? = null,
        opprinneligSendt: LocalDateTime? = null,
        historiskeFolkeregisteridenter: List<String> = emptyList(),
        sendTilGosys: Boolean? = false,
        egenmeldingerFraSykmelding: List<LocalDate> = emptyList()
    ): Pair<String, String> {
        val fom = perioder.minOfOrNull { it.fom!! }
        val sendtSøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.SENDT,
            id = UUID.randomUUID().toString(),
            fnr = fnr,
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnummer),
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            startSyketilfelle = LocalDate.now(),
            sendtNav = sendtNav,
            papirsykmeldinger = emptyList(),
            sporsmal = lagSpørsmål(ikkeJobbetIDetSisteFraAnnetArbeidsforhold),
            egenmeldinger = emptyList(),
            fravar = fravær,
            korrigerer = korrigerer?.toString(),
            opprinneligSendt = opprinneligSendt,
            andreInntektskilder = andreInntektskilder,
            soknadsperioder = perioder.toList(),
            opprettet = LocalDateTime.now(),
            sykmeldingSkrevet = fom!!.atStartOfDay(),
            merknaderFraSykmelding = listOf(
                MerknadDTO("EN_MERKNADSTYPE", null),
                MerknadDTO("EN_ANNEN_MERKNADSTYPE", "tekstlig begrunnelse")
            ),
            sendTilGosys = sendTilGosys,
            egenmeldingsdagerFraSykmelding = egenmeldingerFraSykmelding
        )
        return nyHendelse("sendt_søknad_nav", sendtSøknad.toMapMedFelterFraSpedisjon(fødselsdato, aktørId, historiskeFolkeregisteridenter))
    }

    private fun lagSpørsmål(ikkeJobbetIDetSisteFraAnnetArbeidsforhold: Boolean): List<SporsmalDTO>? {
        if (!ikkeJobbetIDetSisteFraAnnetArbeidsforhold) return null
        return listOf(
            SporsmalDTO(
                undersporsmal = listOf(
                    SporsmalDTO(
                        undersporsmal = listOf(
                            SporsmalDTO(
                                tag = "INNTEKTSKILDE_ANDRE_ARBEIDSFORHOLD_JOBBET_I_DET_SISTE",
                                svar = listOf(
                                    SvarDTO(verdi = "NEI")
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    fun lagSøknadFrilanser(
        fnr: String = fødselsnummer,
        perioder: List<SoknadsperiodeDTO>,
        andreInntektskilder: List<InntektskildeDTO>? = null,
        sendtNav: LocalDateTime? = perioder.maxOfOrNull { it.tom!! }?.atStartOfDay(),
        korrigerer: UUID? = null,
        opprinneligSendt: LocalDateTime? = null,
        historiskeFolkeregisteridenter: List<String> = emptyList(),
        sendTilGosys: Boolean? = false,
        egenmeldingerFraSykmelding: List<LocalDate> = emptyList()
    ): Pair<String, String> {
        val fom = perioder.minOfOrNull { it.fom!! }
        val sendtSøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.SENDT,
            id = UUID.randomUUID().toString(),
            fnr = fnr,
            arbeidsgiver = null,
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
            arbeidssituasjon = ArbeidssituasjonDTO.FRILANSER,
            startSyketilfelle = LocalDate.now(),
            sendtNav = sendtNav,
            sendtArbeidsgiver = null,
            papirsykmeldinger = null,
            egenmeldinger = null,
            fravar = null,
            korrigerer = korrigerer?.toString(),
            opprinneligSendt = opprinneligSendt,
            andreInntektskilder = andreInntektskilder,
            soknadsperioder = perioder.toList(),
            opprettet = LocalDateTime.now(),
            sykmeldingSkrevet = fom!!.atStartOfDay(),
            merknaderFraSykmelding = listOf(
                MerknadDTO("EN_MERKNADSTYPE", null),
                MerknadDTO("EN_ANNEN_MERKNADSTYPE", "tekstlig begrunnelse")
            ),
            sendTilGosys = sendTilGosys,
            egenmeldingsdagerFraSykmelding = egenmeldingerFraSykmelding
        )
        return nyHendelse("sendt_søknad_frilans", sendtSøknad.toMapMedFelterFraSpedisjon(fødselsdato, aktørId, historiskeFolkeregisteridenter))
    }

    fun lagSøknadSelvstendig(
        fnr: String = fødselsnummer,
        perioder: List<SoknadsperiodeDTO>,
        andreInntektskilder: List<InntektskildeDTO>? = null,
        sendtNav: LocalDateTime? = perioder.maxOfOrNull { it.tom!! }?.atStartOfDay(),
        korrigerer: UUID? = null,
        opprinneligSendt: LocalDateTime? = null,
        historiskeFolkeregisteridenter: List<String> = emptyList(),
        sendTilGosys: Boolean? = false,
        egenmeldingerFraSykmelding: List<LocalDate> = emptyList()
    ): Pair<String, String> {
        val fom = perioder.minOfOrNull { it.fom!! }
        val sendtSøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.SENDT,
            id = UUID.randomUUID().toString(),
            fnr = fnr,
            arbeidsgiver = null,
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
            startSyketilfelle = LocalDate.now(),
            sendtNav = sendtNav,
            sendtArbeidsgiver = null,
            papirsykmeldinger = null,
            egenmeldinger = null,
            fravar = null,
            korrigerer = korrigerer?.toString(),
            opprinneligSendt = opprinneligSendt,
            andreInntektskilder = andreInntektskilder,
            soknadsperioder = perioder.toList(),
            opprettet = LocalDateTime.now(),
            sykmeldingSkrevet = fom!!.atStartOfDay(),
            merknaderFraSykmelding = listOf(
                MerknadDTO("EN_MERKNADSTYPE", null),
                MerknadDTO("EN_ANNEN_MERKNADSTYPE", "tekstlig begrunnelse")
            ),
            sendTilGosys = sendTilGosys,
            egenmeldingsdagerFraSykmelding = egenmeldingerFraSykmelding
        )
        return nyHendelse("sendt_søknad_selvstendig", sendtSøknad.toMapMedFelterFraSpedisjon(fødselsdato, aktørId, historiskeFolkeregisteridenter))
    }

    fun lagSøknadArbeidsledig(
        fnr: String = fødselsnummer,
        perioder: List<SoknadsperiodeDTO>,
        tidligereArbeidsgiverOrgnummer: String? = null,
        andreInntektskilder: List<InntektskildeDTO>? = null,
        sendtNav: LocalDateTime? = perioder.maxOfOrNull { it.tom!! }?.atStartOfDay(),
        korrigerer: UUID? = null,
        opprinneligSendt: LocalDateTime? = null,
        historiskeFolkeregisteridenter: List<String> = emptyList(),
        sendTilGosys: Boolean? = false,
        egenmeldingerFraSykmelding: List<LocalDate> = emptyList()
    ): Pair<String, String> {
        val fom = perioder.minOfOrNull { it.fom!! }
        val sendtSøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.SENDT,
            id = UUID.randomUUID().toString(),
            fnr = fnr,
            tidligereArbeidsgiverOrgnummer = tidligereArbeidsgiverOrgnummer,
            arbeidsgiver = null,
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.ARBEIDSLEDIG,
            arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSLEDIG,
            startSyketilfelle = LocalDate.now(),
            sendtNav = sendtNav,
            sendtArbeidsgiver = null,
            papirsykmeldinger = null,
            egenmeldinger = null,
            fravar = null,
            korrigerer = korrigerer?.toString(),
            opprinneligSendt = opprinneligSendt,
            andreInntektskilder = andreInntektskilder,
            soknadsperioder = perioder.toList(),
            opprettet = LocalDateTime.now(),
            sykmeldingSkrevet = fom!!.atStartOfDay(),
            merknaderFraSykmelding = listOf(
                MerknadDTO("EN_MERKNADSTYPE", null),
                MerknadDTO("EN_ANNEN_MERKNADSTYPE", "tekstlig begrunnelse")
            ),
            sendTilGosys = sendTilGosys,
            egenmeldingsdagerFraSykmelding = egenmeldingerFraSykmelding
        )
        return nyHendelse("sendt_søknad_arbeidsledig", sendtSøknad.toMapMedFelterFraSpedisjon(fødselsdato, aktørId, historiskeFolkeregisteridenter))
    }

    private fun lagInntektsmelding(
        arbeidsgiverperiode: List<Periode>,
        førsteFraværsdag: LocalDate,
        opphørAvNaturalytelser: List<OpphoerAvNaturalytelse> = emptyList(),
        beregnetInntekt: Double = inntekt,
        orgnummer: String,
        opphørsdatoForRefusjon: LocalDate? = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        avsenderSystem: AvsenderSystem?
    ) = Inntektsmelding(
        inntektsmeldingId = UUID.randomUUID().toString(),
        arbeidstakerFnr = fødselsnummer,
        arbeidstakerAktorId = aktørId,
        virksomhetsnummer = orgnummer,
        arbeidsgiverFnr = null,
        arbeidsgiverAktorId = null,
        arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
        arbeidsforholdId = null,
        beregnetInntekt = beregnetInntekt.toBigDecimal(),
        refusjon = Refusjon(beregnetInntekt.toBigDecimal(), opphørsdatoForRefusjon),
        endringIRefusjoner = emptyList(),
        opphoerAvNaturalytelser = opphørAvNaturalytelser,
        gjenopptakelseNaturalytelser = emptyList(),
        arbeidsgiverperioder = arbeidsgiverperiode,
        status = Status.GYLDIG,
        arkivreferanse = "",
        ferieperioder = emptyList(),
        foersteFravaersdag = førsteFraværsdag,
        mottattDato = LocalDateTime.now(),
        begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
        naerRelasjon = null,
        avsenderSystem = avsenderSystem,
        innsenderTelefon = "12345678",
        innsenderFulltNavn = "SPLEIS MEDIATOR"
    )

    fun lagInnteksmelding(
        arbeidsgiverperiode: List<Periode>,
        førsteFraværsdag: LocalDate,
        opphørAvNaturalytelser: List<OpphoerAvNaturalytelse> = emptyList(),
        beregnetInntekt: Double = inntekt,
        opphørsdatoForRefusjon: LocalDate? = null,
        orgnummer: String = organisasjonsnummer,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        avsenderSystem: AvsenderSystem = AvsenderSystem("LPS", "V1.0")
    ) = nyHendelse(
        "inntektsmelding", lagInntektsmelding(
            arbeidsgiverperiode,
            førsteFraværsdag,
            opphørAvNaturalytelser,
            beregnetInntekt,
            orgnummer,
            opphørsdatoForRefusjon,
            begrunnelseForReduksjonEllerIkkeUtbetalt,
            avsenderSystem
        ).toMapMedFelterFraSpedisjon(fødselsdato, aktørId)
    )

    fun lagInnteksmeldingReplay(
        vedtaksperiodeId: UUID,
        inntektsmelding: String
    ) = objectMapper.readTree(inntektsmelding).also {
        (it as ObjectNode).put("@event_name", "inntektsmelding_replay")
        it.put("vedtaksperiodeId", "$vedtaksperiodeId")
    }.let { node ->
        UUID.fromString(node.path("@id").asText()) to node.toString()
    }

    fun lagUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        tilstand: TilstandType,
        sykepengehistorikk: List<UtbetalingshistorikkTestdata> = emptyList(),
        orgnummer: String? = null,
        besvart: LocalDateTime = LocalDateTime.now()
    ): Pair<String, String> {
        return lagBehovMedLøsning(
            vedtaksperiodeId = vedtaksperiodeId,
            tilstand = tilstand,
            behov = listOf("Sykepengehistorikk"),
            løsninger = sykepengehistorikk.toJson(),
            orgnummer = orgnummer ?: organisasjonsnummer,
            besvart = besvart
        )
    }

    fun lagUtbetalingshistorikkEtterInfotrygdendring(sykepengehistorikk: List<UtbetalingshistorikkTestdata> = emptyList()): Pair<String, String> {
        return nyHendelse(
            "behov", mutableMapOf(
                "@behov" to listOf("Sykepengehistorikk"),
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "@løsning" to sykepengehistorikk.toJson(),
                "@final" to true,
                "@besvart" to LocalDateTime.now()
            ))
    }

    fun lagUtbetalingshistorikkForFeriepenger(testdata: UtbetalingshistorikkForFeriepengerTestdata) =
        lagBehovMedLøsning(
            vedtaksperiodeId = null,
            tilstand = null,
            behov = listOf("SykepengehistorikkForFeriepenger"),
            ekstraFelter = mapOf(
                "SykepengehistorikkForFeriepenger" to mapOf(
                    "historikkFom" to testdata.fom.toString(),
                    "historikkTom" to testdata.tom.toString()
                )
            ),
            løsninger = mapOf(
                "SykepengehistorikkForFeriepenger" to mapOf(
                    "feriepengerSkalBeregnesManuelt" to testdata.feriepengerSkalBeregnesManuelt,
                    "utbetalinger" to testdata.utbetalinger.map {
                        mapOf(
                            "fom" to it.fom,
                            "tom" to it.tom,
                            "utbetalt" to it.utbetalt,
                            "dagsats" to it.dagsats,
                            "typeKode" to it.typekode,
                            "utbetalingsGrad" to it.utbetalingsgrad,
                            "orgnummer" to it.organisasjonsnummer
                        )
                    },
                    "feriepengehistorikk" to testdata.feriepengehistorikk.map {
                        mapOf(
                            "orgnummer" to it.orgnummer,
                            "beløp" to it.beløp,
                            "fom" to it.fom,
                            "tom" to it.tom
                        )
                    },
                    "arbeidskategorikoder" to testdata.arbeidskategorikoder.map {
                        mapOf(
                            "kode" to it.kode,
                            "fom" to it.fom,
                            "tom" to it.tom
                        )
                    }
                )
            )
        )

    class UtbetalingshistorikkForFeriepengerTestdata(
        val fom: LocalDate,
        val tom: LocalDate,
        val feriepengerSkalBeregnesManuelt: Boolean = false,
        val utbetalinger: List<Utbetaling> = emptyList(),
        val feriepengehistorikk: List<Feriepenger> = emptyList(),
        val arbeidskategorikoder: List<Arbeidskategori> = emptyList()
    ) {
        class Utbetaling(
            val fom: LocalDate,
            val tom: LocalDate,
            val utbetalt: LocalDate,
            val dagsats: Double,
            val typekode: String,
            val utbetalingsgrad: String,
            val organisasjonsnummer: String
        )

        class Feriepenger(
            val orgnummer: String,
            val beløp: Int,
            val fom: LocalDate,
            val tom: LocalDate
        )

        class Arbeidskategori(
            val kode: String,
            val fom: LocalDate,
            val tom: LocalDate
        )
    }

    class UtbetalingshistorikkTestdata(
        val fom: LocalDate,
        val tom: LocalDate,
        val arbeidskategorikode: String,
        val utbetalteSykeperioder: List<UtbetaltSykeperiode> = emptyList(),
        val inntektsopplysninger: List<Inntektsopplysninger> = emptyList(),
        val statslønn: Boolean = false
    ) {
        class UtbetaltSykeperiode(
            val fom: LocalDate,
            val tom: LocalDate,
            val dagsats: Double,
            val typekode: String,
            val utbetalingsgrad: String,
            val organisasjonsnummer: String
        )

        class Inntektsopplysninger(
            val sykepengerFom: LocalDate,
            val inntekt: Double,
            val organisasjonsnummer: String,
            val refusjonTilArbeidsgiver: Boolean,
            val refusjonTom: LocalDate? = null
        )

        private fun toJson() = mapOf(
            "statslønn" to statslønn,
            "inntektsopplysninger" to inntektsopplysninger.map {
                mapOf(
                    "sykepengerFom" to it.sykepengerFom,
                    "inntekt" to it.inntekt,
                    "orgnummer" to it.organisasjonsnummer,
                    "refusjonTilArbeidsgiver" to it.refusjonTilArbeidsgiver,
                    "refusjonTom" to it.refusjonTom
                )
            },
            "utbetalteSykeperioder" to utbetalteSykeperioder.map {
                mapOf(
                    "fom" to it.fom,
                    "tom" to it.tom,
                    "dagsats" to it.dagsats,
                    "utbetalingsGrad" to it.utbetalingsgrad,
                    "orgnummer" to it.organisasjonsnummer,
                    "typeKode" to it.typekode
                )
            },
            "arbeidsKategoriKode" to arbeidskategorikode
        )

        companion object {
            fun List<UtbetalingshistorikkTestdata>.toJson() = mapOf(
                "Sykepengehistorikk" to map { data ->
                    data.toJson()
                }
            )
        }
    }

    class PleiepengerTestdata(
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int
    )

    class OmsorgspengerTestdata(
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int
    )

    class OpplæringspengerTestdata(
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int
    )

    class InstitusjonsoppholdTestdata(
        val startdato: LocalDate,
        val faktiskSluttdato: LocalDate?,
        val institusjonstype: String,
        val kategori: String
    )

    class ArbeidsavklaringspengerTestdata(
        val fom: LocalDate,
        val tom: LocalDate
    )

    class DagpengerTestdata(
        val fom: LocalDate,
        val tom: LocalDate
    )

    data class Arbeidsforhold(
        val orgnummer: String,
        val ansattSiden: LocalDate,
        val ansattTil: LocalDate?,
        val type: Arbeidsforholdtype
    ) {
        enum class Arbeidsforholdtype {
            FORENKLET_OPPGJØRSORDNING,
            FRILANSER,
            MARITIMT,
            ORDINÆRT
        }
    }

    data class ArbeidsforholdOverstyrt(
        val orgnummer: String,
        val deaktivert: Boolean,
        val forklaring: String?
    )

    data class InntekterForSykepengegrunnlagFraLøsning(
        val måned: YearMonth,
        val inntekter: List<Inntekt>,
        val arbeidsforhold: List<Arbeidsforhold>
    ) {
        data class Inntekt(
            val beløp: Double,
            val orgnummer: String
        )

        data class Arbeidsforhold(
            val orgnummer: String,
            val type: String = "frilanserOppdragstakerHonorarPersonerMm"
        )
    }

    data class InntekterForOpptjeningsvurderingFraLøsning(
        val måned: YearMonth,
        val inntekter: List<Inntekt>
    ) {
        data class Inntekt(
            val beløp: Double,
            val orgnummer: String
        )
    }

    class Subsumsjon(
        paragraf: String?,
        ledd: String?,
        bokstav: String?
    ) {
        val toMap = mutableMapOf(
            "paragraf" to paragraf,
        ).apply {
            ledd?.let {
                this["ledd"] = ledd
            }
            bokstav?.let {
                this["bokstav"] = bokstav
            }
        }.toMap()
    }

    class Refusjonsopplysning(
        fom: LocalDate,
        tom: LocalDate?,
        beløp: Double
    ) {
        val toMap = mutableMapOf(
            "fom" to fom,
            "beløp" to beløp
        ).apply {
            tom?.let {
                this["tom"] = it
            }
        }.toMap()
    }

    data class Arbeidsgiveropplysning(
        val organisasjonsnummer: String,
        val månedligInntekt: Double?,
        val forklaring: String? = null,
        val subsumsjon: Subsumsjon? = null,
        val refusjonsopplysninger: List<Refusjonsopplysning>? = null
    )

    data class SkjønnsmessigFastsatt(
        val organisasjonsnummer: String,
        val årlig: Double?,
    )

    fun lagYtelser(
        vedtaksperiodeId: UUID,
        tilstand: TilstandType,
        pleiepenger: List<PleiepengerTestdata> = emptyList(),
        omsorgspenger: List<OmsorgspengerTestdata> = emptyList(),
        opplæringspenger: List<OpplæringspengerTestdata> = emptyList(),
        institusjonsoppholdsperioder: List<InstitusjonsoppholdTestdata> = emptyList(),
        arbeidsavklaringspenger: List<ArbeidsavklaringspengerTestdata> = emptyList(),
        dagpenger: List<DagpengerTestdata> = emptyList(),
        orgnummer: String = organisasjonsnummer
    ): Pair<String, String> {
        val behovliste = mutableListOf(
            "Foreldrepenger",
            "Pleiepenger",
            "Omsorgspenger",
            "Opplæringspenger",
            "Institusjonsopphold",
            "Arbeidsavklaringspenger",
            "Dagpenger"
        )
        return lagBehovMedLøsning(
            vedtaksperiodeId = vedtaksperiodeId,
            tilstand = tilstand,
            orgnummer = orgnummer,
            behov = behovliste,
            løsninger = mapOf(
                "Foreldrepenger" to emptyMap<String, String>(),
                "Pleiepenger" to pleiepenger.map { data ->
                    mapOf(
                        "fom" to data.fom,
                        "tom" to data.tom,
                        "grad" to data.grad
                    )
                },
                "Omsorgspenger" to omsorgspenger.map { data ->
                    mapOf(
                        "fom" to data.fom,
                        "tom" to data.tom,
                        "grad" to data.grad
                    )
                },
                "Opplæringspenger" to opplæringspenger.map { data ->
                    mapOf(
                        "fom" to data.fom,
                        "tom" to data.tom,
                        "grad" to data.grad
                    )
                },
                "Institusjonsopphold" to institusjonsoppholdsperioder.map { data ->
                    mapOf(
                        "startdato" to data.startdato,
                        "faktiskSluttdato" to data.faktiskSluttdato,
                        "institusjonstype" to data.institusjonstype,
                        "kategori" to data.kategori
                    )
                },
                Arbeidsavklaringspenger.name to mapOf(
                    "meldekortperioder" to arbeidsavklaringspenger.map { data ->
                        mapOf(
                            "fom" to data.fom,
                            "tom" to data.tom
                        )
                    }
                ),
                Dagpenger.name to mapOf(
                    "meldekortperioder" to dagpenger.map { data ->
                        mapOf(
                            "fom" to data.fom,
                            "tom" to data.tom
                        )
                    }
                )
            )
        )
    }

    fun lagVilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        skjæringstidspunkt: LocalDate,
        tilstand: TilstandType,
        inntekterForSykepengegrunnlag: List<InntekterForSykepengegrunnlagFraLøsning>,
        inntekterForOpptjeningsvurdering: List<InntekterForOpptjeningsvurderingFraLøsning>,
        arbeidsforhold: List<Arbeidsforhold>,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        orgnummer: String = organisasjonsnummer
    ): Pair<String, String> {
        return lagBehovMedLøsning(
            behov = listOf(
                Medlemskap.name,
                InntekterForSykepengegrunnlag.name,
                InntekterForOpptjeningsvurdering.name,
                ArbeidsforholdV2.name
            ),
            vedtaksperiodeId = vedtaksperiodeId,
            orgnummer = orgnummer,
            tilstand = tilstand,
            løsninger = mapOf(
                Medlemskap.name to mapOf<String, Any>(
                    "resultat" to mapOf<String, Any>(
                        "svar" to when (medlemskapstatus) {
                            Medlemskapsvurdering.Medlemskapstatus.Ja -> "JA"
                            Medlemskapsvurdering.Medlemskapstatus.Nei -> "NEI"
                            Medlemskapsvurdering.Medlemskapstatus.UavklartMedBrukerspørsmål -> "UAVKLART_MED_BRUKERSPORSMAAL"
                            else -> "UAVKLART"
                        }
                    )
                ),
                InntekterForSykepengegrunnlag.name to inntekterForSykepengegrunnlag
                    .map {
                        mapOf(
                            "årMåned" to it.måned,
                            "inntektsliste" to it.inntekter.map { inntekt ->
                                mapOf(
                                    "beløp" to inntekt.beløp,
                                    "inntektstype" to "LOENNSINNTEKT",
                                    "orgnummer" to inntekt.orgnummer,
                                    "fordel" to "kontantytelse",
                                    "beskrivelse" to "fastloenn"
                                )
                            },
                            "arbeidsforholdliste" to it.arbeidsforhold.map { arbeidsforhold ->
                                mapOf(
                                    "orgnummer" to arbeidsforhold.orgnummer,
                                    "type" to arbeidsforhold.type
                                )
                            }
                        )
                    },
                InntekterForOpptjeningsvurdering.name to inntekterForOpptjeningsvurdering
                    .map {
                        mapOf(
                            "årMåned" to it.måned,
                            "inntektsliste" to it.inntekter.map { inntekt ->
                                mapOf(
                                    "beløp" to inntekt.beløp,
                                    "inntektstype" to "LOENNSINNTEKT",
                                    "orgnummer" to inntekt.orgnummer,
                                    "fordel" to "kontantytelse",
                                    "beskrivelse" to "fastloenn"
                                )
                            }
                        )
                    },
                ArbeidsforholdV2.name to arbeidsforhold.map {
                    mapOf(
                        "orgnummer" to it.orgnummer,
                        "ansattSiden" to it.ansattSiden,
                        "ansattTil" to it.ansattTil,
                        "type" to it.type
                    )
                }
            ),
            ekstraFelter = mapOf(
                InntekterForSykepengegrunnlag.name to mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt
                ),
                InntekterForOpptjeningsvurdering.name to mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt
                ),
                ArbeidsforholdV2.name to mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt
                ),
                Medlemskap.name to mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt
                ),
            )
        )
    }

    fun lagSimulering(
        vedtaksperiodeId: UUID,
        tilstand: TilstandType,
        status: SimuleringMessage.Simuleringstatus,
        utbetalingId: UUID,
        fagsystemId: String = "fagsystemid",
        fagområde: String = "SPREF",
        orgnummer: String = organisasjonsnummer
    ): Pair<String, String> {
        return lagBehovMedLøsning(
            behov = listOf("Simulering"),
            vedtaksperiodeId = vedtaksperiodeId,
            orgnummer = orgnummer,
            tilstand = tilstand,
            løsninger = mapOf(
                "Simulering" to mapOf(
                    "fagsystemId" to fagsystemId,
                    "fagområde" to fagområde,
                    "status" to status.name,
                    "feilmelding" to if (status == SimuleringMessage.Simuleringstatus.OK) null else "FEIL I SIMULERING",
                    "simulering" to if (status != SimuleringMessage.Simuleringstatus.OK) null else mapOf(
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
                                        "utbetalesTilId" to orgnummer,
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
                                                "sats" to 1000.5,
                                                "typeSats" to "DAG",
                                                "antallSats" to 9,
                                                "uforegrad" to 100,
                                                "klassekode" to "SPREFAG-IOP",
                                                "klassekodeBeskrivelse" to "Sykepenger, Refusjon arbeidsgiver",
                                                "utbetalingsType" to "YTEL",
                                                "refunderesOrgNr" to orgnummer
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            ekstraFelter = mapOf(
                "utbetalingId" to "$utbetalingId",
                "Simulering" to mapOf(
                    "fagsystemId" to fagsystemId,
                    "fagområde" to fagområde
                )
            )
        )
    }

    fun lagEtterbetaling(
        fagsystemId: String,
        gyldighetsdato: LocalDate
    ): Pair<String, String> {
        return nyHendelse(
            navn = "Etterbetalingskandidat_v1",
            hendelse = mapOf(
                "fagsystemId" to fagsystemId,
                "gyldighetsdato" to gyldighetsdato,
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
            ),
        )
    }

    fun lagEtterbetalingMedHistorikk(
        fagsystemId: String,
        gyldighetsdato: LocalDate
    ): Pair<String, String> {
        return lagBehovMedLøsning(
            behov = listOf("Sykepengehistorikk"),
            løsninger = mapOf(
                "Sykepengehistorikk" to emptyList<Any>()
            ),
            ekstraFelter = mapOf(
                "fagsystemId" to fagsystemId,
                "gyldighetsdato" to gyldighetsdato,
            ),
            vedtaksperiodeId = null,
            tilstand = null,
        )
    }

    fun lagUtbetalingsgodkjenning(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        tilstand: TilstandType,
        utbetalingGodkjent: Boolean,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        automatiskBehandling: Boolean,
        makstidOppnådd: Boolean,
        godkjenttidspunkt: LocalDateTime,
        orgnummer: String = organisasjonsnummer
    ): Pair<String, String> {
        return lagBehovMedLøsning(
            behov = listOf("Godkjenning"),
            orgnummer = orgnummer,
            tilstand = tilstand,
            vedtaksperiodeId = vedtaksperiodeId,
            løsninger = mapOf(
                "Godkjenning" to mapOf(
                    "godkjent" to utbetalingGodkjent,
                    "saksbehandlerIdent" to saksbehandlerIdent,
                    "saksbehandlerEpost" to saksbehandlerEpost,
                    "automatiskBehandling" to automatiskBehandling,
                    "godkjenttidspunkt" to godkjenttidspunkt,
                    "makstidOppnådd" to makstidOppnådd
                )
            ),
            ekstraFelter = mapOf(
                "utbetalingId" to utbetalingId
            )
        )
    }

    fun lagUtbetalingpåminnelse(utbetalingId: UUID, status: Utbetalingstatus): Pair<String, String> {
        return nyHendelse(
            "utbetalingpåminnelse", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "utbetalingId" to utbetalingId,
                "status" to status.name,
                "antallGangerPåminnet" to 0,
                "endringstidspunkt" to LocalDateTime.now(),
                "påminnelsestidspunkt" to LocalDateTime.now()
            )
        )
    }

    fun lagPåminnelse(
        vedtaksperiodeId: UUID,
        tilstand: TilstandType,
        orgnummer: String = organisasjonsnummer,
        tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()
    ): Pair<String, String> {
        return nyHendelse(
            "påminnelse", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to orgnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "tilstand" to tilstand.name,
                "antallGangerPåminnet" to 0,
                "tilstandsendringstidspunkt" to tilstandsendringstidspunkt,
                "påminnelsestidspunkt" to LocalDateTime.now(),
                "nestePåminnelsestidspunkt" to LocalDateTime.now()
            )
        )
    }

    fun lagForkastSykmeldingsperioder(
        orgnummer: String = organisasjonsnummer,
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar
    ): Pair<String, String> {
        return nyHendelse(
            "forkast_sykmeldingsperioder", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to orgnummer,
                "fom" to fom.toString(),
                "tom" to tom.toString()
            )
        )
    }


    fun lagDødsmelding(dødsdato: LocalDate): Pair<String, String> {
        return nyHendelse(
            "dødsmelding", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "dødsdato" to "$dødsdato"
            )
        )
    }

    fun lagPersonPåminnelse(): Pair<String, String> {
        return nyHendelse(
            "person_påminnelse", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer
            )
        )
    }

    fun lagAnmodningOmForkasting(vedtaksperiodeId: UUID = UUID.randomUUID()): Pair<String, String> {
        return nyHendelse(
            "anmodning_om_forkasting", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId
            )
        )
    }

    fun lagUtbetaling(
        fagsystemId: String,
        utbetalingId: String,
        utbetalingOK: Boolean = true,
        avstemmingsnøkkel: Long = 123456L,
        overføringstidspunkt: LocalDateTime = LocalDateTime.now()
    ): Pair<String, String> {
        return lagBehovMedLøsning(
            behov = listOf("Utbetaling"),
            tilstand = null,
            vedtaksperiodeId = null,
            løsninger = mapOf(
                "Utbetaling" to mapOf(
                    "status" to if (utbetalingOK) Oppdragstatus.AKSEPTERT.name else Oppdragstatus.AVVIST.name,
                    "beskrivelse" to if (!utbetalingOK) "FEIL fra Spenn" else "",
                    "avstemmingsnøkkel" to avstemmingsnøkkel,
                    "overføringstidspunkt" to overføringstidspunkt
                )
            ),
            ekstraFelter = mapOf(
                "Utbetaling" to mapOf("fagsystemId" to fagsystemId),
                "utbetalingId" to utbetalingId
            )
        )
    }

    fun lagAnnullering(fagsystemId: String): Pair<String, String> {
        return nyHendelse(
            "annullering", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "fagsystemId" to fagsystemId,
                "saksbehandler" to mapOf(
                    "navn" to "Siri Saksbhandler",
                    "epostaddresse" to "siri.saksbehandler@nav.no",
                    "oid" to "${UUID.randomUUID()}",
                    "ident" to "S1234567",
                )
            )
        )
    }

    fun lagAvstemming() = nyHendelse(
        "person_avstemming", mapOf(
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId
        )
    )

    fun lagMigrate() = nyHendelse(
        "json_migrate", mapOf(
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId
        )
    )

    fun lagOverstyringTidslinje(dager: List<ManuellOverskrivingDag>): Pair<String, String> {
        return nyHendelse(
            "overstyr_tidslinje", mutableMapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "dager" to dager.map {
                    mapOf(
                        "dato" to it.dato,
                        "type" to it.type,
                        "grad" to it.grad
                    )
                }
            ))
    }


    fun lagOverstyrArbeidsforhold(
        skjæringstidspunkt: LocalDate,
        overstyrteArbeidsforhold: List<ArbeidsforholdOverstyrt>
    ): Pair<String, String> {
        return nyHendelse(
            "overstyr_arbeidsforhold", mutableMapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "skjæringstidspunkt" to skjæringstidspunkt,
                "overstyrteArbeidsforhold" to overstyrteArbeidsforhold.map {
                    mutableMapOf<String, Any>(
                        "orgnummer" to it.orgnummer,
                        "deaktivert" to it.deaktivert
                    ).apply {
                        it.forklaring?.let { forklaring ->
                            this["forklaring"] = forklaring
                        }
                    }
                })
        )
    }

    fun lagOverstyrArbeidsgiveropplysninger(
        skjæringstidspunkt: LocalDate,
        arbeidsgiveropplysninger: List<Arbeidsgiveropplysning>
    ) = nyHendelse(
        "overstyr_inntekt_og_refusjon", mutableMapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "arbeidsgivere" to arbeidsgiveropplysninger.map { arbeidgiver ->
                mutableMapOf(
                    "organisasjonsnummer" to arbeidgiver.organisasjonsnummer,
                    "månedligInntekt" to arbeidgiver.månedligInntekt,
                    "forklaring" to arbeidgiver.forklaring,
                    "refusjonsopplysninger" to arbeidgiver.refusjonsopplysninger?.map { it.toMap }
                ).apply {
                    arbeidgiver.subsumsjon?.let {
                        this["subsumsjon"] = it.toMap
                    }
                }
            }
        )
    )

    fun lagMinimumSykdomsgradVurdert(
        perioderMedMinimumSykdomsgradVurdertOK: List<Pair<LocalDate, LocalDate>>,
        perioderMedMinimumSykdomsgradVurdertIkkeOK: List<Pair<LocalDate, LocalDate>>
    ) = nyHendelse(
        "minimum_sykdomsgrad_vurdert", mutableMapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "perioderMedMinimumSykdomsgradVurdertOk" to perioderMedMinimumSykdomsgradVurdertOK.map {
                mutableMapOf(
                    "fom" to it.first,
                    "tom" to it.second
                )
            },
            "perioderMedMinimumSykdomsgradVurdertIkkeOk" to perioderMedMinimumSykdomsgradVurdertIkkeOK.map {
                mutableMapOf(
                    "fom" to it.first,
                    "tom" to it.second
                )
            }
        )
    )

    fun lagSkjønnsmessigFastsettelse(
        skjæringstidspunkt: LocalDate,
        skjønnsmessigFastsatt: List<SkjønnsmessigFastsatt>
    ) = nyHendelse(
        "skjønnsmessig_fastsettelse", mutableMapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "arbeidsgivere" to skjønnsmessigFastsatt.map { arbeidgiver ->
                mutableMapOf(
                    "organisasjonsnummer" to arbeidgiver.organisasjonsnummer,
                    "årlig" to arbeidgiver.årlig
                )
            }
        )
    )

    internal fun lagInfotrygdendringer(endringsmeldingId: String = "1234567") = nyHendelse(
        "infotrygdendring", mutableMapOf(
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "endringsmeldingId" to endringsmeldingId
        )
    )


    private fun nyHendelse(navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(navn, hendelse).let { it.id to it.toJson() }

    private fun lagBehovMedLøsning(
        behov: List<String> = listOf(),
        vedtaksperiodeId: UUID? = UUID.randomUUID(),
        orgnummer: String = organisasjonsnummer,
        tilstand: TilstandType?,
        løsninger: Map<String, Any> = emptyMap(),
        ekstraFelter: Map<String, Any> = emptyMap(),
        besvart: LocalDateTime = LocalDateTime.now()
    ) = nyHendelse(
        "behov", ekstraFelter + mutableMapOf(
            "@behov" to behov,
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to orgnummer,
            "@løsning" to løsninger,
            "@final" to true,
            "@besvart" to besvart
        ).apply {
            tilstand?.let { this["tilstand"] = it.name }
            vedtaksperiodeId?.let { this["vedtaksperiodeId"] = vedtaksperiodeId.toString() }
        }
    )

    fun lagIdentOpphørt(fnr: String, nyttFnr: String) = nyHendelse("ident_opphørt", mapOf(
        "fødselsnummer" to fnr,
        "aktørId" to aktørId,
        "nye_identer" to mapOf(
            "fødselsnummer" to nyttFnr,
            "aktørId" to aktørId,
            "npid" to null
        ),
        "gamle_identer" to listOf(
            mapOf("ident" to fnr, "type" to "FØDSELSNUMMER")
        )
    ))
}
