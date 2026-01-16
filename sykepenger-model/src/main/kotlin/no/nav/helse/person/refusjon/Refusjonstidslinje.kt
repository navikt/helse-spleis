package no.nav.helse.person.refusjon

import java.time.LocalDate
import no.nav.helse.Tidslinje
import no.nav.helse.Tidslinjedag
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

data class Refusjonsdag(val beløp: Inntekt, val kilde: Kilde) {
    override fun toString() = "Daglig=${beløp.dagligInt}, Avsender=${kilde.avsender}, Tidsstempel=${kilde.tidsstempel}, MeldingsreferanseId=${kilde.meldingsreferanseId.id}"
}

class Refusjonstidslinje(vararg perioder: Pair<Periode, Refusjonsdag>): Tidslinje<Refusjonsdag, Refusjonstidslinje>(*perioder) {
    override fun opprett(vararg perioder: Pair<Periode, Refusjonsdag>)= Refusjonstidslinje(*perioder)

    override fun pluss(eksisterendeVerdi: Refusjonsdag, nyVerdi: Refusjonsdag) =
        if (nyVerdi.kilde.tidsstempel >= eksisterendeVerdi.kilde.tidsstempel) nyVerdi
        else eksisterendeVerdi

    internal fun medBeløp() = filter { (it.verdi?.beløp ?: INGEN) != INGEN }.opprett()

    internal fun kunIngenRefusjon() = sumOf { it.verdi?.beløp?.årlig ?: 0.0 } == 0.0

    internal operator fun minus(other: Refusjonstidslinje) = filter { it.verdi != null }.filter { (dato, refusjonsdag) -> refusjonsdag!!.beløp != other[dato]?.beløp }.opprett()

    private fun List<Tidslinjedag<Refusjonsdag>>.opprett() = opprett(*somArray)

    // Fyller alle hull i refusjonstidslinjen med beløp & kilde fra forrige dag med refusjon
    internal fun fyll(): Refusjonstidslinje {
        var forrigeRefusjonsdag: Refusjonsdag = firstOrNull()?.verdi ?: return Refusjonstidslinje()

        val fylteDager = mapNotNull { tidslinjedag ->
            when (val refusjonsdag = tidslinjedag.verdi) {
                null -> Tidslinjedag(tidslinjedag.dato, forrigeRefusjonsdag)
                else -> tidslinjedag.also { forrigeRefusjonsdag = refusjonsdag }
            }
        }

        return Refusjonstidslinje(*fylteDager.somArray)
    }
    // Fyller og strekker
    internal fun fyll(periode: Periode) = fyll().strekk(periode).subset(periode)
    internal fun fyll(til: LocalDate): Refusjonstidslinje {
        val snute = snuteOgHale()?.first?.dato ?: return Refusjonstidslinje()
        val periode = periodeOrNull(snute, til) ?: return Refusjonstidslinje()
        return fyll(periode)
    }

    // De som vet, de vet👃🐈
    private fun snuteOgHale(): Pair<Tidslinjedag<Refusjonsdag>, Tidslinjedag<Refusjonsdag>>? {
        val snute = firstOrNull { it.verdi != null } ?: return null
        return snute to last { it.verdi != null }
    }

    // Strekker, men fyller ei
    internal fun strekk(periode: Periode): Refusjonstidslinje {
        val (snute, hale) = snuteOgHale() ?: return Refusjonstidslinje()

        val snuteTidslinje = periodeOrNull(periode.start, snute.dato.forrigeDag)?.let { snutePeriode ->
            Refusjonstidslinje(snutePeriode to snute.verdi!!)
        } ?: Refusjonstidslinje()

        val haleTidslinje = periodeOrNull(hale.dato.nesteDag, periode.endInclusive)?.let { halePeriode ->
            Refusjonstidslinje(halePeriode to hale.verdi!!)
        } ?: Refusjonstidslinje()

        return snuteTidslinje + this + haleTidslinje
    }

    internal companion object {
        internal val List<Tidslinjedag<Refusjonsdag>>.somArray get() = filter { it.verdi != null }.map { it.dato.somPeriode() to it.verdi!! }.toTypedArray()

        private fun periodeOrNull(fom: LocalDate, tom: LocalDate) = tom.takeIf { it >= fom }?.let { fom til tom }
    }
}
