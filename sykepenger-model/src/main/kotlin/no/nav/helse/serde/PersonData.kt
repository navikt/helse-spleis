package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelser.ModelForeldrepenger
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelSykepengehistorikk
import no.nav.helse.hendelser.ModelYtelser
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.PersonData.*
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
        data: HendelseWrapperData
    ): ArbeidstakerHendelse {
        return when (data.type) {
            HendelseWrapperData.Hendelsestype.Inntektsmelding -> parseInntektsmelding(personData, data.data)
            HendelseWrapperData.Hendelsestype.Ytelser -> parseForeldrepenger(personData, data.data)
            HendelseWrapperData.Hendelsestype.Vilkårsgrunnlag -> TODO()
            HendelseWrapperData.Hendelsestype.ManuellSaksbehandling -> TODO()
            HendelseWrapperData.Hendelsestype.NySøknad -> TODO()
            HendelseWrapperData.Hendelsestype.SendtSøknad -> TODO()
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
            orgnummer = data.orgnummer,
            fødselsnummer = personData.fødselsnummer,
            aktørId = personData.aktørId,
            mottattDato = data.mottattDato,
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = data.refusjon.opphørsdato,
                beløpPrMåned = data.refusjon.beløpPrMåned,
                endringerIRefusjon = data.refusjon.endringerIRefusjon.map { it.endringsdato }
            ),
            førsteFraværsdag = data.førsteFraværsdag,
            beregnetInntekt = data.beregnetInntekt,
            aktivitetslogger = Aktivitetslogger(),
            originalJson = "{}",
            arbeidsgiverperioder = data.arbeidsgiverperioder.map { Periode(it.fom, it.tom) },
            ferieperioder = data.ferieperioder.map { Periode(it.fom, it.tom) }
        )
    }

    private fun parseForeldrepenger(personData: PersonData, jsonNode: JsonNode): ModelYtelser {
        val data: HendelseWrapperData.YtelserData = objectMapper.convertValue(jsonNode)
        return ModelYtelser(
            hendelseId = data.hendelseId,
            vedtaksperiodeId = data.vedtaksperiodeId,
            organisasjonsnummer = data.orgnummer,
            fødselsnummer = personData.fødselsnummer,
            aktørId = personData.aktørId,
            rapportertdato = data.mottattDato,
            sykepengehistorikk = ModelSykepengehistorikk(
                utbetalinger = data.sykepengehistorikk.utbetalinger.map(::parseUtbetaling),
                inntektshistorikk = data.sykepengehistorikk.inntektshistorikk.map(::parseInntektsopplysning),
                aktivitetslogger = Aktivitetslogger()
            ),
            foreldrepenger = ModelForeldrepenger(
                foreldrepengeytelse = parsePeriode(data.foreldrepenger.foreldrepengeytelse),
                svangerskapsytelse = parsePeriode(data.foreldrepenger.svangerskapsytelse),
                aktivitetslogger = Aktivitetslogger()
            ),
            aktivitetslogger = Aktivitetslogger()
        )
    }

    private fun parseUtbetaling(
        periode: HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData
    ): ModelSykepengehistorikk.Periode = when (periode.type) {
        HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.RefusjonTilArbeidsgiver -> {
            ModelSykepengehistorikk.Periode.RefusjonTilArbeidsgiver(
                fom = periode.fom,
                tom = periode.tom,
                dagsats = periode.dagsats
            )
        }
        HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.ReduksjonMedlem -> {
            ModelSykepengehistorikk.Periode.ReduksjonMedlem(
                fom = periode.fom,
                tom = periode.tom,
                dagsats = periode.dagsats
            )
        }
        HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Etterbetaling -> {
            ModelSykepengehistorikk.Periode.Etterbetaling(
                fom = periode.fom,
                tom = periode.tom,
                dagsats = periode.dagsats
            )
        }
        HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.KontertRegnskap -> {
            ModelSykepengehistorikk.Periode.KontertRegnskap(
                fom = periode.fom,
                tom = periode.tom,
                dagsats = periode.dagsats
            )
        }
        HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.ReduksjonArbeidsgiverRefusjon -> {
            ModelSykepengehistorikk.Periode.ReduksjonArbeidsgiverRefusjon(
                fom = periode.fom,
                tom = periode.tom,
                dagsats = periode.dagsats
            )
        }
        HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Tilbakeført -> {
            ModelSykepengehistorikk.Periode.Tilbakeført(
                fom = periode.fom,
                tom = periode.tom,
                dagsats = periode.dagsats
            )
        }
        HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Konvertert -> {
            ModelSykepengehistorikk.Periode.Konvertert(
                fom = periode.fom,
                tom = periode.tom,
                dagsats = periode.dagsats
            )
        }
        HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Ferie -> {
            ModelSykepengehistorikk.Periode.Ferie(
                fom = periode.fom,
                tom = periode.tom,
                dagsats = periode.dagsats
            )
        }
        HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Opphold -> {
            ModelSykepengehistorikk.Periode.Opphold(
                fom = periode.fom,
                tom = periode.tom,
                dagsats = periode.dagsats
            )
        }
        HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Sanksjon -> {
            ModelSykepengehistorikk.Periode.Sanksjon(
                fom = periode.fom,
                tom = periode.tom,
                dagsats = periode.dagsats
            )
        }
        HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Ukjent -> {
            ModelSykepengehistorikk.Periode.Ukjent(
                fom = periode.fom,
                tom = periode.tom,
                dagsats = periode.dagsats
            )
        }
    }

    private fun parseInntektsopplysning(
        data: HendelseWrapperData.YtelserData.SykepengehistorikkData.InntektsopplysningData
    ) = ModelSykepengehistorikk.Inntektsopplysning(
        sykepengerFom = data.sykepengerFom,
        inntektPerMåned = data.inntektPerMåned,
        orgnummer = data.orgnummer
    )

    private fun parsePeriode(periodeData: PeriodeData) =
        Periode(periodeData.fom, periodeData.tom)
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
        }

        data class YtelserData(
            val hendelseId: UUID,
            val vedtaksperiodeId: String,
            val orgnummer: String,
            val mottattDato: LocalDateTime,
            val sykepengehistorikk: SykepengehistorikkData,
            val foreldrepenger: ForeldrepengerData
            ) {

            data class SykepengehistorikkData(
                val hendelseId: UUID,
                val orgnummer: String,
                val utbetalinger: List<UtbetalingPeriodeData>,
                val inntektshistorikk: List<InntektsopplysningData>
            ) {
                data class UtbetalingPeriodeData(
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
                        ReduksjonArbeidsgiverRefusjon,
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

            data class ForeldrepengerData(
                val foreldrepengeytelse: PeriodeData,
                val svangerskapsytelse: PeriodeData
            ) {
            }
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
                val type: TypeData,
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
            Ytelser,
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
            val utbetalingslinjer: List<UtbetalingslinjeData>
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

            data class UtbetalingslinjeData(
                val fom: LocalDate,
                val tom: LocalDate,
                val dagsats: Int
            )
        }
    }

    data class PeriodeData(
        val fom: LocalDate,
        val tom: LocalDate
    )
}
