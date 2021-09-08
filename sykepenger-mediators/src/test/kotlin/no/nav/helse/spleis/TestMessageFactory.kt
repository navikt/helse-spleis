package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.*
import no.nav.syfo.kafka.felles.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class TestMessageFactory(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val organisasjonsnummer: String,
    private val inntekt: Double
) {

    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private fun SykepengesoknadDTO.toMap(): Map<String, Any> = objectMapper.convertValue(this)
        private fun Inntektsmelding.toMap(): Map<String, Any> = objectMapper.convertValue(this)
    }

    fun lagNySøknad(vararg perioder: SoknadsperiodeDTO, orgnummer: String = organisasjonsnummer): String {
        val fom = perioder.minOfOrNull { it.fom!! }!!
        val nySøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.NY,
            id = UUID.randomUUID().toString(),
            sykmeldingId = UUID.randomUUID().toString(),
            aktorId = aktørId,
            fodselsnummer = SkjultVerdi(fødselsnummer),
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnummer),
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            startSyketilfelle = LocalDate.now(),
            sendtNav = null,
            egenmeldinger = emptyList(),
            fravar = emptyList(),
            soknadsperioder = perioder.toList(),
            opprettet = fom.plusMonths(3)?.atStartOfDay(),
            sykmeldingSkrevet = fom.atStartOfDay()
        )
        return nyHendelse("ny_søknad", mapOf("@opprettet" to fom.atStartOfDay()) + nySøknad.toMap())
    }

    fun lagSøknadArbeidsgiver(
        perioder: List<SoknadsperiodeDTO>,
        egenmeldinger: List<PeriodeDTO> = emptyList()
    ): String {
        val fom = perioder.minOfOrNull { it.fom!! }!!
        val sendtSøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.SENDT,
            id = UUID.randomUUID().toString(),
            aktorId = aktørId,
            fodselsnummer = SkjultVerdi(fødselsnummer),
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            startSyketilfelle = LocalDate.now(),
            sendtArbeidsgiver = perioder.maxOfOrNull { it.tom!! }?.atStartOfDay(),
            egenmeldinger = egenmeldinger,
            fravar = emptyList(),
            soknadsperioder = perioder.toList(),
            opprettet = LocalDateTime.now(),
            sykmeldingSkrevet = fom.atStartOfDay()
        )
        return nyHendelse("sendt_søknad_arbeidsgiver", sendtSøknad.toMap())
    }

    fun lagSøknadNav(
        perioder: List<SoknadsperiodeDTO>,
        orgnummer: String = organisasjonsnummer,
        fravær: List<FravarDTO> = emptyList(),
        egenmeldinger: List<PeriodeDTO> = emptyList(),
        andreInntektskilder: List<InntektskildeDTO>? = null
    ): String {
        val fom = perioder.minOfOrNull { it.fom!! }
        val sendtSøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.SENDT,
            id = UUID.randomUUID().toString(),
            aktorId = aktørId,
            fodselsnummer = SkjultVerdi(fødselsnummer),
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = orgnummer),
            fom = fom,
            tom = perioder.maxOfOrNull { it.tom!! },
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            startSyketilfelle = LocalDate.now(),
            sendtNav = perioder.maxOfOrNull { it.tom!! }?.atStartOfDay(),
            papirsykmeldinger = emptyList(),
            egenmeldinger = egenmeldinger,
            fravar = fravær,
            andreInntektskilder = andreInntektskilder,
            soknadsperioder = perioder.toList(),
            opprettet = LocalDateTime.now(),
            sykmeldingSkrevet = fom!!.atStartOfDay()
        )
        return nyHendelse("sendt_søknad_nav", sendtSøknad.toMap())
    }

    fun lagInnteksmelding(
        arbeidsgiverperiode: List<Periode>,
        førsteFraværsdag: LocalDate,
        opphørAvNaturalytelser: List<OpphoerAvNaturalytelse> = emptyList(),
        beregnetInntekt: Double = inntekt,
        opphørsdatoForRefusjon: LocalDate? = null
    ): String {
        val inntektsmelding = Inntektsmelding(
            inntektsmeldingId = UUID.randomUUID().toString(),
            arbeidstakerFnr = fødselsnummer,
            arbeidstakerAktorId = aktørId,
            virksomhetsnummer = organisasjonsnummer,
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
            mottattDato = LocalDateTime.now()
        )
        return nyHendelse("inntektsmelding", inntektsmelding.toMap())
    }

    fun lagUtbetalingshistorikk(vedtaksperiodeId: UUID, tilstand: TilstandType): String {
        return lagBehovMedLøsning(
            vedtaksperiodeId = vedtaksperiodeId,
            tilstand = tilstand,
            behov = listOf("Sykepengehistorikk"),
            løsninger = mapOf(
                "Sykepengehistorikk" to emptyList<Any>()
            )
        )
    }

    fun lagUtbetalingshistorikkForFeriepenger(testdata: UtbetalingshistorikkForFeriepengerTestdata) = lagBehovMedLøsning(
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
        val ansattTil: LocalDate?
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
        sykepengehistorikk: List<UtbetalingshistorikkTestdata>? = emptyList()
    ): String {
        val behovliste = mutableListOf(
            "Foreldrepenger",
            "Pleiepenger",
            "Omsorgspenger",
            "Opplæringspenger",
            "Institusjonsopphold",
            "Arbeidsavklaringspenger",
            "Dødsinfo",
            "Dagpenger"
        )

        val sykepengehistorikkMap = sykepengehistorikk?.let {
            behovliste.add("Sykepengehistorikk")
            mapOf(
                "Sykepengehistorikk" to sykepengehistorikk.map { data ->
                    mapOf(
                        "statslønn" to false,
                        "inntektsopplysninger" to data.inntektsopplysninger.map {
                            mapOf(
                                "sykepengerFom" to it.sykepengerFom,
                                "inntekt" to it.inntekt,
                                "orgnummer" to it.organisasjonsnummer,
                                "refusjonTilArbeidsgiver" to it.refusjonTilArbeidsgiver,
                                "refusjonTom" to it.refusjonTom
                            )
                        },
                        "utbetalteSykeperioder" to data.utbetalteSykeperioder.map {
                            mapOf(
                                "fom" to it.fom,
                                "tom" to it.tom,
                                "dagsats" to it.dagsats,
                                "utbetalingsGrad" to it.utbetalingsgrad,
                                "orgnummer" to it.organisasjonsnummer,
                                "typeKode" to it.typekode
                            )
                        },
                        "arbeidsKategoriKode" to data.arbeidskategorikode
                    )
                }
            )
        } ?: emptyMap()

        return lagBehovMedLøsning(
            vedtaksperiodeId = vedtaksperiodeId,
            tilstand = tilstand,
            behov = behovliste,
            løsninger = sykepengehistorikkMap + mapOf(
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
                "Dødsinfo" to mapOf(
                    "dødsdato" to null
                ),
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
        tilstand: TilstandType,
        inntekter: List<Pair<YearMonth, Double>>,
        inntekterForSykepengegrunnlag: List<Pair<YearMonth, Double>>,
        arbeidsforhold: List<Arbeidsforhold>,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,

    ): String {
        return lagBehovMedLøsning(
            behov = listOf(
                InntekterForSammenligningsgrunnlag.name,
                Arbeidsavklaringspenger.name,
                Medlemskap.name,
                InntekterForSykepengegrunnlag.name,
                ArbeidsforholdV2.name
            ),
            vedtaksperiodeId = vedtaksperiodeId,
            tilstand = tilstand,
            løsninger = mapOf(
                InntekterForSammenligningsgrunnlag.name to inntekter
                    .groupBy { it.first }
                    .map {
                        mapOf(
                            "årMåned" to it.key,
                            "inntektsliste" to it.value.map {
                                mapOf(
                                    "beløp" to it.second,
                                    "inntektstype" to "LOENNSINNTEKT",
                                    "orgnummer" to organisasjonsnummer,
                                    "fordel" to "kontantytelse",
                                    "beskrivelse" to "fastloenn"
                                )
                            }
                        )
                    },
                Medlemskap.name to mapOf<String, Any>(
                    "resultat" to mapOf<String, Any>(
                        "svar" to when (medlemskapstatus) {
                            Medlemskapsvurdering.Medlemskapstatus.Ja -> "JA"
                            Medlemskapsvurdering.Medlemskapstatus.Nei -> "NEI"
                            else -> "UAVKLART"
                        }
                    )
                ),
                InntekterForSykepengegrunnlag.name to inntekterForSykepengegrunnlag
                    .groupBy { it.first }
                    .map {
                        mapOf(
                            "årMåned" to it.key,
                            "inntektsliste" to it.value.map {
                                mapOf(
                                    "beløp" to it.second,
                                    "inntektstype" to "LOENNSINNTEKT",
                                    "orgnummer" to organisasjonsnummer,
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
                        "ansattTil" to it.ansattTil
                    )
                }
            )
        )
    }

    fun lagSimulering(
        vedtaksperiodeId: UUID,
        tilstand: TilstandType,
        status: SimuleringMessage.Simuleringstatus
    ): String {
        return lagBehovMedLøsning(
            behov = listOf("Simulering"),
            vedtaksperiodeId = vedtaksperiodeId,
            tilstand = tilstand,
            løsninger = mapOf(
                "Simulering" to mapOf(
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
                                                "typeSats" to "DAG",
                                                "antallSats" to 9,
                                                "uforegrad" to 100,
                                                "klassekode" to "SPREFAG-IOP",
                                                "klassekodeBeskrivelse" to "Sykepenger, Refusjon arbeidsgiver",
                                                "utbetalingsType" to "YTEL",
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

    fun lagEtterbetaling(
        fagsystemId: String,
        gyldighetsdato: LocalDate
    ): String {
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
    ): String {
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
        makstidOppnådd: Boolean
    ): String {
        return lagBehovMedLøsning(
            behov = listOf("Godkjenning"),
            tilstand = tilstand,
            vedtaksperiodeId = vedtaksperiodeId,
            løsninger = mapOf(
                "Godkjenning" to mapOf(
                    "godkjent" to utbetalingGodkjent,
                    "saksbehandlerIdent" to saksbehandlerIdent,
                    "saksbehandlerEpost" to saksbehandlerEpost,
                    "automatiskBehandling" to automatiskBehandling,
                    "godkjenttidspunkt" to LocalDateTime.now(),
                    "makstidOppnådd" to makstidOppnådd
                )
            ),
            ekstraFelter = mapOf(
                "utbetalingId" to utbetalingId
            )
        )
    }

    fun lagUtbetalingpåminnelse(utbetalingId: UUID, status: Utbetalingstatus): String {
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

    fun lagPåminnelse(vedtaksperiodeId: UUID, tilstand: TilstandType): String {
        return nyHendelse(
            "påminnelse", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "tilstand" to tilstand.name,
                "antallGangerPåminnet" to 0,
                "tilstandsendringstidspunkt" to LocalDateTime.now(),
                "påminnelsestidspunkt" to LocalDateTime.now(),
                "nestePåminnelsestidspunkt" to LocalDateTime.now()
            )
        )
    }

    fun lagPersonPåminnelse(): String {
        return nyHendelse(
            "person_påminnelse", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer
            )
        )
    }

    fun lagUtbetaling(
        fagsystemId: String,
        utbetalingId: String,
        utbetalingOK: Boolean = true,
        avstemmingsnøkkel: Long = 123456L,
        overføringstidspunkt: LocalDateTime = LocalDateTime.now()
    ): String {
        return lagBehovMedLøsning(
            behov = listOf("Utbetaling"),
            tilstand = null,
            vedtaksperiodeId = null,
            løsninger = mapOf(
                "Utbetaling" to mapOf(
                    "status" to if (utbetalingOK) UtbetalingHendelse.Oppdragstatus.AKSEPTERT.name else UtbetalingHendelse.Oppdragstatus.AVVIST.name,
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

    fun lagUtbetalingOverført(
        fagsystemId: String,
        utbetalingId: String,
        avstemmingsnøkkel: Long,
        overføringstidspunkt: LocalDateTime = LocalDateTime.now()
    ): String {
        return lagBehovMedLøsning(
            behov = listOf("Utbetaling"),
            tilstand = null,
            vedtaksperiodeId = null,
            løsninger = mapOf(
                "Utbetaling" to mapOf(
                    "status" to UtbetalingHendelse.Oppdragstatus.OVERFØRT.name,
                    "beskrivelse" to "",
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

    fun lagAnnullering(fagsystemId: String): String {
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

    fun lagOverstyringTidslinje(dager: List<ManuellOverskrivingDag>): String {
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

    private fun nyHendelse(navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(nyHendelse(navn) + hendelse).toJson()

    private fun nyHendelse(navn: String) = mutableMapOf<String, Any>(
        "@id" to UUID.randomUUID(),
        "@event_name" to navn,
        "@opprettet" to LocalDateTime.now()
    )

    private fun lagBehovMedLøsning(
        behov: List<String> = listOf(),
        vedtaksperiodeId: UUID? = UUID.randomUUID(),
        tilstand: TilstandType?,
        løsninger: Map<String, Any> = emptyMap(),
        ekstraFelter: Map<String, Any> = emptyMap()
    ) = nyHendelse(
        "behov", ekstraFelter + mutableMapOf(
            "@behov" to behov,
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "@løsning" to løsninger,
            "@final" to true,
            "@besvart" to LocalDateTime.now()
        ).apply {
            tilstand?.let { this["tilstand"] = it.name }
            vedtaksperiodeId?.let { this["vedtaksperiodeId"] = vedtaksperiodeId.toString() }
        }
    )
}
