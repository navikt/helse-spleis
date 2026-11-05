package no.nav.helse.hendelser

import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.AvslagsdagerFraLøsning
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import java.time.LocalDate
import java.util.UUID

/**
 * Løsningen på behovet `Avslagsdager` fra sp-avslagsdager for beregningsperioden: vurderingene som gjelder for dager i
 * perioden, også oppfylte, og dagene som er avslått.
 */
data class Avslagsdager(
    val vurderingIder: Set<UUID>,
    val dager: List<Dag>,
) {
    data class Dag(
        val vurderingId: UUID,
        val dato: LocalDate,
        val begrunnelse: Begrunnelse,
    )

    /**
     * Lager avslagstidslinjen for [periode] fra løsningen. Løsningen svarer for hele [beregningsperioden], så avslag fra
     * sp-avslagsdager i beregningsperioden erstattes av dagene i løsningen. Avslag utenfor beregningsperioden og avslag
     * med andre kilder står urørt.
     * Kaster hvis en dag fra løsningen allerede er avslått med en annen kilde, siden en dag bare kan ha én avslagsårsak.
     */
    internal fun forPeriode(
        beregningsperiode: Periode,
        periode: Periode,
        eksisterende: Avslagstidslinje,
    ): AvslagsdagerFraLøsning {
        val besvart = periode.overlappendePeriode(beregningsperiode)
        val nyeAvslag =
            dager
                .filter { besvart != null && it.dato in besvart }
                .groupBy({ it.dato }, { it.begrunnelse })

        val konflikter = nyeAvslag.keys.filter { dato -> eksisterende[dato]?.let { it.kilde != KILDE } ?: false }.sorted()
        check(konflikter.isEmpty()) {
            "Kan ikke avslå dager for løpende vilkår som allerede er avslått med en annen kilde: $konflikter"
        }

        val utenTidligereAvslagFraSpAvslagsdager =
            eksisterende
                .filterNot { it.dato in beregningsperiode && it.verdi?.kilde == KILDE }
                .mapNotNull { dag -> dag.verdi?.let { dag.dato.somPeriode() to it } }
        val avslagFraLøsningen =
            nyeAvslag.map { (dato, begrunnelser) ->
                dato.somPeriode() to Avslagstidslinje.Avslagsdag(begrunnelser.distinct(), KILDE)
            }
        return AvslagsdagerFraLøsning(
            avslagstidslinje = Avslagstidslinje(*(utenTidligereAvslagFraSpAvslagsdager + avslagFraLøsningen).toTypedArray()),
            vurderingIder = vurderingIder,
        )
    }

    companion object {
        internal const val KILDE = "sp-avslagsdager"

        private val vilkårskoderForOppholdskrav =
            setOf(
                "OPPHOLD_I_NORGE_EØS_ELLER_AVTALELAND",
                "OPPHOLD_I_NORGE_IKKE_EØS_BORGER",
                "UNNTAK_MEDLEM_ETTER_2_5_2_6_2_8",
                "UNNTAK_INNLAGT_HELSEINSTITUSJON",
                "UNNTAK_GODKJENT_OPPHOLD_INNTIL_FIRE_UKER",
            )

        fun begrunnelse(vilkårskoder: List<String>): Begrunnelse {
            require(vilkårskoder.isNotEmpty()) { "Forventer minst én vilkårskode for en avslagsdag" }
            val ukjente = vilkårskoder.filterNot { it in vilkårskoderForOppholdskrav }
            require(ukjente.isEmpty()) { "Ukjente vilkårskoder for avslagsdag: $ukjente" }
            return Begrunnelse.Oppholdskrav
        }
    }
}
