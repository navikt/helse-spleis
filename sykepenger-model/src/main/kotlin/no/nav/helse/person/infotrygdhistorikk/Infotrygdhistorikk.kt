package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.utbetalingstidslinje.Historie
import java.time.LocalDate
import java.time.LocalDateTime

internal class Infotrygdhistorikk private constructor(
    private val elementer: MutableList<InfotrygdhistorikkElement>
) {
    private val siste get() = elementer.first()

    constructor() : this(mutableListOf())

    private companion object {
        private val gammel get() = LocalDateTime.now()
            .minusHours(2)

        private fun oppfriskningsperiode(tidligsteDato: LocalDate) =
            tidligsteDato.minusYears(4) til LocalDate.now()
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, historie: Historie, organisasjonsnummer: String, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        val avgrensetPeriode = historie.avgrensetPeriode(organisasjonsnummer, periode)
        return valider(aktivitetslogg, historie.periodetype(organisasjonsnummer, periode), avgrensetPeriode, skjæringstidspunkt)
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, periodetype: Periodetype, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        if (!harHistorikk()) return true
        return siste.valider(aktivitetslogg, periodetype, periode, skjæringstidspunkt)
    }

    internal fun validerOverlappende(aktivitetslogg: IAktivitetslogg, historie: Historie, organisasjonsnummer: String, periode: Periode): Boolean {
        val avgrensetPeriode = historie.avgrensetPeriode(organisasjonsnummer, periode)
        return validerOverlappende(aktivitetslogg, avgrensetPeriode, historie.skjæringstidspunkt(periode))
    }

    internal fun validerOverlappende(aktivitetslogg: IAktivitetslogg, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        if (!harHistorikk()) return true
        return siste.validerOverlappende(aktivitetslogg, periode, skjæringstidspunkt)
    }

    internal fun oppfriskNødvendig(aktivitetslogg: IAktivitetslogg, tidligsteDato: LocalDate, cutoff: LocalDateTime? = null): Boolean {
        if (oppfrisket(cutoff ?: gammel)) return false
        oppfrisk(aktivitetslogg, tidligsteDato)
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

    internal fun sisteSykepengedag(orgnummer: String): LocalDate? {
        if (!harHistorikk()) return null
        return siste.sisteSykepengedag(orgnummer)
    }

    internal fun lagreVilkårsgrunnlag(skjæringstidspunkt: LocalDate, periodetype: Periodetype, vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk) {
        if (!harHistorikk()) return
        if (periodetype !in listOf(Periodetype.OVERGANG_FRA_IT, Periodetype.INFOTRYGDFORLENGELSE)) return
        if (vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt) != null) return
        siste.lagreVilkårsgrunnlag(skjæringstidspunkt, vilkårsgrunnlagHistorikk)
    }

    internal fun oppdaterHistorikk(element: InfotrygdhistorikkElement) {
        if (harHistorikk() && element.erstatter(siste)) return
        elementer.add(0, element)
    }

    internal fun tøm() {
        if (!harHistorikk()) return
        oppdaterHistorikk(InfotrygdhistorikkElement.opprettTom())
    }

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.preVisitInfotrygdhistorikk()
        elementer.forEach { it.accept(visitor) }
        visitor.postVisitInfotrygdhistorikk()
    }

    private fun oppfrisket(cutoff: LocalDateTime) =
        elementer.firstOrNull()?.oppfrisket(cutoff) ?: false

    private fun oppfrisk(aktivitetslogg: IAktivitetslogg, tidligsteDato: LocalDate) {
        utbetalingshistorikk(aktivitetslogg, oppfriskningsperiode(tidligsteDato))
    }
    private fun harHistorikk() = elementer.isNotEmpty()
}
