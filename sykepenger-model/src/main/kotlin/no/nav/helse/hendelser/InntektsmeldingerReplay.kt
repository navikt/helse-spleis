package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

class InntektsmeldingerReplay(
    meldingsreferanseId: MeldingsreferanseId,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val inntektsmeldinger: List<Inntektsmelding>
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        organisasjonsnummer = organisasjonsnummer
    )
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

    internal fun erRelevant(other: UUID) = other == vedtaksperiodeId

    internal fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Replayer inntektsmeldinger for vedtaksperiode $vedtaksperiodeId og påfølgende som overlapper")
        inntektsmeldinger.forEach {
            arbeidsgiver.håndter(it, aktivitetslogg, vedtaksperiodeId)
        }
    }
}
