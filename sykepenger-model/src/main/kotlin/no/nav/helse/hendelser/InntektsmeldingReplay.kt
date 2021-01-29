package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.util.*

class InntektsmeldingReplay(
    meldingsreferanseId: UUID,
    internal val wrapped: Inntektsmelding,
    internal val vedtaksperiodeId: UUID
) : SykdomstidslinjeHendelse(meldingsreferanseId, wrapped) {
    override fun organisasjonsnummer() = wrapped.organisasjonsnummer()
    override fun aktørId() = wrapped.aktørId()
    override fun fødselsnummer() = wrapped.fødselsnummer()
    override fun sykdomstidslinje() = wrapped.sykdomstidslinje()
    override fun valider(periode: Periode) = wrapped.valider(periode)
    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.håndter(this)
}
