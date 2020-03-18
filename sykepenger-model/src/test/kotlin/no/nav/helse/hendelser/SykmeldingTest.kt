package no.nav.helse.hendelser

import no.nav.helse.FeatureToggle
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

internal class SykmeldingTest {

    private companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var sykmelding: Sykmelding
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @AfterEach
    internal fun reset() {
        FeatureToggle.støtterGradertSykdom = false
    }

    @Test
    internal fun `sykdomsgrad som er 100% støttes`() {
        sykmelding(Triple(1.januar, 10.januar, 100), Triple(12.januar, 16.januar, 100))
        assertEquals(16, sykmelding.sykdomstidslinje().length())
    }

    @Test
    internal fun `sykdomsgrad under 100% støttes (epic 18)`() {
        FeatureToggle.støtterGradertSykdom = true
        sykmelding(Triple(1.januar, 10.januar, 50), Triple(12.januar, 16.januar, 100))
        assertFalse(sykmelding.valider().hasErrors())
    }

    @Test
    internal fun `sykeperioder mangler`() {
        assertThrows<Aktivitetslogg.AktivitetException> { sykmelding() }
    }

    @Test
    internal fun `overlappende sykeperioder`() {
        assertThrows<Aktivitetslogg.AktivitetException> {
            sykmelding(Triple(10.januar, 12.januar, 100), Triple(1.januar, 12.januar, 100))
        }
    }

    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) {
        sykmelding = Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = "987654321",
            sykeperioder = listOf(*sykeperioder)
        )
    }

}
