package no.nav.helse.person


import no.nav.helse.behov.BehovType
import no.nav.helse.hendelser.*
import no.nav.helse.person.Arbeidsgiver.GjenopptaBehandling
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
import java.time.LocalDateTime
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
    private var forbrukteSykedager: Int?,
    private var utbetalingslinjer: List<Utbetalingslinje>?,
    private var godkjentAv: String?,
    private var godkjenttidspunkt: LocalDateTime?,
    private var utbetalingsreferanse: String,
    private var førsteFraværsdag: LocalDate?,
    private var inntektFraInntektsmelding: Double?,
    private var dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?,
    private val sykdomshistorikk: Sykdomshistorikk
) : Aktivitetskontekst {
    private val påminnelseThreshold = 10

    internal constructor(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        tilstand: Vedtaksperiodetilstand = StartTilstand
    ) : this(
        person = person,
        arbeidsgiver = arbeidsgiver,
        id = id,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        tilstand = tilstand,
        maksdato = null,
        forbrukteSykedager = null,
        utbetalingslinjer = null,
        godkjentAv = null,
        godkjenttidspunkt = null,
        utbetalingsreferanse = genererUtbetalingsreferanse(id),
        førsteFraværsdag = null,
        inntektFraInntektsmelding = null,
        dataForVilkårsvurdering = null,
        sykdomshistorikk = Sykdomshistorikk()
    )

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        visitor.preVisitVedtaksperiode(this, id)
        visitor.visitMaksdato(maksdato)
        visitor.visitforbrukteSykedager(forbrukteSykedager)
        visitor.visitGodkjentAv(godkjentAv)
        visitor.visitFørsteFraværsdag(førsteFraværsdag)
        visitor.visitUtbetalingsreferanse(utbetalingsreferanse)
        visitor.visitInntektFraInntektsmelding(inntektFraInntektsmelding)
        visitor.visitDataForVilkårsvurdering(dataForVilkårsvurdering)
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

    internal fun periode() = Periode(
        sykdomshistorikk.sykdomstidslinje().førsteDag(),
        sykdomshistorikk.sykdomstidslinje().sisteDag()
    )

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to id.toString()))
    }

    internal fun håndter(sykmelding: Sykmelding) = overlapperMed(sykmelding).also {
        if (!it) return it
        sykmelding.kontekst(this)
        tilstand.håndter(this, sykmelding)
    }

    internal fun håndter(søknad: Søknad) =
        overlapperMed(søknad).also {
            if (!it) return it
            søknad.kontekst(this)
            tilstand.håndter(this, arbeidsgiver, person, søknad)
        }

    internal fun håndter(inntektsmelding: Inntektsmelding) = overlapperMed(inntektsmelding).also {
        if (!it) return it
        inntektsmelding.kontekst(this)
        tilstand.håndter(this, inntektsmelding)
    }

    internal fun håndter(ytelser: Ytelser) {
        if (id.toString() != ytelser.vedtaksperiodeId) return
        ytelser.kontekst(this)
        tilstand.håndter(person, arbeidsgiver, this, ytelser)
    }

    internal fun håndter(manuellSaksbehandling: ManuellSaksbehandling) {
        if (id.toString() != manuellSaksbehandling.vedtaksperiodeId()) return
        manuellSaksbehandling.kontekst(this)
        tilstand.håndter(person, arbeidsgiver, this, manuellSaksbehandling)
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        if (id.toString() != vilkårsgrunnlag.vedtaksperiodeId) return
        vilkårsgrunnlag.kontekst(this)
        tilstand.håndter(this, vilkårsgrunnlag)
    }

    internal fun håndter(utbetaling: Utbetaling) {
        if (id.toString() != utbetaling.vedtaksperiodeId) return
        utbetaling.kontekst(this)
        tilstand.håndter(this, utbetaling)
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        if (id.toString() != påminnelse.vedtaksperiodeId) return false
        if (!påminnelse.gjelderTilstand(tilstand.type)) return true

        påminnelse.kontekst(this)
        person.vedtaksperiodePåminnet(påminnelse)

        if (påminnelse.antallGangerPåminnet() < påminnelseThreshold) {
            tilstand.håndter(this, påminnelse)
        } else {
            påminnelse.error("Invaliderer perioden fordi den har blitt påminnet %d ganger", påminnelseThreshold)
            tilstand(påminnelse, TilInfotrygd)
        }
        return true
    }

    internal fun invaliderPeriode(hendelse: ArbeidstakerHendelse) {
        hendelse.info("Invaliderer vedtaksperiode: %s", this.id.toString())
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

        if (!sykdomshistorikk.sykdomstidslinje().valider(hendelse)) {
            tilstand(hendelse, TilInfotrygd)
        } else {
            tilstand(hendelse, nesteTilstand)
        }
    }

    private val kontekst = Vedtaksperiodekontekst(aktørId, fødselsnummer, organisasjonsnummer, id)

    private fun trengerYtelser(hendelse: ArbeidstakerHendelse) {
        hendelse.need(BehovType.Sykepengehistorikk(
            kontekst,
            sykdomshistorikk.sykdomstidslinje().førsteDag().minusDays(1)
        ))
        hendelse.need(BehovType.Foreldrepenger(kontekst))
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
            aktivitetslogg = tidslinjeEvent.aktivitetslogg,
            timeout = varighet
        )

        person.vedtaksperiodeEndret(event)
    }

    internal fun harTilstøtende(other: Vedtaksperiode) =
        this.sykdomshistorikk.sykdomstidslinje().harTilstøtende(other.sykdomshistorikk.sykdomstidslinje())

    internal fun erFerdigBehandlet(other: Vedtaksperiode) =
        (this.periode().start >= other.periode().start) || this.tilstand.type in listOf(TIL_UTBETALING, TIL_INFOTRYGD, UTBETALT, UTBETALING_FEILET)

    internal fun håndter(arbeidsgiver: Arbeidsgiver, other: Vedtaksperiode, hendelse: GjenopptaBehandling) {
        if (this.periode().start > other.periode().start) tilstand.håndter(arbeidsgiver, this, hendelse)
    }

    // Gang of four State pattern
    internal interface Vedtaksperiodetilstand {
        val type: TilstandType

        val timeout: Duration

        fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            sykmelding.error("Forventet ikke sykmelding i %s", type.name)
            vedtaksperiode.tilstand(sykmelding, TilInfotrygd)
        }

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            person: Person,
            søknad: Søknad
        ) {
            søknad.error("Forventet ikke søknad i %s", type.name)
            vedtaksperiode.tilstand(søknad, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            inntektsmelding.warn("Forventet ikke inntektsmelding i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vilkårsgrunnlag.error("Forventet ikke vilkårsgrunnlag i %s", type.name)
        }

        fun håndter(person: Person, arbeidsgiver: Arbeidsgiver, vedtaksperiode: Vedtaksperiode, ytelser: Ytelser) {
            ytelser.error("Forventet ikke ytelsehistorikk i %s", type.name)
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            manuellSaksbehandling: ManuellSaksbehandling
        ) {
            manuellSaksbehandling.error("Forventet ikke svar på manuell behandling i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.tilstand(påminnelse, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: Utbetaling) {
            utbetaling.error("Forventet ikke utbetaling i %s", type.name)
        }

        fun håndter(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            gjenopptaBehandling.hendelse.info("Tidligere periode ferdig behandlet")
        }

        fun leaving(aktivitetslogg: IAktivitetslogg) {}

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {}
    }

    internal object StartTilstand : Vedtaksperiodetilstand {

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            vedtaksperiode.tilstand(sykmelding, MottattSykmelding) {
                vedtaksperiode.sykdomshistorikk.håndter(sykmelding)
            }
            sykmelding.info("Fullført behandling av sykmelding")
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
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerSøknad)
            inntektsmelding.info("Fullført behandling av inntektsmelding")
        }

        override val type = MOTTATT_SYKMELDING

        override val timeout: Duration = Duration.ofDays(30)
    }

    internal object AvventerTidligerePeriodeEllerInntektsmelding : Vedtaksperiodetilstand {
        override val type = AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING
        override val timeout: Duration = Duration.ofDays(30)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.info("Avventer ferdigbehandling av tidligere periode eller inntektsmelding før videre behandling")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerTidligerePeriode)
            inntektsmelding.info("Fullført behandling av inntektsmelding")
        }

        override fun håndter(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            if (!arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)) return
            vedtaksperiode.tilstand(
                gjenopptaBehandling.hendelse,
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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.info("Avventer ferdigbehandlig av tidligere periode før videre behandling")
        }

        override fun håndter(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            if (!arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)) return
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerVilkårsprøving)
        }
    }

    internal object UndersøkerHistorikk : Vedtaksperiodetilstand {

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerYtelser(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser
        ) {
            val sisteHistoriskeSykedag = ytelser.sykepengehistorikk().sisteFraværsdag()
            if (sisteHistoriskeSykedag != null && sisteHistoriskeSykedag.harTilstøtende(vedtaksperiode.periode().start))
                return vedtaksperiode.tilstand(ytelser, TilInfotrygd)

            vedtaksperiode.tilstand(ytelser, AvventerInntektsmelding)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerYtelser(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøving)
            inntektsmelding.info("Fullført behandling av inntektsmelding")
        }

        override val type = UNDERSØKER_HISTORIKK

        override val timeout: Duration = Duration.ofHours(1)
    }

    internal object AvventerSøknad : Vedtaksperiodetilstand {

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
            søknad.info("Fullført behandling av søknad")
        }

        override val type = AVVENTER_SØKNAD

        override val timeout: Duration = Duration.ofDays(30)
    }

    internal object AvventerInntektsmelding : Vedtaksperiodetilstand {

        override val type = AVVENTER_INNTEKTSMELDING

        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøving)
            inntektsmelding.info("Fullført behandling av inntektsmelding")
        }
    }

    internal object AvventerVilkårsprøving : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING

        override val timeout: Duration = Duration.ofHours(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            emitTrengerVilkårsgrunnlag(vedtaksperiode, hendelse)
            hendelse.info("Forespør vilkårsgrunnlag")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            emitTrengerVilkårsgrunnlag(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            val førsteFraværsdag = vedtaksperiode.førsteFraværsdag
                ?: vilkårsgrunnlag.severe("Første fraværsdag mangler i Vilkårsprøving tilstand")
            val inntektFraInntektsmelding = vedtaksperiode.inntektFraInntektsmelding
                ?: vilkårsgrunnlag.severe("Epic 3: Trenger mulighet for syketilfeller hvor det ikke er en inntektsmelding (syketilfellet starter i infotrygd)")

            val resultat = vilkårsgrunnlag.måHåndteresManuelt(inntektFraInntektsmelding, førsteFraværsdag)
            vedtaksperiode.dataForVilkårsvurdering = resultat.grunnlagsdata

            if (resultat.måBehandlesManuelt) return vedtaksperiode.tilstand(vilkårsgrunnlag, TilInfotrygd)

            vilkårsgrunnlag.info("Vilkårsgrunnlag verifisert")
            vedtaksperiode.tilstand(vilkårsgrunnlag, AvventerHistorikk)
        }

        private fun emitTrengerVilkårsgrunnlag(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

    }

    internal object AvventerHistorikk : Vedtaksperiodetilstand {

        override val type = AVVENTER_HISTORIKK
        override val timeout: Duration = Duration.ofHours(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerYtelser(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerYtelser(påminnelse)
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
                    vedtaksperiode.forbrukteSykedager = engineForTimeline?.forbrukteSykedager()
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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            val utbetalingsreferanse = lagUtbetalingsReferanse(vedtaksperiode)
            vedtaksperiode.utbetalingsreferanse = utbetalingsreferanse

            hendelse.need(BehovType.Godkjenning(vedtaksperiode.kontekst))
            hendelse.info("Forespør godkjenning fra saksbehandler")
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            manuellSaksbehandling: ManuellSaksbehandling
        ) {
            if (manuellSaksbehandling.utbetalingGodkjent()) {
                vedtaksperiode.tilstand(manuellSaksbehandling, TilUtbetaling) {
                    vedtaksperiode.godkjenttidspunkt = manuellSaksbehandling.godkjenttidspunkt()
                    vedtaksperiode.godkjentAv = manuellSaksbehandling.saksbehandler().also {
                        manuellSaksbehandling.info("Utbetaling markert som godkjent av saksbehandler $it ${vedtaksperiode.godkjenttidspunkt}"
                        )
                    }
                }
                arbeidsgiver.gjenopptaBehandling(vedtaksperiode, manuellSaksbehandling)
            } else {
                manuellSaksbehandling.error(
                    "Utbetaling markert som ikke godkjent av saksbehandler (%s)",
                    manuellSaksbehandling.saksbehandler()
                )
                vedtaksperiode.tilstand(manuellSaksbehandling, TilInfotrygd)
            }
        }

        private fun lagUtbetalingsReferanse(vedtaksperiode: Vedtaksperiode) =
            vedtaksperiode.arbeidsgiver.tilstøtende(vedtaksperiode)?.utbetalingsreferanse ?: genererUtbetalingsreferanse(vedtaksperiode.id)
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING
        override val timeout: Duration = Duration.ZERO

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.need(BehovType.Utbetaling(
                context = vedtaksperiode.kontekst,
                utbetalingsreferanse = vedtaksperiode.utbetalingsreferanse,
                utbetalingslinjer = requireNotNull(vedtaksperiode.utbetalingslinjer).joinForOppdrag(),
                maksdato = requireNotNull(vedtaksperiode.maksdato),
                saksbehandler = requireNotNull(vedtaksperiode.godkjentAv)
            ))
            val event = PersonObserver.UtbetalingEvent(
                vedtaksperiodeId = vedtaksperiode.id,
                aktørId = vedtaksperiode.aktørId,
                fødselsnummer = vedtaksperiode.fødselsnummer,
                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                utbetalingsreferanse = vedtaksperiode.utbetalingsreferanse,
                utbetalingslinjer = requireNotNull(vedtaksperiode.utbetalingslinjer),
                opprettet = LocalDate.now()
            )

            hendelse.info("Satt til utbetaling")

            vedtaksperiode.person.vedtaksperiodeTilUtbetaling(event)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: Utbetaling) {
            if (utbetaling.isOK()) {
                vedtaksperiode.tilstand(utbetaling, Utbetalt) {
                    utbetaling.info("OK fra Oppdragssystemet")
                }
            } else {
                vedtaksperiode.tilstand(utbetaling, UtbetalingFeilet) {
                    utbetaling.error("Feilmelding fra Oppdragssystemet: ${utbetaling.melding}")
                }
            }
        }
    }

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD
        override val timeout: Duration = Duration.ZERO
        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.info("Sykdom for denne personen kan ikke behandles automatisk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object Utbetalt : Vedtaksperiodetilstand {
        override val type = UTBETALT
        override val timeout: Duration = Duration.ZERO

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.info("Sendt til Oppdragssystemet for utbetaling")

            val event = PersonObserver.UtbetaltEvent(
                vedtaksperiodeId = vedtaksperiode.id,
                aktørId = vedtaksperiode.aktørId,
                fødselsnummer = vedtaksperiode.fødselsnummer,
                utbetalingsreferanse = vedtaksperiode.utbetalingsreferanse,
                utbetalingslinjer = requireNotNull(vedtaksperiode.utbetalingslinjer),
                forbrukteSykedager = requireNotNull(vedtaksperiode.forbrukteSykedager),
                opprettet = LocalDate.now()
            )
            vedtaksperiode.person.vedtaksperiodeUtbetalt(event)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object UtbetalingFeilet : Vedtaksperiodetilstand {
        override val type = UTBETALING_FEILET
        override val timeout: Duration = Duration.ZERO

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.error("Feilrespons fra oppdrag")
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
