package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.SendtSøknad
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse.Hendelsestype
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SendtSøknadReflect(sendtSøknad: SendtSøknad) {
    private val hendelseId: UUID = sendtSøknad.hendelseId()
    private val hendelsestype: Hendelsestype = sendtSøknad.hendelsestype()
    private val fnr: String = sendtSøknad["fnr"]
    private val aktørId: String = sendtSøknad["aktørId"]
    private val orgnummer: String = sendtSøknad["orgnummer"]
    private val sendtNav: LocalDateTime = sendtSøknad["sendtNav"]
    private val perioder: List<SendtSøknad.Periode> = sendtSøknad["perioder"]
    private val aktivitetslogger: Aktivitetslogger = sendtSøknad["aktivitetslogger"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to hendelsestype.name,
        "data" to mutableMapOf<String, Any?>(
            "hendelseId" to hendelseId,
            "fnr" to fnr,
            "aktørId" to aktørId,
            "orgnummer" to orgnummer,
            "sendtNav" to sendtNav,
            "perioder" to perioder.map { PeriodeReflect(it).toMap() },
            "aktivitetslogger" to AktivitetsloggerReflect(aktivitetslogger).toMap()
        )
    )

    internal fun toSpeilMap() = mutableMapOf<String, Any?>(
        "type" to hendelsestype.name,
        "hendelseId" to hendelseId,
        "fnr" to fnr,
        "aktørId" to aktørId,
        "orgnummer" to orgnummer,
        "sendtNav" to sendtNav,
        "perioder" to perioder.map { PeriodeReflect(it).toMap() }
    )

    private class PeriodeReflect(private val periode: SendtSøknad.Periode) {
        private val fom: LocalDate = periode.fom
        private val tom: LocalDate = periode.tom

        internal fun toMap() = mutableMapOf<String, Any?>(
            "fom" to fom,
            "tom" to tom
        ).also {
            when (periode) {
                is SendtSøknad.Periode.Ferie -> it["type"] = "Ferie"
                is SendtSøknad.Periode.Sykdom -> {
                    it["type"] = "Sykdom"
                    it["grad"] = periode.get<SendtSøknad.Periode.Sykdom, Int>("grad")
                    it["faktiskGrad"] = periode.get<SendtSøknad.Periode.Sykdom, Double>("faktiskGrad")
                }
                is SendtSøknad.Periode.Utdanning -> {
                    it["type"] = "Utdanning"
                    it["fom"] = periode.get<SendtSøknad.Periode.Utdanning, LocalDate?>("fom")
                }
                is SendtSøknad.Periode.Permisjon -> it["type"] = "Permisjon"
                is SendtSøknad.Periode.Egenmelding -> it["type"] = "Egenmelding"
                is SendtSøknad.Periode.Arbeid -> it["type"] = "Arbeid"
            }
        }
    }
}
