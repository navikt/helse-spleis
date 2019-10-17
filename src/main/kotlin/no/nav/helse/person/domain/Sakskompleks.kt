package no.nav.helse.person.domain

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelse.*
import no.nav.helse.person.domain.SakskompleksObserver.*
import no.nav.helse.person.domain.SakskompleksObserver.NeedType.TRENGER_PERSONOPPLYSNINGER
import no.nav.helse.person.domain.SakskompleksObserver.NeedType.TRENGER_SYKEPENGEHISTORIKK
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.io.StringWriter
import java.util.*

class Sakskompleks internal constructor(
        private val id: UUID,
        private val aktørId: String,
        private val organisasjonsnummer: String
) {

    private var tilstand: Sakskomplekstilstand = StartTilstand

    private var sykdomstidslinje: Sykdomstidslinje? = null

    private val observers: MutableList<SakskompleksObserver> = mutableListOf()

    internal fun håndterNySøknad(søknad: NySykepengesøknad): Boolean {
        return overlapperMed(søknad).also {
            if (it) {
                tilstand.håndterNySøknad(this, søknad)
            }
        }
    }

    internal fun håndterSendtSøknad(søknad: SendtSykepengesøknad): Boolean {
        return overlapperMed(søknad).also {
            if (it) {
                tilstand.håndterSendtSøknad(this, søknad)
            }
        }
    }

    internal fun håndterInntektsmelding(inntektsmelding: Inntektsmelding) =
    // TODO: blokkert fordi inntektsmelding ikke har tidslinje enda
            // passerMed(inntektsmelding).also {
            true.also {
                if (it) {
                    tilstand.håndterInntektsmelding(this, inntektsmelding)
                }
            }

    internal fun håndterSykepengehistorikk(sykepengehistorikk: Sykepengehistorikk) {
        if (id == sykepengehistorikk.sakskompleksId()) tilstand.håndterSykepengehistorikk(this, sykepengehistorikk)
    }

    private fun overlapperMed(hendelse: Sykdomshendelse) =
            this.sykdomstidslinje?.overlapperMed(hendelse.sykdomstidslinje()) ?: true


    private fun setTilstand(event: Sykdomshendelse, nyTilstand: Sakskomplekstilstand, block: () -> Unit = {}) {
        tilstand.leaving()

        val previousStateName = tilstand.type
        val previousMemento = memento()

        tilstand = nyTilstand
        block()

        tilstand.entering(this)

        notifyStateObservers(tilstand.type, event, previousStateName, previousMemento)
    }

    enum class TilstandType {
        START,
        NY_SØKNAD_MOTTATT,
        SENDT_SØKNAD_MOTTATT,
        INNTEKTSMELDING_MOTTATT,
        KOMPLETT_SAK,
        SYKEPENGEHISTORIKK_MOTTATT,
        TRENGER_MANUELL_HÅNDTERING
    }

    // Gang of four State pattern
    private interface Sakskomplekstilstand {

        val type: TilstandType

        // Default implementasjoner av transisjonene
        fun håndterNySøknad(sakskompleks: Sakskompleks, søknad: NySykepengesøknad) {
            sakskompleks.setTilstand(søknad, TrengerManuellHåndteringTilstand)
        }

        fun håndterSendtSøknad(sakskompleks: Sakskompleks, søknad: SendtSykepengesøknad) {
            sakskompleks.setTilstand(søknad, TrengerManuellHåndteringTilstand)
        }

        fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmelding: Inntektsmelding) {
            sakskompleks.setTilstand(inntektsmelding, TrengerManuellHåndteringTilstand)
        }

        fun håndterSykepengehistorikk(sakskompleks: Sakskompleks, sykepengehistorikk: Sykepengehistorikk) {
            sakskompleks.setTilstand(sykepengehistorikk, TrengerManuellHåndteringTilstand)
        }

        fun leaving() {
        }

        fun entering(sakskompleks: Sakskompleks) {
        }

    }

    private fun slåSammenSykdomstidslinje(hendelse: Sykdomshendelse) {
        this.sykdomstidslinje = this.sykdomstidslinje?.plus(hendelse.sykdomstidslinje())
                ?: hendelse.sykdomstidslinje()
    }

    private object StartTilstand : Sakskomplekstilstand {

        override fun håndterNySøknad(sakskompleks: Sakskompleks, søknad: NySykepengesøknad) {
            sakskompleks.setTilstand(søknad, NySøknadMottattTilstand) {
                sakskompleks.sykdomstidslinje = søknad.sykdomstidslinje()
            }
        }

        override val type = Sakskompleks.TilstandType.START

    }

    private object NySøknadMottattTilstand : Sakskomplekstilstand {

        override fun håndterSendtSøknad(sakskompleks: Sakskompleks, søknad: SendtSykepengesøknad) {
            sakskompleks.setTilstand(søknad, SendtSøknadMottattTilstand) {
                sakskompleks.slåSammenSykdomstidslinje(søknad)
            }
        }

        override fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmelding: Inntektsmelding) {
            sakskompleks.setTilstand(inntektsmelding, InntektsmeldingMottattTilstand) {
                // TODO: blokkert fordi inntektsmelding ikke har tidslinje enda
                // sakskompleks.slåSammenSykdomstidslinje(inntektsmelding)
            }
        }

        override val type = TilstandType.NY_SØKNAD_MOTTATT

    }

    private object SendtSøknadMottattTilstand : Sakskomplekstilstand {

        override fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmelding: Inntektsmelding) {
            sakskompleks.setTilstand(inntektsmelding, KomplettSakTilstand) {
                // TODO: blokkert fordi inntektsmelding ikke har tidslinje enda
                // sakskompleks.slåSammenSykdomstidslinje(inntektsmelding)
            }
        }

        override val type = TilstandType.SENDT_SØKNAD_MOTTATT

    }

    private object InntektsmeldingMottattTilstand : Sakskomplekstilstand {

        override fun håndterSendtSøknad(sakskompleks: Sakskompleks, søknad: SendtSykepengesøknad) {
            sakskompleks.setTilstand(søknad, KomplettSakTilstand) {
                sakskompleks.slåSammenSykdomstidslinje(søknad)
            }
        }

        override val type = TilstandType.INNTEKTSMELDING_MOTTATT

    }

    private object KomplettSakTilstand : Sakskomplekstilstand {
        override val type = TilstandType.KOMPLETT_SAK

        override fun entering(sakskompleks: Sakskompleks) {
            sakskompleks.notifyNeedObservers(TRENGER_SYKEPENGEHISTORIKK)
            sakskompleks.notifyNeedObservers(TRENGER_PERSONOPPLYSNINGER)
        }

        override fun håndterSykepengehistorikk(sakskompleks: Sakskompleks, sykepengehistorikk: Sykepengehistorikk) {
            if (sykepengehistorikk.påvirkerSakensMaksdato(sakskompleks.sykdomstidslinje!!)) sakskompleks.setTilstand(sykepengehistorikk, TrengerManuellHåndteringTilstand)
            else sakskompleks.setTilstand(sykepengehistorikk, SykepengehistorikkMottattTilstand)
        }

    }

    private object TrengerManuellHåndteringTilstand : Sakskomplekstilstand {
        override val type = TilstandType.TRENGER_MANUELL_HÅNDTERING

    }

    private object SykepengehistorikkMottattTilstand : Sakskomplekstilstand {

        override val type = TilstandType.SYKEPENGEHISTORIKK_MOTTATT
    }

    // Gang of four Memento pattern
    companion object {

        internal fun fromJson(sakskompleksJson: SakskompleksJson): Sakskompleks {
            return Sakskompleks(
                    id = sakskompleksJson.id,
                    aktørId = sakskompleksJson.aktørId,
                    organisasjonsnummer = sakskompleksJson.organisasjonsnummer
            ).apply {
                tilstand = tilstandFraEnum(sakskompleksJson.tilstandType)
                sykdomstidslinje = sakskompleksJson.sykdomstidslinje?.let {
                    if (!it.isNull) {
                        Sykdomstidslinje.fromJson(objectMapper.writeValueAsString(it))
                    } else {
                        null
                    }
                }
            }
        }

        private fun tilstandFraEnum(tilstand: TilstandType) = when (tilstand) {
            TilstandType.START -> StartTilstand
            TilstandType.NY_SØKNAD_MOTTATT -> NySøknadMottattTilstand
            TilstandType.SENDT_SØKNAD_MOTTATT -> SendtSøknadMottattTilstand
            TilstandType.INNTEKTSMELDING_MOTTATT -> InntektsmeldingMottattTilstand
            TilstandType.KOMPLETT_SAK -> KomplettSakTilstand
            TilstandType.SYKEPENGEHISTORIKK_MOTTATT -> SykepengehistorikkMottattTilstand
            TilstandType.TRENGER_MANUELL_HÅNDTERING -> TrengerManuellHåndteringTilstand
        }

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        internal fun restore(memento: Memento): Sakskompleks {
            val node = objectMapper.readTree(memento.state)

            val sakskompleks = Sakskompleks(
                    id = UUID.fromString(node["id"].textValue()),
                    aktørId = node["aktørId"].textValue(),
                    organisasjonsnummer = node["organisasjonsnummer"].textValue()
            )

            sakskompleks.tilstand = tilstandFraEnum(enumValueOf(node["tilstand"].textValue()))

            node["sykdomstidslinje"]?.let {
                sakskompleks.sykdomstidslinje = Sykdomstidslinje.fromJson(it.toString())
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
        generator.writeStringField("organisasjonsnummer", organisasjonsnummer)
        generator.writeStringField("tilstand", tilstand.type.name)

        sykdomstidslinje?.also {
            generator.writeFieldName("sykdomstidslinje")
            generator.writeRaw(":")
            generator.writeRaw(it.toJson())
        }

        generator.writeEndObject()

        generator.flush()

        return Memento(state = writer.toString())
    }

    internal fun jsonRepresentation(): SakskompleksJson {
        return SakskompleksJson(
                id = id,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                tilstandType = tilstand.type,
                sykdomstidslinje = sykdomstidslinje?.toJson()?.let {
                    objectMapper.readTree(it)
                }
        )
    }

    class Memento(internal val state: String) {
        override fun toString() = state

    }

    // Gang of four Observer pattern
    internal fun addSakskompleksObserver(observer: SakskompleksObserver) {
        observers.add(observer)
    }

    private fun notifyStateObservers(
            currentState: TilstandType,
            tidslinjeEvent: Sykdomshendelse,
            previousState: TilstandType,
            previousMemento: Memento
    ) {
        val event = StateChangeEvent(
                id = id,
                aktørId = aktørId,
                currentState = currentState,
                previousState = previousState,
                sykdomshendelse = tidslinjeEvent,
                currentMemento = memento(),
                previousMemento = previousMemento
        )

        observers.forEach { observer ->
            observer.sakskompleksEndret(event)
        }
    }

    private fun notifyNeedObservers(needType: NeedType) {
        val needEvent = NeedEvent(
                sakskompleksId = id,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                type = needType)

        observers.forEach { observer ->
            observer.sakskompleksHarBehov(needEvent)
        }
    }

    internal data class SakskompleksJson(
            val id: UUID,
            val aktørId: String,
            val organisasjonsnummer: String,
            val tilstandType: TilstandType,
            val sykdomstidslinje: JsonNode?
    )
}
