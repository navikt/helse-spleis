package no.nav.helse.inspectors

import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

internal val Sykdomshistorikk.inspektør get() = SykdomshistorikkInspektør(this)

internal class SykdomshistorikkInspektør(historikk: Sykdomshistorikk) {
    private val tidslinjer = mutableListOf<Sykdomstidslinje>()
    private val perioderPerHendelse = mutableMapOf<UUID, MutableList<Periode>>()

    val size get() = tidslinjer.size

    init {
        historikk.forEach { innslag ->
            innslag.hendelseId?.let {
                perioderPerHendelse.getOrPut(it) { mutableListOf() }.add(innslag.hendelseSykdomstidslinje.periode()!!)
            }
            tidslinjer.add(innslag.beregnetSykdomstidslinje)
        }
    }

    fun sykdomstidslinje() = tidslinjer.first()
    fun elementer() = tidslinjer.size
    fun perioderPerHendelse() = perioderPerHendelse.toMap()
    fun tidslinje(elementIndeks: Int) = tidslinjer[elementIndeks]
}
