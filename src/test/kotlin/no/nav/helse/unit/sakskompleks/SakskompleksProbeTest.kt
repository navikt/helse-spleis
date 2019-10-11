package no.nav.helse.unit.sakskompleks

import io.prometheus.client.CollectorRegistry
import no.nav.helse.TestConstants.inntektsmelding
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.TestConstants.sendtSøknad
import no.nav.helse.hendelse.Sykdomshendelse
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.SakskompleksObserver
import no.nav.helse.sakskompleks.SakskompleksProbe
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
        val sykmeldingerCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Sykdomshendelse.Type.NySykepengesøknad.name))

        probe.sakskompleksChanged(changeEvent(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, Sakskompleks.TilstandType.START, nySøknad()))

        val sakskompleksCounterAfter = getCounterValue(SakskompleksProbe.sakskompleksTotalsCounterName)
        val sykmeldingerCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Sykdomshendelse.Type.NySykepengesøknad.name))

        assertCounter(sakskompleksCounterAfter, sakskompleksCounterBefore)
        assertCounter(sykmeldingerCounterAfter, sykmeldingerCounterBefore)
    }

    @Test
    fun `teller nye sykmeldinger`() {
        val sykmeldingerCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Sykdomshendelse.Type.NySykepengesøknad.name))

        probe.sakskompleksChanged(changeEvent(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, nySøknad()))

        val sykmeldingerCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Sykdomshendelse.Type.NySykepengesøknad.name))

        assertCounter(sykmeldingerCounterAfter, sykmeldingerCounterBefore)
    }

    @Test
    fun `teller nye søknader`() {
        val søknadCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Sykdomshendelse.Type.SendtSykepengesøknad.name))

        probe.sakskompleksChanged(changeEvent(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, sendtSøknad()))

        val søknadCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Sykdomshendelse.Type.SendtSykepengesøknad.name))

        assertCounter(søknadCounterAfter, søknadCounterBefore)
    }

    @Test
    fun `teller nye inntektsmeldinger`() {
        val inntektsmeldingCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Sykdomshendelse.Type.Inntektsmelding.name))

        probe.sakskompleksChanged(changeEvent(Sakskompleks.TilstandType.KOMPLETT_SAK, Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, inntektsmelding()))

        val inntektsmeldingCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(Sykdomshendelse.Type.Inntektsmelding.name))

        assertCounter(inntektsmeldingCounterAfter, inntektsmeldingCounterBefore)
    }

    private fun assertCounter(after: Int, before: Int) =
        assertEquals(1, after - before)

    private fun sakskompleks() =
        Sakskompleks(
            id = id,
            aktørId = aktørId,
            organisasjonsnummer = "orgnummer"
        )

    private fun changeEvent(currentState: Sakskompleks.TilstandType, previousState: Sakskompleks.TilstandType, eventType: Sykdomshendelse) =
        sakskompleks().let { sakskompleks ->
            SakskompleksObserver.StateChangeEvent(
                id = id,
                aktørId = aktørId,
                currentState = currentState,
                previousState = previousState,
                sykdomshendelse = eventType,
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
