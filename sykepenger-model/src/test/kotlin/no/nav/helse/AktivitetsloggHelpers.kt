package no.nav.helse

import java.util.*
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

private fun Aktivitetslogg.etterspurteBehov(vedtaksperiodeId: UUID) =
    behov.filter {
        it.kontekster.any {
            it.kontekstType == "Vedtaksperiode" && it.kontekstMap["vedtaksperiodeId"] == vedtaksperiodeId.toString()
        }
    }

// TODO: Fjern meg!
internal inline fun <reified T> Aktivitetslogg.hentFeltFraBehov(vedtaksperiodeId: UUID, behov: Aktivitet.Behov.Behovtype, felt: String): T? {
    return etterspurteBehov(vedtaksperiodeId).last { it.type == behov }.detaljer[felt] as T?
}
