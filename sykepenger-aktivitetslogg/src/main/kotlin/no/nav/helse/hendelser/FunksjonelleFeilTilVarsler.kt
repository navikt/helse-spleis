package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

class FunksjonelleFeilTilVarsler(private val other: IAktivitetslogg) : IAktivitetslogg by other {
    override fun funksjonellFeil(kode: Varselkode) {
        varsel(kode)
        info("Deeskalerer $kode")
    }

    override fun barn() = FunksjonelleFeilTilVarsler(other.barn())

    companion object {
        fun wrap(hendelse: PersonHendelse, block: () -> Unit) = hendelse.wrap(::FunksjonelleFeilTilVarsler, block)
    }
}