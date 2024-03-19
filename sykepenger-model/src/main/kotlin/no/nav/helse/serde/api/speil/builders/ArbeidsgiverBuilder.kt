package no.nav.helse.serde.api.speil.builders

import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.serde.api.SpekematDTO
import no.nav.helse.serde.api.SpekematDTO.PølsepakkeDTO.PølseradDTO
import no.nav.helse.serde.api.SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO
import no.nav.helse.serde.api.dto.AlderDTO
import no.nav.helse.serde.api.dto.ArbeidsgiverDTO

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
        fun List<PølseradDTO>.fjernUnødvendigeRader(): List<PølseradDTO> {
            // rader hvor alle pølser er kopiert fra forrige rad - eller blir kopiert til neste rad - kan i praksis fjernes
            // det betyr at vi bare behøver vurdere pølsepakker med minst tre rader siden den vi skal vurdere én rad mot to andre
            if (size <= 2) return this
            val pølseFinnesIRad = { rad: PølseradDTO, pølse: PølseDTO ->
                rad.pølser.any { it.behandlingId == pølse.behandlingId }
            }

            val iterator = this.asReversed().iterator()
            var forrige = iterator.next()
            // første rad bevares uansett
            val result = mutableListOf(forrige)
            var nåværende = iterator.next()
            var radnummer = 2
            while (iterator.hasNext()) {
                val neste = iterator.next()

                if (nåværende.pølser.all { pølse ->
                        pølseFinnesIRad(forrige, pølse) || pølseFinnesIRad(neste, pølse)
                    }) {
                    // raden er unyttig
                    println("unyttige rad $radnummer fra bunnen!! $nåværende er unyttig")
                } else {
                    result.add(0, nåværende)
                }

                radnummer += 1
                forrige = nåværende
                nåværende = neste
            }

            // siste/øverste rad bevares uansett
            result.add(0, nåværende)
            return result
        }
    }
}
