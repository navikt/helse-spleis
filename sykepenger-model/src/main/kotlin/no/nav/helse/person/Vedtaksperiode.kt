package no.nav.helse.person


import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.egenAnsatt
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.godkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.inntektsberegning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opptjening
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.sykepengehistorikk
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetaling
import no.nav.helse.person.Arbeidsgiver.GjenopptaBehandling
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
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
    private val utbetalingslinjer: MutableList<Utbetalingslinje>,
    private var godkjentAv: String?,
    private var godkjenttidspunkt: LocalDateTime?,
    private var utbetalingsreferanse: String,
    private var førsteFraværsdag: LocalDate?,
    private var dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?,
    private var dataForSimulering: Simulering.SimuleringResultat?,
    private val sykdomshistorikk: Sykdomshistorikk,
    private var utbetalingstidslinje: Utbetalingstidslinje = Utbetalingstidslinje()
) : Aktivitetskontekst {

    private val påminnelseThreshold = Integer.MAX_VALUE

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
        utbetalingslinjer = mutableListOf(),
        godkjentAv = null,
        godkjenttidspunkt = null,
        utbetalingsreferanse = genererUtbetalingsreferanse(id),
        førsteFraværsdag = null,
        dataForSimulering = null,
        dataForVilkårsvurdering = null,
        sykdomshistorikk = Sykdomshistorikk(),
        utbetalingstidslinje = Utbetalingstidslinje()
    )

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        visitor.preVisitVedtaksperiode(this, id)
        visitor.visitMaksdato(maksdato)
        visitor.visitForbrukteSykedager(forbrukteSykedager)
        visitor.visitGodkjentAv(godkjentAv)
        visitor.visitFørsteFraværsdag(førsteFraværsdag)
        visitor.visitUtbetalingsreferanse(utbetalingsreferanse)
        visitor.visitDataForVilkårsvurdering(dataForVilkårsvurdering)
        visitor.visitDataForSimulering(dataForSimulering)
        sykdomshistorikk.accept(visitor)
        utbetalingstidslinje.accept(visitor)
        visitor.visitTilstand(tilstand)
        visitor.preVisitUtbetalingslinjer(utbetalingslinjer)
        utbetalingslinjer.forEach { visitor.visitUtbetalingslinje(it) }
        visitor.postVisitUtbetalingslinjer(utbetalingslinjer)
        visitor.postVisitVedtaksperiode(this, id)
    }

    internal fun periode() = Periode(førsteDag(), sisteDag())

    internal fun førsteDag() = sykdomstidslinje().førsteDag()

    internal fun sisteDag() = sykdomstidslinje().sisteDag()

    private fun sykdomstidslinje() = sykdomshistorikk.sykdomstidslinje()

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to id.toString()))
    }

    private fun valider(hendelse: SykdomstidslinjeHendelse, block: () -> Unit) {
        if (hendelse.valider().hasErrors())
            return tilstand(hendelse, TilInfotrygd)
        block()
    }

    internal fun håndter(sykmelding: Sykmelding) = overlapperMed(sykmelding).also {
        if (!it) return it
        kontekst(sykmelding)
        valider(sykmelding) { tilstand.håndter(this, sykmelding) }
    }

    internal fun håndter(søknad: Søknad) = overlapperMed(søknad).also {
        if (!it) return it
        kontekst(søknad)
        valider(søknad) { tilstand.håndter(this, søknad) }
    }

    internal fun håndter(inntektsmelding: Inntektsmelding) = overlapperMed(inntektsmelding).also {
        if (!it) return it
        kontekst(inntektsmelding)
        valider(inntektsmelding) { tilstand.håndter(this, inntektsmelding) }
    }

    internal fun håndter(ytelser: Ytelser) {
        if (id.toString() != ytelser.vedtaksperiodeId) return
        kontekst(ytelser)
        tilstand.håndter(person, arbeidsgiver, this, ytelser)
    }

    internal fun håndter(manuellSaksbehandling: ManuellSaksbehandling) {
        if (id.toString() != manuellSaksbehandling.vedtaksperiodeId()) return
        kontekst(manuellSaksbehandling)
        tilstand.håndter(person, arbeidsgiver, this, manuellSaksbehandling)
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        if (id.toString() != vilkårsgrunnlag.vedtaksperiodeId) return
        kontekst(vilkårsgrunnlag)
        tilstand.håndter(this, vilkårsgrunnlag)
    }

    internal fun håndter(utbetaling: Utbetaling) {
        if (id.toString() != utbetaling.vedtaksperiodeId) return
        kontekst(utbetaling)
        tilstand.håndter(this, utbetaling)
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        if (id.toString() != påminnelse.vedtaksperiodeId) return false
        if (!påminnelse.gjelderTilstand(tilstand.type)) return true

        kontekst(påminnelse)
        person.vedtaksperiodePåminnet(påminnelse)

        if (påminnelse.antallGangerPåminnet() < påminnelseThreshold) {
            tilstand.håndter(this, påminnelse)
        } else {
            påminnelse.error("Invaliderer perioden fordi den har blitt påminnet %d ganger", påminnelseThreshold)
            tilstand(påminnelse, TilInfotrygd)
        }
        return true
    }

    internal fun håndter(other: Vedtaksperiode, hendelse: GjenopptaBehandling) {
        val forlengelse = arbeidsgiver.tilstøtende(this) != null
        val ferdig = arbeidsgiver.tidligerePerioderFerdigBehandlet(this, forlengelse)
        if (this.periode().start > other.periode().start && ferdig) {
            kontekst(hendelse.hendelse)
            tilstand.håndter(this, hendelse)
        }
    }

    internal fun invaliderPeriode(hendelse: ArbeidstakerHendelse) {
        hendelse.info("Invaliderer vedtaksperiode: %s", this.id.toString())
        tilstand(hendelse, TilInfotrygd)
    }

    private fun kontekst(hendelse: ArbeidstakerHendelse) {
        hendelse.kontekst(this)
        hendelse.kontekst(this.tilstand)
    }

    private fun overlapperMed(hendelse: SykdomstidslinjeHendelse) =
        sykdomshistorikk.isEmpty() ||
            this.sykdomstidslinje().overlapperMed(hendelse.sykdomstidslinje())

    private fun tilstand(
        event: ArbeidstakerHendelse,
        nyTilstand: Vedtaksperiodetilstand,
        block: () -> Unit = {}
    ) {
        if (tilstand == nyTilstand) return  // Already in this state => ignore
        tilstand.leaving(event)

        val previousState = tilstand

        tilstand = nyTilstand
        block()

        try {
            event.kontekst(tilstand)
            tilstand.entering(this, event)
        } finally {
            emitVedtaksperiodeEndret(tilstand.type, event, previousState.type, tilstand.timeout)
        }
    }

    private fun håndter(hendelse: Inntektsmelding, nesteTilstand: Vedtaksperiodetilstand) {
        arbeidsgiver.addInntekt(hendelse)
        sykdomshistorikk.håndter(hendelse)
        førsteFraværsdag = hendelse.førsteFraværsdag
        if (hendelse.førsteFraværsdag > sisteDag())
            hendelse.warn("Inntektsmelding har oppgitt første fraværsdag etter tidslinjen til perioden")
        if (hendelse.førsteFraværsdag != sykdomstidslinje().førsteFraværsdag())
            hendelse.warn("Inntektsmelding har oppgitt en annen første fraværsdag")

        if (hendelse.hasErrors()) return tilstand(hendelse, TilInfotrygd)
        tilstand(hendelse, nesteTilstand)
        hendelse.info("Fullført behandling av inntektsmelding")
    }

    private fun håndter(hendelse: SykdomstidslinjeHendelse, nesteTilstand: () -> Vedtaksperiodetilstand) {
        sykdomshistorikk.håndter(hendelse)
        if (hendelse.hasErrors()) return tilstand(hendelse, TilInfotrygd)
        tilstand(hendelse, nesteTilstand())
    }

    private fun håndter(hendelse: Søknad, nesteTilstand: Vedtaksperiodetilstand) {
        sykdomshistorikk.håndter(hendelse)
        if (hendelse.hasErrors()) return tilstand(hendelse, TilInfotrygd)
            .also { trengerInntektsmelding() }
        tilstand(hendelse, nesteTilstand)
    }

    private fun trengerYtelser(hendelse: ArbeidstakerHendelse) {
        sykepengehistorikk(hendelse, arbeidsgiver.sykdomstidslinje().førsteDag().minusYears(4), periode().endInclusive)
        foreldrepenger(hendelse)
    }

    internal fun trengerVilkårsgrunnlag(hendelse: ArbeidstakerHendelse) {
        val beregningSlutt = YearMonth.from(førsteFraværsdag)
        inntektsberegning(hendelse, beregningSlutt.minusMonths(11), beregningSlutt)
        egenAnsatt(hendelse)
        opptjening(hendelse)
    }

    private fun trengerInntektsmelding() {
        this.person.trengerInntektsmelding(
            PersonObserver.ManglendeInntektsmeldingEvent(
                vedtaksperiodeId = this.id,
                organisasjonsnummer = this.organisasjonsnummer,
                fødselsnummer = this.fødselsnummer,
                opprettet = LocalDate.now(),
                fom = this.periode().start,
                tom = this.periode().endInclusive
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
            aktivitetslogg = tidslinjeEvent.aktivitetslogg,
            timeout = varighet
        )

        person.vedtaksperiodeEndret(event)
    }

    internal fun harTilstøtende(other: Vedtaksperiode) =
        this.sykdomstidslinje().harTilstøtende(other.sykdomstidslinje())

    internal fun erFerdigBehandlet(other: Vedtaksperiode, forlengelse: Boolean): Boolean {
        if (this.periode().start >= other.periode().start) return true
        if (this.tilstand.type == TIL_INFOTRYGD && forlengelse) return false
        return this.tilstand.type in listOf(
            TIL_INFOTRYGD,
            AVSLUTTET
        )
    }

    // Gang of four State pattern
    internal interface Vedtaksperiodetilstand : Aktivitetskontekst {
        val type: TilstandType

        val timeout: Duration

        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            return SpesifikkKontekst(
                "Tilstand", mapOf(
                    "tilstand" to type.name
                )
            )
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            sykmelding.warn("Forventet ikke sykmelding i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            søknad.trimLeft(vedtaksperiode.periode().endInclusive) // Kill any overlap with this periode
            søknad.warn("Forventet ikke søknad i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            inntektsmelding.trimLeft(vedtaksperiode.periode().endInclusive) // Kill any overlap with this periode
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

        fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            simulering.error("Forventet ikke simulering i %s", type.name)
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

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
        }

        fun leaving(aktivitetslogg: IAktivitetslogg) {}
    }

    internal object Start : Vedtaksperiodetilstand {
        override val type = START
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            vedtaksperiode.håndter(sykmelding) { nesteTilstand(vedtaksperiode, sykmelding) }
            sykmelding.info("Fullført behandling av sykmelding")
        }

        private fun nesteTilstand(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding): Vedtaksperiodetilstand {
            val tilstøtende = vedtaksperiode.arbeidsgiver.tilstøtende(vedtaksperiode)
            val forlengelse = tilstøtende != null
            val ferdig = vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode, forlengelse)

            return when {
                forlengelse && tilstøtende?.tilstand == TilInfotrygd -> {
                    sykmelding.error("Forlenger en periode som er gått til infotrygd")
                    TilInfotrygd
                }
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøvingGap)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser
        ) {
            Validation(ytelser).also {
                it.onError {
                    vedtaksperiode.tilstand(ytelser, TilInfotrygd)
                        .also { vedtaksperiode.trengerInntektsmelding() }
                }
                it.valider { ValiderYtelser(arbeidsgiver.sykdomstidslinje(), ytelser, vedtaksperiode.periode().start) }
                it.onSuccess {
                    vedtaksperiode.tilstand(ytelser, AvventerInntektsmeldingFerdigGap)
                }
            }
        }

    }

    internal object AvventerInntektsmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_UFERDIG_GAP
        override val timeout: Duration = Duration.ofDays(30)

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerUferdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerGap)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerInntektsmelding()
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerUferdigForlengelse)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerInntektsmelding()
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøvingGap)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerInntektsmelding()
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
                vilkårsgrunnlag.error("Feil i vilkårsgrunnlag i %s", type)
                return vedtaksperiode.tilstand(vilkårsgrunnlag, TilInfotrygd)
            }

            val førsteFraværsdag = vedtaksperiode.sykdomstidslinje().førsteFraværsdag()
                ?: vedtaksperiode.periode().start
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

            vedtaksperiode.arbeidsgiver.tilstøtende(vedtaksperiode)?.let {
                vedtaksperiode.førsteFraværsdag = it.førsteFraværsdag
                if (it.tilstand == TilInfotrygd) {
                    hendelse.error("Kan ikke forlenge en sak som er gått til infotrygd")
                    vedtaksperiode.tilstand(hendelse, TilInfotrygd)
                }
            }
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
                it.valider { ValiderYtelser(arbeidsgiver.sykdomstidslinje(), ytelser, vedtaksperiode.førsteFraværsdag) }
                it.valider { Overlappende(vedtaksperiode.periode(), ytelser.foreldrepenger()) }
                it.valider {
                    HarInntektshistorikk(
                        arbeidsgiver, vedtaksperiode.førsteDag()
                    )
                }
                var engineForTimeline: ByggUtbetalingstidlinjer? = null
                it.valider {
                    ByggUtbetalingstidlinjer(
                        mapOf(
                            arbeidsgiver to utbetalingstidslinje(
                                arbeidsgiver,
                                vedtaksperiode
                            )
                        ),
                        vedtaksperiode.periode(),
                        ytelser,
                        Alder(vedtaksperiode.fødselsnummer),
                        vedtaksperiode.førsteFraværsdag
                    ).also { engineForTimeline = it }
                }
                var engineForLine: ByggUtbetalingslinjer? = null
                it.valider {
                    ByggUtbetalingslinjer(
                        ytelser,
                        vedtaksperiode,
                        arbeidsgiver.peekTidslinje()
                    ).also { engineForLine = it }
                }
                it.onSuccess {
                    vedtaksperiode.maksdato = engineForTimeline?.maksdato()
                    vedtaksperiode.forbrukteSykedager = engineForTimeline?.forbrukteSykedager()
                    vedtaksperiode.utbetalingstidslinje = engineForLine?.utbetalingstidslinje ?: Utbetalingstidslinje()
                    vedtaksperiode.utbetalingslinjer.also {
                        it.clear()
                        it.addAll(engineForLine?.utbetalingslinjer() ?: emptyList())
                    }
                    ytelser.info("""Saken oppfyller krav for behandling, settes til "Til godkjenning"""")
                    vedtaksperiode.tilstand(ytelser, AvventerGodkjenning)
                }
            }
        }

        private fun utbetalingstidslinje(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode
        ): Utbetalingstidslinje {
            return UtbetalingstidslinjeBuilder(
                sykdomstidslinje = arbeidsgiver.sykdomstidslinje(),
                sisteDag = vedtaksperiode.sisteDag(),
                inntekthistorikk = arbeidsgiver.inntektshistorikk(),
                arbeidsgiverRegler = NormalArbeidstaker
            ).result()
        }
    }

    internal object AvventerSimulering : Vedtaksperiodetilstand {

        override val type: TilstandType = AVVENTER_SIMULERING
        override val timeout: Duration = Duration.ofHours(1)

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            trengerSimulering(vedtaksperiode, påminnelse)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            trengerSimulering(vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            simulering.valider()
            if (simulering.hasErrors()) return simulering.warn("Simulering har feil, ignorerer resultatet.")

            vedtaksperiode.dataForSimulering = simulering.simuleringResultat
            vedtaksperiode.tilstand(simulering, AvventerGodkjenning)
        }

        private fun trengerSimulering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) = simulering(
            hendelse, vedtaksperiode.utbetalingsreferanse,
            vedtaksperiode.utbetalingslinjer,
            requireNotNull(vedtaksperiode.maksdato),
            "Spleis"
        )
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
            .map { it.sykdomstidslinje() }.join()

        fun sorter(perioder: MutableList<Vedtaksperiode>) {
            perioder.sortBy { it.periode().start }
        }
    }
}
