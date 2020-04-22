package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDate
import java.util.*

abstract class SykdomstidslinjeHendelse(
    private val meldingsreferanseId: UUID
) : ArbeidstakerHendelse() {
    internal val kilde: Hendelseskilde = Hendelseskilde(this.javaClass, meldingsreferanseId)

    internal class Hendelseskilde(hendelse: Class<SykdomstidslinjeHendelse>, private val meldingsreferanseId: UUID) {
        private val type: String = hendelse.canonicalName.split('.').last()
        companion object {
            internal val INGEN = Hendelseskilde(SykdomstidslinjeHendelse::class.java, UUID.randomUUID())
        }
    }

    internal fun meldingsreferanseId() = meldingsreferanseId

    internal abstract fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje

    internal abstract fun sykdomstidslinje(): Sykdomstidslinje

    internal abstract fun nySykdomstidslinje(): NySykdomstidslinje

    internal abstract fun valider(periode: Periode): Aktivitetslogg

    internal abstract fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver)

    internal open fun padLeft(dato: LocalDate) {}
}
