package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V244FinneOverlappendePerioderTest: MigrationTest(V244FinneOverlappendePerioder()) {
    @Test
    fun `migrerer perioder`() {
        /*
            må teste:

            to perioder som overlapper helt (og består av én dag)
            to perioder som overlapper helt (og består av mer enn én dag)
            to perioder som overlapper delvis

            en periode som består av én dag, og som overlapper med en periode som består av én dag => må ta manuelt?

            blir spist opp-strategi (uavhengig om vedtaksperioden man ser på består av én eller flere dager):
            en periode som består av én dag og som overlapper med en periode som strekker seg lengre: korte ned annen periode hvis éndag-perioden er i snute eller hale
         */
        assertMigration(
            expectedJson = "/migrations/244/expected.json",
            originalJson = "/migrations/244/original.json"
        )
    }
}