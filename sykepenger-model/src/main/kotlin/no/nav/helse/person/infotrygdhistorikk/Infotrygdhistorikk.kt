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
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class Infotrygdhistorikk private constructor(
    private val _elementer: MutableList<InfotrygdhistorikkElement>
) {
    val elementer get() = _elementer.toList()
    val siste get() = _elementer.first()

    constructor() : this(mutableListOf())

    internal companion object {
        internal fun gjenopprett(dto: InfotrygdhistorikkInnDto): Infotrygdhistorikk {
            return Infotrygdhistorikk(
                _elementer = dto.elementer.map { InfotrygdhistorikkElement.gjenopprett(it) }.toMutableList()
            )
        }
    }

    internal fun validerMedFunksjonellFeil(
        aktivitetslogg: IAktivitetslogg,
        periode: Periode
    ): Boolean {
        if (!harHistorikk()) return true
        return siste.validerMedFunksjonellFeil(aktivitetslogg, periode)
    }

    internal fun validerMedVarsel(
        aktivitetslogg: IAktivitetslogg,
        periode: Periode
    ) {
        if (!harHistorikk()) return
        siste.validerMedVarsel(aktivitetslogg, periode)
    }

    internal fun validerNyereOpplysninger(
        aktivitetslogg: IAktivitetslogg,
        periode: Periode
    ) {
        if (!harHistorikk()) return
        siste.validerNyereOpplysninger(aktivitetslogg, periode)
    }

    internal fun oppfriskNødvendig(aktivitetslogg: IAktivitetslogg, tidligsteDato: LocalDate) {
        oppfrisk(aktivitetslogg, tidligsteDato)
    }

    internal fun oppfrisk(aktivitetslogg: IAktivitetslogg, tidligsteDato: LocalDate) {
        utbetalingshistorikk(aktivitetslogg, oppfriskningsperiode(tidligsteDato))
    }

    private fun oppfriskningsperiode(tidligsteDato: LocalDate): Periode {
        val fireÅrFørSpleisdag = tidligsteDato.minusYears(4)
        val førsteInfotrygddag = if (harHistorikk()) siste.perioder.firstOrNull()?.periode?.start else null
        val fom = førsteInfotrygddag?.let { minOf(fireÅrFørSpleisdag, førsteInfotrygddag) } ?: fireÅrFørSpleisdag
        return fom til LocalDate.now()
    }

    internal fun utbetalingstidslinje(): Utbetalingstidslinje {
        if (!harHistorikk()) return Utbetalingstidslinje()
        return siste.utbetalingstidslinje()
    }

    internal fun skjæringstidspunkt(tidslinjer: List<Sykdomstidslinje>): Skjæringstidspunkt {
        return Sykdomstidslinje.beregnSkjæringstidspunkt(tidslinjer + listOf(sykdomstidslinje()))
    }

    private fun sykdomstidslinje(): Sykdomstidslinje {
        if (!harHistorikk()) return Sykdomstidslinje()
        return siste.sykdomstidslinje()
    }

    internal fun oppdaterHistorikk(element: InfotrygdhistorikkElement): LocalDate? {
        if (harHistorikk() && element.erstatter(siste)) return null
        val forrige = _elementer.firstOrNull()
        _elementer.add(0, element)
        return element.tidligsteEndringMellom(forrige)
    }

    internal fun tøm() {
        if (!harHistorikk()) return
        val nyeste = siste
        _elementer.clear()
        _elementer.add(nyeste)
    }

    internal fun betaltePerioder(orgnummer: String? = null) =
        if (!harHistorikk()) emptyList() else siste.betaltePerioder(orgnummer)

    internal fun friperioder() =
        if (!harHistorikk()) emptyList() else siste.friperioder()

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
