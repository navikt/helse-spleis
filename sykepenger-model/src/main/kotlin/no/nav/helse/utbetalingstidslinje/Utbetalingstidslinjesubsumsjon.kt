package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.Tidslinjedag
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder
import no.nav.helse.hendelser.Periode
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
        subsumsjonslogg.`§ 8-17 ledd 1 bokstav a`(false, dagen = dato, tidslinjesubsumsjonsformat)
        subsumsjonslogg.`§ 8-19 andre ledd`(dato, tidslinjesubsumsjonsformat)
    }

    override fun visit(
        dag: Utbetalingsdag.ArbeidsgiverperiodedagNav,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        subsumsjonslogg.`§ 8-17 ledd 1`(dato)
    }

    override fun visit(
        dag: Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        subsumsjonslogg.`§ 8-11 ledd 1`(dato)
    }

    override fun visit(
        dag: Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        subsumsjonslogg.`§ 8-17 ledd 2`(dato, tidslinjesubsumsjonsformat)
    }

    override fun visit(
        dag: Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        if (AndreYtelserAap in dag.begrunnelser) subsumsjonslogg.`§ 8-48 ledd 2 punktum 2`(dato, tidslinjesubsumsjonsformat)
        val andreYtelser = setOf(
            AndreYtelserDagpenger, AndreYtelserForeldrepenger, AndreYtelserOmsorgspenger, AndreYtelserOpplaringspenger, AndreYtelserSvangerskapspenger, AndreYtelserPleiepenger
        )
        if (dag.begrunnelser.none { it in andreYtelser }) return
        subsumsjonslogg.`Trygderettens kjennelse 2006-4023`(dato, tidslinjesubsumsjonsformat)
    }
}