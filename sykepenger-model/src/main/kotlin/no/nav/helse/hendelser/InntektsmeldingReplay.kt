package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Arbeidsgiver

class InntektsmeldingReplay(
    private val wrapped: Inntektsmelding,
    private val vedtaksperiodeId: UUID,
    private val innsendt: LocalDateTime,
    private val registrert: LocalDateTime,
) : ArbeidstakerHendelse(wrapped) {

    internal fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        info("Replayer inntektsmelding for vedtaksperiode $vedtaksperiodeId og påfølgende som overlapper")
        arbeidsgiver.håndter(wrapped, vedtaksperiodeId)
    }
    override fun venter(block: () -> Unit) {
        // Kan potensielt replayes veldig mange inntektsmeldinger. Ønsker ikke å sende ut
        // vedtaksperiode_venter etter hver enkelt. Gjøres heller for InntektsmeldingReplayUtført
    }

    override fun innsendt() = innsendt

    override fun registrert() = registrert

    override fun avsender() = Avsender.ARBEIDSGIVER
}
