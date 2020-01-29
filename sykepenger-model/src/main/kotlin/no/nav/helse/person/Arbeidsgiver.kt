package no.nav.helse.person

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class Arbeidsgiver private constructor(
    private val organisasjonsnummer: String,
    private val id: UUID,
    private val inntekthistorikk: Inntekthistorikk
) {

    internal constructor(organisasjonsnummer: String) : this(organisasjonsnummer, UUID.randomUUID(), Inntekthistorikk())

    internal class Memento internal constructor(
        internal val id: UUID,
        internal val organisasjonsnummer: String,
        internal val perioder: List<Vedtaksperiode.Memento>,
        internal val inntekthistorikk: Inntekthistorikk.Memento
    ) {
        internal companion object {

            private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())

            fun fromString(state: String): Memento {
                val json = objectMapper.readTree(state)
                return Memento(
                    id = UUID.fromString(json["id"].textValue()),
                    organisasjonsnummer = json["organisasjonsnummer"].textValue(),
                    perioder = json["saker"].map {
                        Vedtaksperiode.Memento.fromJsonNode(it)
                    },
                    inntekthistorikk = json["inntektHistorie"]?.takeUnless { it.isNull }?.let { Inntekthistorikk.Memento.fromJsonNode(it) }?: Inntekthistorikk.Memento()
                )
            }

        }

        fun state(): String =
            objectMapper.convertValue<ObjectNode>(
                mapOf(
                    "id" to this.id,
                    "organisasjonsnummer" to this.organisasjonsnummer
                )
            ).also {
                this.perioder.fold(it.putArray("saker")) { result, current ->
                    result.addRawValue(RawValue(current.state()))
                }
                it.set<ObjectNode>("inntektHistorie", this.inntekthistorikk.state())
            }.toString()
    }

    internal companion object {
        fun restore(memento: Memento): Arbeidsgiver {
            return Arbeidsgiver(
                id = memento.id,
                organisasjonsnummer = memento.organisasjonsnummer,
                inntekthistorikk = Inntekthistorikk.restore(memento.inntekthistorikk)
            ).apply {
                this.perioder.addAll(memento.perioder.map {
                    Vedtaksperiode.restore(it)
                })
            }
        }

    }

    private val tidslinjer = mutableListOf<Utbetalingstidslinje>()
    private val perioder = mutableListOf<Vedtaksperiode>()
    private val vedtaksperiodeObservers = mutableListOf<VedtaksperiodeObserver>()
    private val aktivitetslogger = Aktivitetslogger()

    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitArbeidsgiver(this)
        visitor.visitArbeidsgiverAktivitetslogger(aktivitetslogger)
        inntekthistorikk.accept(visitor)
        visitor.preVisitTidslinjer()
        tidslinjer.forEach { it.accept(visitor) }
        visitor.postVisitTidslinjer()
        visitor.preVisitPerioder()
        perioder.forEach { it.accept(visitor) }
        visitor.postVisitPerioder()
        visitor.postVisitArbeidsgiver(this)
    }

    internal fun organisasjonsnummer() = organisasjonsnummer

    internal fun memento() = Memento(
        id = this.id,
        organisasjonsnummer = this.organisasjonsnummer,
        perioder = this.perioder.map { it.memento() },
        inntekthistorikk = this.inntekthistorikk.memento()
    )

    internal fun peekTidslinje() = tidslinjer.last()

    internal fun push(tidslinje: Utbetalingstidslinje) = tidslinjer.add(tidslinje)

    internal fun håndter(nySøknad: ModelNySøknad) {
        if (!perioder.fold(false) { håndtert, periode -> håndtert || periode.håndter(nySøknad) }) {
            nyVedtaksperiode(nySøknad).håndter(nySøknad)
        }
        nySøknad.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(sendtSøknad: ModelSendtSøknad) {
        if (perioder.none { it.håndter(sendtSøknad) }) {
            sendtSøknad.error("Uventet sendt søknad, mangler ny søknad")
        }
        sendtSøknad.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(inntektsmelding: ModelInntektsmelding) {
        inntekthistorikk.add(
            inntektsmelding.førsteFraværsdag,
            inntektsmelding,
            inntektsmelding.beregnetInntekt.toBigDecimal()
        )
        if (perioder.none { it.håndter(inntektsmelding) }) {
            inntektsmelding.error("Uventet inntektsmelding, mangler ny søknad")
        }
        inntektsmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(person: Person, ytelser: ModelYtelser) {
        perioder.forEach { it.håndter(person, this, ytelser) }
        ytelser.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(manuellSaksbehandling: ModelManuellSaksbehandling) {
        perioder.forEach { it.håndter(manuellSaksbehandling) }
        manuellSaksbehandling.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
        perioder.forEach { it.håndter(vilkårsgrunnlag) }
        vilkårsgrunnlag.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(påminnelse: ModelPåminnelse) =
        perioder.any { it.håndter(påminnelse) }.also {
            påminnelse.kopierAktiviteterTil(aktivitetslogger)
        }

    internal fun sykdomstidslinje(): ConcreteSykdomstidslinje? =
        Vedtaksperiode.sykdomstidslinje(perioder)

    internal fun inntekt(dato: LocalDate): BigDecimal? =
        inntekthistorikk.inntekt(dato)

    internal fun invaliderPerioder(hendelse: ArbeidstakerHendelse) {
        perioder.forEach { it.invaliderPeriode(hendelse) }
    }

    fun addObserver(observer: VedtaksperiodeObserver) {
        vedtaksperiodeObservers.add(observer)
        perioder.forEach { it.addVedtaksperiodeObserver(observer) }
    }

    private fun nyVedtaksperiode(hendelse: SykdomstidslinjeHendelse): Vedtaksperiode {
        return Vedtaksperiode.nyPeriode(hendelse).also {
            vedtaksperiodeObservers.forEach(it::addVedtaksperiodeObserver)
            perioder.add(it)
        }
    }

}
