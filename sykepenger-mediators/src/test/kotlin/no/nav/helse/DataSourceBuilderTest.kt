package no.nav.helse

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DataSourceBuilderTest {

    @Test
    fun `kaster ikke exception når tilkobling konfigureres riktig`() {
        assertDoesNotThrow {
            DataSourceBuilder(mapOf(
                "DATABASE_HOST" to "foobar",
                "DATABASE_PORT" to "foobar",
                "DATABASE_NAME" to "foobar",
                "DATABASE_USERNAME" to "foobar",
                "DATABASE_PASSWORD" to "foobar"
            ))
        }

        assertDoesNotThrow {
            DataSourceBuilder(mapOf(
                "DATABASE_JDBC_URL" to "foobar"
            ))
        }

        assertDoesNotThrow {
            DataSourceBuilder(mapOf(
                "DATABASE_HOST" to "foobar",
                "DATABASE_PORT" to "foobar",
                "DATABASE_NAME" to "foobar",
                "VAULT_MOUNTPATH" to "foobar"
            ))
        }

        assertDoesNotThrow {
            DataSourceBuilder(mapOf(
                "DATABASE_JDBC_URL" to "foobar",
                "DATABASE_NAME" to "foobar",
                "VAULT_MOUNTPATH" to "foobar"
            ))
        }
    }

    @Test
    fun `kaster exception ved mangende konfig`() {
        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(emptyMap())
        }

        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar"
                )
            )
        }

        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar"
                )
            )
        }

        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar",
                    "DATABASE_PORT" to "foobar"
                )
            )
        }

        assertThrows<IllegalStateException> {
            DataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar",
                    "DATABASE_PORT" to "foobar",
                    "DATABASE_NAME" to "foobar"
                )
            )
        }

        assertThrows<IllegalStateException> {
            DataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar",
                    "DATABASE_PORT" to "foobar",
                    "DATABASE_NAME" to "foobar",
                    "DATABASE_USERNAME" to "foobar"
                )
            )
        }

        assertThrows<IllegalStateException> {
            DataSourceBuilder(mapOf(
                "DATABASE_JDBC_URL" to "foobar",
                "VAULT_MOUNTPATH" to "foobar"
            ))
        }

        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(mapOf(
                "VAULT_MOUNTPATH" to "foobar",
                "DATABASE_USERNAME" to "foobar"
            ))
        }

        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(mapOf(
                "VAULT_MOUNTPATH" to "foobar",
                "DATABASE_PASSWORD" to "foobar"
            ))
        }
    }
}
