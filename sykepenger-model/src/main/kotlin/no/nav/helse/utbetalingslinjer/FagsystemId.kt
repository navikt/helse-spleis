package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.person.FagsystemIdVisitor
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Companion.head
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Companion.sorterOppdrag
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Companion.utbetaltTidslinje
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Utbetalingtype.ANNULLERING
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

internal class FagsystemId private constructor(
    private val fagsystemId: String,
    private val fagområde: Fagområde,
    private val mottaker: String,
    private var tilstand: Tilstand,
    oppdragsliste: List<Utbetaling>,
    forkastet: List<Utbetaling>
) {
    internal constructor(observatør: FagsystemIdObserver, fagsystemId: String, fagområde: Fagområde, mottaker: String, utbetaling: Utbetaling) : this(
        fagsystemId,
        fagområde,
        mottaker,
        Initiell,
        emptyList(),
        emptyList()
    ) {
        register(observatør)
        tilstand.nyUtbetaling(this, utbetaling)
    }

    private val utbetalinger = sorterOppdrag(oppdragsliste)
    private val forkastet = sorterOppdrag(forkastet)

    private val head get() = utbetalinger.head()
    private val utbetalingstidslinje get() = head.utbetalingstidslinje()
    private val utbetaltTidslinje get() = utbetalinger.utbetaltTidslinje()

    private var observer: FagsystemIdObserver = object : FagsystemIdObserver {}

    init {
        require((tilstand == Initiell && oppdragsliste.isEmpty()) || (tilstand != Initiell && oppdragsliste.isNotEmpty()))
    }

    internal fun nettoBeløp() = head.nettobeløp()

    internal fun append(orgnr: String, bøtte: Historie.Historikkbøtte) {
        if (utbetaltTidslinje.isEmpty()) return
        bøtte.add(orgnr, utbetaltTidslinje)
    }

    internal fun register(fagsystemIdObserver: FagsystemIdObserver) {
        observer = fagsystemIdObserver
    }

    internal fun accept(visitor: FagsystemIdVisitor) {
        visitor.preVisitFagsystemId(this, fagsystemId, fagområde, tilstand::class.simpleName!!, utbetalingstidslinje, utbetaltTidslinje)
        visitor.preVisitOppdragsliste()
        utbetalinger.onEach { it.accept(visitor) }
        visitor.postVisitOppdragsliste()
        visitor.postVisitFagsystemId(this, fagsystemId, fagområde, tilstand::class.simpleName!!, utbetalingstidslinje, utbetaltTidslinje)
    }

    internal fun håndter(hendelse: Utbetalingsgodkjenning) {
        tilstand.godkjenn(this, hendelse, hendelse.saksbehandlerEpost(), hendelse.saksbehandler(), hendelse.automatiskBehandling(), hendelse.godkjenttidspunkt())
    }

    internal fun håndter(hendelse: AnnullerUtbetaling): Boolean {
        if (!hendelse.erRelevant(fagsystemId)) return false
        tilstand.annuller(this, hendelse, hendelse.saksbehandlerEpost, hendelse.saksbehandlerIdent, hendelse.opprettet)
        return true
    }

    internal fun håndter(hendelse: no.nav.helse.hendelser.UtbetalingOverført): Boolean {
        if (!hendelse.erRelevant(fagsystemId)) return false
        tilstand.overført(this, hendelse)
        return true
    }

    internal fun håndter(hendelse: UtbetalingHendelse): Boolean {
        if (!hendelse.erRelevant(fagsystemId)) return false
        tilstand.kvittér(this, hendelse)
        return true
    }

    internal fun simuler(aktivitetslogg: IAktivitetslogg) {
        tilstand.simuler(this, aktivitetslogg)
    }

    internal fun prøvIgjen(aktivitetslogg: IAktivitetslogg) {
        tilstand.prøvIgjen(this, aktivitetslogg)
    }

    internal fun erTom() = utbetalinger.isEmpty()

    internal fun erAnnullert() = tilstand == Annullert

    internal fun utvide(opprinnelig: Oppdrag, utbetalingstidslinje: Utbetalingstidslinje, maksdato: LocalDate, forbrukteSykedager: Int, gjenståendeSykedager: Int, aktivitetslogg: IAktivitetslogg): Boolean {
        val utbetaling = Utbetaling.lagUtvidelse(this, head, opprinnelig, utbetalingstidslinje, maksdato, forbrukteSykedager, gjenståendeSykedager, aktivitetslogg) ?: return false
        return tilstand.nyUtbetaling(this, utbetaling)
    }

    private fun nyUtbetaling(utbetaling: Utbetaling) {
        utbetalinger.add(0, utbetaling)
    }

    private fun nyUtbetaling(nesteTilstand: Tilstand, utbetaling: Utbetaling) {
        nyUtbetaling(utbetaling)
        tilstand(nesteTilstand)
    }

    private fun overfør(aktivitetslogg: IAktivitetslogg, ident: String, epost: String, automatiskBehandlet: Boolean, godkjenttidspunkt: LocalDateTime) {
        head.overfør(aktivitetslogg, ident, epost, automatiskBehandlet, godkjenttidspunkt)
        tilstand(Sendt)
    }

    private fun overført(hendelse: no.nav.helse.hendelser.UtbetalingOverført) {
        head.overført(hendelse.avstemmingsnøkkel, hendelse.overføringstidspunkt)
        tilstand(Overført)
    }

    private fun annuller(aktivitetslogg: IAktivitetslogg, ident: String, epost: String, godkjenttidspunkt: LocalDateTime) {
        nyUtbetaling(Utbetaling.lagAnnullering(head, aktivitetslogg))
        overfør(aktivitetslogg, ident, epost, false, godkjenttidspunkt)
    }

    private fun avsluttUtbetaling() {
        head.avslutt()
        tilstand(if (head.erAnnullering()) Annullert else Aktiv)
    }

    private fun forkastUtbetaling() {
        val søppel = utbetalinger.removeFirst()
        forkastet.add(0, søppel.avslutt())
    }

    private fun tilstand(nyTilstand: Tilstand) {
        val gammelTilstand = tilstand
        tilstand = nyTilstand
        observer.tilstandEndret(this, gammelTilstand::class.simpleName!!, nyTilstand::class.simpleName!!)
        tilstand.entering(this)
    }

    internal class Utbetaling private constructor(
        private val oppdrag: Oppdrag,
        private val utbetalingstidslinje: Utbetalingstidslinje,
        private val type: Utbetalingtype,
        private var maksdato: LocalDate? = null,
        private var forbrukteSykedager: Int = -1,
        private var gjenståendeSykedager: Int = -1,
        private val opprettet: LocalDateTime = LocalDateTime.now(),
        private var godkjentAv: Triple<String, String, LocalDateTime>? = null,
        private var automatiskBehandlet: Boolean = false,
        private var sendt: LocalDateTime? = null,
        private var avstemmingsnøkkel: Long? = null,
        private var overføringstidspunkt: LocalDateTime? = null,
        private var avsluttet: LocalDateTime? = null
    ) {

        init {
            require((type != ANNULLERING && maksdato != null) || (type == ANNULLERING && maksdato == null)) {
                "Maksdato må være satt når det ikke er en annullering"
            }
            require((type != ANNULLERING && gjenståendeSykedager >= 0 && forbrukteSykedager > 0) || (type == ANNULLERING && forbrukteSykedager == -1 && gjenståendeSykedager == -1)) {
                "Gjenstående og forbrukte sykedager må være satt når det ikke er en annullering"
            }
        }

        fun nettobeløp() = oppdrag.nettoBeløp()
        fun utbetalingstidslinje() = utbetalingstidslinje

        fun accept(visitor: FagsystemIdVisitor) {
            visitor.preVisitUtbetaling(oppdrag, utbetalingstidslinje, opprettet, avsluttet)
            oppdrag.accept(visitor)
            utbetalingstidslinje.accept(visitor)
            visitor.postVisitUtbetaling(oppdrag, utbetalingstidslinje, opprettet, avsluttet)
        }

        fun simuler(aktivitetslogg: IAktivitetslogg) {
            val maksdato = requireNotNull(maksdato) { "Kan ikke simulere uten maksdato" }
            oppdrag.simuler(aktivitetslogg, maksdato, systemident)
        }

        fun overfør(aktivitetslogg: IAktivitetslogg, ident: String, epost: String, automatiskBehandlet: Boolean, godkjenttidspunkt: LocalDateTime) {
            godkjentAv = Triple(ident, epost, godkjenttidspunkt)
            this.automatiskBehandlet = automatiskBehandlet
            overfør(aktivitetslogg)
        }

        fun overfør(aktivitetslogg: IAktivitetslogg) {
            val (ident, epost, tidspunkt) = requireNotNull(godkjentAv) { "Utbetalingen må være godkjent før den kan forsøkes overføres på nytt" }
            sendt = LocalDateTime.now()
            oppdrag.overfør(aktivitetslogg, maksdato, ident, epost, tidspunkt)
        }

        fun overført(avstemmingsnøkkel: Long, overføringstidspunkt: LocalDateTime) {
            this.avstemmingsnøkkel = avstemmingsnøkkel
            this.overføringstidspunkt = overføringstidspunkt
        }

        fun avslutt(tidsstempel: LocalDateTime = LocalDateTime.now()) = apply {
            avsluttet = tidsstempel
        }

        private fun erAvsluttet() = avsluttet != null

        internal fun erAnnullering() = type == ANNULLERING

        internal enum class Utbetalingtype { UTBETALING, ANNULLERING }

        companion object {
            private const val systemident = "SPLEIS"

            fun nyUtbetaling(oppdrag: Oppdrag, utbetalingstidslinje: Utbetalingstidslinje, maksdato: LocalDate, forbrukteSykedager: Int, gjenståendeSykedager: Int) =
                Utbetaling(oppdrag, utbetalingstidslinje, UTBETALING, maksdato, forbrukteSykedager, gjenståendeSykedager)

            fun lagUtvidelse(fagsystemId: FagsystemId, siste: Utbetaling, kandidat: Oppdrag, utbetalingstidslinje: Utbetalingstidslinje, maksdato: LocalDate, forbrukteSykedager: Int, gjenståendeSykedager: Int, aktivitetslogg: IAktivitetslogg): Utbetaling? {
                val nytt = kandidat.minus(siste.oppdrag, aktivitetslogg)
                if (!nytt.tilhører(fagsystemId.fagsystemId, fagsystemId.fagområde)) return null
                nytt.nettoBeløp(siste.oppdrag)
                return Utbetaling(nytt, utbetalingstidslinje, UTBETALING, maksdato, forbrukteSykedager, gjenståendeSykedager)
            }

            fun lagAnnullering(siste: Utbetaling, aktivitetslogg: IAktivitetslogg): Utbetaling {
                val oppdrag = siste.oppdrag.emptied().minus(siste.oppdrag, aktivitetslogg)
                return Utbetaling(oppdrag, Utbetalingstidslinje(), ANNULLERING)
            }

            fun sorterOppdrag(liste: List<Utbetaling>) = liste.sortedByDescending { it.opprettet }.toMutableList()

            fun List<Utbetaling>.head() = first()

            fun List<Utbetaling>.utbetaltTidslinje() = firstOrNull { it.erAvsluttet() }?.utbetalingstidslinje ?: Utbetalingstidslinje()
        }
    }

    internal interface Tilstand {
        fun entering(fagsystemId: FagsystemId) {}

        fun godkjenn(
            fagsystemId: FagsystemId,
            hendelse: Utbetalingsgodkjenning,
            epost: String,
            ident: String,
            automatiskBehandlet: Boolean,
            godkjenttidspunkt: LocalDateTime
        ) {
            throw IllegalStateException("Forventet ikke å utbetale på fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }

        fun prøvIgjen(fagsystemId: FagsystemId, aktivitetslogg: IAktivitetslogg) {
            throw IllegalStateException("Forventet ikke å utbetale på fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }

        fun annuller(
            fagsystemId: FagsystemId,
            hendelse: AnnullerUtbetaling,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            throw IllegalStateException("Forventet ikke å annullere på fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }

        fun overført(fagsystemId: FagsystemId, hendelse: no.nav.helse.hendelser.UtbetalingOverført) {
            throw IllegalStateException("Forventet ikke overførtkvittering på fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }

        fun kvittér(fagsystemId: FagsystemId, hendelse: UtbetalingHendelse) {
            throw IllegalStateException("Forventet ikke kvittering på fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }

        fun simuler(
            fagsystemId: FagsystemId,
            aktivitetslogg: IAktivitetslogg
        ) {
            throw IllegalStateException("Forventet ikke simulering på fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }

        fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling): Boolean {
            throw IllegalStateException("Kan ikke legge til ny utbetaling fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }
    }


    internal object Initiell : Tilstand {
        override fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling): Boolean {
            fagsystemId.nyUtbetaling(Ny, utbetaling)
            return true
        }
    }

    internal abstract class AbstractUbetalt: Tilstand {
        abstract fun avvist(fagsystemId: FagsystemId)

        override fun godkjenn(
            fagsystemId: FagsystemId,
            hendelse: Utbetalingsgodkjenning,
            epost: String,
            ident: String,
            automatiskBehandlet: Boolean,
            godkjenttidspunkt: LocalDateTime
        ) {
            if (hendelse.valider().hasErrorsOrWorse()) return avvist(fagsystemId)
            fagsystemId.overfør(hendelse, ident, epost, automatiskBehandlet, godkjenttidspunkt)
        }

        override fun simuler(
            fagsystemId: FagsystemId,
            aktivitetslogg: IAktivitetslogg
        ) {
            fagsystemId.head.simuler(aktivitetslogg)
        }
    }

    internal object Ny : AbstractUbetalt() {
        override fun avvist(fagsystemId: FagsystemId) {
            fagsystemId.tilstand(Avvist)
        }
    }

    internal object Ubetalt : AbstractUbetalt() {
        override fun avvist(fagsystemId: FagsystemId) {
            fagsystemId.forkastUtbetaling()
            fagsystemId.tilstand(Aktiv)
        }

        override fun annuller(
            fagsystemId: FagsystemId,
            hendelse: AnnullerUtbetaling,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            fagsystemId.forkastUtbetaling()
            fagsystemId.annuller(hendelse, ident, epost, godkjenttidspunkt)
        }
    }

    internal object Aktiv : Tilstand {

        override fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling): Boolean {
            fagsystemId.nyUtbetaling(Ubetalt, utbetaling)
            return true
        }
        override fun annuller(
            fagsystemId: FagsystemId,
            hendelse: AnnullerUtbetaling,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            fagsystemId.annuller(hendelse, ident, epost, godkjenttidspunkt)
        }
    }

    internal object Sendt : Tilstand {
        override fun overført(fagsystemId: FagsystemId, hendelse: no.nav.helse.hendelser.UtbetalingOverført) {
            fagsystemId.overført(hendelse)
        }

        override fun prøvIgjen(fagsystemId: FagsystemId, aktivitetslogg: IAktivitetslogg) {
            fagsystemId.head.overfør(aktivitetslogg)
        }
    }

    internal object Overført : Tilstand {

        override fun kvittér(fagsystemId: FagsystemId, hendelse: UtbetalingHendelse) {
            if (hendelse.valider().hasErrorsOrWorse()) return fagsystemId.tilstand(Avvist)
            fagsystemId.avsluttUtbetaling()
        }
        override fun prøvIgjen(fagsystemId: FagsystemId, aktivitetslogg: IAktivitetslogg) {
            fagsystemId.head.overfør(aktivitetslogg)
        }
    }

    internal object Avvist : Tilstand {
        override fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling) = false
    }

    internal object Annullert : Tilstand {
        override fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling) = false
    }
}
