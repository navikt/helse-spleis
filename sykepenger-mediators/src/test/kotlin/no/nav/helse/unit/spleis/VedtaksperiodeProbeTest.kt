package no.nav.helse.unit.spleis

import io.prometheus.client.CollectorRegistry
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.hendelser.SykdomshendelseType
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.TilstandType
import no.nav.helse.sak.VedtaksperiodeObserver
import no.nav.helse.spleis.VedtaksperiodeProbe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

internal class VedtaksperiodeProbeTest {

    private companion object {
        private val id = UUID.randomUUID()
        private val aktørId = "123456789123"
        private val fødselsnummer = "5678"

        private val probe = VedtaksperiodeProbe
    }

    @Test
    fun `teller nye sykmeldinger`() {
        val sykmeldingerCounterBefore = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.NySøknadMottatt.name))

        probe.vedtaksperiodeEndret(changeEvent(TilstandType.NY_SØKNAD_MOTTATT, TilstandType.NY_SØKNAD_MOTTATT, nySøknadHendelse()))

        val sykmeldingerCounterAfter = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.NySøknadMottatt.name))

        assertCounter(sykmeldingerCounterAfter, sykmeldingerCounterBefore)
    }

    @Test
    fun `teller nye søknader`() {
        val søknadCounterBefore = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.SendtSøknadMottatt.name))

        probe.vedtaksperiodeEndret(changeEvent(TilstandType.SENDT_SØKNAD_MOTTATT, TilstandType.NY_SØKNAD_MOTTATT, sendtSøknadHendelse()))

        val søknadCounterAfter = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.SendtSøknadMottatt.name))

        assertCounter(søknadCounterAfter, søknadCounterBefore)
    }

    @Test
    fun `teller nye inntektsmeldinger`() {
        val inntektsmeldingCounterBefore = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.InntektsmeldingMottatt.name))

        probe.vedtaksperiodeEndret(changeEvent(TilstandType.KOMPLETT_SYKDOMSTIDSLINJE, TilstandType.SENDT_SØKNAD_MOTTATT, inntektsmeldingHendelse()))

        val inntektsmeldingCounterAfter = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.InntektsmeldingMottatt.name))

        assertCounter(inntektsmeldingCounterAfter, inntektsmeldingCounterBefore)
    }

    private fun assertCounter(after: Int, before: Int) =
        assertEquals(1, after - before)

    private fun changeEvent(currentState: TilstandType, previousState: TilstandType, eventType: ArbeidstakerHendelse) =
        VedtaksperiodeObserver.StateChangeEvent(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = "orgnummer",
            currentState = currentState,
            previousState = previousState,
            sykdomshendelse = eventType,
            timeout = Duration.ZERO
        )

    private fun getCounterValue(name: String, labelValues: List<String> = emptyList()) =
        (CollectorRegistry.defaultRegistry
            .findMetricSample(name, labelValues)
            ?.value ?: 0.0).toInt()

    private fun CollectorRegistry.findMetricSample(name: String, labelValues: List<String>) =
        findSamples(name).firstOrNull { sample ->
            sample.labelValues.size == labelValues.size && sample.labelValues.containsAll(labelValues)
        }

    private fun CollectorRegistry.findSamples(name: String) =
        filteredMetricFamilySamples(setOf(name))
            .toList()
            .flatMap { metricFamily ->
                metricFamily.samples
            }
}
