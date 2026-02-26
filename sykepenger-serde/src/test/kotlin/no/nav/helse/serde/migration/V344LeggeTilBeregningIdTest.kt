package no.nav.helse.serde.migration

import java.util.UUID
import org.junit.jupiter.api.Test

class SekvensiellUUidGenerator() {
    var i = 0
    fun getIt(): UUID {
        i++
        require(i <= 99)
        return UUID.fromString("00000000-0000-0000-0000-0000000000${i.toString().padStart(2, '0')}")
    }
}

internal class V344LeggeTilBeregningIdTest : MigrationTest(V344LeggeTilBeregningId(idGenerator = SekvensiellUUidGenerator()::getIt)) {

    @Test
    fun `Legger på beregningId`() {
        assertMigration("/migrations/344/expected.json", "/migrations/344/original.json")
    }
}
