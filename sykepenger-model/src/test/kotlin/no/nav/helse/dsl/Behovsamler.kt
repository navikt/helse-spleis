package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import org.junit.jupiter.api.Assertions.assertTrue

internal class Behovsamler(private val log: DeferredLog) : PersonObserver {
    private val behov = mutableListOf<Behov>()
    private val tilstander = mutableMapOf<UUID, TilstandType>()
    private val replays = mutableSetOf<UUID>()

    internal fun registrerBehov(aktivitetslogg: IAktivitetslogg) {
        val nyeBehov = aktivitetslogg.behov().takeUnless { it.isEmpty() } ?: return
        log.info("Registrerer ${nyeBehov.size} nye behov (${nyeBehov.joinToString { it.type.toString() }})")
        behov.addAll(nyeBehov)
        log.info(" -> Det er nå ${behov.size} behov (${behov.joinToString { it.type.toString() }})")
    }

    internal fun harBehov(vedtaksperiodeId: UUID, vararg behovtyper: Behovtype) =
        harBehov(vedtaksperiodebehov(vedtaksperiodeId), *behovtyper)

    internal fun harBehov(orgnummer: String, vararg behovtyper: Behovtype) =
        harBehov(orgnummerbehov(orgnummer), *behovtyper)

    internal fun harBehov(filter: (Behov) -> Boolean, vararg behovtyper: Behovtype): Boolean {
        val behover = behov.filter(filter).map { it.type }
        return behovtyper.all { behovtype -> behovtype in behover }
    }

    internal fun bekreftBehovOppfylt() {
        val ubesvarte = behov.filterNot { it.type == Behovtype.Sykepengehistorikk }.takeUnless { it.isEmpty() } ?: return
        log.info("Etter testen er det ${behov.size} behov uten svar: [${behov.joinToString { it.type.toString() }}]")
    }

    internal fun bekreftOgKvitterReplay(vedtaksperiodeId: UUID) {
        assertTrue(replays.remove(vedtaksperiodeId)) { "Vedtaksperioden har ikke bedt om replay. Den står i ${tilstander.getValue(vedtaksperiodeId)}"}
    }

    internal fun bekreftBehov(vedtaksperiodeId: UUID, vararg behovtyper: Behovtype) {
        bekreftBehov(vedtaksperiodebehov(vedtaksperiodeId), *behovtyper) { "Vedtaksperioden står i ${tilstander.getValue(vedtaksperiodeId)}"}
    }

    internal fun bekreftBehov(orgnummer: String, vararg behovtyper: Behovtype) {
        bekreftBehov(orgnummerbehov(orgnummer), *behovtyper)
    }

    private fun bekreftBehov(filter: (Behov) -> Boolean, vararg behovtyper: Behovtype, melding: () -> String = { "" }) {
        assertTrue(harBehov(filter, *behovtyper)) {
            val behover = behov.filter(filter)
            "Forventer at [${behovtyper.joinToString { it.toString() }}] skal være etterspurt. Fant bare: [${behover.joinToString { it.type.toString() }}]. ${melding()}"
        }
    }

    internal fun detaljerFor(orgnummer: String, behovtype: Behovtype) =
        detaljerFor(orgnummerbehov(orgnummer), behovtype)
    internal fun detaljerFor(vedtaksperiodeId: UUID, behovtype: Behovtype) =
        detaljerFor(vedtaksperiodebehov(vedtaksperiodeId), behovtype)

    internal fun detaljerFor(filter: (Behov) -> Boolean, behovtype: Behovtype) =
        behov.filter { filter(it) && it.type == behovtype }.map { it.detaljer() to it.kontekst() }

    private fun kvitterVedtaksperiode(vedtaksperiodeId: UUID) {
        val vedtaksperiodebehov = behov.filter(vedtaksperiodebehov(vedtaksperiodeId)).takeUnless { it.isEmpty() } ?: return
        log.info("Fjerner ${vedtaksperiodebehov.size} behov (${vedtaksperiodebehov.joinToString { it.type.toString() }})")
        behov.removeAll { behov -> vedtaksperiodeId == behov.vedtaksperiodeId }
        log.info(" -> Det er nå ${behov.size} behov (${behov.joinToString { it.type.toString() }})")
        if (replays.remove(vedtaksperiodeId)) {
            log.info("-> Vedtaksperioden ba om replay, men det ble ikke utført")
        }
    }

    override fun utbetalingUtbetalt(
        hendelseskontekst: Hendelseskontekst,
        event: PersonObserver.UtbetalingUtbetaltEvent
    ) {
        assertTrue(behov.removeAll { it.utbetalingId == event.utbetalingId }) {
            "Utbetaling ble utbetalt, men ingen behov om utbetaling er registrert"
        }
    }

    override fun inntektsmeldingReplay(personidentifikator: Personidentifikator, vedtaksperiodeId: UUID) {
        replays.add(vedtaksperiodeId)
    }

    override fun vedtaksperiodeEndret(
        hendelseskontekst: Hendelseskontekst,
        event: PersonObserver.VedtaksperiodeEndretEvent
    ) {
        val detaljer = mutableMapOf<String, String>().apply { hendelseskontekst.appendTo(this::put) }
        val vedtaksperiodeId = UUID.fromString(detaljer.getValue("vedtaksperiodeId"))
        tilstander[vedtaksperiodeId] = event.gjeldendeTilstand
        kvitterVedtaksperiode(vedtaksperiodeId)
    }

    private companion object {
        private val Behov.utbetalingId get() =
            kontekst()["utbetalingId"]?.let { UUID.fromString(it) }
        private val Behov.vedtaksperiodeId get() =
            kontekst()["vedtaksperiodeId"]?.let { UUID.fromString(it) }
        private val Behov.orgnummer get() = kontekst()["organisasjonsnummer"]

        private val vedtaksperiodebehov = { vedtaksperiodeId: UUID -> { behov: Behov -> behov.vedtaksperiodeId == vedtaksperiodeId } }
        private val orgnummerbehov = { orgnummer: String -> { behov: Behov -> behov.orgnummer == orgnummer } }
    }
}
