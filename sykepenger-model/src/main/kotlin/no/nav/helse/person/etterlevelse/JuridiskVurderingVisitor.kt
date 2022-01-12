package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import java.time.LocalDate

interface JuridiskVurderingVisitor {
    fun preVisitVurdering(
        oppfylt: Boolean,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        bokstaver: List<Bokstav>,
        input: Map<String, Any>,
        output: Map<String, Any>
    ) {}

    fun visitGrupperbarVurdering(fom: LocalDate, tom: LocalDate) {}
    fun visitBetingetVurdering(funnetRelevant: Boolean) {}

    fun postVisitVurdering(
        oppfylt: Boolean,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        bokstaver: List<Bokstav>,
        input: Map<String, Any>,
        output: Map<String, Any>
    ) {}
}
