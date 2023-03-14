package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.Arbeidsgiver

class InntektsmeldingReplay(
    private val wrapped: Inntektsmelding,
    private val vedtaksperiodeId: UUID
) : ArbeidstakerHendelse(wrapped) {

    internal fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        info("Replayer inntektsmelding for vedtaksperiode $vedtaksperiodeId og påfølgende som overlapper")
        arbeidsgiver.håndter(wrapped, vedtaksperiodeId)
    }
    override fun venter(arbeidsgivere: List<Arbeidsgiver>) {}
}
