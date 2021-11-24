package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.*

internal abstract class AbstractEndToEndTest : AbstractPersonTest() {

    internal companion object {
        val INNTEKT = 31000.00.månedlig
        val DAGSINNTEKT = INNTEKT.reflection { _, _, _, dagligInt -> dagligInt }
        val MÅNEDLIG_INNTEKT = INNTEKT.reflection { _, månedlig, _, _ -> månedlig.toInt() }
    }

    internal lateinit var hendelselogg: IAktivitetslogg
    internal var forventetEndringTeller = 0
    internal val sykmeldinger = mutableMapOf<UUID, Array<out Sykmeldingsperiode>>()
    internal val søknader = mutableMapOf<UUID, Triple<LocalDate, List<Søknad.Inntektskilde>, Array<out Søknad.Søknadsperiode>>>()
    internal val inntektsmeldinger = mutableMapOf<UUID, () -> Inntektsmelding>()

    @BeforeEach
    internal fun abstractSetup() {
        sykmeldinger.clear()
        søknader.clear()
        inntektsmeldinger.clear()
        ikkeBesvarteBehov.clear()
    }

    internal fun <T : PersonHendelse> T.håndter(håndter: Person.(T) -> Unit): T {
        hendelselogg = this
        person.håndter(this)
        ikkeBesvarteBehov += EtterspurtBehov.finnEtterspurteBehov(behov())
        return this
    }

    internal val ikkeBesvarteBehov = mutableListOf<EtterspurtBehov>()

    internal fun TestArbeidsgiverInspektør.assertTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType) {
        assertTilstander(
            vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
            tilstander = tilstander,
            orgnummer = arbeidsgiver.organisasjonsnummer(),
            inspektør = this
        )
    }

    internal fun TestArbeidsgiverInspektør.assertHasNoErrors() = assertNoErrors(this)
}
