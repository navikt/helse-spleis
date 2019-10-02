package no.nav.helse.sakskompleks.domain

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Event
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.søknad.domain.Sykepengesøknad
import no.nav.helse.søknad.domain.SØKNAD_SENDT
import java.io.StringWriter
import java.util.*

class Sakskompleks internal constructor(private val id: UUID,
                                        private val aktørId: String) {

    private val nyeSøknader: MutableList<Sykepengesøknad> = mutableListOf()
    private val sendteSøknader: MutableList<Sykepengesøknad> = mutableListOf()
    private val inntektsmeldinger: MutableList<Inntektsmelding> = mutableListOf()
    private var tilstand: Sakskomplekstilstand = StartTilstand()
    private lateinit var sykdomstidslinje: Sykdomstidslinje

    private val observers: MutableList<SakskompleksObserver> = mutableListOf()

    fun leggTil(søknad: Sykepengesøknad) {
        if (søknad.status == SØKNAD_SENDT) {
            tilstand.sendtSøknad(søknad)
        } else {
            tilstand.nySøknad(søknad)
        }
    }

    fun leggTil(inntektsmelding: Inntektsmelding) {
        tilstand.inntektsmeldingMottatt(inntektsmelding)
    }

    fun fom() = sykdomstidslinje.startdato()
    fun tom() = sykdomstidslinje.sluttdato()

    fun hørerSammenMed(sykepengesøknad: Sykepengesøknad) =
            nyeSøknader.any { nySøknad ->
                nySøknad.id == sykepengesøknad.id
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

        open fun nySøknad(søknad: Sykepengesøknad) {
            transition(søknad, TrengerManuellHåndteringTilstand())
        }

        open fun sendtSøknad(søknad: Sykepengesøknad) {
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
        override fun nySøknad(søknad: Sykepengesøknad) {
            transition(søknad, NySøknadMottattTilstand()) {
                nyeSøknader.add(søknad)
            }
        }
    }

    private inner class NySøknadMottattTilstand : Sakskomplekstilstand() {
        override fun sendtSøknad(søknad: Sykepengesøknad) {
            transition(søknad, SendtSøknadMottattTilstand()) {
                sendteSøknader.add(søknad)
            }
        }

        override fun inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            transition(inntektsmelding, InntektsmeldingMottattTilstand()) {
                inntektsmeldinger.add(inntektsmelding)
            }
        }
    }

    private inner class SendtSøknadMottattTilstand : Sakskomplekstilstand() {
        override fun inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            transition(inntektsmelding, KomplettSakTilstand()) {
                inntektsmeldinger.add(inntektsmelding)
            }
        }
    }

    private inner class InntektsmeldingMottattTilstand : Sakskomplekstilstand() {
        override fun sendtSøknad(søknad: Sykepengesøknad) {
            transition(søknad, KomplettSakTilstand()) {
                sendteSøknader.add(søknad)
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
                "NySøknadMottattTilstand" -> sakskompleks.NySøknadMottattTilstand()
                "SendtSøknadMottattTilstand" -> sakskompleks.SendtSøknadMottattTilstand()
                "InntektsmeldingMottattTilstand" -> sakskompleks.InntektsmeldingMottattTilstand()
                "KomplettSakTilstand" -> sakskompleks.KomplettSakTilstand()
                "TrengerManuellHåndteringTilstand" -> sakskompleks.TrengerManuellHåndteringTilstand()
                else -> throw RuntimeException("ukjent tilstand")
            }

            sakskompleks.inntektsmeldinger.addAll(node["inntektsmeldinger"].map { jsonNode ->
                Inntektsmelding(jsonNode)
            })

            sakskompleks.nyeSøknader.addAll(node["nyeSøknader"].map { jsonNode ->
                Sykepengesøknad(jsonNode)
            })

            sakskompleks.sendteSøknader.addAll(node["sendteSøknader"].map {
                jsonNode -> Sykepengesøknad(jsonNode)
            })

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

        generator.writeArrayFieldStart("nyeSøknader")
        nyeSøknader.forEach { søknad ->
            objectMapper.writeValue(generator, søknad.jsonNode)
        }
        generator.writeEndArray()

        generator.writeArrayFieldStart("inntektsmeldinger")
        inntektsmeldinger.forEach { inntektsmelding ->
            objectMapper.writeValue(generator, inntektsmelding.jsonNode)
        }
        generator.writeEndArray()

        generator.writeArrayFieldStart("sendteSøknader")
        sendteSøknader.forEach { søknad ->
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


