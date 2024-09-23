package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.Tidslinjedag
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.nesteDag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserAap
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserDagpenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserForeldrepenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserOmsorgspenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserOpplaringspenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserPleiepenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserSvangerskapspenger
import no.nav.helse.økonomi.Økonomi

internal class Utbetalingstidslinjesubsumsjon(
    private val subsumsjonslogg: Subsumsjonslogg,
    private val sykdomstidslinje: Sykdomstidslinje
) : UtbetalingstidslinjeVisitor {

    // private val tidslinjesubsumsjonsformat = mutableListOf<Tidslinjedag>()
    private val tidslinjesubsumsjonsformat = sykdomstidslinje.subsumsjonsformat()
    private val arbeidsgiverperiodedager = mutableListOf<Periode>()
    private val arbeidsgiverperiodeNavdager = mutableListOf<Periode>()
    private val helger = mutableListOf<Periode>()
    private val fridager = mutableListOf<Periode>()
    private val aap = mutableListOf<Periode>()
    private val andreYtelser = mutableListOf<Periode>()

    fun subsummer() {
        arbeidsgiverperiodedager.forEach { periode ->
            subsumsjonslogg.`§ 8-17 ledd 1 bokstav a`(false, dagen = periode, tidslinjesubsumsjonsformat)
            subsumsjonslogg.`§ 8-19 andre ledd`(periode, tidslinjesubsumsjonsformat)
        }
        arbeidsgiverperiodeNavdager.forEach { periode ->
            subsumsjonslogg.`§ 8-17 ledd 1`(periode)
        }
        helger.forEach { periode ->
            subsumsjonslogg.`§ 8-11 ledd 1`(periode)
        }
        fridager.forEach { periode ->
            subsumsjonslogg.`§ 8-17 ledd 2`(periode, tidslinjesubsumsjonsformat)
        }
        aap.forEach { periode ->
            subsumsjonslogg.`§ 8-48 ledd 2 punktum 2`(periode, tidslinjesubsumsjonsformat)
        }
        andreYtelser.forEach { periode ->
            subsumsjonslogg.`Trygderettens kjennelse 2006-4023`(periode, tidslinjesubsumsjonsformat)
        }
    }

    override fun preVisitUtbetalingstidslinje(
        tidslinje: Utbetalingstidslinje,
        gjeldendePeriode: Periode?
    ) {
        //tidslinjesubsumsjonsformat.clear()
        //tidslinjesubsumsjonsformat.addAll(UtbetalingstidslinjeBuilder(tidslinje).dager())
    }

    override fun visit(
        dag: Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        arbeidsgiverperiodedager.leggTil(dato)
    }

    override fun visit(
        dag: Utbetalingsdag.ArbeidsgiverperiodedagNav,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        arbeidsgiverperiodeNavdager.leggTil(dato)
    }

    override fun visit(
        dag: Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        helger.leggTil(dato)
    }

    override fun visit(
        dag: Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
       fridager.leggTil(dato)
    }

    override fun visit(
        dag: Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        if (AndreYtelserAap in dag.begrunnelser) aap.leggTil(dato)
        val andreYtelser = setOf(
            AndreYtelserDagpenger, AndreYtelserForeldrepenger, AndreYtelserOmsorgspenger, AndreYtelserOpplaringspenger, AndreYtelserSvangerskapspenger, AndreYtelserPleiepenger
        )
        if (dag.begrunnelser.none { it in andreYtelser }) return
        this.andreYtelser.leggTil(dato)
    }

    private companion object {
        // utvider liste av perioder med ny dato. antar at listen er sortert i stigende rekkefølge,
        // og at <dato> må være nyere enn forrige periode. strekker altså -ikke- periodene eventuelt tilbake i tid, kun frem
        private fun MutableList<Periode>.leggTil(dato: LocalDate) {
            when {
                // tom liste eller dagen utvider ikke siste datoperiode
                isEmpty() || dato > last().endInclusive.nesteDag -> add(dato.somPeriode())
                // dagen utvider siste periode
                else -> removeLast().also { add(it.oppdaterTom(dato)) }
            }
        }
    }
}