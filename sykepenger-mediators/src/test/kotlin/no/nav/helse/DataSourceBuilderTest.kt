package no.nav.helse

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DataSourceBuilderTest {

    @Test
    fun `kaster ikke exception når tilkobling konfigureres riktig`() {
        assertDoesNotThrow {
            GcpDataSourceBuilder(mapOf(
                "DATABASE_HOST" to "foobar",
                "DATABASE_PORT" to "foobar",
                "DATABASE_DATABASE" to "foobar",
                "DATABASE_USERNAME" to "foobar",
                "DATABASE_PASSWORD" to "foobar"
            ))
        }
    }

    @Test
    fun `kaster exception ved mangende konfig`() {
        assertThrows<IllegalArgumentException> {
            GcpDataSourceBuilder(emptyMap())
        }

        assertThrows<IllegalArgumentException> {
            GcpDataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar"
                )
            )
        }

        assertThrows<IllegalArgumentException> {
            GcpDataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar",
                    "DATABASE_PORT" to "foobar"
                )
            )
        }

        assertThrows<IllegalArgumentException> {
            GcpDataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar",
                    "DATABASE_PORT" to "foobar",
                    "DATABASE_DATABASE" to "foobar"
                )
            )
        }

        assertThrows<IllegalArgumentException> {
            GcpDataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar",
                    "DATABASE_PORT" to "foobar",
                    "DATABASE_DATABASE" to "foobar",
                    "DATABASE_USERNAME" to "foobar"
                )
            )
        }
    }
}
