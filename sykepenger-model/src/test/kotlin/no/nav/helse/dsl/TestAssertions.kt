package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.e2e.TestObservatør
import org.junit.jupiter.api.Assertions

internal class TestAssertions(private val observatør: TestObservatør, private val inspektør: TestArbeidsgiverInspektør, private val personInspektør: PersonInspektør) {
    internal fun assertTilstander(id: UUID, vararg tilstander: TilstandType) {
        Assertions.assertFalse(inspektør.periodeErForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}:\n${personInspektør.aktivitetslogg}" }
        Assertions.assertTrue(inspektør.periodeErIkkeForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}\n${personInspektør.aktivitetslogg}" }
        Assertions.assertEquals(tilstander.asList(), observatør.tilstandsendringer[id])
    }
}