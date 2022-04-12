package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

data class ManuellOverskrivingDag(
    val dato: LocalDate,
    val type: Dagtype,
    val grad: Int? = null
)

enum class Dagtype {
    Sykedag, Feriedag, Egenmeldingsdag, Permisjonsdag, Avslått
}

class OverstyrTidslinje(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    dager: List<ManuellOverskrivingDag>,
    opprettet: LocalDateTime
) : SykdomstidslinjeHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, opprettet) {

    private val sykdomstidslinje: Sykdomstidslinje
    private var håndtert: Boolean = false

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
                Dagtype.Permisjonsdag -> Sykdomstidslinje.permisjonsdager(
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
                Dagtype.Avslått -> Sykdomstidslinje.avslåttdager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde
                )
            }
        }.reduce { acc, manuellOverskrivingDag -> acc + manuellOverskrivingDag }
    }

    internal fun alleredeHåndtert() = håndtert

    internal fun markerHåndtert() {
        require(!håndtert) { "Flere perioder forsøker å markere hendelsen som håndtert. Kun én periode skal håndtere hendelsen" }
        håndtert = true
    }

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver) = this

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    internal fun tilRevurderingAvvistEvent(): PersonObserver.RevurderingAvvistEvent =
        PersonObserver.RevurderingAvvistEvent(
            fødselsnummer = fødselsnummer,
            errors = this.errorsAndWorse()
        )

    override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.overstyrTidslinje(meldingsreferanseId()))
    }
}
