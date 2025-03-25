package no.nav.helse

import java.util.*
import no.nav.helse.dsl.a1
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.e2e.IdInnhenter

internal fun Aktivitetslogg.antallEtterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, behovtype: Aktivitet.Behov.Behovtype, orgnummer: String = a1) =
    antallEtterspurteBehov(vedtaksperiodeIdInnhenter.id(orgnummer), behovtype)

internal fun Aktivitetslogg.etterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, behovtype: Aktivitet.Behov.Behovtype, orgnummer: String = a1) =
    etterspurteBehovFinnes(vedtaksperiodeIdInnhenter.id(orgnummer), behovtype)

internal fun Aktivitetslogg.etterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String = a1) =
    etterspurteBehov(vedtaksperiodeIdInnhenter.id(orgnummer))

internal fun Aktivitetslogg.sisteBehov(type: Aktivitet.Behov.Behovtype) =
    behov.last { it.type == type }

internal fun Aktivitetslogg.harBehov(behov: Aktivitet.Behov.Behovtype) =
    this.behov.any { it.type == behov }

internal fun Aktivitetslogg.antallEtterspurteBehov(vedtaksperiodeId: UUID, behov: Aktivitet.Behov.Behovtype) =
    this.behov.filter {
        it.alleKontekster["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.count { it.type == behov }

internal fun Aktivitetslogg.etterspurteBehovFinnes(vedtaksperiodeId: UUID, behov: Aktivitet.Behov.Behovtype) =
    this.behov.filter {
        it.alleKontekster["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.any { it.type == behov }

internal fun Aktivitetslogg.etterspurteBehov(vedtaksperiodeId: UUID) =
    behov.filter {
        it.kontekster.any {
            it.kontekstType == "Vedtaksperiode" && it.kontekstMap["vedtaksperiodeId"] == vedtaksperiodeId.toString()
        }
    }

internal inline fun <reified T> Aktivitetslogg.hentFeltFraBehov(vedtaksperiodeId: UUID, behov: Aktivitet.Behov.Behovtype, felt: String): T? {
    return etterspurteBehov(vedtaksperiodeId).last { it.type == behov }.detaljer[felt] as T?
}
