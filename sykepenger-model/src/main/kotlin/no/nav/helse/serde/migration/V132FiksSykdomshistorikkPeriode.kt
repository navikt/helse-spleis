package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate

internal class V132FiksSykdomshistorikkPeriode : JsonMigration(version = 132) {
    override val description = "Lapper en bestemt sykdomshistorikk sin periode"

    private val forventetDato = LocalDate.of(2021, 9, 20)
    private val riktigDato = LocalDate.of(2021, 8, 30)
    private val elementId = setOf(
        "cd2bbc99-8869-4b94-bb8b-68e7c34e9564",
        "3c78fc22-bfd9-4cc5-ad96-b22eff5d6cea",
        "a6ee0235-e279-4c4c-b24a-ec99687164bf",
        "5101d1c3-de21-4020-9f85-a8d0d372daa8",
        "dd7f3306-6182-419c-a560-a083f34d1a61",
        "c0031716-3608-4251-a479-474528253b53",
    )

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver["sykdomshistorikk"].forEach { innslag ->
                if (innslag.path("id").asText() in elementId)
                    migrer(innslag["beregnetSykdomstidslinje"])
            }
        }

    }

    private fun migrer(sykdomstidslinje: JsonNode) {
        val periode = sykdomstidslinje.path("periode") as ObjectNode
        check(periode.path("fom").asText() == "$forventetDato")
        periode.remove("fom")
        periode.put("fom", "$riktigDato")
    }
}
