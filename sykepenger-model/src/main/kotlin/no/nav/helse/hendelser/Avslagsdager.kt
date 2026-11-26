package no.nav.helse.hendelser

import no.nav.helse.person.AvslagsdagerFraLøsning
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.LøpendeVilkår
import no.nav.helse.person.UtfallAvLøpendeVilkår
import no.nav.helse.person.VurderingAvLøpendeVilkår
import no.nav.helse.person.VurderingerAvLøpendeVilkår
import no.nav.helse.person.aktivitetslogg.Varselkode
import java.time.LocalDate
import java.util.UUID

/**
 * Løsningen på behovet `Avslagsdager` fra sp-avslagsdager for beregningsperioden: [avslagsdager] er dagene som er
 * avslått etter et løpende vilkår, og [vurderteDager] er utfallet av den gjeldende vurderingen for hver dag og hvert
 * løpende vilkår (ADR 0016). [grunnlagForLøpendeVilkårId] peker på grunnlaget
 * sp-avslagsdager har lagret for beregningen (ADR 0023), og [varsler] er varslene sp-avslagsdager vil ha på
 * vedtaksperiodene til hver yrkesaktivitet.
 */
data class Avslagsdager(
    val vurderteDager: List<VurdertDag>,
    val avslagsdager: List<Avslagsdag>,
    val grunnlagForLøpendeVilkårId: UUID,
    val varsler: List<Varsel>,
) {
    data class Varsel(
        val yrkesaktivitet: Behandlingsporing.Yrkesaktivitet,
        val varselkode: Varselkode,
    )

    data class Avslagsdag(
        val dato: LocalDate,
        val vilkår: LøpendeVilkår,
    )

    data class VurdertDag(
        val dato: LocalDate,
        val vurdering: VurderingAvLøpendeVilkår,
    )

    /**
     * Lager avslagstidslinjen og vurderingene per dag for [periode] fra løsningen. Løsningen svarer for hele
     * [beregningsperioden], så avslag og vurderinger fra sp-avslagsdager i beregningsperioden erstattes av dagene i
     * løsningen. Avslag og vurderinger utenfor beregningsperioden, og avslag med andre kilder, står urørt.
     * Avslagsdagene i løsningen blir avslag med begrunnelsen for vilkåret. Kaster hvis en slik dag allerede er avslått med en annen kilde, siden en
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
            avslagsdager
                .filter { besvart != null && it.dato in besvart }
                .groupBy({ it.dato }, { it.vilkår.begrunnelse })
                .mapValues { (_, begrunnelser) -> begrunnelser.distinct() }

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
            vurderinger =
                (eksisterendeVurderinger - beregningsperiode) + VurderingerAvLøpendeVilkår(*vurderingerFraLøsningen.toTypedArray()),
            grunnlagForLøpendeVilkårId = grunnlagForLøpendeVilkårId,
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

        fun yrkesaktivitet(
            yrkesaktivitetstype: String,
            organisasjonsnummer: String?,
        ): Behandlingsporing.Yrkesaktivitet =
            when (yrkesaktivitetstype) {
                "ARBEIDSTAKER" ->
                    Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
                        requireNotNull(organisasjonsnummer) {
                            "Mangler organisasjonsnummer for arbeidstaker"
                        },
                    )
                "FRILANS" -> Behandlingsporing.Yrkesaktivitet.Frilans
                "SELVSTENDIG" -> Behandlingsporing.Yrkesaktivitet.Selvstendig
                "ARBEIDSLEDIG" -> Behandlingsporing.Yrkesaktivitet.Arbeidsledig
                else -> throw IllegalArgumentException("Ukjent yrkesaktivitetstype fra sp-avslagsdager: $yrkesaktivitetstype")
            }

        fun varselkode(varselkode: String): Varselkode =
            when (varselkode) {
                "RV_SØ_8" -> Varselkode.RV_SØ_8
                else -> throw IllegalArgumentException("Ukjent varselkode fra sp-avslagsdager: $varselkode")
            }

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
