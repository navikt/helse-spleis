package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer
import no.nav.helse.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.saksbehandling.ManuellSaksbehandling
import no.nav.helse.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykepengehistorikk.Sykepengehistorikk
import no.nav.helse.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.søknad.NySøknadHendelse
import no.nav.helse.søknad.SendtSøknadHendelse
import no.nav.helse.søknad.Sykepengesøknad
import no.nav.helse.søknad.SøknadConsumer
import no.nav.inntektsmeldingkontrakt.*
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*

internal object TestConstants {
    private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val sykeperiodeFOM = 16.september
    val sykeperiodeTOM = 5.oktober
    val egenmeldingFom = 12.september
    val egenmeldingTom = 15.september
    val ferieFom = 1.oktober
    val ferieTom = 4.oktober

    fun søknadDTO(
            id: String = UUID.randomUUID().toString(),
            status: SoknadsstatusDTO,
            aktørId: String = UUID.randomUUID().toString().substring(0, 13),
            arbeidGjenopptatt: LocalDate? = null,
            korrigerer: String? = null,
            egenmeldinger: List<PeriodeDTO> = listOf(PeriodeDTO(
                    fom = egenmeldingFom,
                    tom = egenmeldingTom
            )),
            søknadsperioder: List<SoknadsperiodeDTO> = listOf(SoknadsperiodeDTO(
                    fom = sykeperiodeFOM,
                    tom = 30.september,
                    sykmeldingsgrad = 100
            ), SoknadsperiodeDTO(
                    fom = 5.oktober,
                    tom = sykeperiodeTOM,
                    sykmeldingsgrad = 100
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

    fun sendtSøknadHendelse(
            id: String = UUID.randomUUID().toString(),
            aktørId: String = UUID.randomUUID().toString(),
            arbeidGjenopptatt: LocalDate? = null,
            korrigerer: String? = null,
            egenmeldinger: List<PeriodeDTO> = listOf(PeriodeDTO(
                    fom = egenmeldingFom,
                    tom = egenmeldingTom
            )),
            søknadsperioder: List<SoknadsperiodeDTO> = listOf(SoknadsperiodeDTO(
                    fom = sykeperiodeFOM,
                    tom = 30.september,
                    sykmeldingsgrad = 100
            ), SoknadsperiodeDTO(
                    fom = 5.oktober,
                    tom = sykeperiodeTOM,
                    sykmeldingsgrad = 100
            )),
            fravær: List<FravarDTO> = listOf(FravarDTO(
                    fom = ferieFom,
                    tom = ferieTom,
                    type = FravarstypeDTO.FERIE)),
            arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(
                    navn = "enArbeidsgiver",
                    orgnummer = "123456789"
            )
    ) = SendtSøknadHendelse(Sykepengesøknad(søknadDTO(
            id = id,
            aktørId = aktørId,
            arbeidGjenopptatt = arbeidGjenopptatt,
            korrigerer = korrigerer,
            egenmeldinger = egenmeldinger,
            søknadsperioder = søknadsperioder,
            fravær = fravær,
            status = SoknadsstatusDTO.SENDT,
            arbeidsgiver = arbeidsgiver
    ).toJsonNode()))

    fun nySøknadHendelse(
            id: String = UUID.randomUUID().toString(),
            aktørId: String = UUID.randomUUID().toString(),
            arbeidGjenopptatt: LocalDate? = null,
            korrigerer: String? = null,
            egenmeldinger: List<PeriodeDTO> = listOf(PeriodeDTO(
                    fom = egenmeldingFom,
                    tom = egenmeldingTom
            )),
            søknadsperioder: List<SoknadsperiodeDTO> = listOf(SoknadsperiodeDTO(
                    fom = sykeperiodeFOM,
                    tom = 30.september,
                    sykmeldingsgrad = 100
            ), SoknadsperiodeDTO(
                    fom = 5.oktober,
                    tom = sykeperiodeTOM,
                    sykmeldingsgrad = 100
            )),
            fravær: List<FravarDTO> = listOf(FravarDTO(
                    fom = ferieFom,
                    tom = ferieTom,
                    type = FravarstypeDTO.FERIE)),
            arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(
                    navn = "enArbeidsgiver",
                    orgnummer = "123456789"
            )
    ) = NySøknadHendelse(Sykepengesøknad(søknadDTO(
            id = id,
            aktørId = aktørId,
            arbeidGjenopptatt = arbeidGjenopptatt,
            korrigerer = korrigerer,
            egenmeldinger = egenmeldinger,
            søknadsperioder = søknadsperioder,
            fravær = fravær,
            status = SoknadsstatusDTO.NY,
            arbeidsgiver = arbeidsgiver
    ).toJsonNode()))


    fun inntektsmeldingHendelse(aktørId: String = "",
                                virksomhetsnummer: String? = "123456789",
                                førsteFraværsdag: LocalDate = 10.september,
                                arbeidsgiverperioder: List<Periode> = listOf(
                                        Periode(10.september, 10.september.plusDays(16))
                                ),
                                ferieperioder: List<Periode> = emptyList()) =
            InntektsmeldingHendelse(no.nav.helse.inntektsmelding.Inntektsmelding(
                    inntektsmeldingDTO(aktørId, virksomhetsnummer, førsteFraværsdag, arbeidsgiverperioder, ferieperioder).toJsonNode()
            ))

    fun inntektsmeldingDTO(aktørId: String = "",
                           virksomhetsnummer: String? = "123456789",
                           førsteFraværsdag: LocalDate = 10.september,
                           arbeidsgiverperioder: List<Periode> = listOf(
                                   Periode(10.september, 10.september.plusDays(16))
                           ),
                           feriePerioder: List<Periode> = emptyList()) =
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
                    arbeidsgiverperioder = arbeidsgiverperioder,
                    status = Status.GYLDIG,
                    arkivreferanse = "",
                    ferieperioder = feriePerioder,
                    foersteFravaersdag = førsteFraværsdag,
                    mottattDato = LocalDateTime.now()
            )

    fun responsFraSpole(perioder: List<SpolePeriode>) = mapOf<String, Any>(
            "perioder" to perioder.map {
                mapOf<String, Any>(
                        "fom" to "${it.fom}",
                        "tom" to "${it.tom}",
                        "grad" to it.grad
                )
            }
    )

    fun sykepengehistorikk(
            perioder: List<SpolePeriode>,
            organisasjonsnummer: String = "123546564",
            aktørId: String = "1",
            sakskompleksId: UUID = UUID.randomUUID()
    ): Sykepengehistorikk {
        val behov = Behov.nyttBehov(BehovsTyper.Sykepengehistorikk, mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "sakskompleksId" to sakskompleksId.toString(),
                "aktørId" to aktørId
        )).also {
            it.løsBehov(responsFraSpole(
                    perioder = perioder
            ))
        }
        return Sykepengehistorikk(objectMapper.readTree(behov.toJson()))
    }

    fun sykepengehistorikkHendelse(
            sisteHistoriskeSykedag: LocalDate = LocalDate.now(),
            organisasjonsnummer: String = "123546564",
            aktørId: String = "1",
            sakskompleksId: UUID = UUID.randomUUID()
    ) = SykepengehistorikkHendelse(sykepengehistorikk(
            perioder = listOf(
                    SpolePeriode(
                            fom = sisteHistoriskeSykedag.minusMonths(1),
                            tom = sisteHistoriskeSykedag,
                            grad = "100"
                    )
            ),
            organisasjonsnummer = organisasjonsnummer,
            aktørId = aktørId,
            sakskompleksId = sakskompleksId
    ))

    fun manuellSaksbehandlingHendelse(sakskompleksId: String,
                                      utbetalingGodkjent: Boolean) =
            ManuellSaksbehandlingHendelse(ManuellSaksbehandling(sakskompleksId = sakskompleksId, utbetalingGodkjent = utbetalingGodkjent))
}

data class SpolePeriode(val fom: LocalDate,
                        val tom: LocalDate,
                        val grad: String)

class Uke(ukenr: Long) {
    val mandag = LocalDate.of(2018, 1, 1)
            .plusWeeks(ukenr - 1L)
    val tirsdag get() = mandag.plusDays(1)
    val onsdag get() = mandag.plusDays(2)
    val torsdag get() = mandag.plusDays(3)
    val fredag get() = mandag.plusDays(4)
    val lørdag get() = mandag.plusDays(5)
    val søndag get() = mandag.plusDays(6)
}

operator fun Sykdomstidslinje.get(index: LocalDate) = flatten().firstOrNull { it.startdato() == index }

fun SykepengesoknadDTO.toJsonNode(): JsonNode = SøknadConsumer.søknadObjectMapper.valueToTree(this)
fun Inntektsmelding.toJsonNode(): JsonNode = InntektsmeldingConsumer.inntektsmeldingObjectMapper.valueToTree(this)

val Int.juni
    get() = LocalDate.of(2019, Month.JUNE, this)

val Int.juli
    get() = LocalDate.of(2019, Month.JULY, this)

val Int.august
    get() = LocalDate.of(2019, Month.AUGUST, this)

val Int.september
    get() = LocalDate.of(2019, Month.SEPTEMBER, this)

val Int.oktober
    get() = LocalDate.of(2019, Month.OCTOBER, this)
