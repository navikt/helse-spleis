package no.nav.helse

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.reflection.castAsList
import java.util.*

internal fun IAktivitetslogg.antallEtterspurteBehov(vedtaksperiodeId: UUID, behov: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.filter { it.type == behov }.size

internal fun IAktivitetslogg.etterspurteBehovFinnes(vedtaksperiodeId: UUID, behov: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.filter { it.type == behov }.isNotEmpty()

internal fun IAktivitetslogg.etterspurteBehovFinnes(vedtaksperiodeId: UUID, tilstandType: TilstandType, behov: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
            && it.kontekst()["tilstand"] == tilstandType.name
    }.filter { it.type == behov }.isNotEmpty()

internal fun IAktivitetslogg.etterspurteBehov(vedtaksperiodeId: UUID) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }

internal fun IAktivitetslogg.etterspurteBehov(vedtaksperiodeId: UUID, behov: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.filter { it.type == behov }.size == 1

inline fun <reified T> IAktivitetslogg.etterspurtBehov(vedtaksperiodeId: UUID, behov: Aktivitetslogg.Aktivitet.Behov.Behovtype, felt: String): T? {
    return this.behov()
        .filter { it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString() }
        .first { it.type == behov }.detaljer()[felt] as T?
}

private fun IAktivitetslogg.hent(alvorlighetsgrad: String) = toMap()["aktiviteter"]
    .castAsList<Map<String, Any>>()
    .filter { it["alvorlighetsgrad"] == alvorlighetsgrad }
    .map { it["melding"] }
    .mapNotNull { it.toString() }

internal fun IAktivitetslogg.hentErrors() = hent("ERROR")
internal fun IAktivitetslogg.hentWarnings() = hent("WARN")
internal fun IAktivitetslogg.hentInfo() = hent("INFO")
