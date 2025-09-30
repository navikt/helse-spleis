package no.nav.helse.etterlevelse

import java.time.LocalDate
import no.nav.helse.hendelser.Hendelseskilde.Companion.INGEN
import no.nav.helse.januar
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Foreldrepenger
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EtterlevelseSykdomstidslinjeTest {

    @Test
    fun `Vurderer alle dagtyper hvorvidt de er aktuelle i tidslinjen for subsumméring`() {
        val dager = listOf(
            1.januar.subsummérbar { Dag.AndreYtelser(it, INGEN, Foreldrepenger) },
            2.januar.ikkeSubsummérbar { Dag.Arbeidsdag(it, INGEN) },
            3.januar.ikkeSubsummérbar { Dag.ArbeidsgiverHelgedag(it, 100.prosent, INGEN) },
            4.januar.ikkeSubsummérbar { Dag.Arbeidsgiverdag(it, 100.prosent, INGEN) },
            5.januar.ikkeSubsummérbar { Dag.ArbeidIkkeGjenopptattDag(it, INGEN) },
            6.januar.subsummérbar { Dag.Feriedag(it, INGEN) },
            7.januar.ikkeSubsummérbar { Dag.ForeldetSykedag(it, 100.prosent, INGEN) },
            8.januar.ikkeSubsummérbar { Dag.FriskHelgedag(it, INGEN) },
            9.januar.subsummérbar { Dag.Permisjonsdag(it, INGEN) },
            10.januar.ikkeSubsummérbar { Dag.ProblemDag(it, INGEN, "Melding") },
            11.januar.subsummérbar { Dag.SykHelgedag(it, 100.prosent, INGEN) },
            12.januar.subsummérbar { Dag.Sykedag(it, 100.prosent, INGEN) },
            13.januar.ikkeSubsummérbar { Dag.UkjentDag(it, INGEN) },
            14.januar.ikkeSubsummérbar { Dag.MeldingTilNavDag(it, 100.prosent, INGEN) },
            14.januar.ikkeSubsummérbar { Dag.MeldingTilNavHelgedag(it, 100.prosent, INGEN) }
        ).sortedBy { it.second::class.simpleName }

        // Sjekker at alle dager er hensyntatt i testen
        assertEquals(Dag::class.sealedSubclasses, dager.map { (_, dag) -> dag::class })

        val (subsummérbareDager, ikkeSubsummérbareDager) = dager.partition { (_, _, skalSubsumméres) -> skalSubsumméres }

        // Forsikrer oss om at dager som ikke skal subsummeres ikke legges til i tidslinjen
        val feilaktigSubsummért = ikkeSubsummérbareDager.filter { (dato, dag) ->
            val sykdomstidslinje = Sykdomstidslinje(mapOf(dato to dag))
            val builder = SykdomstidslinjeBuilder(sykdomstidslinje)
            builder.dager().isNotEmpty()
        }

        assertEquals(0, feilaktigSubsummért.size) { "Feilaktig subsummérte dagtyper: ${feilaktigSubsummért.map { (_, dag) -> dag::class.simpleName }}" }

        // Forsikrer oss om at de dagene som skal subsummeres inngår i tidslinjen
        val manglerSubsummering = subsummérbareDager.filter { (dato, dag) ->
            val sykdomstidslinje = Sykdomstidslinje(mapOf(dato to dag))
            val builder = SykdomstidslinjeBuilder(sykdomstidslinje)
            builder.dager().isEmpty()
        }

        assertEquals(0, manglerSubsummering.size) { "Dagtyper som mangler subsummering: ${manglerSubsummering.map { (_, dag) -> dag::class.simpleName }}" }
    }

    private companion object {
        private fun LocalDate.subsummérbar(dag: (dato: LocalDate) -> Dag) = Triple(this, dag(this), true)
        private fun LocalDate.ikkeSubsummérbar(dag: (dato: LocalDate) -> Dag) = Triple(this, dag(this), false)
    }
}

