package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate

internal class V301MaksdatoresultatPåBehandling: JsonMigration(version = 301) {
    override val description = "lagrer maksdatoresultat på behandling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerVedtaksperiode(periode)
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            behandling.path("endringer").forEach { endring ->
                migrerEndring(endring as ObjectNode)
            }
        }
    }

    private fun migrerEndring(endring: ObjectNode) {
        endring.withObject("maksdatoresultat").apply {
            put("vurdertTilOgMed", LocalDate.MIN.toString())
            put("bestemmelse", "IKKE_VURDERT")
            put("startdatoTreårsvindu", LocalDate.MIN.toString())
            putNull("startdatoSykepengerettighet")
            put("maksdato", LocalDate.MIN.toString())
            putArray("forbrukteDager")
            putArray("oppholdsdager")
            putArray("avslåtteDager")
            put("gjenståendeDager", 0)
            withObject("grunnlag").apply {
                putArray("dager")
            }
        }
    }
}
