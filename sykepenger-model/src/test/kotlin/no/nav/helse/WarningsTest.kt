package no.nav.helse

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.io.path.extension
import kotlin.io.path.pathString

internal class WarningsTest {
    private val alleFunnedeWarnings by lazy { finnAlleWarnings() }
    private val alleWarnings by lazy { alleFunnedeWarnings.plus(ikkeFunnedeWarnings) }

    @Test
    @Disabled
    fun `print alle warnings`() {
        println("# Alle warnings i Spleis per ${LocalDate.now()} (${alleWarnings.size} stykk)")
        alleWarnings.forEach { println("- $it") }
    }

    @Test
    fun `warnings vi ikke finner`() {
        finnAlleWarnings().intersect(ikkeFunnedeWarnings).let {
            assertEquals(emptySet<String>(), it) {
                "Finner nå $it så kan fjernes fra listen med warnings vi ikke finner"
            }
        }
    }

    @Test
    fun `alle warnings testes eksplisitt`() {
        val ikkeTestedeWarnings = alleWarnings.toMutableSet()
        finnAlleTeksterITester().forEach { tekst ->
            ikkeTestedeWarnings.removeIf { warningEquals(it, tekst) }
        }

        val warningerSomManglerEksplisittTest = setOf(
            "Arena inneholdt en eller flere AAP-perioder med ugyldig fom/tom",
            "Arena inneholdt en eller flere Dagpengeperioder med ugyldig fom/tom",
            "Endrer tidligere oppdrag. Kontroller simuleringen.",
            "Validering av ytelser ved revurdering feilet. Utbetalingen må annulleres",
            "Perioden er lagt inn i Infotrygd, men ikke utbetalt. Fjern fra Infotrygd hvis det utbetales via speil.",
            "Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %%. Vurder å sende vedtaksbrev fra Infotrygd",
            "Vi fant ugyldige arbeidsforhold i Aareg, burde sjekkes opp nærmere",
            "Bruker har mottatt dagpenger innenfor 4 uker før skjæringstidspunktet. Kontroller om bruker er dagpengemottaker. Kombinerte ytelser støttes foreløpig ikke av systemet",
            "Perioden er avslått på grunn av at den sykmeldte ikke er medlem av Folketrygden",
            "Bruker har mottatt AAP innenfor 6 måneder før skjæringstidspunktet. Kontroller at brukeren har rett til sykepenger",
            "Søknaden inneholder permittering. Vurder om permittering har konsekvens for rett til sykepenger",
            "Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd",
            "Sykmeldingen er tilbakedatert, vurder fra og med dato for utbetaling.",
            "Søknaden inneholder egenmeldingsdager etter sykmeldingsperioden",
            "Opptjeningsvurdering må gjøres manuelt fordi opplysningene fra AA-registeret er ufullstendige",
            "Utbetalingen ble gjennomført, men med advarsel: \$melding",
            "Utbetalingen forlenger et tidligere oppdrag som opphørte alle utbetalte dager. Sjekk simuleringen.",
            "Utbetaling fra og med dato er endret. Kontroller simuleringen",
            "Søknaden inneholder Permisjonsdager utenfor sykdomsvindu"
        )

        val nyeWarningerSomManglerEksplisittTest = ikkeTestedeWarnings.minus(warningerSomManglerEksplisittTest)
        val warningerSomNåTestesEkplisitt = warningerSomManglerEksplisittTest.minus(ikkeTestedeWarnings)

        assertForventetFeil(
            forklaring = "Ikke alle warnings testes eksplisitt",
            ønsket = { assertEquals(emptySet<String>(), ikkeTestedeWarnings) },
            nå = { assertEquals(emptySet<String>(), nyeWarningerSomManglerEksplisittTest) {
                "Legg til eksplisitt test for nye warnings! _ikke_ legg den i listen av warnings som mangler eksplisitt test."
            }}
        )

        assertEquals(emptySet<String>(), warningerSomNåTestesEkplisitt) {
            "Finnes nå eksplisitt tester for disse warningene. Fjern dem fra listen over warnings som mangler eksplisitt test."
        }
    }

    private companion object {
        private fun warningEquals(warningDefinisjon: String, warningBruk: String) = when (warningDefinisjon) {
            "Utbetalingen ble gjennomført, men med advarsel: \$melding" ->
                warningBruk.startsWith("Utbetalingen ble gjennomført, men med advarsel: ")
            "Har mer enn %.0f %% avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene." ->
                warningBruk == "Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene."
            else -> warningDefinisjon == warningBruk
        }

        private fun String.inneholderEnAv(vararg innhold: String) = let { string -> innhold.firstOrNull { string.contains(it) } != null }
        private fun Path.slutterPåEnAv(vararg suffix: String) = let { path -> suffix.firstOrNull { path.endsWith(it) } != null }

        private fun finn(
            scope: String,
            regex: Regex,
            ignorePath: (path: Path) -> Boolean = { false },
            ignoreLinje: (linje: String) -> Boolean = { false }) =
            Files.walk(Paths.get("../")).use { paths ->
                paths
                    .filter(Files::isRegularFile)
                    .filter { it.pathString.contains("/src/$scope/") }
                    .filter { it.fileName.extension == "kt" }
                    .filter { !ignorePath(it) }
                    .map { Files.readAllLines(it) }
                    .toList()
                    .asSequence()
                    .flatten()
                    .filterNot(ignoreLinje)
                    .map { linje -> regex.findAll(linje).toList().map { it.groupValues[1] } }
                    .flatten()
                    .toSet()
            }

        /* Warnings */
        private val warningRegex = "warn\\(\"(.*?)\"".toRegex()

        private fun finnNormaleWarnings() = finn("main", warningRegex) {
            " ${it.lowercase()}".inneholderEnAv(" sikkerlogg.", " log.", " logg.", " logger.")
        }

        private fun finnPrefixedWarnings() = finn("main", tekstRegex, ignoreLinje = { linje ->
            !linje.contains("val WARN_")
        })

        private fun finnAlleWarnings() = finnNormaleWarnings().plus(finnPrefixedWarnings())

        // Warnings som ikke blir identifisert hverken på normal måte eller som prefixed med WARN_
        private val ikkeFunnedeWarnings = listOf(
            "Søknaden inneholder Arbeidsdager utenfor sykdomsvindu",
            "Søknaden inneholder Permisjonsdager utenfor sykdomsvindu"
        )

        /* Tekst */
        private val tekstRegex = "\"(.*?)\"".toRegex()

        private fun finnAlleTeksterITester() = finn("test", tekstRegex, ignorePath = { path ->
            path.slutterPåEnAv("${WarningsTest::class.simpleName}.kt", "${this::class.simpleName}.kt")
        })
    }
}