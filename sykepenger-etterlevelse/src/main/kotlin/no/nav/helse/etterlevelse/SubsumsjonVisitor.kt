package no.nav.helse.etterlevelse

import java.time.LocalDate

interface SubsumsjonVisitor {
    fun preVisitSubsumsjon(
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

    fun visitGrupperbarSubsumsjon(perioder: List<ClosedRange<LocalDate>>) {}
    fun visitBetingetSubsumsjon(funnetRelevant: Boolean) {}

    fun postVisitSubsumsjon(
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
