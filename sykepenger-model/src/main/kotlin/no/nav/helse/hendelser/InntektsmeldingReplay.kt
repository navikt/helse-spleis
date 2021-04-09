package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import java.util.*

class InntektsmeldingReplay(
    private val wrapped: Inntektsmelding,
    private val vedtaksperiodeId: UUID
) : ArbeidstakerHendelse(wrapped) {

    internal fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        info("Replayer inntektsmelding for vedtaksperiode $vedtaksperiodeId og påfølgende som overlapper")
        arbeidsgiver.håndter(wrapped, vedtaksperiodeId)
    }

    override fun organisasjonsnummer() = wrapped.organisasjonsnummer()
    override fun aktørId() = wrapped.aktørId()
    override fun fødselsnummer() = wrapped.fødselsnummer()
}
