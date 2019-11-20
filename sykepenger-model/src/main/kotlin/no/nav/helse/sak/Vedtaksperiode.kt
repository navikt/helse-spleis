package no.nav.helse.sak

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.sak.Vedtaksperiode.TilstandType.*
import no.nav.helse.sak.VedtaksperiodeObserver.StateChangeEvent
import no.nav.helse.hendelser.SykdomshendelseDeserializer
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.sykdomstidslinje.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.apache.commons.codec.binary.Base32
import java.io.StringWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.time.LocalDate
import java.util.*

private inline fun <reified T> Set<*>.førsteAvType(): T {
    return first { it is T } as T
}

class Vedtaksperiode(
        private val id: UUID,
        private val aktørId: String,
        private val organisasjonsnummer: String
) {
    private val `6G` = (6 * 99858).toBigDecimal()

    private var tilstand: Vedtaksperiodetilstand = StartTilstand

    private var sykdomstidslinje: Sykdomstidslinje? = null

    private var maksdato: LocalDate? = null

    private var utbetalingslinjer: List<Utbetalingslinje>? = null

    private var godkjentAv: String? = null

    private var utbetalingsreferanse: String? = null

    private val observers: MutableList<VedtaksperiodeObserver> = mutableListOf()

    private fun inntektsmeldingHendelse() =
            this.sykdomstidslinje?.hendelser()?.førsteAvType<InntektsmeldingHendelse>()

    private fun sykepengegrunnlag() = (inntektsmeldingHendelse()?.beregnetInntekt() as BigDecimal).times(12.toBigDecimal())

    private fun beregningsgrunnlag() = sykepengegrunnlag().min(`6G`)

    internal fun dagsats() = beregningsgrunnlag().divide(260.toBigDecimal(), 0, RoundingMode.HALF_UP).toInt()

    internal fun håndter(nySøknadHendelse: NySøknadHendelse): Boolean {
        return overlapperMed(nySøknadHendelse).also {
            if (it) {
                tilstand.håndter(this, nySøknadHendelse)
            }
        }
    }

    internal fun håndter(sendtSøknadHendelse: SendtSøknadHendelse): Boolean {
        return overlapperMed(sendtSøknadHendelse).also {
            if (it) {
                tilstand.håndter(this, sendtSøknadHendelse)
            }
        }
    }

    internal fun håndter(inntektsmeldingHendelse: InntektsmeldingHendelse): Boolean {
        return overlapperMed(inntektsmeldingHendelse).also {
            if (it) {
                tilstand.håndter(this, inntektsmeldingHendelse)
            }
        }
    }

    internal fun håndter(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        if (id.toString() == sykepengehistorikkHendelse.vedtaksperiodeId()) tilstand.håndter(this, sykepengehistorikkHendelse)
    }

    internal fun håndter(manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
        if (id.toString() == manuellSaksbehandlingHendelse.vedtaksperiodeId()) tilstand.håndter(this, manuellSaksbehandlingHendelse)
    }

    internal fun invaliderSak(hendelse: ArbeidstakerHendelse) {
        setTilstand(hendelse, TilInfotrygdTilstand)
    }

    private fun overlapperMed(hendelse: SykdomstidslinjeHendelse) =
            this.sykdomstidslinje?.overlapperMed(hendelse.sykdomstidslinje()) ?: true

    private fun setTilstand(event: ArbeidstakerHendelse, nyTilstand: Vedtaksperiodetilstand, block: () -> Unit = {}) {
        tilstand.leaving()

        val previousStateName = tilstand.type
        val previousMemento = memento()

        tilstand = nyTilstand
        block()

        tilstand.entering(this)

        emitVedtaksperiodeEndret(tilstand.type, event, previousStateName, previousMemento)
    }

    private fun <HENDELSE> håndter(
            hendelse: HENDELSE,
            nesteTilstand: Vedtaksperiodetilstand
    ) where HENDELSE : SykdomstidslinjeHendelse, HENDELSE : ArbeidstakerHendelse {
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
    private interface Vedtaksperiodetilstand {

        val type: TilstandType

        // Default implementasjoner av transisjonene
        fun håndter(vedtaksperiode: Vedtaksperiode, nySøknadHendelse: NySøknadHendelse) {
            vedtaksperiode.setTilstand(nySøknadHendelse, TilInfotrygdTilstand)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, sendtSøknadHendelse: SendtSøknadHendelse) {
            vedtaksperiode.setTilstand(sendtSøknadHendelse, TilInfotrygdTilstand)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmeldingHendelse: InntektsmeldingHendelse) {
            vedtaksperiode.setTilstand(inntektsmeldingHendelse, TilInfotrygdTilstand)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
        }

        fun leaving() {
        }

        fun entering(vedtaksperiode: Vedtaksperiode) {
        }

    }

    private object StartTilstand : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, nySøknadHendelse: NySøknadHendelse) {
            vedtaksperiode.håndter(nySøknadHendelse, NySøknadMottattTilstand)
        }

        override val type = START

    }

    private object NySøknadMottattTilstand : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, sendtSøknadHendelse: SendtSøknadHendelse) {
            vedtaksperiode.håndter(sendtSøknadHendelse, SendtSøknadMottattTilstand)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmeldingHendelse: InntektsmeldingHendelse) {
            vedtaksperiode.håndter(inntektsmeldingHendelse, InntektsmeldingMottattTilstand)
        }

        override val type = NY_SØKNAD_MOTTATT

    }

    private object SendtSøknadMottattTilstand : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmeldingHendelse: InntektsmeldingHendelse) {
            vedtaksperiode.håndter(inntektsmeldingHendelse, KomplettSykdomstidslinjeTilstand)
        }

        override val type = SENDT_SØKNAD_MOTTATT

    }

    private object InntektsmeldingMottattTilstand : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, sendtSøknadHendelse: SendtSøknadHendelse) {
            vedtaksperiode.håndter(sendtSøknadHendelse, KomplettSykdomstidslinjeTilstand)
        }

        override val type = INNTEKTSMELDING_MOTTATT

    }

    private object KomplettSykdomstidslinjeTilstand : Vedtaksperiodetilstand {

        override val type = KOMPLETT_SYKDOMSTIDSLINJE

        private const val seksMåneder = 180

        override fun entering(vedtaksperiode: Vedtaksperiode) {
            vedtaksperiode.emitTrengerLøsning(BehovsTyper.Sykepengehistorikk, mapOf<String, Any>(
                    "tom" to vedtaksperiode.sykdomstidslinje!!.startdato().minusDays(1)
            ))
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
            val tidslinje = vedtaksperiode.sykdomstidslinje
                    ?: return vedtaksperiode.setTilstand(sykepengehistorikkHendelse, TilInfotrygdTilstand)

            val sisteFraværsdag = sykepengehistorikkHendelse.sisteFraværsdag()

            if (sisteFraværsdag != null && (sisteFraværsdag > tidslinje.startdato() || sisteFraværsdag.datesUntil(tidslinje.startdato()).count() <= seksMåneder)) {
                return vedtaksperiode.setTilstand(sykepengehistorikkHendelse, TilInfotrygdTilstand)
            }

            val utbetalingsberegning = try {
                tidslinje.utbetalingsberegning(vedtaksperiode.dagsats())
            } catch (ie: IllegalArgumentException) {
                return vedtaksperiode.setTilstand(sykepengehistorikkHendelse, TilInfotrygdTilstand)
            }

            if (!vedtaksperiode.helePeriodenSkalBetalesAvArbeidsgiver(utbetalingsberegning)) {
                return vedtaksperiode.setTilstand(sykepengehistorikkHendelse, TilInfotrygdTilstand)
            }

            vedtaksperiode.setTilstand(sykepengehistorikkHendelse, TilGodkjenningTilstand) {
                vedtaksperiode.maksdato = utbetalingsberegning.maksdato
                vedtaksperiode.utbetalingslinjer = utbetalingsberegning.utbetalingslinjer
            }
        }

        private fun Vedtaksperiode.helePeriodenSkalBetalesAvArbeidsgiver(utbetalingsberegning: Utbetalingsberegning): Boolean {
            val inntektsmelding = this.inntektsmeldingHendelse() ?: return false
            val sisteUtbetalingsdag = utbetalingsberegning.utbetalingslinjer.lastOrNull()?.tom ?: return true

            val opphørsdato = inntektsmelding.refusjon().opphoersdato
            if (opphørsdato != null && opphørsdato <= sisteUtbetalingsdag) {
                return false
            }

            return inntektsmelding.endringIRefusjoner().all { it > sisteUtbetalingsdag }
        }
    }

    private object TilGodkjenningTilstand : Vedtaksperiodetilstand {
        override val type = TIL_GODKJENNING

        override fun entering(vedtaksperiode: Vedtaksperiode) {
            vedtaksperiode.emitTrengerLøsning(BehovsTyper.GodkjenningFraSaksbehandler)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
            if (manuellSaksbehandlingHendelse.utbetalingGodkjent()) {
                vedtaksperiode.setTilstand(manuellSaksbehandlingHendelse, TilUtbetalingTilstand) {
                    vedtaksperiode.godkjentAv = manuellSaksbehandlingHendelse.saksbehandler()
                }
            } else {
                vedtaksperiode.setTilstand(manuellSaksbehandlingHendelse, TilInfotrygdTilstand)
            }
        }
    }

    private object TilUtbetalingTilstand : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING

        override fun entering(vedtaksperiode: Vedtaksperiode) {
            val utbetalingsreferanse = lagUtbetalingsReferanse(vedtaksperiode)
            vedtaksperiode.utbetalingsreferanse = utbetalingsreferanse

            vedtaksperiode.emitTrengerLøsning(BehovsTyper.Utbetaling, mapOf(
                    "utbetalingsreferanse" to utbetalingsreferanse
            ))

            val event = VedtaksperiodeObserver.UtbetalingEvent(
                    vedtaksperiodeId = vedtaksperiode.id,
                    aktørId = vedtaksperiode.aktørId,
                    organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                    utbetalingsreferanse = utbetalingsreferanse
            )
            vedtaksperiode.observers.forEach {
                it.vedtaksperiodeTilUtbetaling(event)
            }
        }

        private fun lagUtbetalingsReferanse(vedtaksperiode: Vedtaksperiode) = vedtaksperiode.id.base32Encode()

        private fun UUID.base32Encode(): String {
            val pad = '='
            return Base32(pad.toByte())
                    .encodeAsString(this.byteArray())
                    .replace(pad.toString(), "")
        }

        private fun UUID.byteArray() = ByteBuffer.allocate(Long.SIZE_BYTES * 2).apply {
            putLong(this@byteArray.mostSignificantBits)
            putLong(this@byteArray.leastSignificantBits)
        }.array()
    }

    private object TilInfotrygdTilstand : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD

    }

    // Gang of four Memento pattern
    companion object {

        private val sykdomshendelseDeserializer = SykdomshendelseDeserializer()

        internal fun fromJson(vedtaksperiodeJson: VedtaksperiodeJson): Vedtaksperiode {
            return Vedtaksperiode(
                    id = vedtaksperiodeJson.id,
                    aktørId = vedtaksperiodeJson.aktørId,
                    organisasjonsnummer = vedtaksperiodeJson.organisasjonsnummer
            ).apply {
                tilstand = tilstandFraEnum(vedtaksperiodeJson.tilstandType)
                sykdomstidslinje = vedtaksperiodeJson.sykdomstidslinje?.let {
                    if (!it.isNull) {
                        Sykdomstidslinje.fromJson(objectMapper.writeValueAsString(it), sykdomshendelseDeserializer)
                    } else {
                        null
                    }
                }
                maksdato = vedtaksperiodeJson.maksdato
                utbetalingslinjer = vedtaksperiodeJson.utbetalingslinjer?.map {
                    Utbetalingslinje(
                            fom = LocalDate.parse(it["fom"].textValue()),
                            tom = LocalDate.parse(it["tom"].textValue()),
                            dagsats = when (it["dagsats"]) {
                                is DecimalNode -> it["dagsats"].decimalValue().setScale(0, RoundingMode.HALF_UP).toInt()
                                else -> it["dagsats"].intValue()
                            }
                    )
                }
                godkjentAv = vedtaksperiodeJson.godkjentAv
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

        internal fun restore(memento: Memento): Vedtaksperiode {
            val node = objectMapper.readTree(memento.state)

            val vedtaksperiode = Vedtaksperiode(
                    id = UUID.fromString(node["id"].textValue()),
                    aktørId = node["aktørId"].textValue(),
                    organisasjonsnummer = node["organisasjonsnummer"].textValue()
            )

            vedtaksperiode.tilstand = tilstandFraEnum(enumValueOf(node["tilstand"].textValue()))

            node["sykdomstidslinje"]?.let {
                vedtaksperiode.sykdomstidslinje = Sykdomstidslinje.fromJson(it.toString(), sykdomshendelseDeserializer)
            }

            return vedtaksperiode
        }

    }

    fun memento(): Memento {
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

    internal fun jsonRepresentation(): VedtaksperiodeJson {
        return VedtaksperiodeJson(
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
                godkjentAv = godkjentAv,
                utbetalingsreferanse = utbetalingsreferanse
        )
    }

    class Memento(internal val state: String) {
        override fun toString() = state
    }

    // Gang of four Observer pattern
    internal fun addVedtaksperiodeObserver(observer: VedtaksperiodeObserver) {
        observers.add(observer)
    }

    private fun emitVedtaksperiodeEndret(
            currentState: TilstandType,
            tidslinjeEvent: ArbeidstakerHendelse,
            previousState: TilstandType,
            previousMemento: Memento
    ) {
        val event = StateChangeEvent(
                id = id,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                currentState = currentState,
                previousState = previousState,
                sykdomshendelse = tidslinjeEvent,
                currentMemento = memento(),
                previousMemento = previousMemento
        )

        observers.forEach { observer ->
            observer.vedtaksperiodeEndret(event)
        }
    }

    private fun emitTrengerLøsning(type: BehovsTyper, additionalParams: Map<String, Any> = emptyMap()) {
        val params = mutableMapOf(
                "sakskompleksId" to id,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer
        )

        params.putAll(additionalParams)

        utbetalingslinjer?.let { params.put("utbetalingslinjer",
            if (type == BehovsTyper.Utbetaling) it.joinForOppdrag() else it) }
        maksdato?.let { params.put("maksdato", it) }
        godkjentAv?.let { params.put("saksbehandler", it) }

        val behov = Behov.nyttBehov(type, params)

        observers.forEach { observer ->
            observer.vedtaksperiodeTrengerLøsning(behov)
        }
    }

    internal data class VedtaksperiodeJson(
            val id: UUID,
            val aktørId: String,
            val organisasjonsnummer: String,
            val tilstandType: TilstandType,
            val sykdomstidslinje: JsonNode?,
            val maksdato: LocalDate?,
            val utbetalingslinjer: JsonNode?,
            val godkjentAv: String?,
            val utbetalingsreferanse: String?
    )
}

