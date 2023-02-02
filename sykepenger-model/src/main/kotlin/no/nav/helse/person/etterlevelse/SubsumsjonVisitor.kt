package no.nav.helse.person.etterlevelse

import no.nav.helse.hendelser.Periode
import java.time.LocalDate
import no.nav.helse.etterlevelse.Bokstav
import no.nav.helse.etterlevelse.KontekstType
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.etterlevelse.Punktum

internal interface SubsumsjonVisitor {
    fun preVisitSubsumsjon(
        utfall: Subsumsjon.Utfall,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd?,
        punktum: Punktum?,
        bokstav: Bokstav?,
        input: Map<String, Any>,
        output: Map<String, Any>,
        kontekster: Map<String, KontekstType>
    ) {}

    fun visitGrupperbarSubsumsjon(perioder: List<Periode>) {}
    fun visitBetingetSubsumsjon(funnetRelevant: Boolean) {}

    fun postVisitSubsumsjon(
        utfall: Subsumsjon.Utfall,
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
