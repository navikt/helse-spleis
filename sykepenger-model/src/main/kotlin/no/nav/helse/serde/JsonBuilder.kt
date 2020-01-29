package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.util.*

internal class JsonBuilder : PersonVisitor {

    private val stack: Stack<JsonState> = Stack()

    init {
        stack.push(Root())
    }

    private val currentState: JsonState
        get() = stack.peek()

    override fun toString() = currentState.toJson()

    private fun pushState(state: JsonState) {
        currentState.leaving()
        stack.push(state)
        currentState.entering()
    }

    private fun popState() {
        currentState.leaving()
        stack.pop()
        currentState.entering()
    }

    override fun preVisitPerson(person: Person) = currentState.preVisitPerson(person)
    override fun postVisitPerson(person: Person) = currentState.postVisitPerson(person)
    override fun visitPersonAktivitetslogger(aktivitetslogger: Aktivitetslogger) = currentState.visitPersonAktivitetslogger(aktivitetslogger)
    override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver) =
        currentState.preVisitArbeidsgiver(arbeidsgiver)

    override fun postVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver) =
        currentState.postVisitArbeidsgiver(arbeidsgiver)

    override fun preVisitArbeidsgivere() = currentState.preVisitArbeidsgivere()
    override fun postVisitArbeidsgivere() = currentState.postVisitArbeidsgivere()
    override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) =
        currentState.preVisitInntekthistorikk(inntekthistorikk)

    override fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        currentState.postVisitInntekthistorikk(inntekthistorikk)
    }

    override fun preVisitInntekter() = currentState.preVisitInntekter()
    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) = currentState.visitInntekt(inntekt)
    override fun preVisitTidslinjer() = currentState.preVisitTidslinjer()
    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) =
        currentState.preVisitUtbetalingstidslinje(tidslinje)

    override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) =
        currentState.visitArbeidsdag(dag)
    override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) =
        currentState.visitArbeidsgiverperiodeDag(dag)
    override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) =
        currentState.visitNavDag(dag)
    override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) =
        currentState.visitNavHelgDag(dag)
    override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) =
        currentState.visitFridag(dag)
    override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) =
        currentState.visitAvvistDag(dag)
    override fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) =
        currentState.visitUkjentDag(dag)


    override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) =
        currentState.postVisitUtbetalingstidslinje(tidslinje)

    override fun preVisitPerioder() = currentState.preVisitPerioder()
    override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) =
        currentState.preVisitVedtaksperiode(vedtaksperiode)

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) =
        currentState.preVisitSykdomshistorikk(sykdomshistorikk)

    override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) =
        currentState.preVisitSykdomshistorikkElement(element)

    override fun visitHendelse(hendelse: SykdomstidslinjeHendelse) = currentState.visitHendelse(hendelse)

    override fun preVisitHendelseSykdomstidslinje() = currentState.preVisitHendelseSykdomstidslinje()
    override fun postVisitHendelseSykdomstidslinje() = currentState.postVisitHendelseSykdomstidslinje()
    override fun preVisitBeregnetSykdomstidslinje() = currentState.preVisitBeregnetSykdomstidslinje()
    override fun postVisitBeregnetSykdomstidslinje() = currentState.postVisitBeregnetSykdomstidslinje()
    override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) =
        currentState.preVisitComposite(compositeSykdomstidslinje)

    override fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) =
        currentState.postVisitComposite(compositeSykdomstidslinje)

    override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) =
        currentState.postVisitSykdomshistorikkElement(element)

    override fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) =
        currentState.postVisitVedtaksperiode(vedtaksperiode)

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) = currentState.visitDag(arbeidsdag)
    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) = currentState.visitDag(egenmeldingsdag)
    override fun visitFeriedag(feriedag: Feriedag) = currentState.visitDag(feriedag)
    override fun visitImplisittDag(implisittDag: ImplisittDag) = currentState.visitDag(implisittDag)
    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) = currentState.visitDag(permisjonsdag)
    override fun visitStudiedag(studiedag: Studiedag) = currentState.visitDag(studiedag)
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) = currentState.visitDag(sykHelgedag)
    override fun visitSykedag(sykedag: Sykedag) = currentState.visitDag(sykedag)
    override fun visitUbestemt(ubestemtdag: Ubestemtdag) = currentState.visitDag(ubestemtdag)
    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) = currentState.visitDag(utenlandsdag)
    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) =
        currentState.visitTilstand(tilstand)

    override fun preVisitVedtaksperiodeSykdomstidslinje() = currentState.preVisitVedtaksperiodeSykdomstidslinje()
    override fun postVisitVedtaksperiodeSykdomstidslinje() = currentState.postVisitVedtaksperiodeSykdomstidslinje()
    override fun preVisitUtbetalingslinjer() = currentState.preVisitUtbetalingslinjer()
    override fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) =
        currentState.visitUtbetalingslinje(utbetalingslinje)

    override fun postVisitUtbetalingslinjer() = currentState.postVisitUtbetalingslinjer()
    override fun preVisitHendelser() = currentState.preVisitHendelser()
    override fun postVisitHendelser() = currentState.postVisitHendelser()
    override fun visitInntektsmeldingHendelse(inntektsmelding: ModelInntektsmelding) =
        currentState.visitInntektsmeldingHendelse(inntektsmelding)

    override fun visitManuellSaksbehandlingHendelse(manuellSaksbehandling: ModelManuellSaksbehandling) =
        currentState.visitManuellSaksbehandlingHendelse(manuellSaksbehandling)

    override fun visitNySøknadHendelse(nySøknad: ModelNySøknad) = currentState.visitNySøknadHendelse(nySøknad)
    override fun visitPåminnelseHendelse(påminnelse: ModelPåminnelse) =
        currentState.visitPåminnelseHendelse(påminnelse)

    override fun visitSendtSøknadHendelse(sendtSøknad: ModelSendtSøknad) =
        currentState.visitSendtSøknadHendelse(sendtSøknad)

    override fun visitVilkårsgrunnlagHendelse(vilkårsgrunnlag: ModelVilkårsgrunnlag) =
        currentState.visitVilkårsgrunnlagHendelse(vilkårsgrunnlag)

    override fun visitYtelserHendelse(ytelser: ModelYtelser) = currentState.visitYtelserHendelse(ytelser)

    private interface JsonState : PersonVisitor {
        fun entering() {}
        fun leaving() {}
        fun toJson(): String = throw RuntimeException("toJson() kan bare kalles på rotnode. Ble kalt på ${toString()}")
        fun visitDag(dag: Dag) {}
    }

    private inner class Root : JsonState {
        private val personMap = mutableMapOf<String, Any?>()

        override fun preVisitPerson(person: Person) {
            pushState(PersonState(person, personMap))
        }

        override fun toString() = personMap.toString()

        override fun toJson(): String = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
            .valueToTree<JsonNode>(personMap)
            .toPrettyString()
    }

    private inner class PersonState(person: Person, private val personMap: MutableMap<String, Any?>) : JsonState {
        init {
            personMap.putAll(PersonReflect(person).toMap())
        }

        override fun preVisitHendelser() {
            val hendelser = mutableListOf<MutableMap<String, Any?>>()
            personMap["hendelser"] = hendelser
            pushState(HendelseState(hendelser))
        }

        override fun visitPersonAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            personMap["aktivitetslogger"] = AktivitetsloggerReflect(aktivitetslogger).toMap()
        }

        private val arbeidsgivere = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitArbeidsgivere() {
            personMap["arbeidsgivere"] = arbeidsgivere
        }

        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver) {
            val arbeidsgiverMap = mutableMapOf<String, Any?>()
            arbeidsgivere.add(arbeidsgiverMap)
            pushState(ArbeidsgiverState(arbeidsgiver, arbeidsgiverMap))
        }

        override fun postVisitPerson(person: Person) {
            popState()
        }
    }

    private inner class HendelseState(private val hendelser: MutableList<MutableMap<String, Any?>>) : JsonState {
        override fun visitInntektsmeldingHendelse(inntektsmelding: ModelInntektsmelding) {
            hendelser.add(InntektsmeldingReflect(inntektsmelding).toMap())

            // (private val (.+): .+),
            // $1 = inntektsmelding.getProp("$2")
        }

        override fun visitManuellSaksbehandlingHendelse(
            manuellSaksbehandling: ModelManuellSaksbehandling
        ) {
            hendelser.add(ManuellSaksbehandlingReflect(manuellSaksbehandling).toMap())
        }

        override fun visitNySøknadHendelse(nySøknad: ModelNySøknad) {
            hendelser.add(NySøknadReflect(nySøknad).toMap())
        }

        override fun visitSendtSøknadHendelse(sendtSøknad: ModelSendtSøknad) {
            hendelser.add(SendtSøknadReflect(sendtSøknad).toMap())
        }

        override fun visitVilkårsgrunnlagHendelse(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
            hendelser.add(VilkårsgrunnlagReflect(vilkårsgrunnlag).toMap())
        }

        override fun visitYtelserHendelse(ytelser: ModelYtelser) {
            hendelser.add(YtelserReflect(ytelser).toMap())
        }

        override fun postVisitHendelser() {
            popState()
        }
    }

    private inner class ArbeidsgiverState(arbeidsgiver: Arbeidsgiver, private val arbeidsgiverMap: MutableMap<String, Any?>) :
        JsonState {
        init {
            arbeidsgiverMap.putAll(ArbeidsgiverReflect(arbeidsgiver).toMap())
        }

        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            val inntekter = mutableListOf<MutableMap<String, Any?>>()
            arbeidsgiverMap["inntekter"] = inntekter
            pushState(InntektHistorieState(inntekter))
        }

        private val utbetalingstidslinjer = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitTidslinjer() {
            arbeidsgiverMap["utbetalingstidslinjer"] = utbetalingstidslinjer
        }

        override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
            val utbetalingstidslinjeMap = mutableMapOf<String, Any?>()
            utbetalingstidslinjer.add(utbetalingstidslinjeMap)
            pushState(UtbetalingstidslinjeState(utbetalingstidslinjeMap))
        }

        private val vedtaksperioder = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitPerioder() {
            arbeidsgiverMap["vedtaksperioder"] = vedtaksperioder
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
            val vedtaksperiodeMap = mutableMapOf<String, Any?>()
            vedtaksperioder.add(vedtaksperiodeMap)
            pushState(VedtaksperiodeState(vedtaksperiode, vedtaksperiodeMap))

        }

        override fun postVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver) {
            popState()
        }
    }

    private inner class InntektHistorieState(private val inntekter: MutableList<MutableMap<String, Any?>>) : JsonState {
        override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {
            val inntektMap = mutableMapOf<String, Any?>()
            inntekter.add(inntektMap)

            inntektMap.putAll(InntektReflect(inntekt).toMap())
        }

        override fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            popState()
        }
    }

    private inner class UtbetalingstidslinjeState(utbetalingstidslinjeMap: MutableMap<String, Any?>) :
        JsonState {

        private val dager = mutableListOf<MutableMap<String, Any?>>()

        init {
            utbetalingstidslinjeMap["dager"] = dager
        }

        override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
            dager.add(UtbetalingsdagReflect(dag, "Arbeidsdag").toMap())
        }

        override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
            dager.add(UtbetalingsdagReflect(dag, "ArbeidsgiverperiodeDag").toMap())
        }
        override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
            dager.add(UtbetalingsdagReflect(dag, "NavDag").toMap())
        }
        override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
            dager.add(UtbetalingsdagReflect(dag, "NavHelgDag").toMap())
        }
        override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
            dager.add(UtbetalingsdagReflect(dag, "Fridag").toMap())
        }
        override fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {
            dager.add(UtbetalingsdagReflect(dag, "UkjentDag").toMap())
        }

        override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
            val avvistDagMap = mutableMapOf<String, Any?>()
            dager.add(avvistDagMap)

            avvistDagMap.putAll(AvvistdagReflect(dag).toMap())
        }

        override fun postVisitUtbetalingstidslinje(utbetalingstidslinje: Utbetalingstidslinje) {
            popState()
        }
    }

    private inner class VedtaksperiodeState(
        vedtaksperiode: Vedtaksperiode,
        private val vedtaksperiodeMap: MutableMap<String, Any?>
    ) : JsonState {
        init {
            vedtaksperiodeMap.putAll(VedtaksperiodeReflect(vedtaksperiode).toMap())
        }

        private val sykdomshistorikkElementer = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            vedtaksperiodeMap["sykdomshistorikk"] = sykdomshistorikkElementer
        }

        override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) {
            val elementMap = mutableMapOf<String, Any?>()
            sykdomshistorikkElementer.add(elementMap)

            pushState(SykdomshistorikkElementState(element, elementMap))
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            vedtaksperiodeMap["tilstand"] = tilstand.type.name
        }

        override fun preVisitVedtaksperiodeSykdomstidslinje() {
            val sykdomstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            vedtaksperiodeMap["sykdomstidslinje"] = sykdomstidslinjeListe
            pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun preVisitUtbetalingslinjer() {
            val utbetalingstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            vedtaksperiodeMap["utbetalingslinjer"] = utbetalingstidslinjeListe
            pushState(UtbetalingslinjeState(utbetalingstidslinjeListe))
        }

        override fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
            popState()
        }
    }

    private inner class UtbetalingslinjeState(private val utbetalingstidslinjeListe: MutableList<MutableMap<String, Any?>>) :
        JsonState {
        override fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) {
            val utbetalingstidslinjeMap = mutableMapOf<String, Any?>(
                "fom" to utbetalingslinje.fom,
                "tom" to utbetalingslinje.tom,
                "dagsats" to utbetalingslinje.dagsats
            )
            utbetalingstidslinjeListe.add(utbetalingstidslinjeMap)
        }

        override fun postVisitUtbetalingslinjer() {
            popState()
        }
    }

    private inner class SykdomshistorikkElementState(
        element: Sykdomshistorikk.Element,
        private val elementMap: MutableMap<String, Any?>
    ) : JsonState {
        init {
            elementMap["tidsstempel"] = element.tidsstempel
        }

        override fun visitHendelse(hendelse: SykdomstidslinjeHendelse) {
            elementMap["hendelseId"] = hendelse.hendelseId()
        }

        override fun preVisitHendelseSykdomstidslinje() {
            val sykdomstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            elementMap["hendelseSykdomstidslinje"] = sykdomstidslinjeListe
            pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun preVisitBeregnetSykdomstidslinje() {
            val sykdomstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            elementMap["beregnetSykdomstidslinje"] = sykdomstidslinjeListe
            pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) {
            popState()
        }
    }

    private inner class SykdomstidslinjeState(private val sykdomstidslinjeListe: MutableList<MutableMap<String, Any?>>) :
        JsonState {

        override fun visitDag(dag: Dag) {
            fun mapDag(dagen: Dag): MutableMap<String, Any?> = mutableMapOf(
                "dagen" to dagen.dagen,
                "hendelseId" to dagen.hendelse.hendelseId(),
                "type" to dagen.dagType().name,
                "erstatter" to dagen.erstatter.map { mapDag(it) }
            )

            sykdomstidslinjeListe.add(mapDag(dag))
        }

        override fun postVisitHendelseSykdomstidslinje() {
            popState()
        }

        override fun postVisitBeregnetSykdomstidslinje() {
            popState()
        }

        override fun postVisitVedtaksperiodeSykdomstidslinje() {
            popState()
        }
    }
}
