package no.nav.helse.hendelser

import no.nav.helse.person.AvslagsdagerFraLøsning
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.LøpendeVilkår
import no.nav.helse.person.UtfallAvLøpendeVilkår
import no.nav.helse.person.VurderingAvLøpendeVilkår
import no.nav.helse.person.VurderingerAvLøpendeVilkår
import java.time.LocalDate

/**
 * Løsningen på behovet `Avslagsdager` fra sp-avslagsdager for beregningsperioden: utfallet av den gjeldende vurderingen
 * for hver dag og hvert løpende vilkår (ADR 0016).
 */
data class Avslagsdager(
    val vurderteDager: List<VurdertDag>,
) {
    data class VurdertDag(
        val dato: LocalDate,
        val vurdering: VurderingAvLøpendeVilkår,
    )

    /**
     * Lager avslagstidslinjen og vurderingene per dag for [periode] fra løsningen. Løsningen svarer for hele
     * [beregningsperioden], så avslag og vurderinger fra sp-avslagsdager i beregningsperioden erstattes av dagene i
     * løsningen. Avslag og vurderinger utenfor beregningsperioden, og avslag med andre kilder, står urørt.
     * Dagene som ikke er oppfylt, blir avslag. Kaster hvis en slik dag allerede er avslått med en annen kilde, siden en
     * dag bare kan ha én avslagsårsak.
     */
    internal fun forPeriode(
        beregningsperiode: Periode,
        periode: Periode,
        eksisterende: Avslagstidslinje,
        eksisterendeVurderinger: VurderingerAvLøpendeVilkår,
    ): AvslagsdagerFraLøsning {
        val besvart = periode.overlappendePeriode(beregningsperiode)
        val vurderingerIPerioden =
            vurderteDager
                .filter { besvart != null && it.dato in besvart }
                .groupBy({ it.dato }, { it.vurdering })
        val nyeAvslag =
            vurderingerIPerioden
                .mapValues { (_, vurderinger) ->
                    vurderinger.filter { it.utfall == UtfallAvLøpendeVilkår.IkkeOppfylt }.map { it.vilkår.begrunnelse }.distinct()
                }.filterValues { it.isNotEmpty() }

        val konflikter = nyeAvslag.keys.filter { dato -> eksisterende[dato]?.let { it.kilde != KILDE } ?: false }.sorted()
        check(konflikter.isEmpty()) {
            "Kan ikke avslå dager for løpende vilkår som allerede er avslått med en annen kilde: $konflikter"
        }

        val utenTidligereAvslagFraSpAvslagsdager =
            eksisterende
                .filterNot { it.dato in beregningsperiode && it.verdi?.kilde == KILDE }
                .mapNotNull { dag -> dag.verdi?.let { dag.dato.somPeriode() to it } }
        val avslagFraLøsningen =
            nyeAvslag.map { (dato, begrunnelser) -> dato.somPeriode() to Avslagstidslinje.Avslagsdag(begrunnelser, KILDE) }
        val vurderingerFraLøsningen =
            vurderingerIPerioden.map { (dato, vurderinger) -> dato.somPeriode() to vurderinger.sortedBy { it.vilkår } }

        return AvslagsdagerFraLøsning(
            avslagstidslinje = Avslagstidslinje(*(utenTidligereAvslagFraSpAvslagsdager + avslagFraLøsningen).toTypedArray()),
            vurderinger = (eksisterendeVurderinger - beregningsperiode) + VurderingerAvLøpendeVilkår(*vurderingerFraLøsningen.toTypedArray()),
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

        fun løpendeVilkår(vilkår: String): LøpendeVilkår =
            when (vilkår) {
                "OPPHOLDSKRAV" -> LøpendeVilkår.Oppholdskrav
                else -> throw IllegalArgumentException("Ukjent løpende vilkår: $vilkår")
            }

        fun utfall(utfall: String): UtfallAvLøpendeVilkår =
            when (utfall) {
                "Oppfylt" -> UtfallAvLøpendeVilkår.Oppfylt
                "IkkeOppfylt" -> UtfallAvLøpendeVilkår.IkkeOppfylt
                else -> throw IllegalArgumentException("Ukjent utfall for løpende vilkår: $utfall")
            }

        fun validerVilkårskoder(
            vilkår: LøpendeVilkår,
            vilkårskoder: List<String>,
        ) {
            require(vilkårskoder.isNotEmpty()) { "Forventer minst én vilkårskode for en vurdert dag" }
            val kjente =
                when (vilkår) {
                    LøpendeVilkår.Oppholdskrav -> vilkårskoderForOppholdskrav
                }
            val ukjente = vilkårskoder.filterNot { it in kjente }
            require(ukjente.isEmpty()) { "Ukjente vilkårskoder for $vilkår: $ukjente" }
        }
    }
}
