package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag

@Tag("e2e")
internal abstract class AbstractEndToEndTest : AbstractPersonTest() {

    internal companion object {
        val INNTEKT = 31000.00.månedlig
    }

    internal lateinit var forrigeHendelse: Hendelse
        private set
    internal lateinit var hendelselogg: Aktivitetslogg
        private set

    internal val sykmeldinger = mutableMapOf<UUID, Array<out Sykmeldingsperiode>>()
    internal val søknader = mutableMapOf<UUID, Triple<LocalDate, Boolean, Array<out Søknadsperiode>>>()
    internal val inntektsmeldinger = mutableMapOf<UUID, InnsendtInntektsmelding>()
    internal val inntekter = mutableMapOf<UUID, Inntekt>()

    @BeforeEach
    internal fun abstractSetup() {
        sykmeldinger.clear()
        søknader.clear()
        inntektsmeldinger.clear()
        ikkeBesvarteBehov.clear()
    }

    internal fun <T : Hendelse> T.håndter(håndter: Person.(T, IAktivitetslogg) -> Unit): T {
        hendelselogg = Aktivitetslogg()
        forrigeHendelse = this
        person.håndter(this, hendelselogg)
        ikkeBesvarteBehov += EtterspurtBehov.finnEtterspurteBehov(hendelselogg.behov)
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

    inner class Hendelser(private val hendelser: () -> Unit) {
        infix fun førerTil(postCondition: TilstandType) = førerTil(listOf(postCondition))
        infix fun førerTil(postCondition: List<TilstandType>): Hendelser {
            hendelser()
            postCondition.forEachIndexed { index, tilstand ->
                assertTilstand((index + 1).vedtaksperiode, tilstand)
            }
            return this
        }

        infix fun somEtterfulgtAv(f: () -> Unit) = Hendelser(f)
    }

    fun hendelsene(f: () -> Unit) = Hendelser(f)

    data class InnsendtInntektsmelding(
        val tidspunkt: LocalDateTime,
        val generator: () -> Inntektsmelding,
        val inntektsmeldingkontrakt: no.nav.inntektsmeldingkontrakt.Inntektsmelding
    )
}
