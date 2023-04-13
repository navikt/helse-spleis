package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Person
import no.nav.helse.hendelser.PersonHendelse
import no.nav.helse.person.TilstandType
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.BeforeEach

internal abstract class AbstractEndToEndTest : AbstractPersonTest() {

    internal companion object {
        val INNTEKT = 31000.00.månedlig
        val DAGSINNTEKT = INNTEKT.reflection { _, _, _, dagligInt -> dagligInt }
        val MÅNEDLIG_INNTEKT = INNTEKT.reflection { _, månedlig, _, _ -> månedlig.toInt() }
        val ÅRLIG_INNTEKT = INNTEKT.reflection { årlig, _, _, _ -> årlig.toInt() }
    }

    internal lateinit var hendelselogg: PersonHendelse
    internal val sykmeldinger = mutableMapOf<UUID, Array<out Sykmeldingsperiode>>()
    internal val søknader = mutableMapOf<UUID, Triple<LocalDate, Boolean, Array<out Søknadsperiode>>>()
    internal val inntektsmeldinger = mutableMapOf<UUID, Pair<LocalDateTime, () -> Inntektsmelding>>()
    internal val inntekter = mutableMapOf<UUID, Inntekt>()

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

    inner class Hendelser(private val hendelser:()->Unit) {
        infix fun førerTil(postCondition: TilstandType) = førerTil(listOf(postCondition))
        infix fun førerTil(postCondition: List<TilstandType>):Hendelser {
            hendelser()
            postCondition.forEachIndexed { index, tilstand ->
                assertTilstand((index+1).vedtaksperiode, tilstand)
            }
            return this
        }
        infix fun somEtterfulgtAv(f: ()->Unit) = Hendelser(f)
    }
    fun hendelsene(f:()->Unit) = Hendelser(f)
}
