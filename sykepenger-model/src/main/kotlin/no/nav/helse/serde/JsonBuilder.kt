package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData.TypeData
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
    val jsonBuilder = JsonBuilder(this)
    return SerialisertPerson(jsonBuilder.toJson())
}

internal class JsonBuilder(person: Person) {

    private val root = Root()
    private val stack = mutableListOf<PersonVisitor>(root)

    init {
        person.accept(DelegatedPersonVisitor(stack::first))
    }

    internal fun toJson() = SerialisertPerson.medSkjemaversjon(root.toJson()).toString()
    override fun toString() = toJson()

    private fun pushState(state: PersonVisitor) {
        stack.add(0, state)
    }

    private fun popState() {
        stack.removeAt(0)
    }

    private inner class Root : PersonVisitor {
        private val personMap = mutableMapOf<String, Any?>()

        override fun preVisitPerson(
            person: Person,
            opprettet: LocalDateTime,
            aktørId: String,
            fødselsnummer: String
        ) {
            pushState(PersonState(person, personMap))
        }

        fun toJson(): JsonNode = serdeObjectMapper.valueToTree<JsonNode>(personMap)
    }

    private inner class PersonState(person: Person, private val personMap: MutableMap<String, Any?>) : PersonVisitor {
        init {
            personMap.putAll(PersonReflect(person).toMap())
        }

        override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            personMap["aktivitetslogg"] = AktivitetsloggReflect(aktivitetslogg).toMap()
        }

        private val arbeidsgivere = mutableListOf<MutableMap<String, Any?>>()

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
            fødselsnummer: String
        ) {
            popState()
        }
    }

    private inner class ArbeidsgiverState(
        arbeidsgiver: Arbeidsgiver,
        private val arbeidsgiverMap: MutableMap<String, Any?>
    ) :
        PersonVisitor {
        init {
            arbeidsgiverMap.putAll(ArbeidsgiverReflect(arbeidsgiver).toMap())
        }

        override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
            val inntekter = mutableListOf<MutableMap<String, Any?>>()
            arbeidsgiverMap["inntekter"] = inntekter
            pushState(InntektHistorieState(inntekter))
        }

        override fun preVisitInntekthistorikkVol2(inntektshistorikk: InntektshistorikkVol2) {
            val inntektshistorikkListe = mutableListOf<Map<String, Any?>>()
            arbeidsgiverMap["inntektshistorikk"] = inntektshistorikkListe
            pushState(InntektshistorikkVol2State(inntektshistorikkListe))
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
            hendelseIder: List<UUID>
        ) {
            val vedtaksperiodeMap = mutableMapOf<String, Any?>()
            initVedtaksperiodeMap(vedtaksperiodeMap, periode, opprinneligPeriode, hendelseIder)
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

    private inner class ForkastetVedtaksperiodeState(private val vedtaksperiodeMap: MutableMap<String, Any?>) : PersonVisitor {

        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            hendelseIder: List<UUID>
        ) {
            initVedtaksperiodeMap(vedtaksperiodeMap, periode, opprinneligPeriode, hendelseIder)
            pushState(VedtaksperiodeState(vedtaksperiode, vedtaksperiodeMap))
        }

        override fun postVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak) {
            popState()
        }
    }

    private fun initVedtaksperiodeMap(
        vedtaksperiodeMap: MutableMap<String, Any?>,
        periode: Periode,
        opprinneligPeriode: Periode,
        hendelseIder: List<UUID>
    ) {
        vedtaksperiodeMap["fom"] = periode.start
        vedtaksperiodeMap["tom"] = periode.endInclusive
        vedtaksperiodeMap["sykmeldingFom"] = opprinneligPeriode.start
        vedtaksperiodeMap["sykmeldingTom"] = opprinneligPeriode.endInclusive
        vedtaksperiodeMap["hendelseIder"] = hendelseIder
    }

    private inner class UtbetalingerState(private val utbetalinger: MutableList<MutableMap<String, Any?>>) : PersonVisitor {

        override fun preVisitUtbetaling(
            utbetaling: Utbetaling,
            id: UUID,
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

    private inner class InntektHistorieState(private val inntekter: MutableList<MutableMap<String, Any?>>) : PersonVisitor {
        override fun visitInntekt(inntektsendring: Inntektshistorikk.Inntektsendring, id: UUID) {
            val inntektMap = mutableMapOf<String, Any?>()
            inntekter.add(inntektMap)

            inntektMap.putAll(InntektsendringReflect(inntektsendring).toMap())
        }

        override fun postVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
            popState()
        }
    }

    private inner class InntektshistorikkVol2State(private val inntekter: MutableList<Map<String, Any?>>) :
        PersonVisitor {
        override fun preVisitInnslag(
            innslag: InntektshistorikkVol2.Innslag
        ) {
            val inntektsopplysninger = mutableListOf<Map<String, Any?>>()
            this.inntekter.add(
                mutableMapOf(
                    "inntektsopplysninger" to inntektsopplysninger
                )
            )
            pushState(InntektsendringVol2State(inntektsopplysninger))
        }

        override fun postVisitInntekthistorikkVol2(inntektshistorikk: InntektshistorikkVol2) {
            popState()
        }
    }

    private inner class InntektsendringVol2State(private val inntektsopplysninger: MutableList<Map<String, Any?>>) :
        PersonVisitor {
        override fun visitSaksbehandler(
            saksbehandler: InntektshistorikkVol2.Saksbehandler,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(SaksbehandlerVol2Reflect(saksbehandler).toMap())
        }

        override fun visitInntektsmelding(
            inntektsmelding: InntektshistorikkVol2.Inntektsmelding,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(InntektsmeldingVol2Reflect(inntektsmelding).toMap())
        }

        override fun visitInntektsopplysningKopi(
            inntektsopplysning: InntektshistorikkVol2.InntektsopplysningKopi,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(InntektsopplysningKopiVol2Reflect(inntektsopplysning).toMap())
        }

        override fun visitInfotrygd(
            infotrygd: InntektshistorikkVol2.Infotrygd,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(InfotrygdVol2Reflect(infotrygd).toMap())
        }

        override fun preVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite) {
            val skatteopplysninger = mutableListOf<Map<String, Any?>>()
            this.inntektsopplysninger.add(mutableMapOf("skatteopplysninger" to skatteopplysninger))
            pushState(InntektsendringVol2State(skatteopplysninger))
        }


        override fun visitSkattSykepengegrunnlag(
            sykepengegrunnlag: InntektshistorikkVol2.Skatt.Sykepengegrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: InntektshistorikkVol2.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(SykepengegrunnlagVol2Reflect(sykepengegrunnlag).toMap())
        }

        override fun visitSkattSammenligningsgrunnlag(
            sammenligningsgrunnlag: InntektshistorikkVol2.Skatt.Sammenligningsgrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: InntektshistorikkVol2.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(SammenligningsgrunnlagVol2Reflect(sammenligningsgrunnlag).toMap())
        }

        override fun postVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite) = popState()
        override fun postVisitInnslag(innslag: InntektshistorikkVol2.Innslag) = popState()
    }

    private inner class UtbetalingstidslinjeState(utbetalingstidslinjeMap: MutableMap<String, Any?>) :
        PersonVisitor {

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

    private inner class VedtaksperiodeState(
        vedtaksperiode: Vedtaksperiode,
        private val vedtaksperiodeMap: MutableMap<String, Any?>
    ) : PersonVisitor {
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
            opprinneligPeriode: Periode
        ) {
            popState()
        }
    }

    private inner class SykdomshistorikkState(
        private val sykdomshistorikkElementer: MutableList<MutableMap<String, Any?>>
    ) : PersonVisitor {
        override fun preVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID?,
            tidsstempel: LocalDateTime
        ) {
            val elementMap = mutableMapOf<String, Any?>()
            sykdomshistorikkElementer.add(elementMap)

            pushState(SykdomshistorikkElementState(id, tidsstempel, elementMap))
        }

        override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            popState()
        }
    }

    private inner class SykdomshistorikkElementState(
        id: UUID?,
        tidsstempel: LocalDateTime,
        private val elementMap: MutableMap<String, Any?>
    ) : PersonVisitor {
        init {
            elementMap["hendelseId"] = id
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
            id: UUID?,
            tidsstempel: LocalDateTime
        ) {
            popState()
        }
    }

    private inner class SykdomstidslinjeState(private val sykdomstidslinje: MutableMap<String, Any>) : PersonVisitor {

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

