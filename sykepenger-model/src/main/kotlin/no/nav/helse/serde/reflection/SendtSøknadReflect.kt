package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelSendtSøknad
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SendtSøknadReflect(sendtSøknad: ModelSendtSøknad) {
    private val hendelseId: UUID = sendtSøknad.hendelseId()
    private val fnr: String = sendtSøknad.getProp("fnr")
    private val aktørId: String = sendtSøknad.getProp("aktørId")
    private val orgnummer: String = sendtSøknad.getProp("orgnummer")
    private val rapportertdato: LocalDateTime = sendtSøknad.getProp("rapportertdato")
    private val perioder: List<ModelSendtSøknad.Periode> = sendtSøknad.getProp("perioder")
    private val originalJson: String = sendtSøknad.getProp("originalJson")

    internal fun toMap() = mutableMapOf<String, Any?>(
        "hendelseId" to hendelseId,
        "fnr" to fnr,
        "aktørId" to aktørId,
        "orgnummer" to orgnummer,
        "rapportertdato" to rapportertdato,
        "perioder" to perioder.map { PeriodeReflect(it).toMap() },
        "originalJson" to originalJson
    )

    private class PeriodeReflect(private val periode: ModelSendtSøknad.Periode) {
        private val fom: LocalDate = periode.fom
        private val tom: LocalDate = periode.tom

        internal fun toMap() = mutableMapOf<String, Any?>(
            "fom" to fom,
            "tom" to tom
        ).also {
            when (periode) {
                is ModelSendtSøknad.Periode.Sykdom -> {
                    it["grad"] = periode.getProp<ModelSendtSøknad.Periode.Sykdom, Int>("grad")
                    it["faktiskGrad"] = periode.getProp<ModelSendtSøknad.Periode.Sykdom, Double>("faktiskGrad")
                }
                is ModelSendtSøknad.Periode.Utdanning -> {
                    it["utdanningFom"] = periode.getProp<ModelSendtSøknad.Periode.Utdanning, LocalDate?>("utdanningFom")
                }
            }
        }
    }
}
