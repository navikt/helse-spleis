package no.nav.helse.unit.sakskompleks

import io.prometheus.client.CollectorRegistry
import no.nav.helse.SykdomshendelseType
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.SakskompleksObserver
import no.nav.helse.sakskompleks.SakskompleksProbe
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
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
        val sykmeldingerCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(SykdomshendelseType.NySøknadMottatt.name))

        probe.sakskompleksEndret(changeEvent(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, Sakskompleks.TilstandType.START, nySøknadHendelse()))

        val sakskompleksCounterAfter = getCounterValue(SakskompleksProbe.sakskompleksTotalsCounterName)
        val sykmeldingerCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(SykdomshendelseType.NySøknadMottatt.name))

        assertCounter(sakskompleksCounterAfter, sakskompleksCounterBefore)
        assertCounter(sykmeldingerCounterAfter, sykmeldingerCounterBefore)
    }

    @Test
    fun `teller nye sykmeldinger`() {
        val sykmeldingerCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(SykdomshendelseType.NySøknadMottatt.name))

        probe.sakskompleksEndret(changeEvent(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, nySøknadHendelse()))

        val sykmeldingerCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(SykdomshendelseType.NySøknadMottatt.name))

        assertCounter(sykmeldingerCounterAfter, sykmeldingerCounterBefore)
    }

    @Test
    fun `teller nye søknader`() {
        val søknadCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(SykdomshendelseType.SendtSøknadMottatt.name))

        probe.sakskompleksEndret(changeEvent(Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, sendtSøknadHendelse()))

        val søknadCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(SykdomshendelseType.SendtSøknadMottatt.name))

        assertCounter(søknadCounterAfter, søknadCounterBefore)
    }

    @Test
    fun `teller nye inntektsmeldinger`() {
        val inntektsmeldingCounterBefore = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(SykdomshendelseType.InntektsmeldingMottatt.name))

        probe.sakskompleksEndret(changeEvent(Sakskompleks.TilstandType.KOMPLETT_SAK, Sakskompleks.TilstandType.SENDT_SØKNAD_MOTTATT, inntektsmeldingHendelse()))

        val inntektsmeldingCounterAfter = getCounterValue(SakskompleksProbe.dokumenterKobletTilSakCounterName, listOf(SykdomshendelseType.InntektsmeldingMottatt.name))

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

    private fun changeEvent(currentState: Sakskompleks.TilstandType, previousState: Sakskompleks.TilstandType, eventType: SykdomstidslinjeHendelse) =
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
