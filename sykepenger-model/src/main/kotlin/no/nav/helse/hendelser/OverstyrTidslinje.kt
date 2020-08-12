package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.time.LocalDate
import java.util.*

data class ManuellOverskrivingDag(
    val dato: LocalDate,
    val type: Dagtype,
    val grad: Int? = null
)

enum class Dagtype {
    Sykedag, Feriedag, Egenmeldingsdag
}

class OverstyrTidslinje(
    meldingsreferanseId: UUID,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val organisasjonsnummer: String,
    dager: List<ManuellOverskrivingDag>
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private val sykdomstidslinje: Sykdomstidslinje

    init {
        sykdomstidslinje = dager.map {
            val kilde = Hendelseskilde(OverstyrTidslinje::class, meldingsreferanseId)
            when (it.type) {
                Dagtype.Sykedag -> Sykdomstidslinje.sykedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    grad = it.grad!!, // Sykedager må ha grad
                    kilde = kilde
                )
                Dagtype.Feriedag -> Sykdomstidslinje.feriedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde
                )
                Dagtype.Egenmeldingsdag -> Sykdomstidslinje.arbeidsgiverdager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    grad = 100,
                    kilde = kilde
                )
            }
        }.reduce { acc, manuellOverskrivingDag -> acc + manuellOverskrivingDag }
    }

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun valider(periode: Periode) = Aktivitetslogg()

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer
}
