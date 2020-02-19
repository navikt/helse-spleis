package no.nav.helse.person


import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovType
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.*
import no.nav.helse.person.Arbeidsgiver.GjennoptaBehandling
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.harTilstøtende
import no.nav.helse.tournament.historiskDagturnering
import no.nav.helse.utbetalingstidslinje.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
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
    private val påminnelseThreshold = 10

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
        visitor.visitFørsteFraværsdag(førsteFraværsdag)
        visitor.visitInntektFraInntektsmelding(inntektFraInntektsmelding)
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

    private fun periode() = Periode(
        sykdomshistorikk.sykdomstidslinje().førsteDag(),
        sykdomshistorikk.sykdomstidslinje().sisteDag()
    )

    internal fun håndter(sykmelding: Sykmelding) = overlapperMed(sykmelding).also {
        if (!it) return it
        tilstand.håndter(this, sykmelding)
        sykmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(søknad: Søknad) =
        overlapperMed(søknad).also {
            if (!it) return it
            tilstand.håndter(this, arbeidsgiver, person, søknad)
            søknad.kopierAktiviteterTil(aktivitetslogger)
        }

    internal fun håndter(inntektsmelding: Inntektsmelding) = overlapperMed(inntektsmelding).also {
        if (!it) return it
        tilstand.håndter(this, inntektsmelding)
        inntektsmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(ytelser: Ytelser) {
        if (id.toString() != ytelser.vedtaksperiodeId) return
        tilstand.håndter(person, arbeidsgiver, this, ytelser)
        ytelser.kopierAktiviteterTil(aktivitetslogger)

    }

    internal fun håndter(manuellSaksbehandling: ManuellSaksbehandling) {
        if (id.toString() != manuellSaksbehandling.vedtaksperiodeId) return
        tilstand.håndter(person, arbeidsgiver, this, manuellSaksbehandling)
        manuellSaksbehandling.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        if (id.toString() != vilkårsgrunnlag.vedtaksperiodeId) return
        tilstand.håndter(this, vilkårsgrunnlag)
        vilkårsgrunnlag.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        if (id.toString() != påminnelse.vedtaksperiodeId) return false
        if (!påminnelse.gjelderTilstand(tilstand.type)) return true

        person.vedtaksperiodePåminnet(påminnelse)

        if (påminnelse.antallGangerPåminnet() < påminnelseThreshold) {
            tilstand.håndter(this, påminnelse)
        } else {
            påminnelse.errorOld("Invaliderer perioden fordi den har blitt påminnet $påminnelseThreshold ganger")
            tilstand(påminnelse, TilInfotrygd)
        }

        påminnelse.kopierAktiviteterTil(aktivitetslogger)
        return true
    }

    internal fun invaliderPeriode(hendelse: ArbeidstakerHendelse) {
        hendelse.warnOld("Invaliderer vedtaksperiode: %s", this.id.toString())
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
            hendelse.errorOld("Ikke støttet dag")
            tilstand(hendelse, TilInfotrygd)
        } else {
            tilstand(hendelse, nesteTilstand)
        }
        hendelse.kopierAktiviteterTil(aktivitetslogger)
    }

    val kontekst = object : Vedtaksperiodekontekst {
        override val vedtaksperiodeId = this@Vedtaksperiode.id
        override val orgnummer = this@Vedtaksperiode.organisasjonsnummer
        override val aktørId = this@Vedtaksperiode.aktørId
        override val fødselsnummer = this@Vedtaksperiode.fødselsnummer

        override fun melding() = "person"
    }

    @Deprecated("Skal bruke aktivitetslogger.need()")
    private fun trengerYtelser() {
        person.vedtaksperiodeTrengerLøsning(
            Ytelser.lagBehov(
                vedtaksperiodeId = id,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                utgangspunktForBeregningAvYtelse = sykdomshistorikk.sykdomstidslinje().førsteDag().minusDays(1)
            )
        )
    }

    @Deprecated("Skal bruke aktivitetslogger.need()")
    internal fun trengerVilkårsgrunnlag() {
        val beregningSlutt = YearMonth.from(førsteFraværsdag)
        val beregningStart = beregningSlutt.minusMonths(11)

        person.vedtaksperiodeTrengerLøsning(
            Behov.nyttBehov(
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
        )
    }

    internal fun trengerVilkårsgrunnlag(hendelse: ArbeidstakerHendelse) {
        val beregningSlutt = YearMonth.from(førsteFraværsdag)
        val beregningStart = beregningSlutt.minusMonths(11)
        hendelse.need(BehovType.Inntektsberegning(kontekst, beregningStart, beregningSlutt))
        hendelse.need(BehovType.EgenAnsatt(kontekst))
        hendelse.need(BehovType.Opptjening(kontekst))
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

        fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            sykmelding.errorOld("uventet Sykmelding")
            vedtaksperiode.tilstand(sykmelding, TilInfotrygd)
        }

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            person: Person,
            søknad: Søknad
        ) {
            søknad.errorOld("uventet SendtSøknad")
            vedtaksperiode.tilstand(søknad, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            inntektsmelding.warnOld("uventet Inntektsmelding")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vilkårsgrunnlag.errorOld("uventet vilkårsgrunnlag")
        }

        fun håndter(person: Person, arbeidsgiver: Arbeidsgiver, vedtaksperiode: Vedtaksperiode, ytelser: Ytelser) {
            ytelser.errorOld("uventet sykdom- og inntektshistorikk")
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            manuellSaksbehandling: ManuellSaksbehandling
        ) {
            manuellSaksbehandling.errorOld("uventet manuell saksbehandling")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.tilstand(påminnelse, TilInfotrygd)
        }

        fun håndter(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            gjennoptaBehandling: GjennoptaBehandling
        ) {
            gjennoptaBehandling.infoOld("Tidligere periode ferdig behandlet")
        }

        fun leaving(aktivitetslogger: IAktivitetslogger) {}

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {}
    }

    internal object StartTilstand : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            vedtaksperiode.tilstand(sykmelding, MottattSykmelding) {
                vedtaksperiode.sykdomshistorikk.håndter(sykmelding)
            }
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av sykmelding")
        }

        override val type = START
        override val timeout: Duration = Duration.ofDays(30)
    }

    internal object MottattSykmelding : Vedtaksperiodetilstand {

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            person: Person,
            søknad: Søknad
        ) {
            val nesteTilstand =
                when {
                    !arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode) -> AvventerTidligerePeriodeEllerInntektsmelding
                    else -> arbeidsgiver.tilstøtende(vedtaksperiode)?.also {
                        vedtaksperiode.førsteFraværsdag = it.førsteFraværsdag
                    }?.let { AvventerHistorikk } ?: UndersøkerHistorikk
                }
            vedtaksperiode.håndter(søknad, nesteTilstand)
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerSendtSøknad)
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av inntektsmelding")
        }

        override val type = MOTTATT_NY_SØKNAD

        override val timeout: Duration = Duration.ofDays(30)
    }

    internal object AvventerTidligerePeriodeEllerInntektsmelding : Vedtaksperiodetilstand {
        override val type = AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING
        override val timeout: Duration = Duration.ofDays(30)

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: ArbeidstakerHendelse) {
            aktivitetslogger.infoOld("Avventer ferdigbehandlig av tidligere periode eller inntektsmelding før videre behandling")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerTidligerePeriode)
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av inntektsmelding")
        }

        override fun håndter(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            gjennoptaBehandling: GjennoptaBehandling
        ) {
            if (!arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)) return
            vedtaksperiode.tilstand(
                gjennoptaBehandling,
                arbeidsgiver.tilstøtende(vedtaksperiode)
                    ?.also { vedtaksperiode.førsteFraværsdag = it.førsteFraværsdag }
                    ?.let { AvventerHistorikk }
                    ?: AvventerInntektsmelding
            )
        }
    }

    internal object AvventerTidligerePeriode : Vedtaksperiodetilstand {
        override val type = AVVENTER_TIDLIGERE_PERIODE
        override val timeout: Duration = Duration.ofDays(30)

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: ArbeidstakerHendelse) {
            aktivitetslogger.infoOld("Avventer ferdigbehandlig av tidligere periode før videre behandling")
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

        override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogger: ArbeidstakerHendelse) {
            vedtaksperiode.trengerYtelser()
            aktivitetslogger.infoOld("Forespør sykdoms- og inntektshistorikk")
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
                it.valider { ValiderYtelser(ytelser) }
                it.valider { Overlappende(vedtaksperiode.periode(), ytelser.foreldrepenger()) }
                it.valider {
                    HarInntektshistorikk(
                        arbeidsgiver, vedtaksperiode.sykdomshistorikk.sykdomstidslinje().førsteDag()
                    )
                }
                it.valider { HarArbeidsgivertidslinje(arbeidsgiver) }
                val utbetalingstidslinje =
                    utbetalingstidslinje(arbeidsgiver, vedtaksperiode, sisteHistoriskeSykedag)
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
                    ytelser.infoOld("""Saken oppfyller krav for behandling, settes til "Til godkjenning"""")
                    vedtaksperiode.tilstand(ytelser, AvventerGodkjenning)
                }
            }

        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerYtelser()
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøving)
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av inntektsmelding")
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
            søknad: Søknad
        ) {
            val nesteTilstand =
                if (arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)) AvventerVilkårsprøving
                else AvventerTidligerePeriode
            vedtaksperiode.håndter(søknad, nesteTilstand)
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av søknad")
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
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av inntektsmelding")
        }
    }

    internal object AvventerVilkårsprøving : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING

        override val timeout: Duration = Duration.ofHours(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            emitTrengerVilkårsgrunnlag(vedtaksperiode, hendelse)
            hendelse.infoOld("Forespør vilkårsgrunnlag")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            emitTrengerVilkårsgrunnlag(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            val førsteFraværsdag = vedtaksperiode.førsteFraværsdag
                ?: vedtaksperiode.aktivitetslogger.severeOld("Første fraværsdag mangler i Vilkårsprøving tilstand")
            val inntektFraInntektsmelding = vedtaksperiode.inntektFraInntektsmelding
                ?: vedtaksperiode.aktivitetslogger.severeOld("Epic 3: Trenger mulighet for syketilfeller hvor det ikke er en inntektsmelding (syketilfellet starter i infotrygd)")

            val resultat = vilkårsgrunnlag.måHåndteresManuelt(inntektFraInntektsmelding, førsteFraværsdag)
            vedtaksperiode.dataForVilkårsvurdering = resultat.grunnlagsdata

            if (resultat.måBehandlesManuelt) return vedtaksperiode.tilstand(vilkårsgrunnlag, TilInfotrygd)

            vedtaksperiode.aktivitetslogger.infoOld("Vilkårsgrunnlag verifisert")
            vedtaksperiode.tilstand(vilkårsgrunnlag, AvventerHistorikk)
        }

        private fun emitTrengerVilkårsgrunnlag(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerVilkårsgrunnlag()
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

    }

    internal object AvventerHistorikk : Vedtaksperiodetilstand {

        override val type = AVVENTER_HISTORIKK
        override val timeout: Duration = Duration.ofHours(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerYtelser()
            hendelse.infoOld("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
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
                it.valider { ValiderYtelser(ytelser) }
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
                    ytelser.infoOld("""Saken oppfyller krav for behandling, settes til "Til godkjenning"""")
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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.person.vedtaksperiodeTrengerLøsning(
                ManuellSaksbehandling.lagBehov(
                    vedtaksperiode.id,
                    vedtaksperiode.aktørId,
                    vedtaksperiode.fødselsnummer,
                    vedtaksperiode.organisasjonsnummer
                )
            )
            hendelse.infoOld("Forespør godkjenning fra saksbehandler")
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
                        vedtaksperiode.aktivitetslogger.infoOld(
                            "Utbetaling markert som godkjent av saksbehandler (%s)",
                            it
                        )
                    }
                }
                arbeidsgiver.gjennoptaBehandling(vedtaksperiode)
            } else {
                vedtaksperiode.aktivitetslogger.errorOld(
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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            val utbetalingsreferanse = lagUtbetalingsReferanse(vedtaksperiode)
            vedtaksperiode.utbetalingsreferanse = utbetalingsreferanse

            vedtaksperiode.person.vedtaksperiodeTrengerLøsning(
                Behov.nyttBehov(
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

            val event = PersonObserver.UtbetalingEvent(
                vedtaksperiodeId = vedtaksperiode.id,
                aktørId = vedtaksperiode.aktørId,
                fødselsnummer = vedtaksperiode.fødselsnummer,
                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                utbetalingsreferanse = utbetalingsreferanse,
                utbetalingslinjer = vedtaksperiode.utbetalingslinjer ?: emptyList(),
                opprettet = LocalDate.now()
            )

            vedtaksperiode.aktivitetslogger.infoOld("Satt til utbetaling")

            vedtaksperiode.person.vedtaksperiodeTilUtbetaling(event)
        }

        private fun lagUtbetalingsReferanse(vedtaksperiode: Vedtaksperiode): String {
            if (!vedtaksperiode.arbeidsgiver.harTilstøtendePeriode(vedtaksperiode))
                vedtaksperiode.arbeidsgiver.utbetalingsreferanse++
            return vedtaksperiode.arbeidsgiver.utbetalingsreferanse.toString()
        }
    }

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD
        override val timeout: Duration = Duration.ZERO
        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.warnOld("Sykdom for denne personen kan ikke behandles automatisk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    companion object {
        internal fun tilstøtendePeriode(other: Vedtaksperiode, perioder: List<Vedtaksperiode>) = perioder
            .filter { it.harTilstøtende(other) }
            .sortedBy { it.førsteFraværsdag }
            .firstOrNull { it.førsteFraværsdag != null }

        internal fun sykdomstidslinje(perioder: List<Vedtaksperiode>) = perioder
            .map { it.sykdomshistorikk.sykdomstidslinje() }
            .reduce { concreteSykdomstidslinje, other ->
                concreteSykdomstidslinje.plus(other, ::ImplisittDag, historiskDagturnering)
            }
    }
}
