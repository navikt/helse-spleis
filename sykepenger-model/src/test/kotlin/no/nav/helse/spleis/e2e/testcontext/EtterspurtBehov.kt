package no.nav.helse.spleis.e2e.testcontext

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.TilstandType
import java.util.*

internal class EtterspurtBehov(
    private val type: Aktivitetslogg.Aktivitet.Behov.Behovtype,
    private val tilstand: TilstandType,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID
) {
    companion object {
        internal fun finnEtterspurteBehov(behovsliste: List<Aktivitetslogg.Aktivitet.Behov>) =
            behovsliste
                .filter { "tilstand" in it.kontekst() }
                .filter { "organisasjonsnummer" in it.kontekst() }
                .filter { "vedtaksperiodeId" in it.kontekst() }
                .map {
                    EtterspurtBehov(
                        type = it.type,
                        tilstand = enumValueOf(it.kontekst()["tilstand"] as String),
                        orgnummer = it.kontekst()["organisasjonsnummer"] as String,
                        vedtaksperiodeId = UUID.fromString(it.kontekst()["vedtaksperiodeId"] as String)
                    )
                }

        internal fun finnEtterspurtBehov(
            ikkeBesvarteBehov: List<EtterspurtBehov>,
            type: Aktivitetslogg.Aktivitet.Behov.Behovtype,
            vedtaksperiodeId: UUID,
            orgnummer: String
        ) =
            ikkeBesvarteBehov.firstOrNull { it.type == type && it.orgnummer == orgnummer && it.vedtaksperiodeId == vedtaksperiodeId }
    }

    override fun toString() = "$type ($tilstand)"
}
