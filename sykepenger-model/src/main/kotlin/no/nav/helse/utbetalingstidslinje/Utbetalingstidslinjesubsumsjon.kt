package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erRettFør
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserAap
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserDagpenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserForeldrepenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserOmsorgspenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserOpplaringspenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserPleiepenger
import no.nav.helse.utbetalingstidslinje.Begrunnelse.AndreYtelserSvangerskapspenger
import no.nav.helse.økonomi.Dekningsgrunnlagsubsumsjon
import no.nav.helse.økonomi.Økonomi
import kotlin.collections.last

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
    private val dekningsgrunnlag = mutableListOf<Dekningsgrunnlag>()

    fun subsummer(regler: ArbeidsgiverRegler) {
        subsumsjonslogg.`§ 8-17 ledd 1 bokstav a`(false, arbeidsgiverperiodedager, tidslinjesubsumsjonsformat)
        subsumsjonslogg.`§ 8-19 andre ledd`(arbeidsgiverperiodedager, tidslinjesubsumsjonsformat)
        subsumsjonslogg.`§ 8-17 ledd 1`(arbeidsgiverperiodeNavdager)
        subsumsjonslogg.`§ 8-11 ledd 1`(helger)
        subsumsjonslogg.`§ 8-17 ledd 2`(fridager, tidslinjesubsumsjonsformat)
        subsumsjonslogg.`§ 8-48 ledd 2 punktum 2`(aap, tidslinjesubsumsjonsformat)
        subsumsjonslogg.`Trygderettens kjennelse 2006-4023`(andreYtelser, tidslinjesubsumsjonsformat)
        // subsummerer alle periodene samlet, så lenge inntekten er lik
        dekningsgrunnlag
            .asSequence()
            .filter { it.inntekt.årligInntekt > 0 }
            .groupBy { it.inntekt }
            .forEach { (inntekt, perioder) ->
                subsumsjonslogg.`§ 8-16 ledd 1`(perioder.map { it.periode }, regler.dekningsgrad(), inntekt.årligInntekt, inntekt.årligDekningsgrunnlag)
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
        arbeidsgiverperiodedager.utvidForrigeDatoperiodeEllerLeggTil(dato)
        dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dato, økonomi)
    }

    override fun visit(
        dag: Utbetalingsdag.ArbeidsgiverperiodedagNav,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        arbeidsgiverperiodeNavdager.utvidForrigeDatoperiodeEllerLeggTil(dato)
        dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dato, økonomi)
    }

    override fun visit(
        dag: Utbetalingsdag.NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dato, økonomi)
    }

    override fun visit(
        dag: Utbetalingsdag.ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dato, økonomi)
    }

    override fun visit(
        dag: Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        helger.utvidForrigeDatoperiodeEllerLeggTil(dato)
        dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dato, økonomi)
    }

    override fun visit(
        dag: Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        fridager.utvidForrigeDatoperiodeEllerLeggTil(dato)
        dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dato, økonomi)
    }

    override fun visit(
        dag: Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        if (AndreYtelserAap in dag.begrunnelser) aap.utvidForrigeDatoperiodeEllerLeggTil(dato)
        val andreYtelser = setOf(
            AndreYtelserDagpenger, AndreYtelserForeldrepenger, AndreYtelserOmsorgspenger, AndreYtelserOpplaringspenger, AndreYtelserSvangerskapspenger, AndreYtelserPleiepenger
        )
        if (dag.begrunnelser.none { it in andreYtelser }) return
        this.andreYtelser.utvidForrigeDatoperiodeEllerLeggTil(dato)
    }

    private data class Dekningsgrunnlag(
        val periode: Periode,
        val inntekt: Dekningsgrunnlagsubsumsjon
    )

    private companion object {
        // utvider liste av perioder med ny dato. antar at listen er sortert i stigende rekkefølge,
        // og at <dato> må være nyere enn forrige periode. strekker altså -ikke- periodene eventuelt tilbake i tid, kun frem
        private fun MutableList<Periode>.utvidForrigeDatoperiodeEllerLeggTil(dato: LocalDate) {
            utvidForrigeDatoperiodeEllerLeggTil(
                dato = dato,
                finnPeriode = { it },
                oppdaterElement = { periodeFraFør -> periodeFraFør.oppdaterTom(dato) },
                nyttElement = { dato.somPeriode() }
            )
        }
        private fun MutableList<Dekningsgrunnlag>.utvidForrigeDatoperiodeEllerLeggTil(dato: LocalDate, økonomi: Økonomi) {
            utvidForrigeDatoperiodeEllerLeggTil(
                dato = dato,
                finnPeriode = { it.periode },
                oppdaterElement = { periodeFraFør -> periodeFraFør.copy(periode = periodeFraFør.periode.oppdaterTom(dato)) },
                nyttElement = { Dekningsgrunnlag(periode = dato.somPeriode(), inntekt = økonomi.subsumsjonsdata()) }
            )
        }
        private fun <E> MutableList<E>.utvidForrigeDatoperiodeEllerLeggTil(
            dato: LocalDate,
            finnPeriode: (E) -> Periode,
            oppdaterElement: (E) -> E,
            nyttElement: () -> E
        ) {
            when {
                // tom liste eller dagen utvider ikke siste datoperiode
                isEmpty() || !finnPeriode(last()).endInclusive.erRettFør(dato) -> add(nyttElement())
                // dagen utvider siste periode
                else -> removeLast().also { add(oppdaterElement(it)) }
            }
        }
    }
}