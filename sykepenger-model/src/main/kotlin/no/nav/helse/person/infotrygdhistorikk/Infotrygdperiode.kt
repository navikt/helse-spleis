package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

sealed class Infotrygdperiode(fom: LocalDate, tom: LocalDate) {
    val periode = fom til tom

    internal open fun sykdomstidslinje(kilde: Hendelseskilde): Sykdomstidslinje = Sykdomstidslinje()
    internal open fun utbetalingstidslinje(): Utbetalingstidslinje = Utbetalingstidslinje()

    internal fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode, strategi: IAktivitetslogg.(Varselkode) -> Unit) {
        if (harBetaltTidligere(periode)) aktivitetslogg.strategi(Varselkode.RV_IT_37)
        if (harBetaltRettFør(periode)) aktivitetslogg.strategi(Varselkode.RV_IT_14)
        if (harOverlapp(aktivitetslogg, periode)) aktivitetslogg.varsel(Varselkode.RV_IT_3)
    }

    private fun harBetaltTidligere(other: Periode): Boolean {
        val periodeMellom = periode.periodeMellom(other.start) ?: return false
        return periodeMellom.count() < Vedtaksperiode.MINIMALT_TILLATT_AVSTAND_TIL_INFOTRYGD
    }

    private fun harBetaltRettFør(other: Periode): Boolean {
        return this.periode.erRettFør(other)
    }

    private fun harOverlapp(aktivitetslogg: IAktivitetslogg, periode: Periode): Boolean {
        if (!this.periode.overlapperMed(periode)) return false
        aktivitetslogg.info("Utbetaling i Infotrygd %s til %s overlapper med vedtaksperioden", this.periode.start, this.periode.endInclusive)
        return true
    }

    internal fun validerNyereOpplysninger(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        if (this.periode.start <= periode.endInclusive) return
        val periodeMellom = periode.endInclusive.datesUntil(this.periode.start).count()
        if (periodeMellom > MINIMALT_TILLAT_AVSTAND_NYERE_OPPLYSNINGER) return
        aktivitetslogg.varsel(Varselkode.RV_IT_1)
    }

    internal fun overlapperMed(other: Periode) = periode.overlapperMed(other)

    internal open fun gjelder(orgnummer: String) = true

    internal open fun funksjoneltLik(other: Infotrygdperiode): Boolean {
        if (this::class != other::class) return false
        return this.periode == other.periode
    }

    internal companion object {
        private const val MINIMALT_TILLAT_AVSTAND_NYERE_OPPLYSNINGER = 182

        internal fun sorter(perioder: List<Infotrygdperiode>) =
            perioder.sortedWith(compareBy({ it.periode.start }, { it.periode.endInclusive }, { it::class.simpleName }))

        internal fun List<Infotrygdperiode>.utbetalingsperioder(organisasjonsnummer: String? = null) = this
            .filterIsInstance<Utbetalingsperiode>()
            .filter { organisasjonsnummer == null || it.gjelder(organisasjonsnummer) }
            .map { it.periode }
    }
}
