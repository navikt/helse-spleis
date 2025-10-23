package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.omsluttendePeriode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Ventetidberegner.Ventetidtelling.Companion.MAKSIMALT_ANTALL_OPPHOLDSDAGER
import no.nav.helse.utbetalingstidslinje.Ventetidberegner.Ventetidtelling.Companion.MAKSIMALT_ANTALL_VENTETIDSDAGER
import no.nav.helse.utbetalingstidslinje.Ventetidberegner.Ventetidtilstand.OppholdEtterVentetidFerdigAvklart
import no.nav.helse.utbetalingstidslinje.Ventetidberegner.Ventetidtilstand.OppholdPåbegyntVentetid
import no.nav.helse.utbetalingstidslinje.Ventetidberegner.Ventetidtilstand.TilstrekkeligOppholdFerdigAvklart
import no.nav.helse.utbetalingstidslinje.Ventetidberegner.Ventetidtilstand.VentetidFerdigAvklart
import no.nav.helse.utbetalingstidslinje.Ventetidberegner.Ventetidtilstand.VentetidFerdigAvventerUtbetaltDag
import no.nav.helse.utbetalingstidslinje.Ventetidberegner.Ventetidtilstand.VentetidPåbegynt

internal class Ventetidberegner {

    fun result(sykdomstidslinje: Sykdomstidslinje): List<Ventetidsavklaring> {
        val ventetider = mutableListOf<Ventetidsavklaring>()
        var aktivVentetid: Ventetidtelling? = null
        sykdomstidslinje.forEach { dag ->
            aktivVentetid = when (dag) {
                is Dag.Sykedag -> sykedag(aktivVentetid, dag.dato, VentetidFerdigAvklart)
                is Dag.SykHelgedag -> sykedag(aktivVentetid, dag.dato, VentetidFerdigAvventerUtbetaltDag)

                is Dag.UkjentDag -> oppholdsdag(ventetider, aktivVentetid, dag.dato, dag.dato.erHelg())

                is Dag.Arbeidsdag,
                is Dag.FriskHelgedag -> oppholdsdag(ventetider, aktivVentetid, dag.dato, false)

                is Dag.AndreYtelser,
                is Dag.ArbeidIkkeGjenopptattDag,
                is Dag.ArbeidsgiverHelgedag,
                is Dag.Arbeidsgiverdag,
                is Dag.Feriedag,
                is Dag.ForeldetSykedag,
                is Dag.Permisjonsdag,
                is Dag.ProblemDag -> error("forventer ikke dag av type ${dag::class.simpleName} i ventetidsberegning")
            }
        }
        return ventetider.toList() + listOfNotNull(aktivVentetid?.somAvklaring())
    }

    private fun avslutt(ventetider: MutableList<Ventetidsavklaring>, ventetid: Ventetidtelling, avsluttetTilstand: Ventetidtilstand): Nothing? {
        ventetider.add(ventetid.copy(tilstand = avsluttetTilstand).somAvklaring())
        return null
    }

    private fun oppholdsdag(ventetider: MutableList<Ventetidsavklaring>, ventetid: Ventetidtelling?, dato: LocalDate, erImplsittHelg: Boolean): Ventetidtelling? {
        return when (ventetid?.tilstand) {
            OppholdEtterVentetidFerdigAvklart -> when (MAKSIMALT_ANTALL_OPPHOLDSDAGER) {
                (ventetid.oppholdsdager.size + 1) -> avslutt(ventetider, ventetid, TilstrekkeligOppholdFerdigAvklart)
                else -> ventetid.opphold(dato, ventetid.tilstand)
            }
            VentetidFerdigAvklart -> ventetid.opphold(dato, OppholdEtterVentetidFerdigAvklart)
            VentetidFerdigAvventerUtbetaltDag -> if (erImplsittHelg) ventetid.kjentDag(dato, VentetidFerdigAvventerUtbetaltDag) else avslutt(ventetider, ventetid, OppholdPåbegyntVentetid)
            VentetidPåbegynt -> if (erImplsittHelg) ventetid.utvid(dato, vurderOmVentetidenErFerdig(ventetid)) else avslutt(ventetider, ventetid,  OppholdPåbegyntVentetid)
            TilstrekkeligOppholdFerdigAvklart,
            OppholdPåbegyntVentetid -> error("kan ikke ha opphold i en ventetid i tilstanden ${ventetid.tilstand}")
            null -> null
        }
    }

    private fun sykedag(ventetid: Ventetidtelling?, dato: LocalDate, tilstandHvisAvventerUtbetaltDag: Ventetidtilstand): Ventetidtelling {
        if (ventetid == null) return Ventetidtelling.ny(dato)
        val nyTilstand = tilstandForSykedag(ventetid, tilstandHvisAvventerUtbetaltDag)
        return ventetid.utvid(dato, nyTilstand)
    }

    private fun tilstandForSykedag(ventetid: Ventetidtelling, tilstandHvisAvventerUtbetaltDag: Ventetidtilstand) =
        when (ventetid.tilstand) {
            VentetidFerdigAvventerUtbetaltDag -> tilstandHvisAvventerUtbetaltDag
            OppholdEtterVentetidFerdigAvklart -> VentetidFerdigAvklart
            VentetidPåbegynt -> vurderOmVentetidenErFerdig(ventetid)
            VentetidFerdigAvklart -> ventetid.tilstand
            TilstrekkeligOppholdFerdigAvklart,
            OppholdPåbegyntVentetid -> error("kan ikke utvide en ventetid i tilstanden ${ventetid.tilstand}")
        }

    private fun vurderOmVentetidenErFerdig(ventetid: Ventetidtelling): Ventetidtilstand {
        return when (MAKSIMALT_ANTALL_VENTETIDSDAGER) {
            (ventetid.dager.size + 1) -> VentetidFerdigAvventerUtbetaltDag
            else -> ventetid.tilstand
        }
    }

    private sealed interface Ventetidtilstand {
        data object VentetidPåbegynt : Ventetidtilstand
        data object VentetidFerdigAvventerUtbetaltDag : Ventetidtilstand
        data object VentetidFerdigAvklart : Ventetidtilstand
        data object OppholdEtterVentetidFerdigAvklart : Ventetidtilstand
        data object TilstrekkeligOppholdFerdigAvklart : Ventetidtilstand
        data object OppholdPåbegyntVentetid : Ventetidtilstand
    }

    private data class Ventetidtelling(
        val omsluttendePeriode: Periode,
        val dager: Set<LocalDate>,
        val oppholdsdager: Set<LocalDate>,
        val tilstand: Ventetidtilstand
    ) {
        val ventetid = dager.take(MAKSIMALT_ANTALL_VENTETIDSDAGER)
        val ferdigAvklart = tilstand in setOf(VentetidFerdigAvklart, OppholdEtterVentetidFerdigAvklart, TilstrekkeligOppholdFerdigAvklart)

        fun utvid(dato: LocalDate, tilstand: Ventetidtilstand) = copy(
            omsluttendePeriode = omsluttendePeriode.oppdaterTom(dato),
            dager = this.dager + dato,
            oppholdsdager = emptySet(),
            tilstand = tilstand
        )
        fun opphold(dato: LocalDate, tilstand: Ventetidtilstand) = copy(
            omsluttendePeriode = omsluttendePeriode.oppdaterTom(dato),
            oppholdsdager = this.oppholdsdager + dato,
            tilstand = tilstand
        )
        fun kjentDag(dato: LocalDate, tilstand: Ventetidtilstand) = copy(
            omsluttendePeriode = omsluttendePeriode.oppdaterTom(dato),
            tilstand = tilstand
        )

        companion object {
            const val MAKSIMALT_ANTALL_VENTETIDSDAGER = 16
            const val MAKSIMALT_ANTALL_OPPHOLDSDAGER = 16

            fun ny(dato: LocalDate) =
                Ventetidtelling(
                    omsluttendePeriode = dato.somPeriode(),
                    dager = setOf(dato),
                    oppholdsdager = emptySet(),
                    tilstand = VentetidPåbegynt
                )
        }
    }

    private fun Ventetidtelling.somAvklaring() =
        Ventetidsavklaring(
            omsluttendePeriode = omsluttendePeriode,
            periode = ventetid.omsluttendePeriode,
            ferdigAvklart = ferdigAvklart
        )

}

data class Ventetidsavklaring(
    val omsluttendePeriode: Periode,
    val periode: Periode?,
    val ferdigAvklart: Boolean
)
