package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.Skjæringstidspunkt.Søketilstand.PotensieltGyldigOppholdsperiodeMellomSykdomsperioder

/**
 * Finner skjæringstidspunkter
 *
 * Gitt en vedtaksperiode [fom, tom] så går vi bakover fra og med 'fom' og tilbake
 * i tid frem til vi finner skjæringstidspunkt. Hvis vedtaksperioden har friskmelding i halen
 * så begynner vi leitingen dagen før friskmeldingen.
 *
 * vi tar vare på hver sykedag vi møter på og markerer dagen som potensielt skjæringstidspunkt.
 * dersom vi møter på en ukjent dag (og det er helg) eller om vi møter en feriedag, fortsetter vi
 * til vi kommer på "andre siden". Om det er sykedager på andre siden så fortsetter søket etter skjæringstidspunkt.
 *
 * Ved en oppholdsdag stoppes søket, med mindre det er andre ytelser – da fortsetter søket på samme måte som om det var en feriedag.
 */
internal class Skjæringstidspunkt(private val personsykdomstidslinje: Sykdomstidslinje) {

    fun alle(vedtaksperiode: Periode): List<LocalDate> {
        return finnSkjæringstidspunkt(vedtaksperiode)?.skjæringstidspunkter ?: emptyList()
    }

    fun sisteOrNull(vedtaksperiode: Periode): LocalDate? {
        return finnSkjæringstidspunkt(vedtaksperiode)?.skjæringstidspunkter?.firstOrNull()
    }

    private fun finnSkjæringstidspunkt(vedtaksperiode: Periode): Søkekontekst? {
        if (personsykdomstidslinje.count() == 0) return null
        // sykdomsperioden er den perioden som ikke er friskmeldt
        return søkEtterSkjæringstidspunkt(vedtaksperiode)
    }

    private fun søkEtterSkjæringstidspunkt(søkeperiode: Periode): Søkekontekst {
        return traverserTidslinjenBaklengs(søkeperiode) { søkekontekst, dagen, dato ->
            when (dagen) {
                is Dag.AndreYtelser -> søkekontekst.andreYtelser(dato)

                is Dag.Feriedag -> søkekontekst.potensieltGyldigOppholdsperiode(dato)

                is Dag.ArbeidIkkeGjenopptattDag,
                is Dag.Arbeidsdag,
                is Dag.FriskHelgedag -> søkekontekst.oppholdsdag(søkeperiode, dato)

                is Dag.ArbeidsgiverHelgedag,
                is Dag.Arbeidsgiverdag,
                is Dag.ForeldetSykedag,
                is Dag.SykHelgedag,
                is Dag.Sykedag -> søkekontekst.potensieltSkjæringstidspunkt(dato)

                is Dag.Permisjonsdag -> søkekontekst
                is Dag.ProblemDag -> søkekontekst

                is Dag.UkjentDag -> when (dato.erHelg()) {
                    true -> søkekontekst.potensieltGyldigOppholdsperiode(dato)
                    false -> søkekontekst.oppholdsdag(søkeperiode, dato)
                }
            }
        }
    }

    private fun traverserTidslinjenBaklengs(søkeperiode: Periode, looper: (Søkekontekst, Dag, LocalDate) -> Søkekontekst): Søkekontekst {
        var gjeldendeDag = minOf(søkeperiode.endInclusive, personsykdomstidslinje.sisteDag())
        var søkekontekst = Søkekontekst(søkeperiode, Søketilstand.HarIkkeFunnetSkjæringstidspunkt, emptyList())
        while (gjeldendeDag >= personsykdomstidslinje.førsteDag() && søkekontekst.skalFortsetteÅSøke()) {
            val dagen = personsykdomstidslinje[gjeldendeDag]
            søkekontekst = looper(søkekontekst, dagen, gjeldendeDag)
            gjeldendeDag = gjeldendeDag.minusDays(1L)
        }
        søkekontekst = søkekontekst.avsluttSøk()
        return søkekontekst
    }

    private data class Søkekontekst(
        val søkeperiode: Periode,
        val tilstand: Søketilstand,
        val skjæringstidspunkter: List<LocalDate>,
        val ferdig: Boolean = false
    ) {
        fun skalFortsetteÅSøke() = !ferdig
        fun avsluttSøk(): Søkekontekst {
            return nullstillSøk().copy(ferdig = true)
        }

        fun nullstillSøk() = this.copy(
            tilstand = Søketilstand.HarIkkeFunnetSkjæringstidspunkt,
            skjæringstidspunkter = when (tilstand) {
                is Søketilstand.HarPotensieltSkjæringstidspunkt -> skjæringstidspunkter.plusElement(tilstand.kandidat)
                is PotensieltGyldigOppholdsperiodeMellomSykdomsperioder -> skjæringstidspunkter.plusElement(tilstand.kandidat)

                Søketilstand.HarIkkeFunnetSkjæringstidspunkt,
                Søketilstand.HarSkjæringstidspunkt -> skjæringstidspunkter
            }
        )

        // behandler oppholdsdager før søkeperioden som avslutning av søk, men
        // håndterer at oppholdsdager kan forekomme i f.eks. halen, før vi har
        // funnet et potensielt skjæringstidspunkt
        fun oppholdsdag(søkeperiode: Periode, dagen: LocalDate): Søkekontekst = when {
            dagen <= søkeperiode.start -> avsluttSøk()
            else -> tilstand.oppholdsdag(this)
        }

        fun andreYtelser(dagen: LocalDate): Søkekontekst = when {
            dagen <= søkeperiode.start && harFunnetSkjæringstidspunkt() -> avsluttSøk()
            else -> nullstillSøk()
        }

        private fun harFunnetSkjæringstidspunkt() = when (tilstand) {
            is Søketilstand.HarPotensieltSkjæringstidspunkt,
            is PotensieltGyldigOppholdsperiodeMellomSykdomsperioder,
            Søketilstand.HarSkjæringstidspunkt -> true

            Søketilstand.HarIkkeFunnetSkjæringstidspunkt -> skjæringstidspunkter.isNotEmpty()
        }

        fun potensieltSkjæringstidspunkt(dagen: LocalDate): Søkekontekst = tilstand.potensieltSkjæringstidspunkt(this, dagen)
        fun potensieltGyldigOppholdsperiode(dagen: LocalDate): Søkekontekst = tilstand.potensieltGyldigOppholdsperiode(this, dagen)
    }

    private sealed interface Søketilstand {
        fun oppholdsdag(kontekst: Søkekontekst) = kontekst.nullstillSøk()
        fun potensieltSkjæringstidspunkt(kontekst: Søkekontekst, dagen: LocalDate): Søkekontekst = kontekst.copy(
            tilstand = HarPotensieltSkjæringstidspunkt(dagen)
        )

        // en dag som potensielt kan bygge bro mellom to sykdomsperioder (ferie, andre ytelser)
        fun potensieltGyldigOppholdsperiode(kontekst: Søkekontekst, dagen: LocalDate): Søkekontekst

        data object HarIkkeFunnetSkjæringstidspunkt : Søketilstand {
            // hopper over friskmelding i halen
            override fun oppholdsdag(kontekst: Søkekontekst) = kontekst

            // hopper over ukjent dager så lenge vi ikke har funnet noe som helst
            override fun potensieltGyldigOppholdsperiode(kontekst: Søkekontekst, dagen: LocalDate) = kontekst
        }

        data class HarPotensieltSkjæringstidspunkt(val kandidat: LocalDate) : Søketilstand {
            override fun potensieltGyldigOppholdsperiode(kontekst: Søkekontekst, dagen: LocalDate) = kontekst.copy(
                tilstand = PotensieltGyldigOppholdsperiodeMellomSykdomsperioder(kandidat)
            )
        }

        data class PotensieltGyldigOppholdsperiodeMellomSykdomsperioder(val kandidat: LocalDate) : Søketilstand {
            override fun potensieltGyldigOppholdsperiode(kontekst: Søkekontekst, dagen: LocalDate) = kontekst
        }

        data object HarSkjæringstidspunkt : Søketilstand {
            override fun potensieltSkjæringstidspunkt(kontekst: Søkekontekst, dagen: LocalDate): Søkekontekst = kontekst
            override fun potensieltGyldigOppholdsperiode(kontekst: Søkekontekst, dagen: LocalDate): Søkekontekst = kontekst
        }
    }
}
