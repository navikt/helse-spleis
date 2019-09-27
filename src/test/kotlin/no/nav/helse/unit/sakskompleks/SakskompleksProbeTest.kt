package no.nav.helse.unit.sakskompleks

import io.prometheus.client.CollectorRegistry
import no.nav.helse.Event
import no.nav.helse.sakskompleks.SakskompleksProbe
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.sakskompleks.domain.SakskompleksObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class SakskompleksProbeTest {

    companion object {
        private val id = UUID.randomUUID()
        private val aktørId = "123456789123"

        private val probe = SakskompleksProbe()
    }

    @Test
    fun `teller nye sakskompleks`() {
        val sakskompleksCounterBefore = getCounterValue(SakskompleksProbe.sakskompleksTotalsCounterName)
        val sykmeldingerCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Event.Type.Sykmelding.name))

        probe.sakskompleksChanged(changeEvent("SykmeldingMottattTilstand", "StartTilstand", Event.Type.Sykmelding))

        val sakskompleksCounterAfter = getCounterValue(SakskompleksProbe.sakskompleksTotalsCounterName)
        val sykmeldingerCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Event.Type.Sykmelding.name))

        assertCounter(sakskompleksCounterAfter, sakskompleksCounterBefore)
        assertCounter(sykmeldingerCounterAfter, sykmeldingerCounterBefore)
    }

    @Test
    fun `teller nye sykmeldinger`() {
        val sykmeldingerCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Event.Type.Sykmelding.name))

        probe.sakskompleksChanged(changeEvent("SykmeldingMottattTilstand", "SykmeldingMottattTilstand", Event.Type.Sykmelding))

        val sykmeldingerCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Event.Type.Sykmelding.name))

        assertCounter(sykmeldingerCounterAfter, sykmeldingerCounterBefore)
    }

    @Test
    fun `teller nye søknader`() {
        val søknadCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Event.Type.Sykepengesøknad.name))

        probe.sakskompleksChanged(changeEvent("SøknadMottattTilstand", "SykmeldingMottattTilstand", Event.Type.Sykepengesøknad))

        val søknadCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Event.Type.Sykepengesøknad.name))

        assertCounter(søknadCounterAfter, søknadCounterBefore)
    }

    @Test
    fun `teller nye inntektsmeldinger`() {
        val inntektsmeldingCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Event.Type.Inntektsmelding.name))

        probe.sakskompleksChanged(changeEvent("KomplettSakTilstand", "SøknadMottattTilstand", Event.Type.Inntektsmelding))

        val inntektsmeldingCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Event.Type.Inntektsmelding.name))

        assertCounter(inntektsmeldingCounterAfter, inntektsmeldingCounterBefore)
    }

    private fun assertCounter(after: Int, before: Int) =
            assertEquals(1, after - before)

    private fun sakskompleks() =
            Sakskompleks(
                    id = id,
                    aktørId = aktørId
            )

    private fun changeEvent(currentState: String, previousState: String, eventName: Event.Type) =
            sakskompleks().let { sakskompleks ->
                SakskompleksObserver.StateChangeEvent(
                        id = id,
                        aktørId = aktørId,
                        currentState = currentState,
                        previousState = previousState,
                        eventName = eventName,
                        currentMemento = sakskompleks.memento(),
                        previousMemento = sakskompleks.memento()
                )
            }

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
