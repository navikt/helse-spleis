package no.nav.helse.etterlevelse

import java.util.*

class BehandlingSubsumsjonslogg(
    private val regelverkslogg: Regelverkslogg,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val behandlingId: UUID
) : Subsumsjonslogg {

    override fun logg(subsumsjon: Subsumsjon) {
        if (subsumsjon.erTomPeriode()) return
        regelverkslogg.logg(Regelverksporing(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            subsumsjon = subsumsjon
        ))
    }
}

