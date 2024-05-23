package no.nav.helse.spleis.speil

import java.util.UUID
import no.nav.helse.person.Person
import no.nav.helse.spleis.speil.builders.PersonBuilder
import no.nav.helse.spleis.speil.dto.PersonDTO

fun serializePersonForSpeil(person: Person, pølsepakke: SpekematDTO): PersonDTO {
    val jsonBuilder = SpeilBuilder(person, pølsepakke)
    return jsonBuilder.build()
}

data class SpekematDTO(
    val pakker: List<PølsepakkeDTO>
) {
    data class PølsepakkeDTO(
        val yrkesaktivitetidentifikator: String,
        val rader: List<PølseradDTO>
    ) {
        data class PølseradDTO(
            val pølser: List<PølseDTO>,
            val kildeTilRad: UUID
        ) {
            data class PølseDTO(
                val vedtaksperiodeId: UUID,
                val behandlingId: UUID,
                val status: PølsestatusDTO,
                // tingen som gjorde at generasjonen ble opprettet
                val kilde: UUID
            ) {
                enum class PølsestatusDTO { ÅPEN, LUKKET, FORKASTET }
            }
        }
    }
}

internal class SpeilBuilder(person: Person, private val pølsepakke: SpekematDTO) {

    private companion object {
        /* Økes for å signalisere til spesialist at strukturen i snapshot'et
         * på et eller annet vis har endret seg, og at spesialist derfor må oppdatere cachede snapshots løpende
         */
        const val SNAPSHOT_VERSJON = 53
    }

    private val personBuilder = PersonBuilder(person.dto(), pølsepakke, SNAPSHOT_VERSJON)

    fun build() = personBuilder.build()
}

