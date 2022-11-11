package no.nav.helse.serde.migration

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Refusjonsopplysning
import no.nav.helse.person.Refusjonsopplysning.Refusjonsopplysninger.RefusjonsopplysningerBuilder
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

internal class V197SpissetVilkårsgrunnlagKopi: KopiereVilkårsgrunnlag(
    versjon = 197,
    Triple(vilkårsgrunnlagId, skjæringstidspunkt, refusjonsopplysninger)
) {
    private companion object {
        private val vilkårsgrunnlagId = UUID.fromString("de52b638-4efb-4a51-8a55-08afd519876a")
        private val skjæringstidspunkt = LocalDate.parse("2021-09-21")
        private val refusjonsopplysninger = Refusjonsopplysning(
            meldingsreferanseId = UUID.fromString("8114b8e4-1c41-4c67-9ac7-6da3b55ff305"),
            fom = LocalDate.parse("2021-09-21"),
            tom = null,
            beløp = 43591.67.månedlig
        ).let { RefusjonsopplysningerBuilder().leggTil(it, LocalDateTime.now()).build() }
    }
}