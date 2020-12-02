package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.godkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetaling
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.Oldtidsutbetalinger
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
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
    private var avsluttet: LocalDateTime?
) : Aktivitetskontekst {
    private constructor(
        utbetalingstidslinje: Utbetalingstidslinje,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        type: Utbetalingtype,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?
    ) : this(
        UUID.randomUUID(),
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
        forrige: Utbetaling?,
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingstidslinje: Utbetalingstidslinje,
        type: Utbetalingtype,
        sisteDato: LocalDate,
        aktivitetslogg: IAktivitetslogg,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int
    ) : this(
        utbetalingstidslinje,
        buildArb(forrige?.arbeidsgiverOppdrag, organisasjonsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg),
        buildPerson(fødselsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg, emptyList()),
        type,
        maksdato,
        forbrukteSykedager,
        gjenståendeSykedager
    )

    internal val periode get() =
        arbeidsgiverOppdrag.førstedato til utbetalingstidslinje.sisteDato()

    private val observers = mutableSetOf<UtbetalingObserver>()
    private var forrigeHendelse: ArbeidstakerHendelse? = null

    internal enum class Utbetalingtype { UTBETALING, ETTERUTBETALING, ANNULLERING }

    private fun harHåndtert(hendelse: ArbeidstakerHendelse) =
        (hendelse == forrigeHendelse).also { forrigeHendelse = hendelse }

    internal fun register(observer: UtbetalingObserver) {
        observers.add(observer)
    }

    internal fun erUtbetalt() = tilstand == Utbetalt || tilstand == Annullert
    private fun erAktiv() = erUtbetalt() || tilstand in listOf(Godkjent, Sendt, Overført, UtbetalingFeilet)
    internal fun erAvsluttet() = tilstand in listOf(GodkjentUtenUtbetaling, Utbetalt, Annullert)
    internal fun erAvvist() = tilstand in listOf(IkkeGodkjent)
    internal fun harFeilet() = tilstand == UtbetalingFeilet
    internal fun erAnnullering() = type == Utbetalingtype.ANNULLERING
    internal fun erEtterutbetaling() = type == Utbetalingtype.ETTERUTBETALING

    internal fun harUtbetalinger() =
        arbeidsgiverOppdrag.utenUendretLinjer().isNotEmpty() ||
        personOppdrag.utenUendretLinjer().isNotEmpty()

    internal fun håndter(hendelse: Utbetalingsgodkjenning) {
        if (!hendelse.erRelevant(id)) return
        hendelse.valider()
        godkjenn(hendelse, hendelse.vurdering())
    }

    internal fun håndter(hendelse: Grunnbeløpsregulering) {
        godkjenn(hendelse, Vurdering.automatiskGodkjent)
    }

    internal fun håndter(utbetaling: UtbetalingHendelse) {
        if (!utbetaling.erRelevant(arbeidsgiverOppdrag.fagsystemId(), id)) return
        if (harHåndtert(utbetaling)) return
        utbetaling.kontekst(this)
        tilstand.kvittér(this, utbetaling)
    }

    internal fun håndter(utbetalingOverført: UtbetalingOverført) {
        if (!utbetalingOverført.erRelevant(arbeidsgiverOppdrag.fagsystemId(), id)) return
        if (harHåndtert(utbetalingOverført)) return
        utbetalingOverført.kontekst(this)
        tilstand.overført(this, utbetalingOverført)
    }

    internal fun simuler(hendelse: ArbeidstakerHendelse) {
        hendelse.kontekst(this)
        tilstand.simuler(this, hendelse)
    }

    internal fun godkjenning(hendelse: ArbeidstakerHendelse, vedtaksperiode: Vedtaksperiode, aktivitetslogg: Aktivitetslogg) {
        hendelse.kontekst(this)
        tilstand.godkjenning(this, vedtaksperiode, aktivitetslogg, hendelse)
    }

    internal fun håndter(påminnelse: Utbetalingpåminnelse) {
        if (!påminnelse.erRelevant(id)) return
        påminnelse.kontekst(this)
        if (!påminnelse.gjelderStatus(Utbetalingstatus.fraTilstand(tilstand))) return
        tilstand.håndter(this, påminnelse)
    }

    internal fun gjelderFor(hendelse: UtbetalingHendelse) =
        hendelse.erRelevant(arbeidsgiverOppdrag.fagsystemId(), id)

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

    internal fun forkast(hendelse: ArbeidstakerHendelse) {
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

    private fun tilstand(neste: Tilstand, hendelse: ArbeidstakerHendelse) {
        val forrigeTilstand = tilstand
        tilstand.leaving(this, hendelse)
        tilstand = neste
        observers.forEach {
            it.utbetalingEndret(id, type, arbeidsgiverOppdrag, personOppdrag, forrigeTilstand, neste)
        }
        tilstand.entering(this, hendelse)
    }

    internal companion object {
        val log: Logger = LoggerFactory.getLogger("Utbetaling")

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
            return Utbetaling(
                utbetalinger.aktive().lastOrNull(),
                fødselsnummer,
                organisasjonsnummer,
                utbetalingstidslinje,
                Utbetalingtype.UTBETALING,
                sisteDato,
                aktivitetslogg,
                maksdato,
                forbrukteSykedager,
                gjenståendeSykedager
            )
        }

        internal fun finnUtbetalingForJustering(
            utbetalinger: List<Utbetaling>,
            hendelse: Grunnbeløpsregulering
        ): Utbetaling? {
            val sisteUtbetalte = utbetalinger.aktive().lastOrNull { hendelse.erRelevant(it.arbeidsgiverOppdrag.fagsystemId()) } ?: return null.also {
                hendelse.info("Fant ingen utbetalte utbetalinger. Dette betyr trolig at fagsystemiden er annullert.")
            }
            val periode = sisteUtbetalte.arbeidsgiverOppdrag.førstedato til sisteUtbetalte.utbetalingstidslinje.sisteDato()
            if (!sisteUtbetalte.utbetalingstidslinje.er6GBegrenset()) {
                hendelse.info("Utbetalingen for perioden $periode er ikke begrenset av 6G")
                return null
            }
            return sisteUtbetalte
        }

        internal fun finnUtbetalingForAnnullering(utbetalinger: List<Utbetaling>, hendelse: AnnullerUtbetaling): Utbetaling? {
            return utbetalinger.aktive().lastOrNull() ?: run {
                hendelse.error("Finner ingen utbetaling å annullere")
                return null
            }
        }

        internal fun List<Utbetaling>.kronologisk() = this.sortedBy { it.tidsstempel }

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

        internal fun List<Utbetaling>.aktive() =
            this.groupBy { it.arbeidsgiverOppdrag.fagsystemId() }
                .map { (_, utbetalinger) -> utbetalinger.kronologisk() }
                .sortedBy { it.first().tidsstempel }
                .mapNotNull { it.lastOrNull(Utbetaling::erAktiv) }
                .filterNot(Utbetaling::erAnnullering)

        internal fun List<Utbetaling>.utbetaltTidslinje() =
            aktive()
                .map { it.utbetalingstidslinje }
                .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)
    }

    internal fun accept(visitor: UtbetalingVisitor) {
        visitor.preVisitUtbetaling(this, id, tilstand, tidsstempel, arbeidsgiverOppdrag.nettoBeløp(), personOppdrag.nettoBeløp(), maksdato, forbrukteSykedager, gjenståendeSykedager)
        utbetalingstidslinje.accept(visitor)
        visitor.preVisitArbeidsgiverOppdrag(arbeidsgiverOppdrag)
        arbeidsgiverOppdrag.accept(visitor)
        visitor.postVisitArbeidsgiverOppdrag(arbeidsgiverOppdrag)
        visitor.preVisitPersonOppdrag(personOppdrag)
        personOppdrag.accept(visitor)
        vurdering?.accept(visitor)
        visitor.postVisitPersonOppdrag(personOppdrag)
        visitor.postVisitUtbetaling(this, id, tilstand, tidsstempel, arbeidsgiverOppdrag.nettoBeløp(), personOppdrag.nettoBeløp(), maksdato, forbrukteSykedager, gjenståendeSykedager)
    }

    internal fun utbetalingstidslinje() = utbetalingstidslinje

    internal fun utbetalingstidslinje(periode: Periode) = utbetalingstidslinje.subset(periode)

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

    internal interface Tilstand {
        fun forkast(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            hendelse.info("Forkaster ikke utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun godkjenn(
            utbetaling: Utbetaling,
            hendelse: ArbeidstakerHendelse,
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
            throw IllegalStateException("Forventet ikke overførtkvittering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelse) {
            throw IllegalStateException("Forventet ikke kvittering på utbetaling=${utbetaling.id} i tilstand=${this::class.simpleName}")
        }

        fun håndter(utbetaling: Utbetaling, påminnelse: Utbetalingpåminnelse) {
            påminnelse.info("Utbetaling ble påminnet, men gjør ingenting")
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
        override fun forkast(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            hendelse.info("Forkaster utbetaling")
            utbetaling.tilstand(Forkastet, hendelse)
        }

        override fun godkjenn(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse, vurdering: Vurdering) {
            utbetaling.vurdering = vurdering
            utbetaling.tilstand(vurdering.avgjør(utbetaling), hendelse)
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

    internal object GodkjentUtenUtbetaling : Tilstand {
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
        }
    }

    internal object Godkjent : Tilstand {
        override fun entering(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
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
            lagreOverføringsinformasjon(utbetaling, hendelse, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.tilstand(Overført, hendelse)
        }

        override fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelse) {
            lagreOverføringsinformasjon(utbetaling, hendelse, hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
            utbetaling.håndterKvittering(hendelse)
        }

        private fun lagreOverføringsinformasjon(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse, avstemmingsnøkkel: Long, tidspunkt: LocalDateTime) {
            utbetaling.overføringstidspunkt = tidspunkt
            utbetaling.avstemmingsnøkkel = avstemmingsnøkkel
            hendelse.info("Utbetalingen ble overført til Oppdrag/UR $tidspunkt, " +
                "og har fått avstemmingsnøkkel $avstemmingsnøkkel")
        }
    }

    internal object Overført : Tilstand {
        override fun håndter(utbetaling: Utbetaling, påminnelse: Utbetalingpåminnelse) {
            // trenger ikke overføre på nytt ettersom Spenn har godtatt oppdraget,
            // men vi må nok vente på at Oppdrag/UR sender ut Aksept-kvittering
        }

        override fun kvittér(utbetaling: Utbetaling, hendelse: UtbetalingHendelse) {
            utbetaling.håndterKvittering(hendelse)
        }
    }

    internal object Annullert : Tilstand {
        override fun entering(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            utbetaling.vurdering?.annullert(hendelse, utbetaling, utbetaling.arbeidsgiverOppdrag)
        }
    }

    internal object Utbetalt : Tilstand {
        override fun entering(utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            utbetaling.vurdering?.utbetalt(hendelse, utbetaling)
        }

        override fun annuller(utbetaling: Utbetaling, hendelse: AnnullerUtbetaling) =
            Utbetaling(
                utbetaling.utbetalingstidslinje,
                utbetaling.arbeidsgiverOppdrag.emptied().minus(utbetaling.arbeidsgiverOppdrag, hendelse),
                utbetaling.personOppdrag.emptied().minus(utbetaling.personOppdrag, hendelse),
                Utbetalingtype.ANNULLERING,
                LocalDate.MAX,
                null,
                null
            ).also { hendelse.info("Oppretter annullering med id ${it.id}") }

        override fun etterutbetale(utbetaling: Utbetaling, hendelse: Grunnbeløpsregulering, utbetalingstidslinje: Utbetalingstidslinje) =
            Utbetaling(
                forrige = utbetaling,
                fødselsnummer = hendelse.fødselsnummer(),
                organisasjonsnummer = hendelse.organisasjonsnummer(),
                utbetalingstidslinje = utbetalingstidslinje.kutt(utbetaling.periode.endInclusive),
                type = Utbetalingtype.ETTERUTBETALING,
                sisteDato = utbetaling.periode.endInclusive,
                aktivitetslogg = hendelse,
                maksdato = utbetaling.maksdato,
                forbrukteSykedager = requireNotNull(utbetaling.forbrukteSykedager),
                gjenståendeSykedager = requireNotNull(utbetaling.gjenståendeSykedager)
            ).takeIf { it.arbeidsgiverOppdrag.utenUendretLinjer().isNotEmpty() }

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
        override fun håndter(utbetaling: Utbetaling, påminnelse: Utbetalingpåminnelse) {
            påminnelse.info("Forsøker å sende utbetalingen på nytt")
            utbetaling.overfør(Overført, påminnelse)
        }
    }

    internal object IkkeGodkjent : Tilstand {}
    internal object Forkastet : Tilstand {}

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
            visitor.visitVurdering(this, ident, epost, tidspunkt, automatiskBehandling)
        }

        internal fun annullert(hendelse: ArbeidstakerHendelse, utbetaling: Utbetaling, oppdrag: Oppdrag) {
            utbetaling.observers.forEach {
                it.utbetalingAnnullert(utbetaling.id, oppdrag, hendelse, tidspunkt, epost)
            }
        }

        internal fun utbetalt(hendelse: ArbeidstakerHendelse, utbetaling: Utbetaling) {
            utbetaling.observers.forEach {
                it.utbetalingUtbetalt(
                    utbetaling.id,
                    utbetaling.type,
                    utbetaling.maksdato,
                    utbetaling.forbrukteSykedager!!,
                    utbetaling.gjenståendeSykedager!!,
                    utbetaling.arbeidsgiverOppdrag,
                    utbetaling.personOppdrag,
                    ident,
                    epost,
                    tidspunkt,
                    automatiskBehandling
                )
            }
        }

        internal fun overfør(hendelse: ArbeidstakerHendelse, oppdrag: Oppdrag, maksdato: LocalDate?) {
            utbetaling(hendelse, oppdrag, maksdato, ident)
        }

        internal fun avgjør(utbetaling: Utbetaling) =
            when {
                !godkjent -> IkkeGodkjent
                utbetaling.harUtbetalinger() -> Godkjent
                else -> GodkjentUtenUtbetaling
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
