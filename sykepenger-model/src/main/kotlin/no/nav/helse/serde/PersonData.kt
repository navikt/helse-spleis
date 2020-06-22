package no.nav.helse.serde

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.prosent
import no.nav.helse.økonomi.Økonomi
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

internal data class PersonData(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val arbeidsgivere: List<ArbeidsgiverData>,
    private val aktivitetslogg: AktivitetsloggData?
) {

    private val arbeidsgivereliste = mutableListOf<Arbeidsgiver>()
    private val modelAktivitetslogg get() = aktivitetslogg?.konverterTilAktivitetslogg() ?: Aktivitetslogg()

    private val person = Person::class.primaryConstructor!!
        .apply { isAccessible = true }
        .call(aktørId, fødselsnummer, arbeidsgivereliste, modelAktivitetslogg)

    internal fun createPerson(): Person {
        arbeidsgivereliste.addAll(this.arbeidsgivere.map { it.konverterTilArbeidsgiver(person, this.aktørId, this.fødselsnummer) })
        return person
    }

    internal data class AktivitetsloggData(
        private val aktiviteter: List<AktivitetData>,
        private val kontekster: List<SpesifikkKontekstData>
    ) {
        internal fun konverterTilAktivitetslogg(): Aktivitetslogg {
            val aktivitetslogg = Aktivitetslogg()
            val modellkontekst = kontekster.map { it.parseKontekst() }
            aktivitetslogg.get<MutableList<Any>>("aktiviteter").apply {
                addAll(aktiviteter.map {
                    it.parseAktivitet(modellkontekst)
                })
            }

            return aktivitetslogg
        }

        data class AktivitetData(
            private val alvorlighetsgrad: Alvorlighetsgrad,
            private val label: Char,
            private val behovtype: String?,
            private val melding: String,
            private val tidsstempel: String,
            private val kontekster: List<Int>,
            private val detaljer: Map<String, Any>
        ) {
            internal fun parseAktivitet(spesifikkKontekster: List<SpesifikkKontekst>): Aktivitetslogg.Aktivitet {
                val kontekster = kontekster.map { index -> spesifikkKontekster[index] }
                return when (alvorlighetsgrad) {
                    Alvorlighetsgrad.INFO -> Aktivitetslogg.Aktivitet.Info(
                        kontekster,
                        melding,
                        tidsstempel
                    )
                    Alvorlighetsgrad.WARN -> Aktivitetslogg.Aktivitet.Warn(
                        kontekster,
                        melding,
                        tidsstempel
                    )
                    Alvorlighetsgrad.BEHOV -> Aktivitetslogg.Aktivitet.Behov(
                        Aktivitetslogg.Aktivitet.Behov.Behovtype.valueOf(behovtype!!),
                        kontekster,
                        melding,
                        detaljer,
                        tidsstempel
                    )
                    Alvorlighetsgrad.ERROR -> Aktivitetslogg.Aktivitet.Error(
                        kontekster,
                        melding,
                        tidsstempel
                    )
                    Alvorlighetsgrad.SEVERE -> Aktivitetslogg.Aktivitet.Severe(
                        kontekster,
                        melding,
                        tidsstempel
                    )
                }
            }

        }

        data class SpesifikkKontekstData(
            private val kontekstType: String,
            private val kontekstMap: Map<String, String>
        ) {
            internal fun parseKontekst() =
                SpesifikkKontekst(kontekstType, kontekstMap)
        }

        enum class Alvorlighetsgrad {
            INFO,
            WARN,
            BEHOV,
            ERROR,
            SEVERE
        }
    }

    data class ArbeidsgiverData(
        private val organisasjonsnummer: String,
        private val id: UUID,
        private val inntekter: List<InntektData>,
        private val sykdomshistorikk: List<SykdomshistorikkData>,
        private val vedtaksperioder: List<VedtaksperiodeData>,
        private val forkastede: List<VedtaksperiodeData>,
        private val utbetalinger: List<UtbetalingData>
    ) {
        private val modelInntekthistorikk = Inntekthistorikk().apply {
            InntektData.parseInntekter(inntekter, this)
        }
        private val modelSykdomshistorikk = SykdomshistorikkData.parseSykdomshistorikk(sykdomshistorikk)
        private val vedtaksperiodeliste = mutableListOf<Vedtaksperiode>()
        private val forkastedeliste = mutableListOf<Vedtaksperiode>()
        private val modelUtbetalinger = mutableListOf<Utbetaling>().apply {
            addAll(utbetalinger.map { it.konverterTilUtbetaling() })
        }

        internal fun konverterTilArbeidsgiver(
            person: Person,
            aktørId: String,
            fødselsnummer: String
        ): Arbeidsgiver {
            val arbeidsgiver = Arbeidsgiver::class.primaryConstructor!!
                .apply { isAccessible = true }
                .call(person, organisasjonsnummer, id, modelInntekthistorikk, modelSykdomshistorikk, vedtaksperiodeliste, forkastedeliste, modelUtbetalinger)

            vedtaksperiodeliste.addAll(this.vedtaksperioder.map {
                it.parseVedtaksperiode(
                    person,
                    arbeidsgiver,
                    aktørId,
                    fødselsnummer,
                    this.organisasjonsnummer
                )
            })

            forkastedeliste.addAll(this.forkastede.map {
                it.parseVedtaksperiode(
                    person,
                    arbeidsgiver,
                    aktørId,
                    fødselsnummer,
                    this.organisasjonsnummer
                )
            })
            Vedtaksperiode.sorter(vedtaksperiodeliste)
            Vedtaksperiode.sorter(forkastedeliste)

            return arbeidsgiver
        }

        data class InntektData(
            private val fom: LocalDate,
            private val hendelseId: UUID,
            private val beløp: BigDecimal
        ) {
            internal companion object {
                internal fun parseInntekter(inntekter: List<InntektData>, inntekthistorikk: Inntekthistorikk) {
                    inntekter.forEach { inntektData ->
                        inntekthistorikk.add(
                            fom = inntektData.fom,
                            hendelseId = inntektData.hendelseId,
                            beløp = inntektData.beløp.setScale(1, RoundingMode.HALF_UP)
                        )
                    }
                }
            }
        }

        data class SykdomstidslinjeData(
            private val dager: List<DagData>,
            private val periode: Periode?,
            private val låstePerioder: MutableList<Periode>? = mutableListOf()
        ) {
            private val dagerMap: SortedMap<LocalDate, Dag> = DagData.parseDager(dager)

            internal fun createSykdomstidslinje(): Sykdomstidslinje =
                Sykdomstidslinje::class.primaryConstructor!!
                    .apply { isAccessible = true }
                    .call(
                        dagerMap,
                        null,
                        låstePerioder ?: mutableListOf<Periode>()
                    )

            data class DagData(
                private val dato: LocalDate,
                private val type: JsonDagType,
                private val kilde: KildeData,
                private val grad: Double,
                private val arbeidsgiverBetalingProsent: Double,
                private val aktuellDagsinntekt: Double?,
                private val dekningsgrunnlag: Double?,
                private val arbeidsgiverbeløp: Int?,
                private val personbeløp: Int?,
                private val er6GBegrenset: Boolean?,
                private val melding: String?
            ) {
                internal companion object {
                    internal fun parseDager(dager: List<DagData>): SortedMap<LocalDate, Dag> =
                        dager.map { it.dato to it.parseDag() }.toMap(sortedMapOf())
                }

                private val økonomi get() = Økonomi::class.primaryConstructor!!
                    .apply { isAccessible = true }
                    .call(
                        grad.prosent,
                        arbeidsgiverBetalingProsent.prosent,
                        aktuellDagsinntekt,
                        dekningsgrunnlag,
                        arbeidsgiverbeløp,
                        personbeløp,
                        er6GBegrenset,
                        Økonomi.Tilstand.KunGrad
                    )

                private val hendelseskilde get() = kilde.parseKilde()

                internal fun parseDag(): Dag = when (type) {
                    JsonDagType.ARBEIDSDAG -> Dag.Arbeidsdag(
                        dato,
                        hendelseskilde
                    )
                    JsonDagType.ARBEIDSGIVERDAG -> Dag.Arbeidsgiverdag(
                        dato,
                        økonomi,
                        hendelseskilde
                    )
                    JsonDagType.ARBEIDSGIVER_HELGEDAG -> Dag.ArbeidsgiverHelgedag(
                        dato,
                        økonomi,
                        hendelseskilde
                    )
                    JsonDagType.FERIEDAG -> Dag.Feriedag(
                        dato,
                        hendelseskilde
                    )
                    JsonDagType.FRISK_HELGEDAG -> Dag.FriskHelgedag(
                        dato,
                        hendelseskilde
                    )
                    JsonDagType.FORELDET_SYKEDAG -> Dag.ForeldetSykedag(
                        dato,
                        økonomi,
                        hendelseskilde
                    )
                    JsonDagType.PERMISJONSDAG -> Dag.Permisjonsdag(
                        dato,
                        hendelseskilde
                    )
                    JsonDagType.PROBLEMDAG -> Dag.ProblemDag(
                        dato,
                        hendelseskilde,
                        melding!!
                    )
                    JsonDagType.STUDIEDAG -> Dag.Studiedag(
                        dato,
                        hendelseskilde
                    )
                    JsonDagType.SYKEDAG -> Dag.Sykedag(
                        dato,
                        økonomi,
                        hendelseskilde
                    )
                    JsonDagType.SYK_HELGEDAG -> Dag.SykHelgedag(
                        dato,
                        økonomi,
                        hendelseskilde
                    )
                    JsonDagType.UTENLANDSDAG -> Dag.Utenlandsdag(
                        dato,
                        hendelseskilde
                    )
                    JsonDagType.UKJENT_DAG -> Dag.UkjentDag(
                        dato,
                        hendelseskilde
                    )
                }
            }

            data class KildeData(
                private val type: String,
                private val id: UUID
            ) {
                internal fun parseKilde() =
                    SykdomstidslinjeHendelse.Hendelseskilde(type, id)
            }
        }

        data class VedtaksperiodeData(
            private val id: UUID,
            private val maksdato: LocalDate?,
            private val gjenståendeSykedager: Int?,
            private val forbrukteSykedager: Int?,
            private val godkjentAv: String?,
            private val godkjenttidspunkt: LocalDateTime?,
            private val førsteFraværsdag: LocalDate?,
            private val dataForVilkårsvurdering: DataForVilkårsvurderingData?,
            private val dataForSimulering: DataForSimuleringData?,
            private val sykdomshistorikk: List<SykdomshistorikkData>,
            private val fom: LocalDate,
            private val tom: LocalDate,
            private val tilstand: TilstandType,
            private val utbetalingstidslinje: UtbetalingstidslinjeData,
            private val personFagsystemId: String?,
            private val personNettoBeløp: Int,
            private val arbeidsgiverFagsystemId: String?,
            private val arbeidsgiverNettoBeløp: Int,
            private val forlengelseFraInfotrygd: ForlengelseFraInfotrygd
        ) {

            internal fun parseVedtaksperiode(
                person: Person,
                arbeidsgiver: Arbeidsgiver,
                aktørId: String,
                fødselsnummer: String,
                organisasjonsnummer: String
            ): Vedtaksperiode {
                return Vedtaksperiode::class.primaryConstructor!!
                    .apply { isAccessible = true }
                    .call(
                        person,
                        arbeidsgiver,
                        id,
                        aktørId,
                        fødselsnummer,
                        organisasjonsnummer,
                        parseTilstand(this.tilstand),
                        maksdato,
                        gjenståendeSykedager,
                        forbrukteSykedager,
                        godkjentAv,
                        godkjenttidspunkt,
                        førsteFraværsdag,
                        dataForVilkårsvurdering?.parseDataForVilkårsvurdering(),
                        dataForSimulering?.parseDataForSimulering(),
                        SykdomshistorikkData.parseSykdomshistorikk(this.sykdomshistorikk),
                        Periode(fom, tom),
                        this.utbetalingstidslinje.konverterTilUtbetalingstidslinje(),
                        personFagsystemId,
                        personNettoBeløp,
                        arbeidsgiverFagsystemId,
                        arbeidsgiverNettoBeløp,
                        forlengelseFraInfotrygd
                    )
            }

            private fun parseTilstand(tilstand: TilstandType) = when (tilstand) {
                TilstandType.AVVENTER_HISTORIKK -> Vedtaksperiode.AvventerHistorikk
                TilstandType.AVVENTER_GODKJENNING -> Vedtaksperiode.AvventerGodkjenning
                TilstandType.AVVENTER_SIMULERING -> Vedtaksperiode.AvventerSimulering
                TilstandType.TIL_UTBETALING -> Vedtaksperiode.TilUtbetaling
                TilstandType.AVSLUTTET -> Vedtaksperiode.Avsluttet
                TilstandType.AVSLUTTET_UTEN_UTBETALING -> Vedtaksperiode.AvsluttetUtenUtbetaling
                TilstandType.AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING -> Vedtaksperiode.AvsluttetUtenUtbetalingMedInntektsmelding
                TilstandType.UTBETALING_FEILET -> Vedtaksperiode.UtbetalingFeilet
                TilstandType.TIL_INFOTRYGD -> Vedtaksperiode.TilInfotrygd
                TilstandType.START -> Vedtaksperiode.Start
                TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE -> Vedtaksperiode.MottattSykmeldingFerdigForlengelse
                TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE -> Vedtaksperiode.MottattSykmeldingUferdigForlengelse
                TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP -> Vedtaksperiode.MottattSykmeldingFerdigGap
                TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP -> Vedtaksperiode.MottattSykmeldingUferdigGap
                TilstandType.AVVENTER_SØKNAD_FERDIG_GAP -> Vedtaksperiode.AvventerSøknadFerdigGap
                TilstandType.AVVENTER_VILKÅRSPRØVING_GAP -> Vedtaksperiode.AvventerVilkårsprøvingGap
                TilstandType.AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD -> Vedtaksperiode.AvventerVilkårsprøvingArbeidsgiversøknad
                TilstandType.AVVENTER_GAP -> Vedtaksperiode.AvventerGap
                TilstandType.AVVENTER_SØKNAD_UFERDIG_GAP -> Vedtaksperiode.AvventerSøknadUferdigGap
                TilstandType.AVVENTER_INNTEKTSMELDING_FERDIG_GAP -> Vedtaksperiode.AvventerInntektsmeldingFerdigGap
                TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP -> Vedtaksperiode.AvventerInntektsmeldingUferdigGap
                TilstandType.AVVENTER_UFERDIG_GAP -> Vedtaksperiode.AvventerUferdigGap
                TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE -> Vedtaksperiode.AvventerInntektsmeldingUferdigForlengelse
                TilstandType.AVVENTER_SØKNAD_UFERDIG_FORLENGELSE -> Vedtaksperiode.AvventerSøknadUferdigForlengelse
                TilstandType.AVVENTER_UFERDIG_FORLENGELSE -> Vedtaksperiode.AvventerUferdigForlengelse
            }

            data class DataForVilkårsvurderingData(
                private val erEgenAnsatt: Boolean,
                private val beregnetÅrsinntektFraInntektskomponenten: Double,
                private val avviksprosent: Double,
                private val harOpptjening: Boolean,
                private val antallOpptjeningsdagerErMinst: Int,
                private val medlemskapstatus: JsonMedlemskapstatus
            ) {
                internal fun parseDataForVilkårsvurdering(
                ): Vilkårsgrunnlag.Grunnlagsdata = Vilkårsgrunnlag.Grunnlagsdata(
                        erEgenAnsatt = erEgenAnsatt,
                        beregnetÅrsinntektFraInntektskomponenten = beregnetÅrsinntektFraInntektskomponenten,
                        avviksprosent = avviksprosent,
                        harOpptjening = harOpptjening,
                        antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
                        medlemskapstatus = when (medlemskapstatus) {
                            JsonMedlemskapstatus.JA -> Medlemskapsvurdering.Medlemskapstatus.Ja
                            JsonMedlemskapstatus.NEI -> Medlemskapsvurdering.Medlemskapstatus.Nei
                            else -> Medlemskapsvurdering.Medlemskapstatus.VetIkke
                        }
                    )
            }

            data class DataForSimuleringData(
                private val totalbeløp: Int,
                private val perioder: List<SimulertPeriode>
            ) {
                internal fun parseDataForSimulering() = Simulering.SimuleringResultat(
                    totalbeløp = totalbeløp,
                    perioder = perioder.map { it.parsePeriode() }
                )

                data class SimulertPeriode(
                    private val fom: LocalDate,
                    private val tom: LocalDate,
                    private val utbetalinger: List<SimulertUtbetaling>
                ) {

                    internal fun parsePeriode(): Simulering.SimulertPeriode {
                        return Simulering.SimulertPeriode(
                            periode = Periode(fom, tom),
                            utbetalinger = utbetalinger.map { it.parseUtbetaling() }
                        )
                    }
                }

                data class SimulertUtbetaling(
                    private val forfallsdato: LocalDate,
                    private val utbetalesTil: Mottaker,
                    private val feilkonto: Boolean,
                    private val detaljer: List<Detaljer>
                ) {
                    internal fun parseUtbetaling(): Simulering.SimulertUtbetaling {
                        return Simulering.SimulertUtbetaling(
                            forfallsdato = forfallsdato,
                            utbetalesTil = Simulering.Mottaker(
                                id = utbetalesTil.id,
                                navn = utbetalesTil.navn
                            ),
                            feilkonto = feilkonto,
                            detaljer = detaljer.map { it.parseDetaljer()}
                        )
                    }
                }

                data class Detaljer(
                    private val fom: LocalDate,
                    private val tom: LocalDate,
                    private val konto: String,
                    private val beløp: Int,
                    private val klassekode: Klassekode,
                    private val uføregrad: Int,
                    private val utbetalingstype: String,
                    private val tilbakeføring: Boolean,
                    private val sats: Sats,
                    private val refunderesOrgnummer: String
                ) {
                    internal fun parseDetaljer(): Simulering.Detaljer {
                        return Simulering.Detaljer(
                            periode = Periode(fom, tom),
                            konto = konto,
                            beløp = beløp,
                            klassekode = Simulering.Klassekode(
                                kode = klassekode.kode,
                                beskrivelse = klassekode.beskrivelse
                            ),
                            uføregrad = uføregrad,
                            utbetalingstype = utbetalingstype,
                            tilbakeføring = tilbakeføring,
                            sats = Simulering.Sats(
                                sats = sats.sats,
                                antall = sats.antall,
                                type = sats.type
                            ),
                            refunderesOrgnummer = refunderesOrgnummer
                        )
                    }
                }

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

    data class SykdomshistorikkData(
        private val tidsstempel: LocalDateTime,
        private val hendelseId: UUID?,
        private val hendelseSykdomstidslinje: ArbeidsgiverData.SykdomstidslinjeData,
        private val beregnetSykdomstidslinje: ArbeidsgiverData.SykdomstidslinjeData
    ) {

        internal companion object {
            internal fun parseSykdomshistorikk(data: List<SykdomshistorikkData>) =
                Sykdomshistorikk::class.primaryConstructor!!
                    .apply { isAccessible = true }
                    .call(data.map { it.parseSykdomshistorikk() }.toMutableList())
        }

        internal fun parseSykdomshistorikk(): Sykdomshistorikk.Element {
            return Sykdomshistorikk.Element::class.primaryConstructor!!
                .apply { isAccessible = true }
                .call(
                    hendelseId,
                    tidsstempel,
                    hendelseSykdomstidslinje.createSykdomstidslinje(),
                    beregnetSykdomstidslinje.createSykdomstidslinje()
                )
        }
    }

    data class UtbetalingData(
        val utbetalingstidslinje: UtbetalingstidslinjeData,
        val arbeidsgiverOppdrag: OppdragData,
        val personOppdrag: OppdragData,
        val tidsstempel: LocalDateTime,
        val status: String
    ) {

        internal fun konverterTilUtbetaling() = Utbetaling::class.primaryConstructor!!
            .apply { isAccessible = true }
            .call(
                utbetalingstidslinje.konverterTilUtbetalingstidslinje(),
                arbeidsgiverOppdrag.konverterTilOppdrag(),
                personOppdrag.konverterTilOppdrag(),
                tidsstempel,
                enumValueOf<Utbetaling.Status>(status)
            )
    }

    data class OppdragData(
        private val mottaker: String,
        private val fagområde: String,
        private val linjer: List<UtbetalingslinjeData>,
        private val fagsystemId: String,
        private val endringskode: String,
        private val sisteArbeidsgiverdag: LocalDate?,
        private val nettoBeløp: Int
    ) {
        internal fun konverterTilOppdrag(): Oppdrag {
            return Oppdrag::class.primaryConstructor!!
                .apply { isAccessible = true }
                .call(
                    mottaker,
                    Fagområde.from(fagområde),
                    linjer.map { it.konverterTilUtbetalingslinje() },
                    fagsystemId,
                    Endringskode.valueOf(endringskode),
                    sisteArbeidsgiverdag,
                    nettoBeløp
                )
        }
    }

    data class UtbetalingslinjeData(
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val dagsats: Int,
        private val lønn: Int,
        private val grad: Double,
        private val refFagsystemId: String?,
        private val delytelseId: Int,
        private val refDelytelseId: Int?,
        private val endringskode: String,
        private val klassekode: String,
        private val datoStatusFom: LocalDate?
    ) {

        internal fun konverterTilUtbetalingslinje(): Utbetalingslinje = Utbetalingslinje::class.primaryConstructor!!
            .apply { isAccessible = true }
            .call(
                fom,
                tom,
                dagsats,
                lønn,
                grad,
                refFagsystemId,
                delytelseId,
                refDelytelseId,
                Endringskode.valueOf(endringskode),
                Klassekode.from(klassekode),
                datoStatusFom
            )
    }

    data class UtbetalingstidslinjeData(
        val dager: List<UtbetalingsdagData>
    ) {
        internal fun konverterTilUtbetalingstidslinje(): Utbetalingstidslinje {
            return Utbetalingstidslinje::class.primaryConstructor!!
                .apply { isAccessible = true }
                .call(dager.map { it.parseDag() }.toMutableList())
        }

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
            private val type: TypeData,
            private val dato: LocalDate,
            private val aktuellDagsinntekt: Double,
            private val dekningsgrunnlag: Double,
            private val begrunnelse: BegrunnelseData?,
            private val grad: Double?,
            private val arbeidsgiverBetalingProsent: Double?,
            private val arbeidsgiverbeløp: Int?,
            private val personbeløp: Int?,
            private val er6GBegrenset: Boolean?
        ) {
            private val økonomi get() = Økonomi::class.primaryConstructor!!
                .apply { isAccessible = true }
                .call(
                    grad?.prosent,
                    arbeidsgiverBetalingProsent?.prosent,
                    aktuellDagsinntekt,
                    dekningsgrunnlag,
                    arbeidsgiverbeløp,
                    personbeløp,
                    er6GBegrenset,
                    when {
                        arbeidsgiverbeløp == null && type == TypeData.AvvistDag -> Økonomi.Tilstand.Låst
                        arbeidsgiverbeløp == null -> Økonomi.Tilstand.HarInntekt
                        type == TypeData.AvvistDag -> Økonomi.Tilstand.LåstMedBeløp
                        else -> Økonomi.Tilstand.HarBeløp
                    }
                )

            internal fun parseDag() =
                when (type) {
                    TypeData.ArbeidsgiverperiodeDag -> {
                        Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag(dato = dato, økonomi = økonomi)
                    }
                    TypeData.NavDag -> {
                        Utbetalingstidslinje.Utbetalingsdag.NavDag(dato, økonomi)
                    }
                    TypeData.NavHelgDag -> {
                        Utbetalingstidslinje.Utbetalingsdag.NavHelgDag(dato = dato, økonomi = økonomi)
                    }
                    TypeData.Arbeidsdag -> {
                        Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag(dato = dato, økonomi = økonomi)
                    }
                    TypeData.Fridag -> {
                        Utbetalingstidslinje.Utbetalingsdag.Fridag(dato = dato, økonomi = økonomi)
                    }
                    TypeData.AvvistDag -> {
                        Utbetalingstidslinje.Utbetalingsdag.AvvistDag(
                            dato = dato, økonomi = økonomi, begrunnelse = when (begrunnelse) {
                                BegrunnelseData.SykepengedagerOppbrukt -> Begrunnelse.SykepengedagerOppbrukt
                                BegrunnelseData.MinimumInntekt -> Begrunnelse.MinimumInntekt
                                BegrunnelseData.EgenmeldingUtenforArbeidsgiverperiode -> Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
                                BegrunnelseData.MinimumSykdomsgrad -> Begrunnelse.MinimumSykdomsgrad
                                null -> error("Prøver å deserialisere avvist dag uten begrunnelse")
                            }
                        )
                    }
                    TypeData.UkjentDag -> {
                        Utbetalingstidslinje.Utbetalingsdag.UkjentDag(dato = dato, økonomi = økonomi)
                    }
                    TypeData.ForeldetDag -> {
                        Utbetalingstidslinje.Utbetalingsdag.ForeldetDag(dato = dato, økonomi = økonomi)
                    }
                }
        }
    }
}
