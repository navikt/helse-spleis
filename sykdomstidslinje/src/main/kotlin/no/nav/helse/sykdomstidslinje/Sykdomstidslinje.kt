package no.nav.helse.sykdomstidslinje

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

const val ARBEIDSGIVER_PERIODE: Int = 16

abstract class Sykdomstidslinje {
    abstract fun startdato(): LocalDate
    abstract fun sluttdato(): LocalDate
    abstract fun antallSykedager(): Int
    abstract fun flatten(): List<Dag>
    abstract fun length(): Int

    internal abstract fun dag(dato: LocalDate): Dag
    internal abstract fun rapportertDato(): LocalDateTime

    operator fun plus(other: Sykdomstidslinje): Sykdomstidslinje {
        if (this.startdato().isAfter(other.startdato())) return other + this

        val datesUntil = førsteStartdato(other).datesUntil(sisteSluttdato(other).plusDays(1)).toList()
        val intervalEtterKonflikter =
            datesUntil.map { this.beste(other, it) }.toList()

        return CompositeSykdomstidslinje(intervalEtterKonflikter.map { it.tilDag() })
    }

    fun antallDagerMellom(other: Sykdomstidslinje) =
        when {
            inneholder(other) -> -min(this.length(), other.length())
            harOverlapp(other) -> max(this.avstandMedOverlapp(other), other.avstandMedOverlapp(this))
            else -> min(this.avstand(other), other.avstand(this))
        }

    private fun beste(other: Sykdomstidslinje, dato: LocalDate) = listOf(this.dag(dato), other.dag(dato)).max()!!

    private fun førsteStartdato(other: Sykdomstidslinje) =
        if (this.startdato().isBefore(other.startdato())) this.startdato() else other.startdato()

    private fun sisteSluttdato(other: Sykdomstidslinje) =
        if (this.sluttdato().isAfter(other.sluttdato())) this.sluttdato() else other.sluttdato()

    private fun avstand(other: Sykdomstidslinje) =
        this.sluttdato().until(other.startdato(), ChronoUnit.DAYS).absoluteValue.toInt() - 1

    private fun avstandMedOverlapp(other: Sykdomstidslinje) =
        -(this.sluttdato().until(other.startdato(), ChronoUnit.DAYS).absoluteValue.toInt() + 1)

    private fun erDelAv(other: Sykdomstidslinje) =
        this.harBeggeGrenseneInnenfor(other) || other.harBeggeGrenseneInnenfor(this)

    private fun inneholder(other: Sykdomstidslinje) =
        this.harBeggeGrenseneInnenfor(other) || other.harBeggeGrenseneInnenfor(this)

    private fun harBeggeGrenseneInnenfor(other: Sykdomstidslinje) =
        this.startdato() in other.startdato()..other.sluttdato() && this.sluttdato() in other.startdato()..other.sluttdato()

    private fun harOverlapp(other: Sykdomstidslinje) = this.harGrenseInnenfor(other) || other.harGrenseInnenfor(this)

    private fun harGrenseInnenfor(other: Sykdomstidslinje) =
        this.startdato() in (other.startdato()..other.sluttdato())

    fun grupperPerioderMedMaksAvstand(): List<Sykdomstidslinje> {
        val dager = flatten()

        val resultat = mutableListOf<Sykdomstidslinje>()
        var periode = mutableListOf<Dag>()
        var ikkeSykDagTeller = 0
        for (dag in dager) {
            if (dag.antallSykedager() == 1) {
                ikkeSykDagTeller = 0
                periode.add(dag)
            } else {
                if (ikkeSykDagTeller == 0 && dag is Feriedag) {
                    continue
                }
                ikkeSykDagTeller++
                if (ikkeSykDagTeller == 16) {
                    resultat.add(CompositeSykdomstidslinje(periode))
                    periode = mutableListOf()
                }
            }
        }

        if (periode.size != 0) {
            resultat.add(CompositeSykdomstidslinje(periode))
        }
        return resultat
    }

    fun antallSykedagerArbeidsperiode(): Int {
        val stateMachine = ArbeidsdagStatemaskin()
        for (dag in flatten()) {
            when (dag) {
                is Arbeidsdag -> stateMachine.state.visitArbeidsdag(dag)
                is Sykedag -> stateMachine.state.visitSykdag(dag)
                is Helgedag -> stateMachine.state.visitHelgdag(dag)
                is SykHelgedag -> stateMachine.state.visitSykHelgedag(dag)
            }
        }
        return stateMachine.sykedager
    }

    companion object {
        fun sykedager(gjelder: LocalDate, rapportert: LocalDateTime) =
            if (erArbeidsdag(gjelder)) Sykedag(
                gjelder,
                rapportert
            ) else SykHelgedag(
                gjelder,
                rapportert
            )

        fun ferie(gjelder: LocalDate, rapportert: LocalDateTime) =
            if (erArbeidsdag(gjelder)) Feriedag(
                gjelder,
                rapportert
            ) else Helgedag(
                gjelder,
                rapportert
            )

        fun ikkeSykedag(gjelder: LocalDate, rapportert: LocalDateTime) =
            if (erArbeidsdag(gjelder)) Arbeidsdag(
                gjelder,
                rapportert
            ) else Helgedag(
                gjelder,
                rapportert
            )

        fun sykedager(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                sykedager(
                    it,
                    rapportert
                )
            }.toList())
        }

        fun ferie(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                ferie(
                    it,
                    rapportert
                )
            }.toList())
        }

        fun ikkeSykedager(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                ikkeSykedag(
                    it,
                    rapportert
                )
            }.toList())
        }

        private fun erArbeidsdag(dato: LocalDate) =
            dato.dayOfWeek != DayOfWeek.SATURDAY && dato.dayOfWeek != DayOfWeek.SUNDAY

    }

    internal interface SykedagerTellerTilstand {
        fun visitArbeidsdag(dag: Arbeidsdag)
        fun visitSykdag(dag: Sykedag)
        fun visitHelgdag(dag: Helgedag)
        fun visitSykHelgedag(dag: SykHelgedag)
    }

    internal inner class ArbeidsdagStatemaskin {
        var state: SykedagerTellerTilstand = Starttilstand()
        var sykedager = 0

        internal inner class Starttilstand : SykedagerTellerTilstand {
            override fun visitArbeidsdag(dag: Arbeidsdag) {

            }

            override fun visitSykdag(dag: Sykedag) {
                state = TellerSykedager()
                state.visitSykdag(dag)
            }

            override fun visitHelgdag(dag: Helgedag) {
                state = TellerHelg()
                state.visitHelgdag(dag)
            }

            override fun visitSykHelgedag(dag: SykHelgedag) {
                state = TellerSykedager()
                state.visitSykHelgedag(dag)
            }
        }

        internal inner class TellerSykedager : SykedagerTellerTilstand {
            override fun visitArbeidsdag(dag: Arbeidsdag) {
                state = TellerIkkeSykedager()
                state.visitArbeidsdag(dag)
            }

            override fun visitSykdag(dag: Sykedag) {
                sykedager ++
            }

            override fun visitHelgdag(dag: Helgedag) {
                state = TellerHelg()
                state.visitHelgdag(dag)
            }

            override fun visitSykHelgedag(dag: SykHelgedag) {
                sykedager ++
            }
        }
        internal inner class TellerIkkeSykedager(var ikkeSykedager: Int = 0) : SykedagerTellerTilstand {
            override fun visitArbeidsdag(dag: Arbeidsdag) {
                ikkeSykedager ++
            }

            override fun visitSykdag(dag: Sykedag) {
                state = TellerSykedager()
                state.visitSykdag(dag)
            }

            override fun visitHelgdag(dag: Helgedag) {
                state = TellerHelg()
                state.visitHelgdag(dag)
            }

            override fun visitSykHelgedag(dag: SykHelgedag) {
                state = TellerSykedager()
                state.visitSykHelgedag(dag)
            }
        }
        internal inner class TellerFerie: SykedagerTellerTilstand {
            override fun visitArbeidsdag(dag: Arbeidsdag) {

            }

            override fun visitSykdag(dag: Sykedag) {

            }

            override fun visitHelgdag(dag: Helgedag) {
                state = TellerHelg()
                state.visitHelgdag(dag)
            }

            override fun visitSykHelgedag(dag: SykHelgedag) {

            }
        }
        internal inner class TellerHelg : SykedagerTellerTilstand {
            override fun visitArbeidsdag(dag: Arbeidsdag) {
                state = TellerIkkeSykedager(2)
                state.visitArbeidsdag(dag)
            }

            override fun visitSykdag(dag: Sykedag) {
                sykedager += 2
                state = TellerSykedager()
                state.visitSykdag(dag)
            }

            override fun visitHelgdag(dag: Helgedag) {

            }

            override fun visitSykHelgedag(dag: SykHelgedag) {

            }
        }
    }

}
