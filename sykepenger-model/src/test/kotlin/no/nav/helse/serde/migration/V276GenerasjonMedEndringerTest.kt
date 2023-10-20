package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V276GenerasjonMedEndringerTest: MigrationTest(V276GenerasjonMedEndringer()) {

    @Test
    fun `test 01`() {
        assertMigration("/migrations/276/expected/01__uberegnet_periode.json", "/migrations/276/actual/01__uberegnet_periode.json")
    }
    @Test
    fun `test 02`() {
        assertMigration("/migrations/276/expected/02__uberegnet_periode_med_egenmeldingsdager.json", "/migrations/276/actual/02__uberegnet_periode_med_egenmeldingsdager.json")
    }
    @Test
    fun `test 03`() {
        assertMigration("/migrations/276/expected/03__auu_uten_inntektsmelding.json", "/migrations/276/actual/03__auu_uten_inntektsmelding.json")
    }
    @Test
    fun `test 04`() {
        assertMigration("/migrations/276/expected/04__auu_med_inntektsmelding.json", "/migrations/276/actual/04__auu_med_inntektsmelding.json")
    }
    @Test
    fun `test 05`() {
        assertMigration("/migrations/276/expected/05__avventer_godkjenning.json", "/migrations/276/actual/05__avventer_godkjenning.json")
    }
    @Test
    fun `test 06`() {
        assertMigration("/migrations/276/expected/06__avsluttet_periode.json", "/migrations/276/actual/06__avsluttet_periode.json")
    }
    @Test
    fun `test 07`() {
        assertMigration("/migrations/276/expected/07__uberegnet_revurdering.json", "/migrations/276/actual/07__uberegnet_revurdering.json")
    }
    @Test
    fun `test 08`() {
        assertMigration("/migrations/276/expected/08__beregnet_periode_mange_utbetalinger_som_er_forkastet.json", "/migrations/276/actual/08__beregnet_periode_mange_utbetalinger_som_er_forkastet.json")
    }
    @Test
    fun `test 09`() {
        assertMigration("/migrations/276/expected/09__avvist_beregnet_periode.json", "/migrations/276/actual/09__avvist_beregnet_periode.json")
    }
    @Test
    fun `test 10`() {
        assertMigration("/migrations/276/expected/10__avvist_beregnet_revurdering.json", "/migrations/276/actual/10__avvist_beregnet_revurdering.json")
    }
    @Test
    fun `test 11`() {
        assertMigration("/migrations/276/expected/11__til_utbetaling.json", "/migrations/276/actual/11__til_utbetaling.json")
    }
    @Test
    fun `test 12`() {
        assertMigration("/migrations/276/expected/12__endring_etter_til_utbetaling.json", "/migrations/276/actual/12__endring_etter_til_utbetaling.json")
    }
    @Test
    fun `test 13`() {
        assertMigration("/migrations/276/expected/13__beregnet_revurdering.json", "/migrations/276/actual/13__beregnet_revurdering.json")
    }
    @Test
    fun `test 14`() {
        assertMigration("/migrations/276/expected/14__auu_etter_beregnet_utbetaling.json", "/migrations/276/actual/14__auu_etter_beregnet_utbetaling.json")
    }
    @Test
    fun `test 15`() {
        assertMigration("/migrations/276/expected/15__annullering_etter_utbetalt.json", "/migrations/276/actual/15__annullering_etter_utbetalt.json")
    }
    @Test
    fun `test 16`() {
        assertMigration("/migrations/276/expected/16__annullering_etter_uberegnet_revurdering.json", "/migrations/276/actual/16__annullering_etter_uberegnet_revurdering.json")
    }
    @Test
    fun `test 17`() {
        assertMigration("/migrations/276/expected/17__annullering_etter_beregnet_revurdering.json", "/migrations/276/actual/17__annullering_etter_beregnet_revurdering.json")
    }
    @Test
    fun `test 18`() {
        assertMigration("/migrations/276/expected/18__forkastet_auu.json", "/migrations/276/actual/18__forkastet_auu.json")
    }
    @Test
    fun `test 19`() {
        assertMigration("/migrations/276/expected/19__forkastet_uberegnet_periode.json", "/migrations/276/actual/19__forkastet_uberegnet_periode.json")
    }
    @Test
    fun `test 20`() {
        assertMigration("/migrations/276/expected/20__forkastet_beregnet_periode.json", "/migrations/276/actual/20__forkastet_beregnet_periode.json")
    }
    @Test
    fun `test 21`() {
        assertMigration("/migrations/276/expected/21__periode_forsøkt_revurdert_flere_ganger_før_eldre_uberegnet_periode_tok_over.json", "/migrations/276/actual/21__periode_forsøkt_revurdert_flere_ganger_før_eldre_uberegnet_periode_tok_over.json")
    }
    @Test
    fun `test 22`() {
        assertMigration("/migrations/276/expected/22__periode_forsøkt_revurdert_flere_ganger_før_eldre_beregnet_periode_tok_over.json", "/migrations/276/actual/22__periode_forsøkt_revurdert_flere_ganger_før_eldre_beregnet_periode_tok_over.json")
    }
    @Test
    fun `test 23`() {
        assertMigration("/migrations/276/expected/23__korrigert_søknad_på_uberegnet_forlengelse.json", "/migrations/276/actual/23__korrigert_søknad_på_uberegnet_forlengelse.json")
    }
    @Test
    fun `test 24`() {
        assertMigration("/migrations/276/expected/24__korrigert_søknad_på_tidligere_beregnet_forlengelse.json", "/migrations/276/actual/24__korrigert_søknad_på_tidligere_beregnet_forlengelse.json")
    }
    @Test
    fun `test 25`() {
        assertMigration("/migrations/276/expected/25__flere_arbeidsgivere_med_forlengelse.json", "/migrations/276/actual/25__flere_arbeidsgivere_med_forlengelse.json")
    }
    @Test
    fun `test 26`() {
        assertMigration("/migrations/276/expected/26__omgjøring_forsøkt_avvist.json", "/migrations/276/actual/26__omgjøring_forsøkt_avvist.json")
    }
}