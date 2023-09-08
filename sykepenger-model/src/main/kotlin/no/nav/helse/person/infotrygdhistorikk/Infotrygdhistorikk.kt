package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeBuilder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeMediator
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeteller
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class Infotrygdhistorikk private constructor(
    private val elementer: MutableList<InfotrygdhistorikkElement>
) {
    private val siste get() = elementer.first()

    constructor() : this(mutableListOf())

    internal companion object {
        private fun oppfriskningsperiode(tidligsteDato: LocalDate) =
            tidligsteDato.minusYears(4) til LocalDate.now()

        internal fun ferdigInfotrygdhistorikk(elementer: List<InfotrygdhistorikkElement>) = Infotrygdhistorikk(elementer.map { it }.toMutableList())
    }

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
        orgnummer: String
    ): Boolean {
        if (!harHistorikk()) return true
        return siste.valider(aktivitetslogg, periode, orgnummer)
    }

    internal fun oppfriskNødvendig(aktivitetslogg: IAktivitetslogg, tidligsteDato: LocalDate): Boolean {
        if (harHistorikk()) return false
        oppfrisk(aktivitetslogg, tidligsteDato)
        return true
    }

    internal fun oppfrisk(aktivitetslogg: IAktivitetslogg, tidligsteDato: LocalDate) {
        utbetalingshistorikk(aktivitetslogg, oppfriskningsperiode(tidligsteDato))
    }

    internal fun utbetalingstidslinje(): Utbetalingstidslinje {
        if (!harHistorikk()) return Utbetalingstidslinje()
        return siste.utbetalingstidslinje()
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

    internal fun oppdaterHistorikk(element: InfotrygdhistorikkElement): Boolean {
        if (harHistorikk() && element.erstatter(siste)) return false
        elementer.add(0, element)
        return true
    }

    internal fun harEndretHistorikk(utbetaling: Utbetaling): Boolean {
        if (!harHistorikk()) return false
        val sisteElementSomFantesFørUtbetaling = elementer.firstOrNull{
            it.erEldreEnn(utbetaling)
        } ?: return true
        return siste.erEndretUtbetaling(sisteElementSomFantesFørUtbetaling)
    }

    internal fun tøm() {
        if (!harHistorikk()) return
        val nyeste = siste
        elementer.clear()
        elementer.add(nyeste)
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
        subsumsjonObserver: SubsumsjonObserver?
    ) {
        val teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
        val arbeidsgiverperiodeBuilder = ArbeidsgiverperiodeBuilder(teller, builder, subsumsjonObserver)
        if (!harHistorikk()) return sykdomstidslinje.accept(arbeidsgiverperiodeBuilder)
        siste.build(organisasjonsnummer, sykdomstidslinje, teller, arbeidsgiverperiodeBuilder)
    }

    internal fun buildUtbetalingstidslinje(
        organisasjonsnummer: String,
        sykdomstidslinje: Sykdomstidslinje,
        builder: ArbeidsgiverperiodeMediator,
        subsumsjonObserver: SubsumsjonObserver
    ) {
        val dekoratør = if (harHistorikk()) InfotrygdUtbetalingstidslinjedekoratør(builder, sykdomstidslinje.periode()!!, siste.betaltePerioder()) else builder
        build(organisasjonsnummer, sykdomstidslinje, dekoratør, subsumsjonObserver)
    }
    internal fun utbetalingshistorikkEtterInfotrygdendring(
        vedtaksperiodeId: UUID,
        vedtaksperiode: Periode,
        tilstand: String,
        organisasjonsnummer: String,
        person: Person
    ) {
        if (!harHistorikk()) return
        siste.utbetalingshistorikkEtterInfotrygdendring(vedtaksperiodeId, vedtaksperiode, tilstand, organisasjonsnummer, person)
    }


    private fun harHistorikk() = elementer.isNotEmpty()
    internal fun harUtbetaltI(periode: Periode): Boolean {
        if (!harHistorikk()) return false
        return siste.harUtbetaltI(periode)
    }

    fun villeBlittFiktiv(organisasjonsnummer: String, arbeidsgiverperiode: Arbeidsgiverperiode): Boolean {
        if (!harHistorikk()) return false
        return siste.villeBlittFiktiv(organisasjonsnummer, arbeidsgiverperiode)
    }
}
