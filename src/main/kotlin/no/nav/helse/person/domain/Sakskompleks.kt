package no.nav.helse.person.domain

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.hendelse.*
import no.nav.helse.person.domain.Sakskompleks.TilstandType.*
import no.nav.helse.person.domain.SakskompleksObserver.StateChangeEvent
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

    internal fun håndterNySøknad(nySøknadHendelse: NySøknadHendelse): Boolean {
        return overlapperMed(nySøknadHendelse).also {
            if (it) {
                tilstand.håndterNySøknad(this, nySøknadHendelse)
            }
        }
    }

    internal fun håndterSendtSøknad(sendtSøknadHendelse: SendtSøknadHendelse): Boolean {
        return overlapperMed(sendtSøknadHendelse).also {
            if (it) {
                tilstand.håndterSendtSøknad(this, sendtSøknadHendelse)
            }
        }
    }

    internal fun håndterInntektsmelding(inntektsmeldingHendelse: InntektsmeldingHendelse) =
    // TODO: blokkert fordi inntektsmelding ikke har tidslinje enda
            // passerMed(inntektsmelding).also {
            true.also {
                if (it) {
                    tilstand.håndterInntektsmelding(this, inntektsmeldingHendelse)
                }
            }

    internal fun håndterSykepengehistorikk(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        if (id.toString() == sykepengehistorikkHendelse.sakskompleksId()) tilstand.håndterSykepengehistorikk(this, sykepengehistorikkHendelse)
    }

    private fun overlapperMed(hendelse: SykdomstidslinjeHendelse) =
            this.sykdomstidslinje?.overlapperMed(hendelse.sykdomstidslinje()) ?: true


    private fun setTilstand(event: SykdomstidslinjeHendelse, nyTilstand: Sakskomplekstilstand, block: () -> Unit = {}) {
        tilstand.leaving()

        val previousStateName = tilstand.type
        val previousMemento = memento()

        tilstand = nyTilstand
        block()

        tilstand.entering(this)

        emitSakskompleksEndret(tilstand.type, event, previousStateName, previousMemento)
    }

    enum class TilstandType {
        START,
        NY_SØKNAD_MOTTATT,
        SENDT_SØKNAD_MOTTATT,
        INNTEKTSMELDING_MOTTATT,
        KOMPLETT_SAK,
        SYKEPENGEHISTORIKK_MOTTATT,
        SKAL_TIL_INFOTRYGD
    }

    // Gang of four State pattern
    private interface Sakskomplekstilstand {

        val type: TilstandType

        // Default implementasjoner av transisjonene
        fun håndterNySøknad(sakskompleks: Sakskompleks, nySøknadHendelse: NySøknadHendelse) {
            sakskompleks.setTilstand(nySøknadHendelse, MåBehandlesIInfotrygdTilstand)
        }

        fun håndterSendtSøknad(sakskompleks: Sakskompleks, sendtSøknadHendelse: SendtSøknadHendelse) {
            sakskompleks.setTilstand(sendtSøknadHendelse, MåBehandlesIInfotrygdTilstand)
        }

        fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmeldingHendelse: InntektsmeldingHendelse) {
            sakskompleks.setTilstand(inntektsmeldingHendelse, MåBehandlesIInfotrygdTilstand)
        }

        fun håndterSykepengehistorikk(sakskompleks: Sakskompleks, sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
            sakskompleks.setTilstand(sykepengehistorikkHendelse, MåBehandlesIInfotrygdTilstand)
        }

        fun leaving() {
        }

        fun entering(sakskompleks: Sakskompleks) {
        }

    }

    private fun slåSammenSykdomstidslinje(hendelse: SykdomstidslinjeHendelse, tilstand: Sakskomplekstilstand) {
        val tidslinje = this.sykdomstidslinje?.plus(hendelse.sykdomstidslinje())
                ?: hendelse.sykdomstidslinje()

        if (!tidslinje.erUtenforOmfang()) {
            setTilstand(hendelse, tilstand) {
                sykdomstidslinje = tidslinje
            }
        } else {
            setTilstand(hendelse, MåBehandlesIInfotrygdTilstand)
        }
    }

    private object StartTilstand : Sakskomplekstilstand {

        override fun håndterNySøknad(sakskompleks: Sakskompleks, nySøknadHendelse: NySøknadHendelse) {
            sakskompleks.slåSammenSykdomstidslinje(nySøknadHendelse, NySøknadMottattTilstand)
        }

        override val type = START

    }

    private object NySøknadMottattTilstand : Sakskomplekstilstand {

        override fun håndterSendtSøknad(sakskompleks: Sakskompleks, sendtSøknadHendelse: SendtSøknadHendelse) {
            sakskompleks.slåSammenSykdomstidslinje(sendtSøknadHendelse, SendtSøknadMottattTilstand)
        }

        override fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmeldingHendelse: InntektsmeldingHendelse) {
            sakskompleks.setTilstand(inntektsmeldingHendelse, InntektsmeldingMottattTilstand) {
                // TODO: blokkert fordi inntektsmelding ikke har tidslinje enda
                // sakskompleks.slåSammenSykdomstidslinje(inntektsmelding)
            }
        }

        override val type = NY_SØKNAD_MOTTATT

    }

    private object SendtSøknadMottattTilstand : Sakskomplekstilstand {

        override fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmeldingHendelse: InntektsmeldingHendelse) {
            sakskompleks.setTilstand(inntektsmeldingHendelse, KomplettSakTilstand) {
                // TODO: blokkert fordi inntektsmelding ikke har tidslinje enda
                // sakskompleks.slåSammenSykdomstidslinje(inntektsmelding)
            }
        }

        override val type = SENDT_SØKNAD_MOTTATT

    }

    private object InntektsmeldingMottattTilstand : Sakskomplekstilstand {

        override fun håndterSendtSøknad(sakskompleks: Sakskompleks, sendtSøknadHendelse: SendtSøknadHendelse) {
            sakskompleks.slåSammenSykdomstidslinje(sendtSøknadHendelse, KomplettSakTilstand)
        }

        override val type = INNTEKTSMELDING_MOTTATT

    }

    private object KomplettSakTilstand : Sakskomplekstilstand {
        override val type = KOMPLETT_SAK

        override fun entering(sakskompleks: Sakskompleks) {
            sakskompleks.emitTrengerLøsning(BehovsTyper.Sykepengehistorikk)
            sakskompleks.emitTrengerLøsning(BehovsTyper.Personopplysninger)
        }

        override fun håndterSykepengehistorikk(sakskompleks: Sakskompleks, sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
            if (sykepengehistorikkHendelse.påvirkerSakensMaksdato(sakskompleks.sykdomstidslinje!!)) sakskompleks.setTilstand(sykepengehistorikkHendelse, MåBehandlesIInfotrygdTilstand)
            else sakskompleks.setTilstand(sykepengehistorikkHendelse, SykepengehistorikkMottattTilstand)
        }

    }

    private object MåBehandlesIInfotrygdTilstand : Sakskomplekstilstand {
        override val type = SKAL_TIL_INFOTRYGD
    }

    private object SykepengehistorikkMottattTilstand : Sakskomplekstilstand {

        override val type = SYKEPENGEHISTORIKK_MOTTATT
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
            START -> StartTilstand
            NY_SØKNAD_MOTTATT -> NySøknadMottattTilstand
            SENDT_SØKNAD_MOTTATT -> SendtSøknadMottattTilstand
            INNTEKTSMELDING_MOTTATT -> InntektsmeldingMottattTilstand
            KOMPLETT_SAK -> KomplettSakTilstand
            SYKEPENGEHISTORIKK_MOTTATT -> SykepengehistorikkMottattTilstand
            SKAL_TIL_INFOTRYGD -> MåBehandlesIInfotrygdTilstand
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

    private fun emitSakskompleksEndret(
            currentState: TilstandType,
            tidslinjeEvent: SykdomstidslinjeHendelse,
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

    private fun emitTrengerLøsning(type: BehovsTyper) {
        val behov = Behov.nyttBehov(type, mapOf(
                "sakskompleksId" to id,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer))

        observers.forEach { observer ->
            observer.sakskompleksTrengerLøsning(behov)
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
