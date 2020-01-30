package no.nav.helse.serde

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelVilkårsgrunnlag
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.PersonData.ArbeidsgiverData
import no.nav.helse.serde.mapping.konverterTilHendelse
import no.nav.helse.serde.reflection.ReflectClass
import no.nav.helse.serde.reflection.ReflectClass.Companion.getNestedClass
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.serde.reflection.create.ReflectionCreationHelper
import no.nav.helse.serde.reflection.createArbeidsgiver
import no.nav.helse.serde.reflection.createPerson
import no.nav.helse.serde.reflection.createSykdomshistorikk
import no.nav.helse.serde.reflection.createSykdomshistorikkElement
import no.nav.helse.serde.reflection.createVedtaksperiode
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.helse.sykdomstidslinje.dag.Feriedag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.JsonDagType
import no.nav.helse.sykdomstidslinje.dag.Permisjonsdag
import no.nav.helse.sykdomstidslinje.dag.Studiedag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.sykdomstidslinje.dag.Ubestemtdag
import no.nav.helse.sykdomstidslinje.dag.Utenlandsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
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
    fun result(): Person {
        val personData: PersonData = objectMapper.readValue(json)
        val hendelser = personData.hendelser.map { konverterTilHendelse(objectMapper, personData, it) }
        val arbeidsgivere = personData.arbeidsgivere.map { konverterTilArbeidsgiver(personData, it, hendelser) }
        val aktivitetslogger = konverterTilAktivitetslogger(personData.aktivitetslogger)

        return createPerson(
            aktørId = personData.aktørId,
            fødselsnummer = personData.fødselsnummer,
            arbeidsgivere = arbeidsgivere.toMutableList(),
            hendelser = hendelser.toMutableList(),
            aktivitetslogger = aktivitetslogger
        )
    }

    private fun konverterTilAktivitetslogger(aktivitetsloggerData: AktivitetsloggerData): Aktivitetslogger {
        val aktivitetslogger = Aktivitetslogger(aktivitetsloggerData.originalMessage)

        val aktivitetClass: ReflectClass = getNestedClass<Aktivitetslogger>("Aktivitet")
        val alvorlighetsgradClass: ReflectClass = getNestedClass<Aktivitetslogger>("Alvorlighetsgrad")

        val aktiviteter = aktivitetslogger.get<Aktivitetslogger, MutableList<Any>>("aktiviteter")
        aktivitetsloggerData.aktiviteter.forEach {
            aktiviteter.add(
                aktivitetClass.getInstance(
                    alvorlighetsgradClass.getEnumValue(it.alvorlighetsgrad.name),
                    it.melding,
                    it.tidsstempel
                )
            )
        }

        return aktivitetslogger
    }


    private fun konverterTilArbeidsgiver(
        personData: PersonData,
        data: ArbeidsgiverData,
        hendelser: List<ArbeidstakerHendelse>
    ): Arbeidsgiver {
        val inntekthistorikk = Inntekthistorikk()
        val vedtaksperioder = data.vedtaksperioder.map { parseVedtaksperiode(personData, data, it, hendelser) }

        data.inntekter.forEach { inntektData ->
            inntekthistorikk.add(
                fom = inntektData.fom,
                hendelse = hendelser.find { it.hendelseId() == inntektData.hendelse } as ModelInntektsmelding,
                beløp = inntektData.beløp
            )
        }

        return createArbeidsgiver(
            data.organisasjonsnummer,
            data.id,
            inntekthistorikk,
            mutableListOf(),
            vedtaksperioder.toMutableList(),
            mutableListOf(),
            Aktivitetslogger()
        )
    }

    private fun parseVedtaksperiode(
        personData: PersonData,
        arbeidsgiverData: ArbeidsgiverData,
        data: ArbeidsgiverData.VedtaksperiodeData,
        hendelser: List<ArbeidstakerHendelse>
    ): Vedtaksperiode {
        return createVedtaksperiode(
            id = data.id,
            aktørId = personData.aktørId,
            fødselsnummer = personData.fødselsnummer,
            organisasjonsnummer = arbeidsgiverData.organisasjonsnummer,
            sykdomstidslinje = parseSykdomstidslinje(data.sykdomstidslinje, hendelser),
            tilstand = parseTilstand(data.tilstand),
            maksdato = data.maksdato,
            utbetalingslinjer = data.utbetalingslinjer.map(::parseUtbetalingslinje),
            godkjentAv = data.godkjentAv,
            utbetalingsreferanse = data.utbetalingsreferanse,
            førsteFraværsdag = data.førsteFraværsdag,
            inntektFraInntektsmelding = data.inntektFraInntektsmelding?.toDouble(),
            dataForVilkårsvurdering = data.dataForVilkårsvurdering?.let(::parseDataForVilkårsvurdering),
            sykdomshistorikk = parseSykdomshistorikk(data.sykdomshistorikk, hendelser),
            aktivitetslogger =  konverterTilAktivitetslogger(data.aktivitetslogger)
        )
    }

    private fun parseSykdomstidslinje(
        tidslinjeData: SykdomstidslinjeData,
        hendelser: List<ArbeidstakerHendelse>
    ): CompositeSykdomstidslinje = CompositeSykdomstidslinje(tidslinjeData.map{ parseDag(it, hendelser) })

    private fun parseDag(data: ArbeidsgiverData.VedtaksperiodeData.DagData, hendelser: List<ArbeidstakerHendelse>): Dag {
        val hendelse = hendelser.find { it.hendelseId() == data.hendelseId } as SykdomstidslinjeHendelse
        val dag: Dag = when (data.type) {
            JsonDagType.ARBEIDSDAG -> Arbeidsdag(data.dagen, hendelse)
            JsonDagType.EGENMELDINGSDAG -> Egenmeldingsdag(data.dagen, hendelse)
            JsonDagType.FERIEDAG -> Feriedag(data.dagen, hendelse)
            JsonDagType.IMPLISITT_DAG -> ImplisittDag(data.dagen, hendelse)
            JsonDagType.PERMISJONSDAG -> Permisjonsdag(data.dagen, hendelse)
            JsonDagType.STUDIEDAG -> Studiedag(data.dagen, hendelse)
            JsonDagType.SYKEDAG -> Sykedag(data.dagen, hendelse)
            JsonDagType.SYK_HELGEDAG -> SykHelgedag(data.dagen, hendelse)
            JsonDagType.UBESTEMTDAG -> Ubestemtdag(data.dagen, hendelse)
            JsonDagType.UTENLANDSDAG -> Utenlandsdag(data.dagen, hendelse)
        }

        val erstatter = data.erstatter.map { parseDag(it, hendelser) }
        dag.erstatter.addAll(erstatter)
        return dag
    }

    private fun parseTilstand(tilstand: TilstandType) = when (tilstand) {
        TilstandType.START -> Vedtaksperiode.StartTilstand
        TilstandType.MOTTATT_NY_SØKNAD -> Vedtaksperiode.MottattNySøknad
        TilstandType.MOTTATT_SENDT_SØKNAD -> Vedtaksperiode.MottattSendtSøknad
        TilstandType.MOTTATT_INNTEKTSMELDING -> Vedtaksperiode.MottattInntektsmelding
        TilstandType.VILKÅRSPRØVING -> Vedtaksperiode.Vilkårsprøving
        TilstandType.BEREGN_UTBETALING -> Vedtaksperiode.BeregnUtbetaling
        TilstandType.TIL_GODKJENNING -> Vedtaksperiode.TilGodkjenning
        TilstandType.TIL_UTBETALING -> Vedtaksperiode.TilUtbetaling
        TilstandType.TIL_INFOTRYGD -> Vedtaksperiode.TilInfotrygd
    }

    private fun parseUtbetalingslinje(
        data: ArbeidsgiverData.VedtaksperiodeData.UtbetalingslinjeData
    ): Utbetalingslinje = Utbetalingslinje(fom = data.fom, tom = data.tom, dagsats = data.dagsats)

    private fun parseDataForVilkårsvurdering(
        data: ArbeidsgiverData.VedtaksperiodeData.DataForVilkårsvurderingData
    ): ModelVilkårsgrunnlag.Grunnlagsdata =
        ModelVilkårsgrunnlag.Grunnlagsdata(
            erEgenAnsatt = data.erEgenAnsatt,
            beregnetÅrsinntektFraInntektskomponenten = data.beregnetÅrsinntektFraInntektskomponenten,
            avviksprosent = data.avviksprosent
        )

    private fun parseSykdomshistorikk(
        data: List<ArbeidsgiverData.VedtaksperiodeData.SykdomshistorikkData>,
        hendelser: List<ArbeidstakerHendelse>
    ): Sykdomshistorikk {
        return createSykdomshistorikk(data.map { sykdomshistorikkData ->
            createSykdomshistorikkElement(
                timestamp = sykdomshistorikkData.tidsstempel,
                hendelseSykdomstidslinje = parseSykdomstidslinje(sykdomshistorikkData.hendelseSykdomstidslinje, hendelser),
                beregnetSykdomstidslinje = parseSykdomstidslinje(sykdomshistorikkData.beregnetSykdomstidslinje, hendelser),
                hendelse = hendelser.find { it.hendelseId() == sykdomshistorikkData.hendelseId } as SykdomstidslinjeHendelse) }
        )
    }

}

internal data class AktivitetsloggerData(
    val originalMessage: String?,
    val aktiviteter: List<AktivitetData>
) {
    data class AktivitetData(
        val alvorlighetsgrad: Alvorlighetsgrad,
        val melding: String,
        val tidsstempel: String
    )

    enum class Alvorlighetsgrad {
        INFO,
        WARN,
        ERROR,
        SEVERE
    }
}

internal data class PersonData(
    val skjemaVersjon: Int = 1,
    val aktørId: String,
    val fødselsnummer: String,
    val hendelser: List<HendelseWrapperData>,
    val arbeidsgivere: List<ArbeidsgiverData>,
    val aktivitetslogger: AktivitetsloggerData
) {
    data class HendelseWrapperData(
        val type: Hendelsestype,
        //val tidspunkt: LocalDateTime,
        val data: Map<String, Any?>
    ) {
        data class InntektsmeldingData(
            val hendelseId: UUID,
            val fødselsnummer: String,
            val aktørId: String,
            val orgnummer: String,
            val refusjon: RefusjonData,
            val mottattDato: LocalDateTime,
            val førsteFraværsdag: LocalDate,
            val beregnetInntekt: Double,
            val arbeidsgiverperioder: List<PeriodeData>,
            val ferieperioder: List<PeriodeData>,
            val aktivitetslogger: AktivitetsloggerData
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
            val vedtaksperiodeId: UUID,
            val organisasjonsnummer: String,
            val rapportertdato: LocalDateTime,
            val sykepengehistorikk: SykepengehistorikkData,
            val foreldrepenger: ForeldrepengerData,
            val aktivitetslogger: AktivitetsloggerData
        ) {

            data class SykepengehistorikkData(
                val utbetalinger: List<UtbetalingPeriodeData>,
                val inntektshistorikk: List<InntektsopplysningData>,
                val aktivitetslogger: AktivitetsloggerData
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
            val vedtaksperiodeId: UUID,
            val organisasjonsnummer: String,
            val saksbehandler: String,
            val utbetalingGodkjent: Boolean,
            val rapportertdato: LocalDateTime,
            val aktivitetslogger: AktivitetsloggerData
        ) {
        }

        data class NySøknadData(
            val fnr: String,
            val aktørId: String,
            val hendelseId: UUID,
            val orgnummer: String,
            val rapportertdato: LocalDateTime,
            val sykeperioder: List<SykeperiodeData>,
            val aktivitetslogger: AktivitetsloggerData
        ) {
            data class SykeperiodeData(
                val fom: LocalDate,
                val tom: LocalDate,
                val sykdomsgrad: Int
            )
        }

        data class SendtSøknadData(
            val fnr: String,
            val aktørId: String,
            val hendelseId: UUID,
            val orgnummer: String,
            val rapportertdato: LocalDateTime,
            val perioder: List<SykeperiodeData>,
            val aktivitetslogger: AktivitetsloggerData
        ) {
            data class SykeperiodeData(
                val type: TypeData,
                val fom: LocalDate,
                val tom: LocalDate,
                val grad: Int?,
                val faktiskGrad: Double?
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
            val vedtaksperiodeId: UUID,
            val orgnummer: String,
            val rapportertDato: LocalDateTime,
            val inntektsmåneder: List<Måned>,
            val erEgenAnsatt: Boolean,
            val aktivitetslogger: AktivitetsloggerData
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
        val vedtaksperioder: List<VedtaksperiodeData>,
        val utbetalingstidslinjer: List<Any>, // TODO
        val aktivitetslogger: AktivitetsloggerData
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
            val utbetalingslinjer: List<UtbetalingslinjeData>,
            val aktivitetslogger: AktivitetsloggerData
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
