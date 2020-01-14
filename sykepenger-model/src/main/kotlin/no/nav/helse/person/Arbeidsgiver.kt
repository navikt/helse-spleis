package no.nav.helse.person

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.util.*

internal class Arbeidsgiver private constructor(private val organisasjonsnummer: String, private val id: UUID, private val inntektHistorie: InntektHistorie) {

    internal constructor(organisasjonsnummer: String) : this(organisasjonsnummer, UUID.randomUUID(),
        InntektHistorie()
    )

    internal class Memento internal constructor(
        internal val id: UUID,
        internal val organisasjonsnummer: String,
        internal val perioder: List<Vedtaksperiode.Memento>,
        internal val inntektHistorie: InntektHistorie.Memento
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
                    inntektHistorie = json["inntektHistorie"]?.takeUnless { it.isNull }?.let { InntektHistorie.Memento.fromJsonNode(it) }?: InntektHistorie.Memento()
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
                it.set<ObjectNode>("inntektHistorie", this.inntektHistorie.state())
            }.toString()
    }

    internal companion object {
        fun restore(memento: Memento): Arbeidsgiver {
            return Arbeidsgiver(
                id = memento.id,
                organisasjonsnummer = memento.organisasjonsnummer,
                inntektHistorie = InntektHistorie.restore(memento.inntektHistorie)
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

    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitArbeidsgiver(this)
        tidslinjer.forEach { it.accept(visitor) }
        perioder.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgiver(this)
    }

    internal fun organisasjonsnummer() = organisasjonsnummer

    internal fun memento() = Memento(
        id = this.id,
        organisasjonsnummer = this.organisasjonsnummer,
        perioder = this.perioder.map { it.memento() },
        inntektHistorie = this.inntektHistorie.memento()
    )

    internal fun peekTidslinje() = tidslinjer.last()

    internal fun push(tidslinje: Utbetalingstidslinje) = tidslinjer.add(tidslinje)

    internal fun håndter(nySøknad: NySøknad) {
        if (!perioder.fold(false) { håndtert, periode ->
                håndtert || periode.håndter(nySøknad)
            }) {
            nyVedtaksperiode(nySøknad).håndter(nySøknad)
        }
    }

    internal fun håndter(nySøknad: ModelNySøknad) {
        if (!perioder.fold(false) { håndtert, periode ->
                håndtert || periode.håndter(nySøknad)
            }) {
            nyVedtaksperiode(nySøknad).håndter(nySøknad)
        }
    }

    internal fun håndter(sendtSøknad: SendtSøknad) {
        if (perioder.none { it.håndter(sendtSøknad) }) {
            nyVedtaksperiode(sendtSøknad).håndter(sendtSøknad)
        }
    }

    internal fun håndter(inntektsmelding: Inntektsmelding) {
        inntektHistorie.add(inntektsmelding)
        if (perioder.none { it.håndter(inntektsmelding) }) {
            nyVedtaksperiode(inntektsmelding).håndter(inntektsmelding)
        }
    }

    internal fun håndter(person: Person, ytelser: Ytelser) {
        perioder.forEach { it.håndter(person, this, ytelser) }
    }

    internal fun håndter(manuellSaksbehandling: ManuellSaksbehandling) {
        perioder.forEach { it.håndter(manuellSaksbehandling) }
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        perioder.forEach { it.håndter(vilkårsgrunnlag) }
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        return perioder.any { it.håndter(påminnelse) }
    }

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
