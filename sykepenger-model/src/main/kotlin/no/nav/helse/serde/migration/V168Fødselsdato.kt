package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import no.nav.helse.person.AlderVisitor
import no.nav.helse.somFødselsnummer
import no.nav.helse.utbetalingstidslinje.Alder

internal class V168Fødselsdato: JsonMigration(168) {
    override val description = "Fødselsdato fra fnr/dnr"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("fødselsnummer").asText().somFødselsnummer().alder().accept(object : AlderVisitor {
            override fun visitAlder(alder: Alder, fødselsdato: LocalDate) {
                jsonNode.put("fødselsdato", fødselsdato.toString())
            }
        })
    }
}