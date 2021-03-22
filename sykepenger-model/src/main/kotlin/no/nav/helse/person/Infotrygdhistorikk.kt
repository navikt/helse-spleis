package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Infotrygdhistorikk private constructor(
    private val elementer: MutableList<Element>
) {
    private val siste get() = elementer.first()

    constructor() : this(mutableListOf())

    private companion object {
        private val gammel get() = LocalDateTime.now()
            .minusHours(2)
        private val oppfriskningsperiode get() = LocalDate.now().minusYears(4) til LocalDate.now()
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        if (!harHistorikk()) return true
        return siste.valider(aktivitetslogg, periode, skjæringstidspunkt)
    }

    internal fun oppfriskNødvendig(aktivitetslogg: IAktivitetslogg, cutoff: LocalDateTime = gammel): Boolean {
        if (oppfrisket(cutoff)) return false
        oppfrisk(aktivitetslogg)
        return true
    }

    internal fun oppdaterHistorikk(element: Element) {
        element.erstatt(elementer)
    }

    internal fun tøm() {
        if (!harHistorikk()) return
        oppdaterHistorikk(Element.opprettTom())
    }

    private fun oppfrisket(cutoff: LocalDateTime) =
        elementer.firstOrNull()?.oppfrisket(cutoff) ?: false

    private fun oppfrisk(aktivitetslogg: IAktivitetslogg) {
        // TODO: hente fom. fire år tilbake fra tidligste skjæringstidspunkt tom. "now" ?
        utbetalingshistorikk(aktivitetslogg, oppfriskningsperiode)
    }

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.preVisitInfotrygdhistorikk()
        elementer.forEach { it.accept(visitor) }
        visitor.preVisitInfotrygdhistorikk()
    }
    private fun harHistorikk() = elementer.isNotEmpty()

    internal class Element private constructor(
        private val id: UUID,
        private val tidsstempel: LocalDateTime = LocalDateTime.now(),
        private val hendelseId: UUID? = null,
        private val perioder: List<Infotrygdperiode>,
        private val inntekter: List<Inntektsopplysning>,
        private val arbeidskategorikoder: Map<String, LocalDate>,
        private var oppdatert: LocalDateTime = tidsstempel
    ) {
        init {
            if (!erTom()) requireNotNull(hendelseId) { "HendelseID må være satt når elementet inneholder data" }
        }

        internal companion object {
            fun opprett(
                tidsstempel: LocalDateTime,
                hendelseId: UUID,
                perioder: List<Infotrygdperiode>,
                inntekter: List<Inntektsopplysning>,
                arbeidskategorikoder: Map<String, LocalDate>
            ) =
                Element(
                    id = UUID.randomUUID(),
                    tidsstempel = tidsstempel,
                    hendelseId = hendelseId,
                    perioder = perioder,
                    inntekter = inntekter,
                    arbeidskategorikoder = arbeidskategorikoder
                )

            fun opprettTom() =
                Element(
                    id = UUID.randomUUID(),
                    tidsstempel = LocalDateTime.now(),
                    hendelseId = null,
                    perioder = emptyList(),
                    inntekter = emptyList(),
                    arbeidskategorikoder = emptyMap()
                ).also { it.oppdatert = LocalDateTime.MIN }
        }

        private fun erTom() =
            perioder.isEmpty() && inntekter.isEmpty() && arbeidskategorikoder.isEmpty()

        internal fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
            aktivitetslogg.info("Sjekker utbetalte perioder for overlapp mot %s", periode)
            perioder.forEach { it.valider(aktivitetslogg, periode) }

            aktivitetslogg.info("Sjekker inntektsopplysninger")
            Inntektsopplysning.valider(inntekter, aktivitetslogg, periode, skjæringstidspunkt)

            aktivitetslogg.info("Sjekker arbeidskategorikoder")
            if (!erNormalArbeidstaker(skjæringstidspunkt)) aktivitetslogg.error("Personen er ikke registrert som normal arbeidstaker i Infotrygd")

            return !aktivitetslogg.hasErrorsOrWorse()
        }

        internal fun oppfrisket(cutoff: LocalDateTime) =
            oppdatert > cutoff

        internal fun accept(visitor: InfotrygdhistorikkVisitor) {
            visitor.preVisitInfotrygdhistorikkElement(id, tidsstempel, oppdatert, hendelseId)
            visitor.preVisitInfotrygdhistorikkPerioder()
            perioder.forEach { it.accept(visitor) }
            visitor.postVisitInfotrygdhistorikkPerioder()
            visitor.preVisitInfotrygdhistorikkInntektsopplysninger()
            inntekter.forEach { it.accept(visitor) }
            visitor.postVisitInfotrygdhistorikkInntektsopplysninger()
            visitor.visitInfotrygdhistorikkArbeidskategorikoder(arbeidskategorikoder)
            visitor.postVisitInfotrygdhistorikkElement(id, tidsstempel, oppdatert, hendelseId)
        }

        private fun erNormalArbeidstaker(skjæringstidspunkt: LocalDate?): Boolean {
            if (arbeidskategorikoder.isEmpty() || skjæringstidspunkt == null) return true
            return arbeidskategorikoder
                .filter { (_, dato) -> dato >= skjæringstidspunkt }
                .all { (arbeidskategorikode, _) -> arbeidskategorikode == "01" }
        }

        override fun hashCode(): Int {
            return Objects.hash(perioder, inntekter, arbeidskategorikoder)
        }

        fun erstatt(elementer: MutableList<Element>) {
            if (elementer.isEmpty() || elementer.first().hashCode() != this.hashCode()) return elementer.add(0, this)
            elementer.first().oppdatert = this.oppdatert
        }
    }

    internal abstract class Infotrygdperiode(private val periode: Periode) : ClosedRange<LocalDate> by(periode), Iterable<LocalDate> by(periode) {
        open fun sykdomstidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje = Sykdomstidslinje()
        open fun utbetalingstidslinje(): Utbetalingstidslinje = Utbetalingstidslinje()
        open fun append(bøtte: Historie.Historikkbøtte, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {}

        abstract fun accept(visitor: InfotrygdhistorikkVisitor)
        open fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode) {}

        fun overlapperMed(other: Periode) = periode.overlapperMed(other)

        override fun hashCode() = Objects.hash(this::class, periode)
        override fun equals(other: Any?): Boolean {
            if (other !is Infotrygdperiode) return false
            if (this::class != other::class) return false
            return this.periode == other.periode
        }
    }

    internal class Friperiode(periode: Periode) : Infotrygdperiode(periode) {
        override fun sykdomstidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje {
            return Sykdomstidslinje.feriedager(start, endInclusive, kilde)
        }

        override fun utbetalingstidslinje(): Utbetalingstidslinje {
            return Utbetalingstidslinje().also {
                forEach { dag -> it.addFridag(dag, Økonomi.ikkeBetalt().inntekt(Inntekt.INGEN, skjæringstidspunkt = dag)) }
            }
        }

        override fun append(bøtte: Historie.Historikkbøtte, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            bøtte.add(tidslinje = sykdomstidslinje(kilde))
            bøtte.add(tidslinje = utbetalingstidslinje())
        }

        override fun accept(visitor: InfotrygdhistorikkVisitor) {
            visitor.visitInfotrygdhistorikkFerieperiode(this)
        }
    }

    internal class Ukjent(periode: Periode) : Infotrygdperiode(periode) {
        override fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode) {
            if (periode.endInclusive < periode.start.minusDays(18)) return
            aktivitetslogg.warn("Perioden er lagt inn i Infotrygd, men ikke utbetalt. Fjern fra Infotrygd hvis det utbetales via speil.")
        }

        override fun accept(visitor: InfotrygdhistorikkVisitor) {
            visitor.visitInfotrygdhistorikkFerieperiode(this)
        }
    }

    internal class Utbetalingsperiode(
        private val orgnr: String,
        periode: Periode,
        private val grad: Prosentdel,
        private val inntekt: Inntekt
    ) : Infotrygdperiode(periode) {
        override fun sykdomstidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje {
            return Sykdomstidslinje.sykedager(start, endInclusive, grad, kilde)
        }

        override fun utbetalingstidslinje() =
            Utbetalingstidslinje().also { utbetalingstidslinje ->
                this.forEach { dag -> nyDag(utbetalingstidslinje, dag) }
            }

        private fun nyDag(utbetalingstidslinje: Utbetalingstidslinje, dato: LocalDate) {
            if (dato.erHelg()) utbetalingstidslinje.addHelg(dato, Økonomi.sykdomsgrad(grad).inntekt(Inntekt.INGEN, skjæringstidspunkt = dato))
            else utbetalingstidslinje.addNAVdag(dato, Økonomi.sykdomsgrad(grad).inntekt(inntekt, skjæringstidspunkt = dato))
        }

        override fun append(bøtte: Historie.Historikkbøtte, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            bøtte.add(orgnr, sykdomstidslinje(kilde))
            bøtte.add(orgnr, utbetalingstidslinje())
        }

        override fun accept(visitor: InfotrygdhistorikkVisitor) {
            visitor.visitInfotrygdhistorikkUtbetalingsperiode(orgnr, this, grad, inntekt)
        }

        override fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode) {
            if (!overlapperMed(periode)) return
            aktivitetslogg.error("Utbetaling i Infotrygd %s til %s overlapper med vedtaksperioden", start, endInclusive)
        }

        override fun hashCode() =
            Objects.hash(orgnr, start, endInclusive, grad, inntekt)

        override fun equals(other: Any?): Boolean {
            if (other !is Utbetalingsperiode) return false
            return this.orgnr == other.orgnr && this.start == other.start
                && this.grad == other.grad && this.inntekt == other.inntekt
        }
    }

    internal class Inntektsopplysning(
        private val orgnummer: String,
        private val sykepengerFom: LocalDate,
        private val inntekt: Inntekt,
        private val refusjonTilArbeidsgiver: Boolean,
        private val refusjonTom: LocalDate? = null
    ) {
        fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
            if (!erRelevant(periode, skjæringstidspunkt)) return true
            if (orgnummer.isBlank()) aktivitetslogg.error("Organisasjonsnummer for inntektsopplysning fra Infotrygd mangler")
            if (refusjonTom != null && periode.slutterEtter(refusjonTom)) aktivitetslogg.error("Refusjon fra Infotrygd opphører i eller før perioden")
            if (!refusjonTilArbeidsgiver) aktivitetslogg.error("Utbetaling skal gå rett til bruker")
            return !aktivitetslogg.hasErrorsOrWorse()
        }

        internal fun accept(visitor: InfotrygdhistorikkVisitor) {
            visitor.visitInfotrygdhistorikkInntektsopplysning(orgnummer, sykepengerFom, inntekt, refusjonTilArbeidsgiver, refusjonTom)
        }

        private fun erRelevant(periode: Periode, skjæringstidspunkt: LocalDate?) =
            sykepengerFom >= (skjæringstidspunkt ?: periode.start.minusMonths(12))

        internal companion object {
            fun valider(
                liste: List<Inntektsopplysning>,
                aktivitetslogg: IAktivitetslogg,
                periode: Periode,
                skjæringstidspunkt: LocalDate?
            ) {}
        }
    }
}
