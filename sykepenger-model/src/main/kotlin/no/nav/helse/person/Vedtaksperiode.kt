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
) : Aktivitetskontekst {
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

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", "Arbeidsgiver ${organisasjonsnummer}")
    }

    internal fun håndter(sykmelding: Sykmelding) = overlapperMed(sykmelding).also {
        if (!it) return it
        sykmelding.kontekst(this)
        tilstand.håndter(this, sykmelding)
        sykmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(søknad: Søknad) =
        overlapperMed(søknad).also {
            if (!it) return it
            søknad.kontekst(this)
            tilstand.håndter(this, arbeidsgiver, person, søknad)
            søknad.kopierAktiviteterTil(aktivitetslogger)
        }

    internal fun håndter(inntektsmelding: Inntektsmelding) = overlapperMed(inntektsmelding).also {
        if (!it) return it
        inntektsmelding.kontekst(this)
        tilstand.håndter(this, inntektsmelding)
        inntektsmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(ytelser: Ytelser) {
        if (id.toString() != ytelser.vedtaksperiodeId) return
        ytelser.kontekst(this)
        tilstand.håndter(person, arbeidsgiver, this, ytelser)
        ytelser.kopierAktiviteterTil(aktivitetslogger)

    }

    internal fun håndter(manuellSaksbehandling: ManuellSaksbehandling) {
        if (id.toString() != manuellSaksbehandling.vedtaksperiodeId) return
        manuellSaksbehandling.kontekst(this)
        tilstand.håndter(person, arbeidsgiver, this, manuellSaksbehandling)
        manuellSaksbehandling.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        if (id.toString() != vilkårsgrunnlag.vedtaksperiodeId) return
        vilkårsgrunnlag.kontekst(this)
        tilstand.håndter(this, vilkårsgrunnlag)
        vilkårsgrunnlag.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(utbetaling: Utbetaling) {
        if (id.toString() != utbetaling.vedtaksperiodeId) return
        utbetaling.kontekst(this)
        tilstand.håndter(this, utbetaling)
        utbetaling.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        if (id.toString() != påminnelse.vedtaksperiodeId) return false
        if (!påminnelse.gjelderTilstand(tilstand.type)) return true

        påminnelse.kontekst(this)
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
            hendelse.errorOld("Ikke støttet dag")
            hendelse.error("Ikke støttet dag")
            tilstand(hendelse, TilInfotrygd)
        } else {
            tilstand(hendelse, nesteTilstand)
        }
        hendelse.kopierAktiviteterTil(aktivitetslogger)
    }

    val kontekst = object : Vedtaksperiodekontekst {
        override val vedtaksperiodeId = this@Vedtaksperiode.id
        override val organisasjonsnummer = this@Vedtaksperiode.organisasjonsnummer
        override val aktørId = this@Vedtaksperiode.aktørId
        override val fødselsnummer = this@Vedtaksperiode.fødselsnummer

        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            return SpesifikkKontekst("Vedtaksperiode", "Vedtaksperiode: ${vedtaksperiodeId}")
        }

        override val kontekstId = UUID.randomUUID()
    }

    private fun trengerYtelser(hendelse: ArbeidstakerHendelse) {
        hendelse.need(kontekst.kontekstId,
            BehovType.Sykepengehistorikk(
                kontekst,
                sykdomshistorikk.sykdomstidslinje().førsteDag().minusDays(1)
            )
        )
        hendelse.need(kontekst.kontekstId, BehovType.Foreldrepenger(kontekst))
    }

    internal fun trengerVilkårsgrunnlag(hendelse: ArbeidstakerHendelse) {
        val beregningSlutt = YearMonth.from(førsteFraværsdag)
        val beregningStart = beregningSlutt.minusMonths(11)
        hendelse.need(kontekst.kontekstId, BehovType.Inntektsberegning(kontekst, beregningStart, beregningSlutt))
        hendelse.need(kontekst.kontekstId, BehovType.EgenAnsatt(kontekst))
        hendelse.need(kontekst.kontekstId, BehovType.Opptjening(kontekst))
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
            aktivitetslogger = tidslinjeEvent.aktivitetslogger,
            aktivitetslogg = tidslinjeEvent.aktivitetslogg,
            timeout = varighet
        )

        person.vedtaksperiodeEndret(event)
    }

    internal fun harTilstøtende(other: Vedtaksperiode) =
        this.sykdomshistorikk.sykdomstidslinje().harTilstøtende(other.sykdomshistorikk.sykdomstidslinje())

    internal fun erFerdigBehandlet(other: Vedtaksperiode) =
        (this.periode().start >= other.periode().start) || this.tilstand.type in listOf(TIL_UTBETALING, TIL_INFOTRYGD)

    internal fun håndter(arbeidsgiver: Arbeidsgiver, other: Vedtaksperiode, hendelse: GjenopptaBehandling) {
        if (this.periode().start > other.periode().start) tilstand.håndter(arbeidsgiver, this, hendelse)
    }

    // Gang of four State pattern
    internal interface Vedtaksperiodetilstand {
        val type: TilstandType

        val timeout: Duration

        fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            sykmelding.errorOld("Forventet ikke sykmelding i %s", type.name)
            sykmelding.error("Forventet ikke sykmelding i %s", type.name)
            vedtaksperiode.tilstand(sykmelding, TilInfotrygd)
        }

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            person: Person,
            søknad: Søknad
        ) {
            søknad.errorOld("Forventet ikke søknad i %s", type.name)
            søknad.error("Forventet ikke søknad i %s", type.name)
            vedtaksperiode.tilstand(søknad, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            inntektsmelding.warnOld("Forventet ikke inntektsmelding i %s", type.name)
            inntektsmelding.warn("Forventet ikke inntektsmelding i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vilkårsgrunnlag.errorOld("Forventet ikke vilkårsgrunnlag i %s", type.name)
            vilkårsgrunnlag.error("Forventet ikke vilkårsgrunnlag i %s", type.name)
        }

        fun håndter(person: Person, arbeidsgiver: Arbeidsgiver, vedtaksperiode: Vedtaksperiode, ytelser: Ytelser) {
            ytelser.errorOld("Forventet ikke ytelsehistorikk i %s", type.name)
            ytelser.error("Forventet ikke ytelsehistorikk i %s", type.name)
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            manuellSaksbehandling: ManuellSaksbehandling
        ) {
            manuellSaksbehandling.errorOld("Forventet ikke svar på manuell behandling i %s", type.name)
            manuellSaksbehandling.error("Forventet ikke svar på manuell behandling i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.tilstand(påminnelse, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: Utbetaling) {
            utbetaling.errorOld("Forventet ikke utbetaling i %s", type.name)
            utbetaling.error("Forventet ikke utbetaling i %s", type.name)
        }

        fun håndter(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            gjenopptaBehandling.hendelse.infoOld("Tidligere periode ferdig behandlet")
            gjenopptaBehandling.hendelse.info("Tidligere periode ferdig behandlet")
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
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av søknad")
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerSøknad)
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av inntektsmelding")
            inntektsmelding.info("Fullført behandling av inntektsmelding")
        }

        override val type = MOTTATT_SYKMELDING

        override val timeout: Duration = Duration.ofDays(30)
    }

    internal object AvventerTidligerePeriodeEllerInntektsmelding : Vedtaksperiodetilstand {
        override val type = AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING
        override val timeout: Duration = Duration.ofDays(30)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.infoOld("Avventer ferdigbehandlig av tidligere periode eller inntektsmelding før videre behandling")
            hendelse.info("Avventer ferdigbehandlig av tidligere periode eller inntektsmelding før videre behandling")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerTidligerePeriode)
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av inntektsmelding")
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
            hendelse.infoOld("Avventer ferdigbehandlig av tidligere periode før videre behandling")
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
            hendelse.infoOld("Forespør sykdoms- og inntektshistorikk")
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
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
                    ytelser.info("""Saken oppfyller krav for behandling, settes til "Til godkjenning"""")
                    vedtaksperiode.tilstand(ytelser, AvventerGodkjenning)
                }
            }

        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerYtelser(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.førsteFraværsdag = inntektsmelding.førsteFraværsdag
            vedtaksperiode.inntektFraInntektsmelding = inntektsmelding.beregnetInntekt
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøving)
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av inntektsmelding")
            inntektsmelding.info("Fullført behandling av inntektsmelding")
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
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av søknad")
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
            vedtaksperiode.aktivitetslogger.infoOld("Fullført behandling av inntektsmelding")
            inntektsmelding.info("Fullført behandling av inntektsmelding")
        }
    }

    internal object AvventerVilkårsprøving : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING

        override val timeout: Duration = Duration.ofHours(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            emitTrengerVilkårsgrunnlag(vedtaksperiode, hendelse)
            hendelse.infoOld("Forespør vilkårsgrunnlag")
            hendelse.info("Forespør vilkårsgrunnlag")
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
            hendelse.infoOld("Forespør sykdoms- og inntektshistorikk")
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
                    vedtaksperiode.utbetalingslinjer = engineForLine?.utbetalingslinjer()
                    ytelser.infoOld("""Saken oppfyller krav for behandling, settes til "Til godkjenning"""")
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

            hendelse.need(vedtaksperiode.kontekst.kontekstId, BehovType.Godkjenning(vedtaksperiode.kontekst))
            hendelse.infoOld("Forespør godkjenning fra saksbehandler")
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
                    vedtaksperiode.godkjentAv = manuellSaksbehandling.saksbehandler().also {
                        vedtaksperiode.aktivitetslogger.infoOld(
                            "Utbetaling markert som godkjent av saksbehandler (%s)",
                            it
                        )
                        manuellSaksbehandling.info("Utbetaling markert som godkjent av saksbehandler (%s)", it)
                    }
                }
                arbeidsgiver.gjenopptaBehandling(vedtaksperiode, manuellSaksbehandling)
            } else {
                vedtaksperiode.aktivitetslogger.errorOld(
                    "Utbetaling markert som ikke godkjent av saksbehandler (%s)",
                    manuellSaksbehandling.saksbehandler()
                )
                manuellSaksbehandling.error(
                    "Utbetaling markert som ikke godkjent av saksbehandler (%s)",
                    manuellSaksbehandling.saksbehandler()
                )
                vedtaksperiode.tilstand(manuellSaksbehandling, TilInfotrygd)
            }
        }

        // TODO: Gjør private når TilUtbetaling ikke lenger trenger den
        internal fun lagUtbetalingsReferanse(vedtaksperiode: Vedtaksperiode) =
            vedtaksperiode.arbeidsgiver.tilstøtende(vedtaksperiode)?.utbetalingsreferanse
                ?: genererUtbetalingsreferanse(vedtaksperiode.id)
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING
        override val timeout: Duration = Duration.ZERO

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            if (vedtaksperiode.utbetalingsreferanse == null) {
                // TODO: Kun for overgangsperiode nå som utbetalingsreferansegenerering er flyttet til AvventerGodkjenning
                // Fjern dette når det ikke lenger finnes noen i AvventerGodkjenning-state uten utbetref
               vedtaksperiode.utbetalingsreferanse = AvventerGodkjenning.lagUtbetalingsReferanse(vedtaksperiode)
            }
            val utbetalingsreferanse = requireNotNull(vedtaksperiode.utbetalingsreferanse)

            hendelse.need(vedtaksperiode.kontekst.kontekstId,
                BehovType.Utbetaling(
                    context = vedtaksperiode.kontekst,
                    utbetalingsreferanse = utbetalingsreferanse,
                    utbetalingslinjer = requireNotNull(vedtaksperiode.utbetalingslinjer).joinForOppdrag(),
                    maksdato = requireNotNull(vedtaksperiode.maksdato),
                    saksbehandler = requireNotNull(vedtaksperiode.godkjentAv)
                )
            )
            val event = PersonObserver.UtbetalingEvent(
                vedtaksperiodeId = vedtaksperiode.id,
                aktørId = vedtaksperiode.aktørId,
                fødselsnummer = vedtaksperiode.fødselsnummer,
                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                utbetalingsreferanse = utbetalingsreferanse,
                utbetalingslinjer = requireNotNull(vedtaksperiode.utbetalingslinjer),
                opprettet = LocalDate.now()
            )

            vedtaksperiode.aktivitetslogger.infoOld("Satt til utbetaling")
            hendelse.info("Satt til utbetaling")

            vedtaksperiode.person.vedtaksperiodeTilUtbetaling(event)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: Utbetaling) {
            if(utbetaling.isOK()) {
                vedtaksperiode.tilstand(utbetaling, Utbetalt) {
                    vedtaksperiode.aktivitetslogger.infoOld("OK fra Oppdragssystemet")
                    utbetaling.info("OK fra Oppdragssystemet")
                }
            } else {
                vedtaksperiode.tilstand(utbetaling, UtbetalingFeilet) {
                    vedtaksperiode.aktivitetslogger.warnOld("Feilmelding fra Oppdragssystemet: ${utbetaling.melding}")
                    utbetaling.warn("Feilmelding fra Oppdragssystemet: ${utbetaling.melding}")
                }
            }
        }
    }

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD
        override val timeout: Duration = Duration.ZERO
        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.warnOld("Sykdom for denne personen kan ikke behandles automatisk")
            hendelse.warn("Sykdom for denne personen kan ikke behandles automatisk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object Utbetalt : Vedtaksperiodetilstand {
        override val type = UTBETALT
        override val timeout: Duration = Duration.ZERO

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.infoOld("Sendt til Oppdragssystemet for utbetaling")
            hendelse.info("Sendt til Oppdragssystemet for utbetaling")

            val event = PersonObserver.UtbetaltEvent(
                vedtaksperiodeId = vedtaksperiode.id,
                aktørId = vedtaksperiode.aktørId,
                fødselsnummer = vedtaksperiode.fødselsnummer,
                utbetalingsreferanse = vedtaksperiode.utbetalingsreferanse ?: hendelse.severeOld("Utbetalt vedtaksperiode uten betalingsreferanse"),
                utbetalingslinjer = requireNotNull(vedtaksperiode.utbetalingslinjer),
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
            hendelse.severeOld("Feilrespons fra oppdrag")
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
