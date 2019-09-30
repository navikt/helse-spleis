package no.nav.helse.sakskompleks.domain

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Event
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.sykmelding.domain.gjelderTil
import no.nav.helse.søknad.domain.Sykepengesøknad
import java.io.StringWriter
import java.time.LocalDate
import java.util.*

class Sakskompleks internal constructor(private val id: UUID,
                                        private val aktørId: String) {

    private val sykmeldinger: MutableList<Sykmelding> = mutableListOf()
    private val søknader: MutableList<Sykepengesøknad> = mutableListOf()
    private val inntektsmeldinger: MutableList<Inntektsmelding> = mutableListOf()
    private var tilstand: Sakskomplekstilstand = StartTilstand()

    private val observers: MutableList<SakskompleksObserver> = mutableListOf()

    fun leggTil(søknad: Sykepengesøknad) {
        tilstand.søknadMottatt(søknad)
    }

    fun leggTil(inntektsmelding: Inntektsmelding) {
        tilstand.inntektsmeldingMottatt(inntektsmelding)
    }

    fun leggTil(sykmelding: Sykmelding) {
        tilstand.sykmeldingMottatt(sykmelding)
    }

    fun fom(): LocalDate? = run {
        val syketilfelleStart = sykmeldinger.mapNotNull { sykmelding -> sykmelding.syketilfelleStartDato }.min()
        val tidligsteFOM: LocalDate? =
                sykmeldinger.flatMap { sykmelding -> sykmelding.perioder }.map { periode -> periode.fom }.min()
        val søknadEgenmelding =
                søknader.flatMap { søknad -> søknad.egenmeldinger }.map { egenmelding -> egenmelding.fom }.min()

        return listOfNotNull(syketilfelleStart, tidligsteFOM, søknadEgenmelding).min()
    }

    fun tom(): LocalDate = run {
        val arbeidGjenopptatt = søknader.somIkkeErKorrigerte().maxBy { søknad -> søknad.tom }?.arbeidGjenopptatt
        val sisteTOMSøknad = søknader.maxBy { søknad -> søknad.tom }?.tom
        val sisteTOMSykmelding = sykmeldinger.maxBy { sykmelding -> sykmelding.gjelderTil() }?.gjelderTil()

        return arbeidGjenopptatt
                ?: listOfNotNull(sisteTOMSøknad, sisteTOMSykmelding).max()
                ?: throw RuntimeException("Et sakskompleks må ha en sluttdato!")
    }

    fun hørerSammenMed(sykepengesøknad: Sykepengesøknad) =
            sykmeldinger.any { sykmelding ->
                sykmelding.id == sykepengesøknad.sykmeldingId
            }

    private fun List<Sykepengesøknad>.somIkkeErKorrigerte(): List<Sykepengesøknad> {
        val korrigerteIder = mapNotNull { it.korrigerer }
        return filter { it.id !in korrigerteIder }
    }

    // Gang of four State pattern
    private abstract inner class Sakskomplekstilstand {

        internal fun transition(event: Event, nyTilstand: Sakskomplekstilstand, block: () -> Unit = {}) {
            tilstand.leaving()

            val previousStateName = tilstand.name()
            val previousMemento = memento()

            tilstand = nyTilstand
            block()

            tilstand.entering()

            notifyObservers(tilstand.name(), event, previousStateName, previousMemento)
        }

        fun name() = javaClass.simpleName

        open fun sykmeldingMottatt(sykmelding: Sykmelding) {
            transition(sykmelding, TrengerManuellHåndteringTilstand())
        }

        open fun søknadMottatt(søknad: Sykepengesøknad) {
            transition(søknad, TrengerManuellHåndteringTilstand())
        }

        open fun inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            transition(inntektsmelding, TrengerManuellHåndteringTilstand())
        }

        open fun leaving() {
        }

        open fun entering() {
        }
    }

    private inner class StartTilstand : Sakskomplekstilstand() {
        override fun sykmeldingMottatt(sykmelding: Sykmelding) {
            transition(sykmelding, SykmeldingMottattTilstand()) {
                sykmeldinger.add(sykmelding)
            }
        }
    }

    private inner class SykmeldingMottattTilstand : Sakskomplekstilstand() {
        override fun søknadMottatt(søknad: Sykepengesøknad) {
            transition(søknad, SøknadMottattTilstand()) {
                søknader.add(søknad)
            }
        }

        override fun inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            transition(inntektsmelding, InntektsmeldingMottattTilstand()) {
                inntektsmeldinger.add(inntektsmelding)
            }
        }
    }

    private inner class SøknadMottattTilstand : Sakskomplekstilstand() {
        override fun inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            transition(inntektsmelding, KomplettSakTilstand()) {
                inntektsmeldinger.add(inntektsmelding)
            }
        }
    }

    private inner class InntektsmeldingMottattTilstand : Sakskomplekstilstand() {
        override fun søknadMottatt(søknad: Sykepengesøknad) {
            transition(søknad, KomplettSakTilstand()) {
                søknader.add(søknad)
            }
        }
    }

    private inner class KomplettSakTilstand : Sakskomplekstilstand()

    private inner class TrengerManuellHåndteringTilstand: Sakskomplekstilstand()

    // Gang of four Memento pattern
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun restore(memento: Memento): Sakskompleks {
            val node = objectMapper.readTree(memento.state)

            val sakskompleks = Sakskompleks(
                    id = UUID.fromString(node["id"].textValue()),
                    aktørId = node["aktørId"].textValue()
            )

            sakskompleks.tilstand = when (node["tilstand"].textValue()) {
                "StartTilstand" -> sakskompleks.StartTilstand()
                "SykmeldingMottattTilstand" -> sakskompleks.SykmeldingMottattTilstand()
                "SøknadMottattTilstand" -> sakskompleks.SøknadMottattTilstand()
                "InntektsmeldingMottattTilstand" -> sakskompleks.InntektsmeldingMottattTilstand()
                "KomplettSakTilstand" -> sakskompleks.KomplettSakTilstand()
                "TrengerManuellHåndteringTilstand" -> sakskompleks.TrengerManuellHåndteringTilstand()
                else -> throw RuntimeException("ukjent tilstand")
            }

            node["sykmeldinger"].map { jsonNode ->
                Sykmelding(jsonNode)
            }.let {
                sakskompleks.sykmeldinger.addAll(it)
            }

            node["inntektsmeldinger"].map { jsonNode ->
                Inntektsmelding(jsonNode)
            }.let {
                sakskompleks.inntektsmeldinger.addAll(it)
            }

            node["søknader"].map { jsonNode ->
                Sykepengesøknad(jsonNode)
            }.let {
                sakskompleks.søknader.addAll(it)
            }

            return sakskompleks
        }
    }

    internal fun memento(): Memento {
        val writer = StringWriter()
        val generator = JsonFactory().createGenerator(writer)

        generator.writeStartObject()
        generator.writeStringField("id", id.toString())
        generator.writeStringField("aktørId", aktørId)
        generator.writeStringField("tilstand", tilstand.name())

        generator.writeArrayFieldStart("sykmeldinger")
        sykmeldinger.forEach { sykmelding ->
            objectMapper.writeValue(generator, sykmelding.jsonNode)
        }
        generator.writeEndArray()

        generator.writeArrayFieldStart("inntektsmeldinger")
        inntektsmeldinger.forEach { inntektsmelding ->
            objectMapper.writeValue(generator, inntektsmelding.jsonNode)
        }
        generator.writeEndArray()

        generator.writeArrayFieldStart("søknader")
        søknader.forEach { søknad ->
            objectMapper.writeValue(generator, søknad.jsonNode)
        }
        generator.writeEndArray()

        generator.writeEndObject()

        generator.flush()

        return Memento(state = writer.toString())
    }

    class Memento(internal val state: String) {
        override fun toString() = state
    }

    // Gang of four Observer pattern
    internal fun addObserver(observer: SakskompleksObserver) {
        observers.add(observer)
    }

    private fun notifyObservers(currentState: String, event: Event, previousState: String, previousMemento: Memento) {
        val event = SakskompleksObserver.StateChangeEvent(
            id = id,
            aktørId = aktørId,
            currentState = currentState,
            previousState = previousState,
            eventName = event.name(),
            currentMemento = memento(),
            previousMemento = previousMemento
        )

        observers.forEach { observer ->
            observer.sakskompleksChanged(event)
        }
    }
}


