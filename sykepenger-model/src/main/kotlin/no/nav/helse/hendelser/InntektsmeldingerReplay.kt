package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.SYSTEM

class InntektsmeldingerReplay(
    meldingsreferanseId: MeldingsreferanseId,
    organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val inntektsmeldinger: List<Inntektsmelding>
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
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
}
