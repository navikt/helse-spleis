package no.nav.helse.serde

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.person.Vedtaksperiode.*
import no.nav.helse.serde.PersonData.ArbeidsgiverData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.SykdomstidslinjeData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.DagData
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.serde.mapping.JsonDagType.*
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.serde.mapping.konverterTilAktivitetslogg
import no.nav.helse.serde.migration.*
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Begrunnelse
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

class SerialisertPerson(val json: String) {
    internal companion object {
        private val migrations = listOf(
            V1EndreKunArbeidsgiverSykedagEnum(),
            V2Medlemskapstatus(),
            V3BeregnerGjenståendeSykedagerFraMaksdato(),
            V4LeggTilNySykdomstidslinje(),
            V5BegrensGradTilMellom0Og100(),
            V6LeggTilNySykdomstidslinje(),
            V7DagsatsSomHeltall(),
            V8LeggerTilLønnIUtbetalingslinjer(),
            V9FjernerGamleSykdomstidslinjer(),
            V10EndreNavnPåSykdomstidslinjer(),
            V11LeggeTilForlengelseFraInfotrygd(),
            V12Aktivitetslogg()
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
        val aktivitetslogg = personData.aktivitetslogg?.let(::konverterTilAktivitetslogg) ?: Aktivitetslogg()

        val person = createPerson(
            aktørId = personData.aktørId,
            fødselsnummer = personData.fødselsnummer,
            arbeidsgivere = arbeidsgivere,
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
            perioder = vedtaksperioder,
            utbetalinger = data.utbetalinger.map(::konverterTilUtbetaling).toMutableList()
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
        Vedtaksperiode.sorter(vedtaksperioder)

        return arbeidsgiver
    }

    private fun konverterTilUtbetaling(data: UtbetalingData) = createUtbetaling(
        konverterTilUtbetalingstidslinje(data.utbetalingstidslinje),
        konverterTilOppdrag(data.arbeidsgiverOppdrag),
        konverterTilOppdrag(data.personOppdrag),
        data.tidsstempel
    )

    private fun konverterTilOppdrag(data: OppdragData): Oppdrag {
        return createOppdrag(
            data.mottaker,
            Fagområde.valueOf(data.fagområde),
            data.linjer.map(::konverterTilUtbetalingslinje),
            data.fagsystemId,
            Endringskode.valueOf(data.endringskode),
            data.sisteArbeidsgiverdag
        )
    }

    private fun konverterTilUtbetalingstidslinje(data: UtbetalingstidslinjeData): Utbetalingstidslinje {
        return createUtbetalingstidslinje(data.dager.map {
            when (it.type) {
                UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag -> {
                    Utbetalingsdag.ArbeidsgiverperiodeDag(dagsats = it.dagsats, dato = it.dato)
                }
                UtbetalingstidslinjeData.TypeData.NavDag -> {
                    createNavUtbetalingdag(
                        inntekt = it.dagsats,
                        dato = it.dato,
                        utbetaling = it.utbetaling!!,
                        grad = it.grad!!
                    )
                }
                UtbetalingstidslinjeData.TypeData.NavHelgDag -> {
                    Utbetalingsdag.NavHelgDag(dagsats = it.dagsats, dato = it.dato, grad = it.grad!!)
                }
                UtbetalingstidslinjeData.TypeData.Arbeidsdag -> {
                    Utbetalingsdag.Arbeidsdag(dagsats = it.dagsats, dato = it.dato)
                }
                UtbetalingstidslinjeData.TypeData.Fridag -> {
                    Utbetalingsdag.Fridag(dagsats = it.dagsats, dato = it.dato)
                }
                UtbetalingstidslinjeData.TypeData.AvvistDag -> {
                    Utbetalingsdag.AvvistDag(
                        dagsats = it.dagsats, dato = it.dato, begrunnelse = when (it.begrunnelse) {
                            UtbetalingstidslinjeData.BegrunnelseData.SykepengedagerOppbrukt -> Begrunnelse.SykepengedagerOppbrukt
                            UtbetalingstidslinjeData.BegrunnelseData.MinimumInntekt -> Begrunnelse.MinimumInntekt
                            UtbetalingstidslinjeData.BegrunnelseData.EgenmeldingUtenforArbeidsgiverperiode -> Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
                            UtbetalingstidslinjeData.BegrunnelseData.MinimumSykdomsgrad -> Begrunnelse.MinimumSykdomsgrad
                            null -> error("Prøver å deserialisere avvist dag uten begrunnelse")
                        }, grad = Double.NaN
                    )
                }
                UtbetalingstidslinjeData.TypeData.UkjentDag -> {
                    Utbetalingsdag.UkjentDag(dagsats = it.dagsats, dato = it.dato)
                }
                UtbetalingstidslinjeData.TypeData.ForeldetDag -> {
                    Utbetalingsdag.ForeldetDag(dagsats = it.dagsats, dato = it.dato)
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
            gruppeId = data.gruppeId,
            aktørId = personData.aktørId,
            fødselsnummer = personData.fødselsnummer,
            organisasjonsnummer = arbeidsgiverData.organisasjonsnummer,
            tilstand = parseTilstand(data.tilstand),
            maksdato = data.maksdato,
            gjenståendeSykedager = data.gjenståendeSykedager,
            forbrukteSykedager = data.forbrukteSykedager,
            godkjentAv = data.godkjentAv,
            godkjenttidspunkt = data.godkjenttidspunkt,
            førsteFraværsdag = data.førsteFraværsdag,
            dataForSimulering = data.dataForSimulering?.let(::parseDataForSimulering),
            dataForVilkårsvurdering = data.dataForVilkårsvurdering?.let(::parseDataForVilkårsvurdering),
            sykdomshistorikk = parseSykdomshistorikk(data.sykdomshistorikk),
            utbetalingstidslinje = konverterTilUtbetalingstidslinje(data.utbetalingstidslinje),
            personFagsystemId = data.personFagsystemId,
            arbeidsgiverFagsystemId = data.arbeidsgiverFagsystemId,
            forlengelseFraInfotrygd = data.forlengelseFraInfotrygd
        )
    }

    private fun parseSykdomstidslinje(
        tidslinjeData: SykdomstidslinjeData
    ): Sykdomstidslinje = createSykdomstidslinje(tidslinjeData)

    private fun parseTilstand(tilstand: TilstandType) = when (tilstand) {
        TilstandType.AVVENTER_HISTORIKK -> AvventerHistorikk
        TilstandType.AVVENTER_GODKJENNING -> AvventerGodkjenning
        TilstandType.AVVENTER_SIMULERING -> AvventerSimulering
        TilstandType.TIL_UTBETALING -> TilUtbetaling
        TilstandType.AVSLUTTET -> Avsluttet
        TilstandType.AVSLUTTET_UTEN_UTBETALING -> AvsluttetUtenUtbetaling
        TilstandType.AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING -> AvsluttetUtenUtbetalingMedInntektsmelding
        TilstandType.UTBETALING_FEILET -> UtbetalingFeilet
        TilstandType.TIL_INFOTRYGD -> TilInfotrygd
        TilstandType.START -> Start
        TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE -> MottattSykmeldingFerdigForlengelse
        TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE -> MottattSykmeldingUferdigForlengelse
        TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP -> MottattSykmeldingFerdigGap
        TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP -> MottattSykmeldingUferdigGap
        TilstandType.AVVENTER_SØKNAD_FERDIG_GAP -> AvventerSøknadFerdigGap
        TilstandType.AVVENTER_VILKÅRSPRØVING_GAP -> AvventerVilkårsprøvingGap
        TilstandType.AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD -> AvventerVilkårsprøvingArbeidsgiversøknad
        TilstandType.AVVENTER_GAP -> AvventerGap
        TilstandType.AVVENTER_SØKNAD_UFERDIG_GAP -> AvventerSøknadUferdigGap
        TilstandType.AVVENTER_INNTEKTSMELDING_FERDIG_GAP -> AvventerInntektsmeldingFerdigGap
        TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP -> AvventerInntektsmeldingUferdigGap
        TilstandType.AVVENTER_UFERDIG_GAP -> AvventerUferdigGap
        TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE -> AvventerInntektsmeldingUferdigForlengelse
        TilstandType.AVVENTER_SØKNAD_UFERDIG_FORLENGELSE -> AvventerSøknadUferdigForlengelse
        TilstandType.AVVENTER_UFERDIG_FORLENGELSE -> AvventerUferdigForlengelse
    }

    private fun konverterTilUtbetalingslinje(
        data: UtbetalingslinjeData
    ): Utbetalingslinje = createUtbetalingslinje(
        data.fom,
        data.tom,
        data.dagsats,
        data.lønn,
        data.grad,
        data.refFagsystemId,
        data.delytelseId,
        data.refDelytelseId,
        Endringskode.valueOf(data.endringskode),
        Klassekode.from(data.klassekode),
        data.datoStatusFom
    )

    private fun parseDataForVilkårsvurdering(
        data: ArbeidsgiverData.VedtaksperiodeData.DataForVilkårsvurderingData
    ): Vilkårsgrunnlag.Grunnlagsdata =
        Vilkårsgrunnlag.Grunnlagsdata(
            erEgenAnsatt = data.erEgenAnsatt,
            beregnetÅrsinntektFraInntektskomponenten = data.beregnetÅrsinntektFraInntektskomponenten,
            avviksprosent = data.avviksprosent,
            harOpptjening = data.harOpptjening,
            antallOpptjeningsdagerErMinst = data.antallOpptjeningsdagerErMinst,
            medlemskapstatus = when (data.medlemskapstatus) {
                JsonMedlemskapstatus.JA -> Medlemskapsvurdering.Medlemskapstatus.Ja
                JsonMedlemskapstatus.NEI -> Medlemskapsvurdering.Medlemskapstatus.Nei
                else -> Medlemskapsvurdering.Medlemskapstatus.VetIkke
            }
        )

    private fun parseDataForSimulering(
        data: ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData
    ) = Simulering.SimuleringResultat(
        totalbeløp = data.totalbeløp,
        perioder = data.perioder.map { periode ->
            Simulering.SimulertPeriode(
                periode = Periode(periode.fom, periode.tom),
                utbetalinger = periode.utbetalinger.map { utbetaling ->
                    Simulering.SimulertUtbetaling(
                        forfallsdato = utbetaling.forfallsdato,
                        utbetalesTil = Simulering.Mottaker(
                            id = utbetaling.utbetalesTil.id,
                            navn = utbetaling.utbetalesTil.navn
                        ),
                        feilkonto = utbetaling.feilkonto,
                        detaljer = utbetaling.detaljer.map { detalj ->
                            Simulering.Detaljer(
                                periode = Periode(detalj.fom, detalj.tom),
                                konto = detalj.konto,
                                beløp = detalj.beløp,
                                klassekode = Simulering.Klassekode(
                                    kode = detalj.klassekode.kode,
                                    beskrivelse = detalj.klassekode.beskrivelse
                                ),
                                uføregrad = detalj.uføregrad,
                                utbetalingstype = detalj.utbetalingstype,
                                tilbakeføring = detalj.tilbakeføring,
                                sats = Simulering.Sats(
                                    sats = detalj.sats.sats,
                                    antall = detalj.sats.antall,
                                    type = detalj.sats.type
                                ),
                                refunderesOrgnummer = detalj.refunderesOrgnummer
                            )
                        }
                    )
                }
            )
        }
    )

    private fun parseSykdomshistorikk(
        data: List<ArbeidsgiverData.VedtaksperiodeData.SykdomshistorikkData>
    ): Sykdomshistorikk {
        return createSykdomshistorikk(data.map { sykdomshistorikkData ->
            createSykdomshistorikkElement(
                timestamp = sykdomshistorikkData.tidsstempel,
                hendelseId = sykdomshistorikkData.hendelseId,
                hendelseSykdomstidslinje = parseSykdomstidslinje(sykdomshistorikkData.hendelseSykdomstidslinje),
                beregnetSykdomstidslinje = parseSykdomstidslinje(sykdomshistorikkData.beregnetSykdomstidslinje)
            )
        })
    }

}

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
        val utbetalinger: List<UtbetalingData>
    ) {
        data class InntektData(
            val fom: LocalDate,
            val hendelseId: UUID,
            val beløp: BigDecimal
        )

        data class SykdomstidslinjeData(
            val dager: List<DagData>,
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
                data: DagData
            ): Dag = when (data.type) {
                ARBEIDSDAG -> Arbeidsdag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id)
                )
                ARBEIDSGIVERDAG -> Arbeidsgiverdag(
                    data.dato,
                    data.grad,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id)
                )
                ARBEIDSGIVER_HELGEDAG -> ArbeidsgiverHelgedag(
                    data.dato,
                    data.grad,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id)
                )
                FERIEDAG -> Feriedag(data.dato, SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id))
                FRISK_HELGEDAG -> FriskHelgedag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id)
                )
                FORELDET_SYKEDAG -> ForeldetSykedag(
                    data.dato,
                    data.grad,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id)
                )
                PERMISJONSDAG -> Permisjonsdag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id)
                )
                PROBLEMDAG -> ProblemDag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id),
                    data.melding!!
                )
                STUDIEDAG -> Studiedag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id)
                )
                SYKEDAG -> Sykedag(
                    data.dato,
                    data.grad,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id)
                )
                SYK_HELGEDAG -> SykHelgedag(
                    data.dato,
                    data.grad,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id)
                )
                UTENLANDSDAG -> Utenlandsdag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id)
                )
                UKJENT_DAG -> UkjentDag(
                    data.dato,
                    SykdomstidslinjeHendelse.Hendelseskilde(data.kilde.type, data.kilde.id)
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
            val arbeidsgiverFagsystemId: String?,
            val forlengelseFraInfotrygd: ForlengelseFraInfotrygd
        ) {
            data class DagData(
                val dato: LocalDate,
                val type: JsonDagType,
                val kilde: KildeData,
                val grad: Double,
                val melding: String?
            )

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
}

data class UtbetalingData(
    val utbetalingstidslinje: UtbetalingstidslinjeData,
    val arbeidsgiverOppdrag: OppdragData,
    val personOppdrag: OppdragData,
    val tidsstempel: LocalDateTime
)

data class OppdragData(
    val mottaker: String,
    val fagområde: String,
    val linjer: List<UtbetalingslinjeData>,
    val fagsystemId: String,
    val endringskode: String,
    val sisteArbeidsgiverdag: LocalDate?
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
        val dagsats: Int,
        val utbetaling: Int?,
        val begrunnelse: BegrunnelseData?,
        val grad: Double?
    )
}
