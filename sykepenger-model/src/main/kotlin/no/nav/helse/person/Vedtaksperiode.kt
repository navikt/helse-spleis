package no.nav.helse.person


import no.nav.helse.Grunnbeløp
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Overlappende
import no.nav.helse.person.TilstandType.BEREGN_UTBETALING
import no.nav.helse.person.TilstandType.MOTTATT_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.MOTTATT_NY_SØKNAD
import no.nav.helse.person.TilstandType.MOTTATT_SENDT_SØKNAD
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_GODKJENNING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.TilstandType.VILKÅRSPRØVING
import no.nav.helse.person.VedtaksperiodeObserver.StateChangeEvent
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilder
import no.nav.helse.utbetalingstidslinje.joinForOppdrag
import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

private inline fun <reified T> Set<*>.førsteAvType(): T? {
    return firstOrNull { it is T } as T?
}

internal class Vedtaksperiode private constructor(
    private val id: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private var sykdomstidslinje: ConcreteSykdomstidslinje,
    private var tilstand: Vedtaksperiodetilstand,
    private var maksdato: LocalDate?,
    private var utbetalingslinjer: List<Utbetalingslinje>?,
    private var godkjentAv: String?,
    private var utbetalingsreferanse: String?,
    private var førsteFraværsdag: LocalDate?,
    private var inntektFraInntektsmelding: Double?,
    private var dataForVilkårsvurdering: ModelVilkårsgrunnlag.Grunnlagsdata?,
    private val sykdomshistorikk: Sykdomshistorikk,
    private val aktivitetslogger: Aktivitetslogger
) {

    internal constructor(
        id: UUID,
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        sykdomstidslinje: ConcreteSykdomstidslinje,
        tilstand: Vedtaksperiodetilstand = StartTilstand,
        aktivitetslogger: Aktivitetslogger = Aktivitetslogger()
    ) : this(
        id = id,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        sykdomstidslinje = sykdomstidslinje,
        tilstand = tilstand,
        aktivitetslogger = aktivitetslogger,
        maksdato = null,
        utbetalingslinjer = null,
        godkjentAv = null,
        utbetalingsreferanse = null,
        førsteFraværsdag = null,
        inntektFraInntektsmelding = null,
        dataForVilkårsvurdering = null,
        sykdomshistorikk = Sykdomshistorikk()
    )

    private val observers: MutableList<VedtaksperiodeObserver> = mutableListOf()

    private fun inntektsmeldingHendelse() =
        this.sykdomstidslinje.hendelser().førsteAvType<ModelInntektsmelding>()

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        visitor.preVisitVedtaksperiode(this, id)
        visitor.visitMaksdato(maksdato)
        visitor.visitGodkjentAv(godkjentAv)
        visitor.visitFørsteFraværsdag(førsteFraværsdag())
        visitor.visitInntektFraInntektsmelding(inntektFraInntektsmelding())
        visitor.visitDataForVilkårsvurdering(dataForVilkårsvurdering)
        visitor.visitVedtaksperiodeAktivitetslogger(aktivitetslogger)
        sykdomshistorikk.accept(visitor)
        visitor.visitTilstand(tilstand)
        visitor.preVisitVedtaksperiodeSykdomstidslinje()
        sykdomstidslinje.accept(visitor)
        visitor.postVisitVedtaksperiodeSykdomstidslinje()
        visitor.preVisitUtbetalingslinjer()
        utbetalingslinjer?.forEach { visitor.visitUtbetalingslinje(it) }
        visitor.postVisitUtbetalingslinjer()
        visitor.postVisitVedtaksperiode(this, id)
    }

    private fun førsteFraværsdag(): LocalDate? = førsteFraværsdag ?: inntektsmeldingHendelse()?.førsteFraværsdag
    private fun inntektFraInntektsmelding() =
        inntektFraInntektsmelding ?: inntektsmeldingHendelse()?.beregnetInntekt

    private fun dagsats() = inntektsmeldingHendelse()?.dagsats(LocalDate.MAX, Grunnbeløp.`6G`)

    private fun periode() = Periode(
        sykdomshistorikk.sykdomstidslinje().førsteDag(),
        sykdomshistorikk.sykdomstidslinje().sisteDag()
    )

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
                utgangspunktForBeregningAvYtelse = sykdomshistorikk.sykdomstidslinje().førsteDag().minusDays(1)
            )
        )
    }

    internal fun trengerVilkårsgrunnlag() {
        val beregningSlutt = YearMonth.from(førsteFraværsdag())
        val beregningStart = beregningSlutt.minusMonths(11)

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
            val inntektFraInntektsmelding = vedtaksperiode.inntektFraInntektsmelding()
                ?: vedtaksperiode.aktivitetslogger.severe("Epic 3: Trenger mulighet for syketilfeller hvor det ikke er en inntektsmelding (syketilfellet starter i infotrygd)")

            val (behandlesManuelt, grunnlagsdata) = vilkårsgrunnlag.måHåndteresManuelt(inntektFraInntektsmelding)
            vedtaksperiode.dataForVilkårsvurdering = grunnlagsdata

            if (behandlesManuelt) return vedtaksperiode.tilstand(vilkårsgrunnlag, TilInfotrygd)

            vedtaksperiode.aktivitetslogger.info("Vilkårsgrunnlag verifisert")
            vedtaksperiode.tilstand(vilkårsgrunnlag, BeregnUtbetaling)
        }

        private fun emitTrengerVilkårsgrunnlag(vedtaksperiode: Vedtaksperiode) {
            vedtaksperiode.trengerVilkårsgrunnlag()
        }

    }

    internal object BeregnUtbetaling : Vedtaksperiodetilstand {

        override val type = BEREGN_UTBETALING
        override val timeout: Duration = Duration.ofHours(1)

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
            Validation(ytelser).also { it ->
                it.onError { vedtaksperiode.tilstand(ytelser, TilInfotrygd) }
                it.valider { Overlappende(vedtaksperiode.periode(), ytelser.foreldrepenger()) }
                val sisteHistoriskeSykedag = ytelser.sykepengehistorikk().sisteFraværsdag()
                it.valider { GapPå26Uker(vedtaksperiode.sykdomshistorikk.sykdomstidslinje(), sisteHistoriskeSykedag) }
                it.valider {
                    HarInntektshistorikk(
                        arbeidsgiver, vedtaksperiode.sykdomshistorikk.sykdomstidslinje().førsteDag()
                    )
                }
                it.valider { HarArbeidsgivertidslinje(arbeidsgiver) }
                val utbetalingslinje = utbetalingstidslinje(arbeidsgiver, vedtaksperiode, sisteHistoriskeSykedag)
                var engineForTimeline: ByggUtbetalingstidlinjer? = null
                it.valider {
                    ByggUtbetalingstidlinjer(
                        mapOf(arbeidsgiver to utbetalingslinje),
                        vedtaksperiode.periode(),
                        ytelser,
                        Alder(vedtaksperiode.fødselsnummer)
                    ).also { engineForTimeline = it }
                }
                var engineForLine: ByggUtbetalingslinjer? = null
                it.valider { ByggUtbetalingslinjer(ytelser, arbeidsgiver.peekTidslinje()).also { engineForLine = it } }
                it.onSuccess {
                    vedtaksperiode.maksdato = engineForTimeline?.maksdato()
                    vedtaksperiode.utbetalingslinjer = engineForLine?.utbetalingslinjer()
                    ytelser.info("""Saken oppfyller krav for behandling, settes til "Til godkjenning"""")
                    vedtaksperiode.tilstand(ytelser, TilGodkjenning)
                }
            }
        }

        private fun utbetalingstidslinje(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            sisteHistoriskeSykedag: LocalDate?
        ): Utbetalingstidslinje {
            return UtbetalingstidslinjeBuilder(
                sykdomstidslinje = arbeidsgiver.sykdomstidslinje()!!,
                sisteDag = vedtaksperiode.sykdomshistorikk.sykdomstidslinje().sisteDag(),
                inntekthistorikk = arbeidsgiver.inntektshistorikk(),
                sisteNavDagForArbeidsgiverFørPerioden = sisteHistoriskeSykedag,
                arbeidsgiverRegler = NormalArbeidstaker
            ).result()
        }
    }


    internal fun continueIfNoErrors(vararg steps: ValidationStep, onError: () -> Unit) {
        return aktivitetslogger.continueIfNoErrors(*steps) { onError() }
    }

    internal object TilGodkjenning : Vedtaksperiodetilstand {
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

    internal object TilUtbetaling : Vedtaksperiodetilstand {
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

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD
        override val timeout: Duration = Duration.ZERO
        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            aktivitetslogger.warn("Sykdom for denne personen kan ikke behandles automatisk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: ModelPåminnelse) {}
    }

    companion object {
        internal fun sykdomstidslinje(perioder: List<Vedtaksperiode>) = perioder
            .map { it.sykdomshistorikk.sykdomstidslinje() }
            .reduce(ConcreteSykdomstidslinje::plus)

        internal fun nyPeriode(hendelse: SykdomstidslinjeHendelse, id: UUID = UUID.randomUUID()): Vedtaksperiode {
            return Vedtaksperiode(
                id = id,
                aktørId = hendelse.aktørId(),
                fødselsnummer = hendelse.fødselsnummer(),
                organisasjonsnummer = hendelse.organisasjonsnummer(),
                sykdomstidslinje = hendelse.sykdomstidslinje()
            )
        }
    }
}

