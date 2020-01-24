package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelNySøknad
import java.time.LocalDateTime
import java.util.*

internal class NySøknadReflect(nySøknad: ModelNySøknad) {
    private val hendelseId: UUID = nySøknad.getProp("hendelseId")
    private val fnr: String = nySøknad.getProp("fnr")
    private val aktørId: String = nySøknad.getProp("aktørId")
    private val orgnummer: String = nySøknad.getProp("orgnummer")
    private val rapportertdato: LocalDateTime = nySøknad.getProp("rapportertdato")
    private val sykeperioder: List<ModelNySøknad.Sykeperiode> = nySøknad.getProp("sykeperioder")
    private val originalJson: String = nySøknad.getProp("originalJson")

    internal fun toMap() = mutableMapOf<String, Any?>(
        "hendelseId" to hendelseId,
        "fnr" to fnr,
        "aktørId" to aktørId,
        "orgnummer" to orgnummer,
        "rapportertdato" to rapportertdato,
        "sykeperioder" to sykeperioder,
        "originalJson" to originalJson
    )
}
