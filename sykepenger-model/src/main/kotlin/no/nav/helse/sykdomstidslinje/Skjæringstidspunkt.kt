package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode

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

    fun beregnSkjæringstidspunkt(vedtaksperiode: Periode, sykdomsperiode: Periode?): LocalDate {
        if (personsykdomstidslinje.count() == 0) return vedtaksperiode.start

        // sykdomsperioden er den perioden som ikke er friskmeldt
        val søkeperiode = sykdomsperiode ?: vedtaksperiode

        return søkEtterSkjæringstidspunkt(søkeperiode)
    }

    private fun søkEtterSkjæringstidspunkt(søkeperiode: Periode): LocalDate {
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
                is Dag.Sykedag,
                is Dag.SykedagNav -> søkekontekst.potensieltSkjæringstidspunkt(dato)

                is Dag.Permisjonsdag -> søkekontekst
                is Dag.ProblemDag -> søkekontekst

                is Dag.UkjentDag -> when (dato.erHelg()) {
                    true -> søkekontekst.potensieltGyldigOppholdsperiode(dato)
                    false -> søkekontekst.oppholdsdag(søkeperiode, dato)
                }
            }
        }
    }

    private fun traverserTidslinjenBaklengs(søkeperiode: Periode, looper: (Søkekontekst, Dag, LocalDate) -> Søkekontekst): LocalDate {
        var gjeldendeDag = minOf(søkeperiode.endInclusive, personsykdomstidslinje.sisteDag())
        // default-situasjon: vi faller alltid tilbake til vedtaksperiodens fom som mulig skjæringstidspunkt
        var søkekontekst = Søkekontekst(Søketilstand.HarIkkeFunnetSkjæringstidspunkt, søkeperiode.start)
        while (gjeldendeDag >= personsykdomstidslinje.førsteDag() && søkekontekst.skalFortsetteÅSøke()) {
            val dagen = personsykdomstidslinje[gjeldendeDag]
            søkekontekst = looper(søkekontekst, dagen, gjeldendeDag)
            gjeldendeDag = gjeldendeDag.minusDays(1L)
        }
        return søkekontekst.skjæringstidspunkt
    }

    private data class Søkekontekst(
        val tilstand: Søketilstand,
        val skjæringstidspunkt: LocalDate
    ) {
        fun skalFortsetteÅSøke() = tilstand != Søketilstand.HarSkjæringstidspunkt
        fun avsluttSøk() = copy(tilstand = Søketilstand.HarSkjæringstidspunkt)

        // behandler oppholdsdager før søkeperioden som avslutning av søk, men
        // håndterer at oppholdsdager kan forekomme i f.eks. halen, før vi har
        // funnet et potensielt skjæringstidspunkt
        fun oppholdsdag(søkeperiode: Periode, dagen: LocalDate): Søkekontekst = when {
            dagen <= søkeperiode.start -> avsluttSøk()
            else -> tilstand.oppholdsdag(this)
        }

        fun andreYtelser(dagen: LocalDate): Søkekontekst = tilstand.andreYtelser(this, dagen)
        fun potensieltSkjæringstidspunkt(dagen: LocalDate): Søkekontekst = tilstand.potensieltSkjæringstidspunkt(this, dagen)
        fun potensieltGyldigOppholdsperiode(dagen: LocalDate): Søkekontekst = tilstand.potensieltGyldigOppholdsperiode(this, dagen)
    }

    private sealed interface Søketilstand {
        fun oppholdsdag(kontekst: Søkekontekst) = kontekst.avsluttSøk()
        fun potensieltSkjæringstidspunkt(kontekst: Søkekontekst, dagen: LocalDate): Søkekontekst = kontekst.copy(tilstand = HarPotensieltSkjæringstidspunkt, skjæringstidspunkt = dagen)
        // en dag som potensielt kan bygge bro mellom to sykdomsperioder (ferie, andre ytelser)
        fun potensieltGyldigOppholdsperiode(kontekst: Søkekontekst, dagen: LocalDate) = kontekst.copy(tilstand = PotensieltGyldigOppholdsperiodeMellomSykdomsperioder)

        fun andreYtelser(kontekst: Søkekontekst, dagen: LocalDate) = kontekst.copy(tilstand = AndreYtelser)

        data object HarIkkeFunnetSkjæringstidspunkt : Søketilstand {
            // hopper over friskmelding i halen
            override fun oppholdsdag(kontekst: Søkekontekst) = kontekst

            // hopper over ukjent dager så lenge vi ikke har funnet noe som helst
            override fun potensieltGyldigOppholdsperiode(kontekst: Søkekontekst, dagen: LocalDate) = kontekst
        }
        data object HarPotensieltSkjæringstidspunkt : Søketilstand {
            override fun andreYtelser(kontekst: Søkekontekst, dagen: LocalDate) = kontekst.avsluttSøk()
        }
        data object PotensieltGyldigOppholdsperiodeMellomSykdomsperioder : Søketilstand {
            override fun andreYtelser(kontekst: Søkekontekst, dagen: LocalDate) = kontekst.avsluttSøk()
        }
        data object AndreYtelser : Søketilstand
        data object HarSkjæringstidspunkt : Søketilstand {
            override fun potensieltSkjæringstidspunkt(kontekst: Søkekontekst, dagen: LocalDate): Søkekontekst = kontekst
            override fun potensieltGyldigOppholdsperiode(kontekst: Søkekontekst, dagen: LocalDate): Søkekontekst = kontekst
            override fun andreYtelser(kontekst: Søkekontekst, dagen: LocalDate) = kontekst
        }
    }

}