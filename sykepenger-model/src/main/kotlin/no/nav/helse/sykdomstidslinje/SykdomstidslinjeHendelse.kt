package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

internal typealias Melding = KClass<out SykdomstidslinjeHendelse>

abstract class SykdomstidslinjeHendelse(
    meldingsreferanseId: UUID,
    melding: Melding? = null,
    private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(meldingsreferanseId, aktivitetslogg) {

    protected constructor(meldingsreferanseId: UUID, other: SykdomstidslinjeHendelse) : this(meldingsreferanseId, null, other.aktivitetslogg)

    private var forrigeTom: LocalDate? = null

    internal val kilde: Hendelseskilde = Hendelseskilde(melding ?: this::class, meldingsreferanseId())

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

    internal abstract fun sykdomstidslinje(): Sykdomstidslinje

    internal open fun erRelevant(other: Periode) = periode().overlapperMed(other)

    internal open fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje {
        require(forrigeTom == null || (forrigeTom != null && tom > forrigeTom)) { "Kalte metoden flere ganger med samme eller en tidligere dato" }

        return (forrigeTom?.let { sykdomstidslinje().subset(Periode(it.plusDays(1), tom)) }
            ?: sykdomstidslinje().fremTilOgMed(tom))
            .also { trimLeft(tom) }
            .also { it.periode() ?: severe("Ugyldig subsetting av tidslinjen til søknad") }
    }

    internal fun trimLeft(dato: LocalDate) {
        forrigeTom = dato
    }

    internal fun periode(): Periode {
        val periode = sykdomstidslinje().periode()!!
        val fom = forrigeTom?.plusDays(1) ?: return periode
        return periode.forskyvFom(sykdomstidslinje().førsteSykedagEtter(fom) ?: fom)
    }

    internal abstract fun valider(periode: Periode): IAktivitetslogg

    internal abstract fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver)

    internal open fun padLeft(dato: LocalDate) {}

    override fun equals(other: Any?): Boolean = other is SykdomstidslinjeHendelse
        && this.meldingsreferanseId() == other.meldingsreferanseId()
}
