package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.AktivitetsloggVisitor
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.serde.api.AktivitetDTO
import java.util.*

internal class PeriodeVarslerBuilder(
    aktivitetslogg: Aktivitetslogg
) : AktivitetsloggVisitor {

    private val varsler = mutableListOf<AktivitetDTO>()

    init {
        aktivitetslogg.accept(this)
    }

    override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
        kontekster.find { it.kontekstType == "Vedtaksperiode" }
            ?.let { it.kontekstMap["vedtaksperiodeId"] }
            ?.let(UUID::fromString)
            ?.also { vedtaksperiodeId ->
                varsler.add(AktivitetDTO(vedtaksperiodeId, "W", melding, tidsstempel))
            }
    }

    fun build(): List<AktivitetDTO> = varsler.distinctBy { it.melding }
}


