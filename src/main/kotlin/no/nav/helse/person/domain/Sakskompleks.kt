package no.nav.helse.person.domain

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.SykdomshendelseDeserializer
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.person.domain.Sakskompleks.TilstandType.*
import no.nav.helse.person.domain.SakskompleksObserver.StateChangeEvent
import no.nav.helse.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import no.nav.helse.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.søknad.NySøknadHendelse
import no.nav.helse.søknad.SendtSøknadHendelse
import java.io.StringWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

class Sakskompleks internal constructor(
        private val id: UUID,
        private val aktørId: String,
        private val organisasjonsnummer: String
) {
    private val `6G` = (6 * 99858).toBigDecimal()

    private var tilstand: Sakskomplekstilstand = StartTilstand

    private var sykdomstidslinje: Sykdomstidslinje? = null

    private var maksdato: LocalDate? = null

    private var utbetalingslinjer: List<Utbetalingslinje>? = null

    private var godkjentAv: String? = null

    private val observers: MutableList<SakskompleksObserver> = mutableListOf()

    private inline fun <reified T> Set<*>.førsteAvType(): T {
        return first { it is T } as T
    }

    private fun sykepengegrunnlag() = (sykdomstidslinje?.hendelser()?.førsteAvType<InntektsmeldingHendelse>()?.beregnetInntekt() as BigDecimal).times(12.toBigDecimal())

    private fun beregningsgrunnlag() = sykepengegrunnlag().min(`6G`)

    internal fun dagsats() = beregningsgrunnlag().divide(260.toBigDecimal(), RoundingMode.HALF_UP)

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

    internal fun håndterInntektsmelding(inntektsmeldingHendelse: InntektsmeldingHendelse): Boolean {
        return overlapperMed(inntektsmeldingHendelse).also {
            if (it) {
                tilstand.håndterInntektsmelding(this, inntektsmeldingHendelse)
            }
        }
    }

    internal fun håndterSykepengehistorikk(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        if (id.toString() == sykepengehistorikkHendelse.sakskompleksId()) tilstand.håndterSykepengehistorikk(this, sykepengehistorikkHendelse)
    }

    internal fun håndterManuellSaksbehandling(manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
        if (id.toString() == manuellSaksbehandlingHendelse.sakskompleksId()) tilstand.håndterManuellSaksbehandling(this, manuellSaksbehandlingHendelse)
    }

    internal fun invaliderSak(hendelse: ArbeidstakerHendelse) {
        setTilstand(hendelse, TilInfotrygdTilstand)
    }

    private fun overlapperMed(hendelse: SykdomstidslinjeHendelse) =
            this.sykdomstidslinje?.overlapperMed(hendelse.sykdomstidslinje()) ?: true

    private fun setTilstand(event: ArbeidstakerHendelse, nyTilstand: Sakskomplekstilstand, block: () -> Unit = {}) {
        tilstand.leaving()

        val previousStateName = tilstand.type
        val previousMemento = memento()

        tilstand = nyTilstand
        block()

        tilstand.entering(this)

        emitSakskompleksEndret(tilstand.type, event, previousStateName, previousMemento)
    }

    private fun <HENDELSE> håndterSykdomstidslinjeHendelse(
            hendelse: HENDELSE,
            nesteTilstand: Sakskomplekstilstand
    ) where HENDELSE: SykdomstidslinjeHendelse, HENDELSE: ArbeidstakerHendelse {
        val tidslinje = this.sykdomstidslinje?.plus(hendelse.sykdomstidslinje()) ?: hendelse.sykdomstidslinje()

        if (tidslinje.erUtenforOmfang()) {
            setTilstand(hendelse, TilInfotrygdTilstand)
        } else {
            setTilstand(hendelse, nesteTilstand) {
                sykdomstidslinje = tidslinje
            }
        }
    }

    enum class TilstandType {
        START,
        NY_SØKNAD_MOTTATT,
        SENDT_SØKNAD_MOTTATT,
        INNTEKTSMELDING_MOTTATT,
        KOMPLETT_SYKDOMSTIDSLINJE,
        TIL_GODKJENNING,
        TIL_UTBETALING,
        TIL_INFOTRYGD
    }

    // Gang of four State pattern
    private interface Sakskomplekstilstand {

        val type: TilstandType

        // Default implementasjoner av transisjonene
        fun håndterNySøknad(sakskompleks: Sakskompleks, nySøknadHendelse: NySøknadHendelse) {
            sakskompleks.setTilstand(nySøknadHendelse, TilInfotrygdTilstand)
        }

        fun håndterSendtSøknad(sakskompleks: Sakskompleks, sendtSøknadHendelse: SendtSøknadHendelse) {
            sakskompleks.setTilstand(sendtSøknadHendelse, TilInfotrygdTilstand)
        }

        fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmeldingHendelse: InntektsmeldingHendelse) {
            sakskompleks.setTilstand(inntektsmeldingHendelse, TilInfotrygdTilstand)
        }

        fun håndterSykepengehistorikk(sakskompleks: Sakskompleks, sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        }

        fun håndterManuellSaksbehandling(sakskompleks: Sakskompleks, manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
        }

        fun leaving() {
        }

        fun entering(sakskompleks: Sakskompleks) {
        }

    }

    private object StartTilstand : Sakskomplekstilstand {

        override fun håndterNySøknad(sakskompleks: Sakskompleks, nySøknadHendelse: NySøknadHendelse) {
            sakskompleks.håndterSykdomstidslinjeHendelse(nySøknadHendelse, NySøknadMottattTilstand)
        }

        override val type = START

    }

    private object NySøknadMottattTilstand : Sakskomplekstilstand {

        override fun håndterSendtSøknad(sakskompleks: Sakskompleks, sendtSøknadHendelse: SendtSøknadHendelse) {
            sakskompleks.håndterSykdomstidslinjeHendelse(sendtSøknadHendelse, SendtSøknadMottattTilstand)
        }

        override fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmeldingHendelse: InntektsmeldingHendelse) {
            sakskompleks.håndterSykdomstidslinjeHendelse(inntektsmeldingHendelse, InntektsmeldingMottattTilstand)
        }

        override val type = NY_SØKNAD_MOTTATT

    }

    private object SendtSøknadMottattTilstand : Sakskomplekstilstand {

        override fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmeldingHendelse: InntektsmeldingHendelse) {
            sakskompleks.håndterSykdomstidslinjeHendelse(inntektsmeldingHendelse, KomplettSykdomstidslinjeTilstand)
        }

        override val type = SENDT_SØKNAD_MOTTATT

    }

    private object InntektsmeldingMottattTilstand : Sakskomplekstilstand {

        override fun håndterSendtSøknad(sakskompleks: Sakskompleks, sendtSøknadHendelse: SendtSøknadHendelse) {
            sakskompleks.håndterSykdomstidslinjeHendelse(sendtSøknadHendelse, KomplettSykdomstidslinjeTilstand)
        }

        override val type = INNTEKTSMELDING_MOTTATT

    }

    private object KomplettSykdomstidslinjeTilstand : Sakskomplekstilstand {

        override val type = KOMPLETT_SYKDOMSTIDSLINJE

        private const val seksMåneder = 180

        override fun entering(sakskompleks: Sakskompleks) {
            sakskompleks.emitTrengerLøsning(BehovsTyper.Sykepengehistorikk)
        }

        override fun håndterSykepengehistorikk(sakskompleks: Sakskompleks, sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
            val sisteFraværsdag = sykepengehistorikkHendelse.sisteFraværsdag()
                    ?: return sakskompleks.setTilstand(sykepengehistorikkHendelse, TilGodkjenningTilstand)

            val tidslinje = sakskompleks.sykdomstidslinje
                    ?: return sakskompleks.setTilstand(sykepengehistorikkHendelse, TilInfotrygdTilstand)

            if (sisteFraværsdag > tidslinje.startdato() || sisteFraværsdag.datesUntil(tidslinje.startdato()).count() <= seksMåneder) {
                sakskompleks.setTilstand(sykepengehistorikkHendelse, TilInfotrygdTilstand)
            } else {
                sakskompleks.setTilstand(sykepengehistorikkHendelse, TilGodkjenningTilstand) {
                    val utbetalingsberegning = tidslinje.utbetalingsberegning(sakskompleks.dagsats())
                    sakskompleks.maksdato = utbetalingsberegning.maksdato
                    sakskompleks.utbetalingslinjer = utbetalingsberegning.utbetalingslinjer
                }
            }
        }
    }

    private object TilGodkjenningTilstand : Sakskomplekstilstand {
        override val type = TIL_GODKJENNING

        override fun entering(sakskompleks: Sakskompleks) {
            sakskompleks.emitTrengerLøsning(BehovsTyper.GodkjenningFraSaksbehandler)
        }

        override fun håndterManuellSaksbehandling(sakskompleks: Sakskompleks, manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
            if (manuellSaksbehandlingHendelse.utbetalingGodkjent()) {
                sakskompleks.setTilstand(manuellSaksbehandlingHendelse, TilUtbetalingTilstand) {
                    sakskompleks.godkjentAv = manuellSaksbehandlingHendelse.saksbehandler()
                }
            } else {
                sakskompleks.setTilstand(manuellSaksbehandlingHendelse, TilInfotrygdTilstand)
            }
        }
    }

    private object TilUtbetalingTilstand : Sakskomplekstilstand {
        override val type = TIL_UTBETALING

        override fun entering(sakskompleks: Sakskompleks) {
            sakskompleks.emitTrengerLøsning(BehovsTyper.Utbetaling)
        }

    }

    private object TilInfotrygdTilstand : Sakskomplekstilstand {
        override val type = TIL_INFOTRYGD

    }

    // Gang of four Memento pattern
    companion object {

        private val sykdomshendelseDeserializer = SykdomshendelseDeserializer()

        internal fun fromJson(sakskompleksJson: SakskompleksJson): Sakskompleks {
            return Sakskompleks(
                    id = sakskompleksJson.id,
                    aktørId = sakskompleksJson.aktørId,
                    organisasjonsnummer = sakskompleksJson.organisasjonsnummer
            ).apply {
                tilstand = tilstandFraEnum(sakskompleksJson.tilstandType)
                sykdomstidslinje = sakskompleksJson.sykdomstidslinje?.let {
                    if (!it.isNull) {
                        Sykdomstidslinje.fromJson(objectMapper.writeValueAsString(it), sykdomshendelseDeserializer)
                    } else {
                        null
                    }
                }
                maksdato = sakskompleksJson.maksdato
                utbetalingslinjer = sakskompleksJson.utbetalingslinjer?.map {
                    Utbetalingslinje(
                            fom = LocalDate.parse(it["fom"].textValue()),
                            tom = LocalDate.parse(it["tom"].textValue()),
                            dagsats = BigDecimal(it["dagsats"].doubleValue())
                    )
                }
                godkjentAv = sakskompleksJson.godkjentAv
            }
        }

        private fun tilstandFraEnum(tilstand: TilstandType) = when (tilstand) {
            START -> StartTilstand
            NY_SØKNAD_MOTTATT -> NySøknadMottattTilstand
            SENDT_SØKNAD_MOTTATT -> SendtSøknadMottattTilstand
            INNTEKTSMELDING_MOTTATT -> InntektsmeldingMottattTilstand
            KOMPLETT_SYKDOMSTIDSLINJE -> KomplettSykdomstidslinjeTilstand
            TIL_GODKJENNING -> TilGodkjenningTilstand
            TIL_UTBETALING -> TilUtbetalingTilstand
            TIL_INFOTRYGD -> TilInfotrygdTilstand
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
                sakskompleks.sykdomstidslinje = Sykdomstidslinje.fromJson(it.toString(), sykdomshendelseDeserializer)
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
                },
                maksdato = maksdato,
                utbetalingslinjer = utbetalingslinjer
                        ?.let { objectMapper.convertValue<JsonNode>(it) },
                godkjentAv = godkjentAv
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
            tidslinjeEvent: ArbeidstakerHendelse,
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
        val params = mutableMapOf(
                "sakskompleksId" to id,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer
        )

        utbetalingslinjer?.let { params.put("utbetalingslinjer", it) }
        maksdato?.let { params.put("maksdato", it) }
        godkjentAv?.let { params.put("saksbehandler", it) }

        val behov = Behov.nyttBehov(type, params)

        observers.forEach { observer ->
            observer.sakskompleksTrengerLøsning(behov)
        }
    }

    internal data class SakskompleksJson(
            val id: UUID,
            val aktørId: String,
            val organisasjonsnummer: String,
            val tilstandType: TilstandType,
            val sykdomstidslinje: JsonNode?,
            val maksdato: LocalDate?,
            val utbetalingslinjer: JsonNode?,
            val godkjentAv: String?
    )
}

