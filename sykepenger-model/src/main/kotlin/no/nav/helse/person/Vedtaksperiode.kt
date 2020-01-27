package no.nav.helse.person


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Grunnbeløp
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.VedtaksperiodeObserver.StateChangeEvent
import no.nav.helse.serde.safelyUnwrapDate
import no.nav.helse.sykdomstidslinje.*
import org.apache.commons.codec.binary.Base32
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

private inline fun <reified T> Set<*>.førsteAvType(): T? {
    return firstOrNull { it is T } as T?
}

internal class Vedtaksperiode internal constructor(
    private val id: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private var sykdomstidslinje: ConcreteSykdomstidslinje,
    private var tilstand: Vedtaksperiodetilstand = StartTilstand,
    private val aktivitetslogger: Aktivitetslogger = Aktivitetslogger()
) {

    private var maksdato: LocalDate? = null

    private var utbetalingslinjer: List<Utbetalingslinje>? = null

    private var godkjentAv: String? = null

    private var utbetalingsreferanse: String? = null

    private var førsteFraværsdag: LocalDate? = null
    private var inntektFraInntektsmelding: Double? = null
    private var dataForVilkårsvurdering: ModelVilkårsgrunnlag.Grunnlagsdata? = null

    private val sykdomshistorikk = Sykdomshistorikk()

    private val observers: MutableList<VedtaksperiodeObserver> = mutableListOf()

    private fun inntektsmeldingHendelse() =
        this.sykdomstidslinje.hendelser().førsteAvType<ModelInntektsmelding>()

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        visitor.preVisitVedtaksperiode(this)
        visitor.visitVedtaksperiodeAktivitetslogger(aktivitetslogger)
        sykdomshistorikk.accept(visitor)
        visitor.visitTilstand(tilstand)
        visitor.preVisitVedtaksperiodeSykdomstidslinje()
        sykdomstidslinje.accept(visitor)
        visitor.postVisitVedtaksperiodeSykdomstidslinje()
        visitor.preVisitUtbetalingslinjer()
        utbetalingslinjer?.forEach { visitor.visitUtbetalingslinje(it) }
        visitor.postVisitUtbetalingslinjer()
        visitor.postVisitVedtaksperiode(this)
    }

    internal fun førsteFraværsdag(): LocalDate? = førsteFraværsdag ?: inntektsmeldingHendelse()?.førsteFraværsdag
    internal fun dataForVilkårsvurdering() = dataForVilkårsvurdering
    internal fun inntektFraInntektsmelding() = inntektFraInntektsmelding ?: inntektsmeldingHendelse()?.beregnetInntekt

    private fun dagsats() = inntektsmeldingHendelse()?.dagsats(LocalDate.MAX, Grunnbeløp.`6G`)

    internal fun håndter(nySøknad: ModelNySøknad) = overlapperMed(nySøknad).also {
        if (it) tilstand.håndter(this, nySøknad)
        nySøknad.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(sendtSøknad: ModelSendtSøknad) = overlapperMed(sendtSøknad).also {
        if (it) tilstand.håndter(this, sendtSøknad)
        sendtSøknad.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(inntektsmelding: ModelInntektsmelding) = overlapperMed(inntektsmelding).also {
        if (it) tilstand.håndter(this, inntektsmelding)
        inntektsmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(person: Person, arbeidsgiver: Arbeidsgiver, ytelser: ModelYtelser) {
        if (id.toString() == ytelser.vedtaksperiodeId()) tilstand.håndter(
            person,
            arbeidsgiver,
            this,
            ytelser
        )
        ytelser.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(manuellSaksbehandling: ModelManuellSaksbehandling) {
        if (id.toString() == manuellSaksbehandling.vedtaksperiodeId()) tilstand.håndter(
            this,
            manuellSaksbehandling
        )
        manuellSaksbehandling.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
        if (id.toString() == vilkårsgrunnlag.vedtaksperiodeId()) tilstand.håndter(this, vilkårsgrunnlag)
        vilkårsgrunnlag.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(påminnelse: ModelPåminnelse): Boolean {
        if (id.toString() != påminnelse.vedtaksperiodeId()) return false
        tilstand.håndter(this, påminnelse)
        påminnelse.kopierAktiviteterTil(aktivitetslogger)
        return true
    }

    internal fun invaliderPeriode(hendelse: ArbeidstakerHendelse) {
        hendelse.warn("Invaliderer vedtaksperiode: %s", this.id.toString())
        tilstand(hendelse, TilInfotrygd)
    }

    private fun overlapperMed(hendelse: SykdomstidslinjeHendelse) =
        this.sykdomstidslinje.overlapperMed(hendelse.sykdomstidslinje())

    private fun tilstand(
        event: ArbeidstakerHendelse,
        nyTilstand: Vedtaksperiodetilstand,
        block: () -> Unit = {}
    ) {
        if (tilstand == nyTilstand) return  // Already in this state => ignore
        tilstand.leaving(event)

        val previousStateName = tilstand.type

        tilstand = nyTilstand
        block()

        try {
            tilstand.entering(this, event)
        } finally {
            emitVedtaksperiodeEndret(tilstand.type, event, previousStateName, tilstand.timeout)
        }
    }

    private fun håndter(hendelse: SykdomstidslinjeHendelse, nesteTilstand: Vedtaksperiodetilstand) {
        sykdomshistorikk.håndter(hendelse)
        val tidslinje = this.sykdomstidslinje + hendelse.sykdomstidslinje()

        if (tidslinje.erUtenforOmfang()) {
            hendelse.error("Ikke støttet dag")
            tilstand(hendelse, TilInfotrygd)
        } else {
            tilstand(hendelse, nesteTilstand) {
                sykdomstidslinje = tidslinje
            }
        }
        hendelse.kopierAktiviteterTil(aktivitetslogger)
    }

    private fun trengerYtelser() {
        emitTrengerLøsning(
            ModelYtelser.lagBehov(
                vedtaksperiodeId = id,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                utgangspunktForBeregningAvYtelse = sykdomstidslinje.utgangspunktForBeregningAvYtelse().minusDays(1)
            )
        )
    }

    internal fun trengerVilkårsgrunnlag(beregningStart: YearMonth, beregningSlutt: YearMonth) {
        emitTrengerLøsning(
            Behov.nyttBehov(
                hendelsestype = ArbeidstakerHendelse.Hendelsestype.Vilkårsgrunnlag,
                behov = listOf(Behovstype.Inntektsberegning, Behovstype.EgenAnsatt),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = id,
                additionalParams = mapOf(
                    "beregningStart" to beregningStart,
                    "beregningSlutt" to beregningSlutt
                )
            )
        )
    }

    private fun emitTrengerLøsning(behov: Behov) {
        observers.forEach { observer ->
            observer.vedtaksperiodeTrengerLøsning(behov)
        }
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
            gjeldendeTilstand = currentState,
            forrigeTilstand = previousState,
            sykdomshendelse = tidslinjeEvent,
            timeout = varighet
        )

        observers.forEach { observer ->
            observer.vedtaksperiodeEndret(event)
        }
    }

    private fun emitVedtaksperiodePåminnet(påminnelse: ModelPåminnelse) {
        observers.forEach { observer ->
            observer.vedtaksperiodePåminnet(påminnelse)
        }
    }

    // Gang of four State pattern
    internal interface Vedtaksperiodetilstand {
        val type: TilstandType

        val timeout: Duration

        fun håndter(vedtaksperiode: Vedtaksperiode, nySøknad: ModelNySøknad) {
            nySøknad.error("uventet NySøknad")
            vedtaksperiode.tilstand(nySøknad, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, sendtSøknad: ModelSendtSøknad) {
            sendtSøknad.error("uventet SendtSøknad")
            vedtaksperiode.tilstand(sendtSøknad, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: ModelInntektsmelding) {
            inntektsmelding.error("uventet Inntektsmelding")
            vedtaksperiode.tilstand(inntektsmelding, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: ModelVilkårsgrunnlag) {
            vilkårsgrunnlag.error("uventet vilkårsgrunnlag")
        }

        fun håndter(person: Person, arbeidsgiver: Arbeidsgiver, vedtaksperiode: Vedtaksperiode, ytelser: ModelYtelser) {
            ytelser.error("uventet sykdom- og inntektshistorikk")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, manuellSaksbehandling: ModelManuellSaksbehandling) {
            manuellSaksbehandling.error("uventet manuell saksbehandling")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: ModelPåminnelse) {
            if (!påminnelse.gjelderTilstand(type)) return
            vedtaksperiode.emitVedtaksperiodePåminnet(påminnelse)
            vedtaksperiode.tilstand(påminnelse, TilInfotrygd)
        }

        fun leaving(aktivitetslogger: IAktivitetslogger) {}

        fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {}
    }

    internal object StartTilstand : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, nySøknad: ModelNySøknad) {
            vedtaksperiode.tilstand(nySøknad, MottattNySøknad) {
                vedtaksperiode.sykdomshistorikk.håndter(nySøknad)
                vedtaksperiode.sykdomstidslinje = nySøknad.sykdomstidslinje()
            }
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av ny søknad")
        }

        override val type = START
        override val timeout: Duration = Duration.ofDays(30)
    }

    internal object MottattNySøknad : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, sendtSøknad: ModelSendtSøknad) {
            vedtaksperiode.håndter(sendtSøknad, MottattSendtSøknad)
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av sendt søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: ModelInntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.håndter(inntektsmelding, MottattInntektsmelding)
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av inntektsmelding")
        }

        override val type = MOTTATT_NY_SØKNAD

        override val timeout: Duration = Duration.ofDays(30)

    }

    internal object MottattSendtSøknad : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: ModelInntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, Vilkårsprøving)
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av inntektsmelding")
        }

        override val type = MOTTATT_SENDT_SØKNAD

        override val timeout: Duration = Duration.ofDays(30)

    }

    internal object MottattInntektsmelding : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, sendtSøknad: ModelSendtSøknad) {
            vedtaksperiode.håndter(sendtSøknad, Vilkårsprøving)
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av sendt søknad")
        }

        override val type = MOTTATT_INNTEKTSMELDING

        override val timeout: Duration = Duration.ofDays(30)

    }

    internal object Vilkårsprøving : Vedtaksperiodetilstand {
        override val type = VILKÅRSPRØVING

        override val timeout: Duration = Duration.ofHours(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            emitTrengerVilkårsgrunnlag(vedtaksperiode)
            aktivitetslogger.info("Forespør vilkårsgrunnlag")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: ModelPåminnelse) {
            if (!påminnelse.gjelderTilstand(type)) return
            vedtaksperiode.emitVedtaksperiodePåminnet(påminnelse)
            emitTrengerVilkårsgrunnlag(vedtaksperiode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: ModelVilkårsgrunnlag) {
            val inntektFraInntektsmelding = if (vedtaksperiode.inntektFraInntektsmelding() == null) {
                vedtaksperiode.aktivitetslogger.severe("Epic 3: Trenger mulighet for syketilfeller hvor det ikke er en inntektsmelding (syketilfellet starter i infotrygd)")
            } else vedtaksperiode.inntektFraInntektsmelding()!!

            val (behandlesManuelt, grunnlagsdata) = vilkårsgrunnlag.måHåndteresManuelt(inntektFraInntektsmelding)
            vedtaksperiode.dataForVilkårsvurdering = grunnlagsdata

            if (behandlesManuelt) return vedtaksperiode.tilstand(vilkårsgrunnlag, TilInfotrygd)

            vedtaksperiode.aktivitetslogger.info("Vilkårsgrunnlag verifisert")
            vedtaksperiode.tilstand(vilkårsgrunnlag, BeregnUtbetaling)
        }

        private fun emitTrengerVilkårsgrunnlag(vedtaksperiode: Vedtaksperiode) {
            val inntektsberegningSlutt = YearMonth.from(vedtaksperiode.førsteFraværsdag())
            val inntektsberegningStart = inntektsberegningSlutt.minusMonths(11)
            vedtaksperiode.trengerVilkårsgrunnlag(
                beregningStart = inntektsberegningStart,
                beregningSlutt = inntektsberegningSlutt
            )
        }

    }

    internal object BeregnUtbetaling : Vedtaksperiodetilstand {

        override val type = BEREGN_UTBETALING
        override val timeout: Duration = Duration.ofHours(1)

        private const val seksMåneder = 180

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            vedtaksperiode.trengerYtelser()
            aktivitetslogger.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: ModelPåminnelse) {
            if (!påminnelse.gjelderTilstand(type)) return
            vedtaksperiode.emitVedtaksperiodePåminnet(påminnelse)
            vedtaksperiode.trengerYtelser()
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: ModelYtelser
        ) {
            if (ytelser.foreldrepenger().overlapperMedSyketilfelle(
                    vedtaksperiode.sykdomstidslinje.førsteDag(),
                    vedtaksperiode.sykdomstidslinje.sisteDag()
                )
            ) {
                ytelser.error("Foreldrepenger overlapper med syketilfelle, sender saken til Infotrygd")
                return vedtaksperiode.tilstand(ytelser, TilInfotrygd)
            }

            if (harFraværsdagInnen6Mnd(ytelser, vedtaksperiode.sykdomstidslinje)) {
                ytelser.error("Har fraværsdag innenfor seks måneder, sender saken til Infotrygd")
                return vedtaksperiode.tilstand(ytelser, TilInfotrygd)
            }

            val dagsats = if (vedtaksperiode.dagsats() == null) {
                vedtaksperiode.aktivitetslogger.severe("Epic 3: Trenger mulighet for syketilfeller hvor det ikke er en inntektsmelding (syketilfellet starter i infotrygd)")
            } else vedtaksperiode.dagsats()!!

            val utbetalingsberegning = try {
                vedtaksperiode.sykdomstidslinje.utbetalingsberegning(dagsats, vedtaksperiode.fødselsnummer)
            } catch (ie: IllegalArgumentException) {
                vedtaksperiode.aktivitetslogger.error("Kunne ikke beregne utbetaling: ${ie.message}. Sender saken til Infotrygd")
                return vedtaksperiode.tilstand(ytelser, TilInfotrygd)
            }

            val sisteUtbetalingsdag = utbetalingsberegning.utbetalingslinjer.lastOrNull()?.tom
            val inntektsmelding = if (vedtaksperiode.inntektsmeldingHendelse() == null) {
                vedtaksperiode.aktivitetslogger.severe("Epic 3: Trenger mulighet for syketilfeller hvor det ikke er en inntektsmelding (syketilfellet starter i infotrygd)")
            } else vedtaksperiode.inntektsmeldingHendelse()!!

            if (sisteUtbetalingsdag == null || inntektsmelding.harEndringIRefusjon(sisteUtbetalingsdag)) {
                vedtaksperiode.aktivitetslogger.error("Mangler enten siste utbetalingsdag eller inntektsmelding har endringer i refusjon, sender saken til Infotrygd")
                return vedtaksperiode.tilstand(ytelser, TilInfotrygd)
            }

            vedtaksperiode.tilstand(ytelser, TilGodkjenning) {
                vedtaksperiode.maksdato = utbetalingsberegning.maksdato
                vedtaksperiode.utbetalingslinjer = utbetalingsberegning.utbetalingslinjer
                vedtaksperiode.aktivitetslogger.info("""Saken oppfyller krav for behandling, settes til "Til godkjenning"""")
            }
        }

        private fun harFraværsdagInnen6Mnd(
            ytelser: ModelYtelser,
            tidslinje: ConcreteSykdomstidslinje
        ): Boolean {
            val sisteFraværsdag = ytelser.sykepengehistorikk().sisteFraværsdag() ?: return false

            return sisteFraværsdag > tidslinje.utgangspunktForBeregningAvYtelse()
                || sisteFraværsdag.datesUntil(tidslinje.utgangspunktForBeregningAvYtelse()).count() <= seksMåneder
        }
    }

    private object TilGodkjenning : Vedtaksperiodetilstand {
        override val type = TIL_GODKJENNING
        override val timeout: Duration = Duration.ofDays(7)

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            vedtaksperiode.emitTrengerLøsning(
                ModelManuellSaksbehandling.lagBehov(
                    vedtaksperiode.id,
                    vedtaksperiode.aktørId,
                    vedtaksperiode.fødselsnummer,
                    vedtaksperiode.organisasjonsnummer
                )
            )
            aktivitetslogger.info("Forespør godkjenning fra saksbehandler")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            manuellSaksbehandling: ModelManuellSaksbehandling
        ) {
            if (manuellSaksbehandling.utbetalingGodkjent()) {
                vedtaksperiode.tilstand(manuellSaksbehandling, TilUtbetaling) {
                    vedtaksperiode.godkjentAv = manuellSaksbehandling.saksbehandler()
                    vedtaksperiode.aktivitetslogger.info("Utbetaling markert som godkjent av saksbehandler")
                }
            } else {
                vedtaksperiode.aktivitetslogger.error("Utbetaling markert som ikke godkjent av saksbehandler")
                vedtaksperiode.tilstand(manuellSaksbehandling, TilInfotrygd)
            }
        }
    }

    private object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING
        override val timeout: Duration = Duration.ZERO

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: ModelPåminnelse) {}

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            val utbetalingsreferanse = lagUtbetalingsReferanse(vedtaksperiode)
            vedtaksperiode.utbetalingsreferanse = utbetalingsreferanse

            vedtaksperiode.emitTrengerLøsning(
                Behov.nyttBehov(
                    hendelsestype = ArbeidstakerHendelse.Hendelsestype.Utbetaling,
                    behov = listOf(Behovstype.Utbetaling),
                    aktørId = vedtaksperiode.aktørId,
                    fødselsnummer = vedtaksperiode.fødselsnummer,
                    organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiode.id,
                    additionalParams = mapOf(
                        "utbetalingsreferanse" to utbetalingsreferanse,
                        "utbetalingslinjer" to (vedtaksperiode.utbetalingslinjer?.joinForOppdrag() ?: emptyList()),
                        "maksdato" to (vedtaksperiode.maksdato ?: ""),
                        "saksbehandler" to (vedtaksperiode.godkjentAv ?: "")
                    )
                )
            )

            val event = VedtaksperiodeObserver.UtbetalingEvent(
                vedtaksperiodeId = vedtaksperiode.id,
                aktørId = vedtaksperiode.aktørId,
                fødselsnummer = vedtaksperiode.fødselsnummer,
                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                utbetalingsreferanse = utbetalingsreferanse,
                utbetalingslinjer = vedtaksperiode.utbetalingslinjer ?: emptyList(),
                opprettet = LocalDate.now()
            )

            vedtaksperiode.aktivitetslogger.info("Satt til utbetaling")

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

    private object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD
        override val timeout: Duration = Duration.ZERO
        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            aktivitetslogger.warn("Sykdom for denne personen kan ikke behandles automatisk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: ModelPåminnelse) {}
    }

    // Gang of four Memento pattern
    companion object {

        internal fun restore(memento: Memento): Vedtaksperiode {
            return Vedtaksperiode(
                id = memento.id,
                aktørId = memento.aktørId,
                fødselsnummer = memento.fødselsnummer,
                organisasjonsnummer = memento.organisasjonsnummer,
                sykdomstidslinje = ConcreteSykdomstidslinje.fromJson(memento.sykdomstidslinje.toString())
            ).also { vedtaksperiode ->
                vedtaksperiode.tilstand = tilstandFraEnum(memento.tilstandType)
                vedtaksperiode.maksdato = memento.maksdato
                vedtaksperiode.utbetalingslinjer = memento.utbetalingslinjer?.map {
                    Utbetalingslinje(
                        fom = LocalDate.parse(it["fom"].textValue()),
                        tom = LocalDate.parse(it["tom"].textValue()),
                        dagsats = when (it["dagsats"]) {
                            is DecimalNode -> it["dagsats"].decimalValue().setScale(0, RoundingMode.HALF_UP).toInt()
                            else -> it["dagsats"].intValue()
                        }
                    )
                }
                vedtaksperiode.godkjentAv = memento.godkjentAv
                vedtaksperiode.utbetalingsreferanse = memento.utbetalingsreferanse
                vedtaksperiode.førsteFraværsdag = memento.førsteFraværsdag
                vedtaksperiode.dataForVilkårsvurdering = memento.dataForVilkårsvurdering?.let {
                    objectMapper.convertValue(it)
                }
            }
        }

        private fun tilstandFraEnum(tilstand: TilstandType) = when (tilstand) {
            START -> StartTilstand
            MOTTATT_NY_SØKNAD -> MottattNySøknad
            MOTTATT_SENDT_SØKNAD -> MottattSendtSøknad
            MOTTATT_INNTEKTSMELDING -> MottattInntektsmelding
            VILKÅRSPRØVING -> Vilkårsprøving
            BEREGN_UTBETALING -> BeregnUtbetaling
            TIL_GODKJENNING -> TilGodkjenning
            TIL_UTBETALING -> TilUtbetaling
            TIL_INFOTRYGD -> TilInfotrygd
        }

        internal fun nyPeriode(hendelse: SykdomstidslinjeHendelse, id: UUID = UUID.randomUUID()): Vedtaksperiode {
            return Vedtaksperiode(
                id = id,
                aktørId = hendelse.aktørId(),
                fødselsnummer = hendelse.fødselsnummer(),
                organisasjonsnummer = hendelse.organisasjonsnummer(),
                sykdomstidslinje = hendelse.sykdomstidslinje()
            )
        }

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    internal fun memento(): Memento {
        return Memento(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            tilstandType = tilstand.type,
            sykdomstidslinje = objectMapper.readTree(sykdomstidslinje.toJson()),
            maksdato = maksdato,
            utbetalingslinjer = utbetalingslinjer
                ?.let { objectMapper.convertValue<JsonNode>(it) },
            godkjentAv = godkjentAv,
            utbetalingsreferanse = utbetalingsreferanse,
            førsteFraværsdag = førsteFraværsdag,
            dataForVilkårsvurdering = dataForVilkårsvurdering?.let { objectMapper.convertValue<JsonNode>(it) }
        )
    }

    internal class Memento internal constructor(
        internal val id: UUID,
        internal val aktørId: String,
        internal val fødselsnummer: String,
        internal val organisasjonsnummer: String,
        internal val tilstandType: TilstandType,
        internal val sykdomstidslinje: JsonNode,
        internal val maksdato: LocalDate?,
        internal val utbetalingslinjer: JsonNode?,
        internal val godkjentAv: String?,
        internal val utbetalingsreferanse: String?,
        internal val førsteFraværsdag: LocalDate?,
        internal val dataForVilkårsvurdering: JsonNode?
    ) {

        internal companion object {
            private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

            fun fromJsonNode(json: JsonNode): Memento {
                return Memento(
                    id = UUID.fromString(json["id"].textValue()),
                    aktørId = json["aktørId"].textValue(),
                    fødselsnummer = json["fødselsnummer"].textValue(),
                    organisasjonsnummer = json["organisasjonsnummer"].textValue(),
                    tilstandType = valueOf(json["tilstandType"].textValue()),
                    sykdomstidslinje = json["sykdomstidslinje"],
                    maksdato = json["maksdato"].safelyUnwrapDate(),
                    utbetalingslinjer = json["utbetalingslinjer"]?.takeUnless { it.isNull },
                    godkjentAv = json["godkjentAv"]?.textValue(),
                    utbetalingsreferanse = json["utbetalingsreferanse"]?.textValue(),
                    førsteFraværsdag = json["førsteFraværsdag"].safelyUnwrapDate(),
                    dataForVilkårsvurdering = json["dataForVilkårsvurdering"]
                )
            }
        }

        fun state(): String =
            objectMapper.writeValueAsString(
                mapOf(
                    "id" to this.id,
                    "aktørId" to this.aktørId,
                    "fødselsnummer" to this.fødselsnummer,
                    "organisasjonsnummer" to this.organisasjonsnummer,
                    "tilstandType" to this.tilstandType,
                    "sykdomstidslinje" to this.sykdomstidslinje,
                    "maksdato" to this.maksdato,
                    "utbetalingslinjer" to this.utbetalingslinjer,
                    "godkjentAv" to this.godkjentAv,
                    "utbetalingsreferanse" to this.utbetalingsreferanse,
                    "førsteFraværsdag" to this.førsteFraværsdag,
                    "dataForVilkårsvurdering" to this.dataForVilkårsvurdering
                )
            )
    }
}

