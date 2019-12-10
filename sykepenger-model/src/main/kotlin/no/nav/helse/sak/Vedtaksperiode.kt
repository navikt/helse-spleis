package no.nav.helse.sak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.hendelser.SykdomshendelseDeserializer
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.sak.TilstandType.*
import no.nav.helse.sak.VedtaksperiodeObserver.StateChangeEvent
import no.nav.helse.serde.safelyUnwrapDate
import no.nav.helse.sykdomstidslinje.*
import org.apache.commons.codec.binary.Base32
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.time.Duration
import java.time.LocalDate
import java.util.*

private inline fun <reified T> Set<*>.førsteAvType(): T {
    return first { it is T } as T
}

internal class Vedtaksperiode internal constructor(
    private val id: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String
) : Comparable<Vedtaksperiode> {
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

    private fun sykepengegrunnlag() =
        (inntektsmeldingHendelse()?.beregnetInntekt() as BigDecimal).times(12.toBigDecimal())

    private fun beregningsgrunnlag() = sykepengegrunnlag().min(`6G`)

    internal fun dagsats() = beregningsgrunnlag().divide(260.toBigDecimal(), 0, RoundingMode.HALF_UP).toInt()

    internal fun håndter(nySøknadHendelse: NySøknadHendelse) = overlapperMed(nySøknadHendelse).also {
        if (it) tilstand.håndter(this, nySøknadHendelse)
    }

    internal fun håndter(sendtSøknadHendelse: SendtSøknadHendelse) = overlapperMed(sendtSøknadHendelse).also {
        if (it) tilstand.håndter(this, sendtSøknadHendelse)
    }

    internal fun håndter(inntektsmeldingHendelse: InntektsmeldingHendelse): Boolean {
        return overlapperMed(inntektsmeldingHendelse).also {
            if (it) {
                tilstand.håndter(this, inntektsmeldingHendelse)
            }
        }
    }

    internal fun håndter(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        if (id.toString() == sykepengehistorikkHendelse.vedtaksperiodeId()) tilstand.håndter(
            this,
            sykepengehistorikkHendelse
        )
    }

    internal fun håndter(manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
        if (id.toString() == manuellSaksbehandlingHendelse.vedtaksperiodeId()) tilstand.håndter(
            this,
            manuellSaksbehandlingHendelse
        )
    }

    internal fun håndter(påminnelse: Påminnelse) {
        if (id.toString() == påminnelse.vedtaksperiodeId()) tilstand.håndter(
            this,
            påminnelse
        )
    }

    internal fun invaliderPeriode(hendelse: ArbeidstakerHendelse) {
        setTilstand(hendelse, TilInfotrygdTilstand)
    }

    private fun overlapperMed(hendelse: SykdomstidslinjeHendelse) =
        this.sykdomstidslinje?.overlapperMed(hendelse.sykdomstidslinje()) ?: true

    private fun setTilstand(event: ArbeidstakerHendelse, nyTilstand: Vedtaksperiodetilstand, block: () -> Unit = {}) {
        tilstand.leaving()

        val previousStateName = tilstand.type

        tilstand = nyTilstand
        block()

        tilstand.entering(this)

        emitVedtaksperiodeEndret(tilstand.type, event, previousStateName, tilstand.timeout)
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

    // Gang of four State pattern
    private interface Vedtaksperiodetilstand {

        val type: TilstandType
        val timeout: Duration

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

        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) =
            vedtaksperiode.setTilstand(påminnelse, TilInfotrygdTilstand)

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
        override val timeout: Duration = Duration.ofDays(30)
    }

    private object NySøknadMottattTilstand : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, sendtSøknadHendelse: SendtSøknadHendelse) {
            vedtaksperiode.håndter(sendtSøknadHendelse, SendtSøknadMottattTilstand)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmeldingHendelse: InntektsmeldingHendelse) {
            vedtaksperiode.håndter(inntektsmeldingHendelse, InntektsmeldingMottattTilstand)
        }

        override val type = NY_SØKNAD_MOTTATT
        override val timeout: Duration = Duration.ofDays(30)

    }

    private object SendtSøknadMottattTilstand : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmeldingHendelse: InntektsmeldingHendelse) {
            vedtaksperiode.håndter(inntektsmeldingHendelse, KomplettSykdomstidslinjeTilstand)
        }

        override val type = SENDT_SØKNAD_MOTTATT
        override val timeout: Duration = Duration.ofDays(30)

    }

    private object InntektsmeldingMottattTilstand : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, sendtSøknadHendelse: SendtSøknadHendelse) {
            vedtaksperiode.håndter(sendtSøknadHendelse, KomplettSykdomstidslinjeTilstand)
        }

        override val type = INNTEKTSMELDING_MOTTATT
        override val timeout: Duration = Duration.ofDays(30)

    }

    private object KomplettSykdomstidslinjeTilstand : Vedtaksperiodetilstand {

        override val type = KOMPLETT_SYKDOMSTIDSLINJE
        override val timeout: Duration = Duration.ofHours(1)

        private const val seksMåneder = 180

        override fun entering(vedtaksperiode: Vedtaksperiode) {
            trengerSykepengehistorikk(vedtaksperiode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) = trengerSykepengehistorikk(vedtaksperiode)

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
            val tidslinje = vedtaksperiode.sykdomstidslinje
                ?: return vedtaksperiode.setTilstand(sykepengehistorikkHendelse, TilInfotrygdTilstand)

            if (harFraværsdagInnen6Mnd(sykepengehistorikkHendelse, tidslinje)) {
                return vedtaksperiode.setTilstand(sykepengehistorikkHendelse, TilInfotrygdTilstand)
            }

            val utbetalingsberegning = try {
                tidslinje.utbetalingsberegning(vedtaksperiode.dagsats(), vedtaksperiode.fødselsnummer)
            } catch (ie: IllegalArgumentException) {
                return vedtaksperiode.setTilstand(sykepengehistorikkHendelse, TilInfotrygdTilstand)
            }

            if (utbetalingsberegning.utbetalingslinjer.isEmpty() || delerAvPeriodenSkalIkkeBetalesAvArbeidsgiver(vedtaksperiode, utbetalingsberegning)) {
                return vedtaksperiode.setTilstand(sykepengehistorikkHendelse, TilInfotrygdTilstand)
            }

            vedtaksperiode.setTilstand(sykepengehistorikkHendelse, TilGodkjenningTilstand) {
                vedtaksperiode.maksdato = utbetalingsberegning.maksdato
                vedtaksperiode.utbetalingslinjer = utbetalingsberegning.utbetalingslinjer
            }
        }

        private fun harFraværsdagInnen6Mnd(sykepengehistorikkHendelse: SykepengehistorikkHendelse, tidslinje: Sykdomstidslinje): Boolean {
            val sisteFraværsdag = sykepengehistorikkHendelse.sisteFraværsdag() ?: return false

            return sisteFraværsdag > tidslinje.utgangspunktForBeregningAvYtelse()
                || sisteFraværsdag.datesUntil(tidslinje.utgangspunktForBeregningAvYtelse()).count() <= seksMåneder
        }

        private fun delerAvPeriodenSkalIkkeBetalesAvArbeidsgiver(
            vedtaksperiode: Vedtaksperiode,
            utbetalingsberegning: Utbetalingsberegning
        ): Boolean {
            val inntektsmelding = vedtaksperiode.inntektsmeldingHendelse() ?: return true
            val sisteUtbetalingsdag = utbetalingsberegning.utbetalingslinjer.lastOrNull()?.tom ?: return false

            val opphørsdato = inntektsmelding.refusjon().opphoersdato
            if (opphørsdato != null && opphørsdato <= sisteUtbetalingsdag) {
                return true
            }

            return !inntektsmelding.endringIRefusjoner().all { it > sisteUtbetalingsdag }
        }

        private fun trengerSykepengehistorikk(vedtaksperiode: Vedtaksperiode) {
            vedtaksperiode.emitTrengerLøsning(
                BehovsTyper.Sykepengehistorikk, mapOf<String, Any>(
                    "tom" to vedtaksperiode.sykdomstidslinje!!.utgangspunktForBeregningAvYtelse().minusDays(1)
                )
            )
        }
    }

    private object TilGodkjenningTilstand : Vedtaksperiodetilstand {
        override val type = TIL_GODKJENNING
        override val timeout: Duration = Duration.ofDays(7)

        override fun entering(vedtaksperiode: Vedtaksperiode) {
            vedtaksperiode.emitTrengerLøsning(BehovsTyper.GodkjenningFraSaksbehandler)
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse
        ) {
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
        override val timeout: Duration = Duration.ofDays(7)

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            // TODO bør kanskje varsle saksbehandler hvis utbetaling ikke har skjedd?
            //  Revisit når Spenn svarer på status for utbetaling
        }

        override fun entering(vedtaksperiode: Vedtaksperiode) {
            val utbetalingsreferanse = lagUtbetalingsReferanse(vedtaksperiode)
            vedtaksperiode.utbetalingsreferanse = utbetalingsreferanse

            vedtaksperiode.emitTrengerLøsning(
                BehovsTyper.Utbetaling, mapOf(
                    "utbetalingsreferanse" to utbetalingsreferanse,
                    "utbetalingslinjer" to (vedtaksperiode.utbetalingslinjer?.joinForOppdrag() ?: emptyList()),
                    "maksdato" to (vedtaksperiode.maksdato ?: ""),
                    "saksbehandler" to (vedtaksperiode.godkjentAv ?: "")
                )
            )

            val event = VedtaksperiodeObserver.UtbetalingEvent(
                vedtaksperiodeId = vedtaksperiode.id,
                aktørId = vedtaksperiode.aktørId,
                fødselsnummer = vedtaksperiode.fødselsnummer,
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
        override val timeout: Duration = Duration.ZERO

    }

    // Gang of four Memento pattern
    companion object {

        private val sykdomshendelseDeserializer = SykdomshendelseDeserializer()

        internal fun restore(memento: Memento): Vedtaksperiode {
            return Vedtaksperiode(
                id = memento.id,
                aktørId = memento.aktørId,
                fødselsnummer = memento.fødselsnummer,
                organisasjonsnummer = memento.organisasjonsnummer
            ).also {
                it.tilstand = tilstandFraEnum(memento.tilstandType)
                it.sykdomstidslinje = memento.sykdomstidslinje
                    ?.let {
                        Sykdomstidslinje.fromJson(it.toString(), sykdomshendelseDeserializer)
                    }
                it.maksdato = memento.maksdato
                it.utbetalingslinjer = memento.utbetalingslinjer?.map {
                    Utbetalingslinje(
                        fom = LocalDate.parse(it["fom"].textValue()),
                        tom = LocalDate.parse(it["tom"].textValue()),
                        dagsats = when (it["dagsats"]) {
                            is DecimalNode -> it["dagsats"].decimalValue().setScale(0, RoundingMode.HALF_UP).toInt()
                            else -> it["dagsats"].intValue()
                        }
                    )
                }
                it.godkjentAv = memento.godkjentAv
                it.utbetalingsreferanse = memento.utbetalingsreferanse
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

        fun compare(leftFom: LocalDate?, leftTom: LocalDate?, rightFom: LocalDate?, rightTom: LocalDate?): Int =
            when {
                rightFom == null && leftFom != null -> 1
                rightFom != null && leftFom == null -> -1
                rightFom == null && rightTom == null && leftFom == null && leftTom == null -> 0
                leftFom != rightFom -> leftFom!!.compareTo(rightFom)
                rightTom == null && leftTom != null -> -1
                rightTom != null && leftTom == null -> 1
                else -> leftTom!!.compareTo(rightTom)
            }
    }

    internal fun memento(): Memento {
        return Memento(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
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

    // Gang of four Observer pattern
    internal fun addVedtaksperiodeObserver(observer: VedtaksperiodeObserver) {
        observers.add(observer)
    }

    private fun emitVedtaksperiodeEndret(
        currentState: TilstandType,
        tidslinjeEvent: ArbeidstakerHendelse,
        previousState: TilstandType,
        varighet: Duration
    ) {
        val event = StateChangeEvent(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            currentState = currentState,
            previousState = previousState,
            sykdomshendelse = tidslinjeEvent,
            timeout = varighet
        )

        observers.forEach { observer ->
            observer.vedtaksperiodeEndret(event)
        }
    }

    private fun emitTrengerLøsning(type: BehovsTyper, additionalParams: Map<String, Any> = emptyMap()) {
        val params = mutableMapOf(
            "sakskompleksId" to id,
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer
        )

        params.putAll(additionalParams)

        val behov = Behov.nyttBehov(type, params)

        observers.forEach { observer ->
            observer.vedtaksperiodeTrengerLøsning(behov)
        }
    }

    internal class Memento internal constructor(
        internal val id: UUID,
        internal val aktørId: String,
        internal val fødselsnummer: String,
        internal val organisasjonsnummer: String,
        internal val tilstandType: TilstandType,
        internal val sykdomstidslinje: JsonNode?,
        internal val maksdato: LocalDate?,
        internal val utbetalingslinjer: JsonNode?,
        internal val godkjentAv: String?,
        internal val utbetalingsreferanse: String?
    ) {

        internal companion object {
            private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

            fun fromString(state: String): Memento {
                val json = objectMapper.readTree(state)

                return Memento(
                    id = UUID.fromString(json["id"].textValue()),
                    aktørId = json["aktørId"].textValue(),
                    fødselsnummer = json["fødselsnummer"].textValue(),
                    organisasjonsnummer = json["organisasjonsnummer"].textValue(),
                    tilstandType = valueOf(json["tilstandType"].textValue()),
                    sykdomstidslinje = json["sykdomstidslinje"]?.takeUnless { it.isNull },
                    maksdato = json["maksdato"].safelyUnwrapDate(),
                    utbetalingslinjer = json["utbetalingslinjer"]?.takeUnless { it.isNull },
                    godkjentAv = json["godkjentAv"]?.textValue(),
                    utbetalingsreferanse = json["utbetalingsreferanse"]?.textValue()
                )
            }
        }

        fun state(): String =
            objectMapper.writeValueAsString(mapOf(
                "id" to this.id,
                "aktørId" to this.aktørId,
                "fødselsnummer" to this.fødselsnummer,
                "organisasjonsnummer" to this.organisasjonsnummer,
                "tilstandType" to this.tilstandType,
                "sykdomstidslinje" to this.sykdomstidslinje,
                "maksdato" to this.maksdato,
                "utbetalingslinjer" to this.utbetalingslinjer,
                "godkjentAv" to this.godkjentAv,
                "utbetalingsreferanse" to this.utbetalingsreferanse
            ))
    }

    override fun compareTo(other: Vedtaksperiode): Int = compare(
        leftFom = this.sykdomstidslinje?.førsteDag(),
        leftTom = this.sykdomstidslinje?.sisteDag(),
        rightFom = other.sykdomstidslinje?.førsteDag(),
        rightTom = other.sykdomstidslinje?.sisteDag()
    )
}

