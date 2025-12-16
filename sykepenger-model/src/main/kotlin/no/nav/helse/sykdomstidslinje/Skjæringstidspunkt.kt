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

        personsykdomstidslinje.forEach { dagen ->
            when (dagen) {
                is Dag.AndreYtelser,
                is Dag.ArbeidIkkeGjenopptattDag,
                is Dag.Arbeidsdag,
                is Dag.FriskHelgedag -> aktivtSkjæringspunkt = aktivtSkjæringspunkt?.utvidEtterFerdigSykdom(dagen.dato)

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
                            aktivtSkjæringspunkt.sykdomErFerdig -> {
                                resultater.add(aktivtSkjæringspunkt)
                                Søkekontekst(dagen.dato)
                            }

                            else -> aktivtSkjæringspunkt.utvidLøpendeSykdom(dagen.dato)
                        }
                    }
                }

                is Dag.Permisjonsdag,
                is Dag.Feriedag,
                is Dag.ProblemDag -> aktivtSkjæringspunkt = aktivtSkjæringspunkt?.utvidLøpendeSykdom(dagen.dato)

                is Dag.UkjentDag -> when (dagen.dato.erHelg()) {
                    true -> aktivtSkjæringspunkt = aktivtSkjæringspunkt?.utvidLøpendeSykdom(dagen.dato)
                    false -> {
                        aktivtSkjæringspunkt?.also { resultater.add(it) }
                        aktivtSkjæringspunkt = null
                    }
                }
            }
        }

        return resultater + listOfNotNull(aktivtSkjæringspunkt)
    }

    private data class Søkekontekst(
        val skjæringstidspunkt: LocalDate,
        val tom: LocalDate = skjæringstidspunkt,
        val sykdomErFerdig: Boolean = false
    ) {
        val periode = skjæringstidspunkt til tom

        fun utvidLøpendeSykdom(dato: LocalDate): Søkekontekst {
            return copy(tom = dato)
        }

        fun utvidEtterFerdigSykdom(dato: LocalDate): Søkekontekst {
            return copy(
                sykdomErFerdig = true,
                tom = dato
            )
        }
    }

}
