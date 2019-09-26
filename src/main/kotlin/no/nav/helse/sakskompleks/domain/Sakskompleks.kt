package no.nav.helse.sakskompleks.domain

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.sykmelding.domain.gjelderTil
import no.nav.helse.søknad.domain.Sykepengesøknad
import java.io.StringWriter
import java.time.LocalDate
import java.util.*

class Sakskompleks(private val id: UUID,
                   private val aktørId: String) {

    private val sykmeldinger: MutableList<Sykmelding> = mutableListOf()
    private val søknader: MutableList<Sykepengesøknad> = mutableListOf()
    private val inntektsmeldinger: MutableList<Inntektsmelding> = mutableListOf()
    private var tilstand: Sakskomplekstilstand = StartTilstand()

    private val observers: MutableList<Observer> = mutableListOf()

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
                sakskompleks.StartTilstand().name() -> sakskompleks.StartTilstand()
                sakskompleks.SykmeldingMottattTilstand().name() -> sakskompleks.SykmeldingMottattTilstand()
                sakskompleks.SøknadMottattTilstand().name() -> sakskompleks.SøknadMottattTilstand()
                sakskompleks.InntektsmeldingMottattTilstand().name() -> sakskompleks.InntektsmeldingMottattTilstand()
                sakskompleks.KomplettSakTilstand().name() -> sakskompleks.KomplettSakTilstand()
                sakskompleks.TrengerManuellHåndteringTilstand().name() -> sakskompleks.TrengerManuellHåndteringTilstand()
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

    internal fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    private fun notifyObservers(eventType: Observer.Event.Type, oldEventType: Observer.Event.Type, previousState: Memento) {
        val event = Observer.Event(
                type = eventType,
                id = id,
                aktørId = aktørId,
                currentState = state(),
                previousType = oldEventType,
                previousState = previousState
        )

        observers.forEach { observer ->
            observer.stateChange(event)
        }
    }

    internal fun state(): Memento {
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

        return Memento(state = writer.toString().toByteArray(Charsets.UTF_8))
    }

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

    fun har(sykmelding: Sykmelding) =
        sykmeldinger.any { enSykmelding ->
            sykmelding == enSykmelding
        }

    fun har(sykepengesøknad: Sykepengesøknad) =
        søknader.any { enSøknad ->
            sykepengesøknad == enSøknad
        }

    fun har(inntektsmelding: Inntektsmelding) =
        inntektsmeldinger.any { enInntektsmelding ->
            inntektsmelding == enInntektsmelding
        }

    class Memento(internal val state: ByteArray) {
        override fun toString() = String(state, Charsets.UTF_8)
    }

    private inner class StartTilstand : Sakskomplekstilstand() {
        override fun eventType() =
                Observer.Event.Type.StartTilstand

        override fun sykmeldingMottatt(sykmelding: Sykmelding) {
            transition(SykmeldingMottattTilstand()) {
                sykmeldinger.add(sykmelding)
            }
        }
    }

    private inner class SykmeldingMottattTilstand : Sakskomplekstilstand() {
        override fun eventType() =
                Observer.Event.Type.SykmeldingMottatt

        override fun søknadMottatt(søknad: Sykepengesøknad) {
            transition(SøknadMottattTilstand()) {
                søknader.add(søknad)
            }
        }

        override fun inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            transition(InntektsmeldingMottattTilstand()) {
                inntektsmeldinger.add(inntektsmelding)
            }
        }
    }

    private inner class SøknadMottattTilstand : Sakskomplekstilstand() {
        override fun eventType() =
                Observer.Event.Type.SøknadMottatt

        override fun inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            transition(KomplettSakTilstand()) {
                inntektsmeldinger.add(inntektsmelding)
            }
        }
    }

    private inner class InntektsmeldingMottattTilstand : Sakskomplekstilstand() {
        override fun eventType() =
                Observer.Event.Type.InntektsmeldingMottatt

        override fun søknadMottatt(søknad: Sykepengesøknad) {
            transition(KomplettSakTilstand()) {
                søknader.add(søknad)
            }
        }
    }

    private inner class KomplettSakTilstand : Sakskomplekstilstand() {
        override fun eventType() =
                Observer.Event.Type.KomplettSak
    }

    private inner class TrengerManuellHåndteringTilstand: Sakskomplekstilstand() {
        override fun eventType() =
                Observer.Event.Type.TrengerManuellHåndtering
    }

    interface Observer {
        data class Event(val type: Type,
                         val id: UUID,
                         val aktørId: String,
                         val currentState: Memento,
                         val previousType: Type,
                         val previousState: Memento) {

            sealed class Type {
                object StartTilstand: Type()
                object TrengerManuellHåndtering: Type()
                object KomplettSak: Type()
                object SykmeldingMottatt: Type()
                object SøknadMottatt: Type()
                object InntektsmeldingMottatt: Type()
            }
        }

        fun stateChange(event: Event)
    }

    private abstract inner class Sakskomplekstilstand {

        internal fun transition(nyTilstand: Sakskomplekstilstand, block: () -> Unit = {}) {
            tilstand.leaving()

            val oldType = tilstand.eventType()
            val oldState = state()

            tilstand = nyTilstand
            block()

            notifyObservers(tilstand.eventType(), oldType, oldState)

            tilstand.entering()
        }

        open fun name() =
                this::javaClass.get().simpleName

        abstract fun eventType(): Observer.Event.Type

        open fun sykmeldingMottatt(sykmelding: Sykmelding) {
            transition(TrengerManuellHåndteringTilstand())
        }

        open fun søknadMottatt(søknad: Sykepengesøknad) {
            transition(TrengerManuellHåndteringTilstand())
        }

        open fun inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            transition(TrengerManuellHåndteringTilstand())
        }

        open fun leaving() {
        }

        open fun entering() {
        }
    }
}

fun List<Sykepengesøknad>.somIkkeErKorrigerte(): List<Sykepengesøknad> {
    val korrigerteIder = mapNotNull { it.korrigerer }
    return filter { it.id !in korrigerteIder }
}
