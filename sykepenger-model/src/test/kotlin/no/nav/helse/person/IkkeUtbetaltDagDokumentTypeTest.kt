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
            "AndreYtelserAap",
            "AndreYtelserDagpenger",
            "AndreYtelserForeldrepenger",
            "AndreYtelserOmsorgspenger",
            "AndreYtelserOpplaringspenger",
            "AndreYtelserPleiepenger",
            "AndreYtelserSvangerskapspenger"
        )
        PersonObserver.Utbetalingsdag.EksternBegrunnelseDTO.entries.forEach { begrunnelse ->
            assertTrue(begrunnelserSomSpreGosysKjennerTil.contains(begrunnelse.name)) {
                """

Hvis denne testen brekker bør du vurdere om ${begrunnelse.name} må legges inn i spre-gosys, før du legger inn verdien i denne testen.
Enum-verdiene brukes her: 
- spre-gosys https://github.com/navikt/helse-spre/blob/3630001e084497c48c20f2f8b90ccb8b947a77dc/gosys/src/main/kotlin/no/nav/helse/spre/gosys/vedtak/VedtakMessage.kt#L133-L157
- sporbar: https://github.com/navikt/helse-sporbar/blob/8576fe61e2abb9b2bf7439c6b9c9a62cda6c92b0/src/main/kotlin/no/nav/helse/sporbar/UtbetalingUtbetaltRiver.kt#L197-L216

"""
            }
        }
    }
}
