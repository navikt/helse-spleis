package no.nav.helse.dsl

import java.util.UUID

interface EnBehovssamler {

    fun utbetalingsdetaljer(orgnummer: String): List<Utbetalingsdetaljer>
    fun simuleringsdetaljer(vedtaksperiodeId: UUID): List<Simuleringsdetaljer>
    fun godkjenningsdetaljer(vedtaksperiodeId: UUID): Godkjenningsdetaljer
    fun bekreftForespurtVilkårsprøving(vedtaksperiodeId: UUID)
    fun bekreftForespurtBeregningAvSelvstendig(vedtaksperiodeId: UUID)
    fun bekreftForespurtBeregningAvArbeidstaker(vedtaksperiodeId: UUID)

    data class Utbetalingsdetaljer(
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val utbetalingId: UUID,
        val fagsystemId: String
    )

    data class Simuleringsdetaljer(
        val vedtaksperiodeId: UUID,
        val utbetalingId: UUID,
        val fagsystemId: String,
        val fagområde: String
    )

    data class Godkjenningsdetaljer(
        val behandlingId: UUID,
        val utbetalingId: UUID
    )
}


