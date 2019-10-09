package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelse.NySykepengesøknad
import no.nav.helse.hendelse.SendtSykepengesøknad
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

    val sykeperiodFOM = LocalDate.of(2019, Month.SEPTEMBER, 16)
    val sykeperiodeTOM = LocalDate.of(2019, Month.OCTOBER, 5)
    val egenmeldingFom = LocalDate.of(2019, Month.SEPTEMBER, 12)
    val egenmeldingTom = LocalDate.of(2019, Month.SEPTEMBER, 15)
    val ferieFom = LocalDate.of(2019, Month.OCTOBER, 1)
    val ferieTom = LocalDate.of(2019, Month.OCTOBER, 4)

    private fun søknad(
            id: String = UUID.randomUUID().toString(),
            status: SoknadsstatusDTO = SoknadsstatusDTO.SENDT,
            fom: LocalDate = LocalDate.of(2019, Month.SEPTEMBER, 10),
            tom: LocalDate = LocalDate.of(2019, Month.OCTOBER, 5),
            arbeidGjenopptatt: LocalDate? = null,
            korrigerer: String? = null,
            egenmeldinger: List<PeriodeDTO> = listOf(PeriodeDTO(
                    fom = egenmeldingFom,
                    tom = egenmeldingTom
            )),
            søknadsperioder: List<SoknadsperiodeDTO> = listOf(SoknadsperiodeDTO(
                    fom = sykeperiodFOM,
                    tom = LocalDate.of(2019, Month.SEPTEMBER, 30)
            ), SoknadsperiodeDTO(
                    fom = LocalDate.of(2019, Month.OCTOBER, 5),
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
    ) = objectMapper.valueToTree<JsonNode>(SykepengesoknadDTO(
            id = id,
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = status,
            aktorId = UUID.randomUUID().toString(),
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = arbeidsgiver,
            arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
            arbeidsgiverForskutterer = ArbeidsgiverForskuttererDTO.JA,
            fom = fom,
            tom = tom,
            startSyketilfelle = LocalDate.of(2019, Month.SEPTEMBER, 10),
            arbeidGjenopptatt = arbeidGjenopptatt,
            korrigerer = korrigerer,
            opprettet = LocalDateTime.now(),
            sendtNav = LocalDateTime.now(),
            sendtArbeidsgiver = LocalDateTime.of(2019, Month.SEPTEMBER, 30, 0, 0, 0),
            egenmeldinger = egenmeldinger,
            soknadsperioder = søknadsperioder,
            fravar = fravær
    )).let {
        when (status) {
            SoknadsstatusDTO.NY -> NySykepengesøknad(it)
            SoknadsstatusDTO.FREMTIDIG -> NySykepengesøknad(it)
            SoknadsstatusDTO.SENDT -> SendtSykepengesøknad(it)
            else -> throw IllegalArgumentException("Kan ikke håndtere søknadstatus: $status")
        }
    }

    fun sendtSøknad(
            arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(
                    navn = "enArbeidsgiver",
                    orgnummer = "123456789"
            )) =
            søknad(arbeidsgiver = arbeidsgiver
            ) as SendtSykepengesøknad

    fun nySøknad(
            arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(
                    navn = "enArbeidsgiver",
                    orgnummer = "123456789"
            )) =
            søknad(
                    status = SoknadsstatusDTO.NY,
                    arbeidsgiver = arbeidsgiver
            ) as NySykepengesøknad

    fun inntektsmelding(virksomhetsnummer: String? = null) = no.nav.helse.hendelse.Inntektsmelding(objectMapper.valueToTree(Inntektsmelding(
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
    )))
}
