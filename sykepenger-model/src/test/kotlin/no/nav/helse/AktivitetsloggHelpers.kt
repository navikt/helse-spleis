package no.nav.helse

import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import java.util.UUID

internal fun Aktivitetslogg.antallEtterspurteBehov(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    behovtype: Aktivitet.Behov.Behovtype,
    orgnummer: String = AbstractPersonTest.ORGNUMMER
) = antallEtterspurteBehov(vedtaksperiodeIdInnhenter.id(orgnummer), behovtype)

internal fun Aktivitetslogg.etterspurteBehov(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    behovtype: Aktivitet.Behov.Behovtype,
    orgnummer: String = AbstractPersonTest.ORGNUMMER
) = etterspurteBehovFinnes(vedtaksperiodeIdInnhenter.id(orgnummer), behovtype)

internal fun Aktivitetslogg.etterspurteBehov(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    orgnummer: String = AbstractPersonTest.ORGNUMMER
) = etterspurteBehov(vedtaksperiodeIdInnhenter.id(orgnummer))

internal fun Aktivitetslogg.sisteBehov(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    orgnummer: String = AbstractPersonTest.ORGNUMMER
) = behov.last { it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeIdInnhenter.id(orgnummer).toString() }

internal fun Aktivitetslogg.sisteBehov(vedtaksperiodeId: UUID) = behov.last { it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString() }

internal fun Aktivitetslogg.sisteBehov(type: Aktivitet.Behov.Behovtype) = behov.last { it.type == type }

internal fun Aktivitetslogg.harBehov(behov: Aktivitet.Behov.Behovtype) = this.behov.any { it.type == behov }

internal fun Aktivitetslogg.antallEtterspurteBehov(
    vedtaksperiodeId: UUID,
    behov: Aktivitet.Behov.Behovtype
) = this.behov
    .filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.count { it.type == behov }

internal fun Aktivitetslogg.etterspurteBehovFinnes(
    vedtaksperiodeId: UUID,
    behov: Aktivitet.Behov.Behovtype
) = this.behov
    .filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.any { it.type == behov }

internal fun Aktivitetslogg.etterspurteBehov(vedtaksperiodeId: UUID) =
    this.behov.filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }

internal fun Aktivitetslogg.etterspurteBehov(
    vedtaksperiodeId: UUID,
    behov: Aktivitet.Behov.Behovtype
) = this.behov
    .filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.filter { it.type == behov }
    .size == 1

inline fun <reified T> Aktivitetslogg.etterspurtBehov(
    vedtaksperiodeId: UUID,
    behov: Aktivitet.Behov.Behovtype,
    felt: String
): T? =
    this.behov
        .filter { it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString() }
        .last { it.type == behov }
        .detaljer()[felt] as T?
