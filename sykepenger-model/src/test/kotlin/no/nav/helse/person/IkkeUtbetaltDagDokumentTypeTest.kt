package no.nav.helse.person

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class IkkeUtbetaltDagDokumentTypeTest {

    @Test
    fun `spre-gosys og Flex skal kjenne til alle begrunnelser for avvist dag`() {
        val begrunnelserSomSpreGosysKjennerTil = listOf(
            "SykepengedagerOppbrukt",
            "MinimumInntekt",
            "SykepengedagerOppbruktOver67",
            "MinimumInntektOver67",
            "EgenmeldingUtenforArbeidsgiverperiode",
            "MinimumSykdomsgrad",
            "ManglerOpptjening",
            "ManglerMedlemskap",
            "EtterDødsdato",
            "Over70",
        )
        PersonObserver.Utbetalingsdag.EksternBegrunnelseDTO.values().forEach { begrunnelse ->
            assertTrue(begrunnelserSomSpreGosysKjennerTil.contains(begrunnelse.name)) {
                """

Hvis denne testen brekker bør du vurdere om ${begrunnelse.name} må legges inn i spre-gosys, før du legger inn verdien i denne testen.
Enum-verdiene brukes her: https://github.com/navikt/helse-spre/blob/d10087e5ab3795f50b1104315de086379e3f5019/gosys/src/main/kotlin/no/nav/helse/spre/gosys/vedtak/VedtakMessage.kt#L140-L154

"""
            }
        }
    }
}
