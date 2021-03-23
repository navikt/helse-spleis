package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Historie
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

    internal fun valider(aktivitetslogg: IAktivitetslogg, historie: Historie, organisasjonsnummer: String, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        val avgrensetPeriode = historie.avgrensetPeriode(organisasjonsnummer, periode)
        return valider(aktivitetslogg, avgrensetPeriode, skjæringstidspunkt)
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        if (!harHistorikk()) return true
        return siste.valider(aktivitetslogg, periode, skjæringstidspunkt)
    }

    internal fun overlapperMed(aktivitetslogg: IAktivitetslogg, historie: Historie, organisasjonsnummer: String, periode: Periode): Boolean {
        val avgrensetPeriode = historie.avgrensetPeriode(organisasjonsnummer, periode)
        return overlapperMed(aktivitetslogg, avgrensetPeriode)
    }

    internal fun overlapperMed(aktivitetslogg: IAktivitetslogg, periode: Periode): Boolean {
        if (!harHistorikk()) return false
        return siste.overlapperMed(aktivitetslogg, periode)
    }

    internal fun oppfriskNødvendig(aktivitetslogg: IAktivitetslogg, cutoff: LocalDateTime = gammel): Boolean {
        if (oppfrisket(cutoff)) return false
        oppfrisk(aktivitetslogg)
        return true
    }

    internal fun addInntekter(person: Person, aktivitetslogg: IAktivitetslogg) {
        if (!harHistorikk()) return
        siste.addInntekter(person, aktivitetslogg)
    }

    internal fun append(bøtte: Historie.Historikkbøtte) {
        if (!harHistorikk()) return
        siste.append(bøtte)
    }

    internal fun lagreVilkårsgrunnlag(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk) {
        if (!harHistorikk()) return
        siste.lagreVilkårsgrunnlag(skjæringstidspunkt, vilkårsgrunnlagHistorikk)
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
        private val kilde = SykdomstidslinjeHendelse.Hendelseskilde("Infotrygdhistorikk", id)

        init {
            if (!erTom()) requireNotNull(hendelseId) { "HendelseID må være satt når elementet inneholder data" }
        }

        internal companion object {
            fun opprett(
                oppdatert: LocalDateTime,
                hendelseId: UUID,
                perioder: List<Infotrygdperiode>,
                inntekter: List<Inntektsopplysning>,
                arbeidskategorikoder: Map<String, LocalDate>
            ) =
                Element(
                    id = UUID.randomUUID(),
                    tidsstempel = LocalDateTime.now(),
                    hendelseId = hendelseId,
                    perioder = perioder.sortedBy { it.start },
                    inntekter = inntekter,
                    arbeidskategorikoder = arbeidskategorikoder,
                    oppdatert = oppdatert
                )

            fun opprettTom() =
                Element(
                    id = UUID.randomUUID(),
                    tidsstempel = LocalDateTime.now(),
                    hendelseId = null,
                    perioder = emptyList(),
                    inntekter = emptyList(),
                    arbeidskategorikoder = emptyMap(),
                    oppdatert = LocalDateTime.MIN
                )
        }

        private fun erTom() =
            perioder.isEmpty() && inntekter.isEmpty() && arbeidskategorikoder.isEmpty()

        internal fun addInntekter(person: Person, aktivitetslogg: IAktivitetslogg) {
            Inntektsopplysning.addInntekter(inntekter, person, aktivitetslogg, id)
        }

        fun lagreVilkårsgrunnlag(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk) {
            vilkårsgrunnlagHistorikk.lagre(skjæringstidspunkt, VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag())
        }

        internal fun append(bøtte: Historie.Historikkbøtte) {
            perioder.forEach { it.append(bøtte, kilde) }
        }

        internal fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
            aktivitetslogg.info("Sjekker utbetalte perioder")
            perioder.forEach { it.valider(aktivitetslogg, periode) }

            aktivitetslogg.info("Sjekker inntektsopplysninger")
            Inntektsopplysning.valider(inntekter, aktivitetslogg, periode, skjæringstidspunkt)

            aktivitetslogg.info("Sjekker arbeidskategorikoder")
            if (!erNormalArbeidstaker(skjæringstidspunkt)) aktivitetslogg.error("Personen er ikke registrert som normal arbeidstaker i Infotrygd")

            return !aktivitetslogg.hasErrorsOrWorse()
        }

        internal fun overlapperMed(aktivitetslogg: IAktivitetslogg, periode: Periode): Boolean {
            aktivitetslogg.info("Sjekker utbetalte perioder for overlapp mot %s", periode)
            perioder.forEach { it.validerOverlapp(aktivitetslogg, periode) }
            return aktivitetslogg.hasErrorsOrWorse()
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

        override fun equals(other: Any?): Boolean {
            if (other !is Element) return false
            return this.id == other.id
        }

        fun erstatt(elementer: MutableList<Element>) {
            if (elementer.isEmpty() || elementer.first().hashCode() != this.hashCode()) return elementer.add(0, this)
            elementer.first().oppdatert = this.oppdatert
        }
    }
}
