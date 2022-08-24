package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.person.AktivitetsloggObserver
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.serdeObjectMapper

internal class V174AktivitetsloggDatadump : JsonMigration(version = 174) {
    override val description = """Dumper aktivitetsloggen"""

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        val aktivitetsloggData = serdeObjectMapper.convertValue<PersonData.AktivitetsloggData>(jsonNode.path("aktivitetslogg"))
        val aktivitetslogg = aktivitetsloggData.konverterTilAktivitetslogg()
        aktivitetslogg.aktiviteter.forEach { aktivitet ->
            aktivitet.notify(observer)
        }
    }
}