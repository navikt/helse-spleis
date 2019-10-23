package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*

internal object TestHendelser {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val sykeperiodeFOM = LocalDate.of(2019, Month.SEPTEMBER, 16)
    val sykeperiodeTOM = LocalDate.of(2019, Month.OCTOBER, 5)
    val egenmeldingFom = LocalDate.of(2019, Month.SEPTEMBER, 12)
    val egenmeldingTom = LocalDate.of(2019, Month.SEPTEMBER, 15)
    val ferieFom = LocalDate.of(2019, Month.SEPTEMBER, 20)
    val ferieTom = LocalDate.of(2019, Month.SEPTEMBER, 23)
    val `seks måneder og én dag før første sykedag` = egenmeldingFom.minusMonths(6).minusDays(1)
    val `én dag færre enn seks måneder før første sykedag` = egenmeldingFom.minusMonths(6).plusDays(1)


    val søknadsperioder = listOf(
        SoknadsperiodeDTO(
            fom = sykeperiodeFOM,
            tom = LocalDate.of(2019, Month.SEPTEMBER, 30)
        ),
        SoknadsperiodeDTO(
            fom = LocalDate.of(2019, Month.OCTOBER, 5),
            tom = sykeperiodeTOM
        )
    )

    private fun søknad(
        id: String = UUID.randomUUID().toString(),
        status: SoknadsstatusDTO = SoknadsstatusDTO.SENDT,
        opprettetTidspunkt: LocalDateTime,
        arbeidGjenopptatt: LocalDate? = null,
        korrigerer: String? = null,
        egenmeldinger: List<PeriodeDTO> = listOf(
            PeriodeDTO(
                fom = egenmeldingFom,
                tom = egenmeldingTom
            )
        ),
        søknadsperioder: List<SoknadsperiodeDTO>,
        fravær: List<FravarDTO>,
        arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(
            navn = "enArbeidsgiver",
            orgnummer = "123456789"
        )
    ) = objectMapper.valueToTree<JsonNode>(
        SykepengesoknadDTO(
            id = id,
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = status,
            aktorId = UUID.randomUUID().toString(),
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = arbeidsgiver,
            arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
            arbeidsgiverForskutterer = ArbeidsgiverForskuttererDTO.JA,
            fom = søknadsperioder.first().fom,
            tom = søknadsperioder.last().tom,
            startSyketilfelle = LocalDate.of(2019, Month.SEPTEMBER, 16),
            arbeidGjenopptatt = arbeidGjenopptatt,
            korrigerer = korrigerer,
            opprettet = opprettetTidspunkt,
            sendtNav = opprettetTidspunkt,
            sendtArbeidsgiver = LocalDateTime.of(2019, Month.SEPTEMBER, 30, 0, 0, 0),
            egenmeldinger = egenmeldinger,
            soknadsperioder = søknadsperioder,
            fravar = fravær
        )
    ).let {
        when (status) {
            SoknadsstatusDTO.NY -> NySøknadOpprettet(it)
            SoknadsstatusDTO.FREMTIDIG -> NySøknadOpprettet(it)
            SoknadsstatusDTO.SENDT -> SendtSøknadMottatt(it)
            else -> throw IllegalArgumentException("Kan ikke håndtere søknadstatus: $status")
        }
    }

    fun sendtSøknad(
        opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
        arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(
            navn = "enArbeidsgiver",
            orgnummer = "123456789"
        ),
        fravær: List<FravarDTO> = listOf(
            FravarDTO(
                fom = ferieFom,
                tom = ferieTom,
                type = FravarstypeDTO.FERIE
            )
        ),
        søknadsperioder: List<SoknadsperiodeDTO> = this.søknadsperioder,
        arbeidGjenopptatt: LocalDate? = null
    ) =
        søknad(
            opprettetTidspunkt = opprettetTidspunkt,
            arbeidsgiver = arbeidsgiver,
            fravær = fravær,
            søknadsperioder = søknadsperioder,
            arbeidGjenopptatt = arbeidGjenopptatt
        ) as SendtSøknadMottatt

    fun nySøknad(
        opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
        arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(
            navn = "enArbeidsgiver",
            orgnummer = "123456789"
        ),
        søknadsperioder: List<SoknadsperiodeDTO> = this.søknadsperioder
    ) =
        søknad(
            opprettetTidspunkt = opprettetTidspunkt,
            status = SoknadsstatusDTO.NY,
            arbeidsgiver = arbeidsgiver,
            fravær = emptyList(),
            søknadsperioder = søknadsperioder
        ) as NySøknadOpprettet

    fun sykepengeHistorikk(sisteHistoriskeSykedag: LocalDate): Sykepengehistorikk {
        val historikk: Map<String, Any> = mapOf<String, Any>(
            "organisasjonsnummer" to "ola sitt orgnummer",
            "sakskompleksId" to "121312",
            "aktørId" to "12345678910",
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

    fun inntektsmelding(virksomhetsnummer: String? = null) = no.nav.helse.hendelse.InntektsmeldingMottatt(
        objectMapper.valueToTree(
            Inntektsmelding(
                inntektsmeldingId = "",
                arbeidstakerFnr = "",
                arbeidstakerAktorId = "",
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
        )
    )
}
