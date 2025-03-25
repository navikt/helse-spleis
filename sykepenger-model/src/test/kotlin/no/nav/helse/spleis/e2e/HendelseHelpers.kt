package no.nav.helse.spleis.e2e

import java.util.*
import no.nav.helse.person.aktivitetslogg.Aktivitet

internal class EtterspurtBehov(
    private val type: Aktivitet.Behov.Behovtype,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID
) {
    companion object {
        internal fun fjern(liste: MutableList<EtterspurtBehov>, orgnummer: String, type: Aktivitet.Behov.Behovtype) {
            liste.removeIf { it.orgnummer == orgnummer && it.type == type }
        }

        internal fun finnEtterspurteBehov(behovsliste: List<Aktivitet.Behov>) =
            behovsliste
                .filter { "organisasjonsnummer" in it.alleKontekster }
                .filter { "vedtaksperiodeId" in it.alleKontekster }
                .map {
                    EtterspurtBehov(
                        type = it.type,
                        orgnummer = (it.alleKontekster["organisasjonsnummer"] as String),
                        vedtaksperiodeId = UUID.fromString(it.alleKontekster["vedtaksperiodeId"] as String)
                    )
                }

        internal fun finnEtterspurtBehov(
            ikkeBesvarteBehov: MutableList<EtterspurtBehov>,
            type: Aktivitet.Behov.Behovtype,
            vedtaksperiodeIdInnhenter: IdInnhenter,
            orgnummer: String
        ) =
            ikkeBesvarteBehov.firstOrNull { it.type == type && it.orgnummer == orgnummer && it.vedtaksperiodeId == vedtaksperiodeIdInnhenter.id(orgnummer) }

    }
}
