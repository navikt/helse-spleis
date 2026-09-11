package no.nav.helse.inspectors.view

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

internal data class SykdomshistorikkView(val elementer: List<SykdomshistorikkElementView>)
internal data class SykdomshistorikkElementView(
    val id: UUID,
    val hendelseId: MeldingsreferanseId?,
    val tidsstempel: LocalDateTime,
    val hendelseSykdomstidslinje: Sykdomstidslinje,
    val beregnetSykdomstidslinje: Sykdomstidslinje
)

internal fun Sykdomshistorikk.view() = SykdomshistorikkView(elementer = elementer().map { it.view() })

internal fun Sykdomshistorikk.Element.view() = SykdomshistorikkElementView(
    id = id,
    hendelseId = hendelseId,
    tidsstempel = tidsstempel,
    hendelseSykdomstidslinje = hendelseSykdomstidslinje,
    beregnetSykdomstidslinje = beregnetSykdomstidslinje
)
