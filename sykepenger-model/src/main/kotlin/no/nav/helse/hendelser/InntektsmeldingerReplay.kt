package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

class InntektsmeldingerReplay(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    aktivitetslogg: Aktivitetslogg,
    private val vedtaksperiodeId: UUID,
    private val inntektsmeldinger: List<Inntektsmelding>
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg) {

    internal fun erRelevant(other: UUID) = other == vedtaksperiodeId

    internal fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        info("Replayer inntektsmeldinger for vedtaksperiode $vedtaksperiodeId og påfølgende som overlapper")
        inntektsmeldinger.forEach {
            arbeidsgiver.håndter(it, vedtaksperiodeId)
        }
    }
}
