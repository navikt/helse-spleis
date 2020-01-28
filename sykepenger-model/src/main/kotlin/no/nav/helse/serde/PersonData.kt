package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.PersonData.ArbeidsgiverData
import no.nav.helse.serde.PersonData.HendelseWrapperData.InntektsmeldingData
import no.nav.helse.serde.reflection.create.ReflectionCreationHelper
import no.nav.helse.sykdomstidslinje.dag.JsonDagType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

internal fun parseJson(json: String): PersonData = objectMapper.readValue(json)

private typealias SykdomstidslinjeData = List<ArbeidsgiverData.VedtaksperiodeData.DagData>

class DataClassModelBuilder(private val json: String) {
    private val reflector = ReflectionCreationHelper()

    fun result(): Person {
        val personData: PersonData = objectMapper.readValue(json)
        val hendelser = personData.hendelser.map{ konverterTilHendelse(personData, it) }
        val arbeidsgivere = personData.arbeidsgivere.map { konverterTilArbeidsgiver(it, hendelser) }

        return Person(personData.aktørId, personData.fødselsnummer).apply {
            val personArbeidsgivere = privatProp<MutableMap<String, Arbeidsgiver>>("arbeidsgivere")
            personArbeidsgivere.putAll(arbeidsgivere.map { it.organisasjonsnummer() to it }.toMap())
        }
    }

    private fun konverterTilHendelse(
        personData: PersonData,
        data: PersonData.HendelseWrapperData
    ): ArbeidstakerHendelse {
        return when (data.type) {
            PersonData.HendelseWrapperData.Hendelsestype.Inntektsmelding -> parseInntektsmelding(personData, data.data)
            PersonData.HendelseWrapperData.Hendelsestype.Foreldrepenger -> TODO()
            PersonData.HendelseWrapperData.Hendelsestype.Sykepengehistorikk -> TODO()
            PersonData.HendelseWrapperData.Hendelsestype.Vilkårsgrunnlag -> TODO()
            PersonData.HendelseWrapperData.Hendelsestype.ManuellSaksbehandling -> TODO()
            PersonData.HendelseWrapperData.Hendelsestype.NySøknad -> TODO()
            PersonData.HendelseWrapperData.Hendelsestype.SendtSøknad -> TODO()
        }
    }

    private fun konverterTilArbeidsgiver(data: ArbeidsgiverData, hendelser: List<ArbeidstakerHendelse>): Arbeidsgiver {
        val inntekthistorikk = Inntekthistorikk()

        data.inntekter.forEach { inntektData ->
            inntekthistorikk.add(
                fom = inntektData.fom,
                hendelse = hendelser.find { it.hendelseId() == inntektData.hendelse } as ModelInntektsmelding,
                beløp = inntektData.beløp
            )
        }

        return reflector.lagArbeidsgiver(
            organisasjonsnummer = data.organisasjonsnummer,
            id = data.id,
            inntekthistorikk = inntekthistorikk
        )
    }

    private fun parseInntektsmelding(
        personData: PersonData,
        jsonNode: JsonNode
    ): ModelInntektsmelding {
        val data: InntektsmeldingData = objectMapper.convertValue(jsonNode)
        return ModelInntektsmelding(
            hendelseId = data.hendelseId,
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = data.refusjon.opphørsdato,
                beløpPrMåned = data.refusjon.beløpPrMåned,
                endringerIRefusjon = data.refusjon.endringerIRefusjon.map { it.endringsdato }
            ),
            orgnummer = data.orgnummer,
            fødselsnummer = personData.fødselsnummer,
            aktørId = personData.aktørId,
            mottattDato = data.mottattDato,
            førsteFraværsdag = data.førsteFraværsdag,
            beregnetInntekt = data.beregnetInntekt,
            aktivitetslogger = Aktivitetslogger(),
            originalJson = "{}",
            arbeidsgiverperioder = data.arbeidsgiverperioder.map { Periode(it.fom, it.tom) },
            ferieperioder = data.ferieperioder.map { Periode(it.fom, it.tom) }
        )
    }
}

internal data class PersonData(
    val skjemaVersjon: Int = 1,
    val aktørId: String,
    val fødselsnummer: String,
    val hendelser: List<HendelseWrapperData>,
    val arbeidsgivere: List<ArbeidsgiverData>
) {
    data class HendelseWrapperData(
        val type: Hendelsestype,
        val tidspunkt: LocalDateTime,
        val data: JsonNode
    ) {
        data class InntektsmeldingData(
            val hendelseId: UUID,
            val orgnummer: String,
            val refusjon: RefusjonData,
            val mottattDato: LocalDateTime,
            val førsteFraværsdag: LocalDate,
            val beregnetInntekt: Double,
            val arbeidsgiverperioder: List<PeriodeData>,
            val ferieperioder: List<PeriodeData>
        ) {
            data class RefusjonData(
                val opphørsdato: LocalDate?,
                val beløpPrMåned: Double,
                val endringerIRefusjon: List<EndringIRefusjonData>
            ) {
                data class EndringIRefusjonData(
                    val endringsdato: LocalDate
                )
            }

            data class PeriodeData(
                val fom: LocalDate,
                val tom: LocalDate
            )
        }

        data class ManuellSaksbehandlingData(
            val hendelseId: UUID,
            val orgnummer: String,
            val saksbehandler: String,
            val utbetalingGodkjent: Boolean,
            val rapportertdato: LocalDateTime
        ) {
            }

        data class NySøknadData(
            val hendelseId: UUID,
            val rapportertdato: LocalDateTime,
            val sykeperioder: List<SykeperiodeData>
        ) {
            data class SykeperiodeData(
                val fom: LocalDate,
                val tom: LocalDate,
                val grad: Int
            )
        }

        data class SendtSøknadData(
            val hendelseId: UUID,
            val rapportertdato: LocalDateTime,
            val perioder: List<SykeperiodeData>
        ) {
            data class SykeperiodeData(
                val type: SykeperiodeData.TypeData,
                val fom: LocalDate,
                val tom: LocalDate,
                val grad: Int?,
                val faktiskGrad: Int?
            ) {
                enum class TypeData {
                    Ferie,
                    Sykdom,
                    Utdanning,
                    Permisjon,
                    Egenmelding,
                    Arbeid
                }
            }
        }

        data class Sykepengehistorikk(
            val hendelseId: UUID,
            val orgnummer: String,
            val utbetalinger: List<PeriodeData>,
            val inntektshistorikk: List<InntektsopplysningData>
        ) {
            data class PeriodeData(
                val fom: LocalDate,
                val tom: LocalDate,
                val dagsats: Int,
                val type: TypeData
            ) {
                enum class TypeData {
                    RefusjonTilArbeidsgiver,
                    ReduksjonMedlem,
                    Etterbetaling,
                    KontertRegnskap,
                    ReduksjonArbeidsRefusjon,
                    Tilbakeført,
                    Konvertert,
                    Ferie,
                    Opphold,
                    Sanksjon,
                    Ukjent
                }
            }

            data class InntektsopplysningData(
                val hendelseId: UUID,
                val orgnummer: String,
                val sykepengerFom: LocalDate,
                val inntektPerMåned: Int
            )
        }

        data class VilkårsgrunnlagData(
            val hendelseId: UUID,
            val orgnummer: String,
            val rapportertDato: LocalDateTime,
            val inntektsmåneder: List<Måned>,
            val erEgenAnsatt: Boolean
        ) {
            data class Måned(
                val årMåned: YearMonth,
                val inntektsliste: List<Inntekt>
            ) {
                data class Inntekt(
                    val beløp: Double
                )
            }
        }

        enum class Hendelsestype {
            Foreldrepenger,
            Sykepengehistorikk,
            Vilkårsgrunnlag,
            ManuellSaksbehandling,
            Inntektsmelding,
            NySøknad,
            SendtSøknad
        }
    }

    data class ArbeidsgiverData(
        val organisasjonsnummer: String,
        val id: UUID,
        val inntekter: List<InntektData>,
        val vedtaksperioder: List<VedtaksperiodeData>
    ) {
        data class InntektData(
            val fom: LocalDate,
            val hendelse: UUID,
            val beløp: BigDecimal
        )

        data class VedtaksperiodeData(
            val id: UUID,
            val maksdato: LocalDate?,
            val godkjentAv: String?,
            val utbetalingsreferanse: String?,
            val førsteFraværsdag: LocalDate?,
            val inntektFraInntektsmelding: BigDecimal?,
            val dataForVilkårsvurdering: DataForVilkårsvurderingData?,
            val sykdomshistorikk: List<SykdomshistorikkData>,
            val tilstand: TilstandType,
            val sykdomstidslinje: SykdomstidslinjeData,
            val utbetalingslinjer: List<Any>
        ) {
            data class DagData(
                val dagen: LocalDate,
                val hendelseId: UUID,
                val type: JsonDagType,
                val erstatter: List<DagData>
            )

            data class SykdomshistorikkData(
                val tidsstempel: LocalDateTime,
                val hendelseId: UUID,
                val hendelseSykdomstidslinje: SykdomstidslinjeData,
                val beregnetSykdomstidslinje: SykdomstidslinjeData
            )

            data class DataForVilkårsvurderingData(
                val erEgenAnsatt: Boolean,
                val beregnetÅrsinntektFraInntektskomponenten: Double,
                val avviksprosent: Double
            )
        }
    }
}
