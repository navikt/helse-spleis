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

class Sakskompleks {
    private val id: UUID

    private val aktørId: String

    private val sykmeldinger: MutableList<Sykmelding> = mutableListOf()
    private val søknader: MutableList<Sykepengesøknad> = mutableListOf()
    private val inntektsmeldinger: MutableList<Inntektsmelding> = mutableListOf()
    private var tilstand: Sakskomplekstilstand = StartTilstand()

    private val observers: MutableList<Observer> = mutableListOf()

    constructor(id: UUID, aktørId: String) {
        this.id = id
        this.aktørId = aktørId
    }

    constructor(json: ByteArray) {
        val node = fromJson(json)

        id = UUID.fromString(node["id"].textValue())
        aktørId = node["aktørId"].textValue()

        tilstand = when (node["tilstand"].textValue()) {
            StartTilstand().name() -> StartTilstand()
            SykmeldingMottattTilstand().name() -> SykmeldingMottattTilstand()
            SøknadMottattTilstand().name() -> SøknadMottattTilstand()
            InntektsmeldingMottattTilstand().name() -> InntektsmeldingMottattTilstand()
            KomplettSakTilstand().name() -> KomplettSakTilstand()
            TrengerManuellHåndteringTilstand().name() -> TrengerManuellHåndteringTilstand()
            else -> throw RuntimeException("ukjent tilstand")
        }

        node["sykmeldinger"].map { jsonNode ->
            Sykmelding(jsonNode)
        }.let {
            sykmeldinger.addAll(it)
        }

        node["inntektsmeldinger"].map { jsonNode ->
            Inntektsmelding(jsonNode)
        }.let {
            inntektsmeldinger.addAll(it)
        }

        node["søknader"].map { jsonNode ->
            Sykepengesøknad(jsonNode)
        }.let {
            søknader.addAll(it)
        }
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    internal fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    private fun notifyObservers(event: Observer.Event) {
        observers.forEach { observer ->
            observer.stateChange(event)
        }
    }

    fun id() = id
    fun aktørId() = aktørId

    private fun fromJson(json: ByteArray) =
        objectMapper.readTree(json)

    internal fun lagre(): Memento {
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

        return Memento(
                id = id,
                aktørId = aktørId,
                tilstand = tilstand.name(),
                json = writer.toString().toByteArray(Charsets.UTF_8)
        )
    }

    fun leggTil(søknad: Sykepengesøknad) {
        with(tilstand) { søknadMottatt(søknad) }
    }

    fun leggTil(inntektsmelding: Inntektsmelding) {
        with(tilstand) { inntektsmeldingMottatt(inntektsmelding) }
    }

    fun leggTil(sykmelding: Sykmelding) {
        with(tilstand) { sykmeldingMottatt(sykmelding) }
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

    data class Memento(val id: UUID,
                       val aktørId: String,
                       val tilstand: String,
                       val json: ByteArray)

    private inner class StartTilstand : Sakskomplekstilstand() {
        override fun sykmeldingMottatt(sykmelding: Sykmelding) {
            transition(SykmeldingMottattTilstand()) {
                sykmeldinger.add(sykmelding)
            }
        }
    }

    private inner class SykmeldingMottattTilstand : Sakskomplekstilstand() {
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
        override fun inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            transition(KomplettSakTilstand()) {
                inntektsmeldinger.add(inntektsmelding)
            }
        }
    }

    private inner class InntektsmeldingMottattTilstand : Sakskomplekstilstand() {
        override fun søknadMottatt(søknad: Sykepengesøknad) {
            transition(KomplettSakTilstand()) {
                søknader.add(søknad)
            }
        }
    }

    private inner class KomplettSakTilstand : Sakskomplekstilstand()

    private inner class TrengerManuellHåndteringTilstand: Sakskomplekstilstand()

    interface Observer {
        data class Event(val type: Type,
                         val currentState: Memento,
                         val oldState: Memento? = null) {

            sealed class Type {
                object LeavingState: Type()
                object StateChange: Type()
                object EnteringState: Type()
            }
        }

        fun stateChange(event: Event)
    }

    private abstract inner class Sakskomplekstilstand {

        internal fun transition(nyTilstand: Sakskomplekstilstand, block: () -> Unit = {}) {
            tilstand.leaving()

            val oldState = lagre()

            tilstand = nyTilstand
            block()

            notifyObservers(Observer.Event(
                    type = Observer.Event.Type.StateChange,
                    currentState = lagre(),
                    oldState = oldState
            ))

            tilstand.entering()
        }

        open fun name() =
                this::javaClass.get().simpleName

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
            notifyObservers(Observer.Event(Observer.Event.Type.LeavingState, lagre()))
        }

        open fun entering() {
            notifyObservers(Observer.Event(Observer.Event.Type.EnteringState, lagre()))
        }
    }
}

fun List<Sykepengesøknad>.somIkkeErKorrigerte(): List<Sykepengesøknad> {
    val korrigerteIder = mapNotNull { it.korrigerer }
    return filter { it.id !in korrigerteIder }
}
