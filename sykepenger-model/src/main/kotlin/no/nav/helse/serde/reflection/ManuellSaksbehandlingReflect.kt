package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelManuellSaksbehandling
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse.Hendelsestype
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import java.time.LocalDateTime
import java.util.*

internal class ManuellSaksbehandlingReflect(manuellSaksbehandling: ModelManuellSaksbehandling) {
    private val hendelseId: UUID = manuellSaksbehandling.hendelseId()
    private val hendelsestype: Hendelsestype = manuellSaksbehandling.hendelsestype()
    private val aktørId: String = manuellSaksbehandling["aktørId"]
    private val fødselsnummer: String = manuellSaksbehandling["fødselsnummer"]
    private val organisasjonsnummer: String = manuellSaksbehandling["organisasjonsnummer"]
    private val vedtaksperiodeId: String = manuellSaksbehandling["vedtaksperiodeId"]
    private val saksbehandler: String = manuellSaksbehandling["saksbehandler"]
    private val utbetalingGodkjent: Boolean = manuellSaksbehandling["utbetalingGodkjent"]
    private val rapportertdato: LocalDateTime = manuellSaksbehandling["rapportertdato"]
    private val aktivitetslogger: Aktivitetslogger = manuellSaksbehandling["aktivitetslogger"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to hendelsestype.name,
        "data" to mutableMapOf<String, Any?>(
            "hendelseId" to hendelseId,
            //"aktørId" to aktørId, // TODO ?
            //"fødselsnummer" to fødselsnummer, // TODO ?
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "saksbehandler" to saksbehandler,
            "utbetalingGodkjent" to utbetalingGodkjent,
            "rapportertdato" to rapportertdato,
            "aktivitetslogger" to AktivitetsloggerReflect(aktivitetslogger).toMap()
        )
    )

    internal fun toSpeilMap() = mutableMapOf<String, Any?>(
        "type" to hendelsestype.name,
        "hendelseId" to hendelseId,
        "organisasjonsnummer" to organisasjonsnummer,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "saksbehandler" to saksbehandler,
        "utbetalingGodkjent" to utbetalingGodkjent,
        "rapportertdato" to rapportertdato
    )
}
