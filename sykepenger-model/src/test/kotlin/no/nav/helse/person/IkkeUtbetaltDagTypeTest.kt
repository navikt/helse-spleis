package no.nav.helse.person

import no.nav.helse.person.PersonObserver.UtbetaltEvent.IkkeUtbetaltDag
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class IkkeUtbetaltDagTypeTest {
    @Test
    fun `alle typer IkkeUtbetaltDag dag skal ligge i spre-gosys`() {
        val ikkeUtbetaltDagerSomSpreGosysKjennerTil = listOf(
            "AvvistDag",
            "Fridag",
            "Arbeidsdag"
        )
        IkkeUtbetaltDag.Type.values()
            .filterNot { it == IkkeUtbetaltDag.Type.Annullering } // Denne fins ikke i spre-gosys - men jeg tror ikke den er i bruk i Spleis
            .forEach { type ->
                assertTrue(ikkeUtbetaltDagerSomSpreGosysKjennerTil.contains(type.name)) {
                    """

Hvis denne testen brekker bør du vurdere om ${type.name} må legges inn i spre-gosys, før du legger inn verdien i denne testen.
Enum-verdiene brukes her: https://github.com/navikt/helse-spre/blob/d10087e5ab3795f50b1104315de086379e3f5019/gosys/src/main/kotlin/no/nav/helse/spre/gosys/vedtak/VedtakMessage.kt#L122-L135

"""
                }
            }
    }

    @Test
    fun `alle begrunnelser for avvist dag skal ligge i spre-gosys`() {
        val begrunnelserSomSpreGosysKjennerTil = listOf(
            "SykepengedagerOppbrukt",
            "MinimumInntekt",
            "EgenmeldingUtenforArbeidsgiverperiode",
            "MinimumSykdomsgrad",
            "ManglerOpptjening",
            "ManglerMedlemskap",
            "EtterDødsdato"
        )
        IkkeUtbetaltDag.Begrunnelse.values().forEach { begrunnelse ->
            assertTrue(begrunnelserSomSpreGosysKjennerTil.contains(begrunnelse.name)) {
                """

Hvis denne testen brekker bør du vurdere om ${begrunnelse.name} må legges inn i spre-gosys, før du legger inn verdien i denne testen.
Enum-verdiene brukes her: https://github.com/navikt/helse-spre/blob/d10087e5ab3795f50b1104315de086379e3f5019/gosys/src/main/kotlin/no/nav/helse/spre/gosys/vedtak/VedtakMessage.kt#L140-L154

"""
            }
        }
    }
}
