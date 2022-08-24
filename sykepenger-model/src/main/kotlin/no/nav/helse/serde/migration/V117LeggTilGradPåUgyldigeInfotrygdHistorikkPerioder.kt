package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V117LeggTilGradPåUgyldigeInfotrygdHistorikkPerioder : JsonMigration(version = 117) {
    override val description: String = "Legg til grad på ugyldigePerioder i infotrygdhistorikken"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        jsonNode["infotrygdhistorikk"]
            .filterNot { it["ugyldigePerioder"].isEmpty }
            .forEach {
                it["ugyldigePerioder"].forEach { ugyldigPeriode ->
                    val fom = ugyldigPeriode["first"].textValue()
                    val tom = ugyldigPeriode["second"].textValue()

                    (ugyldigPeriode as ObjectNode).removeAll()
                    ugyldigPeriode.put("fom", fom)
                    ugyldigPeriode.put("tom", tom)
                    ugyldigPeriode.putNull("utbetalingsgrad")
                }
            }
    }
}
