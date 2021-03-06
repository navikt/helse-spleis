package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.IUtbetalingstidslinjeBuilder
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilder
import java.time.LocalDate
import java.time.LocalDateTime

internal class Infotrygdhistorikk private constructor(
    private val elementer: MutableList<InfotrygdhistorikkElement>
) {
    private val siste get() = elementer.first()

    constructor() : this(mutableListOf())

    private companion object {
        private val gammel: LocalDateTime get() = LocalDateTime.now().minusHours(2)

        private fun oppfriskningsperiode(tidligsteDato: LocalDate) =
            tidligsteDato.minusYears(4) til LocalDate.now()
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, arbeidsgiver: Arbeidsgiver, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        val avgrensetPeriode = arbeidsgiver.avgrensetPeriode(periode)
        return valider(aktivitetslogg, arbeidsgiver.periodetype(periode), avgrensetPeriode, skjæringstidspunkt)
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, periodetype: Periodetype, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        if (!harHistorikk()) return true
        return siste.valider(aktivitetslogg, periodetype, periode, skjæringstidspunkt)
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

    internal fun utbetalingstidslinje(): Utbetalingstidslinje {
        if (!harHistorikk()) return Utbetalingstidslinje()
        return siste.utbetalingstidslinje()
    }

    internal fun historikkFor(orgnummer: String, sykdomstidslinje: Sykdomstidslinje): Sykdomstidslinje {
        if (!harHistorikk()) return sykdomstidslinje
        return siste.historikkFor(orgnummer, sykdomstidslinje)
    }

    internal fun harBetalt(organisasjonsnummer: String, dato: LocalDate): Boolean {
        if (!harHistorikk()) return false
        return siste.harBetalt(organisasjonsnummer, dato)
    }

    internal fun ingenUkjenteArbeidsgivere(organisasjonsnumre: List<String>, dato: LocalDate): Boolean {
        if (!harHistorikk()) return true
        return siste.ingenUkjenteArbeidsgivere(organisasjonsnumre, dato)
    }

    internal fun skjæringstidspunkt(organisasjonsnummer: String, periode: Periode, tidslinje: Sykdomstidslinje): LocalDate {
        return Sykdomstidslinje.skjæringstidspunkt(periode.endInclusive, listOf(historikkFor(organisasjonsnummer, tidslinje))) ?: periode.start
    }

    internal fun skjæringstidspunkt(periode: Periode, tidslinjer: List<Sykdomstidslinje>): LocalDate {
        return Sykdomstidslinje.skjæringstidspunkt(periode.endInclusive, tidslinjer + listOf(sykdomstidslinje())) ?: periode.start
    }

    internal fun skjæringstidspunkter(tidslinjer: List<Sykdomstidslinje>): List<LocalDate> {
        return Sykdomstidslinje.skjæringstidspunkter(tidslinjer + listOf(sykdomstidslinje()))
    }

    private fun sykdomstidslinje(): Sykdomstidslinje {
        if (!harHistorikk()) return Sykdomstidslinje()
        return siste.sykdomstidslinje()
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

    internal fun oppdaterHistorikk(element: InfotrygdhistorikkElement): Boolean {
        if (harHistorikk() && element.erstatter(siste)) return false
        elementer.add(0, element)
        return true
    }

    internal fun harEndretHistorikk(utbetaling: Utbetaling): Boolean {
        if (!harHistorikk()) return false
        return siste.harEndretHistorikk(utbetaling)
    }

    internal fun tøm() {
        if (!harHistorikk()) return
        elementer.removeIf(InfotrygdhistorikkElement::kanSlettes)
        if (!harHistorikk()) return
        oppdaterHistorikk(InfotrygdhistorikkElement.opprettTom())
    }

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.preVisitInfotrygdhistorikk()
        elementer.forEach { it.accept(visitor) }
        visitor.postVisitInfotrygdhistorikk()
    }

    internal fun builder(organisasjonsnummer: String, builder: UtbetalingstidslinjeBuilder): IUtbetalingstidslinjeBuilder {
        if (!harHistorikk()) return builder
        return Infotrygdhistorikkdekoratør(builder, organisasjonsnummer)
    }

    private fun oppfrisket(cutoff: LocalDateTime) =
        elementer.firstOrNull()?.oppfrisket(cutoff) ?: false

    private fun oppfrisk(aktivitetslogg: IAktivitetslogg, tidligsteDato: LocalDate) {
        utbetalingshistorikk(aktivitetslogg, oppfriskningsperiode(tidligsteDato))
    }

    private fun harHistorikk() = elementer.isNotEmpty()

    private inner class Infotrygdhistorikkdekoratør(
        private val builder: UtbetalingstidslinjeBuilder,
        private val organisasjonsnummer: String
    ): IUtbetalingstidslinjeBuilder {
        private lateinit var samletSykdomstidslinje: Sykdomstidslinje

        init {
            builder.forlengelsestrategi { dagen ->
                harBetalt(organisasjonsnummer, samletSykdomstidslinje.skjæringstidspunkt(dagen) ?: dagen)
            }
        }

        override fun result(sykdomstidslinje: Sykdomstidslinje, periode: Periode): Utbetalingstidslinje {
            samletSykdomstidslinje = historikkFor(organisasjonsnummer, sykdomstidslinje)
            return siste.fjernHistorikk(builder.result(samletSykdomstidslinje, periode), organisasjonsnummer, sykdomstidslinje.førsteDag())
        }
    }
}
