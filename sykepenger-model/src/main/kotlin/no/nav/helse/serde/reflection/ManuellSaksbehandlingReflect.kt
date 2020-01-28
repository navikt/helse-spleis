package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelManuellSaksbehandling
import no.nav.helse.person.ArbeidstakerHendelse.Hendelsestype
import java.time.LocalDateTime
import java.util.*

internal class ManuellSaksbehandlingReflect(manuellSaksbehandling: ModelManuellSaksbehandling) {
    private val hendelseId: UUID = manuellSaksbehandling.hendelseId()
    private val hendelsestype: Hendelsestype = manuellSaksbehandling.hendelsetype()
    private val aktørId: String = manuellSaksbehandling["aktørId"]
    private val fødselsnummer: String = manuellSaksbehandling["fødselsnummer"]
    private val organisasjonsnummer: String = manuellSaksbehandling["organisasjonsnummer"]
    private val vedtaksperiodeId: String = manuellSaksbehandling["vedtaksperiodeId"]
    private val saksbehandler: String = manuellSaksbehandling["saksbehandler"]
    private val utbetalingGodkjent: Boolean = manuellSaksbehandling["utbetalingGodkjent"]
    private val rapportertdato: LocalDateTime = manuellSaksbehandling["rapportertdato"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "hendelseId" to hendelseId,
        "hendelsetype" to hendelsestype.name,
        "aktørId" to aktørId,
        "fødselsnummer" to fødselsnummer,
        "organisasjonsnummer" to organisasjonsnummer,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "saksbehandler" to saksbehandler,
        "utbetalingGodkjent" to utbetalingGodkjent,
        "rapportertdato" to rapportertdato
    )
}
