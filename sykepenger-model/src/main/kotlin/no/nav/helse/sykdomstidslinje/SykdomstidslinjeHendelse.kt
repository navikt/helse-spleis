package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

internal typealias Melding = KClass<out SykdomstidslinjeHendelse>

abstract class SykdomstidslinjeHendelse(
    private val meldingsreferanseId: UUID,
    melding: Melding? = null
) : ArbeidstakerHendelse() {
    private var forrigeTom: LocalDate? = null

    internal val kilde: Hendelseskilde = Hendelseskilde(melding ?: this::class, meldingsreferanseId)

    internal class Hendelseskilde(private val type: String, private val meldingsreferanseId: UUID) {
        internal constructor(
            hendelse: Melding,
            meldingsreferanseId: UUID
        ) : this(kildenavn(hendelse), meldingsreferanseId)

        companion object {
            internal val INGEN = Hendelseskilde(SykdomstidslinjeHendelse::class, UUID.randomUUID())

            private fun kildenavn(hendelse: Melding): String =
                hendelse.simpleName ?: "Ukjent"
        }

        override fun toString() = type
        internal fun meldingsreferanseId() = meldingsreferanseId
        internal fun erAvType(meldingstype: Melding) = this.type == kildenavn(meldingstype)
    }

    internal fun meldingsreferanseId() = meldingsreferanseId
    override fun kontekst() = mapOf(
        "id" to "$meldingsreferanseId"
    )

    internal abstract fun sykdomstidslinje(): Sykdomstidslinje

    internal open fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje {
        require(forrigeTom == null || (forrigeTom != null && tom > forrigeTom)) { "Kalte metoden flere ganger med samme eller en tidligere dato" }

        return (forrigeTom?.let { sykdomstidslinje().subset(Periode(it.plusDays(1), tom))} ?: sykdomstidslinje().kutt(tom))
            .also { trimLeft(tom) }
            .also { it.periode() ?: severe("Ugyldig subsetting av tidslinjen til søknad") }
    }

    internal fun trimLeft(dato: LocalDate) { forrigeTom = dato }

    internal open fun periode() = Periode(forrigeTom?.plusDays(1) ?: sykdomstidslinje().førsteDag(), sykdomstidslinje().sisteDag())

    internal abstract fun valider(periode: Periode): Aktivitetslogg

    internal abstract fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver)

    internal open fun padLeft(dato: LocalDate) {}

    override fun equals(other: Any?): Boolean = other is SykdomstidslinjeHendelse
        && this.meldingsreferanseId == other.meldingsreferanseId
}
