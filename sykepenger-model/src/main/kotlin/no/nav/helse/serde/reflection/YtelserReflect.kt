package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse.Hendelsestype
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import java.time.LocalDateTime
import java.util.*

internal class YtelserReflect(ytelser: Ytelser) {
    private val hendelseId: UUID = ytelser.hendelseId()
    private val hendelsestype: Hendelsestype = ytelser.hendelsestype()
    private val aktørId: String = ytelser["aktørId"]
    private val fødselsnummer: String = ytelser["fødselsnummer"]
    private val organisasjonsnummer: String = ytelser["organisasjonsnummer"]
    private val vedtaksperiodeId: String = ytelser["vedtaksperiodeId"]
    private val utbetalingshistorikk: Utbetalingshistorikk = ytelser["sykepengehistorikk"]
    private val foreldrepenger: Foreldrepermisjon = ytelser["foreldrepenger"]
    private val rapportertdato: LocalDateTime = ytelser["rapportertdato"]
    private val aktivitetslogger: Aktivitetslogger = ytelser["aktivitetslogger"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to hendelsestype.name,
        "data" to mutableMapOf<String, Any?>(
            "hendelseId" to hendelseId,
            //"aktørId" to aktørId, // TODO ?
            //"fødselsnummer" to fødselsnummer, // TODO ?
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "sykepengehistorikk" to utbetalingshistorikk.let { SykepengehistorikkReflect(it).toMap() },
            "foreldrepenger" to foreldrepenger.let { ForeldrepengerReflect(it).toMap() },
            "rapportertdato" to rapportertdato,
            "aktivitetslogger" to AktivitetsloggerReflect(aktivitetslogger).toMap()
        )
    )
}
