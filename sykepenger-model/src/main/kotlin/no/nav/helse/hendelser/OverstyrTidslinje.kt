package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.AAP
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Dagpenger
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Foreldrepenger
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Omsorgspenger
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Opplæringspenger
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Pleiepenger
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

data class ManuellOverskrivingDag(
    val dato: LocalDate,
    val type: Dagtype,
    val grad: Int? = null
) {
    init {
        check(type !in setOf(Dagtype.Sykedag, Dagtype.SykedagNav) || grad != null) {
            "👉 Sykedager må ha grad altså 👈"
        }
    }
}

enum class Dagtype {
    Sykedag, Feriedag, ArbeidIkkeGjenopptattDag, Egenmeldingsdag, Permisjonsdag, Arbeidsdag, SykedagNav,
    Foreldrepengerdag, AAPdag, Omsorgspengerdag, Pleiepengerdag, Svangerskapspengerdag, Opplaringspengerdag, Dagpengerdag;

    companion object {
        val gyldigeTyper = Dagtype.values().map { it.name }
        val String.dagtype get() = Dagtype.valueOf(this)
    }
}

class OverstyrTidslinje(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    dager: List<ManuellOverskrivingDag>,
    private val opprettet: LocalDateTime
) : SykdomstidslinjeHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, opprettet) {

    private val periode: Periode
    private var sykdomstidslinje: Sykdomstidslinje

    init {
        sykdomstidslinje = dager.map {
            when (it.type) {
                Dagtype.Sykedag -> Sykdomstidslinje.sykedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    grad = it.grad!!.prosent, // Sykedager må ha grad
                    kilde = kilde
                )
                Dagtype.Feriedag -> Sykdomstidslinje.feriedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde
                )
                Dagtype.ArbeidIkkeGjenopptattDag -> Sykdomstidslinje.arbeidIkkeGjenopptatt(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde
                )
                Dagtype.Permisjonsdag -> Sykdomstidslinje.permisjonsdager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde
                )
                Dagtype.Arbeidsdag -> Sykdomstidslinje.arbeidsdager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde
                )
                Dagtype.Egenmeldingsdag -> Sykdomstidslinje.arbeidsgiverdager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    grad = 100.prosent,
                    kilde = kilde
                )
                Dagtype.SykedagNav -> Sykdomstidslinje.sykedagerNav(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    grad = it.grad!!.prosent, // Sykedager må ha grad
                    kilde = kilde
                )
                Dagtype.Foreldrepengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Foreldrepenger
                )
                Dagtype.AAPdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = AAP
                )
                Dagtype.Omsorgspengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Omsorgspenger
                )
                Dagtype.Pleiepengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Pleiepenger
                )
                Dagtype.Svangerskapspengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Svangerskapspenger
                )
                Dagtype.Opplaringspengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Opplæringspenger
                )
                Dagtype.Dagpengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Dagpenger
                )
            }
        }.reduce(Sykdomstidslinje::plus)
        periode = checkNotNull(sykdomstidslinje.periode()) {
            "Overstyr tidslinje må ha minst én overstyrt dag"
        }
    }

    override fun erRelevant(other: Periode) = other.oppdaterFom(other.start.forrigeDag).overlapperMed(periode())

    override fun sykdomstidslinje() = sykdomstidslinje
    override fun trimSykdomstidslinje(fom: LocalDate) {
        sykdomstidslinje = sykdomstidslinje.fraOgMed(fom)
    }

    override fun dokumentsporing() = Dokumentsporing.overstyrTidslinje(meldingsreferanseId())

    override fun innsendt() = opprettet
    override fun avsender() = SAKSBEHANDLER
}
