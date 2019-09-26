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
    private var tilstand: Sakskomplekstilstand = StartTilstand

    private val observers: MutableList<Observer> = mutableListOf()

    constructor(id: UUID, aktørId: String) {
        this.id = id
        this.aktørId = aktørId
    }

    constructor(json: ByteArray) {
        val node = fromJson(json)

        id = UUID.fromString(node["id"].textValue())
        aktørId = node["aktørId"].textValue()
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    internal fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    private fun tilstand(nyTilstand: Sakskomplekstilstand) {
        val oldState = Observer.State(id, aktørId, lagre())

        tilstand.leaving()
        tilstand = nyTilstand
        tilstand.arriving()

        val newState = Observer.State(id, aktørId, lagre())

        observers.forEach { observer ->
            observer.stateChange(newState, oldState)
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
        generator.writeStringField("tilstand", tilstand::class.java.simpleName)

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

        return Memento(id, aktørId, writer.toString().toByteArray(Charsets.UTF_8))
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
                       val json: ByteArray)

    private object StartTilstand : Sakskomplekstilstand() {
        override fun Sakskompleks.sykmeldingMottatt(sykmelding: Sykmelding) {
            sykmeldinger.add(sykmelding)
            tilstand(SykmeldingMottattTilstand)
        }
    }

    private object SykmeldingMottattTilstand : Sakskomplekstilstand() {
        override fun Sakskompleks.søknadMottatt(søknad: Sykepengesøknad) {
            søknader.add(søknad)
            tilstand(SøknadMottattTilstand)
        }

        override fun Sakskompleks.inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            inntektsmeldinger.add(inntektsmelding)
            tilstand(InntektsmeldingMottattTilstand)
        }
    }

    private object SøknadMottattTilstand : Sakskomplekstilstand() {
        override fun Sakskompleks.inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            inntektsmeldinger.add(inntektsmelding)
            tilstand(KomplettSakTilstand)
        }
    }

    private object InntektsmeldingMottattTilstand : Sakskomplekstilstand() {
        override fun Sakskompleks.søknadMottatt(søknad: Sykepengesøknad) {
            søknader.add(søknad)
            tilstand(KomplettSakTilstand)
        }
    }

    interface Observer {
        data class State(val id: UUID,
                         val aktørId: String,
                         val memento: Memento)

        fun stateChange(newState: State, oldState: State)
    }

    private object KomplettSakTilstand : Sakskomplekstilstand()

    abstract class Sakskomplekstilstand {

        internal open fun Sakskompleks.sykmeldingMottatt(sykmelding: Sykmelding) {
            throw IllegalStateException()
        }

        internal open fun Sakskompleks.søknadMottatt(søknad: Sykepengesøknad) {
            throw IllegalStateException()
        }

        internal open fun Sakskompleks.inntektsmeldingMottatt(inntektsmelding: Inntektsmelding) {
            throw IllegalStateException()
        }

        internal open fun leaving() {}

        internal open fun arriving() {}

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }

    }
}

fun List<Sykepengesøknad>.somIkkeErKorrigerte(): List<Sykepengesøknad> {
    val korrigerteIder = mapNotNull { it.korrigerer }
    return filter { it.id !in korrigerteIder }
}
