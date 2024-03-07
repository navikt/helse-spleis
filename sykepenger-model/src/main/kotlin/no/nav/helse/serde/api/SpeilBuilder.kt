package no.nav.helse.serde.api

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.api.dto.PersonDTO
import no.nav.helse.serde.api.speil.builders.PersonBuilder

fun serializePersonForSpeil(person: Person, pølsepakke: SpekematDTO? = null): PersonDTO {
    val jsonBuilder = SpeilBuilder(pølsepakke)
    person.accept(jsonBuilder)
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

internal class SpeilBuilder(private val pølsepakke: SpekematDTO?) : AbstractBuilder() {

    private companion object {
        /* Økes for å signalisere til spesialist at strukturen i snapshot'et
         * på et eller annet vis har endret seg, og at spesialist derfor må oppdatere cachede snapshots løpende
         */
        const val SNAPSHOT_VERSJON = 52
    }

    private lateinit var personBuilder: PersonBuilder

    fun build() = personBuilder.build()

    override fun preVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        personidentifikator: Personidentifikator,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        personBuilder = PersonBuilder(this, personidentifikator, aktørId, pølsepakke, vilkårsgrunnlagHistorikk, SNAPSHOT_VERSJON)
        pushState(personBuilder)
    }
}

