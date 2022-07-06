package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.PersonObserver
import org.junit.jupiter.api.Assertions.assertTrue

internal class Behovsamler : PersonObserver {
    private val behov = mutableListOf<Behov>()

    internal fun registrerBehov(aktivitetslogg: IAktivitetslogg) {
        val nyeBehov = aktivitetslogg.behov().takeUnless { it.isEmpty() } ?: return
        println("Registrerer ${nyeBehov.size} nye behov (${nyeBehov.joinToString { it.type.toString() }})")
        behov.addAll(nyeBehov)
        println(" -> Det er nå ${behov.size} behov (${behov.joinToString { it.type.toString() }})")
    }

    internal fun harBehov(vedtaksperiodeId: UUID, vararg behovtyper: Behovtype) =
        harBehov(vedtaksperiodebehov(vedtaksperiodeId), *behovtyper)

    internal fun harBehov(orgnummer: String, vararg behovtyper: Behovtype) =
        harBehov(orgnummerbehov(orgnummer), *behovtyper)

    internal fun harBehov(filter: (Behov) -> Boolean, vararg behovtyper: Behovtype): Boolean {
        val behover = behov.filter(filter).map { it.type }
        return behovtyper.all { behovtype -> behovtype in behover }
    }

    internal fun bekreftBehov(vedtaksperiodeId: UUID, vararg behovtyper: Behovtype) {
        bekreftBehov(vedtaksperiodebehov(vedtaksperiodeId), *behovtyper)
    }

    internal fun bekreftBehov(orgnummer: String, vararg behovtyper: Behovtype) {
        bekreftBehov(orgnummerbehov(orgnummer), *behovtyper)
    }

    internal fun bekreftBehov(filter: (Behov) -> Boolean, vararg behovtyper: Behovtype) {
        assertTrue(harBehov(filter, *behovtyper)) {
            val behover = behov.filter(filter)
            "Forventer at $behovtyper skal være etterspurt. Fant bare: ${behover.joinToString { it.type.toString() }}"
        }
    }

    internal fun detaljerFor(orgnummer: String, behovtype: Behovtype) =
        detaljerFor(orgnummerbehov(orgnummer), behovtype)

    internal fun detaljerFor(vedtaksperiodeId: UUID, behovtype: Behovtype) =
        detaljerFor(vedtaksperiodebehov(vedtaksperiodeId), behovtype)
    internal fun detaljerFor(filter: (Behov) -> Boolean, behovtype: Behovtype) =
        behov.filter { filter(it) && it.type == behovtype }.map { it.detaljer() to it.kontekst() }

    internal fun kvitterBehov(vedtaksperiodeId: UUID) {
        val vedtaksperiodebehov = behov.filter(vedtaksperiodebehov(vedtaksperiodeId)).takeUnless { it.isEmpty() } ?: return
        println("Fjerner ${vedtaksperiodebehov.size} behov (${vedtaksperiodebehov.joinToString { it.type.toString() }})")
        behov.removeAll { behov -> vedtaksperiodeId == behov.vedtaksperiodeId }
        println(" -> Det er nå ${behov.size} behov (${behov.joinToString { it.type.toString() }})")
    }

    override fun vedtaksperiodeEndret(
        hendelseskontekst: Hendelseskontekst,
        event: PersonObserver.VedtaksperiodeEndretEvent
    ) {
        val detaljer = mutableMapOf<String, String>().apply { hendelseskontekst.appendTo(this::put) }
        val vedtaksperiodeId = UUID.fromString(detaljer.getValue("vedtaksperiodeId"))
        kvitterBehov(vedtaksperiodeId)
    }

    private companion object {
        private val Behov.vedtaksperiodeId get() =
            kontekst()["vedtaksperiodeId"]?.let { UUID.fromString(it) }
        private val Behov.orgnummer get() = kontekst()["organisasjonsnummer"]

        private val vedtaksperiodebehov = { vedtaksperiodeId: UUID -> { behov: Behov -> behov.vedtaksperiodeId == vedtaksperiodeId } }
        private val orgnummerbehov = { orgnummer: String -> { behov: Behov -> behov.orgnummer == orgnummer } }
    }
}
