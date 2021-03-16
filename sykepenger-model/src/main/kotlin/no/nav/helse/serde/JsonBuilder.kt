package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.api.builders.BuilderState
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.collections.set

fun Person.serialize(): SerialisertPerson {
    val jsonBuilder = JsonBuilder()
    this.accept(jsonBuilder)
    return SerialisertPerson(jsonBuilder.toJson())
}

internal class JsonBuilder : AbstractBuilder() {

    private lateinit var personBuilder: PersonState

    internal fun toJson() = personBuilder.build().toString()
    override fun toString() = toJson()

    override fun preVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: String,
        dødsdato: LocalDate?
    ) {
        personBuilder = PersonState(fødselsnummer, aktørId, opprettet, dødsdato)
        pushState(personBuilder)
    }

    private class PersonState(fødselsnummer: String, aktørId: String, opprettet: LocalDateTime, dødsdato: LocalDate?) : BuilderState() {
        private val personMap = mutableMapOf<String, Any?>(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "opprettet" to opprettet,
            "dødsdato" to dødsdato
        )

        private val arbeidsgivere = mutableListOf<MutableMap<String, Any?>>()

        fun build() =
            SerialisertPerson.medSkjemaversjon(serdeObjectMapper.valueToTree<JsonNode>(personMap))

        override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            personMap["aktivitetslogg"] = AktivitetsloggReflect(aktivitetslogg).toMap()
        }

        override fun preVisitArbeidsgivere() {
            personMap["arbeidsgivere"] = arbeidsgivere
        }

        override fun preVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            val arbeidsgiverMap = mutableMapOf<String, Any?>()
            arbeidsgivere.add(arbeidsgiverMap)
            pushState(ArbeidsgiverState(arbeidsgiver, arbeidsgiverMap))
        }

        override fun postVisitPerson(
            person: Person,
            opprettet: LocalDateTime,
            aktørId: String,
            fødselsnummer: String,
            dødsdato: LocalDate?
        ) {
            popState()
        }

        override fun preVisitVilkårsgrunnlagHistorikk() {
            val historikk = mutableListOf<Map<String, Any?>>()
            personMap["vilkårsgrunnlagHistorikk"] = historikk
            pushState(VilkårsgrunnlagHistorikkState(historikk))
        }
    }

    private companion object {
        fun initVedtaksperiodeMap(
            vedtaksperiodeMap: MutableMap<String, Any?>,
            periode: Periode,
            opprinneligPeriode: Periode,
            hendelseIder: List<UUID>,
            inntektskilde: Inntektskilde
        ) {
            vedtaksperiodeMap["fom"] = periode.start
            vedtaksperiodeMap["tom"] = periode.endInclusive
            vedtaksperiodeMap["sykmeldingFom"] = opprinneligPeriode.start
            vedtaksperiodeMap["sykmeldingTom"] = opprinneligPeriode.endInclusive
            vedtaksperiodeMap["hendelseIder"] = hendelseIder
            vedtaksperiodeMap["inntektskilde"] = inntektskilde
        }
    }

    private class ArbeidsgiverState(
        arbeidsgiver: Arbeidsgiver,
        private val arbeidsgiverMap: MutableMap<String, Any?>
    ) : BuilderState() {
        init {
            arbeidsgiverMap.putAll(ArbeidsgiverReflect(arbeidsgiver).toMap())
        }

        override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
            val inntektshistorikkListe = mutableListOf<Map<String, Any?>>()
            arbeidsgiverMap["inntektshistorikk"] = inntektshistorikkListe
            pushState(InntektshistorikkState(inntektshistorikkListe))
        }

        override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            val sykdomshistorikkList = mutableListOf<MutableMap<String, Any?>>()
            arbeidsgiverMap["sykdomshistorikk"] = sykdomshistorikkList
            pushState(SykdomshistorikkState(sykdomshistorikkList))
        }

        private val utbetalingstidslinjer = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {
            arbeidsgiverMap["utbetalingstidslinjer"] = utbetalingstidslinjer
        }

        override fun preVisit(tidslinje: Utbetalingstidslinje) {
            val utbetalingstidslinjeMap = mutableMapOf<String, Any?>()
            utbetalingstidslinjer.add(utbetalingstidslinjeMap)
            pushState(UtbetalingstidslinjeState(utbetalingstidslinjeMap))
        }

        override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            arbeidsgiverMap["utbetalinger"] = mutableListOf<MutableMap<String, Any?>>().also {
                pushState(UtbetalingerState(it))
            }
        }

        private val vedtaksperiodeListe = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            vedtaksperiodeListe.clear()
        }

        override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            arbeidsgiverMap["vedtaksperioder"] = vedtaksperiodeListe.toList()
        }

        override fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
            vedtaksperiodeListe.clear()
        }

        override fun preVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak) {
            val vedtaksperiodeMap = mutableMapOf<String, Any?>()
            vedtaksperiodeListe.add(
                mutableMapOf(
                    "vedtaksperiode" to vedtaksperiodeMap,
                    "årsak" to forkastetÅrsak
                )
            )
            pushState(ForkastetVedtaksperiodeState(vedtaksperiodeMap))
        }

        override fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
            arbeidsgiverMap["forkastede"] = vedtaksperiodeListe.toList()
        }

        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            skjæringstidspunkt: LocalDate,
            periodetype: Periodetype,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: List<UUID>,
            inntektsmeldingId: UUID?,
            inntektskilde: Inntektskilde
        ) {
            val vedtaksperiodeMap = mutableMapOf<String, Any?>()
            initVedtaksperiodeMap(vedtaksperiodeMap, periode, opprinneligPeriode, hendelseIder, inntektskilde)
            vedtaksperiodeListe.add(vedtaksperiodeMap)
            pushState(VedtaksperiodeState(vedtaksperiode, vedtaksperiodeMap))
        }

        override fun postVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            popState()
        }
    }

    private class VilkårsgrunnlagHistorikkState(private val historikk: MutableList<Map<String, Any?>>) : BuilderState() {
        override fun visitGrunnlagsdata(skjæringstidspunkt: LocalDate, grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
            historikk.add(mapOf(
                "skjæringstidspunkt" to skjæringstidspunkt,
                "type" to "Vilkårsprøving",
                "antallOpptjeningsdagerErMinst" to grunnlagsdata.antallOpptjeningsdagerErMinst,
                "avviksprosent" to grunnlagsdata.avviksprosent?.ratio(),
                "sammenligningsgrunnlag" to grunnlagsdata.sammenligningsgrunnlag.reflection { årlig, _, _, _ -> årlig },
                "harOpptjening" to grunnlagsdata.harOpptjening,
                "medlemskapstatus" to when (grunnlagsdata.medlemskapstatus) {
                    Medlemskapsvurdering.Medlemskapstatus.Ja -> JsonMedlemskapstatus.JA
                    Medlemskapsvurdering.Medlemskapstatus.Nei -> JsonMedlemskapstatus.NEI
                    Medlemskapsvurdering.Medlemskapstatus.VetIkke -> JsonMedlemskapstatus.VET_IKKE
                },
                "vurdertOk" to grunnlagsdata.vurdertOk
            ))
        }

        override fun visitInfotrygdVilkårsgrunnlag(skjæringstidspunkt: LocalDate, infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag) {
            historikk.add(mapOf(
                "skjæringstidspunkt" to skjæringstidspunkt,
                "type" to "Infotrygd"
            ))
        }

        override fun postVisitVilkårsgrunnlagHistorikk() {
            popState()
        }
    }

    private class ForkastetVedtaksperiodeState(private val vedtaksperiodeMap: MutableMap<String, Any?>) : BuilderState() {

        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            skjæringstidspunkt: LocalDate,
            periodetype: Periodetype,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: List<UUID>,
            inntektsmeldingId: UUID?,
            inntektskilde: Inntektskilde
        ) {
            initVedtaksperiodeMap(vedtaksperiodeMap, periode, opprinneligPeriode, hendelseIder, inntektskilde)
            pushState(VedtaksperiodeState(vedtaksperiode, vedtaksperiodeMap))
        }

        override fun postVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak) {
            popState()
        }
    }

    private class UtbetalingerState(private val utbetalinger: MutableList<MutableMap<String, Any?>>) : BuilderState() {

        override fun preVisitUtbetaling(
            utbetaling: Utbetaling,
            id: UUID,
            beregningId: UUID,
            type: Utbetaling.Utbetalingtype,
            tilstand: Utbetaling.Tilstand,
            tidsstempel: LocalDateTime,
            oppdatert: LocalDateTime,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?
        ) {
            utbetalinger.add(UtbetalingReflect(utbetaling).toMap())
        }

        override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            popState()
        }
    }

    private class InntektshistorikkState(private val inntekter: MutableList<Map<String, Any?>>) :
        BuilderState() {
        override fun preVisitInnslag(
            innslag: Inntektshistorikk.Innslag,
            id: UUID
        ) {
            val inntektsopplysninger = mutableListOf<Map<String, Any?>>()
            this.inntekter.add(
                mutableMapOf(
                    "id" to id,
                    "inntektsopplysninger" to inntektsopplysninger
                )
            )
            pushState(InntektsendringState(inntektsopplysninger))
        }

        override fun postVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
            popState()
        }
    }

    private class InntektsendringState(private val inntektsopplysninger: MutableList<Map<String, Any?>>) :
        BuilderState() {
        override fun visitSaksbehandler(
            saksbehandler: Inntektshistorikk.Saksbehandler,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(SaksbehandlerReflect(saksbehandler).toMap())
        }

        override fun visitInntektsmelding(
            inntektsmelding: Inntektshistorikk.Inntektsmelding,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(InntektsmeldingReflect(inntektsmelding).toMap())
        }

        override fun preVisitInntektsopplysningKopi(
            inntektsopplysning: Inntektshistorikk.InntektsopplysningReferanse,
            dato: LocalDate,
            hendelseId: UUID,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(InntektsopplysningKopiReflect(inntektsopplysning).toMap())
            pushState(InntektsopplysningKopiState())
        }

        override fun visitInfotrygd(
            infotrygd: Inntektshistorikk.Infotrygd,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(InfotrygdReflect(infotrygd).toMap())
        }

        override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID) {
            val skatteopplysninger = mutableListOf<Map<String, Any?>>()
            this.inntektsopplysninger.add(
                mutableMapOf(
                    "id" to id,
                    "skatteopplysninger" to skatteopplysninger
                )
            )
            pushState(InntektsendringState(skatteopplysninger))
        }


        override fun visitSkattSykepengegrunnlag(
            sykepengegrunnlag: Inntektshistorikk.Skatt.Sykepengegrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Inntektshistorikk.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(SykepengegrunnlagReflect(sykepengegrunnlag).toMap())
        }

        override fun visitSkattSammenligningsgrunnlag(
            sammenligningsgrunnlag: Inntektshistorikk.Skatt.Sammenligningsgrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Inntektshistorikk.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(SammenligningsgrunnlagReflect(sammenligningsgrunnlag).toMap())
        }

        override fun postVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID) = popState()
        override fun postVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) = popState()
    }

    private class InntektsopplysningKopiState : BuilderState() {
        override fun postVisitInntektsopplysningKopi(
            inntektsopplysning: Inntektshistorikk.InntektsopplysningReferanse,
            dato: LocalDate,
            hendelseId: UUID,
            tidsstempel: LocalDateTime
        ) = popState()
    }

    private class UtbetalingstidslinjeState(utbetalingstidslinjeMap: MutableMap<String, Any?>) :
        BuilderState() {

        private val dager = mutableListOf<MutableMap<String, Any?>>()

        init {
            utbetalingstidslinjeMap["dager"] = dager
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.Arbeidsdag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.ArbeidsgiverperiodeDag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.NavDag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.NavHelgDag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.Fridag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.UkjentDag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            val avvistDagMap = mutableMapOf<String, Any?>()
            dager.add(avvistDagMap)

            avvistDagMap.putAll(AvvistdagReflect(dag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            val foreldetDagMap = mutableMapOf<String, Any?>()
            dager.add(foreldetDagMap)

            foreldetDagMap.putAll(UtbetalingsdagReflect(dag, TypeData.ForeldetDag).toMap())
        }

        override fun postVisit(tidslinje: Utbetalingstidslinje) = popState()
    }

    private class VedtaksperiodeState(
        vedtaksperiode: Vedtaksperiode,
        private val vedtaksperiodeMap: MutableMap<String, Any?>
    ) : BuilderState() {
        init {
            vedtaksperiodeMap.putAll(VedtaksperiodeReflect(vedtaksperiode).toMap())
        }

        private var inUtbetaling = false

        override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
            val sykdomstidslinje = mutableMapOf<String, Any>()
            sykdomstidslinje["låstePerioder"] = låstePerioder.map {
                mapOf("fom" to it.start, "tom" to it.endInclusive)
            }
            vedtaksperiodeMap["sykdomstidslinje"] = sykdomstidslinje
            pushState(SykdomstidslinjeState(sykdomstidslinje))
        }

        override fun preVisitUtbetaling(
            utbetaling: Utbetaling,
            id: UUID,
            beregningId: UUID,
            type: Utbetaling.Utbetalingtype,
            tilstand: Utbetaling.Tilstand,
            tidsstempel: LocalDateTime,
            oppdatert: LocalDateTime,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?
        ) {
            inUtbetaling = true
        }

        override fun postVisitUtbetaling(
            utbetaling: Utbetaling,
            id: UUID,
            beregningId: UUID,
            type: Utbetaling.Utbetalingtype,
            tilstand: Utbetaling.Tilstand,
            tidsstempel: LocalDateTime,
            oppdatert: LocalDateTime,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?
        ) {
            inUtbetaling = false
        }

        override fun postVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            skjæringstidspunkt: LocalDate,
            periodetype: Periodetype,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: List<UUID>,
            inntektsmeldingId: UUID?,
            inntektskilde: Inntektskilde
        ) {
            popState()
        }
    }

    private class SykdomshistorikkState(
        private val sykdomshistorikkElementer: MutableList<MutableMap<String, Any?>>
    ) : BuilderState() {
        override fun preVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID,
            hendelseId: UUID?,
            tidsstempel: LocalDateTime
        ) {
            val elementMap = mutableMapOf<String, Any?>()
            sykdomshistorikkElementer.add(elementMap)

            pushState(SykdomshistorikkElementState(id, hendelseId, tidsstempel, elementMap))
        }

        override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            popState()
        }
    }

    private class SykdomshistorikkElementState(
        id: UUID,
        hendelseId: UUID?,
        tidsstempel: LocalDateTime,
        private val elementMap: MutableMap<String, Any?>
    ) : BuilderState() {
        init {
            elementMap["id"] = id
            elementMap["hendelseId"] = hendelseId
            elementMap["tidsstempel"] = tidsstempel
        }

        override fun preVisitHendelseSykdomstidslinje(
            tidslinje: Sykdomstidslinje,
            hendelseId: UUID?,
            tidsstempel: LocalDateTime
        ) {
            val sykdomstidslinje = mutableMapOf<String, Any>()
            elementMap["hendelseSykdomstidslinje"] = sykdomstidslinje
            pushState(SykdomstidslinjeState(sykdomstidslinje))
        }

        override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            val sykdomstidslinje = mutableMapOf<String, Any>()
            elementMap["beregnetSykdomstidslinje"] = sykdomstidslinje
            pushState(SykdomstidslinjeState(sykdomstidslinje))
        }

        override fun postVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID,
            hendelseId: UUID?,
            tidsstempel: LocalDateTime
        ) {
            popState()
        }
    }

    private class SykdomstidslinjeState(private val sykdomstidslinje: MutableMap<String, Any>) : BuilderState() {

        private val dager: MutableList<MutableMap<String, Any>> = mutableListOf()

        override fun preVisitSykdomstidslinje(
            tidslinje: Sykdomstidslinje,
            låstePerioder: List<Periode>
        ) {
            sykdomstidslinje["låstePerioder"] = låstePerioder.map {
                mapOf("fom" to it.start, "tom" to it.endInclusive)
            }
        }

        override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            sykdomstidslinje["dager"] = dager
            popState()
        }

        override fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: Hendelseskilde) = leggTilDag(dag, kilde)

        override fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: Arbeidsgiverdag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) = leggTilDag(dag, kilde)

        override fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: ArbeidsgiverHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: Sykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: ForeldetSykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: SykHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: ProblemDag,
            dato: LocalDate,
            kilde: Hendelseskilde,
            melding: String
        ) = leggTilDag(dag, kilde, melding)

        private fun leggTilDag(
            dag: Dag,
            kilde: Hendelseskilde,
            melding: String? = null
        ) {
            dager.add(serialisertSykdomstidslinjedag(dag, kilde, melding))
        }
    }
}

