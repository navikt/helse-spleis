package no.nav.helse.sak

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.inntektsmelding.Inntektsmelding
import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandling
import no.nav.helse.hendelser.søknad.NySøknad
import no.nav.helse.hendelser.søknad.SendtSøknad
import no.nav.helse.hendelser.ytelser.Ytelser
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.util.*

internal class Arbeidsgiver private constructor(private val organisasjonsnummer: String, private val id: UUID) {
    internal constructor(organisasjonsnummer: String) : this(organisasjonsnummer, UUID.randomUUID())

    internal class Memento internal constructor(
        internal val id: UUID,
        internal val organisasjonsnummer: String,
        internal val saker: List<Vedtaksperiode.Memento>
    ) {
        internal companion object {

            private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())

            fun fromString(state: String): Memento {
                val json = objectMapper.readTree(state)
                return Memento(
                    id = UUID.fromString(json["id"].textValue()),
                    organisasjonsnummer = json["organisasjonsnummer"].textValue(),
                    saker = json["saker"].map {
                        Vedtaksperiode.Memento.fromString(it.toString())
                    }
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
                this.saker.fold(it.putArray("saker")) { result, current ->
                    result.addRawValue(RawValue(current.state()))
                }
            }.toString()
    }

    internal companion object {
        fun restore(memento: Memento): Arbeidsgiver {
            return Arbeidsgiver(
                id = memento.id,
                organisasjonsnummer = memento.organisasjonsnummer
            ).apply {
                this.perioder.addAll(memento.saker.map {
                    Vedtaksperiode.restore(it)
                })
            }
        }

    }

    private val perioder = mutableListOf<Vedtaksperiode>()
    private val vedtaksperiodeObservers = mutableListOf<VedtaksperiodeObserver>()

    internal fun organisasjonsnummer() = organisasjonsnummer

    internal fun memento() = Memento(
        id = this.id,
        organisasjonsnummer = this.organisasjonsnummer,
        saker = this.perioder.map { it.memento() }
    )

    internal fun håndter(nySøknad: NySøknad) {
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
        if (perioder.none { it.håndter(inntektsmelding) }) {
            nyVedtaksperiode(inntektsmelding).håndter(inntektsmelding)
        }
    }

    internal fun håndter(sak: Sak, ytelser: Ytelser) {
        perioder.forEach { it.håndter(sak, this, ytelser) }
    }

    internal fun håndter(manuellSaksbehandling: ManuellSaksbehandling) {
        perioder.forEach { it.håndter(manuellSaksbehandling) }
    }

    internal fun håndter(påminnelse: Påminnelse) {
        perioder.forEach { it.håndter(påminnelse) }
    }

    internal fun invaliderSaker(hendelse: ArbeidstakerHendelse) {
        perioder.forEach { it.invaliderPeriode(hendelse) }
    }

    fun addObserver(observer: VedtaksperiodeObserver) {
        vedtaksperiodeObservers.add(observer)
        perioder.forEach { it.addVedtaksperiodeObserver(observer) }
    }

    private fun <Hendelse> nyVedtaksperiode(hendelse: Hendelse): Vedtaksperiode where Hendelse : SykdomstidslinjeHendelse, Hendelse : ArbeidstakerHendelse {
        return Vedtaksperiode.nyPeriode(hendelse).also {
            vedtaksperiodeObservers.forEach(it::addVedtaksperiodeObserver)
            perioder.add(it)
        }
    }

}
