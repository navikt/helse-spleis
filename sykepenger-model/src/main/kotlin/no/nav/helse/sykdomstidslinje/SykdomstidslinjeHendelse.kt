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

    internal class Hendelseskilde(private val type: String, private val meldingsreferanseId: UUID) {
        internal constructor(
            hendelse: Class<SykdomstidslinjeHendelse>,
            meldingsreferanseId: UUID
        ) : this(hendelse.canonicalName.split('.').last(), meldingsreferanseId)

        companion object {
            internal val INGEN = Hendelseskilde(SykdomstidslinjeHendelse::class.java, UUID.randomUUID())
        }

        override fun toString() = type
        internal fun meldingsreferanseId() = meldingsreferanseId
    }

    internal fun meldingsreferanseId() = meldingsreferanseId

    internal abstract fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje

    internal abstract fun sykdomstidslinje(): Sykdomstidslinje

    internal abstract fun nySykdomstidslinje(): NySykdomstidslinje

    internal abstract fun valider(periode: Periode): Aktivitetslogg

    internal abstract fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver)

    internal open fun padLeft(dato: LocalDate) {}
}
