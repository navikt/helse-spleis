package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants.objectMapper
import no.nav.helse.behov.Behov
import no.nav.inntektsmeldingkontrakt.*
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*
import no.nav.inntektsmeldingkontrakt.Inntektsmelding as Inntektsmeldingkontrakt

internal object TestConstants {
    internal val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val sykeperiodeFOM = 16.september
    private val sykeperiodeTOM = 5.oktober
    private val egenmeldingFom = 12.september
    private val egenmeldingTom = 15.september
    private val ferieFom = 1.oktober
    private val ferieTom = 4.oktober

    fun søknadDTO(
        id: String = UUID.randomUUID().toString(),
        status: SoknadsstatusDTO,
        aktørId: String = UUID.randomUUID().toString().substring(0, 13),
        fødselsnummer: String = UUID.randomUUID().toString().substring(0, 11),
        arbeidGjenopptatt: LocalDate? = null,
        korrigerer: String? = null,
        egenmeldinger: List<PeriodeDTO> = listOf(
            PeriodeDTO(
                fom = egenmeldingFom,
                tom = egenmeldingTom
            )
        ),
        søknadsperioder: List<SoknadsperiodeDTO> = listOf(
            SoknadsperiodeDTO(
                fom = sykeperiodeFOM,
                tom = 30.september,
                sykmeldingsgrad = 100
            ), SoknadsperiodeDTO(
                fom = 5.oktober,
                tom = sykeperiodeTOM,
                sykmeldingsgrad = 100
            )
        ),
        fravær: List<FravarDTO> = listOf(
            FravarDTO(
                fom = ferieFom,
                tom = ferieTom,
                type = FravarstypeDTO.FERIE
            )
        ),
        arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(
            navn = "enArbeidsgiver",
            orgnummer = "123456789"
        ),
        sendtNav: LocalDateTime = sykeperiodeTOM.plusDays(10).atStartOfDay()
    ) = SykepengesoknadDTO(
        id = id,
        type = SoknadstypeDTO.ARBEIDSTAKERE,
        status = status,
        aktorId = aktørId,
        fnr = fødselsnummer,
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
        sendtNav = sendtNav,
        sendtArbeidsgiver = LocalDateTime.of(2019, Month.SEPTEMBER, 30, 0, 0, 0),
        egenmeldinger = egenmeldinger,
        soknadsperioder = søknadsperioder,
        fravar = fravær
    )

    fun inntektsmeldingDTO(
        aktørId: String = "",
        fødselsnummer: String = "",
        virksomhetsnummer: String? = "123456789",
        førsteFraværsdag: LocalDate = 10.september,
        arbeidsgiverperioder: List<Periode> = listOf(
            Periode(10.september, 10.september.plusDays(16))
        ),
        feriePerioder: List<Periode> = emptyList(),
        refusjon: Refusjon = Refusjon(
            beloepPrMnd = 666.toBigDecimal(),
            opphoersdato = null
        ),
        endringerIRefusjoner: List<EndringIRefusjon> = emptyList(),
        beregnetInntekt: BigDecimal? = 666.toBigDecimal()
    ) =
        Inntektsmeldingkontrakt(
            inntektsmeldingId = "",
            arbeidstakerFnr = fødselsnummer,
            arbeidstakerAktorId = aktørId,
            virksomhetsnummer = virksomhetsnummer,
            arbeidsgiverFnr = null,
            arbeidsgiverAktorId = null,
            arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
            arbeidsforholdId = null,
            beregnetInntekt = beregnetInntekt,
            refusjon = refusjon,
            endringIRefusjoner = endringerIRefusjoner,
            opphoerAvNaturalytelser = emptyList(),
            gjenopptakelseNaturalytelser = emptyList(),
            arbeidsgiverperioder = arbeidsgiverperioder,
            status = Status.GYLDIG,
            arkivreferanse = "",
            ferieperioder = feriePerioder,
            foersteFravaersdag = førsteFraværsdag,
            mottattDato = LocalDateTime.now()
        )

}

internal fun Behov.løsBehov(løsning: Any): Behov {
    val pakke = objectMapper.readTree(this.toJson()) as ObjectNode
    pakke.put("@besvart", LocalDateTime.now().toString())
    pakke.set<JsonNode>("@løsning", objectMapper.valueToTree<JsonNode>(løsning))
    pakke.put("@final", true)
    return Behov.fromJson(pakke.toString())
}

internal data class SpolePeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: String
)

internal fun SykepengesoknadDTO.toJsonNode(): JsonNode = objectMapper.valueToTree(this)
internal fun SykepengesoknadDTO.toJson(): String = objectMapper.writeValueAsString(this)
internal fun Inntektsmeldingkontrakt.toJsonNode(): JsonNode = objectMapper.valueToTree(this)

internal val Int.juni
    get() = LocalDate.of(2019, Month.JUNE, this)

internal val Int.juli
    get() = LocalDate.of(2019, Month.JULY, this)

internal val Int.august
    get() = LocalDate.of(2019, Month.AUGUST, this)

internal val Int.september
    get() = LocalDate.of(2019, Month.SEPTEMBER, this)

internal val Int.oktober
    get() = LocalDate.of(2019, Month.OCTOBER, this)
