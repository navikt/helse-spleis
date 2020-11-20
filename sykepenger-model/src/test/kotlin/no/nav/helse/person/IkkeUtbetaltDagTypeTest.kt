package no.nav.helse.person

import no.nav.helse.person.PersonObserver.UtbetaltEvent.IkkeUtbetaltDag
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class IkkeUtbetaltDagTypeTest {
    @Test
    fun `alle typer avvist dag skal ligge i spre-gosys`() {
        val verdierSomSpreGosysKjennerTil = listOf(
            "SykepengedagerOppbrukt",
            "MinimumInntekt",
            "EgenmeldingUtenforArbeidsgiverperiode",
            "MinimumSykdomsgrad",
            "Fridag",
            "Arbeidsdag",
            "EtterDødsdato",
        )
        IkkeUtbetaltDag.Type.values()
            .filterNot { it == IkkeUtbetaltDag.Type.Annullering } // Denne fins ikke i spre-gosys - men jeg tror ikke den er i bruk i Spleis
            .forEach { type ->
                assertTrue(verdierSomSpreGosysKjennerTil.contains(type.name)) {
"""

Hvis denne testen brekker bør du vurdere om ${type.name} må legges inn i spre-gosys, før du legger inn verdien i denne testen.
Enum-verdiene brukes her: https://github.com/navikt/helse-spre-gosys/blob/88e10422a8488cabc470ddb992df13d6cbb2d44c/src/main/kotlin/no/nav/helse/vedtak/VedtakMessage.kt#L118-L132

"""
                }
            }
    }
}
