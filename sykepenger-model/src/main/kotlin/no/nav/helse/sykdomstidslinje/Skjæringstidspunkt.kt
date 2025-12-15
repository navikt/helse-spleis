package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til

internal data class Skjæringstidspunkter(val skjæringstidspunkter: List<Periode>) {
    fun alle(søkeperiode: Periode): List<LocalDate> {
        return skjæringstidspunkter
            .filter { søkeperiode.overlapperMed(it) }
            .map { it.start }
            .reversed()
    }

    fun sisteOrNull(vedtaksperiode: Periode): LocalDate? {
        return alle(vedtaksperiode).firstOrNull()
    }

    fun dto() = skjæringstidspunkter.map { it.dto() }

    companion object {
        fun gjenopprett(perioder: List<PeriodeDto>) =
            Skjæringstidspunkter(perioder.map { Periode.gjenopprett(it) })
    }
}

internal class Skjæringstidspunkt(private val personsykdomstidslinje: Sykdomstidslinje) {

    fun alle(): Skjæringstidspunkter {
        return Skjæringstidspunkter(finnSkjæringstidspunkt().map { it.periode })
    }

    private fun finnSkjæringstidspunkt(): List<Søkekontekst> {
        if (personsykdomstidslinje.count() == 0) return emptyList()
        val resultater = mutableListOf<Søkekontekst>()
        var aktivtSkjæringspunkt: Søkekontekst? = null
        /*
        det skal være nytt skjæringstidspunkt, når dagen er en sykedag,
        _med mindre_ dagen før også er en sykedag,
        _eller_ dagen før er en feriedag og det bare er feriedager bakover mot forrige sykedag

        soooo om vi står på en feriedag, så holder vi en state _syk ferie_ om dagen før er syk, eller dagen før også hadde _syk_ferie_

        og så, når vi kommer til sykedag igjen, så blir det nytt skjæringstidspunkt med mindre syk-ferie er flagget..
         */
        personsykdomstidslinje.forEach { dagen ->
            when (dagen) {
                is Dag.Feriedag,
                is Dag.ArbeidIkkeGjenopptattDag,
                is Dag.Arbeidsdag,
                is Dag.FriskHelgedag,
                is Dag.Permisjonsdag,
                is Dag.ProblemDag,
                is Dag.AndreYtelser -> aktivtSkjæringspunkt = aktivtSkjæringspunkt?.utvid(dagen)

                is Dag.ArbeidsgiverHelgedag,
                is Dag.Arbeidsgiverdag,
                is Dag.MeldingTilNavDag,
                is Dag.MeldingTilNavHelgedag,
                is Dag.ForeldetSykedag,
                is Dag.SykHelgedag,
                is Dag.Sykedag -> {
                    aktivtSkjæringspunkt = when (aktivtSkjæringspunkt) {
                        null -> Søkekontekst(dagen)
                        else -> when {
                            aktivtSkjæringspunkt.sisteDag is Dag.Feriedag -> {
                                aktivtSkjæringspunkt.utvid(dagen)
                            }
                            // er i aktivt skjæringstidspunkt, så det må ha vært sykdom
                            else -> if (aktivtSkjæringspunkt.sisteDagErDelAvSykeforløp()) {
                                aktivtSkjæringspunkt.utvid(dagen)
                            } else {
                                resultater.add(aktivtSkjæringspunkt)
                                Søkekontekst(dagen)
                            }
                        }
                    }
                }

                is Dag.UkjentDag -> when (dagen.dato.erHelg()) {
                    true -> aktivtSkjæringspunkt = aktivtSkjæringspunkt?.utvid(dagen)
                    false -> {
                        aktivtSkjæringspunkt?.also { resultater.add(it) }
                        aktivtSkjæringspunkt = null
                    }
                }
            }
        }
        return resultater + listOfNotNull(aktivtSkjæringspunkt)
    }

    /*
    personsykdomstidslinje.forEach { dagen ->
            when (dagen) {
                is Dag.AndreYtelser -> aktivtSkjæringspunkt = aktivtSkjæringspunkt?.utvidMedAndreYtelser(dagen.dato)
                is Dag.Feriedag -> aktivtSkjæringspunkt = aktivtSkjæringspunkt?.utvid(dagen.dato)

                is Dag.ArbeidIkkeGjenopptattDag,
                is Dag.Arbeidsdag,
                is Dag.FriskHelgedag -> {
                    aktivtSkjæringspunkt?.also { resultater.add(it) }
                    aktivtSkjæringspunkt = null
                }

                is Dag.ArbeidsgiverHelgedag,
                is Dag.Arbeidsgiverdag,
                is Dag.MeldingTilNavDag,
                is Dag.MeldingTilNavHelgedag,
                is Dag.ForeldetSykedag,
                is Dag.SykHelgedag,
                is Dag.Sykedag -> {
                    aktivtSkjæringspunkt = when (aktivtSkjæringspunkt) {
                        null -> Søkekontekst(dagen.dato)
                        else -> when {
                            // det er alltid nytt skjæringstidspunkt etter en periode med andre ytelser
                            aktivtSkjæringspunkt.erYtelseperiode -> {
                                resultater.add(aktivtSkjæringspunkt)
                                Søkekontekst(dagen.dato)
                            }
                            else -> aktivtSkjæringspunkt.utvid(dagen.dato)
                        }
                    }
                }

                is Dag.Permisjonsdag,
                is Dag.ProblemDag -> aktivtSkjæringspunkt = aktivtSkjæringspunkt?.utvid(dagen.dato)

                is Dag.UkjentDag -> when (dagen.dato.erHelg()) {
                    true -> aktivtSkjæringspunkt = aktivtSkjæringspunkt?.utvid(dagen.dato)
                    false -> {
                        aktivtSkjæringspunkt?.also { resultater.add(it) }
                        aktivtSkjæringspunkt = null
                    }
                }
            }
        }
     */

    private data class Søkekontekst(
        val sisteDag: Dag,
        val skjæringstidspunkt: LocalDate = sisteDag.dato,
    ) {
        val periode = skjæringstidspunkt til sisteDag.dato

        fun utvid(dag: Dag): Søkekontekst {
            return copy(sisteDag = dag)
        }

        fun sisteDagErDelAvSykeforløp() = when (sisteDag) {
            is Dag.ArbeidsgiverHelgedag,
            is Dag.Arbeidsgiverdag,
            is Dag.MeldingTilNavDag,
            is Dag.MeldingTilNavHelgedag,
            is Dag.ForeldetSykedag,
            is Dag.SykHelgedag,
            is Dag.Sykedag -> true

            is Dag.AndreYtelser,
            is Dag.ArbeidIkkeGjenopptattDag,
            is Dag.Arbeidsdag,
            is Dag.Feriedag,
            is Dag.FriskHelgedag,
            is Dag.Permisjonsdag,
            is Dag.ProblemDag -> false

            is Dag.UkjentDag -> sisteDag.dato.erHelg()
        }
    }
}
