package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelse.NySykepengesøknad
import no.nav.helse.hendelse.SendtSykepengesøknad
import no.nav.helse.hendelse.Sykepengehistorikk
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer
import no.nav.helse.søknad.SøknadConsumer
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*

internal object TestConstants {
    private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val sykeperiodFOM = 16.september
    val sykeperiodeTOM = 5.oktober
    val egenmeldingFom = 12.september
    val egenmeldingTom = 15.september
    val ferieFom = 1.oktober
    val ferieTom = 4.oktober

    fun søknadDTO(
            id: String = UUID.randomUUID().toString(),
            status: SoknadsstatusDTO,
            aktørId: String = UUID.randomUUID().toString().substring(0, 13),
            fom: LocalDate = 10.september,
            tom: LocalDate = 5.oktober,
            arbeidGjenopptatt: LocalDate? = null,
            korrigerer: String? = null,
            egenmeldinger: List<PeriodeDTO> = listOf(PeriodeDTO(
                    fom = egenmeldingFom,
                    tom = egenmeldingTom
            )),
            søknadsperioder: List<SoknadsperiodeDTO> = listOf(SoknadsperiodeDTO(
                    fom = sykeperiodFOM,
                    tom = 30.september
            ), SoknadsperiodeDTO(
                    fom = 5.oktober,
                    tom = sykeperiodeTOM
            )),
            fravær: List<FravarDTO> = listOf(FravarDTO(
                    fom = ferieFom,
                    tom = ferieTom,
                    type = FravarstypeDTO.FERIE)),
            arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(
                    navn = "enArbeidsgiver",
                    orgnummer = "123456789"
            )
    ) = SykepengesoknadDTO(
            id = id,
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = status,
            aktorId = aktørId,
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = arbeidsgiver,
            arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
            arbeidsgiverForskutterer = ArbeidsgiverForskuttererDTO.JA,
            fom = søknadsperioder.sortedBy { it.fom }.first().fom,
            tom = søknadsperioder.sortedBy { it.tom }.last().tom,
            startSyketilfelle = LocalDate.of(2019, Month.SEPTEMBER, 10),
            arbeidGjenopptatt = arbeidGjenopptatt,
            korrigerer = korrigerer,
            opprettet = LocalDateTime.now(),
            sendtNav = LocalDateTime.now(),
            sendtArbeidsgiver = LocalDateTime.of(2019, Month.SEPTEMBER, 30, 0, 0, 0),
            egenmeldinger = egenmeldinger,
            soknadsperioder = søknadsperioder,
            fravar = fravær
    )

    fun sendtSøknad(
            id: String = UUID.randomUUID().toString(),
            aktørId: String = UUID.randomUUID().toString(),
            fom: LocalDate = 10.september,
            tom: LocalDate = 5.oktober,
            arbeidGjenopptatt: LocalDate? = null,
            korrigerer: String? = null,
            egenmeldinger: List<PeriodeDTO> = listOf(PeriodeDTO(
                    fom = egenmeldingFom,
                    tom = egenmeldingTom
            )),
            søknadsperioder: List<SoknadsperiodeDTO> = listOf(SoknadsperiodeDTO(
                    fom = sykeperiodFOM,
                    tom = 30.september
            ), SoknadsperiodeDTO(
                    fom = 5.oktober,
                    tom = sykeperiodeTOM
            )),
            fravær: List<FravarDTO> = listOf(FravarDTO(
                    fom = ferieFom,
                    tom = ferieTom,
                    type = FravarstypeDTO.FERIE)),
            arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(
                    navn = "enArbeidsgiver",
                    orgnummer = "123456789"
            )
    ) = SendtSykepengesøknad(søknadDTO(
            id = id,
            aktørId = aktørId,
            fom = fom,
            tom = tom,
            arbeidGjenopptatt = arbeidGjenopptatt,
            korrigerer = korrigerer,
            egenmeldinger = egenmeldinger,
            søknadsperioder = søknadsperioder,
            fravær = fravær,
            status = SoknadsstatusDTO.SENDT,
            arbeidsgiver = arbeidsgiver
    ).toJsonNode())

    fun nySøknad(
            id: String = UUID.randomUUID().toString(),
            aktørId: String = UUID.randomUUID().toString(),
            fom: LocalDate = 10.september,
            tom: LocalDate = 5.oktober,
            arbeidGjenopptatt: LocalDate? = null,
            korrigerer: String? = null,
            egenmeldinger: List<PeriodeDTO> = listOf(PeriodeDTO(
                    fom = egenmeldingFom,
                    tom = egenmeldingTom
            )),
            søknadsperioder: List<SoknadsperiodeDTO> = listOf(SoknadsperiodeDTO(
                    fom = sykeperiodFOM,
                    tom = 30.september
            ), SoknadsperiodeDTO(
                    fom = 5.oktober,
                    tom = sykeperiodeTOM
            )),
            fravær: List<FravarDTO> = listOf(FravarDTO(
                    fom = ferieFom,
                    tom = ferieTom,
                    type = FravarstypeDTO.FERIE)),
            arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(
                    navn = "enArbeidsgiver",
                    orgnummer = "123456789"
            )
    ) = NySykepengesøknad(søknadDTO(
            id = id,
            aktørId = aktørId,
            fom = fom,
            tom = tom,
            arbeidGjenopptatt = arbeidGjenopptatt,
            korrigerer = korrigerer,
            egenmeldinger = egenmeldinger,
            søknadsperioder = søknadsperioder,
            fravær = fravær,
            status = SoknadsstatusDTO.NY,
            arbeidsgiver = arbeidsgiver
    ).toJsonNode())

    fun inntektsmelding(aktørId: String = "", virksomhetsnummer: String? = "123456789") = no.nav.helse.hendelse.Inntektsmelding(inntektsmeldingDTO(aktørId, virksomhetsnummer).toJsonNode())

    fun inntektsmeldingDTO(aktørId: String = "", virksomhetsnummer: String? = "123456789") =
            Inntektsmelding(
                    inntektsmeldingId = "",
                    arbeidstakerFnr = "",
                    arbeidstakerAktorId = aktørId,
                    virksomhetsnummer = virksomhetsnummer,
                    arbeidsgiverFnr = null,
                    arbeidsgiverAktorId = null,
                    arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
                    arbeidsforholdId = null,
                    beregnetInntekt = null,
                    refusjon = Refusjon(
                            beloepPrMnd = null,
                            opphoersdato = null
                    ),
                    endringIRefusjoner = emptyList(),
                    opphoerAvNaturalytelser = emptyList(),
                    gjenopptakelseNaturalytelser = emptyList(),
                    arbeidsgiverperioder = emptyList(),
                    status = Status.GYLDIG,
                    arkivreferanse = ""
            )

    fun sykepengehistorikk(
            sisteHistoriskeSykedag: LocalDate,
            organisasjonsnummer: String = "123546564",
            aktørId: String = "1",
            sakskompleksId: UUID = UUID.randomUUID()
    ): Sykepengehistorikk {
        val historikk: Map<String, Any> = mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "sakskompleksId" to sakskompleksId.toString(),
                "aktørId" to aktørId,
                "@løsning" to mapOf<String, Any>(
                        "perioder" to listOf(
                                mapOf<String, Any>(
                                        "fom" to "${sisteHistoriskeSykedag.minusMonths(1)}",
                                        "tom" to "$sisteHistoriskeSykedag",
                                        "grad" to "100"
                                )
                        )
                )
        )
        return Sykepengehistorikk(objectMapper.valueToTree(historikk))
    }

}

fun SykepengesoknadDTO.toJsonNode(): JsonNode = SøknadConsumer.søknadObjectMapper.valueToTree(this)
fun Inntektsmelding.toJsonNode(): JsonNode = InntektsmeldingConsumer.inntektsmeldingObjectMapper.valueToTree(this)

val Int.juli
    get() = LocalDate.of(2019, Month.JULY, this)

val Int.august
    get() = LocalDate.of(2019, Month.AUGUST, this)

val Int.september
    get() = LocalDate.of(2019, Month.SEPTEMBER, this)

val Int.oktober
    get() = LocalDate.of(2019, Month.OCTOBER, this)
