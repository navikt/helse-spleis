package no.nav.helse.spleis.speil.builders

import java.util.UUID
import kotlin.test.assertEquals
import no.nav.helse.spleis.speil.SpekematDTO
import no.nav.helse.spleis.speil.SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO
import no.nav.helse.spleis.speil.SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO.PølsestatusDTO
import no.nav.helse.spleis.speil.builders.ArbeidsgiverBuilder.Companion.fjernUnødvendigeRader
import org.junit.jupiter.api.Test

// fun List<SpekematDTO.PølsepakkeDTO.PølseradDTO>.fjernUnødvendigeRader(): List<SpekematDTO.PølsepakkeDTO.PølseradDTO>
// vi skal altså redusere en liste med PølseradDTOer til en muligens litt mindre liste med PølseradDTOer
// hver pølserad er ... en generasjon?
class UnødvendigeRaderTest {

    @Test
    fun `én vedtaksperiode, masse behandlinger`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val org = listOf(
            rad(pøls(vedtaksperiodeId, UUID.randomUUID())),
            rad(pøls(vedtaksperiodeId, UUID.randomUUID())),
            rad(pøls(vedtaksperiodeId, UUID.randomUUID())),
            rad(pøls(vedtaksperiodeId, UUID.randomUUID())),
            rad(pøls(vedtaksperiodeId, UUID.randomUUID()))
        ).fjernUnødvendigeRader()
        assertEquals(5, org.size)
    }

    @Test
    fun `én vedtaksperiode, bare duplikate behandlinger`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val org = listOf(
            rad(pøls(vedtaksperiodeId, behandlingId)),
            rad(pøls(vedtaksperiodeId, behandlingId)),
            rad(pøls(vedtaksperiodeId, behandlingId)),
            rad(pøls(vedtaksperiodeId, behandlingId)),
            rad(pøls(vedtaksperiodeId, behandlingId))
        ).fjernUnødvendigeRader()
        assertEquals(2, org.size, "siden først og siste alltid tas med")
    }

    @Test
    fun `én vedtaksperiode, duplikate behandlinger inni der et sted`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val vedtaksperiodeId3 = UUID.randomUUID()
        val vedtaksperiodeId4 = UUID.randomUUID()
        val vedtaksperiodeId5 = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val org = listOf(
            rad(pøls(vedtaksperiodeId1, UUID.randomUUID())),
            rad(pøls(vedtaksperiodeId2, behandlingId)),
            rad(pøls(vedtaksperiodeId3, behandlingId)),
            rad(pøls(vedtaksperiodeId4, behandlingId)),
            rad(pøls(vedtaksperiodeId5, UUID.randomUUID()))
        ).fjernUnødvendigeRader()
        // dette tror jeg egentlig er litt buggy; vi mister alle de tre pølsene inni pakka, siden de er hverandres duplikater
        assertEquals(2, org.size, "dette må jo være feil?")
    }

    @Test
    fun `én vedtaksperiode, duplikate behandlinger med noe greier mellom et sted`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val vedtaksperiodeId3 = UUID.randomUUID()
        val vedtaksperiodeId4 = UUID.randomUUID()
        val vedtaksperiodeId5 = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val org = listOf(
            rad(pøls(vedtaksperiodeId1, UUID.randomUUID())),
            rad(pøls(vedtaksperiodeId2, behandlingId)),
            rad(pøls(vedtaksperiodeId3, UUID.randomUUID())),
            rad(pøls(vedtaksperiodeId4, behandlingId)),
            rad(pøls(vedtaksperiodeId5, UUID.randomUUID()))
        ).fjernUnødvendigeRader()
        assertEquals(5, org.size, "dette må jo være feil? Skal _alle_ med samme behandlings-id fjernes?")
    }

    private fun pøls(vedtaksperiodeId: UUID, behandlingId: UUID = UUID.randomUUID()) = PølseDTO(
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = behandlingId,
        status = PølsestatusDTO.LUKKET,
        kilde = UUID.randomUUID(),
    )

    private fun rad(vararg pøls: PølseDTO) = SpekematDTO.PølsepakkeDTO.PølseradDTO(pølser = pøls.asList(), UUID.randomUUID())
}
