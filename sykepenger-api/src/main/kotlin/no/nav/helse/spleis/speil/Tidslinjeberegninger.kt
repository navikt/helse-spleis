package no.nav.helse.spleis.speil

import no.nav.helse.dto.LøpendeVilkårDto
import no.nav.helse.dto.UtfallAvLøpendeVilkårDto
import no.nav.helse.dto.VurderingerAvLøpendeVilkårDto
import no.nav.helse.spleis.speil.dto.*
import java.time.LocalDate

internal fun List<Sykdomstidslinjedag>.merge(
    utbetalingstidslinje: List<Utbetalingstidslinjedag>,
    vurderingerAvLøpendeVilkår: VurderingerAvLøpendeVilkårDto,
): List<SammenslåttDag> {
    fun begrunnelser(utbetalingsdag: Utbetalingstidslinjedag) = if (utbetalingsdag is AvvistDag) utbetalingsdag.begrunnelser else null

    val vurderingerPerDag = vurderingerAvLøpendeVilkår.perDag()

    return map { sykdomsdag ->
        val utbetalingsdag = utbetalingstidslinje.find { it.dato.isEqual(sykdomsdag.dagen) }
        SammenslåttDag(
            sykdomsdag.dagen,
            sykdomsdag.type,
            utbetalingsdag?.type ?: UtbetalingstidslinjedagType.UkjentDag,
            kilde = sykdomsdag.kilde,
            grad = sykdomsdag.grad,
            utbetalingsinfo = utbetalingsdag?.utbetalingsinfo(),
            begrunnelser = utbetalingsdag?.let { begrunnelser(it) },
            vurderingerAvLøpendeVilkår = vurderingerPerDag[sykdomsdag.dagen] ?: emptyList(),
        )
    }
}

private fun VurderingerAvLøpendeVilkårDto.perDag(): Map<LocalDate, List<VurderingAvLøpendeVilkårDTO>> =
    perioder
        .flatMap { periode ->
            val vurderinger = periode.vurderinger.map { it.tilSpeil() }
            periode.periode.fom
                .datesUntil(periode.periode.tom.plusDays(1))
                .toList()
                .map { it to vurderinger }
        }.toMap()

private fun VurderingerAvLøpendeVilkårDto.VurderingDto.tilSpeil() =
    VurderingAvLøpendeVilkårDTO(
        vilkår =
            when (vilkår) {
                LøpendeVilkårDto.Oppholdskrav -> LøpendeVilkårDTO.Oppholdskrav
            },
        vurderingId = vurderingId,
        utfall =
            when (utfall) {
                UtfallAvLøpendeVilkårDto.Oppfylt -> UtfallAvLøpendeVilkårDTO.Oppfylt
                UtfallAvLøpendeVilkårDto.IkkeOppfylt -> UtfallAvLøpendeVilkårDTO.IkkeOppfylt
            },
    )
