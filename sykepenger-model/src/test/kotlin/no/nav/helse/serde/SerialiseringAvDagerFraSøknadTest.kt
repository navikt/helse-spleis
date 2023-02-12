package no.nav.helse.serde

import java.time.LocalDate.EPOCH
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utdanning
import no.nav.helse.januar
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.somPersonidentifikator
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SerialiseringAvDagerFraSøknadTest {

    @Test
    fun `perioder fra søknaden skal serialiseres og deserialiseres riktig - jackson`() {
        val person = person
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val personDeserialisert = SerialisertPerson(jsonBuilder.toString())
            .deserialize(MaskinellJurist())

        assertJsonEquals(person, personDeserialisert)
    }

    @Test
    fun `perioder fra søknaden skal serialiseres og deserialiseres riktig`() {
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val json = jsonBuilder.toString()

        val result = SerialisertPerson(json).deserialize(MaskinellJurist())
        val jsonBuilder2 = JsonBuilder()
        result.accept(jsonBuilder2)
        val json2 = jsonBuilder2.toString()

        assertEquals(json, json2)
        assertJsonEquals(person, result)
    }

    private val aktørId = "12345"
    private val fnr = "12029240045"
    private val orgnummer = "987654321"
    private val hendelsefabrikk = ArbeidsgiverHendelsefabrikk(
        aktørId = aktørId,
        personidentifikator = fnr.somPersonidentifikator(),
        organisasjonsnummer = orgnummer
    )

    private lateinit var aktivitetslogg: Aktivitetslogg

    private lateinit var person: Person

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()

        person = Person(aktørId, fnr.somPersonidentifikator(), EPOCH.alder, MaskinellJurist()).apply {
            håndter(sykmelding)
            håndter(søknad)
        }
    }

    private val sykmelding get() = hendelsefabrikk.lagSykmelding(
        sykeperioder = arrayOf(Sykmeldingsperiode(1.januar, 2.januar))
    )

    private val søknad get() = hendelsefabrikk.lagSøknad(
        perioder = arrayOf(
            Sykdom(1.januar, 5.januar, 100.prosent),
            Arbeid(3.januar, 3.januar),
            Ferie(4.januar, 4.januar),
            Permisjon(5.januar, 5.januar),
            Utdanning(5.januar, 5.januar)
        ),
        sendtTilNAVEllerArbeidsgiver = 5.januar
    )
}

