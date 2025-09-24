package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.flattenMutableSet
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_3
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

internal class Arbeidsgiverperiodeberegner(
    private val arbeidsgiverperiodeteller: Arbeidsgiverperiodeteller,
) : Arbeidsgiverperiodeteller.Observatør {

    private val arbeidsgiverperioder = mutableListOf<Arbeidsgiverperioderesultat>()
    private var aktivArbeidsgiverperioderesultat: Arbeidsgiverperioderesultat? = null

    init {
        arbeidsgiverperiodeteller.observer(this)
    }

    private var tilstand: Tilstand = Initiell

    private fun arbeidsgiverperiodeResultatet(dato: LocalDate): Arbeidsgiverperioderesultat {
        return aktivArbeidsgiverperioderesultat ?: Arbeidsgiverperioderesultat(
            omsluttendePeriode = dato.somPeriode(),
            arbeidsgiverperiode = emptyList(),
            utbetalingsperioder = emptyList(),
            oppholdsperioder = emptyList(),
            fullstendig = false,
            ferdigAvklart = false
        ).also { aktivArbeidsgiverperioderesultat = it }
    }

    internal fun resultat(
        sykdomstidslinje: Sykdomstidslinje,
        infotrygdBetalteDager: List<Periode>,
        infotrygdFerieperioder: List<Periode>
    ): List<Arbeidsgiverperioderesultat> {
        val periodeFremTilSpleis = infotrygdperiodeFørSpleis(sykdomstidslinje.periode(), infotrygdBetalteDager, infotrygdFerieperioder)
        // vurderer en eventuell infotrygdperiode i forkant av spleis, i tilfelle
        // vi starter spleishistorikken med en ferdig avklart arbeidsgiverperiode
        val gjenståendeInfotrygdBetalteDager = infotrygdBetalteDager.flattenMutableSet()
        val gjenståendeInfotrygdFerieperioder = infotrygdFerieperioder.flattenMutableSet()

        periodeFremTilSpleis?.forEach { dato ->
            håndterUkjentDag(dato, gjenståendeInfotrygdBetalteDager, gjenståendeInfotrygdFerieperioder)
        }

        sykdomstidslinje.forEach { dag ->
            when (dag) {
                is Dag.AndreYtelser -> tilstand.andreYtelser(this, dag.dato)
                is Dag.ArbeidIkkeGjenopptattDag -> tilstand.feriedag(this, dag.dato)
                is Dag.Arbeidsdag -> arbeidsdag(dag.dato)
                is Dag.ArbeidsgiverHelgedag -> sykedag(dag.dato)
                is Dag.Arbeidsgiverdag -> egenmeldingsdag(dag.dato)
                is Dag.Feriedag -> feriedagMedSykmelding(dag.dato)
                is Dag.ForeldetSykedag -> foreldetDag(dag.dato)
                is Dag.FriskHelgedag -> arbeidsdag(dag.dato)
                is Dag.Permisjonsdag -> tilstand.feriedag(this, dag.dato)
                is Dag.ProblemDag -> throw ProblemdagException(dag.melding)
                is Dag.SykHelgedag -> {
                    ferdigstillTellingHvisInfotrygdHarUtbetalt(gjenståendeInfotrygdBetalteDager, dag.dato)
                    sykedag(dag.dato)
                }

                is Dag.Sykedag -> {
                    ferdigstillTellingHvisInfotrygdHarUtbetalt(gjenståendeInfotrygdBetalteDager, dag.dato)
                    sykedag(dag.dato)
                }

                is Dag.UkjentDag -> {
                    håndterUkjentDag(dag.dato, gjenståendeInfotrygdBetalteDager, gjenståendeInfotrygdFerieperioder)
                }
            }
        }
        fridager.somFerieOppholdsdager()

        return aktivArbeidsgiverperioderesultat
            ?.let { arbeidsgiverperioder.toList() + it }
            ?: arbeidsgiverperioder.toList()
    }

    private fun infotrygdperiodeFørSpleis(sykdomstidslinjeperiode: Periode?, infotrygdBetalteDager: List<Periode>, infotrygdFerieperioder: List<Periode>): Periode? {
        if (sykdomstidslinjeperiode == null) return null
        val samletInfotrygdperiode = (infotrygdBetalteDager + infotrygdFerieperioder).periode() ?: return null
        val periodeFørSpleis = samletInfotrygdperiode.utenDagerFør(sykdomstidslinjeperiode) ?: return null
        return periodeFørSpleis.oppdaterTom(sykdomstidslinjeperiode.start.forrigeDag)
    }

    private fun håndterUkjentDag(dato: LocalDate, infotrygdBetalteDager: MutableSet<LocalDate>, infotrygdFerieperioder: MutableSet<LocalDate>) {
        if (infotrygdFerieperioder.remove(dato)) {
            return feriedagMedSykmelding(dato)
        }

        if (infotrygdBetalteDager.remove(dato)) {
            arbeidsgiverperiodeteller.fullfør()
            return sykedag(dato)
        }

        if (dato.erHelg()) return tilstand.feriedag(this, dato)

        arbeidsdag(dato)
    }

    private fun tilstand(tilstand: Tilstand) {
        if (this.tilstand == tilstand) return
        this.tilstand.leaving(this)
        this.tilstand = tilstand
        this.tilstand.entering(this)
    }

    private fun ferdigstillTellingHvisInfotrygdHarUtbetalt(infotrygdBetalteDager: MutableSet<LocalDate>, dato: LocalDate) {
        if (infotrygdBetalteDager.remove(dato)) arbeidsgiverperiodeteller.fullfør()
    }

    override fun arbeidsgiverperiodeFerdig() {
        tilstand(ArbeidsgiverperiodeSisteDag)
    }

    override fun arbeidsgiverperiodedag() {
        tilstand(Arbeidsgiverperiode)
    }

    override fun sykedag() {
        tilstand(Utbetaling)
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        tilstand(ArbeidsgiverperiodeAvbrutt)
    }

    private fun MutableList<LocalDate>.somSykedager() {
        onEach {
            arbeidsgiverperiodeteller.inc()
            tilstand.feriedagSomSyk(this@Arbeidsgiverperiodeberegner, it)
        }.clear()
    }

    private fun MutableList<LocalDate>.somFerieOppholdsdager() {
        onEach {
            arbeidsgiverperiodeteller.dec()
            tilstand.oppholdsdag(this@Arbeidsgiverperiodeberegner, it)
        }.clear()
    }

    private fun sykedag(dato: LocalDate) {
        fridager.somSykedager()
        arbeidsgiverperiodeteller.inc()
        tilstand.sykdomsdag(this, dato)
    }

    private fun egenmeldingsdag(dato: LocalDate) {
        fridager.somSykedager()
        arbeidsgiverperiodeteller.inc()
        tilstand.egenmeldingsdag(this, dato)
    }

    private fun feriedagMedSykmelding(dato: LocalDate) {
        fridager.somSykedager()
        tilstand.feriedagMedSykmelding(this, dato)
    }

    private fun arbeidsdag(dato: LocalDate) {
        fridager.somFerieOppholdsdager()
        arbeidsgiverperiodeteller.dec()
        tilstand.oppholdsdag(this, dato)
        tilstand(Initiell)
    }

    private fun foreldetDag(dato: LocalDate) {
        fridager.somSykedager()
        arbeidsgiverperiodeteller.inc()
        tilstand.foreldetDag(this, dato)
    }

    private val fridager = mutableListOf<LocalDate>()

    private interface Tilstand {
        fun entering(builder: Arbeidsgiverperiodeberegner) {}
        fun oppholdsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                oppholdsperiode = dato
            )
        }

        fun sykdomsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                ferdigAvklart = true,
                utbetalingsperiode = dato
            )
        }

        fun egenmeldingsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)
        fun foreldetDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)

        fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate)
        fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate)
        fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate)
        fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate)
        fun leaving(builder: Arbeidsgiverperiodeberegner) {}
    }

    private object Initiell : Tilstand {
        override fun oppholdsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat?.utvideMed(
                dato = dato,
                oppholdsperiode = dato
            )?.also {
                builder.aktivArbeidsgiverperioderesultat = it
            }
        }

        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.arbeidsgiverperiodeteller.dec()
            builder.tilstand.oppholdsdag(builder, dato)
        }

        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = feriedag(builder, dato)
        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = feriedag(builder, dato)

        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            throw IllegalStateException()
        }
    }

    private object Arbeidsgiverperiode : Tilstand {
        override fun sykdomsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                arbeidsgiverperiode = dato
            )
        }

        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)
        override fun foreldetDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)

        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.arbeidsgiverperiodeteller.inc()
            builder.tilstand.feriedagSomSyk(builder, dato)
        }

        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.fridager.add(dato)
        }

        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = feriedag(builder, dato)
    }

    private object ArbeidsgiverperiodeSisteDag : Tilstand {
        override fun sykdomsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                arbeidsgiverperiode = dato,
                fullstendig = true,
                ferdigAvklart = true
            )
            builder.tilstand(Utbetaling)
        }

        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)
        override fun foreldetDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = sykdomsdag(builder, dato)

        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            throw IllegalStateException("kan ikke ha fridag som siste dag i arbeidsgiverperioden")
        }

        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            throw IllegalStateException("kan ikke ha fridag som siste dag i arbeidsgiverperioden")
        }

        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            throw IllegalStateException("kan ikke ha andre ytelser som siste dag i arbeidsgiverperioden")
        }
    }

    private object Utbetaling : Tilstand {
        private fun kjentDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato
            )
        }

        override fun egenmeldingsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = kjentDag(builder, dato)
        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = kjentDag(builder, dato)
        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = kjentDag(builder, dato)
        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = kjentDag(builder, dato)
        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = kjentDag(builder, dato)
    }

    private object ArbeidsgiverperiodeAvbrutt : Tilstand {
        override fun oppholdsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) {
            builder.aktivArbeidsgiverperioderesultat = builder.arbeidsgiverperiodeResultatet(dato).utvideMed(
                dato = dato,
                oppholdsperiode = dato
            )
            builder.tilstand(Initiell)
        }

        override fun feriedag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = oppholdsdag(builder, dato)
        override fun feriedagMedSykmelding(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = oppholdsdag(builder, dato)
        override fun andreYtelser(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = oppholdsdag(builder, dato)

        override fun sykdomsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = throw IllegalStateException()
        override fun egenmeldingsdag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = throw IllegalStateException()
        override fun foreldetDag(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = throw IllegalStateException()
        override fun feriedagSomSyk(builder: Arbeidsgiverperiodeberegner, dato: LocalDate) = throw IllegalStateException()

        override fun leaving(builder: Arbeidsgiverperiodeberegner) {
            builder.aktivArbeidsgiverperioderesultat?.let {
                builder.arbeidsgiverperioder.add(it)
            }
            builder.aktivArbeidsgiverperioderesultat = null
        }
    }
}

class ProblemdagException(melding: String) : RuntimeException("Forventet ikke ProblemDag i utbetalingstidslinjen. Melding: $melding") {
    fun logg(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Feilmelding: $message")
        aktivitetslogg.funksjonellFeil(RV_UT_3)
    }
}
