package no.nav.helse.inspectors

import java.util.UUID
import no.nav.helse.sykdomstidslinje.SykdomshistorikkElementView
import no.nav.helse.sykdomstidslinje.SykdomshistorikkView
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

internal val SykdomshistorikkView.inspektør get() = SykdomshistorikkInspektør(this.elementer)

internal class SykdomshistorikkInspektør(historikk: Collection<SykdomshistorikkElementView>) {
    private val tidslinjer = mutableListOf<Sykdomstidslinje>()
    private val perioderPerHendelse = mutableMapOf<UUID, MutableList<Sykdomstidslinje>>()

    val size get() = tidslinjer.size

    init {
        historikk.forEach { innslag ->
            innslag.hendelseId?.let {
                perioderPerHendelse.getOrPut(it.id) { mutableListOf() }.add(innslag.hendelseSykdomstidslinje)
            }
            tidslinjer.add(innslag.beregnetSykdomstidslinje)
        }
    }

    fun sykdomstidslinje() = tidslinjer.first()
    fun elementer() = tidslinjer.size
    fun perioderPerHendelse() = perioderPerHendelse.toMap().mapValues { (_, sykdomstidslinjer) -> sykdomstidslinjer.toList() }
    fun tidslinje(elementIndeks: Int) = tidslinjer[elementIndeks]
}
