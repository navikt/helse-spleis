package no.nav.helse.hendelser

import no.nav.helse.FeatureToggle
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

internal class SykmeldingTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var sykmelding: Sykmelding
    private lateinit var aktivitetslogger: Aktivitetslogger
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setup() {
        aktivitetslogger = Aktivitetslogger()
        aktivitetslogg = Aktivitetslogg()
    }

    @AfterEach
    internal fun reset() {
        FeatureToggle.støtterGradertSykdom = false
    }

    @Test
    internal fun `sykdomsgrad som er 100% støttes`() {
        sykmelding(Triple(1.januar, 10.januar, 100), Triple(12.januar, 16.januar, 100))
        assertFalse(sykmelding.valider().hasErrorsOld())
        assertEquals(16, sykmelding.sykdomstidslinje().length())
    }

    @Test
    internal fun `sykdomsgrad under 100% støttes ikke`() {
        sykmelding(Triple(1.januar, 10.januar, 50), Triple(12.januar, 16.januar, 100))
        assertTrue(sykmelding.valider().hasErrorsOld())

    }

    @Test
    internal fun `sykdomsgrad under 100% støttes (epic 18)`() {
        FeatureToggle.støtterGradertSykdom = true
        sykmelding(Triple(1.januar, 10.januar, 50), Triple(12.januar, 16.januar, 100))
        assertFalse(sykmelding.valider().hasErrorsOld())
    }

    @Test
    internal fun `sykeperioder mangler`() {
        assertThrows<Aktivitetslogger.AktivitetException> { sykmelding() }
    }

    @Test
    internal fun `overlappende sykeperioder`() {
        assertThrows<Aktivitetslogger.AktivitetException> {
            sykmelding(Triple(10.januar, 12.januar, 100), Triple(1.januar, 12.januar, 100))
        }
    }

    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) {
        sykmelding = Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = "987654321",
            sykeperioder = listOf(*sykeperioder),
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg
        )
    }

}
