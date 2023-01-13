package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Personopplysninger
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import kotlin.reflect.KClass

internal typealias Melding = KClass<out SykdomstidslinjeHendelse>

abstract class SykdomstidslinjeHendelse(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    private val opprettet: LocalDateTime,
    melding: Melding? = null,
    private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
    personopplysninger: Personopplysninger? = null
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg, personopplysninger) {
    private companion object {
        private val aldri = LocalDate.MIN til LocalDate.MIN
    }

    protected constructor(meldingsreferanseId: UUID, other: SykdomstidslinjeHendelse) : this(meldingsreferanseId, other.fødselsnummer, other.aktørId, other.organisasjonsnummer, other.opprettet, null, other.aktivitetslogg)

    private var nesteFom: LocalDate? = null

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

    internal fun erRelevant(other: Periode) = overlappsperiode()?.overlapperMed(other) ?: false

    internal fun oppdaterFom(other: Periode): Periode {
        if (trimmetForbi()) return other
        return other.oppdaterFom(this.periode())
    }

    protected open fun overlappsperiode(): Periode? = sykdomstidslinje().periode()

    internal fun trimLeft(dato: LocalDate) {
        nesteFom = dato.plusDays(1)
    }

    protected fun trimmetForbi() = periode() == aldri

    internal fun periode(): Periode {
        val periode = overlappsperiode() ?: aldri
        val fom = nesteFom?.takeUnless { it < periode.start } ?: return periode
        if (fom > periode.endInclusive) return aldri
        return (sykdomstidslinje().førsteSykedagEtterEllerLik(fom) ?: fom) til periode.endInclusive
    }

    internal abstract fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver): IAktivitetslogg

    internal abstract fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver)

    internal open fun padLeft(dato: LocalDate) {}

    override fun equals(other: Any?): Boolean = other is SykdomstidslinjeHendelse
        && this.meldingsreferanseId() == other.meldingsreferanseId()

    internal abstract fun leggTil(hendelseIder: MutableSet<Dokumentsporing>)

    override fun hashCode(): Int {
        return meldingsreferanseId().hashCode()
    }
}

