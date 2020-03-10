package no.nav.helse.person


import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.egenAnsatt
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.godkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.inntektsberegning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opptjening
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.sykepengehistorikk
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetaling
import no.nav.helse.person.Arbeidsgiver.GjenopptaBehandling
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.harTilstøtende
import no.nav.helse.sykdomstidslinje.join
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
        tilstand: Vedtaksperiodetilstand = Start
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
        dataForVilkårsvurdering = null,
        sykdomshistorikk = Sykdomshistorikk()
    )

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        visitor.preVisitVedtaksperiode(this, id)
        visitor.visitMaksdato(maksdato)
        visitor.visitForbrukteSykedager(forbrukteSykedager)
        visitor.visitGodkjentAv(godkjentAv)
        visitor.visitFørsteFraværsdag(førsteFraværsdag)
        visitor.visitUtbetalingsreferanse(utbetalingsreferanse)
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
            tilstand.håndter(this, søknad)
        }

    internal fun håndter(inntektsmelding: Inntektsmelding) = overlapperMed(inntektsmelding).also {
        if (!it) return it
        inntektsmelding.kontekst(this)
        tilstand.håndter(this, arbeidsgiver, inntektsmelding)
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
        sykdomshistorikk.isEmpty() || this.sykdomshistorikk.sykdomstidslinje()
            .overlapperMed(hendelse.sykdomstidslinje())

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

    private fun håndter(hendelse: Inntektsmelding, nesteTilstand: Vedtaksperiodetilstand) {
        arbeidsgiver.addInntektsmelding(hendelse)
        sykdomshistorikk.håndter(hendelse)
        førsteFraværsdag = hendelse.førsteFraværsdag
        if (hendelse.førsteFraværsdag > sykdomshistorikk.sykdomstidslinje().sisteDag())
            hendelse.error("Inntektsmelding har oppgitt første fraværsdag etter tidslinjen til perioden")
        if (hendelse.førsteFraværsdag != sykdomshistorikk.sykdomstidslinje().førsteFraværsdag())
            hendelse.warn("Inntektsmelding har oppgitt en annen første fraværsdag")

        if (hendelse.hasErrors()) return tilstand(hendelse, TilInfotrygd)
        tilstand(hendelse, nesteTilstand)
        hendelse.info("Fullført behandling av inntektsmelding")
    }

    private fun håndter(hendelse: SykdomstidslinjeHendelse, nesteTilstand: Vedtaksperiodetilstand) {
        håndter(hendelse) { nesteTilstand }
    }

    private fun håndter(hendelse: SykdomstidslinjeHendelse, nesteTilstand: () -> Vedtaksperiodetilstand) {
        sykdomshistorikk.håndter(hendelse)
        if (hendelse.hasErrors()) return tilstand(hendelse, TilInfotrygd)
        tilstand(hendelse, nesteTilstand())
    }

    private fun trengerYtelser(hendelse: ArbeidstakerHendelse) {
        sykepengehistorikk(hendelse, sykdomshistorikk.sykdomstidslinje().førsteDag().minusDays(1))
        foreldrepenger(hendelse)
    }

    internal fun trengerVilkårsgrunnlag(hendelse: ArbeidstakerHendelse) {
        val beregningSlutt = YearMonth.from(førsteFraværsdag)
        inntektsberegning(hendelse, beregningSlutt.minusMonths(11), beregningSlutt)
        egenAnsatt(hendelse)
        opptjening(hendelse)
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

    internal fun erFerdigBehandlet(other: Vedtaksperiode, forlengelse: Boolean): Boolean {
        if (this.periode().start >= other.periode().start) return true
        if(this.tilstand.type == TIL_INFOTRYGD && forlengelse) return false
        return this.tilstand.type in listOf(
            TIL_INFOTRYGD,
            AVSLUTTET
        )
    }

    internal fun håndter(other: Vedtaksperiode, hendelse: GjenopptaBehandling) {
        if (this.periode().start > other.periode().start) {
            hendelse.hendelse.kontekst(this)
            tilstand.håndter(this, hendelse)
        }
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
            søknad: Søknad
        ) {
            søknad.error("Forventet ikke søknad i %s", type.name)
            vedtaksperiode.tilstand(søknad, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, arbeidsgiver: Arbeidsgiver, inntektsmelding: Inntektsmelding) {
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
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            gjenopptaBehandling.hendelse.info("Tidligere periode ferdig behandlet")
        }

        fun leaving(aktivitetslogg: IAktivitetslogg) {}

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {}
    }

    internal object Start : Vedtaksperiodetilstand {
        override val type = START
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            vedtaksperiode.håndter(sykmelding) { nesteTilstand(vedtaksperiode, sykmelding) }
            sykmelding.info("Fullført behandling av sykmelding")
        }

        private fun nesteTilstand(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding): Vedtaksperiodetilstand {
            val forlengelse = vedtaksperiode.arbeidsgiver.tilstøtende(vedtaksperiode) != null
            val ferdig = vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode, forlengelse)

            return when {
                forlengelse && ferdig -> MottattSykmeldingFerdigForlengelse
                forlengelse && !ferdig -> MottattSykmeldingUferdigForlengelse
                !forlengelse && ferdig -> MottattSykmeldingFerdigGap
                !forlengelse && !ferdig -> MottattSykmeldingUferdigGap
                else -> sykmelding.severe("Klarer ikke bestemme hvilken sykmeldingmottattilstand vi skal til")
            }
        }
    }

    internal object MottattSykmeldingFerdigForlengelse : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_FERDIG_FORLENGELSE
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerHistorikk)
            søknad.info("Fullført behandling av søknad")
        }
    }

    internal object MottattSykmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerInntektsmeldingUferdigForlengelse)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding, AvventerSøknadUferdigForlengelse)
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, MottattSykmeldingFerdigForlengelse)
        }
    }

    internal object MottattSykmeldingFerdigGap : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_FERDIG_GAP
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding, AvventerSøknadFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerGap)
            søknad.info("Fullført behandling av søknad")
        }
    }

    internal object MottattSykmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_UFERDIG_GAP
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, MottattSykmeldingFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerInntektsmeldingUferdigGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding, AvventerSøknadUferdigGap)
        }
    }


    internal object AvventerSøknadFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_FERDIG_GAP
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerVilkårsprøvingGap)
            søknad.info("Fullført behandling av søknad")
        }
    }

    internal object AvventerGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_GAP
        override val timeout: Duration = Duration.ofHours(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerYtelser(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerYtelser(påminnelse)
            påminnelse.info("Forespør sykdoms- og inntektshistorikk (Påminnet)")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøvingGap)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser
        ) {
            if (ytelser.valider().hasErrors()) {
                ytelser.error("Feil i ytelser i %s", AvventerHistorikk.type)
                return vedtaksperiode.tilstand(ytelser, TilInfotrygd)
            }

            val sisteUtbetalteDag = ytelser.utbetalingshistorikk().sisteUtbetalteDag()
            if (sisteUtbetalteDag != null && sisteUtbetalteDag.harTilstøtende(vedtaksperiode.periode().start)) {
                ytelser.error("Har tilstøtende periode i Infotrygd som er utbetalt")
                return vedtaksperiode.tilstand(ytelser, TilInfotrygd)
            }

            vedtaksperiode.tilstand(ytelser, AvventerInntektsmeldingFerdigGap)
        }

    }

    internal object AvventerInntektsmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_UFERDIG_GAP
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding, AvventerUferdigGap)
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerGap)
        }
    }

    internal object AvventerUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_UFERDIG_GAP
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerVilkårsprøvingGap)
        }
    }

    internal object AvventerInntektsmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerHistorikk)
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding, AvventerUferdigForlengelse)
        }
    }

    internal object AvventerUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_UFERDIG_FORLENGELSE
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerHistorikk)
        }
    }

    internal object AvventerSøknadUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_FORLENGELSE
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, MottattSykmeldingFerdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerUferdigForlengelse)
            søknad.info("Fullført behandling av søknad")
        }
    }

    internal object AvventerSøknadUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_GAP
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerUferdigGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerSøknadFerdigGap)
        }
    }

    internal object AvventerInntektsmeldingFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_FERDIG_GAP
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøvingGap)
        }
    }

    internal object AvventerVilkårsprøvingGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING_GAP
        override val timeout: Duration = Duration.ofHours(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
            hendelse.info("Forespør vilkårsgrunnlag")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            if (vilkårsgrunnlag.valider().hasErrors()) {
                vilkårsgrunnlag.error("Feil i vilkårsgrunnlag i %s", AvventerVilkårsprøvingGap.type)
                return vedtaksperiode.tilstand(vilkårsgrunnlag, TilInfotrygd)
            }

            val førsteFraværsdag = vedtaksperiode.sykdomshistorikk.sykdomstidslinje().førsteFraværsdag()
            val beregnetInntekt = vedtaksperiode.arbeidsgiver.inntekt(førsteFraværsdag)
                ?: vilkårsgrunnlag.severe("Finner ikke inntekt for perioden %s", førsteFraværsdag)

            val resultat = vilkårsgrunnlag.måHåndteresManuelt(beregnetInntekt, førsteFraværsdag)
            vedtaksperiode.dataForVilkårsvurdering = resultat.grunnlagsdata

            if (resultat.måBehandlesManuelt) return vedtaksperiode.tilstand(vilkårsgrunnlag, TilInfotrygd)

            vilkårsgrunnlag.info("Vilkårsgrunnlag verifisert")
            vedtaksperiode.tilstand(vilkårsgrunnlag, AvventerHistorikk)
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
                val sisteHistoriskeSykedag = ytelser.utbetalingshistorikk().sisteUtbetalteDag()
                it.valider {
                    HarInntektshistorikk(
                        arbeidsgiver, vedtaksperiode.sykdomshistorikk.sykdomstidslinje().førsteDag()
                    )
                }
                it.valider { HarArbeidsgivertidslinje(arbeidsgiver) }
                var engineForTimeline: ByggUtbetalingstidlinjer? = null
                it.valider {
                    ByggUtbetalingstidlinjer(
                        mapOf(
                            arbeidsgiver to utbetalingstidslinje(
                                arbeidsgiver,
                                vedtaksperiode,
                                sisteHistoriskeSykedag
                            )
                        ),
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

            godkjenning(hendelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            manuellSaksbehandling: ManuellSaksbehandling
        ) {
            if (manuellSaksbehandling.utbetalingGodkjent()) {
                vedtaksperiode.tilstand(
                    manuellSaksbehandling,
                    if (vedtaksperiode.utbetalingslinjer.isNullOrEmpty()) Avsluttet else TilUtbetaling
                ) {
                    vedtaksperiode.godkjenttidspunkt = manuellSaksbehandling.godkjenttidspunkt()
                    vedtaksperiode.godkjentAv = manuellSaksbehandling.saksbehandler().also {
                        manuellSaksbehandling.info(
                            "Utbetaling markert som godkjent av saksbehandler $it ${vedtaksperiode.godkjenttidspunkt}"
                        )
                    }
                }
            } else {
                manuellSaksbehandling.error(
                    "Utbetaling markert som ikke godkjent av saksbehandler (%s)",
                    manuellSaksbehandling.saksbehandler()
                )
                vedtaksperiode.tilstand(manuellSaksbehandling, TilInfotrygd)
            }
        }

        private fun lagUtbetalingsReferanse(vedtaksperiode: Vedtaksperiode) =
            vedtaksperiode.arbeidsgiver.tilstøtende(vedtaksperiode)?.utbetalingsreferanse
                ?: genererUtbetalingsreferanse(vedtaksperiode.id)
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING
        override val timeout: Duration = Duration.ZERO

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            utbetaling(
                hendelse,
                vedtaksperiode.utbetalingsreferanse,
                requireNotNull(vedtaksperiode.utbetalingslinjer),
                requireNotNull(vedtaksperiode.maksdato),
                requireNotNull(vedtaksperiode.godkjentAv)
            )
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
                vedtaksperiode.tilstand(utbetaling, Avsluttet) {
                    utbetaling.info("OK fra Oppdragssystemet")
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

    internal object Avsluttet : Vedtaksperiodetilstand {
        override val type = AVSLUTTET
        override val timeout: Duration = Duration.ZERO

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling(vedtaksperiode, hendelse)
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
            .minBy { it.periode().start }

        internal fun sykdomstidslinje(perioder: List<Vedtaksperiode>) = perioder
            .filterNot { it.tilstand == TilInfotrygd }
            .map { it.sykdomshistorikk.sykdomstidslinje() }.join()
    }
}
