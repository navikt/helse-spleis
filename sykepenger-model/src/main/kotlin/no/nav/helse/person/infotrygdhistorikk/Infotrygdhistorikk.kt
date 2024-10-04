package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.dto.deserialisering.InfotrygdhistorikkInnDto
import no.nav.helse.dto.serialisering.InfotrygdhistorikkUtDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Skjæringstidspunkt
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class Infotrygdhistorikk private constructor(
    private val _elementer: MutableList<InfotrygdhistorikkElement>
) {
    val elementer get() = _elementer.toList()
    val siste get() = _elementer.first()

    constructor() : this(mutableListOf())

    internal companion object {
        private fun oppfriskningsperiode(tidligsteDato: LocalDate) =
            tidligsteDato.minusYears(4) til LocalDate.now()

        internal fun gjenopprett(dto: InfotrygdhistorikkInnDto): Infotrygdhistorikk {
            return Infotrygdhistorikk(
                _elementer = dto.elementer.map { InfotrygdhistorikkElement.gjenopprett(it) }.toMutableList()
            )
        }
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

    internal fun oppfriskNødvendig(aktivitetslogg: IAktivitetslogg, tidligsteDato: LocalDate) {
        oppfrisk(aktivitetslogg, tidligsteDato)
    }

    internal fun oppfrisk(aktivitetslogg: IAktivitetslogg, tidligsteDato: LocalDate) {
        utbetalingshistorikk(aktivitetslogg, oppfriskningsperiode(tidligsteDato))
    }

    internal fun utbetalingstidslinje(): Utbetalingstidslinje {
        if (!harHistorikk()) return Utbetalingstidslinje()
        return siste.utbetalingstidslinje()
    }

    internal fun skjæringstidspunkt(tidslinjer: List<Sykdomstidslinje>): Skjæringstidspunkt {
        return Sykdomstidslinje.beregnSkjæringstidspunkt(tidslinjer + listOf(sykdomstidslinje()))
    }

    internal fun sykdomstidslinje(orgnummer: String): Sykdomstidslinje {
        if (!harHistorikk()) return Sykdomstidslinje()
        return siste.sykdomstidslinje(orgnummer)
    }

    private fun sykdomstidslinje(): Sykdomstidslinje {
        if (!harHistorikk()) return Sykdomstidslinje()
        return siste.sykdomstidslinje()
    }

    internal fun oppdaterHistorikk(element: InfotrygdhistorikkElement): Boolean {
        if (harHistorikk() && element.erstatter(siste)) return false
        _elementer.add(0, element)
        return true
    }

    internal fun harEndretHistorikk(utbetaling: Utbetaling): Boolean {
        if (!harHistorikk()) return false
        val sisteElementSomFantesFørUtbetaling = _elementer.firstOrNull{
            it.erEldreEnn(utbetaling)
        } ?: return siste.erNyopprettet()
        return siste.erEndretUtbetaling(sisteElementSomFantesFørUtbetaling)
    }

    internal fun tøm() {
        if (!harHistorikk()) return
        val nyeste = siste
        _elementer.clear()
        _elementer.add(nyeste)
    }

    internal fun betaltePerioder(orgnummer: String? = null) =
        if (!harHistorikk()) emptyList() else siste.betaltePerioder(orgnummer)

    internal fun harHistorikk() = _elementer.isNotEmpty()
    internal fun harUtbetaltI(periode: Periode): Boolean {
        if (!harHistorikk()) return false
        return siste.harUtbetaltI(periode)
    }

    internal fun harFerieI(periode: Periode): Boolean {
        if (!harHistorikk()) return false
        return siste.harFerieI(periode)
    }

    internal fun dto() = InfotrygdhistorikkUtDto(
        elementer = this._elementer.map { it.dto() }
    )
}
