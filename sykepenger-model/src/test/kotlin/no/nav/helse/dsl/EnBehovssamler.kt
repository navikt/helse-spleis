package no.nav.helse.dsl

import java.util.UUID

interface EnBehovssamler {

    fun utbetalingsdetaljer(orgnummer: String): List<Utbetalingsdetaljer>
    fun simuleringsdetaljer(vedtaksperiodeId: UUID): List<Simuleringsdetaljer>

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
}


