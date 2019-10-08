package no.nav.helse.person.domain

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Event
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.søknad.domain.Sykepengesøknad
import java.io.StringWriter
import java.util.*

class Sakskompleks internal constructor(
        private val id: UUID,
        private val aktørId: String
) {

    private val nyeSøknader: MutableList<Sykepengesøknad> = mutableListOf()
    private val sendteSøknader: MutableList<Sykepengesøknad> = mutableListOf()
    private val inntektsmeldinger: MutableList<Inntektsmelding> = mutableListOf()
    private var tilstand: Sakskomplekstilstand = StartTilstand

    private val sykdomstidslinje
        get() = nyeSøknader.plus(sendteSøknader)
                .map(Sykepengesøknad::sykdomstidslinje)
                .reduce { sum, sykdomstidslinje ->
                    sum + sykdomstidslinje
                }

    private val observers: MutableList<SakskompleksObserver> = mutableListOf()

    internal fun håndterNySøknad(søknad: Sykepengesøknad): Boolean {
        return passerMed(søknad).also {
            if (it) {
                nyeSøknader.add(søknad)
                tilstand.håndterNySøknad(this, søknad)
            }
        }
    }

    internal fun håndterSendtSøknad(søknad: Sykepengesøknad): Boolean {
        return passerMed(søknad).also {
            if (it) {
                sendteSøknader.add(søknad)
                tilstand.håndterSendtSøknad(this, søknad)
            }
        }
    }

    internal fun håndterInntektsmelding(inntektsmelding: Inntektsmelding) =
            passerMed(inntektsmelding).also {
                if (it) {
                    inntektsmeldinger.add(inntektsmelding)
                    tilstand.håndterInntektsmelding(this, inntektsmelding)
                }
            }

    private fun passerMed(hendelse: Sykdomshendelse): Boolean {
        return true
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

    private fun setTilstand(event: Event, nyTilstand: Sakskomplekstilstand, block: () -> Unit = {}) {
        tilstand.leaving()

        val previousStateName = tilstand.type
        val previousMemento = memento()

        tilstand = nyTilstand
        block()

        tilstand.entering()

        notifyObservers(tilstand.type, event, previousStateName, previousMemento)
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
    private interface Sakskomplekstilstand {

        val type: TilstandType

        fun håndterNySøknad(sakskompleks: Sakskompleks, søknad: Sykepengesøknad) {
            sakskompleks.setTilstand(søknad, TrengerManuellHåndteringTilstand)
        }

        fun håndterSendtSøknad(sakskompleks: Sakskompleks, søknad: Sykepengesøknad) {
            sakskompleks.setTilstand(søknad, TrengerManuellHåndteringTilstand)
        }

        fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmelding: Inntektsmelding) {
            sakskompleks.setTilstand(inntektsmelding, TrengerManuellHåndteringTilstand)
        }

        fun leaving() {
        }

        fun entering() {
        }

    }

    private object StartTilstand : Sakskomplekstilstand {

        override fun håndterNySøknad(sakskompleks: Sakskompleks, søknad: Sykepengesøknad) {
            sakskompleks.setTilstand(søknad, NySøknadMottattTilstand)
        }

        override val type = TilstandType.START

    }

    private object NySøknadMottattTilstand : Sakskomplekstilstand {

        override fun håndterSendtSøknad(sakskompleks: Sakskompleks, søknad: Sykepengesøknad) {
            sakskompleks.setTilstand(søknad, SendtSøknadMottattTilstand)
        }

        override fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmelding: Inntektsmelding) {
            sakskompleks.setTilstand(inntektsmelding, InntektsmeldingMottattTilstand)
        }

        override val type = TilstandType.NY_SØKNAD_MOTTATT

    }

    private object SendtSøknadMottattTilstand : Sakskomplekstilstand {

        override fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmelding: Inntektsmelding) {
            sakskompleks.setTilstand(inntektsmelding, KomplettSakTilstand)
        }

        override val type = TilstandType.SENDT_SØKNAD_MOTTATT

    }

    private object InntektsmeldingMottattTilstand : Sakskomplekstilstand {

        override fun håndterSendtSøknad(sakskompleks: Sakskompleks, søknad: Sykepengesøknad) {
            sakskompleks.setTilstand(søknad, KomplettSakTilstand)
        }

        override val type = TilstandType.INNTEKTSMELDING_MOTTATT

    }

    private object KomplettSakTilstand : Sakskomplekstilstand {
        override val type = TilstandType.KOMPLETT_SAK

    }

    private object TrengerManuellHåndteringTilstand : Sakskomplekstilstand {
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
                TilstandType.START -> StartTilstand
                TilstandType.NY_SØKNAD_MOTTATT -> NySøknadMottattTilstand
                TilstandType.SENDT_SØKNAD_MOTTATT -> SendtSøknadMottattTilstand
                TilstandType.INNTEKTSMELDING_MOTTATT -> InntektsmeldingMottattTilstand
                TilstandType.KOMPLETT_SAK -> KomplettSakTilstand
                TilstandType.TRENGER_MANUELL_HÅNDTERING -> TrengerManuellHåndteringTilstand
            }

            sakskompleks.inntektsmeldinger.addAll(node["inntektsmeldinger"].map { jsonNode ->
                Inntektsmelding(jsonNode)
            })

            sakskompleks.nyeSøknader.addAll(node["nyeSøknader"].map { jsonNode ->
                Sykepengesøknad(jsonNode)
            })

            sakskompleks.sendteSøknader.addAll(node["sendteSøknader"].map { jsonNode ->
                Sykepengesøknad(jsonNode)
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
