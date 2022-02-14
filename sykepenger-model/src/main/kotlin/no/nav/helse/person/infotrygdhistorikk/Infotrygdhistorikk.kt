package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.ny.ArbeidsgiverperiodeBuilder
import no.nav.helse.utbetalingstidslinje.ny.ArbeidsgiverperiodeMediator
import no.nav.helse.utbetalingstidslinje.ny.Arbeidsgiverperiodeteller
import no.nav.helse.utbetalingstidslinje.ny.IUtbetalingstidslinjeBuilder
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

    internal fun valider(aktivitetslogg: IAktivitetslogg, arbeidsgiver: Arbeidsgiver, periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
        val avgrensetPeriode = arbeidsgiver.avgrensetPeriode(periode)
        return valider(aktivitetslogg, arbeidsgiver.periodetype(periode), avgrensetPeriode, skjæringstidspunkt)
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, periodetype: Periodetype, periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
        if (!harHistorikk()) return true
        return siste.valider(aktivitetslogg, periodetype, periode, skjæringstidspunkt)
    }

    internal fun validerOverlappende(aktivitetslogg: IAktivitetslogg, periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
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

    internal fun utbetalingstidslinje(organisasjonsnummer: String): Utbetalingstidslinje {
        if (!harHistorikk()) return Utbetalingstidslinje()
        return siste.utbetalingstidslinje(organisasjonsnummer)
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
        return Sykdomstidslinje.sisteRelevanteSkjæringstidspunktForPerioden(periode, listOf(historikkFor(organisasjonsnummer, tidslinje))) ?: periode.start
    }

    internal fun skjæringstidspunkt(periode: Periode, tidslinjer: List<Sykdomstidslinje>): LocalDate {
        return Sykdomstidslinje.sisteRelevanteSkjæringstidspunktForPerioden(periode, tidslinjer + listOf(sykdomstidslinje())) ?: periode.start
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

    internal fun førsteSykepengedagISenestePeriode(orgnummer: String): LocalDate? {
        if (!harHistorikk()) return null
        return siste.førsteSykepengedagISenestePeriode(orgnummer)
    }

    internal fun lagreVilkårsgrunnlag(
        skjæringstidspunkt: LocalDate,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
        kanOverskriveVilkårsgrunnlag: (LocalDate) -> Boolean,
        sykepengegrunnlagFor: (skjæringstidspunkt: LocalDate) -> Sykepengegrunnlag
    ) {
        if (!harHistorikk()) return
        if (vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt) != null && !kanOverskriveVilkårsgrunnlag(skjæringstidspunkt)) return
        siste.lagreVilkårsgrunnlag(vilkårsgrunnlagHistorikk, sykepengegrunnlagFor)
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
        elementer.subList(1, elementer.size).removeIf(InfotrygdhistorikkElement::kanSlettes)
    }

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.preVisitInfotrygdhistorikk()
        elementer.forEach { it.accept(visitor) }
        visitor.postVisitInfotrygdhistorikk()
    }

    internal fun build(
        organisasjonsnummer: String,
        sykdomstidslinje: Sykdomstidslinje,
        builder: ArbeidsgiverperiodeMediator,
        subsumsjonObserver: SubsumsjonObserver
    ) {
        val teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
        val arbeidsgiverperiodeBuilder = ArbeidsgiverperiodeBuilder(teller, builder, subsumsjonObserver)
        if (!harHistorikk()) return sykdomstidslinje.accept(arbeidsgiverperiodeBuilder)
        siste.build(organisasjonsnummer, sykdomstidslinje, teller, arbeidsgiverperiodeBuilder)
    }

    internal fun build(
        organisasjonsnummer: String,
        sykdomstidslinje: Sykdomstidslinje,
        builder: IUtbetalingstidslinjeBuilder,
        subsumsjonObserver: SubsumsjonObserver
    ): Utbetalingstidslinje {
        build(organisasjonsnummer, sykdomstidslinje, builder as ArbeidsgiverperiodeMediator, subsumsjonObserver)
        return fjernHistorikk(organisasjonsnummer, builder.result(), sykdomstidslinje.førsteDag())
    }

    private fun fjernHistorikk(organisasjonsnummer: String, utbetalingstidslinje: Utbetalingstidslinje, førsteDag: LocalDate): Utbetalingstidslinje {
        if (!harHistorikk()) return utbetalingstidslinje
        return siste.fjernHistorikk(utbetalingstidslinje, organisasjonsnummer, førsteDag)
    }

    private fun oppfrisket(cutoff: LocalDateTime) =
        elementer.firstOrNull()?.oppfrisket(cutoff) ?: false

    private fun oppfrisk(aktivitetslogg: IAktivitetslogg, tidligsteDato: LocalDate) {
        utbetalingshistorikk(aktivitetslogg, oppfriskningsperiode(tidligsteDato))
    }

    private fun harHistorikk() = elementer.isNotEmpty()

    internal fun harBrukerutbetalingerFor(organisasjonsnummer: String, periode: Periode): Boolean {
        if(!harHistorikk()) return false
        return siste.harBrukerutbetalingerFor(organisasjonsnummer, periode)
    }
}
