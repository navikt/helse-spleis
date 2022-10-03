package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Person
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeBuilder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeMediator
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeteller
import no.nav.helse.utbetalingstidslinje.IUtbetalingstidslinjeBuilder
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class Infotrygdhistorikk private constructor(
    private val elementer: MutableList<InfotrygdhistorikkElement>
) {
    private val siste get() = elementer.first()

    constructor() : this(mutableListOf())

    internal companion object {
        private val gammel: LocalDateTime get() = LocalDateTime.now().minusHours(2)

        private fun oppfriskningsperiode(tidligsteDato: LocalDate) =
            tidligsteDato.minusYears(4) til LocalDate.now()

        internal fun ferdigInfotrygdhistorikk(elementer: List<InfotrygdhistorikkElement>) = Infotrygdhistorikk(elementer.map { it }.toMutableList())
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, arbeidsgiver: Arbeidsgiver, periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
        return valider(aktivitetslogg, arbeidsgiver.periodetype(periode), periode, skjæringstidspunkt, arbeidsgiver.organisasjonsnummer(), arbeidsgiver.avgrensetPeriode(periode))
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, periodetype: Periodetype, periode: Periode, skjæringstidspunkt: LocalDate, orgnummer: String, avgrensetPeriode: Periode = periode): Boolean {
        if (!harHistorikk()) return true
        return siste.valider(aktivitetslogg, periodetype, periode, skjæringstidspunkt, orgnummer, avgrensetPeriode)
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

    internal fun harBetaltRettFør(periode: Periode): Boolean {
        if (!harHistorikk()) return false
        return siste.harBetaltRettFør(periode)
    }

    internal fun periodetype(organisasjonsnummer: String, other: Periode, dag: LocalDate): Periodetype? {
        if (!harHistorikk()) return null
        return siste.periodetype(organisasjonsnummer, other, dag)
    }

    internal fun ingenUkjenteArbeidsgivere(organisasjonsnumre: List<String>, dato: LocalDate): Boolean {
        if (!harHistorikk()) return true
        return siste.ingenUkjenteArbeidsgivere(organisasjonsnumre, dato)
    }

    internal fun skjæringstidspunkt(organisasjonsnummer: String, periode: Periode, tidslinje: Sykdomstidslinje): LocalDate {
        return Sykdomstidslinje.sisteRelevanteSkjæringstidspunktForPerioden(periode, listOf(tidslinje) + listOf(sykdomstidslinje(organisasjonsnummer))) ?: periode.start
    }

    internal fun skjæringstidspunkt(periode: Periode, tidslinjer: List<Sykdomstidslinje>): LocalDate {
        return Sykdomstidslinje.sisteRelevanteSkjæringstidspunktForPerioden(periode, tidslinjer + listOf(sykdomstidslinje())) ?: periode.start
    }

    internal fun skjæringstidspunkter(tidslinjer: List<Sykdomstidslinje>): List<LocalDate> {
        return Sykdomstidslinje.skjæringstidspunkter(tidslinjer + listOf(sykdomstidslinje()))
    }

    private fun sykdomstidslinje(orgnummer: String): Sykdomstidslinje {
        if (!harHistorikk()) return Sykdomstidslinje()
        return siste.sykdomstidslinje(orgnummer)
    }

    private fun sykdomstidslinje(): Sykdomstidslinje {
        if (!harHistorikk()) return Sykdomstidslinje()
        return siste.sykdomstidslinje()
    }

    internal fun sisteSykepengedag(orgnummer: String): LocalDate? {
        if (!harHistorikk()) return null
        return siste.sisteSykepengedag(orgnummer)
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

    internal fun arbeidsgiverperiodeFor(organisasjonsnummer: String, sykdomshistorikkId: UUID): List<Arbeidsgiverperiode>? {
        if (!harHistorikk()) return null
        return siste.arbeidsgiverperiodeFor(organisasjonsnummer, sykdomshistorikkId)
    }

    internal fun lagreResultat(organisasjonsnummer: String, sykdomshistorikkId: UUID, resultat: List<Arbeidsgiverperiode>) {
        if (!harHistorikk()) return
        return siste.lagreResultat(organisasjonsnummer, sykdomshistorikkId, resultat)
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
        build(organisasjonsnummer, sykdomstidslinje, InfotrygdUtbetalingstidslinjedekoratør(builder, sykdomstidslinje.førsteDag()), subsumsjonObserver)
        return fjernHistorikk(organisasjonsnummer, builder.result())
    }

    private fun fjernHistorikk(organisasjonsnummer: String, utbetalingstidslinje: Utbetalingstidslinje): Utbetalingstidslinje {
        if (!harHistorikk()) return utbetalingstidslinje
        return siste.fjernHistorikk(utbetalingstidslinje, organisasjonsnummer)
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
