package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Vedtaksperiode
import java.util.*

class InntektsmeldingReplay(
    private val wrapped: Inntektsmelding,
    private val vedtaksperiodeId: UUID
) : ArbeidstakerHendelse(wrapped) {

    private var harTruffet = false

    internal fun håndter(vedtaksperiode: Vedtaksperiode, id: UUID, periode: Periode): Boolean {
        if (!erRelevant(id)) {
            wrapped.trimLeft(periode.endInclusive)
            return false
        }
        kontekst(vedtaksperiode)
        info("Replayer inntektsmelding for vedtaksperiode $id - ${vedtaksperiode.periode()}")
        return vedtaksperiode.håndter(wrapped)
    }

    private fun erRelevant(vedtaksperiodeId: UUID): Boolean {
        if (!harTruffet) harTruffet = vedtaksperiodeId == this.vedtaksperiodeId
        return harTruffet
    }

    internal fun cacheRefusjon(arbeidsgiver: Arbeidsgiver) {
        wrapped.cacheRefusjon(arbeidsgiver)
    }

    override fun organisasjonsnummer() = wrapped.organisasjonsnummer()
    override fun aktørId() = wrapped.aktørId()
    override fun fødselsnummer() = wrapped.fødselsnummer()
}
