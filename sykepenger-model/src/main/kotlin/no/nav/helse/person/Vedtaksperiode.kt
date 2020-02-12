package no.nav.helse.person


import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.behov.Pakke
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogger.Aktivitet.Need.NeedType
import no.nav.helse.person.Arbeidsgiver.GjennoptaBehandling
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.harTilstøtende
import no.nav.helse.tournament.historiskDagturnering
import no.nav.helse.utbetalingstidslinje.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class Vedtaksperiode private constructor(
    private val person: Person,
    private val arbeidsgiver: Arbeidsgiver,
    private val id: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private var tilstand: Vedtaksperiodetilstand,
    private var maksdato: LocalDate?,
    private var utbetalingslinjer: List<Utbetalingslinje>?,
    private var godkjentAv: String?,
    private var utbetalingsreferanse: String?,
    private var førsteFraværsdag: LocalDate?,
    private var inntektFraInntektsmelding: Double?,
    private var dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?,
    private val sykdomshistorikk: Sykdomshistorikk,
    private val aktivitetslogger: Aktivitetslogger
) {

    internal constructor(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        tilstand: Vedtaksperiodetilstand = StartTilstand,
        aktivitetslogger: Aktivitetslogger = Aktivitetslogger()
    ) : this(
        person = person,
        arbeidsgiver = arbeidsgiver,
        id = id,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
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
        sykdomshistorikk.sykdomstidslinje().accept(visitor)
        visitor.postVisitVedtaksperiodeSykdomstidslinje()
        visitor.preVisitUtbetalingslinjer()
        utbetalingslinjer?.forEach { visitor.visitUtbetalingslinje(it) }
        visitor.postVisitUtbetalingslinjer()
        visitor.postVisitVedtaksperiode(this, id)
    }

    private fun førsteFraværsdag(): LocalDate? = førsteFraværsdag
    private fun inntektFraInntektsmelding() = inntektFraInntektsmelding

    private fun periode() = Periode(
        sykdomshistorikk.sykdomstidslinje().førsteDag(),
        sykdomshistorikk.sykdomstidslinje().sisteDag()
    )

    internal fun håndter(nySøknad: NySøknad) = overlapperMed(nySøknad).also {
        if (!it) return it
        tilstand.håndter(this, nySøknad)
        nySøknad.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(sendtSøknad: SendtSøknad) =
        overlapperMed(sendtSøknad).also {
            if (!it) return it
            tilstand.håndter(this, arbeidsgiver, person, sendtSøknad)
            sendtSøknad.kopierAktiviteterTil(aktivitetslogger)
        }

    internal fun håndter(inntektsmelding: Inntektsmelding) = overlapperMed(inntektsmelding).also {
        if (!it) return it
        tilstand.håndter(this, inntektsmelding)
        inntektsmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(ytelser: Ytelser) {
        if (id.toString() != ytelser.vedtaksperiodeId()) return
        tilstand.håndter(person, arbeidsgiver, this, ytelser)
        ytelser.kopierAktiviteterTil(aktivitetslogger)

    }

    internal fun håndter(manuellSaksbehandling: ManuellSaksbehandling) {
        if (id.toString() != manuellSaksbehandling.vedtaksperiodeId()) return
        tilstand.håndter(person, arbeidsgiver, this, manuellSaksbehandling)
        manuellSaksbehandling.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        if (id.toString() != vilkårsgrunnlag.vedtaksperiodeId()) return
        tilstand.håndter(this, vilkårsgrunnlag)
        vilkårsgrunnlag.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
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
        sykdomshistorikk.isEmpty() || this.sykdomshistorikk.sykdomstidslinje().overlapperMed(hendelse.sykdomstidslinje())

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

        if (sykdomshistorikk.sykdomstidslinje().erUtenforOmfang()) {
            hendelse.error("Ikke støttet dag")
            tilstand(hendelse, TilInfotrygd)
        } else {
            tilstand(hendelse, nesteTilstand)
        }
        hendelse.kopierAktiviteterTil(aktivitetslogger)
    }

    private val transport = object : Pakke.Transportpakke {
        override operator fun plus(other: Pakke) = Pakke(
            emptyList(), mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to id
            )
        ) + other
    }

    private fun trengerYtelser() {
        aktivitetslogger.need(
            NeedType.Sykepengehistorikk(
                transportpakke = transport,
                utgangspunktForBeregningAvYtelse = sykdomshistorikk.sykdomstidslinje().førsteDag().minusDays(1)
            ), "Trenger sykepengehistorikk"
        )
        aktivitetslogger.need(
            NeedType.Foreldrepenger(
                transportpakke = transport
            ), "Trenger foreldrepenger"
        )
    }

    @Deprecated("Skal bruke aktivitetslogger.need()")
    internal fun trengerVilkårsgrunnlag() {
        aktivitetslogger.need(NeedType.Inntektsberegning(transport), "Trenger inntektsberegning")
        aktivitetslogger.need(NeedType.EgenAnsatt(transport), "Trenger egenAnsatt")
        aktivitetslogger.need(NeedType.Opptjening(transport), "Trenger opptjening")

        val beregningSlutt = YearMonth.from(førsteFraværsdag())
        val beregningStart = beregningSlutt.minusMonths(11)

        Behov.nyttBehov(
            hendelsestype = ArbeidstakerHendelse.Hendelsestype.Vilkårsgrunnlag,
            behov = listOf(Behovstype.Inntektsberegning, Behovstype.EgenAnsatt, Behovstype.Opptjening),
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = id,
            additionalParams = mapOf(
                "beregningStart" to beregningStart,
                "beregningSlutt" to beregningSlutt
            )
        )
    }

    private fun emitVedtaksperiodeEndret(
        currentState: TilstandType,
        tidslinjeEvent: ArbeidstakerHendelse,
        previousState: TilstandType,
        varighet: Duration
    ) {
        val event = PersonObserver.VedtaksperiodeEndretTilstandEvent(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            gjeldendeTilstand = currentState,
            forrigeTilstand = previousState,
            sykdomshendelse = tidslinjeEvent,
            timeout = varighet
        )

        person.vedtaksperiodeEndret(event)
    }

    internal fun harTilstøtende(other: Vedtaksperiode) =
        this.sykdomshistorikk.sykdomstidslinje().harTilstøtende(other.sykdomshistorikk.sykdomstidslinje())

    internal fun erFerdigBehandlet(other: Vedtaksperiode) =
        (this.periode().start >= other.periode().start) || this.tilstand.type in listOf(TIL_UTBETALING, TIL_INFOTRYGD)

    internal fun håndter(arbeidsgiver: Arbeidsgiver, other: Vedtaksperiode, hendelse: GjennoptaBehandling) {
        if (this.periode().start > other.periode().start) tilstand.håndter(arbeidsgiver, this, hendelse)
    }

    // Gang of four State pattern
    internal interface Vedtaksperiodetilstand {
        val type: TilstandType

        val timeout: Duration

        fun håndter(vedtaksperiode: Vedtaksperiode, nySøknad: NySøknad) {
            nySøknad.error("uventet NySøknad")
            vedtaksperiode.tilstand(nySøknad, TilInfotrygd)
        }

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            person: Person,
            sendtSøknad: SendtSøknad
        ) {
            sendtSøknad.error("uventet SendtSøknad")
            vedtaksperiode.tilstand(sendtSøknad, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            inntektsmelding.warn("uventet Inntektsmelding")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vilkårsgrunnlag.error("uventet vilkårsgrunnlag")
        }

        fun håndter(person: Person, arbeidsgiver: Arbeidsgiver, vedtaksperiode: Vedtaksperiode, ytelser: Ytelser) {
            ytelser.error("uventet sykdom- og inntektshistorikk")
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            manuellSaksbehandling: ManuellSaksbehandling
        ) {
            manuellSaksbehandling.error("uventet manuell saksbehandling")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (!påminnelse.gjelderTilstand(type)) return
            vedtaksperiode.person.vedtaksperiodePåminnet(påminnelse)
            vedtaksperiode.tilstand(påminnelse, TilInfotrygd)
        }

        fun håndter(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            gjennoptaBehandling: GjennoptaBehandling
        ) {
            gjennoptaBehandling.info("Tidligere periode ferdig behandlet")
        }

        fun leaving(aktivitetslogger: IAktivitetslogger) {}

        fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {}
    }

    internal object StartTilstand : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, nySøknad: NySøknad) {
            vedtaksperiode.tilstand(nySøknad, MottattNySøknad) {
                vedtaksperiode.sykdomshistorikk.håndter(nySøknad)
            }
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av ny søknad")
        }

        override val type = START
        override val timeout: Duration = Duration.ofDays(30)
    }

    internal object MottattNySøknad : Vedtaksperiodetilstand {

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            person: Person,
            sendtSøknad: SendtSøknad
        ) {
            val nesteTilstand =
                when {
                    !arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode) -> AvventerTidligerePeriodeEllerInntektsmelding
                    arbeidsgiver.harTilstøtendePeriode(vedtaksperiode) -> AvventerHistorikk
                    else -> UndersøkerHistorikk
                }
            vedtaksperiode.håndter(sendtSøknad, nesteTilstand)
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av sendt søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerSendtSøknad)
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av inntektsmelding")
        }

        override val type = MOTTATT_NY_SØKNAD

        override val timeout: Duration = Duration.ofDays(30)
    }

    internal object AvventerTidligerePeriodeEllerInntektsmelding : Vedtaksperiodetilstand {
        override val type = AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING
        override val timeout: Duration = Duration.ofDays(30)

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            aktivitetslogger.info("Avventer ferdigbehandlig av tidligere periode eller inntektsmelding før videre behandling")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerTidligerePeriode)
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av inntektsmelding")
        }

        override fun håndter(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            gjennoptaBehandling: GjennoptaBehandling
        ) {
            if (!arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)) return
            vedtaksperiode.tilstand(
                gjennoptaBehandling,
                if (arbeidsgiver.harTilstøtendePeriode(vedtaksperiode)) AvventerHistorikk
                else AvventerInntektsmelding
            )
        }
    }

    internal object AvventerTidligerePeriode : Vedtaksperiodetilstand {
        override val type = AVVENTER_TIDLIGERE_PERIODE
        override val timeout: Duration = Duration.ofDays(30)

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            aktivitetslogger.info("Avventer ferdigbehandlig av tidligere periode før videre behandling")
        }

        override fun håndter(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            gjennoptaBehandling: GjennoptaBehandling
        ) {
            if (!arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)) return
            vedtaksperiode.tilstand(gjennoptaBehandling, AvventerVilkårsprøving)
        }
    }

    internal object UndersøkerHistorikk : Vedtaksperiodetilstand {

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            vedtaksperiode.trengerYtelser()
            aktivitetslogger.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser
        ) {
            val sisteHistoriskeSykedag = ytelser.sykepengehistorikk().sisteFraværsdag()
            if (sisteHistoriskeSykedag == null || !sisteHistoriskeSykedag.harTilstøtende(vedtaksperiode.periode().start))
                return vedtaksperiode.tilstand(ytelser, AvventerInntektsmelding)
            Validation(ytelser).also { it ->
                it.onError { vedtaksperiode.tilstand(ytelser, TilInfotrygd) }
                it.valider { Overlappende(vedtaksperiode.periode(), ytelser.foreldrepenger()) }
                it.valider {
                    HarInntektshistorikk(
                        arbeidsgiver, vedtaksperiode.sykdomshistorikk.sykdomstidslinje().førsteDag()
                    )
                }
                it.valider { HarArbeidsgivertidslinje(arbeidsgiver) }
                val utbetalingstidslinje =
                    utbetalingstidslinje(arbeidsgiver, vedtaksperiode, sisteHistoriskeSykedag)
                lateinit var engineForTimeline: ByggUtbetalingstidlinjer
                it.valider {
                    ByggUtbetalingstidlinjer(
                        mapOf(arbeidsgiver to utbetalingstidslinje),
                        vedtaksperiode.periode(),
                        ytelser,
                        Alder(vedtaksperiode.fødselsnummer)
                    ).also { engineForTimeline = it }
                }
                lateinit var engineForLine: ByggUtbetalingslinjer
                it.valider { ByggUtbetalingslinjer(ytelser, arbeidsgiver.peekTidslinje()).also { engineForLine = it } }
                it.onSuccess {
                    vedtaksperiode.maksdato = engineForTimeline.maksdato()
                    vedtaksperiode.utbetalingslinjer = engineForLine.utbetalingslinjer()
                    ytelser.info("""Saken oppfyller krav for behandling, settes til "Til godkjenning"""")
                    vedtaksperiode.tilstand(ytelser, AvventerGodkjenning)
                }
            }

        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (!påminnelse.gjelderTilstand(AvventerHistorikk.type)) return
            vedtaksperiode.person.vedtaksperiodePåminnet(påminnelse)
            vedtaksperiode.trengerYtelser()
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøving)
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av inntektsmelding")
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


        override val type = UNDERSØKER_HISTORIKK

        override val timeout: Duration = Duration.ofHours(1)
    }

    internal object AvventerSendtSøknad : Vedtaksperiodetilstand {

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            person: Person,
            sendtSøknad: SendtSøknad
        ) {
            val nesteTilstand =
                if (arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)) AvventerVilkårsprøving
                else AvventerTidligerePeriode
            vedtaksperiode.håndter(sendtSøknad, nesteTilstand)
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av sendt søknad")
        }

        override val type = AVVENTER_SENDT_SØKNAD

        override val timeout: Duration = Duration.ofDays(30)
    }

    internal object AvventerInntektsmelding : Vedtaksperiodetilstand {

        override val type = AVVENTER_INNTEKTSMELDING

        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøving)
            vedtaksperiode.aktivitetslogger.info("Fullført behandling av inntektsmelding")
        }
    }

    internal object AvventerVilkårsprøving : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING

        override val timeout: Duration = Duration.ofHours(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            emitTrengerVilkårsgrunnlag(vedtaksperiode)
            aktivitetslogger.info("Forespør vilkårsgrunnlag")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (!påminnelse.gjelderTilstand(type)) return
            vedtaksperiode.person.vedtaksperiodePåminnet(påminnelse)
            emitTrengerVilkårsgrunnlag(vedtaksperiode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            val førsteFraværsdag = vedtaksperiode.førsteFraværsdag
                ?: vedtaksperiode.aktivitetslogger.severe("Første fraværsdag mangler i Vilkårsprøving tilstand")
            val inntektFraInntektsmelding = vedtaksperiode.inntektFraInntektsmelding()
                ?: vedtaksperiode.aktivitetslogger.severe("Epic 3: Trenger mulighet for syketilfeller hvor det ikke er en inntektsmelding (syketilfellet starter i infotrygd)")

            val resultat = vilkårsgrunnlag.måHåndteresManuelt(inntektFraInntektsmelding, førsteFraværsdag)
            vedtaksperiode.dataForVilkårsvurdering = resultat.grunnlagsdata

            if (resultat.måBehandlesManuelt) return vedtaksperiode.tilstand(vilkårsgrunnlag, TilInfotrygd)

            vedtaksperiode.aktivitetslogger.info("Vilkårsgrunnlag verifisert")
            vedtaksperiode.tilstand(vilkårsgrunnlag, AvventerHistorikk)
        }

        private fun emitTrengerVilkårsgrunnlag(vedtaksperiode: Vedtaksperiode) {
            vedtaksperiode.trengerVilkårsgrunnlag()
        }

    }

    internal object AvventerHistorikk : Vedtaksperiodetilstand {

        override val type = AVVENTER_HISTORIKK
        override val timeout: Duration = Duration.ofHours(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            vedtaksperiode.trengerYtelser()
            aktivitetslogger.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (!påminnelse.gjelderTilstand(type)) return
            vedtaksperiode.person.vedtaksperiodePåminnet(påminnelse)
            vedtaksperiode.trengerYtelser()
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser
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
                val utbetalingstidslinje = utbetalingstidslinje(arbeidsgiver, vedtaksperiode, sisteHistoriskeSykedag)
                var engineForTimeline: ByggUtbetalingstidlinjer? = null
                it.valider {
                    ByggUtbetalingstidlinjer(
                        mapOf(arbeidsgiver to utbetalingstidslinje),
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
                    vedtaksperiode.tilstand(ytelser, AvventerGodkjenning)
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

    internal object AvventerGodkjenning : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING
        override val timeout: Duration = Duration.ofDays(7)

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            aktivitetslogger.need(
                NeedType.Godkjenning(vedtaksperiode.transport),
                "Forespør godkjenning fra saksbehandler"
            )
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            manuellSaksbehandling: ManuellSaksbehandling
        ) {
            if (manuellSaksbehandling.utbetalingGodkjent()) {
                vedtaksperiode.tilstand(manuellSaksbehandling, TilUtbetaling) {
                    vedtaksperiode.godkjentAv = manuellSaksbehandling.saksbehandler().also {
                        vedtaksperiode.aktivitetslogger.info(
                            "Utbetaling markert som godkjent av saksbehandler (%s)",
                            it
                        )
                    }
                }
                arbeidsgiver.gjennoptaBehandling(vedtaksperiode)
            } else {
                vedtaksperiode.aktivitetslogger.error(
                    "Utbetaling markert som ikke godkjent av saksbehandler (%s)",
                    manuellSaksbehandling.saksbehandler()
                )
                vedtaksperiode.tilstand(manuellSaksbehandling, TilInfotrygd)
            }
        }
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING
        override val timeout: Duration = Duration.ZERO

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: IAktivitetslogger) {
            val utbetalingsreferanse = lagUtbetalingsReferanse(vedtaksperiode)
            vedtaksperiode.utbetalingsreferanse = utbetalingsreferanse

            vedtaksperiode.aktivitetslogger.need(
                NeedType.Utbetaling(
                    vedtaksperiode.transport,
                    utbetalingsreferanse = utbetalingsreferanse,
                    utbetalingslinjer = requireNotNull(vedtaksperiode.utbetalingslinjer).joinForOppdrag(),
                    maksdato = requireNotNull(vedtaksperiode.maksdato),
                    saksbehandler = requireNotNull(vedtaksperiode.godkjentAv)
                ), "Sender til utbetaling"
            )

            val event = PersonObserver.UtbetalingEvent(
                vedtaksperiodeId = vedtaksperiode.id,
                aktørId = vedtaksperiode.aktørId,
                fødselsnummer = vedtaksperiode.fødselsnummer,
                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                utbetalingsreferanse = utbetalingsreferanse,
                utbetalingslinjer = vedtaksperiode.utbetalingslinjer ?: emptyList(),
                opprettet = LocalDate.now()
            )

            vedtaksperiode.aktivitetslogger.info("Satt til utbetaling")

            vedtaksperiode.person.vedtaksperiodeTilUtbetaling(event)
        }

        private fun lagUtbetalingsReferanse(vedtaksperiode: Vedtaksperiode): String {
            if (!vedtaksperiode.arbeidsgiver.harTilstøtendePeriode(vedtaksperiode))
                vedtaksperiode.arbeidsgiver.utbetalingsreferanse++
            return vedtaksperiode.arbeidsgiver.utbetalingsreferanse.toString()
        }

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

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    companion object {
        internal fun sykdomstidslinje(perioder: List<Vedtaksperiode>) = perioder
            .map { it.sykdomshistorikk.sykdomstidslinje() }
            .reduce { concreteSykdomstidslinje, other ->
                concreteSykdomstidslinje.plus(
                    other,
                    ConcreteSykdom©stidslinje.Companion::implisittDag,
                    historiskDagturnering
                )
            }
    }
}
