package no.nav.helse.utbetalingslinjer

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.Grunnbeløpsregulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingpåminnelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.Aktivitetskontekst
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.godkjenning
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Periodetype
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Understands related payment activities for an Arbeidsgiver
internal class Utbetaling private constructor(
    private val id: UUID,
    private val korrelasjonsId: UUID,
    private val beregningId: UUID,
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
    private constructor(
        beregningId: UUID,
        korrelerendeUtbetaling: Utbetaling?,
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
        utbetalingstidslinje,
        arbeidsgiverOppdrag,
        personOppdrag,
        LocalDateTime.now(),
        Ubetalt,
        type,
        maksdato,
        forbrukteSykedager,
        gjenståendeSykedager,
        null,
        null,
        null,
        null
    )

    private constructor(
        sisteAktive: Utbetaling?,
        fødselsnummer: String,
        beregningId: UUID,
        organisasjonsnummer: String,
        utbetalingstidslinje: Utbetalingstidslinje,
        type: Utbetalingtype,
        sisteDato: LocalDate,
        aktivitetslogg: IAktivitetslogg,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        forrige: Utbetaling?
    ) : this(
        beregningId,
        forrige ?: sisteAktive,
        utbetalingstidslinje.kutt(sisteDato),
        byggArbeidsgiveroppdrag(sisteAktive?.arbeidsgiverOppdrag, organisasjonsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg, forrige?.arbeidsgiverOppdrag),
        byggPersonoppdrag(sisteAktive?.personOppdrag, fødselsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg, forrige?.personOppdrag),
        type,
        maksdato,
        forbrukteSykedager,
        gjenståendeSykedager
    )

    private val oppdragsperiode = Oppdrag.periode(arbeidsgiverOppdrag, personOppdrag)
    private val periode get() = oppdragsperiode?.oppdaterTom(utbetalingstidslinje.periode()) ?: utbetalingstidslinje.periode().oppdaterFom(LocalDate.MIN)
    private val stønadsdager get() = Oppdrag.stønadsdager(arbeidsgiverOppdrag, personOppdrag)
    private val observers = mutableSetOf<UtbetalingObserver>()
    private var forrigeHendelse: ArbeidstakerHendelse? = null

    private fun harHåndtert(hendelse: ArbeidstakerHendelse) =
        (hendelse == forrigeHendelse).also { forrigeHendelse = hendelse }

    internal fun registrer(observer: UtbetalingObserver) {
        observers.add(observer)
    }

    internal fun gyldig() = tilstand != Forkastet
    internal fun erUbetalt() = tilstand == Ubetalt
    internal fun erUtbetalt() = tilstand == Utbetalt || tilstand == Annullert
    private fun erAktiv() = erAvsluttet() || erInFlight()
    private fun erInFlight() = tilstand in listOf(Godkjent, Sendt, Overført, UtbetalingFeilet)
    internal fun erAvsluttet() = erUtbetalt() || tilstand == GodkjentUtenUtbetaling
    internal fun erAvvist() = tilstand == IkkeGodkjent
    internal fun harFeilet() = tilstand == UtbetalingFeilet
    internal fun kanIkkeForsøkesPåNy() = Oppdrag.kanIkkeForsøkesPåNy(arbeidsgiverOppdrag, personOppdrag)
    private fun erAnnullering() = type == Utbetalingtype.ANNULLERING

    internal fun reberegnUtbetaling(hvisRevurdering: () -> Unit, hvisUtbetaling: () -> Unit) {
        check(kanIkkeForsøkesPåNy())
        if (type == Utbetalingtype.REVURDERING) return hvisRevurdering()
        return hvisUtbetaling()
    }

    internal fun harNærliggendeUtbetaling(other: Periode): Boolean {
        if (arbeidsgiverOppdrag.isEmpty() && personOppdrag.isEmpty()) return false
        return periode.overlapperMed(other.oppdaterFom(other.start.minusDays(16)))
    }

    // this kan revurdere other gitt at fagsystemId == other.fagsystemId,
    // og at this er lik den siste aktive utbetalingen for fagsystemIden
    internal fun hørerSammen(other: Utbetaling) =
        this.korrelasjonsId == other.korrelasjonsId

    internal fun harUtbetalinger() =
        arbeidsgiverOppdrag.harUtbetalinger() || personOppdrag.harUtbetalinger()
    internal fun harDelvisRefusjon() = arbeidsgiverOppdrag.harUtbetalinger () && personOppdrag.harUtbetalinger()

    internal fun harBrukerutbetaling() = personOppdrag.harUtbetalinger()

    internal fun erKlarForGodkjenning() = personOppdrag.erKlarForGodkjenning() && arbeidsgiverOppdrag.erKlarForGodkjenning()

    internal fun kanForkastes(utbetalinger: List<Utbetaling>) =
        this.tilstand in listOf(Ubetalt, IkkeGodkjent, Forkastet) || utbetalinger.filter { it.erAnnullering() }.any(::hørerSammen)

    internal fun håndter(hendelse: Utbetalingsgodkjenning) {
        if (!hendelse.erRelevant(id)) return
        hendelse.valider()
        godkjenn(hendelse, hendelse.vurdering())
    }

    internal fun håndter(hendelse: Grunnbeløpsregulering) {
        godkjenn(hendelse, Vurdering.automatiskGodkjent)
    }

    internal fun håndter(utbetaling: UtbetalingHendelse) {
        if (!utbetaling.erRelevant(arbeidsgiverOppdrag.fagsystemId(), personOppdrag.fagsystemId(), id)) return
        if (harHåndtert(utbetaling)) return
        utbetaling.kontekst(this)
        tilstand.kvittér(this, utbetaling)
    }

    internal fun håndter(utbetalingOverført: UtbetalingOverført) {
        if (!utbetalingOverført.erRelevant(arbeidsgiverOppdrag.fagsystemId(), personOppdrag.fagsystemId(), id)) return
        if (harHåndtert(utbetalingOverført)) return
        utbetalingOverført.kontekst(this)
        tilstand.overført(this, utbetalingOverført)
    }

    internal fun håndter(simulering: Simulering) {
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
        vedtaksperiode: Vedtaksperiode,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        aktiveVedtaksperioder: List<Aktivitetslogg.Aktivitet.AktivVedtaksperiode>,
        orgnummereMedRelevanteArbeidsforhold: List<String>,
        arbeidsforholdId: String?,
        aktivitetslogg: Aktivitetslogg
    ) {
        hendelse.kontekst(this)
        tilstand.godkjenning(this, vedtaksperiode, skjæringstidspunkt, periodetype, aktiveVedtaksperioder, orgnummereMedRelevanteArbeidsforhold, arbeidsforholdId, aktivitetslogg, hendelse)
    }

    internal fun håndter(påminnelse: Utbetalingpåminnelse) {
        if (!påminnelse.erRelevant(id)) return
        påminnelse.kontekst(this)
        if (!påminnelse.gjelderStatus(Utbetalingstatus.fraTilstand(tilstand))) return
        tilstand.håndter(this, påminnelse)
    }

    internal fun gjelderFor(hendelse: UtbetalingHendelse) =
        hendelse.erRelevant(arbeidsgiverOppdrag.fagsystemId(), personOppdrag.fagsystemId(), id)

    internal fun gjelderFor(hendelse: Utbetalingsgodkjenning) =
        hendelse.erRelevant(id)

    internal fun valider(simulering: Simulering): IAktivitetslogg {
        arbeidsgiverOppdrag.valider(simulering)
        personOppdrag.valider(simulering)
        return simulering
    }

    internal fun build(builder: VedtakFattetBuilder) {
        builder.utbetalingId(id)
        vurdering?.build(builder)
    }

    internal fun håndter(hendelse: AnnullerUtbetaling) {
        godkjenn(hendelse, hendelse.vurdering())
    }

    internal fun annuller(hendelse: AnnullerUtbetaling): Utbetaling? {
        if (!hendelse.erRelevant(arbeidsgiverOppdrag.fagsystemId())) {
            hendelse.error("Kan ikke annullere: hendelsen er ikke relevant for ${arbeidsgiverOppdrag.fagsystemId()}.")
            return null
        }
        return tilstand.annuller(this, hendelse)
    }

    internal fun etterutbetale(hendelse: Grunnbeløpsregulering, utbetalingstidslinje: Utbetalingstidslinje): Utbetaling? {
        return tilstand.etterutbetale(this, hendelse, utbetalingstidslinje)
    }

    internal fun forkast(hendelse: IAktivitetslogg) {
        hendelse.kontekst(this)
        tilstand.forkast(this, hendelse)
    }

    internal fun arbeidsgiverOppdrag() = arbeidsgiverOppdrag

    internal fun personOppdrag() = personOppdrag

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst("Utbetaling", mapOf("utbetalingId" to "$id"))

    private fun godkjenn(hendelse: ArbeidstakerHendelse, vurdering: Vurdering) {
        hendelse.kontekst(this)
        tilstand.godkjenn(this, hendelse, vurdering)
    }

    private fun tilstand(neste: Tilstand, hendelse: IAktivitetslogg) {
        oppdatert = LocalDateTime.now()
        if (Oppdrag.ingenFeil(arbeidsgiverOppdrag, personOppdrag) && !Oppdrag.synkronisert(arbeidsgiverOppdrag, personOppdrag)) return hendelse.info("Venter på status på det andre oppdraget før vi kan gå videre")
        val forrigeTilstand = tilstand
        tilstand = neste
        observers.forEach {
            it.utbetalingEndret(hendelse.hendelseskontekst(), id, type, arbeidsgiverOppdrag, personOppdrag, forrigeTilstand, neste)
        }
        tilstand.entering(this, hendelse)
    }

    internal companion object {

        val log: Logger = LoggerFactory.getLogger("Utbetaling")

        private const val systemident = "SPLEIS"

        internal fun lagRevurdering(
            utbetalinger: List<Utbetaling>,
            fødselsnummer: String,
            beregningId: UUID,
            organisasjonsnummer: String,
            utbetalingstidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int,
            forrige: List<Utbetaling> = emptyList()
        ): Utbetaling {
            return Utbetaling(
                utbetalinger.aktive().lastOrNull(),
                fødselsnummer,
                beregningId,
                organisasjonsnummer,
                utbetalingstidslinje,
                Utbetalingtype.REVURDERING,
                sisteDato,
                aktivitetslogg,
                maksdato,
                forbrukteSykedager,
                gjenståendeSykedager,
                forrige.aktive().lastOrNull()
            )
        }

        internal fun lagUtbetaling(
            utbetalinger: List<Utbetaling>,
            fødselsnummer: String,
            beregningId: UUID,
            organisasjonsnummer: String,
            utbetalingstidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int,
            forrige: Utbetaling? = null
        ): Utbetaling = lagUtbetaling(
                utbetalinger,
                fødselsnummer,
                beregningId,
                organisasjonsnummer,
                utbetalingstidslinje,
                sisteDato,
                aktivitetslogg,
                maksdato,
                forbrukteSykedager,
                gjenståendeSykedager,
                forrige,
                Utbetalingtype.UTBETALING
            )

        private fun lagUtbetaling(
            utbetalinger: List<Utbetaling>,
            fødselsnummer: String,
            beregningId: UUID,
            organisasjonsnummer: String,
            utbetalingstidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int,
            forrige: Utbetaling? = null,
            type: Utbetalingtype
        ): Utbetaling {
            return Utbetaling(
                utbetalinger.aktive().lastOrNull(),
                fødselsnummer,
                beregningId,
                organisasjonsnummer,
                utbetalingstidslinje,
                type,
                sisteDato,
                aktivitetslogg,
                maksdato,
                forbrukteSykedager,
                gjenståendeSykedager,
                forrige?.takeIf { it.erAktiv() || it.kanIkkeForsøkesPåNy() }
            )
        }

        internal fun finnUtbetalingForJustering(
            utbetalinger: List<Utbetaling>,
            hendelse: Grunnbeløpsregulering
        ): Utbetaling? {
            val sisteUtbetalte = utbetalinger.aktive().lastOrNull {
                hendelse.erRelevant(it.arbeidsgiverOppdrag.fagsystemId()) ||
                hendelse.erRelevant(it.personOppdrag.fagsystemId())
            } ?: return null.also {
                hendelse.info("Fant ingen utbetalte utbetalinger. Dette betyr trolig at fagsystemiden er annullert.")
            }
            if (!sisteUtbetalte.utbetalingstidslinje.er6GBegrenset()) {
                hendelse.info("Utbetalingen for perioden ${sisteUtbetalte.periode} er ikke begrenset av 6G")
                return null
            }
            return sisteUtbetalte
        }

        internal fun finnUtbetalingForAnnullering(utbetalinger: List<Utbetaling>, hendelse: AnnullerUtbetaling): Utbetaling? {
            return utbetalinger.utbetalte().lastOrNull() ?: run {
                hendelse.error("Finner ingen utbetaling å annullere")
                return null
            }
        }

        private fun byggOppdrag(
            sisteAktive: Oppdrag?,
            mottaker: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: IAktivitetslogg,
            forrige: Oppdrag?,
            fagområde: Fagområde
        ): Oppdrag = OppdragBuilder(tidslinje, mottaker, fagområde, sisteDato, forrige?.fagsystemId()).build(forrige?.takeUnless { Oppdrag.kanIkkeForsøkesPåNy(it) } ?: sisteAktive, aktivitetslogg)

        private fun byggArbeidsgiveroppdrag(
            sisteAktive: Oppdrag?,
            organisasjonsnummer: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: IAktivitetslogg,
            forrige: Oppdrag?
        ) = byggOppdrag(sisteAktive, organisasjonsnummer, tidslinje, sisteDato, aktivitetslogg, forrige, SykepengerRefusjon)

        private fun byggPersonoppdrag(
            sisteAktive: Oppdrag?,
            fødselsnummer: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: IAktivitetslogg,
            forrige: Oppdrag?
        ): Oppdrag {
            return byggOppdrag(sisteAktive, fødselsnummer, tidslinje, sisteDato, aktivitetslogg, forrige, Sykepenger)
        }

        internal fun List<Utbetaling>.aktive() = grupperUtbetalinger(Utbetaling::erAktiv)
        private fun List<Utbetaling>.utbetalte() = grupperUtbetalinger { it.erUtbetalt() || it.erInFlight() }
        private fun List<Utbetaling>.grupperUtbetalinger(filter: (Utbetaling) -> Boolean) =
            this.groupBy { it.arbeidsgiverOppdrag.fagsystemId() }
                .map { (_, utbetalinger) -> utbetalinger.sortedBy { it.tidsstempel } }
                .sortedBy { it.first().tidsstempel }
                .mapNotNull { it.lastOrNull(filter) }
                .filterNot(Utbetaling::erAnnullering)

        internal fun sykdomstidslinje(utbetalinger: List<Utbetaling>, sykdomstidslinje: Sykdomstidslinje): Sykdomstidslinje {
            return utbetalinger.utbetalte().fold(sykdomstidslinje) { result, utbetaling ->
                utbetaling.sykdomstidslinje(result)
            }
        }

        internal fun List<Utbetaling>.harNærliggendeUtbetaling(periode: Periode) =
            aktive().any { it.harNærliggendeUtbetaling(periode) }

        internal fun List<Utbetaling>.utbetaltTidslinje() =
            utbetalte()
                .map { it.utbetalingstidslinje }
                .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

        internal fun List<Utbetaling>.harId(id: UUID) = any { it.id == id }
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

    internal fun utbetalingstidslinje(periode: Periode) =
        utbetalingstidslinje.avgrensSisteArbeidsgiverperiode(periode)

    internal fun sykdomstidslinje(other: Sykdomstidslinje): Sykdomstidslinje {
        return Utbetalingstidslinje.konverter(utbetalingstidslinje).merge(other, replace)
    }

    private fun overfør(nesteTilstand: Tilstand, hendelse: IAktivitetslogg) {
        overfør(hendelse)
        tilstand(nesteTilstand, hendelse)
    }

    private fun overfør(hendelse: IAktivitetslogg) {
        vurdering?.overfør(hendelse, arbeidsgiverOppdrag, maksdato.takeUnless { type == Utbetalingtype.ANNULLERING })
        vurdering?.overfør(hendelse, personOppdrag, maksdato.takeUnless { type == Utbetalingtype.ANNULLERING })
    }

    private fun håndterKvittering(hendelse: UtbetalingHendelse) {
        hendelse.valider()
        val nesteTilstand = when {
            tilstand == Sendt && hendelse.skalForsøkesIgjen() -> return // utbetaling gjør retry ved neste påminnelse
            hendelse.hasErrorsOrWorse() -> UtbetalingFeilet
            type == Utbetalingtype.ANNULLERING -> Annullert
            else -> Utbetalt
        }
        tilstand(nesteTilstand, hendelse)
    }

    internal fun overlapperMed(other: Utbetaling): Boolean {
        return this.periode.overlapperMed(other.periode)
    }

    internal fun erEldreEnn(other: LocalDateTime): Boolean {
        return other > tidsstempel
    }

    private fun lagreOverføringsinformasjon(hendelse: ArbeidstakerHendelse, avstemmingsnøkkel: Long, tidspunkt: LocalDateTime) {
        hendelse.info("Utbetalingen ble overført til Oppdrag/UR $tidspunkt, og har fått avstemmingsnøkkel $avstemmingsnøkkel.\n")
        if (this.avstemmingsnøkkel != null && this.avstemmingsnøkkel != avstemmingsnøkkel)
            hendelse.info("Avstemmingsnøkkel har endret seg.\nTidligere verdi: ${this.avstemmingsnøkkel}")
        if (this.overføringstidspunkt == null) this.overføringstidspunkt = tidspunkt
        if (this.avstemmingsnøkkel == null) this.avstemmingsnøkkel = avstemmingsnøkkel
    }
    override fun toString() = "$type(${Utbetalingstatus.fraTilstand(tilstand)}) - $periode"

    internal interface Tilstand {
        fun forkast(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            hendelse.info("Forkaster ikke utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun godkjenn(
            utbetaling: Utbetaling,
            hendelse: IAktivitetslogg,
            vurdering: Vurdering
        ) {
            hendelse.error("Forventet ikke godkjenning på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun etterutbetale(utbetaling: Utbetaling, hendelse: Grunnbeløpsregulering, utbetalingstidslinje: Utbetalingstidslinje): Utbetaling? {
            hendelse.error("Forventet ikke å etterutbetale på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            return null
        }

        fun annuller(utbetaling: Utbetaling, hendelse: AnnullerUtbetaling): Utbetaling? {
            hendelse.error("Forventet ikke å annullere på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
            return null
        }

        fun overført(utbetaling: Utbetaling, hendelse: UtbetalingOverført) {
            hendelse.error("Forventet ikke overførtkvittering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelse) {
            hendelse.error("Forventet ikke kvittering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun håndter(utbetaling: Utbetaling, påminnelse: Utbetalingpåminnelse) {
            påminnelse.info("Utbetaling ble påminnet, men gjør ingenting")
        }

        fun simuler(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.error("Forventet ikke simulering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun godkjenning(
            utbetaling: Utbetaling,
            vedtaksperiode: Vedtaksperiode,
            skjæringstidspunkt: LocalDate,
            periodetype: Periodetype,
            aktiveVedtaksperioder: List<Aktivitetslogg.Aktivitet.AktivVedtaksperiode>,
            orgnummereMedRelevanteArbeidsforhold: List<String>,
            arbeidsforholdId: String?,
            aktivitetslogg: Aktivitetslogg,
            hendelse: IAktivitetslogg
        ) {
            hendelse.error("Forventet ikke å lage godkjenning på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun entering(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {}
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
            vedtaksperiode: Vedtaksperiode,
            skjæringstidspunkt: LocalDate,
            periodetype: Periodetype,
            aktiveVedtaksperioder: List<Aktivitetslogg.Aktivitet.AktivVedtaksperiode>,
            orgnummereMedRelevanteArbeidsforhold: List<String>,
            arbeidsforholdId: String?,
            aktivitetslogg: Aktivitetslogg,
            hendelse: IAktivitetslogg
        ) {
            godkjenning(
                aktivitetslogg = hendelse,
                periodeFom = vedtaksperiode.periode().start,
                periodeTom = vedtaksperiode.periode().endInclusive,
                skjæringstidspunkt = skjæringstidspunkt,
                vedtaksperiodeaktivitetslogg = aktivitetslogg.logg(vedtaksperiode),
                periodetype = periodetype,
                utbetalingtype = utbetaling.type,
                inntektskilde = vedtaksperiode.inntektskilde(),
                aktiveVedtaksperioder =  aktiveVedtaksperioder,
                orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold,
                arbeidsforholdId = arbeidsforholdId
            )
        }
    }

    internal object GodkjentUtenUtbetaling : Tilstand {

        override fun entering(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            check(!utbetaling.harUtbetalinger())
            utbetaling.vurdering?.avsluttetUtenUtbetaling(hendelse.hendelseskontekst(), utbetaling)
            utbetaling.avsluttet = LocalDateTime.now()
        }
    }

    internal object Godkjent : Tilstand {
        override fun entering(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            utbetaling.overfør(Sendt, hendelse)
        }

        override fun håndter(utbetaling: Utbetaling, påminnelse: Utbetalingpåminnelse) {
            utbetaling.overfør(Sendt, påminnelse)
        }
    }

    internal object Sendt : Tilstand {
        private val makstid = Duration.ofDays(7)

        override fun håndter(utbetaling: Utbetaling, påminnelse: Utbetalingpåminnelse) {
            if (påminnelse.harOversteget(makstid)) {
                påminnelse.error("Gir opp å prøve utbetaling på nytt etter ${makstid.toHours()} timer")
                return utbetaling.tilstand(UtbetalingFeilet, påminnelse)
            }
            utbetaling.overfør(påminnelse)
        }

        override fun overført(utbetaling: Utbetaling, hendelse: UtbetalingOverført) {
            utbetaling.lagreOverføringsinformasjon(hendelse, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.tilstand(Overført, hendelse)
        }

        override fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelse) {
            utbetaling.lagreOverføringsinformasjon(hendelse, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.håndterKvittering(hendelse)
        }
    }

    internal object Overført : Tilstand {
        override fun håndter(utbetaling: Utbetaling, påminnelse: Utbetalingpåminnelse) {
            utbetaling.overfør(påminnelse)
        }

        override fun overført(utbetaling: Utbetaling, hendelse: UtbetalingOverført) {
            hendelse.info("Mottok overførtkvittering, men står allerede i Overført. Venter på kvittering.")
            utbetaling.lagreOverføringsinformasjon(hendelse, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
        }

        override fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelse) {
            utbetaling.lagreOverføringsinformasjon(hendelse, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.håndterKvittering(hendelse)
        }
    }

    internal object Annullert : Tilstand {
        override fun entering(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            utbetaling.vurdering?.annullert(hendelse.hendelseskontekst(), utbetaling)
            utbetaling.avsluttet = LocalDateTime.now()
        }
    }

    internal object Utbetalt : Tilstand {
        override fun entering(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            utbetaling.vurdering?.utbetalt(hendelse.hendelseskontekst(), utbetaling)
            utbetaling.avsluttet = LocalDateTime.now()
        }

        override fun annuller(utbetaling: Utbetaling, hendelse: AnnullerUtbetaling) =
            Utbetaling(
                utbetaling.beregningId,
                utbetaling,
                utbetaling.utbetalingstidslinje,
                utbetaling.arbeidsgiverOppdrag.annuller(hendelse),
                utbetaling.personOppdrag.annuller(hendelse),
                Utbetalingtype.ANNULLERING,
                LocalDate.MAX,
                null,
                null
            ).also { hendelse.info("Oppretter annullering med id ${it.id}") }

        override fun etterutbetale(utbetaling: Utbetaling, hendelse: Grunnbeløpsregulering, utbetalingstidslinje: Utbetalingstidslinje) =
            Utbetaling(
                sisteAktive = null,
                fødselsnummer = hendelse.fødselsnummer(),
                beregningId = utbetaling.beregningId,
                organisasjonsnummer = hendelse.organisasjonsnummer(),
                utbetalingstidslinje = utbetalingstidslinje.kutt(utbetaling.periode.endInclusive),
                type = Utbetalingtype.ETTERUTBETALING,
                sisteDato = utbetaling.periode.endInclusive,
                aktivitetslogg = hendelse,
                maksdato = utbetaling.maksdato,
                forbrukteSykedager = requireNotNull(utbetaling.forbrukteSykedager),
                gjenståendeSykedager = requireNotNull(utbetaling.gjenståendeSykedager),
                forrige = utbetaling
            )
                .takeIf { it.arbeidsgiverOppdrag.harUtbetalinger() }
                ?.also {
                    if (it.arbeidsgiverOppdrag.sistedato != utbetaling.arbeidsgiverOppdrag.sistedato)
                        hendelse.severe("Etterutbetaling har utvidet eller kortet ned oppdraget")
                }
    }

    internal object UtbetalingFeilet : Tilstand {
        override fun forkast(utbetaling: Utbetaling, hendelse: IAktivitetslogg) {
            hendelse.info("Forkaster feilet utbetaling")
            utbetaling.tilstand(Forkastet, hendelse)
        }

        override fun håndter(utbetaling: Utbetaling, påminnelse: Utbetalingpåminnelse) {
            påminnelse.info("Forsøker å sende utbetalingen på nytt")
            utbetaling.overfør(Overført, påminnelse)
        }

        override fun overført(utbetaling: Utbetaling, hendelse: UtbetalingOverført) {
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
        }

        override fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelse) {
            utbetaling.arbeidsgiverOppdrag.lagreOverføringsinformasjon(hendelse)
            utbetaling.personOppdrag.lagreOverføringsinformasjon(hendelse)
        }
    }

    internal object IkkeGodkjent : Tilstand
    internal object Forkastet : Tilstand

    internal class Vurdering(
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

        internal fun annullert(hendelseskontekst: Hendelseskontekst, utbetaling: Utbetaling) {
            utbetaling.observers.forEach {
                it.utbetalingAnnullert(
                    hendelseskontekst = hendelseskontekst,
                    id = utbetaling.id,
                    korrelasjonsId = utbetaling.korrelasjonsId,
                    periode = utbetaling.periode,
                    arbeidsgiverFagsystemId = utbetaling.arbeidsgiverOppdrag.takeIf(Oppdrag::harUtbetalinger)?.fagsystemId(),
                    personFagsystemId = utbetaling.personOppdrag.takeIf(Oppdrag::harUtbetalinger)?.fagsystemId(),
                    godkjenttidspunkt = tidspunkt,
                    saksbehandlerEpost = epost,
                    saksbehandlerIdent = ident
                )
            }
        }

        internal fun utbetalt(hendelseskontekst: Hendelseskontekst, utbetaling: Utbetaling) {
            utbetaling.observers.forEach {
                it.utbetalingUtbetalt(
                    hendelseskontekst,
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

        internal fun avsluttetUtenUtbetaling(hendelseskontekst: Hendelseskontekst, utbetaling: Utbetaling) {
            utbetaling.observers.forEach {
                it.utbetalingUtenUtbetaling(
                    hendelseskontekst,
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
                else -> GodkjentUtenUtbetaling
            }

        internal fun build(builder: VedtakFattetBuilder) {
            builder.utbetalingVurdert(tidspunkt)
        }
    }

    enum class Utbetalingtype { UTBETALING, ETTERUTBETALING, ANNULLERING, REVURDERING, FERIEPENGER }
}
