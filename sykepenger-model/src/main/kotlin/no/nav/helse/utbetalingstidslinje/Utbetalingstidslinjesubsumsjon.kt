package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erRettFør
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-11 ledd 1`
import no.nav.helse.etterlevelse.`§ 8-16 ledd 1`
import no.nav.helse.etterlevelse.`§ 8-17 ledd 1`
import no.nav.helse.etterlevelse.`§ 8-17 ledd 1 bokstav a`
import no.nav.helse.etterlevelse.`§ 8-17 ledd 2`
import no.nav.helse.etterlevelse.`§ 8-19 andre ledd`
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
    private val sykdomstidslinje: Sykdomstidslinje,
    utbetalingstidslinje: Utbetalingstidslinje
) {

    // private val tidslinjesubsumsjonsformat = mutableListOf<Tidslinjedag>()
    private val tidslinjesubsumsjonsformat = sykdomstidslinje.subsumsjonsformat()
    private val arbeidsgiverperiodedager = mutableListOf<Periode>()
    private val arbeidsgiverperiodeNavdager = mutableListOf<Periode>()
    private val helger = mutableListOf<Periode>()
    private val fridager = mutableListOf<Periode>()
    private val aap = mutableListOf<Periode>()
    private val andreYtelser = mutableListOf<Periode>()
    private val dekningsgrunnlag = mutableListOf<Dekningsgrunnlag>()

    init {
        utbetalingstidslinje.forEach { dag ->
            when (dag) {
                is Utbetalingsdag.ArbeidsgiverperiodeDag -> {
                    arbeidsgiverperiodedager.utvidForrigeDatoperiodeEllerLeggTil(dag.dato)
                    dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dag.dato, dag.økonomi)
                }
                is Utbetalingsdag.ArbeidsgiverperiodedagNav -> {
                    arbeidsgiverperiodeNavdager.utvidForrigeDatoperiodeEllerLeggTil(dag.dato)
                    dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dag.dato, dag.økonomi)
                }
                is Utbetalingsdag.AvvistDag -> {
                    if (AndreYtelserAap in dag.begrunnelser) aap.utvidForrigeDatoperiodeEllerLeggTil(dag.dato)
                    val andreYtelser = setOf(
                        AndreYtelserDagpenger, AndreYtelserForeldrepenger, AndreYtelserOmsorgspenger, AndreYtelserOpplaringspenger, AndreYtelserSvangerskapspenger, AndreYtelserPleiepenger
                    )
                    if (dag.begrunnelser.any { it in andreYtelser }) {
                        this.andreYtelser.utvidForrigeDatoperiodeEllerLeggTil(dag.dato)
                    }
                }
                is Utbetalingsdag.ForeldetDag -> {
                    dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dag.dato, dag.økonomi)
                }
                is Utbetalingsdag.Fridag -> {
                    fridager.utvidForrigeDatoperiodeEllerLeggTil(dag.dato)
                    dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dag.dato, dag.økonomi)
                }
                is Utbetalingsdag.NavDag -> {
                    dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dag.dato, dag.økonomi)
                }
                is Utbetalingsdag.NavHelgDag -> {
                    helger.utvidForrigeDatoperiodeEllerLeggTil(dag.dato)
                    dekningsgrunnlag.utvidForrigeDatoperiodeEllerLeggTil(dag.dato, dag.økonomi)
                }
                is Utbetalingsdag.Arbeidsdag,
                is Utbetalingsdag.UkjentDag -> { /* gjør ingenting */ }
            }
        }
    }

    fun subsummer(vedtaksperiode: Periode, regler: ArbeidsgiverRegler) {
        subsumsjonslogg.logg(`§ 8-17 ledd 1 bokstav a`(false, arbeidsgiverperiodedager, tidslinjesubsumsjonsformat))
        subsumsjonslogg.logg(`§ 8-19 andre ledd`(arbeidsgiverperiodedager, tidslinjesubsumsjonsformat))
        subsumsjonslogg.logg(`§ 8-17 ledd 1`(arbeidsgiverperiodeNavdager))
        subsumsjonslogg.logg(`§ 8-11 ledd 1`(vedtaksperiode, helger))
        subsumsjonslogg.logg(`§ 8-17 ledd 2`(fridager, tidslinjesubsumsjonsformat))
        subsumsjonslogg.`§ 8-48 ledd 2 punktum 2`(aap, tidslinjesubsumsjonsformat)
        subsumsjonslogg.`Trygderettens kjennelse 2006-4023`(andreYtelser, tidslinjesubsumsjonsformat)
        // subsummerer alle periodene samlet, så lenge inntekten er lik
        dekningsgrunnlag
            .asSequence()
            .filter { it.inntekt.årligInntekt > 0 }
            .groupBy { it.inntekt }
            .forEach { (inntekt, perioder) ->
                subsumsjonslogg.logg(`§ 8-16 ledd 1`(perioder.map { it.periode }, regler.dekningsgrad(), inntekt.årligInntekt, inntekt.årligDekningsgrunnlag))
            }
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