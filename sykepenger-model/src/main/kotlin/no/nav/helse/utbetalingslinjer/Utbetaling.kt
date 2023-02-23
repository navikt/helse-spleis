package no.nav.helse.utbetalingslinjer

import AnnullerUtbetalingPort
import GrunnbeløpsreguleringPort
import OverføringsinformasjonPort
import SimuleringPort
import UtbetalingHendelsePort
import UtbetalingpåminnelsePort
import UtbetalingsgodkjenningPort
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.godkjenning
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_11
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_12
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_13
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_14
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_15
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_6
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_9
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingslinjer.Oppdrag.Companion.trekkerTilbakePenger
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype.ANNULLERING
import no.nav.helse.utbetalingslinjer.Utbetalingkladd.Companion.finnKladd
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Utbetaling private constructor(
    private val id: UUID,
    private val korrelasjonsId: UUID,
    private val beregningId: UUID,
    private val periode: Periode,
    private val utbetalingstidslinje: Utbetalingstidslinje,
    private val arbeidsgiverOppdrag: Oppdrag,
    private val personOppdrag: Oppdrag,
    private val tidsstempel: LocalDateTime,
    private var tilstand: Tilstand,
    private val type: Utbetalingtype,
    private val maksdato: LocalDate,
    private val forbrukteSykedager: Int?,
    private val gjenståendeSykedager: Int?,
    private var vurdering: Vurdering?,
    private var overføringstidspunkt: LocalDateTime?,
    private var avstemmingsnøkkel: Long?,
    private var avsluttet: LocalDateTime?,
    private var oppdatert: LocalDateTime = tidsstempel
) : Aktivitetskontekst {
    internal constructor(
        beregningId: UUID,
        korrelerendeUtbetaling: Utbetaling?,
        periode: Periode,
        utbetalingstidslinje: Utbetalingstidslinje,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        type: Utbetalingtype,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?
    ) : this(
        UUID.randomUUID(),
        korrelerendeUtbetaling?.takeIf { arbeidsgiverOppdrag.tilhører(it.arbeidsgiverOppdrag) || personOppdrag.tilhører(it.personOppdrag) }?.korrelasjonsId ?: UUID.randomUUID(),
        beregningId,
        periode,
        utbetalingstidslinje,
        arbeidsgiverOppdrag,
        personOppdrag,
        LocalDateTime.now(),
        Ny,
        type,
        maksdato,
        forbrukteSykedager,
        gjenståendeSykedager,
        null,
        null,
        null,
        null
    )

    private val stønadsdager get() = Oppdrag.stønadsdager(arbeidsgiverOppdrag, personOppdrag)
    private val observers = mutableSetOf<UtbetalingObserver>()
    private var forrigeHendelse: IAktivitetslogg? = null

    private fun harHåndtert(hendelse: IAktivitetslogg) =
        (hendelse == forrigeHendelse).also { forrigeHendelse = hendelse }

    internal fun registrer(observer: UtbetalingObserver) {
        observers.add(observer)
    }

    internal fun gyldig() = tilstand !in setOf(Ny, Forkastet)
    internal fun erUbetalt() = tilstand == Ubetalt
    internal fun erUtbetalt() = tilstand == Utbetalt || tilstand == Annullert
    private fun erAktiv() = erAvsluttet() || erInFlight()
    private fun erAktivEllerUbetalt() = erAktiv() || erUbetalt()
    internal fun erInFlight() = tilstand in listOf(Godkjent, Sendt, Overført, UtbetalingFeilet)
    internal fun erAvsluttet() = erUtbetalt() || tilstand == GodkjentUtenUtbetaling
    internal fun erAvvist() = tilstand == IkkeGodkjent
    internal fun harFeilet() = tilstand == UtbetalingFeilet
    internal fun kanIkkeForsøkesPåNy() = Oppdrag.kanIkkeForsøkesPåNy(arbeidsgiverOppdrag, personOppdrag)
    private fun erAnnullering() = type == ANNULLERING

    internal fun reberegnUtbetaling(hendelse: IAktivitetslogg, hvisRevurdering: () -> Unit, hvisUtbetaling: () -> Unit) {
        check(kanIkkeForsøkesPåNy())
        forkast(hendelse)
        if (type == Utbetalingtype.REVURDERING) return hvisRevurdering()
        return hvisUtbetaling()
    }

    internal fun harNærliggendeUtbetaling(other: Periode): Boolean {
        if (arbeidsgiverOppdrag.isEmpty() && personOppdrag.isEmpty()) return false
        return periode.overlapperMed(other.oppdaterFom(other.start.minusDays(16)))
    }
    internal fun trekkerTilbakePenger() = listOf(arbeidsgiverOppdrag, personOppdrag).trekkerTilbakePenger()

    // this kan revurdere other gitt at fagsystemId == other.fagsystemId,
    // og at this er lik den siste aktive utbetalingen for fagsystemIden
    internal fun hørerSammen(other: Utbetaling) =
        this.korrelasjonsId == other.korrelasjonsId

    internal fun harUtbetalinger() =
        arbeidsgiverOppdrag.harUtbetalinger() || personOppdrag.harUtbetalinger()

    internal fun harDelvisRefusjon() = arbeidsgiverOppdrag.harUtbetalinger () && personOppdrag.harUtbetalinger()

    internal fun erKlarForGodkjenning() = personOppdrag.erKlarForGodkjenning() && arbeidsgiverOppdrag.erKlarForGodkjenning()

    internal fun opprett(hendelse: IAktivitetslogg) {
        tilstand.opprett(this, hendelse)
    }

    internal fun håndter(hendelse: UtbetalingsgodkjenningAdapter) {
        if (!hendelse.erRelevant(id)) return
        hendelse.valider()
        godkjenn(hendelse, hendelse.vurdering())
    }

    internal fun håndter(hendelse: GrunnbeløpsreguleringPort) {
        godkjenn(hendelse, Vurdering.automatiskGodkjent)
    }

    internal fun håndter(utbetaling: UtbetalingHendelsePort) {
        if (!utbetaling.erRelevant(arbeidsgiverOppdrag.fagsystemId(), personOppdrag.fagsystemId(), id)) return
        if (harHåndtert(utbetaling)) return
        utbetaling.kontekst(this)
        tilstand.kvittér(this, utbetaling)
    }

    internal fun håndter(utbetalingOverført: OverføringsinformasjonPort) {
        if (!utbetalingOverført.erRelevant(arbeidsgiverOppdrag.fagsystemId(), personOppdrag.fagsystemId(), id)) return
        if (harHåndtert(utbetalingOverført)) return
        utbetalingOverført.kontekst(this)
        tilstand.overført(this, utbetalingOverført)
    }

    internal fun håndter(simulering: SimuleringPort) {
        if (!simulering.erRelevantForUtbetaling(id)) return
        personOppdrag.håndter(simulering)
        arbeidsgiverOppdrag.håndter(simulering)
    }

    internal fun simuler(hendelse: IAktivitetslogg) {
        hendelse.kontekst(this)
        tilstand.simuler(this, hendelse)
    }

    internal fun godkjenning(
        hendelse: IAktivitetslogg,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
        periodetype: UtbetalingPeriodetype,
        førstegangsbehandling: Boolean,
        inntektskilde: UtbetalingInntektskilde,
        orgnummereMedRelevanteArbeidsforhold: List<String>
    ) {
        hendelse.kontekst(this)
        tilstand.godkjenning(
            this,
            periode,
            skjæringstidspunkt,
            periodetype,
            førstegangsbehandling,
            inntektskilde,
            orgnummereMedRelevanteArbeidsforhold,
            hendelse
        )
    }

    internal fun håndter(påminnelse: UtbetalingpåminnelsePort) {
        if (!påminnelse.erRelevant(id)) return
        påminnelse.kontekst(this)
        if (!påminnelse.gjelderStatus(Utbetalingstatus.fraTilstand(tilstand))) return
        tilstand.håndter(this, påminnelse)
    }

    internal fun gjelderFor(hendelse: UtbetalingHendelsePort) =
        hendelse.erRelevant(arbeidsgiverOppdrag.fagsystemId(), personOppdrag.fagsystemId(), id)

    internal fun gjelderFor(hendelse: UtbetalingsgodkjenningPort) =
        hendelse.erRelevant(id)

    internal fun valider(simulering: SimuleringPort): IAktivitetslogg {
        arbeidsgiverOppdrag.valider(simulering)
        personOppdrag.valider(simulering)
        return simulering
    }

    internal fun build(builder: UtbetalingVedtakFattetBuilder) {
        builder.utbetalingId(id)
        vurdering?.build(builder)
    }

    internal fun håndter(hendelse: AnnullerUtbetalingPort) {
        godkjenn(hendelse, hendelse.vurdering())
    }

    internal fun annuller(hendelse: AnnullerUtbetalingPort): Utbetaling? {
        if (!hendelse.erRelevant(arbeidsgiverOppdrag.fagsystemId())) {
            hendelse.info("Kan ikke annullere: hendelsen er ikke relevant for ${arbeidsgiverOppdrag.fagsystemId()}.")
            hendelse.funksjonellFeil(RV_UT_15)
            return null
        }
        return tilstand.annuller(this, hendelse)
    }

    internal fun forkast(hendelse: IAktivitetslogg) {
        hendelse.kontekst(this)
        tilstand.forkast(this, hendelse)
    }

    internal fun arbeidsgiverOppdrag() = arbeidsgiverOppdrag

    internal fun personOppdrag() = personOppdrag

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst("Utbetaling", mapOf("utbetalingId" to "$id"))

    private fun godkjenn(hendelse: IAktivitetslogg, vurdering: Vurdering) {
        hendelse.kontekst(this)
        tilstand.godkjenn(this, hendelse, vurdering)
    }

    private fun tilstand(neste: Tilstand, hendelse: IAktivitetslogg) {
        oppdatert = LocalDateTime.now()
        if (Oppdrag.ingenFeil(arbeidsgiverOppdrag, personOppdrag) && !Oppdrag.synkronisert(arbeidsgiverOppdrag, personOppdrag)) return hendelse.info("Venter på status på det andre oppdraget før vi kan gå videre")
        val forrigeTilstand = tilstand
        tilstand = neste
        observers.forEach {
            it.utbetalingEndret(id, type, arbeidsgiverOppdrag, personOppdrag, forrigeTilstand, neste, korrelasjonsId)
        }
        tilstand.entering(this, hendelse)
    }


    internal companion object {

        val log: Logger = LoggerFactory.getLogger("Utbetaling")
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        private const val systemident = "SPLEIS"

        internal fun lagUtbetaling(
            utbetalinger: List<Utbetaling>,
            fødselsnummer: String,
            beregningId: UUID,
            organisasjonsnummer: String,
            utbetalingstidslinje: Utbetalingstidslinje,
            periode: Periode,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int,
            type: Utbetalingtype = Utbetalingtype.UTBETALING
        ): Utbetaling {
            val bb = UtbetalingkladderBuilder(utbetalingstidslinje, organisasjonsnummer, fødselsnummer)
            val oppdragene = bb.build()

            val kladdene = oppdragene.finnKladd(periode)
            val kladden = kladdene.firstOrNull() ?: Utbetalingkladd(
                periode = periode,
                arbeidsgiveroppdrag = Oppdrag(organisasjonsnummer, SykepengerRefusjon),
                personoppdrag = Oppdrag(fødselsnummer, Sykepenger)
            )
            if (kladdene.size > 1) {
                sikkerlogg.error("Vedtaksperioden $periode brytes opp i flere utbetalinger, mest sannsynlig en overlappende Infotrygd-utbetaling",
                    keyValue("fødselsnummer", fødselsnummer),
                    keyValue("organisasjonsnummer", organisasjonsnummer)
                )
            }

            val forrigeUtbetalte = kladden.forrigeUtbetalte(utbetalinger)
            val korrelerendeUtbetaling = forrigeUtbetalte.singleOrNull()

            val nyUtbetaling = if (korrelerendeUtbetaling == null) {
                if (forrigeUtbetalte.isEmpty()) {
                    // dette er nok første gangen vi utbetaler denne perioden
                    kladden.begrensTil(periode)
                } else {
                    // mer enn én forrige utbetalte for samme periode, dette
                    // er nok en merge-hendelse: tidslinjen har endret seg slik at to uavhengige
                    // oppdragsperioder nå sees på som samme sak.
                    // Eksempelvis: tidligere utbetaling i Infotrygd er fjernet
                    // eller at vi har revurdert inn sykedager slik at to arbeidsgiverperioder nå er én felles AGP
                    throw IllegalStateException("Dette støtter vi ikke helt enda: må annullere/opphøre ${forrigeUtbetalte.size - 1} oppdrag for å kunne kjøre frem igjen ett.")
                }
            } else {
                if (kladden.opphørerHale(korrelerendeUtbetaling.periode)) {
                    kladden.begrensTil(aktivitetslogg, periode.oppdaterFom(korrelerendeUtbetaling.periode), korrelerendeUtbetaling.arbeidsgiverOppdrag, korrelerendeUtbetaling.personOppdrag)
                } else {
                    kladden.begrensTilOgKopier(aktivitetslogg, periode.oppdaterFom(korrelerendeUtbetaling.periode), korrelerendeUtbetaling.arbeidsgiverOppdrag, korrelerendeUtbetaling.personOppdrag)
                }
            }

            return nyUtbetaling.lagUtbetaling(type, korrelerendeUtbetaling, beregningId, utbetalingstidslinje, maksdato, forbrukteSykedager, gjenståendeSykedager)
        }

        internal fun finnUtbetalingForAnnullering(utbetalinger: List<Utbetaling>, hendelse: AnnullerUtbetalingPort): Utbetaling? {
            return utbetalinger.aktive().lastOrNull() ?: run {
                hendelse.funksjonellFeil(RV_UT_4)
                return null
            }
        }
        internal fun List<Utbetaling>.aktive() = grupperUtbetalinger(Utbetaling::erAktiv)
        private fun List<Utbetaling>.aktiveMedUbetalte() = grupperUtbetalinger(Utbetaling::erAktivEllerUbetalt)
        internal fun List<Utbetaling>.aktive(periode: Periode) = this
            .aktive()
            .filter { utbetaling ->
                utbetaling.periode.overlapperMed(periode) || utbetaling.periode.erRettFør(periode.start)
            }
        private fun List<Utbetaling>.grupperUtbetalinger(filter: (Utbetaling) -> Boolean) =
            this
                .filter { it.gyldig() }
                .groupBy { it.korrelasjonsId }
                .filter { (_, utbetalinger) -> utbetalinger.any(filter) }
                .map { (_, utbetalinger) -> utbetalinger.sortedBy { it.tidsstempel } }
                .mapNotNull { it.last(filter) }
                .sortedBy { it.tidsstempel }
                .filterNot(Utbetaling::erAnnullering)

        internal fun List<Utbetaling>.tillaterOpprettelseAvUtbetaling(other: Utbetaling): Boolean {
            if (other.erAnnullering()) return true // må godta annulleringer ettersom de vil rydde opp i nettopp overlappende utbetalinger
            val overlappendeUtbetalingsperioder = overlappendeUtbetalingsperioder(other)
            if (overlappendeUtbetalingsperioder.isNotEmpty()) {
                sikkerlogg.warn("Vi har opprettet en utbetaling med periode ${other.periode} & korrelasjonsId ${other.korrelasjonsId} som overlapper med eksisterende utbetalinger $overlappendeUtbetalingsperioder")
            }
            return overlappendeUtbetalingsperioder.isEmpty()
        }

        private fun List<Utbetaling>.overlappendeUtbetalingsperioder(other: Utbetaling): List<Periode> {
            return aktive()
                .filterNot { it.hørerSammen(other) }
                .filter { it.periode.overlapperMed(other.periode) }
                .map { it.periode }
        }

        internal fun List<Utbetaling>.harNærliggendeUtbetaling(periode: Periode) =
            aktiveMedUbetalte().any { it.harNærliggendeUtbetaling(periode) }

        internal fun List<Utbetaling>.harId(id: UUID) = any { it.id == id }
        internal fun ferdigUtbetaling(
            id: UUID,
            korrelasjonsId: UUID,
            beregningId: UUID,
            opprinneligPeriode: Periode,
            utbetalingstidslinje: Utbetalingstidslinje,
            arbeidsgiverOppdrag: Oppdrag,
            personOppdrag: Oppdrag,
            tidsstempel: LocalDateTime,
            tilstand: Tilstand,
            utbetalingtype: Utbetalingtype,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?,
            vurdering: Vurdering?,
            overføringstidspunkt: LocalDateTime?,
            avstemmingsnøkkel: Long?,
            avsluttet: LocalDateTime?,
            oppdatert: LocalDateTime
        ): Utbetaling = Utbetaling(
            id = id,
            korrelasjonsId = korrelasjonsId,
            beregningId = beregningId,
            periode = opprinneligPeriode,
            utbetalingstidslinje = utbetalingstidslinje,
            arbeidsgiverOppdrag = arbeidsgiverOppdrag,
            personOppdrag = personOppdrag,
            tidsstempel = tidsstempel,
            tilstand = tilstand,
            type = utbetalingtype,
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            vurdering = vurdering,
            overføringstidspunkt = overføringstidspunkt,
            avstemmingsnøkkel = avstemmingsnøkkel,
            avsluttet = avsluttet,
            oppdatert = oppdatert
        )

        internal fun ferdigVurdering(
            godkjent: Boolean,
            ident: String,
            epost: String,
            tidspunkt: LocalDateTime,
            automatiskBehandling: Boolean
        ): Vurdering = Vurdering(godkjent, ident, epost, tidspunkt, automatiskBehandling)

        // kan forkaste dersom ingen utbetalinger er utbetalt/in flight, eller de er annullert
        fun kanForkastes(vedtaksperiodeUtbetalinger: List<Utbetaling>, arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
            val annulleringer = arbeidsgiverUtbetalinger.filter { it.erAnnullering() }
            return vedtaksperiodeUtbetalinger.filter { it.erAktiv() }.all { utbetaling ->
                annulleringer.any { annullering -> annullering.hørerSammen(utbetaling) }
            }
        }
    }

    internal fun accept(visitor: UtbetalingVisitor) {
        visitor.preVisitUtbetaling(
            this,
            id,
            korrelasjonsId,
            type,
            tilstand,
            periode,
            tidsstempel,
            oppdatert,
            arbeidsgiverOppdrag.nettoBeløp(),
            personOppdrag.nettoBeløp(),
            maksdato,
            forbrukteSykedager,
            gjenståendeSykedager,
            stønadsdager,
            beregningId,
            overføringstidspunkt,
            avsluttet,
            avstemmingsnøkkel
        )
        utbetalingstidslinje.accept(visitor)
        visitor.preVisitArbeidsgiverOppdrag(arbeidsgiverOppdrag)
        arbeidsgiverOppdrag.accept(visitor)
        visitor.postVisitArbeidsgiverOppdrag(arbeidsgiverOppdrag)
        visitor.preVisitPersonOppdrag(personOppdrag)
        personOppdrag.accept(visitor)
        visitor.postVisitPersonOppdrag(personOppdrag)
        vurdering?.accept(visitor)
        visitor.postVisitUtbetaling(
            this,
            id,
            korrelasjonsId,
            type,
            tilstand,
            periode,
            tidsstempel,
            oppdatert,
            arbeidsgiverOppdrag.nettoBeløp(),
            personOppdrag.nettoBeløp(),
            maksdato,
            forbrukteSykedager,
            gjenståendeSykedager,
            stønadsdager,
            beregningId,
            overføringstidspunkt,
            avsluttet,
            avstemmingsnøkkel
        )
    }

    internal fun utbetalingstidslinje() = utbetalingstidslinje
    internal fun utbetalingstidslinje(periode: Periode) = utbetalingstidslinje.subset(periode)

    private fun overfør(nesteTilstand: Tilstand, hendelse: IAktivitetslogg) {
        overfør(hendelse)
        tilstand(nesteTilstand, hendelse)
    }

    private fun overfør(hendelse: IAktivitetslogg) {
        vurdering?.overfør(hendelse, arbeidsgiverOppdrag, maksdato.takeUnless { type == ANNULLERING })
        vurdering?.overfør(hendelse, personOppdrag, maksdato.takeUnless { type == ANNULLERING })
    }

    private fun håndterKvittering(hendelse: UtbetalingHendelsePort) {
        hendelse.valider()
        val nesteTilstand = when {
            tilstand == Sendt && hendelse.skalForsøkesIgjen() -> return // utbetaling gjør retry ved neste påminnelse
            hendelse.harFunksjonelleFeilEllerVerre() -> UtbetalingFeilet
            type == ANNULLERING -> Annullert
            else -> Utbetalt
        }
        tilstand(nesteTilstand, hendelse)
    }

    internal fun nyVedtaksperiodeUtbetaling(vedtaksperiodeId: UUID) {
        observers.forEach { it.nyVedtaksperiodeUtbetaling(this.id, vedtaksperiodeId) }
    }

    internal fun overlapperMed(other: Utbetaling): Boolean {
        return this.periode.overlapperMed(other.periode)
    }
    internal fun erNyereEnn(other: LocalDateTime): Boolean {
        return other <= tidsstempel
    }
    private fun lagreOverføringsinformasjon(hendelse: IAktivitetslogg, avstemmingsnøkkel: Long, tidspunkt: LocalDateTime) {
        hendelse.info("Utbetalingen ble overført til Oppdrag/UR $tidspunkt, og har fått avstemmingsnøkkel $avstemmingsnøkkel.\n")
        if (this.avstemmingsnøkkel != null && this.avstemmingsnøkkel != avstemmingsnøkkel)
            hendelse.info("Avstemmingsnøkkel har endret seg.\nTidligere verdi: ${this.avstemmingsnøkkel}")
        if (this.overføringstidspunkt == null) this.overføringstidspunkt = tidspunkt
        if (this.avstemmingsnøkkel == null) this.avstemmingsnøkkel = avstemmingsnøkkel
    }
    override fun toString() = "$type(${Utbetalingstatus.fraTilstand(tilstand)}) - $periode"

    internal interface Tilstand {
        fun forkast(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {}

        fun opprett(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            hendelse.info("Forventet ikke å opprette utbetaling i tilstand=${this::class.simpleName}")
            hendelse.funksjonellFeil(RV_UT_6)
        }

        fun godkjenn(
            utbetaling: Utbetaling,
            hendelse: IAktivitetslogg,
            vurdering: Vurdering
        ) {
            hendelse.info("Forventet ikke godkjenning på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            hendelse.funksjonellFeil(RV_UT_7)
        }

        fun annuller(utbetaling: Utbetaling, hendelse: AnnullerUtbetalingPort): Utbetaling? {
            hendelse.info("Forventet ikke å annullere på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            hendelse.funksjonellFeil(RV_UT_9)
            return null
        }

        fun overført(utbetaling: Utbetaling, hendelse: OverføringsinformasjonPort) {
            hendelse.info("Forventet ikke overførtkvittering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            hendelse.funksjonellFeil(RV_UT_10)
        }

        fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelsePort) {
            hendelse.info("Forventet ikke kvittering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            hendelse.funksjonellFeil(RV_UT_11)
        }

        fun håndter(utbetaling: Utbetaling, påminnelse: UtbetalingpåminnelsePort) {
            påminnelse.info("Utbetaling ble påminnet, men gjør ingenting")
        }

        fun simuler(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Forventet ikke simulering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            aktivitetslogg.funksjonellFeil(RV_UT_12)
        }

        fun godkjenning(
            utbetaling: Utbetaling,
            periode: Periode,
            skjæringstidspunkt: LocalDate,
            periodetype: UtbetalingPeriodetype,
            førstegangsbehandling: Boolean,
            inntektskilde: UtbetalingInntektskilde,
            orgnummereMedRelevanteArbeidsforhold: List<String>,
            hendelse: IAktivitetslogg
        ) {
            hendelse.info("Forventet ikke å lage godkjenning på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            hendelse.funksjonellFeil(RV_UT_13)
        }

        fun entering(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {}
    }

    internal object Ny : Tilstand {
        override fun opprett(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            utbetaling.tilstand(Ubetalt, hendelse)
        }
    }

    internal object Ubetalt : Tilstand {
        override fun forkast(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            hendelse.info("Forkaster utbetaling")
            utbetaling.tilstand(Forkastet, hendelse)
        }

        override fun godkjenn(utbetaling: Utbetaling, hendelse: IAktivitetslogg, vurdering: Vurdering) {
            utbetaling.vurdering = vurdering
            utbetaling.tilstand(vurdering.avgjør(utbetaling), hendelse)
        }

        override fun simuler(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            utbetaling.arbeidsgiverOppdrag.simuler(aktivitetslogg, utbetaling.maksdato, systemident)
            utbetaling.personOppdrag.simuler(aktivitetslogg, utbetaling.maksdato, systemident)
        }

        override fun godkjenning(
            utbetaling: Utbetaling,
            periode: Periode,
            skjæringstidspunkt: LocalDate,
            periodetype: UtbetalingPeriodetype,
            førstegangsbehandling: Boolean,
            inntektskilde: UtbetalingInntektskilde,
            orgnummereMedRelevanteArbeidsforhold: List<String>,
            hendelse: IAktivitetslogg
        ) {
            godkjenning(
                aktivitetslogg = hendelse,
                periodeFom = periode.start,
                periodeTom = periode.endInclusive,
                skjæringstidspunkt = skjæringstidspunkt,
                periodetype = periodetype.name,
                førstegangsbehandling = førstegangsbehandling,
                utbetalingtype = utbetaling.type.name,
                inntektskilde = inntektskilde.name,
                orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
            )
        }
    }

    internal object GodkjentUtenUtbetaling : Tilstand {

        override fun entering(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            check(!utbetaling.harUtbetalinger())
            utbetaling.vurdering?.avsluttetUtenUtbetaling(utbetaling)
            utbetaling.avsluttet = LocalDateTime.now()
        }

        override fun annuller(utbetaling: Utbetaling, hendelse: AnnullerUtbetalingPort) = Utbetaling(
            utbetaling.beregningId,
            utbetaling,
            utbetaling.periode,
            utbetaling.utbetalingstidslinje,
            utbetaling.arbeidsgiverOppdrag.annuller(hendelse),
            utbetaling.personOppdrag.annuller(hendelse),
            ANNULLERING,
            LocalDate.MAX,
            null,
            null
        ).also { hendelse.info("Oppretter annullering med id ${it.id}") }
    }

    internal object Godkjent : Tilstand {
        override fun entering(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            utbetaling.overfør(Sendt, hendelse)
        }

        override fun håndter(utbetaling: Utbetaling, påminnelse: UtbetalingpåminnelsePort) {
            utbetaling.overfør(Sendt, påminnelse)
        }
    }

    internal object Sendt : Tilstand {
        private val makstid = Duration.ofDays(7)

        override fun håndter(utbetaling: Utbetaling, påminnelse: UtbetalingpåminnelsePort) {
            if (påminnelse.harOversteget(makstid)) {
                påminnelse.info("Gir opp å prøve utbetaling på nytt etter ${makstid.toHours()} timer")
                påminnelse.funksjonellFeil(RV_UT_14)
                return utbetaling.tilstand(UtbetalingFeilet, påminnelse)
            }
            utbetaling.overfør(påminnelse)
        }

        override fun overført(utbetaling: Utbetaling, hendelse: OverføringsinformasjonPort) {
            utbetaling.lagreOverføringsinformasjon(hendelse, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.tilstand(Overført, hendelse)
        }

        override fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelsePort) {
            utbetaling.lagreOverføringsinformasjon(hendelse, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.håndterKvittering(hendelse)
        }
    }

    internal object Overført : Tilstand {
        override fun håndter(utbetaling: Utbetaling, påminnelse: UtbetalingpåminnelsePort) {
            utbetaling.overfør(påminnelse)
        }

        override fun overført(utbetaling: Utbetaling, hendelse: OverføringsinformasjonPort) {
            hendelse.info("Mottok overførtkvittering, men står allerede i Overført. Venter på kvittering.")
            utbetaling.lagreOverføringsinformasjon(hendelse, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
        }

        override fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelsePort) {
            utbetaling.lagreOverføringsinformasjon(hendelse, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.håndterKvittering(hendelse)
        }
    }

    internal object Annullert : Tilstand {
        override fun entering(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            utbetaling.vurdering?.annullert(utbetaling)
            utbetaling.avsluttet = LocalDateTime.now()
        }
    }

    internal object Utbetalt : Tilstand {
        override fun entering(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            utbetaling.vurdering?.utbetalt(utbetaling)
            utbetaling.avsluttet = LocalDateTime.now()
        }

        override fun annuller(utbetaling: Utbetaling, hendelse: AnnullerUtbetalingPort) =
            Utbetaling(
                utbetaling.beregningId,
                utbetaling,
                utbetaling.periode,
                utbetaling.utbetalingstidslinje,
                utbetaling.arbeidsgiverOppdrag.annuller(hendelse),
                utbetaling.personOppdrag.annuller(hendelse),
                ANNULLERING,
                LocalDate.MAX,
                null,
                null
            ).also { hendelse.info("Oppretter annullering med id ${it.id}") }
    }

    internal object UtbetalingFeilet : Tilstand {
        override fun forkast(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            hendelse.info("Forkaster feilet utbetaling")
            utbetaling.tilstand(Forkastet, hendelse)
        }

        override fun håndter(utbetaling: Utbetaling, påminnelse: UtbetalingpåminnelsePort) {
            påminnelse.info("Forsøker å sende utbetalingen på nytt")
            utbetaling.overfør(Overført, påminnelse)
        }

        override fun overført(utbetaling: Utbetaling, hendelse: OverføringsinformasjonPort) {
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
        }

        override fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelsePort) {
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
        }
    }

    internal object IkkeGodkjent : Tilstand
    internal object Forkastet : Tilstand

    class Vurdering(
        private val godkjent: Boolean,
        private val ident: String,
        private val epost: String,
        private val tidspunkt: LocalDateTime,
        private val automatiskBehandling: Boolean
    ) {
        internal companion object {
            val automatiskGodkjent get() = Vurdering(true, systemident, "tbd@nav.no", LocalDateTime.now(), true)
        }

        internal fun accept(visitor: UtbetalingVisitor) {
            visitor.visitVurdering(this, ident, epost, tidspunkt, automatiskBehandling, godkjent)
        }

        internal fun annullert(utbetaling: Utbetaling) {
            utbetaling.observers.forEach {
                it.utbetalingAnnullert(
                    id = utbetaling.id,
                    korrelasjonsId = utbetaling.korrelasjonsId,
                    periode = utbetaling.periode,
                    personFagsystemId = utbetaling.personOppdrag.fagsystemId(),
                    godkjenttidspunkt = tidspunkt,
                    saksbehandlerEpost = epost,
                    saksbehandlerIdent = ident,
                    arbeidsgiverFagsystemId = utbetaling.arbeidsgiverOppdrag.fagsystemId()
                )
            }
        }

        internal fun utbetalt(utbetaling: Utbetaling) {
            utbetaling.observers.forEach {
                it.utbetalingUtbetalt(
                    utbetaling.id,
                    utbetaling.korrelasjonsId,
                    utbetaling.type,
                    utbetaling.periode,
                    utbetaling.maksdato,
                    utbetaling.forbrukteSykedager!!,
                    utbetaling.gjenståendeSykedager!!,
                    utbetaling.stønadsdager,
                    utbetaling.arbeidsgiverOppdrag,
                    utbetaling.personOppdrag,
                    epost,
                    tidspunkt,
                    automatiskBehandling,
                    utbetaling.utbetalingstidslinje,
                    ident
                )
            }
        }

        internal fun avsluttetUtenUtbetaling(utbetaling: Utbetaling) {
            utbetaling.observers.forEach {
                it.utbetalingUtenUtbetaling(
                    utbetaling.id,
                    utbetaling.korrelasjonsId,
                    utbetaling.type,
                    utbetaling.periode,
                    utbetaling.maksdato,
                    utbetaling.forbrukteSykedager!!,
                    utbetaling.gjenståendeSykedager!!,
                    utbetaling.stønadsdager,
                    utbetaling.personOppdrag,
                    ident,
                    utbetaling.arbeidsgiverOppdrag,
                    tidspunkt,
                    automatiskBehandling,
                    utbetaling.utbetalingstidslinje,
                    epost
                )
            }
        }

        internal fun overfør(hendelse: IAktivitetslogg, oppdrag: Oppdrag, maksdato: LocalDate?) {
            oppdrag.overfør(hendelse, maksdato, ident)
        }

        internal fun avgjør(utbetaling: Utbetaling) =
            when {
                !godkjent -> IkkeGodkjent
                utbetaling.harUtbetalinger() -> Godkjent
                utbetaling.type == ANNULLERING -> Annullert
                else -> GodkjentUtenUtbetaling
            }

        internal fun build(builder: UtbetalingVedtakFattetBuilder) {
            builder.utbetalingVurdert(tidspunkt)
        }
    }

    enum class Utbetalingtype { UTBETALING, ETTERUTBETALING, ANNULLERING, REVURDERING, FERIEPENGER }
}

interface UtbetalingVedtakFattetBuilder {
    fun utbetalingVurdert(tidspunkt: LocalDateTime): UtbetalingVedtakFattetBuilder
    fun utbetalingId(id: UUID): UtbetalingVedtakFattetBuilder
}