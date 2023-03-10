package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import java.time.LocalDate.MIN
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import kotlin.reflect.KClass

internal typealias Melding = KClass<out SykdomstidslinjeHendelse>

abstract class SykdomstidslinjeHendelse(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    private val opprettet: LocalDateTime,
    melding: Melding? = null,
    private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg) {
    private companion object {
        private val aldri = LocalDate.MIN til LocalDate.MIN
    }

    protected constructor(meldingsreferanseId: UUID, other: SykdomstidslinjeHendelse) : this(meldingsreferanseId, other.fødselsnummer, other.aktørId, other.organisasjonsnummer, other.opprettet, null, other.aktivitetslogg)

    private var forrigeTom: LocalDate = LocalDate.MIN
    internal val kilde: Hendelseskilde = Hendelseskilde(melding ?: this::class, meldingsreferanseId(), opprettet)

    internal class Hendelseskilde(
        private val type: String,
        private val meldingsreferanseId: UUID,
        private val tidsstempel: LocalDateTime
    ) {
        internal constructor(
            hendelse: Melding,
            meldingsreferanseId: UUID,
            tidsstempel: LocalDateTime
        ) : this(kildenavn(hendelse), meldingsreferanseId, tidsstempel)

        companion object {
            internal val INGEN = Hendelseskilde(SykdomstidslinjeHendelse::class, UUID.randomUUID(), LocalDateTime.now())

            private fun kildenavn(hendelse: Melding): String =
                hendelse.simpleName ?: "Ukjent"

            internal fun tidligsteTidspunktFor(kilder: List<Hendelseskilde>, type: Melding): LocalDateTime {
                check(kilder.all { it.erAvType(type) })
                return kilder.first().tidsstempel
            }
        }

        override fun toString() = type
        internal fun meldingsreferanseId() = meldingsreferanseId
        internal fun erAvType(meldingstype: Melding) = this.type == kildenavn(meldingstype)
        internal fun toJson() = mapOf("type" to type, "id" to meldingsreferanseId, "tidsstempel" to tidsstempel)
    }

    internal abstract fun sykdomstidslinje(): Sykdomstidslinje

    internal fun oppdaterFom(other: Periode): Periode {
        if (trimmetForbi()) return other
        return other.oppdaterFom(this.periode())
    }

    internal fun trimLeft(dato: LocalDate) {
        forrigeTom = dato
    }

    internal fun noenHarHåndtert() = forrigeTom != MIN

    private fun trimmetForbi() = periode() == aldri

    protected open fun overlappsperiode(): Periode? = sykdomstidslinje().periode()

    internal fun periode(): Periode {
        val periode = overlappsperiode() ?: aldri
        return periode.beholdDagerEtter(forrigeTom) ?: aldri
    }

    internal abstract fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver): IAktivitetslogg

    internal open fun padLeft(dato: LocalDate) {}

    override fun equals(other: Any?): Boolean = other is SykdomstidslinjeHendelse
        && this.meldingsreferanseId() == other.meldingsreferanseId()

    internal abstract fun leggTil(hendelseIder: MutableSet<Dokumentsporing>): Boolean

    override fun hashCode(): Int {
        return meldingsreferanseId().hashCode()
    }
}

