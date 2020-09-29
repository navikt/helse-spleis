package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Annullering(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    internal val saksbehandlerIdent: String,
    internal val saksbehandler: String,
    internal val saksbehandlerEpost: String,
    internal val opprettet: LocalDateTime,
    internal val fom: LocalDate,
    internal val tom: LocalDate
) : SykdomstidslinjeHendelse(meldingsreferanseId, Annullering::class){

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun sykdomstidslinje() = Sykdomstidslinje.annullerteDager(fom til tom, this.kilde)

    override fun valider(periode: Periode) = aktivitetslogg

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun organisasjonsnummer() = organisasjonsnummer

}
