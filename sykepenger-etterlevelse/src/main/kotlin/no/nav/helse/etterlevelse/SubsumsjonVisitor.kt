package no.nav.helse.etterlevelse

import java.time.LocalDate

interface SubsumsjonVisitor {
    fun visitSubsumsjon(
        utfall: Subsumsjon.Utfall,
        lovverk: String,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd?,
        punktum: Punktum?,
        bokstav: Bokstav?,
        input: Map<String, Any>,
        output: Map<String, Any>,
        kontekster: Map<String, KontekstType>
    ) {}
}
