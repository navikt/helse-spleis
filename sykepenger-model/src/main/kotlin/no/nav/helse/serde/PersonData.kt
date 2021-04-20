package no.nav.helse.serde

import no.nav.helse.appender
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.til
import no.nav.helse.person.*
import no.nav.helse.person.infotrygdhistorikk.*
import no.nav.helse.serde.PersonData.InfotrygdhistorikkElementData.Companion.tilModellObjekt
import no.nav.helse.serde.PersonData.VilkårsgrunnlagElement.Companion.tilModellObjekt
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.serde.reflection.Kilde
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjeberegning
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosent.Companion.ratio
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

internal data class PersonData(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val arbeidsgivere: List<ArbeidsgiverData>,
    private val aktivitetslogg: AktivitetsloggData?,
    private val opprettet: LocalDateTime,
    private val infotrygdhistorikk: List<InfotrygdhistorikkElementData>,
    private val vilkårsgrunnlagHistorikk: List<VilkårsgrunnlagElement>,
    private val dødsdato: LocalDate?
) {

    private val arbeidsgivereliste = mutableListOf<Arbeidsgiver>()
    private val modelAktivitetslogg get() = aktivitetslogg?.konverterTilAktivitetslogg() ?: Aktivitetslogg()

    private val person = Person::class.primaryConstructor!!
        .apply { isAccessible = true }
        .call(aktørId, fødselsnummer, arbeidsgivereliste, modelAktivitetslogg, opprettet, infotrygdhistorikk.tilModellObjekt(), vilkårsgrunnlagHistorikk.tilModellObjekt(), dødsdato)

    internal fun createPerson(): Person {
        arbeidsgivereliste.addAll(this.arbeidsgivere.map {
            it.konverterTilArbeidsgiver(
                person,
                this.aktørId,
                this.fødselsnummer
            )
        })
        return person
    }

    data class InfotrygdhistorikkElementData(
        private val id: UUID,
        private val tidsstempel: LocalDateTime,
        private val hendelseId: UUID?,
        private val ferieperioder: List<FerieperiodeData>,
        private val utbetalingsperioder: List<UtbetalingsperiodeData>,
        private val ukjenteperioder: List<UkjentperiodeData>,
        private val inntekter: List<InntektsopplysningData>,
        private val arbeidskategorikoder: Map<String, LocalDate>,
        private val ugyldigePerioder: List<Pair<LocalDate?, LocalDate?>>,
        private val harStatslønn: Boolean,
        private val oppdatert: LocalDateTime,
        private val lagretInntekter: Boolean,
        private val lagretVilkårsgrunnlag: Boolean
    ) {
        internal companion object {
            fun List<InfotrygdhistorikkElementData>.tilModellObjekt() =
                Infotrygdhistorikk::class.primaryConstructor!!
                    .apply { isAccessible = true }
                    .call(map { it.parseInfotrygdhistorikkElement() })
        }

        internal fun parseInfotrygdhistorikkElement() = InfotrygdhistorikkElement::class.primaryConstructor!!
            .apply { isAccessible = true }
            .call(
                id,
                tidsstempel,
                hendelseId,
                utbetalingsperioder.map { it.parsePeriode() } + ferieperioder.map { it.parsePeriode() } + ukjenteperioder.map { it.parsePeriode() },
                inntekter.map { it.parseInntektsopplysning() },
                arbeidskategorikoder,
                ugyldigePerioder,
                harStatslønn,
                oppdatert,
                lagretInntekter,
                lagretVilkårsgrunnlag
            )

        data class FerieperiodeData(
            private val fom: LocalDate,
            private val tom: LocalDate
        ) {
            internal fun parsePeriode() = Friperiode(fom til tom)
        }

        data class UkjentperiodeData(
            private val fom: LocalDate,
            private val tom: LocalDate
        ) {
            internal fun parsePeriode() = UkjentInfotrygdperiode(fom til tom)
        }

        data class UtbetalingsperiodeData(
            private val orgnr: String,
            private val fom: LocalDate,
            private val tom: LocalDate,
            private val grad: Int,
            private val inntekt: Double
        ) {
            internal fun parsePeriode() = Utbetalingsperiode(
                orgnr = orgnr,
                periode = fom til tom,
                grad = grad.prosent,
                inntekt = inntekt.månedlig
            )
        }

        data class InntektsopplysningData(
            private val orgnr: String,
            private val sykepengerFom: LocalDate,
            private val inntekt: Double,
            private val refusjonTilArbeidsgiver: Boolean,
            private val refusjonTom: LocalDate?,
            private val lagret: LocalDateTime?
        ) {
            internal fun parseInntektsopplysning() = Inntektsopplysning::class.primaryConstructor!!
                .apply { isAccessible = true }
                .call(
                    orgnr,
                    sykepengerFom,
                    inntekt.månedlig,
                    refusjonTilArbeidsgiver,
                    refusjonTom,
                    lagret
                )
        }
    }

    data class VilkårsgrunnlagElement(
        private val skjæringstidspunkt: LocalDate,
        private val type: GrunnlagsdataType,
        private val sammenligningsgrunnlag: Double?,
        private val avviksprosent: Double?,
        private val harOpptjening: Boolean?,
        private val antallOpptjeningsdagerErMinst: Int?,
        private val medlemskapstatus: JsonMedlemskapstatus?,
        private val vurdertOk: Boolean?
    ) {
        internal fun parseDataForVilkårsvurdering(): Pair<LocalDate, VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement> = skjæringstidspunkt to when (type) {
            GrunnlagsdataType.Vilkårsprøving -> VilkårsgrunnlagHistorikk.Grunnlagsdata(
                sammenligningsgrunnlag = sammenligningsgrunnlag!!.årlig,
                avviksprosent = avviksprosent?.ratio,
                harOpptjening = harOpptjening!!,
                antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst!!,
                medlemskapstatus = when (medlemskapstatus!!) {
                    JsonMedlemskapstatus.JA -> Medlemskapsvurdering.Medlemskapstatus.Ja
                    JsonMedlemskapstatus.NEI -> Medlemskapsvurdering.Medlemskapstatus.Nei
                    JsonMedlemskapstatus.VET_IKKE -> Medlemskapsvurdering.Medlemskapstatus.VetIkke
                },
                vurdertOk = vurdertOk!!
            )
            GrunnlagsdataType.Infotrygd -> VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag()
        }

        enum class GrunnlagsdataType {
            Infotrygd,
            Vilkårsprøving
        }

        companion object {
            private fun List<VilkårsgrunnlagElement>.toMap() =
                map(VilkårsgrunnlagElement::parseDataForVilkårsvurdering)
                .toMap(mutableMapOf())

            internal fun List<VilkårsgrunnlagElement>.tilModellObjekt() = VilkårsgrunnlagHistorikk::class.primaryConstructor!!
                .apply { isAccessible = true }
                .call(toMap())
        }
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
        private val inntektshistorikk: List<InntektshistorikkInnslagData> = listOf(),
        private val sykdomshistorikk: List<SykdomshistorikkData>,
        private val vedtaksperioder: List<VedtaksperiodeData>,
        private val forkastede: List<ForkastetVedtaksperiodeData>,
        private val utbetalinger: List<UtbetalingData>,
        private val beregnetUtbetalingstidslinjer: List<BeregnetUtbetalingstidslinjeData>,
        private val refusjonOpphører: List<LocalDate?> = emptyList()
    ) {
        private val modelInntekthistorikk = Inntektshistorikk().apply {
            InntektshistorikkInnslagData.parseInntekter(inntektshistorikk, this)
        }
        private val modelSykdomshistorikk = SykdomshistorikkData.parseSykdomshistorikk(sykdomshistorikk)
        private val vedtaksperiodeliste = mutableListOf<Vedtaksperiode>()
        private val forkastedeliste = mutableListOf<ForkastetVedtaksperiode>()
        private val modelUtbetalinger = utbetalinger.map { it.konverterTilUtbetaling() }
        private val utbetalingMap = utbetalinger.zip(modelUtbetalinger) { data, utbetaling -> data.id to utbetaling }.toMap()

        internal fun konverterTilArbeidsgiver(
            person: Person,
            aktørId: String,
            fødselsnummer: String
        ): Arbeidsgiver {
            val arbeidsgiver = Arbeidsgiver.JsonRestorer.restore(
                person,
                organisasjonsnummer,
                id,
                modelInntekthistorikk,
                modelSykdomshistorikk,
                vedtaksperiodeliste,
                forkastedeliste,
                modelUtbetalinger,
                beregnetUtbetalingstidslinjer.map { it.tilBeregnetUtbetalingstidslinje() },
                refusjonOpphører
            )

            vedtaksperiodeliste.addAll(this.vedtaksperioder.map {
                it.createVedtaksperiode(
                    person,
                    arbeidsgiver,
                    aktørId,
                    fødselsnummer,
                    this.organisasjonsnummer,
                    utbetalingMap
                )
            })

            forkastedeliste.addAll(this.forkastede.map { (periode, årsak) ->
                ForkastetVedtaksperiode(
                    periode.createVedtaksperiode(
                        person,
                        arbeidsgiver,
                        aktørId,
                        fødselsnummer,
                        this.organisasjonsnummer,
                        utbetalingMap
                    ), årsak
                )
            })
            vedtaksperiodeliste.sort()

            return arbeidsgiver
        }

        data class BeregnetUtbetalingstidslinjeData(
            private val id: UUID,
            private val tidsstempel: LocalDateTime,
            private val sykdomshistorikkElementId: UUID,
            private val organisasjonsnummer: String,
            private val utbetalingstidslinje: UtbetalingstidslinjeData
        ) {
            internal fun tilBeregnetUtbetalingstidslinje() =
                Utbetalingstidslinjeberegning.restore(
                    id,
                    tidsstempel,
                    sykdomshistorikkElementId,
                    organisasjonsnummer,
                    utbetalingstidslinje.konverterTilUtbetalingstidslinje()
                )
        }

        data class InntektshistorikkInnslagData(
            val id: UUID,
            val inntektsopplysninger: List<InntektsopplysningData>
        ) {
            internal companion object {
                internal fun parseInntekter(
                    inntekter: List<InntektshistorikkInnslagData>,
                    inntektshistorikk: Inntektshistorikk
                ) {
                    val alleOpplysninger = mutableMapOf<Pair<UUID, UUID>, Inntektshistorikk.Inntektsopplysning>()
                    inntektshistorikk.appender(Inntektshistorikk.RestoreJsonMode) {
                        inntekter.reversed().forEach {
                            innslag(it.id) {
                                InntektsopplysningData.parseInntekter(it.inntektsopplysninger, this, it.id, alleOpplysninger)
                            }
                        }
                    }
                }
            }
        }

        data class InntektsopplysningData(
            private val id: UUID?,
            private val innslagId: UUID?,
            private val orginalOpplysningId: UUID?,
            private val dato: LocalDate?,
            private val hendelseId: UUID?,
            private val beløp: Double?,
            private val kilde: String?,
            private val måned: YearMonth?,
            private val type: String?,
            private val fordel: String?,
            private val beskrivelse: String?,
            private val begrunnelse: String?,
            private val tidsstempel: LocalDateTime?,
            private val skatteopplysninger: List<InntektsopplysningData>?
        ) {
            internal companion object {
                internal fun parseInntekter(
                    inntektsopplysninger: List<InntektsopplysningData>,
                    innslag: Inntektshistorikk.RestoreJsonMode.InnslagAppender,
                    innslagId: UUID,
                    alleOpplysninger: MutableMap<Pair<UUID, UUID>, Inntektshistorikk.Inntektsopplysning>
                ) {
                    inntektsopplysninger.forEach { inntektData ->
                        when (inntektData.kilde?.let(Kilde::valueOf)) {
                            Kilde.INFOTRYGD ->
                                Inntektshistorikk.Infotrygd(
                                    id = requireNotNull(inntektData.id),
                                    dato = requireNotNull(inntektData.dato),
                                    hendelseId = requireNotNull(inntektData.hendelseId),
                                    beløp = requireNotNull(inntektData.beløp).månedlig,
                                    tidsstempel = requireNotNull(inntektData.tidsstempel)
                                )
                            Kilde.INNTEKTSMELDING ->
                                Inntektshistorikk.Inntektsmelding(
                                    id = requireNotNull(inntektData.id),
                                    dato = requireNotNull(inntektData.dato),
                                    hendelseId = requireNotNull(inntektData.hendelseId),
                                    beløp = requireNotNull(inntektData.beløp).månedlig,
                                    tidsstempel = requireNotNull(inntektData.tidsstempel)
                                )
                            Kilde.SAKSBEHANDLER ->
                                Inntektshistorikk.Saksbehandler(
                                    id = requireNotNull(inntektData.id),
                                    dato = requireNotNull(inntektData.dato),
                                    hendelseId = requireNotNull(inntektData.hendelseId),
                                    beløp = requireNotNull(inntektData.beløp).månedlig,
                                    tidsstempel = requireNotNull(inntektData.tidsstempel)
                                )
                            Kilde.INNTEKTSOPPLYSNING_REFERANSE ->
                                Inntektshistorikk.InntektsopplysningReferanse(
                                    id = requireNotNull(inntektData.id),
                                    innslagId = requireNotNull(inntektData.innslagId),
                                    orginalOpplysningId = requireNotNull(inntektData.orginalOpplysningId),
                                    orginalOpplysning = alleOpplysninger.getValue(inntektData.innslagId to inntektData.orginalOpplysningId),
                                    dato = requireNotNull(inntektData.dato),
                                    hendelseId = requireNotNull(inntektData.hendelseId),
                                    tidsstempel = requireNotNull(inntektData.tidsstempel)
                                )
                            null -> Inntektshistorikk.SkattComposite(
                                id = requireNotNull(inntektData.id),
                                inntektsopplysninger = requireNotNull(inntektData.skatteopplysninger).map { skatteData ->
                                    when (skatteData.kilde?.let(Kilde::valueOf)) {
                                        Kilde.SKATT_SAMMENLIGNINGSGRUNNLAG ->
                                            Inntektshistorikk.Skatt.Sammenligningsgrunnlag(
                                                dato = requireNotNull(skatteData.dato),
                                                hendelseId = requireNotNull(skatteData.hendelseId),
                                                beløp = requireNotNull(skatteData.beløp).månedlig,
                                                måned = requireNotNull(skatteData.måned),
                                                type = enumValueOf(requireNotNull(skatteData.type)),
                                                fordel = requireNotNull(skatteData.fordel),
                                                beskrivelse = requireNotNull(skatteData.beskrivelse),
                                                tidsstempel = requireNotNull(skatteData.tidsstempel)
                                            )
                                        Kilde.SKATT_SYKEPENGEGRUNNLAG ->
                                            Inntektshistorikk.Skatt.Sykepengegrunnlag(
                                                dato = requireNotNull(skatteData.dato),
                                                hendelseId = requireNotNull(skatteData.hendelseId),
                                                beløp = requireNotNull(skatteData.beløp).månedlig,
                                                måned = requireNotNull(skatteData.måned),
                                                type = enumValueOf(requireNotNull(skatteData.type)),
                                                fordel = requireNotNull(skatteData.fordel),
                                                beskrivelse = requireNotNull(skatteData.beskrivelse),
                                                tidsstempel = requireNotNull(skatteData.tidsstempel)
                                            )
                                        else -> error("Kan kun være skatteopplysninger i SkattComposite")
                                    }
                                }
                            )
                            Kilde.SKATT_SAMMENLIGNINGSGRUNNLAG,
                            Kilde.SKATT_SYKEPENGEGRUNNLAG -> error("Fant ${inntektData.kilde}. Kan kun være i SkattComposite")
                        }
                            .also(innslag::add)
                            .also { alleOpplysninger[innslagId to inntektData.id] = it }
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
                private val skjæringstidspunkt: LocalDate?,
                private val totalGrad: Double?,
                private val arbeidsgiverbeløp: Int?,
                private val personbeløp: Int?,
                private val er6GBegrenset: Boolean?,
                private val melding: String?
            ) {
                internal companion object {
                    internal fun parseDager(dager: List<DagData>): SortedMap<LocalDate, Dag> =
                        dager.map { it.dato to it.parseDag() }.toMap(sortedMapOf())
                }

                private val økonomi
                    get() = Økonomi::class.primaryConstructor!!
                        .apply { isAccessible = true }
                        .call(
                            grad.prosent,
                            arbeidsgiverBetalingProsent.prosent,
                            aktuellDagsinntekt,
                            dekningsgrunnlag,
                            skjæringstidspunkt,
                            totalGrad?.prosent,
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
                    JsonDagType.UKJENT_DAG -> Dag.UkjentDag(
                        dato,
                        hendelseskilde
                    )
                }
            }

            enum class JsonDagType {
                ARBEIDSDAG,
                ARBEIDSGIVERDAG,
                ARBEIDSGIVER_HELGEDAG,
                FERIEDAG,
                FRISK_HELGEDAG,
                FORELDET_SYKEDAG,
                PERMISJONSDAG,
                PROBLEMDAG,
                SYKEDAG,
                SYK_HELGEDAG,
                UKJENT_DAG,
            }

            data class KildeData(
                private val type: String,
                private val id: UUID,
                private val tidsstempel: LocalDateTime
            ) {
                internal fun parseKilde() =
                    SykdomstidslinjeHendelse.Hendelseskilde(type, id, tidsstempel)
            }
        }

        data class ForkastetVedtaksperiodeData(
            val vedtaksperiode: VedtaksperiodeData,
            val årsak: ForkastetÅrsak
        )

        data class VedtaksperiodeData(
            private val id: UUID,
            private val skjæringstidspunktFraInfotrygd: LocalDate?,
            private val dataForSimulering: DataForSimuleringData?,
            private val sykdomstidslinje: SykdomstidslinjeData,
            private val hendelseIder: MutableList<UUID>,
            private val inntektsmeldingId: UUID?,
            private val fom: LocalDate,
            private val tom: LocalDate,
            private val sykmeldingFom: LocalDate,
            private val sykmeldingTom: LocalDate,
            private val tilstand: TilstandType,
            private val utbetalinger: List<UUID>,
            private val utbetalingstidslinje: UtbetalingstidslinjeData,
            private val forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            private val inntektskilde: Inntektskilde,
            private val opprettet: LocalDateTime,
            private val oppdatert: LocalDateTime
        ) {

            internal fun createVedtaksperiode(
                person: Person,
                arbeidsgiver: Arbeidsgiver,
                aktørId: String,
                fødselsnummer: String,
                organisasjonsnummer: String,
                utbetalinger: Map<UUID, Utbetaling>
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
                        skjæringstidspunktFraInfotrygd,
                        dataForSimulering?.parseDataForSimulering(),
                        sykdomstidslinje.createSykdomstidslinje(),
                        hendelseIder,
                        inntektsmeldingId,
                        Periode(fom, tom),
                        Periode(sykmeldingFom, sykmeldingTom),
                        this.utbetalinger.map { utbetalinger.getValue(it) }.toMutableList(),
                        this.utbetalingstidslinje.konverterTilUtbetalingstidslinje(),
                        forlengelseFraInfotrygd,
                        inntektskilde,
                        opprettet,
                        oppdatert
                    )
            }

            private fun parseTilstand(tilstand: TilstandType) = when (tilstand) {
                TilstandType.AVVENTER_HISTORIKK -> Vedtaksperiode.AvventerHistorikk
                TilstandType.AVVENTER_GODKJENNING -> Vedtaksperiode.AvventerGodkjenning
                TilstandType.AVVENTER_SIMULERING -> Vedtaksperiode.AvventerSimulering
                TilstandType.TIL_UTBETALING -> Vedtaksperiode.TilUtbetaling
                TilstandType.TIL_ANNULLERING -> Vedtaksperiode.TilAnnullering
                TilstandType.AVSLUTTET -> Vedtaksperiode.Avsluttet
                TilstandType.AVSLUTTET_UTEN_UTBETALING -> Vedtaksperiode.AvsluttetUtenUtbetaling
                TilstandType.UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP -> Vedtaksperiode.UtenUtbetalingMedInntektsmeldingUferdigGap
                TilstandType.UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_FORLENGELSE -> Vedtaksperiode.UtenUtbetalingMedInntektsmeldingUferdigForlengelse
                TilstandType.UTBETALING_FEILET -> Vedtaksperiode.UtbetalingFeilet
                TilstandType.TIL_INFOTRYGD -> Vedtaksperiode.TilInfotrygd
                TilstandType.START -> Vedtaksperiode.Start
                TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE -> Vedtaksperiode.MottattSykmeldingFerdigForlengelse
                TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE -> Vedtaksperiode.MottattSykmeldingUferdigForlengelse
                TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP -> Vedtaksperiode.MottattSykmeldingFerdigGap
                TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP -> Vedtaksperiode.MottattSykmeldingUferdigGap
                TilstandType.AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP -> Vedtaksperiode.AvventerArbeidsgiversøknadFerdigGap
                TilstandType.AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP -> Vedtaksperiode.AvventerArbeidsgiversøknadUferdigGap
                TilstandType.AVVENTER_SØKNAD_FERDIG_GAP -> Vedtaksperiode.AvventerSøknadFerdigGap
                TilstandType.AVVENTER_VILKÅRSPRØVING -> Vedtaksperiode.AvventerVilkårsprøving
                TilstandType.AVVENTER_SØKNAD_UFERDIG_GAP -> Vedtaksperiode.AvventerSøknadUferdigGap
                TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP -> Vedtaksperiode.AvventerInntektsmeldingEllerHistorikkFerdigGap
                TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP -> Vedtaksperiode.AvventerInntektsmeldingUferdigGap
                TilstandType.AVVENTER_UFERDIG_GAP -> Vedtaksperiode.AvventerUferdigGap
                TilstandType.AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE -> Vedtaksperiode.AvventerInntektsmeldingFerdigForlengelse
                TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE -> Vedtaksperiode.AvventerInntektsmeldingUferdigForlengelse
                TilstandType.AVVENTER_SØKNAD_UFERDIG_FORLENGELSE -> Vedtaksperiode.AvventerSøknadUferdigForlengelse
                TilstandType.AVVENTER_SØKNAD_FERDIG_FORLENGELSE -> Vedtaksperiode.AvventerSøknadFerdigForlengelse
                TilstandType.AVVENTER_UFERDIG_FORLENGELSE -> Vedtaksperiode.AvventerUferdigForlengelse
                TilstandType.AVVENTER_ARBEIDSGIVERE -> Vedtaksperiode.AvventerArbeidsgivere
                TilstandType.AVVENTER_REVURDERING -> Vedtaksperiode.AvventerRevurdering
                TilstandType.AVSLUTTET_INGEN_ENDRING -> Vedtaksperiode.AvsluttetIngenEndring
                TilstandType.AVVENTER_SIMULERING_REVURDERING -> Vedtaksperiode.AvventerSimuleringRevurdering
                TilstandType.AVVENTER_GODKJENNING_REVURDERING -> Vedtaksperiode.AvventerGodkjenningRevurdering
                TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING -> Vedtaksperiode.AvventerArbeidsgivereRevurdering
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
                            detaljer = detaljer.map { it.parseDetaljer() }
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
        private val id: UUID,
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
                    id,
                    hendelseId,
                    tidsstempel,
                    hendelseSykdomstidslinje.createSykdomstidslinje(),
                    beregnetSykdomstidslinje.createSykdomstidslinje()
                )
        }
    }

    data class UtbetalingData(
        val id: UUID,
        private val beregningId: UUID,
        private val utbetalingstidslinje: UtbetalingstidslinjeData,
        private val arbeidsgiverOppdrag: OppdragData,
        private val personOppdrag: OppdragData,
        private val tidsstempel: LocalDateTime,
        private val type: String,
        private val status: String,
        private val maksdato: LocalDate,
        private val forbrukteSykedager: Int?,
        private val gjenståendeSykedager: Int?,
        private val vurdering: VurderingData?,
        private val overføringstidspunkt: LocalDateTime?,
        private val avstemmingsnøkkel: Long?,
        private val avsluttet: LocalDateTime?,
        private val oppdatert: LocalDateTime?
    ) {

        internal fun konverterTilUtbetaling() = Utbetaling::class.primaryConstructor!!
            .apply { isAccessible = true }
            .call(
                id,
                beregningId,
                utbetalingstidslinje.konverterTilUtbetalingstidslinje(),
                arbeidsgiverOppdrag.konverterTilOppdrag(),
                personOppdrag.konverterTilOppdrag(),
                tidsstempel,
                enumValueOf<Utbetalingstatus>(status).tilTilstand(),
                enumValueOf<Utbetaling.Utbetalingtype>(type),
                maksdato,
                forbrukteSykedager,
                gjenståendeSykedager,
                vurdering?.konverterTilVurdering(),
                overføringstidspunkt,
                avstemmingsnøkkel,
                avsluttet,
                oppdatert
            )

        data class VurderingData(
            private val godkjent: Boolean,
            private val ident: String,
            private val epost: String,
            private val tidspunkt: LocalDateTime,
            private val automatiskBehandling: Boolean
        ) {
            internal fun konverterTilVurdering() = Utbetaling.Vurdering::class.primaryConstructor!!
                .apply { isAccessible = true }
                .call(
                    godkjent,
                    ident,
                    epost,
                    tidspunkt,
                    automatiskBehandling
                )
        }
    }

    data class OppdragData(
        private val mottaker: String,
        private val fagområde: String,
        private val linjer: List<UtbetalingslinjeData>,
        private val fagsystemId: String,
        private val endringskode: String,
        private val sisteArbeidsgiverdag: LocalDate?,
        private val tidsstempel: LocalDateTime,
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
                    nettoBeløp,
                    tidsstempel
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
            MinimumSykdomsgrad,
            EtterDødsdato;

            fun tilBegrunnelse() = when(this) {
                SykepengedagerOppbrukt -> Begrunnelse.SykepengedagerOppbrukt
                MinimumSykdomsgrad -> Begrunnelse.MinimumSykdomsgrad
                EgenmeldingUtenforArbeidsgiverperiode -> Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
                MinimumInntekt -> Begrunnelse.MinimumInntekt
                EtterDødsdato -> Begrunnelse.EtterDødsdato
            }

            internal companion object {
                fun fraBegrunnelse(begrunnelse: Begrunnelse) = when(begrunnelse) {
                    is Begrunnelse.SykepengedagerOppbrukt -> SykepengedagerOppbrukt
                    is Begrunnelse.MinimumSykdomsgrad -> MinimumSykdomsgrad
                    is Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode -> EgenmeldingUtenforArbeidsgiverperiode
                    is Begrunnelse.MinimumInntekt -> MinimumInntekt
                    is Begrunnelse.EtterDødsdato -> EtterDødsdato
                }
            }
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
            private val skjæringstidspunkt: LocalDate?,
            private val totalGrad: Double?,
            private val begrunnelse: BegrunnelseData?,
            private val grad: Double?,
            private val arbeidsgiverBetalingProsent: Double?,
            private val arbeidsgiverbeløp: Int?,
            private val personbeløp: Int?,
            private val er6GBegrenset: Boolean?
        ) {
            private val økonomi
                get() = Økonomi::class.primaryConstructor!!
                    .apply { isAccessible = true }
                    .call(
                        grad?.prosent,
                        arbeidsgiverBetalingProsent?.prosent,
                        aktuellDagsinntekt.daglig,
                        dekningsgrunnlag.daglig,
                        skjæringstidspunkt,
                        totalGrad?.prosent,
                        arbeidsgiverbeløp?.daglig,
                        personbeløp?.daglig,
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
                            dato = dato, økonomi = økonomi, begrunnelse = begrunnelse?.tilBegrunnelse() ?: error("Prøver å deserialisere avvist dag uten begrunnelse")
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
