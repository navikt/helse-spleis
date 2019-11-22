package no.nav.helse.unit.spleis

import io.prometheus.client.CollectorRegistry
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.hendelser.SykdomshendelseType
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.Vedtaksperiode
import no.nav.helse.sak.VedtaksperiodeObserver
import no.nav.helse.spleis.VedtaksperiodeProbe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class VedtaksperiodeProbeTest {

    private companion object {
        private val id = UUID.randomUUID()
        private val aktørId = "123456789123"

        private val probe = VedtaksperiodeProbe
    }

    @Test
    fun `teller nye sykmeldinger`() {
        val sykmeldingerCounterBefore = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.NySøknadMottatt.name))

        probe.vedtaksperiodeEndret(changeEvent(Vedtaksperiode.TilstandType.NY_SØKNAD_MOTTATT, Vedtaksperiode.TilstandType.NY_SØKNAD_MOTTATT, nySøknadHendelse()))

        val sykmeldingerCounterAfter = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.NySøknadMottatt.name))

        assertCounter(sykmeldingerCounterAfter, sykmeldingerCounterBefore)
    }

    @Test
    fun `teller nye søknader`() {
        val søknadCounterBefore = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.SendtSøknadMottatt.name))

        probe.vedtaksperiodeEndret(changeEvent(Vedtaksperiode.TilstandType.SENDT_SØKNAD_MOTTATT, Vedtaksperiode.TilstandType.NY_SØKNAD_MOTTATT, sendtSøknadHendelse()))

        val søknadCounterAfter = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.SendtSøknadMottatt.name))

        assertCounter(søknadCounterAfter, søknadCounterBefore)
    }

    @Test
    fun `teller nye inntektsmeldinger`() {
        val inntektsmeldingCounterBefore = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.InntektsmeldingMottatt.name))

        probe.vedtaksperiodeEndret(changeEvent(Vedtaksperiode.TilstandType.KOMPLETT_SYKDOMSTIDSLINJE, Vedtaksperiode.TilstandType.SENDT_SØKNAD_MOTTATT, inntektsmeldingHendelse()))

        val inntektsmeldingCounterAfter = getCounterValue("dokumenter_koblet_til_sak_totals", listOf(SykdomshendelseType.InntektsmeldingMottatt.name))

        assertCounter(inntektsmeldingCounterAfter, inntektsmeldingCounterBefore)
    }

    private fun assertCounter(after: Int, before: Int) =
        assertEquals(1, after - before)

    private fun changeEvent(currentState: Vedtaksperiode.TilstandType, previousState: Vedtaksperiode.TilstandType, eventType: ArbeidstakerHendelse) =
        VedtaksperiodeObserver.StateChangeEvent(
            id = id,
            aktørId = aktørId,
            organisasjonsnummer = "orgnummer",
            currentState = currentState,
            previousState = previousState,
            sykdomshendelse = eventType
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
