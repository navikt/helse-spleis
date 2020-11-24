package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.godkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetaling
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.Oldtidsutbetalinger
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// Understands related payment activities for an Arbeidsgiver
internal class Utbetaling private constructor(
    private val id: UUID,
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
    private var forrige: UUID?,
    private var neste: UUID?
) : Aktivitetskontekst {
    private constructor(
        forrige: Utbetaling?,
        utbetalingstidslinje: Utbetalingstidslinje,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        tilstand: Tilstand,
        type: Utbetalingtype,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        vurdering: Vurdering?
    ) : this(
        UUID.randomUUID(),
        utbetalingstidslinje,
        arbeidsgiverOppdrag,
        personOppdrag,
        LocalDateTime.now(),
        tilstand,
        type,
        maksdato,
        forbrukteSykedager,
        gjenståendeSykedager,
        vurdering,
        null,
        null,
        null,
        null,
        null
    ) {
        forrige?.neste = this.id
        this.forrige = forrige?.id
    }

    private val observers = mutableListOf<UtbetalingObserver>()
    private var forrigeHendelse: ArbeidstakerHendelse? = null

    internal enum class Utbetalingtype { UTBETALING, ANNULLERING }

    private fun harHåndtert(hendelse: ArbeidstakerHendelse) =
        (hendelse == forrigeHendelse).also { forrigeHendelse = hendelse }

    internal fun register(observer: UtbetalingObserver) {
        observers.add(observer)
    }

    internal fun erUtbetalt() = tilstand == Utbetalt || tilstand == Annullert
    internal fun erAnnullering() = type == Utbetalingtype.ANNULLERING

    internal fun håndter(hendelse: Utbetalingsgodkjenning) {
        hendelse.kontekst(this)
        tilstand.godkjenn(this, hendelse)
    }

    internal fun håndter(utbetaling: UtbetalingHendelse) {
        if (!utbetaling.erRelevant(arbeidsgiverOppdrag.fagsystemId(), id)) return
        if (harHåndtert(utbetaling)) return
        utbetaling.kontekst(this)
        tilstand.kvittér(this, utbetaling)
    }

    internal fun utbetalingFeilet(hendelse: ArbeidstakerHendelse) {
        hendelse.kontekst(this)
        tilstand.utbetalingFeilet(this, hendelse)
    }

    internal fun håndter(utbetalingOverført: UtbetalingOverført) {
        if (!utbetalingOverført.erRelevant(arbeidsgiverOppdrag.fagsystemId(), id)) return
        if (harHåndtert(utbetalingOverført)) return
        utbetalingOverført.kontekst(this)
        tilstand.overført(this, utbetalingOverført)
    }

    internal fun utbetal(hendelse: ArbeidstakerHendelse) {
        hendelse.kontekst(this)
        tilstand.overfør(this, hendelse)
    }

    internal fun simuler(hendelse: ArbeidstakerHendelse) {
        hendelse.kontekst(this)
        tilstand.simuler(this, hendelse)
    }

    internal fun godkjenning(hendelse: ArbeidstakerHendelse, vedtaksperiode: Vedtaksperiode, aktivitetslogg: Aktivitetslogg) {
        hendelse.kontekst(this)
        tilstand.godkjenning(this, vedtaksperiode, aktivitetslogg, hendelse)
    }

    internal fun valider(simulering: Simulering): IAktivitetslogg {
        return simulering.valider(arbeidsgiverOppdrag.utenUendretLinjer())
    }

    internal fun ferdigstill(
        hendelse: ArbeidstakerHendelse,
        person: Person,
        periode: Periode,
        sykepengegrunnlag: Inntekt,
        inntekt: Inntekt,
        hendelseIder: List<UUID>
    ) {
        tilstand.avslutt(this, hendelse, person, periode, sykepengegrunnlag, inntekt, hendelseIder)
    }

    internal fun arbeidsgiverOppdrag() = arbeidsgiverOppdrag

    internal fun personOppdrag() = personOppdrag

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst("Utbetaling", mapOf("utbetalingId" to "$id"))

    private fun tilstand(neste: Tilstand, hendelse: ArbeidstakerHendelse) {
        tilstand.leaving(this, hendelse)
        tilstand = neste
        tilstand.entering(this, hendelse)
    }

    internal companion object {
        private const val systemident = "SPLEIS"

        internal fun lagUtbetaling(
            utbetalinger: List<Utbetaling>,
            fødselsnummer: String,
            organisasjonsnummer: String,
            utbetalingstidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int
        ): Utbetaling {
            val sisteUtbetalte = utbetalinger.utbetalte().lastOrNull()
            val arbeidsgiverOppdrag = buildArb(sisteUtbetalte?.arbeidsgiverOppdrag, organisasjonsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg)
            return Utbetaling(
                sisteUtbetalte?.takeIf { it.arbeidsgiverOppdrag.fagsystemId() == arbeidsgiverOppdrag.fagsystemId() },
                utbetalingstidslinje,
                arbeidsgiverOppdrag,
                buildPerson(fødselsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg, utbetalinger),
                Ubetalt,
                Utbetalingtype.UTBETALING,
                maksdato,
                forbrukteSykedager,
                gjenståendeSykedager,
                null
            )
        }

        internal fun annuller(utbetalinger: List<Utbetaling>, hendelse: AnnullerUtbetaling): Utbetaling? {
            if (utbetalinger.any { it.tilstand in listOf(Sendt, Overført) }) {
                hendelse.error("Kan ikke annullere: det finnes utbetalinger in-flight")
                return null
            }

            val kandidat = utbetalinger.reversed()
                .filter { utbetaling -> utbetaling.tilstand in listOf(Utbetalt, Annullert, UtbetalingFeilet) }
                .distinctBy { it.arbeidsgiverOppdrag().fagsystemId() }
                .filterNot { it.tilstand == Annullert }
                .firstOrNull()

            return when {
                kandidat == null -> {
                    hendelse.error("Finner ingen utbetaling å annullere")
                    null
                }
                // TODO: Håndterer kun arbeidsgiverOppdrag p.t. Må på sikt håndtere personOppdrag
                !hendelse.erRelevant(kandidat.arbeidsgiverOppdrag().fagsystemId()) -> {
                    hendelse.error("Kan ikke annullere: er ikke siste utbetaling.")
                    null
                }
                kandidat.tilstand == UtbetalingFeilet -> {
                    hendelse.error("Kan ikke annullere: siste utbetaling er feilet ${kandidat.arbeidsgiverOppdrag.fagsystemId()}")
                    null
                }
                kandidat.type == Utbetalingtype.ANNULLERING -> {
                    hendelse.info("Forsøkte å annullere en utbetaling som allerede er annullert")
                    null
                }
                else -> kandidat.annuller(hendelse)
            }
        }

        private fun buildArb(
            sisteUtbetalte: Oppdrag?,
            organisasjonsnummer: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: IAktivitetslogg
        ) = OppdragBuilder(tidslinje, organisasjonsnummer, SykepengerRefusjon, sisteDato)
            .result()
            .minus(sisteUtbetalte ?: Oppdrag(organisasjonsnummer, SykepengerRefusjon), aktivitetslogg)
            .also { oppdrag ->
                if (sisteUtbetalte?.fagsystemId() == oppdrag.fagsystemId()) oppdrag.nettoBeløp(sisteUtbetalte)
                aktivitetslogg.info(
                    if (oppdrag.isEmpty()) "Ingen utbetalingslinjer bygget"
                    else "Utbetalingslinjer bygget vellykket"
                )
            }

        private fun buildPerson(        // TODO("To be completed when payments to employees is supported")
            fødselsnummer: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: IAktivitetslogg,
            utbetalinger: List<Utbetaling>
        ) = Oppdrag(fødselsnummer, Sykepenger)

        private fun sisteGyldig(utbetalinger: List<Utbetaling>, default: () -> Oppdrag) =
            utbetalinger
                .utbetalte()
                .lastOrNull()
                ?.arbeidsgiverOppdrag
                ?: default()

        internal fun List<Utbetaling>.utbetalte() =
            filterNot { harAnnullerte(it.arbeidsgiverOppdrag.fagsystemId()) }
                .filter { it.erUtbetalt() }

        private fun List<Utbetaling>.harAnnullerte(fagsystemId: String) =
            filter { it.arbeidsgiverOppdrag.fagsystemId() == fagsystemId }
                .any { it.erAnnullering() }
    }

    internal fun accept(visitor: UtbetalingVisitor) {
        visitor.preVisitUtbetaling(this, tilstand, tidsstempel, arbeidsgiverOppdrag.nettoBeløp(), personOppdrag.nettoBeløp(), maksdato, forbrukteSykedager, gjenståendeSykedager)
        utbetalingstidslinje.accept(visitor)
        visitor.preVisitArbeidsgiverOppdrag(arbeidsgiverOppdrag)
        arbeidsgiverOppdrag.accept(visitor)
        visitor.postVisitArbeidsgiverOppdrag(arbeidsgiverOppdrag)
        visitor.preVisitPersonOppdrag(personOppdrag)
        personOppdrag.accept(visitor)
        vurdering?.accept(visitor)
        visitor.postVisitPersonOppdrag(personOppdrag)
        visitor.postVisitUtbetaling(this, tilstand, tidsstempel, arbeidsgiverOppdrag.nettoBeløp(), personOppdrag.nettoBeløp(), maksdato, forbrukteSykedager, gjenståendeSykedager)
    }
    internal fun utbetalingstidslinje() = utbetalingstidslinje

    internal fun utbetalingstidslinje(periode: Periode) = utbetalingstidslinje.subset(periode)

    internal fun annuller(hendelse: AnnullerUtbetaling) = Utbetaling(
        this,
        utbetalingstidslinje,
        arbeidsgiverOppdrag.emptied().minus(arbeidsgiverOppdrag, hendelse),
        personOppdrag.emptied().minus(personOppdrag, hendelse),
        Godkjent,
        Utbetalingtype.ANNULLERING,
        LocalDate.MAX,
        null,
        null,
        hendelse.vurdering()
    )

    internal fun append(organisasjonsnummer: String, oldtid: Oldtidsutbetalinger) {
        oldtid.add(organisasjonsnummer, utbetalingstidslinje)
    }

    internal fun append(organisasjonsnummer: String, bøtte: Historie.Historikkbøtte) {
        bøtte.add(organisasjonsnummer, utbetalingstidslinje)
    }

    private fun overfør(nesteTilstand: Tilstand, hendelse: ArbeidstakerHendelse) {
        overfør(hendelse)
        tilstand(nesteTilstand, hendelse)
    }

    private fun overfør(hendelse: ArbeidstakerHendelse) {
        vurdering?.overfør(hendelse, arbeidsgiverOppdrag, maksdato.takeUnless { type == Utbetalingtype.ANNULLERING })
    }

    private fun avslutt(
        hendelse: ArbeidstakerHendelse,
        person: Person,
        periode: Periode,
        sykepengegrunnlag: Inntekt,
        inntekt: Inntekt,
        hendelseIder: List<UUID>
    ) {
        val vurdering = checkNotNull(vurdering) { "Mangler vurdering" }
        vurdering.ferdigstill(hendelse, this, person, periode, sykepengegrunnlag, inntekt, hendelseIder)
        avsluttet = LocalDateTime.now()
    }

    internal interface Tilstand {
        fun godkjenn(
            utbetaling: Utbetaling,
            hendelse: Utbetalingsgodkjenning
        ) {
            throw IllegalStateException("Forventet ikke å utbetale på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun overfør(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            throw IllegalStateException("Forventet ikke å utbetale på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun annuller(utbetaling: Utbetaling, hendelse: AnnullerUtbetaling) {
            throw IllegalStateException("Forventet ikke å annullere på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun overført(utbetaling: Utbetaling, hendelse: UtbetalingOverført) {
            throw IllegalStateException("Forventet ikke overførtkvittering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelse) {
            throw IllegalStateException("Forventet ikke kvittering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun utbetalingFeilet(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            throw IllegalStateException("Forventet ikke utbetaling feilet på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun simuler(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            throw IllegalStateException("Forventet ikke simulering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun godkjenning(utbetaling: Utbetaling, vedtaksperiode: Vedtaksperiode, aktivitetslogg: Aktivitetslogg, hendelse: ArbeidstakerHendelse) {
            throw IllegalStateException("Forventet ikke å lage godkjenning på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun avslutt(
            utbetaling: Utbetaling,
            hendelse: ArbeidstakerHendelse,
            person: Person,
            periode: Periode,
            sykepengegrunnlag: Inntekt,
            inntekt: Inntekt,
            hendelseIder: List<UUID>
        ) {
            throw IllegalStateException("Forventet ikke avslutte på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun entering(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {}
        fun leaving(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {}
    }

    internal object Ubetalt : Tilstand {
        override fun godkjenn(utbetaling: Utbetaling, hendelse: Utbetalingsgodkjenning) {
            utbetaling.vurdering = hendelse.vurdering()
            utbetaling.tilstand(if (hendelse.valider().hasErrorsOrWorse()) IkkeGodkjent else Godkjent, hendelse)
        }

        override fun simuler(utbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg) {
            simulering(
                aktivitetslogg = aktivitetslogg,
                oppdrag = utbetaling.arbeidsgiverOppdrag.utenUendretLinjer(),
                maksdato = utbetaling.maksdato,
                saksbehandler = systemident
            )
        }

        override fun godkjenning(utbetaling: Utbetaling, vedtaksperiode: Vedtaksperiode, aktivitetslogg: Aktivitetslogg, hendelse: ArbeidstakerHendelse) {
            godkjenning(
                aktivitetslogg = hendelse,
                periodeFom = vedtaksperiode.periode().start,
                periodeTom = vedtaksperiode.periode().endInclusive,
                vedtaksperiodeaktivitetslogg = aktivitetslogg.logg(vedtaksperiode),
                periodetype = vedtaksperiode.periodetype()
            )
        }
    }

    internal object IkkeGodkjent : Tilstand {}
    internal object GodkjentUtenUtbetaling : Tilstand {}

    internal object Godkjent : Tilstand {
        override fun overfør(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            utbetaling.overfør(Sendt, hendelse)
        }

        override fun avslutt(
            utbetaling: Utbetaling,
            hendelse: ArbeidstakerHendelse,
            person: Person,
            periode: Periode,
            sykepengegrunnlag: Inntekt,
            inntekt: Inntekt,
            hendelseIder: List<UUID>
        ) {
            // TODO: korte perioder uten utbetaling blir ikke utbetalt, men blir Avsluttet automatisk.
            // skal vi fortsatt drive å sende Utbetalt-event da?
            utbetaling.avslutt(hendelse, person, periode, sykepengegrunnlag, inntekt, hendelseIder)
            utbetaling.tilstand(GodkjentUtenUtbetaling, hendelse)
        }
    }

    internal object Sendt : Tilstand {
        override fun overfør(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            utbetaling.overfør(hendelse)
        }

        override fun overført(utbetaling: Utbetaling, hendelse: UtbetalingOverført) {
            utbetaling.overføringstidspunkt = hendelse.overføringstidspunkt
            utbetaling.avstemmingsnøkkel = hendelse.avstemmingsnøkkel
            hendelse.info("Utbetalingen ble overført til Oppdrag/UR ${hendelse.overføringstidspunkt}, " +
                "og har fått avstemmingsnøkkel ${hendelse.avstemmingsnøkkel}")
            utbetaling.tilstand(Overført, hendelse)
        }
    }

    internal object Overført : Tilstand {
        override fun overfør(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            // trenger ikke overføre på nytt ettersom Spenn har godtatt oppdraget,
            // men vi må nok vente på at Oppdrag/UR sender ut Aksept-kvittering
        }

        override fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelse) {
            hendelse.valider()
            val erAnnullering = utbetaling.type == Utbetalingtype.ANNULLERING
            val nesteTilstand = when {
                hendelse.hasErrorsOrWorse() && !erAnnullering && hendelse.skalForsøkesIgjen() -> return // forventning om at vedtaksperioden står for retry
                hendelse.hasErrorsOrWorse() -> UtbetalingFeilet
                erAnnullering -> Annullert
                else -> Utbetalt
            }
            utbetaling.tilstand(nesteTilstand, hendelse)
        }

        override fun utbetalingFeilet(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            hendelse.error("Feilrespons fra oppdrag")
            utbetaling.tilstand(UtbetalingFeilet, hendelse)
        }
    }

    internal object Annullert : Tilstand {
        override fun entering(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            utbetaling.vurdering?.annullert(hendelse, utbetaling, utbetaling.arbeidsgiverOppdrag)
        }
    }

    internal object Utbetalt : Tilstand {
        override fun avslutt(
            utbetaling: Utbetaling,
            hendelse: ArbeidstakerHendelse,
            person: Person,
            periode: Periode,
            sykepengegrunnlag: Inntekt,
            inntekt: Inntekt,
            hendelseIder: List<UUID>
        ) {
            utbetaling.avslutt(hendelse, person, periode, sykepengegrunnlag, inntekt, hendelseIder)
        }
    }

    internal object UtbetalingFeilet : Tilstand {
        override fun utbetalingFeilet(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {}

        override fun overfør(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            utbetaling.overfør(Overført, hendelse)
        }
    }

    internal class Vurdering(
        private val ident: String,
        private val epost: String,
        private val tidspunkt: LocalDateTime,
        private val automatiskBehandling: Boolean
    ) {

        internal fun accept(visitor: UtbetalingVisitor) {
            visitor.visitVurdering(this, ident, epost, tidspunkt, automatiskBehandling)
        }

        internal fun annullert(hendelse: ArbeidstakerHendelse, utbetaling: Utbetaling, oppdrag: Oppdrag) {
            utbetaling.observers.forEach {
                it.utbetalingAnnullert(oppdrag, hendelse, tidspunkt, epost)
            }
        }

        internal fun overfør(hendelse: ArbeidstakerHendelse, oppdrag: Oppdrag, maksdato: LocalDate?) {
            utbetaling(hendelse, oppdrag, maksdato, ident)
        }

        fun ferdigstill(
            hendelse: ArbeidstakerHendelse,
            utbetaling: Utbetaling,
            person: Person,
            periode: Periode,
            sykepengegrunnlag: Inntekt,
            inntekt: Inntekt,
            hendelseIder: List<UUID>
        ) {
            person.vedtaksperiodeUtbetalt(
                tilUtbetaltEvent(
                    aktørId = hendelse.aktørId(),
                    fødselnummer = hendelse.fødselsnummer(),
                    orgnummer = hendelse.organisasjonsnummer(),
                    utbetaling = utbetaling,
                    utbetalingstidslinje = utbetaling.utbetalingstidslinje(periode),
                    sykepengegrunnlag = sykepengegrunnlag,
                    inntekt = inntekt,
                    forbrukteSykedager = requireNotNull(utbetaling.forbrukteSykedager),
                    gjenståendeSykedager = requireNotNull(utbetaling.gjenståendeSykedager),
                    godkjentAv = ident,
                    automatiskBehandling = automatiskBehandling,
                    hendelseIder = hendelseIder,
                    periode = periode,
                    maksdato = utbetaling.maksdato
                )
            )
        }
    }
}
