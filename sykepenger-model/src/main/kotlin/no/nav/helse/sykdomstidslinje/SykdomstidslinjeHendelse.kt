package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import java.time.LocalDate.MIN
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde

abstract class SykdomstidslinjeHendelse internal constructor(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    private val opprettet: LocalDateTime,
    melding: Melding? = null,
    private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg), SykdomshistorikkHendelse {
    private companion object {
        private val aldri = LocalDate.MIN til LocalDate.MIN
    }

    protected constructor(meldingsreferanseId: UUID, other: SykdomstidslinjeHendelse) : this(meldingsreferanseId, other.fødselsnummer, other.aktørId, other.organisasjonsnummer, other.opprettet, null, other.aktivitetslogg)

    private var forrigeTom: LocalDate = LocalDate.MIN
    internal val kilde: Hendelseskilde = Hendelseskilde(melding ?: this::class, meldingsreferanseId(), opprettet)

    internal abstract fun sykdomstidslinje(): Sykdomstidslinje
    override fun element() = Sykdomshistorikk.Element.opprett(meldingsreferanseId(), sykdomstidslinje())

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

    override fun equals(other: Any?): Boolean = other is SykdomstidslinjeHendelse
        && this.meldingsreferanseId() == other.meldingsreferanseId()

    internal abstract fun leggTil(hendelseIder: MutableSet<Dokumentsporing>): Boolean

    override fun hashCode(): Int {
        return meldingsreferanseId().hashCode()
    }
}

