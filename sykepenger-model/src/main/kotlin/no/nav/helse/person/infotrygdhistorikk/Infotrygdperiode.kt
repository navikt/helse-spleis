package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning.Companion.harInntekterFor
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*

abstract class Infotrygdperiode(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
    internal open fun sykdomstidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje = Sykdomstidslinje()
    internal open fun utbetalingstidslinje(): Utbetalingstidslinje = Utbetalingstidslinje()

    internal abstract fun accept(visitor: InfotrygdhistorikkVisitor)
    internal open fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode, nødnummer: Nødnummer) {}
    internal open fun validerOverlapp(aktivitetslogg: IAktivitetslogg, periode: Periode, nødnummer: Nødnummer) {}

    internal fun historikkFor(orgnummer: String, sykdomstidslinje: Sykdomstidslinje, kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje {
        if (!gjelder(orgnummer)) return sykdomstidslinje
        return sykdomstidslinje(kilde).merge(sykdomstidslinje, replace)
    }

    internal open fun gjelder(orgnummer: String) = true
    internal open fun utbetalingEtter(orgnumre: List<String>, dato: LocalDate) = false

    override fun hashCode() = Objects.hash(this::class, start, endInclusive)
    override fun equals(other: Any?): Boolean {
        if (other !is Infotrygdperiode) return false
        if (this::class != other::class) return false
        return super.equals(other)
    }

    companion object {
        internal fun Iterable<Infotrygdperiode>.validerInntektForPerioder(aktivitetslogg: IAktivitetslogg, inntekter: List<Inntektsopplysning>) {
            // Liste med første utbetalingsdag i hver periode etter:
            // filtrering av irrellevante perioder, padding av helgedager og sammenslåing av perioder som henger sammen
            val førsteUtbetalingsdager = this
                .filter { it is Utbetalingsperiode || it is Friperiode }
                .flatten()
                .fold(emptyList<LocalDate>()) { acc, nesteDag ->
                    if (nesteDag.dayOfWeek == DayOfWeek.FRIDAY) acc + (0..2L).map { nesteDag.plusDays(it) }
                    else acc + nesteDag
                }
                .grupperSammenhengendePerioder()
                .mapNotNull { periode ->
                    periode.firstOrNull {
                        it in this.filterIsInstance<Utbetalingsperiode>().flatten()
                    }
                }

            if (inntekter.harInntekterFor(førsteUtbetalingsdager)) return
            aktivitetslogg.info("Mangler inntekt for første utbetalingsdag i en av infotrygdperiodene: $førsteUtbetalingsdager")
            aktivitetslogg.error("Mangler inntekt for første utbetalingsdag i en av infotrygdperiodene")
        }

        internal fun Iterable<Infotrygdperiode>.harBrukerutbetalingFor(organisasjonsnummer: String, periode: Periode) = this
            .filter { it.gjelder(organisasjonsnummer) }
            .filter { it.overlapperMed(periode) }
            .any(Infotrygdperiode::harBrukerutbetaling)

        internal fun sorter(perioder: List<Infotrygdperiode>) =
            perioder.sortedBy { it.start }.sortedBy { it.hashCode() }

    }

    protected open fun harBrukerutbetaling() = false
}
