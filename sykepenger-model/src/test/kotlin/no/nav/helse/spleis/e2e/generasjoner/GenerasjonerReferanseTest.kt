package no.nav.helse.spleis.e2e.generasjoner

import java.nio.file.Paths
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectory
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText

@Disabled("kjøres kun ved behov")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GenerasjonerReferanseTest : AbstractDslTest() {
    private companion object {
        private const val testversjon = 274
    }
    private val tester = mutableMapOf<Int, Pair<String, String>>()
    @AfterAll
    fun `skriv tester til disk`() {
        tester.keys.sorted().fold(0) { forrigeNummer, gjeldendeNummer ->
            gjeldendeNummer.also {
                check((forrigeNummer + 1) == gjeldendeNummer) {
                    "Det er problem med rekkefølgen i testene. Test nummer $gjeldendeNummer er mer enn 1 nummer større enn forrige $forrigeNummer"
                }
            }
        }
        val path = Paths.get("${Paths.get("").absolutePathString()}/src/test/resources/migrations/$testversjon")
        if (!path.isDirectory()) path.createDirectory()
        tester.forEach { (testnummer, test) ->
            val (navn, innhold) = test
            val vennlig_filnavn = navn.replace(" ", "_")
            val filnavn = "${testnummer.toString().padStart(2, '0')}__$vennlig_filnavn.json"
            val actual = path.resolve("actual/$filnavn").createParentDirectories().writeText(innhold)
            val expected = path.resolve("expected/$filnavn").createParentDirectories().writeText(innhold)
            println("Skriver $navn til $actual")
        }
    }

    @AfterEach
    fun lagrejson(testInfo: TestInfo) {
        val json = serialize(pretty = true)
        val testnavn = checkNotNull(testInfo.displayName)
        val scenario = "^test (\\d+) - (.+)\\(\\)$".toRegex().matchEntire(testnavn) ?: fail { "ugyldig testnavn! $testnavn" }
        val testnummer = scenario.groupValues[1].toInt()
        val scenarionavn = scenario.groupValues[2]
        check(null == tester.putIfAbsent(testnummer, scenarionavn to json.json)) {
            "to tester har samme nummer: $testnummer. Kunne ikke sette inn $scenarionavn"
        }
    }

    @Test
    fun `test 1 - uberegnet periode`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
    }
    @Test
    fun `test 2 - uberegnet periode med egenmeldingsdager`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent), egenmeldinger = listOf(
                Søknad.Søknadsperiode.Arbeidsgiverdag(1.januar, 2.januar)
            ))
        }
    }
    @Test
    fun `test 3 - auu uten inntektsmelding`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 17.januar, 100.prosent))
        }
    }
    @Test
    fun `test 4 - auu med inntektsmelding`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
            håndterInntektsmelding(listOf(3.januar til 19.januar))
        }
    }
    @Test
    fun `test 5 - avventer godkjenning`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
        }
    }
    @Test
    fun `test 6 - avsluttet periode`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
        }
    }
    @Test
    fun `test 7 - uberegnet revurdering`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        }
    }
    @Test
    fun `test 8 - beregnet periode mange utbetalinger som er forkastet`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Sykedag, 100)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
    }
    @Test
    fun `test 9 - avvist beregnet periode`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
        }
    }
    @Test
    fun `test 10 - avvist beregnet revurdering`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
        }
    }
    @Test
    fun `test 11 - til utbetaling`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true)
        }
    }
    @Test
    fun `test 12 - endring etter til utbetaling`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true)
            håndterSøknad(Sykdom(1.januar, 31.januar, 80.prosent))
        }
    }
    @Test
    fun `test 13 - beregnet revurdering`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 80.prosent))
            håndterYtelser(1.vedtaksperiode)
        }
    }
    @Test
    fun `test 14 - auu etter beregnet utbetaling`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 31.januar))
        }
    }
    @Test
    fun `test 15 - annullering etter utbetalt`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            nyttVedtak(10.februar, 28.februar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
            håndterAnnullering(inspektør.utbetalinger(2.vedtaksperiode).last().inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        }
    }
}