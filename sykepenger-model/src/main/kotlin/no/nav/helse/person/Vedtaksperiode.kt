package no.nav.helse.person


import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.arbeidsavklaringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.dagpenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.egenAnsatt
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.godkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.inntektsberegning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.medlemskap
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opptjening
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetaling
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.person.Arbeidsgiver.GjenopptaBehandling
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.harTilstøtende
import no.nav.helse.sykdomstidslinje.join
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class Vedtaksperiode private constructor(
    private val person: Person,
    private val arbeidsgiver: Arbeidsgiver,
    private val id: UUID,
    private var gruppeId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private var tilstand: Vedtaksperiodetilstand,
    private var maksdato: LocalDate?,
    private var gjenståendeSykedager: Int?,
    private var forbrukteSykedager: Int?,
    private var godkjentAv: String?,
    private var godkjenttidspunkt: LocalDateTime?,
    private var førsteFraværsdag: LocalDate?,
    private var dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?,
    private var dataForSimulering: Simulering.SimuleringResultat?,
    private val sykdomshistorikk: Sykdomshistorikk,
    private var utbetalingstidslinje: Utbetalingstidslinje = Utbetalingstidslinje(),
    private var personFagsystemId: String?,
    private var arbeidsgiverFagsystemId: String?,
    private var forlengelseFraInfotrygd: ForlengelseFraInfotrygd = ForlengelseFraInfotrygd.IKKE_ETTERSPURT
) : Aktivitetskontekst {

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
        gruppeId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        tilstand = tilstand,
        maksdato = null,
        gjenståendeSykedager = null,
        forbrukteSykedager = null,
        godkjentAv = null,
        godkjenttidspunkt = null,
        førsteFraværsdag = null,
        dataForVilkårsvurdering = null,
        dataForSimulering = null,
        sykdomshistorikk = Sykdomshistorikk(),
        utbetalingstidslinje = Utbetalingstidslinje(),
        personFagsystemId = null,
        arbeidsgiverFagsystemId = null
    )

    internal fun arbeidsgiverFagsystemId() = arbeidsgiverFagsystemId
    internal fun personFagsystemId() = personFagsystemId

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        visitor.preVisitVedtaksperiode(this, id, gruppeId)
        sykdomshistorikk.accept(visitor)
        utbetalingstidslinje.accept(visitor)
        visitor.visitTilstand(tilstand)
        visitor.visitMaksdato(maksdato)
        visitor.visitGjenståendeSykedager(gjenståendeSykedager)
        visitor.visitForbrukteSykedager(forbrukteSykedager)
        visitor.visitGodkjentAv(godkjentAv)
        visitor.visitFørsteFraværsdag(førsteFraværsdag)
        visitor.visitDataForVilkårsvurdering(dataForVilkårsvurdering)
        visitor.visitDataForSimulering(dataForSimulering)
        visitor.postVisitVedtaksperiode(this, id, gruppeId)
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to id.toString()))
    }

    internal fun håndter(sykmelding: Sykmelding) = overlapperMed(sykmelding).also {
        if (!it) return it
        kontekst(sykmelding)
        valider(sykmelding) { tilstand.håndter(this, sykmelding) }
    }

    internal fun håndter(søknad: SøknadArbeidsgiver) = overlapperMed(søknad).also {
        if (!it) return it
        kontekst(søknad)
        valider(søknad) { tilstand.håndter(this, søknad) }
    }

    internal fun håndter(søknad: Søknad) = overlapperMed(søknad).also {
        if (!it) return it
        kontekst(søknad)
        valider(søknad) { tilstand.håndter(this, søknad) }
    }

    override fun toString() = "${this.periode().start} - ${this.periode().endInclusive}"

    internal fun håndter(inntektsmelding: Inntektsmelding) = overlapperMed(inntektsmelding).also {
        if (!it) return it
        kontekst(inntektsmelding)
        valider(inntektsmelding) { tilstand.håndter(this, inntektsmelding) }
    }

    internal fun håndter(utbetalingshistorikk: Utbetalingshistorikk) {
        if (id.toString() != utbetalingshistorikk.vedtaksperiodeId) return
        kontekst(utbetalingshistorikk)
        tilstand.håndter(person, arbeidsgiver, this, utbetalingshistorikk)
    }

    internal fun håndter(ytelser: Ytelser) {
        if (id.toString() != ytelser.vedtaksperiodeId) return
        kontekst(ytelser)
        tilstand.håndter(person, arbeidsgiver, this, ytelser)
    }

    internal fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        if (id.toString() != utbetalingsgodkjenning.vedtaksperiodeId()) return
        kontekst(utbetalingsgodkjenning)
        tilstand.håndter(person, arbeidsgiver, this, utbetalingsgodkjenning)
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        if (id.toString() != vilkårsgrunnlag.vedtaksperiodeId) return
        kontekst(vilkårsgrunnlag)
        tilstand.håndter(this, vilkårsgrunnlag)
    }

    internal fun håndter(simulering: Simulering) {
        if (id.toString() != simulering.vedtaksperiodeId) return
        kontekst(simulering)
        tilstand.håndter(this, simulering)
    }

    internal fun håndter(utbetaling: UtbetalingOverført) {
        if (id.toString() != utbetaling.vedtaksperiodeId) return
        kontekst(utbetaling)
        tilstand.håndter(this, utbetaling)
    }


    internal fun håndter(utbetaling: UtbetalingHendelse) {
        if (id.toString() != utbetaling.vedtaksperiodeId) return
        kontekst(utbetaling)
        tilstand.håndter(this, utbetaling)
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        if (id.toString() != påminnelse.vedtaksperiodeId) return false
        if (!påminnelse.gjelderTilstand(tilstand.type)) return true
        kontekst(påminnelse)
        person.vedtaksperiodePåminnet(påminnelse)
        if (LocalDateTime.now() >= tilstand.makstid(this, påminnelse.tilstandsendringstidspunkt())) {
            påminnelse.kontekst(person)
            påminnelse.error("Gir opp fordi tilstanden er nådd makstid")
            tilstand(påminnelse, TilInfotrygd)
        } else {
            tilstand.håndter(this, påminnelse)
        }
        return true
    }

    internal fun håndter(other: Vedtaksperiode, hendelse: GjenopptaBehandling) {
        val ferdig = arbeidsgiver.tidligerePerioderFerdigBehandlet(this)
        if (this.periode().start > other.periode().start && ferdig) {
            kontekst(hendelse.hendelse)
            tilstand.håndter(this, hendelse)
        }
    }

    internal fun håndter(other: Vedtaksperiode, hendelse: Arbeidsgiver.AvsluttBehandling) {
        if (this.periode()
                .overlapperMed(other.periode()) || other.periode().endInclusive.harTilstøtende(this.periode().start)
        ) {
            kontekst(hendelse.hendelse)
            tilstand.håndter(this, hendelse)
        }
    }

    internal fun invaliderPeriode(hendelse: ArbeidstakerHendelse) {
        hendelse.info("Invaliderer vedtaksperiode: %s", this.id.toString())
        tilstand(hendelse, TilInfotrygd)
    }

    internal fun annullerPeriode(hendelse: ArbeidstakerHendelse) {
        hendelse.info("Invaliderer vedtaksperiode: %s på grunn av annullering", this.id.toString())
        tilstand(hendelse, TilInfotrygd)
    }

    private fun periode() =
        if (sykdomshistorikk.isEmpty()) Periode(LocalDate.MIN, LocalDate.MAX)
        else Periode(førsteDag(), sisteDag())

    private fun førsteDag() = sykdomstidslinje().førsteDag()

    private fun sisteDag() = sykdomstidslinje().sisteDag()

    private fun sykdomstidslinje() = sykdomshistorikk.sykdomstidslinje()

    private fun valider(hendelse: SykdomstidslinjeHendelse, block: () -> Unit) {
        if (hendelse.valider(periode()).hasErrors())
            return tilstand(hendelse, TilInfotrygd)
        block()
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

        event.kontekst(tilstand)
        emitVedtaksperiodeEndret(tilstand, event.aktivitetslogg, person.aktivitetslogg.logg(this), previousState)
        tilstand.entering(this, event)
    }

    private fun håndter(hendelse: Inntektsmelding, nesteTilstand: Vedtaksperiodetilstand) {
        arbeidsgiver.addInntekt(hendelse)
        sykdomshistorikk.håndter(hendelse)
        førsteFraværsdag = hendelse.førsteFraværsdag
        if (hendelse.førsteFraværsdag != null) {
            if (hendelse.førsteFraværsdag > sisteDag())
                hendelse.warn("Første fraværsdag i inntektsmeldingen er utenfor sykmeldingsperioden")
            if (arbeidsgiver.tilstøtende(this) == null && hendelse.førsteFraværsdag != sykdomstidslinje().førsteFraværsdag())
                hendelse.warn("Første fraværsdag i inntektsmeldingen er utenfor søknadsperioden. Kontroller at inntektsmeldingen er knyttet til riktig periode")
        }
        hendelse.valider(periode())
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

    private fun håndter(hendelse: SøknadArbeidsgiver, nesteTilstand: Vedtaksperiodetilstand) {
        sykdomshistorikk.håndter(hendelse)
        if (hendelse.hasErrors()) return tilstand(hendelse, TilInfotrygd)
        tilstand(hendelse, nesteTilstand)
    }

    private fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag, nesteTilstand: Vedtaksperiodetilstand) {
        val førsteFraværsdag = sykdomstidslinje().førsteFraværsdag()
            ?: periode().start
        val beregnetInntekt = arbeidsgiver.inntekt(førsteFraværsdag) ?: vilkårsgrunnlag.severe(
            "Finner ikke inntekt for perioden $førsteFraværsdag"
        )
        if (vilkårsgrunnlag.valider(beregnetInntekt, førsteFraværsdag).hasErrors().also {
                dataForVilkårsvurdering = vilkårsgrunnlag.grunnlagsdata()
            }) {
            vilkårsgrunnlag.info("Feil i vilkårsgrunnlag i %s", tilstand.type)
            return tilstand(vilkårsgrunnlag, TilInfotrygd)
        }
        vilkårsgrunnlag.info("Vilkårsgrunnlag verifisert")
        tilstand(vilkårsgrunnlag, nesteTilstand)
    }

    private fun trengerYtelser(hendelse: ArbeidstakerHendelse) {
        utbetalingshistorikk(
            hendelse,
            Periode(arbeidsgiver.sykdomstidslinje().førsteDag().minusYears(4), periode().endInclusive)
        )
        foreldrepenger(hendelse)
    }

    private fun trengerKortHistorikkFraInfotrygd(hendelse: ArbeidstakerHendelse) {
        utbetalingshistorikk(hendelse, periode())
    }

    private fun trengerVilkårsgrunnlag(hendelse: ArbeidstakerHendelse) {
        val beregningSlutt = YearMonth.from(førsteFraværsdag).minusMonths(1)
        inntektsberegning(hendelse, beregningSlutt.minusMonths(11), beregningSlutt)
        egenAnsatt(hendelse)
        opptjening(hendelse)
        dagpenger(hendelse, periode().start.minusMonths(6), periode().endInclusive)
        arbeidsavklaringspenger(hendelse, periode().start.minusMonths(6), periode().endInclusive)
        medlemskap(hendelse, periode().start.minusYears(1), periode().endInclusive)
    }

    private fun trengerInntektsmelding() {
        this.person.trengerInntektsmelding(
            PersonObserver.ManglendeInntektsmeldingEvent(
                vedtaksperiodeId = this.id,
                organisasjonsnummer = this.organisasjonsnummer,
                fødselsnummer = this.fødselsnummer,
                fom = this.periode().start,
                tom = this.periode().endInclusive
            )
        )
    }

    private fun emitVedtaksperiodeEndret(
        currentState: Vedtaksperiodetilstand,
        hendelseaktivitetslogg: Aktivitetslogg,
        vedtaksperiodeaktivitetslogg: Aktivitetslogg,
        previousState: Vedtaksperiodetilstand
    ) {
        val hendelser = mutableSetOf<UUID>()
        sykdomshistorikk.accept(object : SykdomshistorikkVisitor {
            override fun postVisitSykdomshistorikkElement(
                element: Sykdomshistorikk.Element,
                id: UUID,
                tidsstempel: LocalDateTime
            ) {
                hendelser.add(id)
            }
        })
        val event = PersonObserver.VedtaksperiodeEndretTilstandEvent(
            vedtaksperiodeId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            gjeldendeTilstand = currentState.type,
            forrigeTilstand = previousState.type,
            aktivitetslogg = hendelseaktivitetslogg,
            vedtaksperiodeaktivitetslogg = vedtaksperiodeaktivitetslogg,
            hendelser = hendelser,
            makstid = currentState.makstid(this, LocalDateTime.now())
        )

        person.vedtaksperiodeEndret(event)
    }

    private fun høstingsresultater(
        engineForTimeline: ByggUtbetalingstidlinjer,
        ytelser: Ytelser
    ) {
        maksdato = engineForTimeline.maksdato()
        gjenståendeSykedager = engineForTimeline.gjenståendeSykedager()
        forbrukteSykedager = engineForTimeline.forbrukteSykedager()
        utbetalingstidslinje = arbeidsgiver.nåværendeTidslinje().subset(periode())
        personFagsystemId = arbeidsgiver.utbetaling()?.personOppdrag()?.fagsystemId()
        arbeidsgiverFagsystemId = arbeidsgiver.utbetaling()?.arbeidsgiverOppdrag()?.fagsystemId()
        if (utbetalingstidslinje.kunArbeidsgiverdager() &&
            person.aktivitetslogg.logg(this).hasOnlyInfoAndNeeds()
        ) return tilstand(ytelser, AvsluttetUtenUtbetalingMedInntektsmelding) {
            ytelser.info("""Saken inneholder ingen utbetalingsdager for Nav og avluttes""")
        }
        if (!utbetalingstidslinje.harUtbetalinger()) return tilstand(
            ytelser,
            AvventerGodkjenning
        ) {
            ytelser.info("""Saken oppfyller krav for behandling, settes til "Avventer godkjenning" fordi ingenting skal utbetales""")
        }
        tilstand(ytelser, AvventerSimulering) {
            ytelser.info("""Saken oppfyller krav for behandling, settes til "Avventer simulering"""")
        }
    }

    private fun erFerdigBehandlet(other: Vedtaksperiode): Boolean {
        if (this.periode().start >= other.periode().start) return true
        return this.tilstand.type in listOf(
            TIL_INFOTRYGD,
            AVSLUTTET,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
    }

    private fun utbetaling() =
        arbeidsgiver.utbetaling() ?: throw IllegalStateException("mangler utbetalinger")

    private fun sendUtbetaltEvent() {
        person.vedtaksperiodeUtbetalt(
            tilUtbetaltEvent(
                aktørId = aktørId,
                fødselnummer = fødselsnummer,
                orgnummer = organisasjonsnummer,
                utbetaling = utbetaling(),
                forbrukteSykedager = requireNotNull(forbrukteSykedager),
                gjenståendeSykedager = requireNotNull(gjenståendeSykedager),
                sykdomshistorikk = sykdomshistorikk,
                periode = periode()
            )
        )
    }

    // Gang of four State pattern
    internal interface Vedtaksperiodetilstand : Aktivitetskontekst {
        val type: TilstandType

        fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(30)

        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            return SpesifikkKontekst(
                "Tilstand", mapOf(
                    "tilstand" to type.name
                )
            )
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            sykmelding.warn("Mottatt flere sykmeldinger for perioden - den første sykmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            søknad.trimLeft(vedtaksperiode.periode().endInclusive)
            søknad.warn("Mottatt flere søknade for perioden - den første søknaden som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            søknad.trimLeft(vedtaksperiode.periode().endInclusive)
            søknad.warn("Mottatt flere søknader - den første søknaden som ble mottatt er lagt til grunn.")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            inntektsmelding.trimLeft(vedtaksperiode.periode().endInclusive)
            inntektsmelding.warn("Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vilkårsgrunnlag.error("Forventet ikke vilkårsgrunnlag i %s", type.name)
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
            if (utbetalingshistorikk.valider(vedtaksperiode.periode()).hasErrors()) return vedtaksperiode.tilstand(
                utbetalingshistorikk,
                TilInfotrygd
            ) {
                utbetalingshistorikk.kontekst(person)
                utbetalingshistorikk.error("Avdekket overlapp med utbetalt periode i Infotrygd")
            }
            utbetalingshistorikk.info("Utbetalingshistorikk sjekket for overlapp; fant ingenting.")
        }

        fun håndter(person: Person, arbeidsgiver: Arbeidsgiver, vedtaksperiode: Vedtaksperiode, ytelser: Ytelser) {
            ytelser.error("Forventet ikke ytelsehistorikk i %s", type.name)
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingsgodkjenning: Utbetalingsgodkjenning
        ) {
            utbetalingsgodkjenning.error("Forventet ikke utbetalingsgodkjenning i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerKortHistorikkFraInfotrygd(påminnelse)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            simulering.error("Forventet ikke simulering i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: UtbetalingOverført) {
            utbetaling.error("Forventet ikke utbetaling overført i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: UtbetalingHendelse) {
            utbetaling.error("Forventet ikke utbetaling i %s", type.name)
        }

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            gjenopptaBehandling.hendelse.info("Tidligere periode ferdig behandlet")
        }

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            avsluttBehandling: Arbeidsgiver.AvsluttBehandling
        ) {
            avsluttBehandling.hendelse.warn("Avslutter perioden fordi tilstøtende gikk til Infotrygd")
            vedtaksperiode.tilstand(avsluttBehandling.hendelse, TilInfotrygd)
        }

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {}

        fun leaving(aktivitetslogg: IAktivitetslogg) {}
    }

    internal object Start : Vedtaksperiodetilstand {
        override val type = START

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            vedtaksperiode.håndter(sykmelding) { nesteTilstand(vedtaksperiode, sykmelding) }
            sykmelding.info("Fullført behandling av sykmelding")
        }

        private fun nesteTilstand(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding): Vedtaksperiodetilstand {
            val tilstøtende = vedtaksperiode.arbeidsgiver.tilstøtende(vedtaksperiode)
            val forlengelse = tilstøtende != null
            val ferdig = vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)

            if (tilstøtende != null) {
                vedtaksperiode.forlengelseFraInfotrygd = tilstøtende.forlengelseFraInfotrygd
                vedtaksperiode.gruppeId = tilstøtende.gruppeId
            }

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

        override fun håndter(vedtaksperiode: Vedtaksperiode, avsluttBehandling: Arbeidsgiver.AvsluttBehandling) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerHistorikk)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object MottattSykmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, avsluttBehandling: Arbeidsgiver.AvsluttBehandling) {}

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

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, avsluttBehandling: Arbeidsgiver.AvsluttBehandling) {}

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

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object MottattSykmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_UFERDIG_GAP

        override fun håndter(vedtaksperiode: Vedtaksperiode, avsluttBehandling: Arbeidsgiver.AvsluttBehandling) {}

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

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
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

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (søknad.sykdomstidslinje().starterFør(vedtaksperiode.sykdomstidslinje())) {
                søknad.warn("Søknaden inneholder egenmeldingsdager som ikke er oppgitt i inntektsmeldingen")
                søknad.trimLeft(vedtaksperiode.sykdomshistorikk.sykdomstidslinje().førsteDag())
            }
            vedtaksperiode.håndter(søknad, AvventerVilkårsprøvingGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvventerVilkårsprøvingArbeidsgiversøknad)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object AvventerGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_GAP

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusHours(24)

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
                it.valider { ValiderYtelser(vedtaksperiode.periode(), ytelser) }
                it.onSuccess {
                    arbeidsgiver.addInntekt(ytelser)
                    val sistePeriode =
                        ytelser.utbetalingshistorikk().utbetalingstidslinje(vedtaksperiode.periode().start)
                            .sisteSykepengeperiode()
                    val nesteTilstand =
                        if (sistePeriode == null || !sistePeriode.endInclusive.harTilstøtende(vedtaksperiode.førsteDag())) {
                            vedtaksperiode.forlengelseFraInfotrygd = ForlengelseFraInfotrygd.NEI
                            AvventerInntektsmeldingFerdigGap
                        } else {
                            vedtaksperiode.førsteFraværsdag = sistePeriode.start
                            vedtaksperiode.forlengelseFraInfotrygd = ForlengelseFraInfotrygd.JA
                            if (arbeidsgiver.inntekt(sistePeriode.start) == null) TilInfotrygd.also {
                                ytelser.error("Kan ikke forlenge periode fra Infotrygd uten inntektsopplysninger")
                            } else {
                                ytelser.warn("Perioden er en direkte overgang fra periode i Infotrygd")
                                AvventerVilkårsprøvingGap
                            }
                        }
                    vedtaksperiode.tilstand(ytelser, nesteTilstand)
                }
            }
        }

    }

    internal object AvventerInntektsmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_UFERDIG_GAP

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

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

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerVilkårsprøvingGap)
        }
    }

    internal object AvventerInntektsmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

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

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerHistorikk)
        }
    }

    internal object AvventerSøknadUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_FORLENGELSE

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, MottattSykmeldingFerdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerUferdigForlengelse)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvventerVilkårsprøvingArbeidsgiversøknad)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object AvventerVilkårsprøvingArbeidsgiversøknad : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusHours(24)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
            hendelse.info("Forespør vilkårsgrunnlag")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vedtaksperiode.håndter(vilkårsgrunnlag, AvsluttetUtenUtbetalingMedInntektsmelding)
        }
    }

    internal object AvventerSøknadUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_GAP

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

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

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøvingGap)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerInntektsmelding()
        }

    }

    internal object AvventerVilkårsprøvingGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING_GAP

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusHours(24)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
            hendelse.info("Forespør vilkårsgrunnlag")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vedtaksperiode.håndter(vilkårsgrunnlag, AvventerHistorikk)
        }
    }

    internal object AvventerHistorikk : Vedtaksperiodetilstand {
        override val type = AVVENTER_HISTORIKK

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusHours(24)

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
                it.valider { ValiderYtelser(vedtaksperiode.periode(), ytelser) }
                it.valider { Overlappende(vedtaksperiode.periode(), ytelser.foreldrepenger()) }
                it.valider {
                    object : Valideringssteg {
                        override fun isValid(): Boolean {
                            val tilstøtende = arbeidsgiver.tilstøtende(vedtaksperiode) ?: return true
                            if (tilstøtende.tilstand != TilInfotrygd) return true.also {
                                vedtaksperiode.førsteFraværsdag = tilstøtende.førsteFraværsdag
                            }

                            val sistePeriode = ytelser.utbetalingshistorikk()
                                .utbetalingstidslinje(vedtaksperiode.førsteDag())
                                .sisteSykepengeperiode() ?: return false

                            if (!sistePeriode.endInclusive.harTilstøtende(vedtaksperiode.førsteDag())) return false
                            vedtaksperiode.forlengelseFraInfotrygd = ForlengelseFraInfotrygd.JA
                            if (vedtaksperiode.førsteFraværsdag == null) vedtaksperiode.førsteFraværsdag =
                                sistePeriode.start

                            arbeidsgiver.addInntekt(ytelser)
                            ytelser.warn("Perioden er en direkte overgang fra periode i Infotrygd")
                            return true
                        }

                        override fun feilmelding() =
                            "Oppdaget forlengelse fra Infotrygd, men perioden er ikke utbetalt enda"
                    }
                }
                it.valider { HarInntektshistorikk(arbeidsgiver, vedtaksperiode.førsteDag()) }
                lateinit var engineForTimeline: ByggUtbetalingstidlinjer
                it.valider {
                    val førsteFraværsdag =
                        requireNotNull(vedtaksperiode.førsteFraværsdag) { "Mangler første fraværsdag" }
                    ByggUtbetalingstidlinjer(
                        mapOf(
                            arbeidsgiver to utbetalingstidslinje(
                                arbeidsgiver,
                                vedtaksperiode,
                                ytelser
                            )
                        ),
                        vedtaksperiode.periode(),
                        ytelser,
                        vedtaksperiode.fødselsnummer,
                        vedtaksperiode.organisasjonsnummer,
                        Alder(vedtaksperiode.fødselsnummer),
                        førsteFraværsdag
                    ).also { engineForTimeline = it }
                }
                it.onSuccess {
                    vedtaksperiode.høstingsresultater(engineForTimeline, ytelser)
                }
            }
        }

        private fun utbetalingstidslinje(
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser
        ): Utbetalingstidslinje {
            return UtbetalingstidslinjeBuilder(
                sisteDag = vedtaksperiode.sisteDag(),
                inntekthistorikk = arbeidsgiver.inntektshistorikk(),
                forlengelseStrategy = { sykdomstidslinje ->
                    ytelser.utbetalingshistorikk().arbeidsgiverperiodeGjennomført(sykdomstidslinje.førsteDag())
                },
                arbeidsgiverRegler = NormalArbeidstaker
            ).result(arbeidsgiver.sykdomstidslinje())
        }
    }

    internal object AvventerSimulering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_SIMULERING

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            if (!vedtaksperiode.utbetalingstidslinje.harUtbetalinger()) return vedtaksperiode.tilstand(
                hendelse,
                AvventerGodkjenning
            ) {
                hendelse.warn("Ingen simulering av utbetaling på grunn av manglende utbetalingsinformasjon, går til Avventer godkjenning")
            }

            trengerSimulering(vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            trengerSimulering(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            if (simulering.valider(vedtaksperiode.utbetaling().arbeidsgiverOppdrag().removeUEND()).hasErrors()) {
                return vedtaksperiode.tilstand(simulering, TilInfotrygd)
            }
            vedtaksperiode.dataForSimulering = simulering.simuleringResultat
            vedtaksperiode.tilstand(simulering, AvventerGodkjenning)
        }

        private fun trengerSimulering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            simulering(
                aktivitetslogg = hendelse,
                oppdrag = Fagområde.SPREF.utbetalingslinjer(vedtaksperiode.utbetaling()).removeUEND(),
                maksdato = requireNotNull(vedtaksperiode.maksdato),
                saksbehandler = "Spleis"
            )
        }
    }

    internal object AvventerGodkjenning : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            tilstandsendringstidspunkt
                .plusHours(24)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            godkjenning(
                aktivitetslogg = hendelse,
                periodeFom = vedtaksperiode.førsteDag(),
                periodeTom = vedtaksperiode.sisteDag(),
                vedtaksperiodeaktivitetslogg = vedtaksperiode.person.aktivitetslogg.logg(vedtaksperiode)
            )
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingsgodkjenning: Utbetalingsgodkjenning
        ) {
            if (utbetalingsgodkjenning.valider().hasErrors()) return vedtaksperiode.tilstand(
                utbetalingsgodkjenning,
                TilInfotrygd
            )

            vedtaksperiode.tilstand(
                utbetalingsgodkjenning,
                if (!vedtaksperiode.utbetalingstidslinje.harUtbetalinger()) Avsluttet else TilUtbetaling
            ) {
                vedtaksperiode.godkjenttidspunkt = utbetalingsgodkjenning.godkjenttidspunkt()
                vedtaksperiode.godkjentAv = utbetalingsgodkjenning.saksbehandler().also {
                    utbetalingsgodkjenning.info(
                        "Utbetaling markert som godkjent av saksbehandler $it ${vedtaksperiode.godkjenttidspunkt}"
                    )
                }
            }
        }
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            utbetaling(
                hendelse,
                vedtaksperiode.utbetaling().arbeidsgiverOppdrag(),
                requireNotNull(vedtaksperiode.maksdato),
                requireNotNull(vedtaksperiode.godkjentAv) { "Forventer at saksbehandler har blitt satt på dette tidspunktet" }
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: UtbetalingOverført) {
            utbetaling.info(
                "Utbetalingen ble overført til Oppdrag/UR ${utbetaling.overføringstidspunkt}, " +
                    "og har fått avstemmingsnøkkel ${utbetaling.avstemmingsnøkkel}"
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: UtbetalingHendelse) {
            if (utbetaling.valider().hasErrors()) return vedtaksperiode.tilstand(utbetaling, UtbetalingFeilet) {
                utbetaling.error("Utbetaling ble ikke gjennomført")
            }

            vedtaksperiode.tilstand(utbetaling, Avsluttet) {
                utbetaling.info("OK fra Oppdragssystemet")
            }
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object UtbetalingFeilet : Vedtaksperiodetilstand {
        override val type = UTBETALING_FEILET

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.error("Feilrespons fra oppdrag")
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object AvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
        override val type = AVSLUTTET_UTEN_UTBETALING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling(vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(
                inntektsmelding,
                if (inntektsmelding.isNotQualified()) {
                    inntektsmelding.beingQualified()
                    AvventerVilkårsprøvingArbeidsgiversøknad
                } else
                    AvsluttetUtenUtbetalingMedInntektsmelding
            )
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object AvsluttetUtenUtbetalingMedInntektsmelding : Vedtaksperiodetilstand {
        override val type = AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling(vedtaksperiode, hendelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object Avsluttet : Vedtaksperiodetilstand {
        override val type = AVSLUTTET

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.sendUtbetaltEvent()

            vedtaksperiode.arbeidsgiver.gjenopptaBehandling(vedtaksperiode, hendelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.info("Sykdom for denne personen kan ikke behandles automatisk.")
            vedtaksperiode.arbeidsgiver.avsluttBehandling(vedtaksperiode, hendelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal companion object {
        internal fun tilstøtendePeriode(other: Vedtaksperiode, perioder: List<Vedtaksperiode>) = perioder
            .filter { it.sykdomstidslinje().harTilstøtende(other.sykdomstidslinje()) }
            .minBy { it.periode().start }

        internal fun sykdomstidslinje(perioder: List<Vedtaksperiode>) = perioder
            .filterNot { it.tilstand == TilInfotrygd }
            .map { it.sykdomstidslinje() }.join()

        internal fun sorter(perioder: MutableList<Vedtaksperiode>) {
            perioder.sortBy { it.periode().start }
        }

        internal fun tidligerePerioderFerdigBehandlet(perioder: List<Vedtaksperiode>, vedtaksperiode: Vedtaksperiode) =
            perioder.all { it.erFerdigBehandlet(vedtaksperiode) }
    }
}

enum class ForlengelseFraInfotrygd {
    IKKE_ETTERSPURT,
    JA,
    NEI
}
