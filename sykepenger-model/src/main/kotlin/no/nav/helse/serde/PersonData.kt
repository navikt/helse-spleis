package no.nav.helse.serde

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelser.ModelVilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.serde.PersonData.ArbeidsgiverData
import no.nav.helse.serde.mapping.konverterTilAktivitetslogger
import no.nav.helse.serde.mapping.konverterTilHendelse
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

internal fun parseJson(json: String): PersonData = objectMapper.readValue(json)

private typealias SykdomstidslinjeData = List<ArbeidsgiverData.VedtaksperiodeData.DagData>

fun parsePerson(json: String): Person {
    val personData: PersonData = objectMapper.readValue(json)
    val hendelser = personData.hendelser.map { konverterTilHendelse(objectMapper, personData, it) }
    val arbeidsgivere = mutableListOf<Arbeidsgiver>()
    val aktivitetslogger = konverterTilAktivitetslogger(personData.aktivitetslogger)

    val person = createPerson(
        aktørId = personData.aktørId,
        fødselsnummer = personData.fødselsnummer,
        arbeidsgivere = arbeidsgivere,
        hendelser = hendelser.toMutableList(),
        aktivitetslogger = aktivitetslogger
    )

    arbeidsgivere.addAll(personData.arbeidsgivere.map { konverterTilArbeidsgiver(person, personData, it, hendelser) })

    return person
}

private fun konverterTilArbeidsgiver(
    person: Person,
    personData: PersonData,
    data: ArbeidsgiverData,
    hendelser: List<ArbeidstakerHendelse>
): Arbeidsgiver {
    val inntekthistorikk = Inntekthistorikk()
    val vedtaksperioder = data.vedtaksperioder.map { parseVedtaksperiode(person, personData, data, it, hendelser) }

    data.inntekter.forEach { inntektData ->
        inntekthistorikk.add(
            fom = inntektData.fom,
            hendelse = requireNotNull(hendelser.find { it.hendelseId() == inntektData.hendelse }),
            beløp = inntektData.beløp.setScale(1, RoundingMode.HALF_UP)
        )
    }

    return createArbeidsgiver(
        director = person,
        organisasjonsnummer = data.organisasjonsnummer,
        id = data.id,
        inntekthistorikk = inntekthistorikk,
        tidslinjer = data.utbetalingstidslinjer.map(::konverterTilUtbetalingstidslinje).toMutableList(),
        perioder = vedtaksperioder.toMutableList(),
        aktivitetslogger = konverterTilAktivitetslogger(data.aktivitetslogger)
    )
}

private fun konverterTilUtbetalingstidslinje(data: ArbeidsgiverData.UtbetalingstidslinjeData): Utbetalingstidslinje {
    return createUtbetalingstidslinje(data.dager.map {
        when (it.type) {
            ArbeidsgiverData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag -> {
                Utbetalingsdag.ArbeidsgiverperiodeDag(inntekt = it.inntekt, dato = it.dato)
            }
            ArbeidsgiverData.UtbetalingstidslinjeData.TypeData.NavDag -> {
                createNavUtbetalingdag(inntekt = it.inntekt, dato = it.dato, utbetaling = it.utbetaling!!)
            }
            ArbeidsgiverData.UtbetalingstidslinjeData.TypeData.NavHelgDag -> {
                Utbetalingsdag.NavHelgDag(inntekt = it.inntekt, dato = it.dato)
            }
            ArbeidsgiverData.UtbetalingstidslinjeData.TypeData.Arbeidsdag -> {
                Utbetalingsdag.Arbeidsdag(inntekt = it.inntekt, dato = it.dato)
            }
            ArbeidsgiverData.UtbetalingstidslinjeData.TypeData.Fridag -> {
                Utbetalingsdag.Fridag(inntekt = it.inntekt, dato = it.dato)
            }
            ArbeidsgiverData.UtbetalingstidslinjeData.TypeData.AvvistDag -> {
                Utbetalingsdag.AvvistDag(
                    inntekt = it.inntekt, dato = it.dato, begrunnelse = when (it.begrunnelse) {
                        ArbeidsgiverData.UtbetalingstidslinjeData.BegrunnelseData.SykepengedagerOppbrukt -> Begrunnelse.SykepengedagerOppbrukt
                        ArbeidsgiverData.UtbetalingstidslinjeData.BegrunnelseData.MinimumInntekt -> Begrunnelse.MinimumInntekt
                        null -> error("Prøver å deserialisere avvist dag uten begrunnelse")
                    }
                )
            }
            ArbeidsgiverData.UtbetalingstidslinjeData.TypeData.UkjentDag -> {
                Utbetalingsdag.UkjentDag(inntekt = it.inntekt, dato = it.dato)
            }
        }
    }
        .toMutableList())
}

private fun parseVedtaksperiode(
    person: Person,
    personData: PersonData,
    arbeidsgiverData: ArbeidsgiverData,
    data: ArbeidsgiverData.VedtaksperiodeData,
    hendelser: List<ArbeidstakerHendelse>
): Vedtaksperiode {
    return createVedtaksperiode(
        director = person,
        id = data.id,
        aktørId = personData.aktørId,
        fødselsnummer = personData.fødselsnummer,
        organisasjonsnummer = arbeidsgiverData.organisasjonsnummer,
        tilstand = parseTilstand(data.tilstand),
        maksdato = data.maksdato,
        utbetalingslinjer = data.utbetalingslinjer?.map(::parseUtbetalingslinje),
        godkjentAv = data.godkjentAv,
        utbetalingsreferanse = data.utbetalingsreferanse,
        førsteFraværsdag = data.førsteFraværsdag,
        inntektFraInntektsmelding = data.inntektFraInntektsmelding?.toDouble(),
        dataForVilkårsvurdering = data.dataForVilkårsvurdering?.let(::parseDataForVilkårsvurdering),
        sykdomshistorikk = parseSykdomshistorikk(data.sykdomshistorikk, hendelser),
        aktivitetslogger = konverterTilAktivitetslogger(data.aktivitetslogger)
    )
}

private fun parseSykdomstidslinje(
    tidslinjeData: SykdomstidslinjeData
): CompositeSykdomstidslinje = CompositeSykdomstidslinje(tidslinjeData.map(::parseDag))

private fun parseDag(
    data: ArbeidsgiverData.VedtaksperiodeData.DagData
): Dag {
    return when (data.type) {
        JsonDagType.ARBEIDSDAG -> Arbeidsdag(data.dagen, data.hendelseType)
        JsonDagType.EGENMELDINGSDAG -> Egenmeldingsdag(data.dagen, data.hendelseType)
        JsonDagType.FERIEDAG -> Feriedag(data.dagen, data.hendelseType)
        JsonDagType.IMPLISITT_DAG -> ImplisittDag(data.dagen, data.hendelseType)
        JsonDagType.PERMISJONSDAG -> Permisjonsdag(data.dagen, data.hendelseType)
        JsonDagType.STUDIEDAG -> Studiedag(data.dagen, data.hendelseType)
        JsonDagType.SYKEDAG -> Sykedag(data.dagen, data.hendelseType)
        JsonDagType.SYK_HELGEDAG -> SykHelgedag(data.dagen, data.hendelseType)
        JsonDagType.UBESTEMTDAG -> Ubestemtdag(data.dagen, data.hendelseType)
        JsonDagType.UTENLANDSDAG -> Utenlandsdag(data.dagen, data.hendelseType)
    }
}

private fun parseTilstand(tilstand: TilstandTypeGammelOgNy) = when (tilstand) {
    TilstandTypeGammelOgNy.START -> Vedtaksperiode.StartTilstand
    TilstandTypeGammelOgNy.MOTTATT_NY_SØKNAD -> Vedtaksperiode.MottattNySøknad
    TilstandTypeGammelOgNy.AVVENTER_SENDT_SØKNAD,
    TilstandTypeGammelOgNy.MOTTATT_SENDT_SØKNAD -> Vedtaksperiode.AvventerSendtSøknad
    TilstandTypeGammelOgNy.AVVENTER_INNTEKTSMELDING,
    TilstandTypeGammelOgNy.MOTTATT_INNTEKTSMELDING-> Vedtaksperiode.AvventerInntektsmelding
    TilstandTypeGammelOgNy.AVVENTER_VILKÅRSPRØVING,
    TilstandTypeGammelOgNy.VILKÅRSPRØVING -> Vedtaksperiode.AvventerVilkårsprøving
    TilstandTypeGammelOgNy.AVVENTER_HISTORIKK,
    TilstandTypeGammelOgNy.BEREGN_UTBETALING -> Vedtaksperiode.AvventerHistorikk
    TilstandTypeGammelOgNy.AVVENTER_GODKJENNING,
    TilstandTypeGammelOgNy.TIL_GODKJENNING -> Vedtaksperiode.AvventerGodkjenning
    TilstandTypeGammelOgNy.UNDERSØKER_HISTORIKK -> Vedtaksperiode.UndersøkerHistorikk
    TilstandTypeGammelOgNy.TIL_UTBETALING -> Vedtaksperiode.TilUtbetaling
    TilstandTypeGammelOgNy.TIL_INFOTRYGD -> Vedtaksperiode.TilInfotrygd
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
        avviksprosent = data.avviksprosent,
        harOpptjening = data.harOpptjening,
        antallOpptjeningsdagerErMinst = data.antallOpptjeningsdagerErMinst
    )

private fun parseSykdomshistorikk(
    data: List<ArbeidsgiverData.VedtaksperiodeData.SykdomshistorikkData>,
    hendelser: List<ArbeidstakerHendelse>
): Sykdomshistorikk {
    return createSykdomshistorikk(data.map { sykdomshistorikkData ->
        createSykdomshistorikkElement(
            timestamp = sykdomshistorikkData.tidsstempel,
            hendelseSykdomstidslinje = parseSykdomstidslinje(sykdomshistorikkData.hendelseSykdomstidslinje),
            beregnetSykdomstidslinje = parseSykdomstidslinje(sykdomshistorikkData.beregnetSykdomstidslinje),
            hendelse = hendelser.find { it.hendelseId() == sykdomshistorikkData.hendelseId } as SykdomstidslinjeHendelse)
    }
    )
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
        HELP,
        ERROR,
        SEVERE
    }
}

internal data class PersonData(
    val aktørId: String,
    val fødselsnummer: String,
    val hendelser: List<HendelseWrapperData>,
    val arbeidsgivere: List<ArbeidsgiverData>,
    val aktivitetslogger: AktivitetsloggerData
) {
    companion object {
        const val skjemaVersjon: Int = 4
    }

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
                val foreldrepengeytelse: PeriodeData?,
                val svangerskapsytelse: PeriodeData?
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
            val sendtNav: LocalDateTime,
            val perioder: List<SykeperiodeData>,
            val aktivitetslogger: AktivitetsloggerData,
            val harAndreInntektskilder: Boolean
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
            val arbeidsforhold: List<ArbeidsforholdData>?,
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
            data class ArbeidsforholdData(
                val orgnummer: String,
                val fom: LocalDate,
                val tom: LocalDate?
            )
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
        val utbetalingstidslinjer: List<UtbetalingstidslinjeData>,
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
            val tilstand: TilstandTypeGammelOgNy,
            val utbetalingslinjer: List<UtbetalingslinjeData>?,
            val aktivitetslogger: AktivitetsloggerData
        ) {
            data class DagData(
                val dagen: LocalDate,
                val hendelseType: Dag.NøkkelHendelseType,
                val type: JsonDagType
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
                val avviksprosent: Double,
                val harOpptjening: Boolean,
                val antallOpptjeningsdagerErMinst: Int
            )

            data class UtbetalingslinjeData(
                val fom: LocalDate,
                val tom: LocalDate,
                val dagsats: Int
            )
        }

        data class UtbetalingstidslinjeData(
            val dager: List<UtbetalingsdagData>
        ) {
            enum class BegrunnelseData {
                SykepengedagerOppbrukt,
                MinimumInntekt
            }

            enum class TypeData {
                ArbeidsgiverperiodeDag,
                NavDag,
                NavHelgDag,
                Arbeidsdag,
                Fridag,
                AvvistDag,
                UkjentDag
            }

            data class UtbetalingsdagData(
                val type: TypeData,
                val dato: LocalDate,
                val inntekt: Double,
                val utbetaling: Int?,
                val begrunnelse: BegrunnelseData?
            )
        }
    }

    data class PeriodeData(
        val fom: LocalDate,
        val tom: LocalDate
    )
}
