package no.nav.helse.serde

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.serde.PersonData.ArbeidsgiverData
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.serde.mapping.konverterTilAktivitetslogg
import no.nav.helse.serde.mapping.konverterTilAktivitetslogger
import no.nav.helse.serde.migration.*
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal val serdeObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private typealias SykdomstidslinjeData = List<ArbeidsgiverData.VedtaksperiodeData.DagData>

class SerialisertPerson(val json: String) {
    internal companion object {
        private val migrations = listOf(
            V1FjernHendelsetypeEnumFraDag(),
            V2EndreTilstandTyper(),
            V3FjerneUtbetalingsreferanseFraArbeidsgiver(),
            V4LagerAktivitetslogg(),
            V6LeggerTilGrad()
        )

        fun gjeldendeVersjon() = JsonMigration.gjeldendeVersjon(migrations)
        fun medSkjemaversjon(jsonNode: JsonNode) = JsonMigration.medSkjemaversjon(migrations, jsonNode)
    }

    val skjemaVersjon = gjeldendeVersjon()

    private fun migrate(jsonNode: JsonNode) {
        migrations.migrate(jsonNode)
    }

    fun deserialize(): Person {
        val jsonNode = serdeObjectMapper.readTree(json)

        migrate(jsonNode)

        val personData: PersonData = serdeObjectMapper.treeToValue(jsonNode)
        val arbeidsgivere = mutableListOf<Arbeidsgiver>()
        val aktivitetslogger = konverterTilAktivitetslogger(personData.aktivitetslogger)
        val aktivitetslogg = personData.aktivitetslogg?.let(::konverterTilAktivitetslogg) ?: Aktivitetslogg()

        val person = createPerson(
            aktørId = personData.aktørId,
            fødselsnummer = personData.fødselsnummer,
            arbeidsgivere = arbeidsgivere,
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg
        )

        arbeidsgivere.addAll(personData.arbeidsgivere.map { konverterTilArbeidsgiver(person, personData, it) })

        return person
    }

    private fun konverterTilArbeidsgiver(
        person: Person,
        personData: PersonData,
        data: ArbeidsgiverData
    ): Arbeidsgiver {
        val inntekthistorikk = Inntekthistorikk()
        val vedtaksperioder = mutableListOf<Vedtaksperiode>()

        data.inntekter.forEach { inntektData ->
            inntekthistorikk.add(
                fom = inntektData.fom,
                hendelseId = inntektData.hendelseId,
                beløp = inntektData.beløp.setScale(1, RoundingMode.HALF_UP)
            )
        }

        val arbeidsgiver = createArbeidsgiver(
            person = person,
            organisasjonsnummer = data.organisasjonsnummer,
            id = data.id,
            inntekthistorikk = inntekthistorikk,
            tidslinjer = data.utbetalingstidslinjer.map(::konverterTilUtbetalingstidslinje).toMutableList(),
            perioder = vedtaksperioder,
            aktivitetslogger = konverterTilAktivitetslogger(data.aktivitetslogger)
        )

        vedtaksperioder.addAll(data.vedtaksperioder.map {
            parseVedtaksperiode(
                person,
                arbeidsgiver,
                personData,
                data,
                it
            )
        })

        return arbeidsgiver
    }

    private fun konverterTilUtbetalingstidslinje(data: ArbeidsgiverData.UtbetalingstidslinjeData): Utbetalingstidslinje {
        return createUtbetalingstidslinje(data.dager.map {
            when (it.type) {
                ArbeidsgiverData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag -> {
                    Utbetalingsdag.ArbeidsgiverperiodeDag(inntekt = it.inntekt, dato = it.dato, grad = it.grad!!)
                }
                ArbeidsgiverData.UtbetalingstidslinjeData.TypeData.NavDag -> {
                    createNavUtbetalingdag(inntekt = it.inntekt, dato = it.dato, utbetaling = it.utbetaling!!, grad = it.grad!!)
                }
                ArbeidsgiverData.UtbetalingstidslinjeData.TypeData.NavHelgDag -> {
                    Utbetalingsdag.NavHelgDag(inntekt = it.inntekt, dato = it.dato, grad = it.grad!!)
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
                            ArbeidsgiverData.UtbetalingstidslinjeData.BegrunnelseData.EgenmeldingUtenforArbeidsgiverperioden -> Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
                            null -> error("Prøver å deserialisere avvist dag uten begrunnelse")
                        }, grad = Double.NaN
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
        arbeidsgiver: Arbeidsgiver,
        personData: PersonData,
        arbeidsgiverData: ArbeidsgiverData,
        data: ArbeidsgiverData.VedtaksperiodeData
    ): Vedtaksperiode {
        return createVedtaksperiode(
            person = person,
            arbeidsgiver = arbeidsgiver,
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
            sykdomshistorikk = parseSykdomshistorikk(data.sykdomshistorikk),
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
            JsonDagType.ARBEIDSDAG_INNTEKTSMELDING -> Arbeidsdag.Inntektsmelding(data.dagen)
            JsonDagType.ARBEIDSDAG_SØKNAD -> Arbeidsdag.Søknad(data.dagen)
            JsonDagType.EGENMELDINGSDAG_INNTEKTSMELDING -> Egenmeldingsdag.Inntektsmelding(data.dagen)
            JsonDagType.EGENMELDINGSDAG_SØKNAD -> Egenmeldingsdag.Søknad(data.dagen)
            JsonDagType.FERIEDAG_INNTEKTSMELDING -> Feriedag.Inntektsmelding(data.dagen)
            JsonDagType.FERIEDAG_SØKNAD -> Feriedag.Søknad(data.dagen)
            JsonDagType.IMPLISITT_DAG -> ImplisittDag(data.dagen)
            JsonDagType.PERMISJONSDAG_SØKNAD -> Permisjonsdag.Søknad(data.dagen)
            JsonDagType.PERMISJONSDAG_AAREG -> Permisjonsdag.Aareg(data.dagen)
            JsonDagType.STUDIEDAG -> Studiedag(data.dagen)
            JsonDagType.SYKEDAG_SYKMELDING -> Sykedag.Sykmelding(data.dagen, data.grad)
            JsonDagType.SYKEDAG_SØKNAD -> Sykedag.Søknad(data.dagen, data.grad)
            JsonDagType.SYK_HELGEDAG -> SykHelgedag(data.dagen, data.grad)
            JsonDagType.UBESTEMTDAG -> Ubestemtdag(data.dagen)
            JsonDagType.UTENLANDSDAG -> Utenlandsdag(data.dagen)
        }
    }

    private fun parseTilstand(tilstand: TilstandType) = when (tilstand) {
        TilstandType.START -> Vedtaksperiode.StartTilstand
        TilstandType.MOTTATT_SYKMELDING -> Vedtaksperiode.MottattSykmelding
        TilstandType.AVVENTER_SØKNAD -> Vedtaksperiode.AvventerSøknad
        TilstandType.AVVENTER_INNTEKTSMELDING -> Vedtaksperiode.AvventerInntektsmelding
        TilstandType.AVVENTER_VILKÅRSPRØVING -> Vedtaksperiode.AvventerVilkårsprøving
        TilstandType.AVVENTER_HISTORIKK -> Vedtaksperiode.AvventerHistorikk
        TilstandType.AVVENTER_GODKJENNING -> Vedtaksperiode.AvventerGodkjenning
        TilstandType.UNDERSØKER_HISTORIKK -> Vedtaksperiode.UndersøkerHistorikk
        TilstandType.TIL_UTBETALING -> Vedtaksperiode.TilUtbetaling
        TilstandType.UTBETALT -> Vedtaksperiode.Utbetalt
        TilstandType.UTBETALING_FEILET -> Vedtaksperiode.UtbetalingFeilet
        TilstandType.TIL_INFOTRYGD -> Vedtaksperiode.TilInfotrygd
        TilstandType.AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING -> Vedtaksperiode.AvventerTidligerePeriodeEllerInntektsmelding
        TilstandType.AVVENTER_TIDLIGERE_PERIODE -> Vedtaksperiode.AvventerTidligerePeriode
    }

    private fun parseUtbetalingslinje(
        data: ArbeidsgiverData.VedtaksperiodeData.UtbetalingslinjeData
    ): Utbetalingslinje = Utbetalingslinje(fom = data.fom, tom = data.tom, dagsats = data.dagsats)

    private fun parseDataForVilkårsvurdering(
        data: ArbeidsgiverData.VedtaksperiodeData.DataForVilkårsvurderingData
    ): Vilkårsgrunnlag.Grunnlagsdata =
        Vilkårsgrunnlag.Grunnlagsdata(
            erEgenAnsatt = data.erEgenAnsatt,
            beregnetÅrsinntektFraInntektskomponenten = data.beregnetÅrsinntektFraInntektskomponenten,
            avviksprosent = data.avviksprosent,
            harOpptjening = data.harOpptjening,
            antallOpptjeningsdagerErMinst = data.antallOpptjeningsdagerErMinst
        )

    private fun parseSykdomshistorikk(
        data: List<ArbeidsgiverData.VedtaksperiodeData.SykdomshistorikkData>
    ): Sykdomshistorikk {
        return createSykdomshistorikk(data.map { sykdomshistorikkData ->
            createSykdomshistorikkElement(
                timestamp = sykdomshistorikkData.tidsstempel,
                hendelseSykdomstidslinje = parseSykdomstidslinje(sykdomshistorikkData.hendelseSykdomstidslinje),
                beregnetSykdomstidslinje = parseSykdomstidslinje(sykdomshistorikkData.beregnetSykdomstidslinje),
                hendelseId = sykdomshistorikkData.hendelseId
            )
        }
        )
    }

}

internal data class AktivitetsloggerData(
    val originalMessage: String?,
    val aktiviteter: List<AktivitetData>
) {
    data class AktivitetData(
        val alvorlighetsgrad: Alvorlighetsgrad,
        val needType: String?,
        val melding: String,
        val tidsstempel: String
    )

    enum class Alvorlighetsgrad {
        INFO,
        WARN,
        NEED,
        ERROR,
        SEVERE
    }
}

internal data class PersonData(
    val aktørId: String,
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverData>,
    val aktivitetslogger: AktivitetsloggerData,
    val aktivitetslogg: AktivitetsloggData?
) {
    companion object {
        const val skjemaVersjon: Int = 4
    }

    internal data class AktivitetsloggData(
        val aktiviteter: List<AktivitetData>
    ) {
        data class AktivitetData(
            val alvorlighetsgrad: Alvorlighetsgrad,
            val label: Char,
            val needType: String?,
            val melding: String,
            val tidsstempel: String,
            val kontekster: List<SpesifikkKontekstData>
        )

        data class SpesifikkKontekstData(
            val kontekstType: String,
            val melding: String
        )

        enum class Alvorlighetsgrad {
            INFO,
            WARN,
            NEED,
            ERROR,
            SEVERE
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
            val hendelseId: UUID,
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
            val utbetalingslinjer: List<UtbetalingslinjeData>?,
            val aktivitetslogger: AktivitetsloggerData
        ) {
            data class DagData(
                val dagen: LocalDate,
                val type: JsonDagType,
                val grad: Double
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
                MinimumInntekt,
                EgenmeldingUtenforArbeidsgiverperioden
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
                val begrunnelse: BegrunnelseData?,
                val grad: Double?
            )
        }
    }

    data class PeriodeData(
        val fom: LocalDate,
        val tom: LocalDate
    )
}
