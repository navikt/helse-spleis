package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.person.ArbeidstakerHendelse.Hendelsestype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class NySøknadReflect(nySøknad: ModelNySøknad) {
    private val hendelseId: UUID = nySøknad.hendelseId()
    private val hendelsestype: Hendelsestype = nySøknad.hendelsetype()
    private val fnr: String = nySøknad["fnr"]
    private val aktørId: String = nySøknad["aktørId"]
    private val orgnummer: String = nySøknad["orgnummer"]
    private val rapportertdato: LocalDateTime = nySøknad["rapportertdato"]
    private val sykeperioder: List<Reflect> = nySøknad["Sykeperiode", "sykeperioder"]
    private val originalJson: String = nySøknad["originalJson"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "hendelseId" to hendelseId,
        "hendelsetype" to hendelsestype.name,
        "fnr" to fnr,
        "aktørId" to aktørId,
        "orgnummer" to orgnummer,
        "rapportertdato" to rapportertdato,
        "sykeperioder" to sykeperioder.map { SykeperiodeReflect(it).toMap() },
        "originalJson" to originalJson
    )

    private class SykeperiodeReflect(sykeperiode: Reflect) {
        private val fom: LocalDate = sykeperiode["fom"]
        private val tom: LocalDate = sykeperiode["tom"]
        private val sykdomsgrad: Int = sykeperiode["sykdomsgrad"]

        internal fun toMap() = mutableMapOf<String, Any?>(
            "fom" to fom,
            "tom" to tom,
            "sykdomsgrad" to sykdomsgrad
        )
    }
}
