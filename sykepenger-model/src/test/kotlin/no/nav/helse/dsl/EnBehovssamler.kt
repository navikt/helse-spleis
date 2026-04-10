package no.nav.helse.dsl

import java.util.UUID

interface EnBehovssamler {

    fun utbetalingsdetaljer(orgnummer: String): List<Utbetalingsdetaljer>

    data class Utbetalingsdetaljer(
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val utbetalingId: UUID,
        val fagsystemId: String
    )
}


