package no.nav.helse.serde

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.serde.reflection.createØkonomi
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal data class PersonData(
    val aktørId: String,
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverData>,
    val aktivitetslogg: AktivitetsloggData?
) {

    internal data class AktivitetsloggData(
        val aktiviteter: List<AktivitetData>,
        val kontekster: List<SpesifikkKontekstData>
    ) {
        data class AktivitetData(
            val alvorlighetsgrad: Alvorlighetsgrad,
            val label: Char,
            val behovtype: String?,
            val melding: String,
            val tidsstempel: String,
            val kontekster: List<Int>,
            val detaljer: Map<String, Any>
        )

        data class SpesifikkKontekstData(
            val kontekstType: String,
            val kontekstMap: Map<String, String>
        )

        enum class Alvorlighetsgrad {
            INFO,
            WARN,
            BEHOV,
            ERROR,
            SEVERE
        }
    }

    data class ArbeidsgiverData(
        val organisasjonsnummer: String,
        val id: UUID,
        val inntekter: List<InntektData>,
        val vedtaksperioder: List<VedtaksperiodeData>,
        val forkastede: List<VedtaksperiodeData>,
        val utbetalinger: List<UtbetalingData>
    ) {
        data class InntektData(
            val fom: LocalDate,
            val hendelseId: UUID,
            val beløp: BigDecimal
        )

        data class SykdomstidslinjeData(
            val dager: List<VedtaksperiodeData.DagData>,
            val periode: Periode?,
            val låstePerioder: MutableList<Periode>? = mutableListOf(),
            val id: UUID,
            val tidsstempel: LocalDateTime
        ) {
            val dagerMap: SortedMap<LocalDate, Dag>

            init {
                dagerMap = dager.map { it.dato to parseDag(it) }.toMap(sortedMapOf())
            }

            private fun parseDag(
                data: VedtaksperiodeData.DagData
            ): Dag = when (data.type) {
                JsonDagType.ARBEIDSDAG -> Dag.Arbeidsdag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
                JsonDagType.ARBEIDSGIVERDAG -> Dag.Arbeidsgiverdag(
                    data.dato,
                    data.økonomi,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
                JsonDagType.ARBEIDSGIVER_HELGEDAG -> Dag.ArbeidsgiverHelgedag(
                    data.dato,
                    data.økonomi,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
                JsonDagType.FERIEDAG -> Dag.Feriedag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
                JsonDagType.FRISK_HELGEDAG -> Dag.FriskHelgedag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
                JsonDagType.FORELDET_SYKEDAG -> Dag.ForeldetSykedag(
                    data.dato,
                    data.økonomi,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
                JsonDagType.PERMISJONSDAG -> Dag.Permisjonsdag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
                JsonDagType.PROBLEMDAG -> Dag.ProblemDag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    ),
                    data.melding!!
                )
                JsonDagType.STUDIEDAG -> Dag.Studiedag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
                JsonDagType.SYKEDAG -> Dag.Sykedag(
                    data.dato,
                    data.økonomi,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
                JsonDagType.SYK_HELGEDAG -> Dag.SykHelgedag(
                    data.dato,
                    data.økonomi,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
                JsonDagType.UTENLANDSDAG -> Dag.Utenlandsdag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
                JsonDagType.UKJENT_DAG -> Dag.UkjentDag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(
                        data.kilde.type,
                        data.kilde.id
                    )
                )
            }
        }

        data class VedtaksperiodeData(
            val id: UUID,
            val gruppeId: UUID,
            val maksdato: LocalDate?,
            val gjenståendeSykedager: Int?,
            val forbrukteSykedager: Int?,
            val godkjentAv: String?,
            val godkjenttidspunkt: LocalDateTime?,
            val førsteFraværsdag: LocalDate?,
            val dataForVilkårsvurdering: DataForVilkårsvurderingData?,
            val dataForSimulering: DataForSimuleringData?,
            val sykdomshistorikk: List<SykdomshistorikkData>,
            val tilstand: TilstandType,
            val utbetalingstidslinje: UtbetalingstidslinjeData,
            val personFagsystemId: String?,
            val personNettoBeløp: Int,
            val arbeidsgiverFagsystemId: String?,
            val arbeidsgiverNettoBeløp: Int,
            val forlengelseFraInfotrygd: ForlengelseFraInfotrygd
        ) {
            data class DagData(
                val dato: LocalDate,
                val type: JsonDagType,
                val kilde: KildeData,
                val grad: Double,
                val arbeidsgiverBetalingProsent: Double,
                val aktuellDagsinntekt: Double?,
                val dekningsgrunnlag: Double?,
                val arbeidsgiverbeløp: Int?,
                val personbeløp: Int?,
                val er6GBegrenset: Boolean?,
                val melding: String?
            ) {
                val økonomi get() = createØkonomi(this)
            }

            data class KildeData(
                val type: String,
                val id: UUID
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
                val antallOpptjeningsdagerErMinst: Int,
                val medlemskapstatus: JsonMedlemskapstatus
            )

            data class DataForSimuleringData(
                val totalbeløp: Int,
                val perioder: List<SimulertPeriode>
            ) {
                data class SimulertPeriode(
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val utbetalinger: List<SimulertUtbetaling>
                )

                data class SimulertUtbetaling(
                    val forfallsdato: LocalDate,
                    val utbetalesTil: Mottaker,
                    val feilkonto: Boolean,
                    val detaljer: List<Detaljer>
                )

                data class Detaljer(
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val konto: String,
                    val beløp: Int,
                    val klassekode: Klassekode,
                    val uføregrad: Int,
                    val utbetalingstype: String,
                    val tilbakeføring: Boolean,
                    val sats: Sats,
                    val refunderesOrgnummer: String
                )

                data class Sats(
                    val sats: Int,
                    val antall: Int,
                    val type: String
                )

                data class Klassekode(
                    val kode: String,
                    val beskrivelse: String
                )

                data class Mottaker(
                    val id: String,
                    val navn: String
                )
            }
        }
    }

    data class UtbetalingData(
        val utbetalingstidslinje: UtbetalingstidslinjeData,
        val arbeidsgiverOppdrag: OppdragData,
        val personOppdrag: OppdragData,
        val tidsstempel: LocalDateTime,
        val status: String
    )

    data class OppdragData(
        val mottaker: String,
        val fagområde: String,
        val linjer: List<UtbetalingslinjeData>,
        val fagsystemId: String,
        val endringskode: String,
        val sisteArbeidsgiverdag: LocalDate?,
        val nettoBeløp: Int
    )

    data class UtbetalingslinjeData(
        val fom: LocalDate,
        val tom: LocalDate,
        val dagsats: Int,
        val lønn: Int,
        val grad: Double,
        val refFagsystemId: String?,
        val delytelseId: Int,
        val refDelytelseId: Int?,
        val endringskode: String,
        val klassekode: String,
        val datoStatusFom: LocalDate?
    )

    data class UtbetalingstidslinjeData(
        val dager: List<UtbetalingsdagData>
    ) {
        enum class BegrunnelseData {
            SykepengedagerOppbrukt,
            MinimumInntekt,
            EgenmeldingUtenforArbeidsgiverperiode,
            MinimumSykdomsgrad
        }

        enum class TypeData {
            ArbeidsgiverperiodeDag,
            NavDag,
            NavHelgDag,
            Arbeidsdag,
            Fridag,
            AvvistDag,
            UkjentDag,
            ForeldetDag
        }

        data class UtbetalingsdagData(
            val type: TypeData,
            val dato: LocalDate,
            val aktuellDagsinntekt: Double,
            val dekningsgrunnlag: Double,
            val begrunnelse: BegrunnelseData?,
            val grad: Double?,
            val arbeidsgiverBetalingProsent: Double?,
            val arbeidsgiverbeløp: Int?,
            val personbeløp: Int?,
            val er6GBegrenset: Boolean?
        )
    }
}
