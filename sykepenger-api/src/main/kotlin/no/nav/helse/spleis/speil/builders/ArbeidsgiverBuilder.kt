package no.nav.helse.spleis.speil.builders

import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.spleis.speil.SpekematDTO
import no.nav.helse.spleis.speil.builders.ArbeidsgiverBuilder.Companion.fjernUnødvendigeRader
import no.nav.helse.spleis.speil.dto.AlderDTO
import no.nav.helse.spleis.speil.dto.ArbeidsgiverDTO

internal class ArbeidsgiverBuilder(
    private val arbeidsgiverUtDto: ArbeidsgiverUtDto,
    private val pølsepakke: SpekematDTO.PølsepakkeDTO?
) {

    internal fun build(alder: AlderDTO, vilkårsgrunnlagHistorikk: IVilkårsgrunnlagHistorikk): ArbeidsgiverDTO {
        return ArbeidsgiverDTO(
            id = arbeidsgiverUtDto.id,
            organisasjonsnummer = arbeidsgiverUtDto.organisasjonsnummer,
            generasjoner = pølsepakke?.let { SpeilGenerasjonerBuilder(arbeidsgiverUtDto.organisasjonsnummer, alder, arbeidsgiverUtDto, vilkårsgrunnlagHistorikk, pølsepakke).build() } ?: emptyList()
        )
    }

    internal companion object {
        fun List<SpekematDTO.PølsepakkeDTO.PølseradDTO>.fjernUnødvendigeRader(): List<SpekematDTO.PølsepakkeDTO.PølseradDTO> = if (size <= 2) this
        else foldIndexed(emptyList()) { index, acc, rad ->
            when {
                pølsaSkalMed(index) -> acc + rad
                else -> acc
            }
        }

        private fun List<SpekematDTO.PølsepakkeDTO.PølseradDTO>.pølsaSkalMed(pølseIndex: Int): Boolean {
            if (pølseIndex == 0) return true
            if (pølseIndex == size - 1) return true
            val forrige = this[pølseIndex - 1].pølser
            val denne = this[pølseIndex].pølser
            val neste = this[pølseIndex + 1].pølser
            val behandlinger = (forrige + neste).map { it.behandlingId }
            return denne.any { pølse -> !behandlinger.contains(pølse.behandlingId) }
        }
    }
}
