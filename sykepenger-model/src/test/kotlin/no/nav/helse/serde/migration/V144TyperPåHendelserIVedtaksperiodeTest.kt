package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import java.util.*

internal class V144TyperPåHendelserIVedtaksperiodeTest : MigrationTest(V144TyperPåHendelserIVedtaksperiode()) {

    @Test
    fun `legger til typer på hendelser i vedtaksperiode`() {
        assertMigration("/migrations/144/expected.json", "/migrations/144/original.json")
    }

    override fun meldingerSupplier(): MeldingerSupplier {
        return MeldingerSupplier {
            mapOf(
                UUID.fromString("791D9EA4-D1FD-4605-838B-AE4EE07B09D8") to Pair("NY_SØKNAD", "{}"),
                UUID.fromString("1E235A89-7C1E-49C8-8B05-1E3F4DB70417") to Pair("INNTEKTSMELDING", "{}"),
                UUID.fromString("9C404C60-B696-4E49-B66F-621E308F7D3A") to Pair("SENDT_SØKNAD_NAV", "{}"),
                UUID.fromString("387AF1E4-8667-4648-8980-5B444DEF43E6") to Pair("SENDT_SØKNAD_ARBEIDSGIVER", "{}"),
                UUID.fromString("14965CD7-70F6-4C9E-AF41-BB852928CE2E") to Pair("OVERSTYRTIDSLINJE", "{}"),
                UUID.fromString("70C99C8A-6C4F-456A-B637-C6A12EDAC287") to Pair("OVERSTYRTIDSLINJE", "{}"),
                UUID.fromString("4594D999-2D43-4C90-AEAE-F710B57130EE") to Pair("OVERSTYRINNTEKT", "{}"),
                UUID.fromString("29B6E217-49B7-4B1F-8C31-DC9EBB6E5F46") to Pair("OVERSTYRARBEIDSFORHOLD", "{}"),
            )
        }
    }
}
