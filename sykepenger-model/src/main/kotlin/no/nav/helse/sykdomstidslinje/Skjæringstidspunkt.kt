package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
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
        //return beregnSkjærignstidspunktV1000()
    }

    private fun finnSkjæringstidspunkt(): List<Søkekontekst> {
        if (personsykdomstidslinje.count() == 0) return emptyList()
        val resultater = mutableListOf<Søkekontekst>()
        var aktivtSkjæringspunkt: Søkekontekst? = null

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

        return resultater + listOfNotNull(aktivtSkjæringspunkt)
    }

    private data class Søkekontekst(
        val skjæringstidspunkt: LocalDate,
        val tom: LocalDate = skjæringstidspunkt,
        val erYtelseperiode: Boolean = false
    ) {
        val periode = skjæringstidspunkt til tom

        fun utvid(dato: LocalDate): Søkekontekst {
            return copy(tom = dato)
        }

        fun utvidMedAndreYtelser(dato: LocalDate): Søkekontekst {
            return copy(
                erYtelseperiode = true,
                tom = dato
            )
        }
    }

    private fun beregnSkjærignstidspunktV1000(): Skjæringstidspunkter {
        if (personsykdomstidslinje.count() == 0) return Skjæringstidspunkter(emptyList())
        var forrigeDag: ForrigeDag = ForrigeDag.Frisk
        val skjæringstidspunkter = mutableListOf<Periode>()

        personsykdomstidslinje.forEach { dagen ->
            forrigeDag = when (dagen) {
                // Syk
                is Dag.ArbeidsgiverHelgedag,
                is Dag.Arbeidsgiverdag,
                is Dag.MeldingTilNavDag,
                is Dag.MeldingTilNavHelgedag,
                is Dag.ForeldetSykedag,
                is Dag.SykHelgedag,
                is Dag.Sykedag -> forrigeDag.syk(dagen.dato, skjæringstidspunkter)

                // Ikke syk
                is Dag.AndreYtelser,
                is Dag.ArbeidIkkeGjenopptattDag,
                is Dag.Arbeidsdag,
                is Dag.FriskHelgedag,
                is Dag.ProblemDag -> forrigeDag.ikkeSyk(dagen.dato, skjæringstidspunkter)

                // Ferie
                is Dag.Permisjonsdag,
                is Dag.Feriedag -> forrigeDag.ferie(dagen.dato, skjæringstidspunkter)

                // Hull i tidslinjen
                is Dag.UkjentDag -> forrigeDag.ukjent(dagen.dato, skjæringstidspunkter)
            }
        }

        // Når det ikke er flere dager må vi avslutte siste skjæringstidspunkt på personen
        forrigeDag.flush(skjæringstidspunkter)

        return Skjæringstidspunkter(skjæringstidspunkter)
    }

    private sealed interface ForrigeDag {
        fun syk(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>): ForrigeDag
        fun ikkeSyk(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>): ForrigeDag
        fun ferie(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>): ForrigeDag
        fun ukjent(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>): ForrigeDag
        fun flush(skjæringstidspunkt: MutableList<Periode>)

        object Frisk: ForrigeDag {
            override fun syk(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = Syk(dato.somPeriode())
            override fun ikkeSyk(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = Frisk
            override fun ferie(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = Frisk
            override fun ukjent(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = Frisk
            override fun flush(skjæringstidspunkt: MutableList<Periode>) {}
        }

        data class Syk(private val periode: Periode): ForrigeDag {
            override fun syk(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = Syk(periode.oppdaterTom(dato))
            override fun ikkeSyk(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = IkkeSyk(periode.oppdaterTom(dato))
            override fun ferie(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = FerieEtterSyk(periode.oppdaterTom(dato))
            override fun ukjent(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = when (dato.erHelg()) {
                true -> Syk(periode.oppdaterTom(dato))
                false -> {
                    skjæringstidspunkt.add(periode)
                    Frisk
                }
            }
            override fun flush(skjæringstidspunkt: MutableList<Periode>) { skjæringstidspunkt.add(periode) }
        }

        // Er ikke Frisk og IkkeSyk det samme?
        // Nei, IkkeSyk er en dag vi kjenner til å sykdomstidslinjen som ikke er sykdom
        // mens Frisk er hull i sykdomstidslinjen vi ikke kjenner til
        // .. finn på noe bedre navn selv da vel 🤷‍♂️
        data class IkkeSyk(private val periode: Periode): ForrigeDag {
            override fun syk(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>): ForrigeDag {
                skjæringstidspunkt.add(periode)
                return Syk(dato.somPeriode())
            }
            override fun ikkeSyk(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = IkkeSyk(periode.oppdaterTom(dato))
            override fun ferie(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = IkkeSyk(periode.oppdaterTom(dato))
            override fun ukjent(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>): ForrigeDag {
                skjæringstidspunkt.add(periode)
                return Frisk
            }
            override fun flush(skjæringstidspunkt: MutableList<Periode>) { skjæringstidspunkt.add(periode) }
        }

        data class FerieEtterSyk(private val periode: Periode): ForrigeDag {
            override fun syk(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = Syk(periode.oppdaterTom(dato))
            override fun ikkeSyk(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = IkkeSyk(periode.oppdaterTom(dato))
            override fun ferie(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = FerieEtterSyk(periode.oppdaterTom(dato))
            override fun ukjent(dato: LocalDate, skjæringstidspunkt: MutableList<Periode>) = when (dato.erHelg()) {
                true -> FerieEtterSyk(periode.oppdaterTom(dato))
                false -> {
                    skjæringstidspunkt.add(periode)
                    Frisk
                }
            }
            override fun flush(skjæringstidspunkt: MutableList<Periode>) { skjæringstidspunkt.add(periode) }
        }
    }
}
