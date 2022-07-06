package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.PersonObserver
import org.junit.jupiter.api.Assertions.assertTrue

internal class Behovsamler : PersonObserver {
    private val behov = mutableListOf<Aktivitetslogg.Aktivitet.Behov>()

    internal fun registrerBehov(aktivitetslogg: IAktivitetslogg) {
        val nyeBehov = aktivitetslogg.behov().takeUnless { it.isEmpty() } ?: return
        println("Registrerer ${nyeBehov.size} nye behov (${nyeBehov.joinToString { it.type.toString() }})")
        behov.addAll(nyeBehov)
        println(" -> Det er nå ${behov.size} behov (${behov.joinToString { it.type.toString() }})")
    }

    internal fun harBehov(vedtaksperiodeId: UUID, vararg behovtyper: Behovtype): Boolean {
        val vedtaksperiodebehov = behovFor(vedtaksperiodeId).map { it.type }
        return behovtyper.all { behovtype -> behovtype in vedtaksperiodebehov }
    }

    internal fun bekreftBehov(vedtaksperiodeId: UUID, vararg behovtyper: Behovtype) {
        assertTrue(harBehov(vedtaksperiodeId, *behovtyper)) {
            val vedtaksperiodebehov = behovFor(vedtaksperiodeId)
            "Forventer at $behovtyper skal være etterspurt. Fant bare: ${vedtaksperiodebehov.joinToString { it.type.toString() }}"
        }
    }

    internal fun detaljerFor(vedtaksperiodeId: UUID, behovtype: Behovtype) =
        behovFor(vedtaksperiodeId).filter { it.type == behovtype }.map { it.detaljer() to it.kontekst() }

    internal fun kvitterBehov(vedtaksperiodeId: UUID) {
        val vedtaksperiodebehov = behovFor(vedtaksperiodeId).takeUnless { it.isEmpty() } ?: return
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

    private fun behovFor(vedtaksperiodeId: UUID) =
        behov.filter { behov -> behov.vedtaksperiodeId == vedtaksperiodeId }

    private companion object {
        private val Aktivitetslogg.Aktivitet.Behov.vedtaksperiodeId get() =
            kontekst()["vedtaksperiodeId"]?.let { UUID.fromString(it) }
    }
}
