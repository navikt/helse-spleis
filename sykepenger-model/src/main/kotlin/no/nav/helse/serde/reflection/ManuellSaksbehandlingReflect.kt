package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelManuellSaksbehandling
import java.time.LocalDateTime
import java.util.*

internal class ManuellSaksbehandlingReflect(manuellSaksbehandling: ModelManuellSaksbehandling) {
    private val hendelseId: UUID = manuellSaksbehandling.hendelseId()
    private val aktørId: String = manuellSaksbehandling.getProp("aktørId")
    private val fødselsnummer: String = manuellSaksbehandling.getProp("fødselsnummer")
    private val organisasjonsnummer: String = manuellSaksbehandling.getProp("organisasjonsnummer")
    private val vedtaksperiodeId: String = manuellSaksbehandling.getProp("vedtaksperiodeId")
    private val saksbehandler: String = manuellSaksbehandling.getProp("saksbehandler")
    private val utbetalingGodkjent: Boolean = manuellSaksbehandling.getProp("utbetalingGodkjent")
    private val rapportertdato: LocalDateTime = manuellSaksbehandling.getProp("rapportertdato")

    internal fun toMap() = mutableMapOf<String, Any?>(
        "hendelseId" to hendelseId,
        "aktørId" to aktørId,
        "fødselsnummer" to fødselsnummer,
        "organisasjonsnummer" to organisasjonsnummer,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "saksbehandler" to saksbehandler,
        "utbetalingGodkjent" to utbetalingGodkjent,
        "rapportertdato" to rapportertdato
    )
}
