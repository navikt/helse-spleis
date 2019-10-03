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


    private fun oppdaterTidslinje() {
        val combined = (nyeSøknader.map { it.sykdomstidslinje() } + sendteSøknader.map { it.sykdomstidslinje() })
        sykdomstidslinje = combined
            .reduce { sum, sykdomstidslinje -> sum + sykdomstidslinje }
    }

    fun leggTil(søknad: Sykepengesøknad) {
        if (søknad.status == SØKNAD_SENDT) {
            sendteSøknader.add(søknad)
            tilstand.sendtSøknad(søknad)
        } else {
            nyeSøknader.add(søknad)
            tilstand.nySøknad(søknad)
        }
        oppdaterTidslinje()
    }

    fun leggTil(inntektsmelding: Inntektsmelding) {
        inntektsmeldinger.add(inntektsmelding)
        tilstand.inntektsmeldingMottatt(inntektsmelding)
    }

    fun fom() = sykdomstidslinje.startdato()
    fun tom() = sykdomstidslinje.sluttdato()
    fun sisteSykdag() = sykdomstidslinje.syketilfeller().last().sluttdato()

    fun hørerSammenMed(sykepengesøknad: Sykepengesøknad) =
            nyeSøknader.any { nySøknad ->
                nySøknad.id == sykepengesøknad.id
            }

    private fun List<Sykepengesøknad>.somIkkeErKorrigerte(): List<Sykepengesøknad> {
        val korrigerteIder = mapNotNull { it.korrigerer }
        return filter { it.id !in korrigerteIder }
    }

    enum class TilstandType {
        START,
        NY_SØKNAD_MOTTATT,
        SENDT_SØKNAD_MOTTATT,
        INNTEKTSMELDING_MOTTATT,
        KOMPLETT_SAK,
        TRENGER_MANUELL_HÅNDTERING
    }

    // Gang of four State pattern
    private abstract inner class Sakskomplekstilstand {

        internal fun transition(event: Event, nyTilstand: Sakskomplekstilstand, block: () -> Unit = {}) {
            tilstand.leaving()

            val previousStateName = tilstand.type
            val previousMemento = memento()

            tilstand = nyTilstand
            block()

            tilstand.entering()

            notifyObservers(tilstand.type, event, previousStateName, previousMemento)
        }

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

        abstract val type: TilstandType
    }

    private inner class StartTilstand : Sakskomplekstilstand() {
        override fun nySøknad(søknad: Sykepengesøknad) {
            transition(søknad, NySøknadMottattTilstand()) {
                nyeSøknader.add(søknad)
            }
        }

        override val type = TilstandType.START
    }

    private inner class NySøknadMottattTilstand : Sakskomplekstilstand() {
        override fun sendtSøknad(søknad: Sykepengesøknad) {
            transition(søknad, SendtSøknadMottattTilstand()) {
                oppdaterTidslinje()
            }
        }

        override fun inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            transition(inntektsmelding, InntektsmeldingMottattTilstand())
        }

        override val type = TilstandType.NY_SØKNAD_MOTTATT
    }

    private inner class SendtSøknadMottattTilstand : Sakskomplekstilstand() {
        override fun inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            transition(inntektsmelding, KomplettSakTilstand())
        }

        override val type = TilstandType.SENDT_SØKNAD_MOTTATT
    }

    private inner class InntektsmeldingMottattTilstand : Sakskomplekstilstand() {
        override fun sendtSøknad(søknad: Sykepengesøknad) {
            transition(søknad, KomplettSakTilstand())
        }

        override val type = TilstandType.INNTEKTSMELDING_MOTTATT
    }

    private inner class KomplettSakTilstand : Sakskomplekstilstand() {
        override val type = TilstandType.KOMPLETT_SAK
    }

    private inner class TrengerManuellHåndteringTilstand: Sakskomplekstilstand() {
        override val type = TilstandType.TRENGER_MANUELL_HÅNDTERING
    }

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

            sakskompleks.tilstand = when (TilstandType.valueOf(node["tilstand"].textValue())) {
                TilstandType.START -> sakskompleks.StartTilstand()
                TilstandType.NY_SØKNAD_MOTTATT -> sakskompleks.NySøknadMottattTilstand()
                TilstandType.SENDT_SØKNAD_MOTTATT -> sakskompleks.SendtSøknadMottattTilstand()
                TilstandType.INNTEKTSMELDING_MOTTATT -> sakskompleks.InntektsmeldingMottattTilstand()
                TilstandType.KOMPLETT_SAK -> sakskompleks.KomplettSakTilstand()
                TilstandType.TRENGER_MANUELL_HÅNDTERING -> sakskompleks.TrengerManuellHåndteringTilstand()
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

            return sakskompleks.apply {
                oppdaterTidslinje()
            }
        }
    }

    internal fun memento(): Memento {
        val writer = StringWriter()
        val generator = JsonFactory().createGenerator(writer)

        generator.writeStartObject()
        generator.writeStringField("id", id.toString())
        generator.writeStringField("aktørId", aktørId)
        generator.writeStringField("tilstand", tilstand.type.name)

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

    private fun notifyObservers(currentState: TilstandType, event: Event, previousState: TilstandType, previousMemento: Memento) {
        val event = SakskompleksObserver.StateChangeEvent(
            id = id,
            aktørId = aktørId,
            currentState = currentState,
            previousState = previousState,
            eventType = event.eventType(),
            currentMemento = memento(),
            previousMemento = previousMemento
        )

        observers.forEach { observer ->
            observer.sakskompleksChanged(event)
        }
    }
}


