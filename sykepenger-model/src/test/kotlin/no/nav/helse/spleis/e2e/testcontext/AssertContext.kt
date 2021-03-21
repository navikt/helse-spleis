package no.nav.helse.spleis.e2e.testcontext

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.spleis.e2e.TestObservatør
import org.junit.jupiter.api.Assertions.*
import java.util.*
import kotlin.reflect.KClass

internal class AssertContext(
    private val e2eTest: AbstractEndToEndTest,
    private val person: Person,
    private val observatør: TestObservatør,
    private val ikkeBesvarteBehov: MutableList<EtterspurtBehov>,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID
) {
    private val inspektør get() = TestArbeidsgiverInspektør(person, organisasjonsnummer)

    internal operator fun invoke(assertBlock: AssertContext.(TestArbeidsgiverInspektør, TestObservatør) -> Unit) {
        assert(assertBlock)
    }

    internal fun assert(assertBlock: AssertContext.(TestArbeidsgiverInspektør, TestObservatør) -> Unit) {
        assertBlock(inspektør, observatør)
    }

    internal fun assertTilstander(vararg tilstand: TilstandType) {
        assertFalse(inspektør.periodeErForkastet(vedtaksperiodeId)) { "Perioden er forkastet" }
        assertTrue(inspektør.periodeErIkkeForkastet(vedtaksperiodeId)) { "Perioden er forkastet" }
        assertEquals(tilstand.asList(), observatør.tilstander[vedtaksperiodeId])
    }

    internal fun assertEtterspurt(løsning: KClass<out ArbeidstakerHendelse>, vararg type: Aktivitetslogg.Aktivitet.Behov.Behovtype) {
        type.forEach {
            val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, it, vedtaksperiodeId, organisasjonsnummer)
            assertTrue(ikkeBesvarteBehov.remove(etterspurtBehov)) {
                "Forventer at $it skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
                    observatør.tilstander[vedtaksperiodeId]?.last()
                }.\nAktivitetsloggen:\n${inspektør.personLogg}"
            }
        }
    }

    internal fun assertIkkeEtterspurt(løsning: KClass<out ArbeidstakerHendelse>, vararg type: Aktivitetslogg.Aktivitet.Behov.Behovtype) {
        type.forEach {
            val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, it, vedtaksperiodeId, organisasjonsnummer)
            assertFalse(etterspurtBehov in ikkeBesvarteBehov) {
                "Forventer ikke at $it skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
                    observatør.tilstander[vedtaksperiodeId]?.last()
                }"
            }
        }
    }
}
