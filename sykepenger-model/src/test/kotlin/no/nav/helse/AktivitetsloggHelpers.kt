package no.nav.helse

import java.util.UUID
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal fun IAktivitetslogg.antallEtterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, behovtype: Aktivitet.Behov.Behovtype, orgnummer: String = AbstractPersonTest.ORGNUMMER) =
    antallEtterspurteBehov(vedtaksperiodeIdInnhenter.id(orgnummer), behovtype)

internal fun IAktivitetslogg.etterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, behovtype: Aktivitet.Behov.Behovtype, orgnummer: String = AbstractPersonTest.ORGNUMMER) =
    etterspurteBehovFinnes(vedtaksperiodeIdInnhenter.id(orgnummer), behovtype)

internal fun IAktivitetslogg.etterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String = AbstractPersonTest.ORGNUMMER) =
    etterspurteBehov(vedtaksperiodeIdInnhenter.id(orgnummer))

internal fun IAktivitetslogg.sisteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String = AbstractPersonTest.ORGNUMMER) =
    behov().last { it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeIdInnhenter.id(orgnummer).toString() }

internal fun IAktivitetslogg.sisteBehov(vedtaksperiodeId: UUID) =
    behov().last { it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString() }

internal fun IAktivitetslogg.sisteBehov(type: Aktivitet.Behov.Behovtype) =
    behov().last { it.type == type }

internal fun IAktivitetslogg.harBehov(behov: Aktivitet.Behov.Behovtype) =
    this.behov().any { it.type == behov }

internal fun IAktivitetslogg.antallEtterspurteBehov(vedtaksperiodeId: UUID, behov: Aktivitet.Behov.Behovtype) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.count { it.type == behov }

internal fun IAktivitetslogg.etterspurteBehovFinnes(vedtaksperiodeId: UUID, behov: Aktivitet.Behov.Behovtype) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.any { it.type == behov }

internal fun IAktivitetslogg.etterspurteBehov(vedtaksperiodeId: UUID) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }

internal fun IAktivitetslogg.etterspurteBehov(vedtaksperiodeId: UUID, behov: Aktivitet.Behov.Behovtype) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.filter { it.type == behov }.size == 1

inline fun <reified T> IAktivitetslogg.etterspurtBehov(vedtaksperiodeId: UUID, behov: Aktivitet.Behov.Behovtype, felt: String): T? {
    return this.behov()
        .filter { it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString() }
        .last { it.type == behov }.detaljer()[felt] as T?
}
