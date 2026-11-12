package no.nav.helse.person

import no.nav.helse.Tidslinje
import no.nav.helse.dto.LøpendeVilkårDto
import no.nav.helse.dto.UtfallAvLøpendeVilkårDto
import no.nav.helse.dto.VurderingerAvLøpendeVilkårDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import java.time.LocalDate
import java.util.UUID

enum class LøpendeVilkår(
    internal val begrunnelse: Begrunnelse,
) {
    Oppholdskrav(Begrunnelse.Oppholdskrav),
}

enum class UtfallAvLøpendeVilkår {
    Oppfylt,
    IkkeOppfylt,
}

data class VurderingAvLøpendeVilkår(
    val vilkår: LøpendeVilkår,
    val vurderingId: UUID,
    val utfall: UtfallAvLøpendeVilkår,
)

/** Den gjeldende vurderingen fra sp-avslagsdager per dag og løpende vilkår (ADR 0016). */
internal class VurderingerAvLøpendeVilkår(
    vararg perioder: Pair<Periode, List<VurderingAvLøpendeVilkår>>,
) : Tidslinje<List<VurderingAvLøpendeVilkår>, VurderingerAvLøpendeVilkår>(*perioder) {
    internal constructor(dato: LocalDate, vurderinger: List<VurderingAvLøpendeVilkår>) : this(*arrayOf(dato.somPeriode() to vurderinger))

    override fun opprett(vararg perioder: Pair<Periode, List<VurderingAvLøpendeVilkår>>) = VurderingerAvLøpendeVilkår(*perioder)

    fun dto() =
        VurderingerAvLøpendeVilkårDto(
            gruppér().map { (periode, vurderinger) ->
                VurderingerAvLøpendeVilkårDto.VurdertPeriodeDto(
                    periode = periode.dto(),
                    vurderinger =
                        vurderinger.map {
                            VurderingerAvLøpendeVilkårDto.VurderingDto(
                                vilkår =
                                    when (it.vilkår) {
                                        LøpendeVilkår.Oppholdskrav -> LøpendeVilkårDto.Oppholdskrav
                                    },
                                vurderingId = it.vurderingId,
                                utfall =
                                    when (it.utfall) {
                                        UtfallAvLøpendeVilkår.Oppfylt -> UtfallAvLøpendeVilkårDto.Oppfylt
                                        UtfallAvLøpendeVilkår.IkkeOppfylt -> UtfallAvLøpendeVilkårDto.IkkeOppfylt
                                    },
                            )
                        },
                )
            },
        )

    companion object {
        fun gjenopprett(dto: VurderingerAvLøpendeVilkårDto) =
            VurderingerAvLøpendeVilkår(
                *dto.perioder
                    .map { periode ->
                        Periode(periode.periode.fom, periode.periode.tom) to
                            periode.vurderinger.map {
                                VurderingAvLøpendeVilkår(
                                    vilkår =
                                        when (it.vilkår) {
                                            LøpendeVilkårDto.Oppholdskrav -> LøpendeVilkår.Oppholdskrav
                                        },
                                    vurderingId = it.vurderingId,
                                    utfall =
                                        when (it.utfall) {
                                            UtfallAvLøpendeVilkårDto.Oppfylt -> UtfallAvLøpendeVilkår.Oppfylt
                                            UtfallAvLøpendeVilkårDto.IkkeOppfylt -> UtfallAvLøpendeVilkår.IkkeOppfylt
                                        },
                                )
                            }
                    }.toTypedArray(),
            )
    }
}
