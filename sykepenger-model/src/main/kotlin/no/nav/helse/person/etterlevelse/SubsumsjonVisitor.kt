package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import java.time.LocalDate

interface SubsumsjonVisitor {
    fun preVisitSubsumsjon(
        utfall: Subsumsjon.Utfall,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        bokstaver: List<Bokstav>,
        input: Map<String, Any>,
        output: Map<String, Any>,
        kontekster: Map<String, String>
    ) {}

    fun visitGrupperbarSubsumsjon(fom: LocalDate, tom: LocalDate) {}
    fun visitBetingetSubsumsjon(funnetRelevant: Boolean) {}

    fun postVisitSubsumsjon(
        utfall: Subsumsjon.Utfall,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        bokstaver: List<Bokstav>,
        input: Map<String, Any>,
        output: Map<String, Any>,
        kontekster: Map<String, String>
    ) {}
}
