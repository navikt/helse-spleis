package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V129KopiereSimuleringsResultatTilOppdragTest : MigrationTest(V129KopiereSimuleringsResultatTilOppdrag()) {

    @Test
    fun `kopierer simuleringsresultat over til personoppdrag når personen er mottaker`() {
        assertMigration(
            expectedJson = "/migrations/129/velgerPersonOppdragExpected.json",
            originalJson = "/migrations/129/velgerPersonOppdragOriginal.json"
        )
    }

    @Test
    fun `kopierer simuleringsresultat over til arbeidsgiveroppdrag når arbeidsgiver er mottaker`() {
        assertMigration(
            expectedJson = "/migrations/129/velgerPersonOppdragExpected.json",
            originalJson = "/migrations/129/velgerPersonOppdragOriginal.json"
        )
    }

    @Test
    fun `kopierer ikke simuleringsresultat når oppdraget allerede har det`() {
        assertMigration(
            expectedJson = "/migrations/129/oppdragSomAlleredeHarSimuleringsResultatExpected.json",
            originalJson = "/migrations/129/oppdragSomAlleredeHarSimuleringsResultatOriginal.json"
        )
    }

    @Test
    fun `hensyntar ikke utbetalinger hvor oppdragene ikke har noen utbetalinger`() {
        assertMigration(
            expectedJson = "/migrations/129/utbetalingerMedOppdragUtenUtbetalngerExpected.json",
            originalJson = "/migrations/129/utbetalingerMedOppdragUtenUtbetalngerOriginal.json"
        )
    }

    @Test
    fun `vedtaksperiode med to simulerte utbetalinger`() {
        assertMigration(
            expectedJson = "/migrations/129/vedtaksperiodMedToSimulerteUtbetalingerExpected.json",
            originalJson = "/migrations/129/vedtaksperiodMedToSimulerteUtbetalingerOriginal.json"
        )
    }

    @Test
    fun `vedtaksperiode uten simuleringsresultat`() {
        assertMigration(
            expectedJson = "/migrations/129/vedtaksperiodeUtenSimuleringsResultatExpected.json",
            originalJson = "/migrations/129/vedtaksperiodeUtenSimuleringsResultatOriginal.json"
        )
    }

    @Test
    fun `finner ingen utbetaling simuleringsresultatet kan høre til`() {
        assertMigration(
            expectedJson = "/migrations/129/ingenUtbetalingSimuleringenHørerTilExpected.json",
            originalJson = "/migrations/129/ingenUtbetalingSimuleringenHørerTilOriginal.json"
        )
    }
}
