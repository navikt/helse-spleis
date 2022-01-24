package no.nav.helse.serde

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.somFødselsnummer
import no.nav.helse.april
import no.nav.helse.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SerialiseringAvDagerFraSøknadTest {

    @Test
    fun `perioder fra søknaden skal serialiseres og deserialiseres riktig - jackson`() {
        val person = person
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val personDeserialisert = SerialisertPerson(jsonBuilder.toString())
            .deserialize()

        assertJsonEquals(person, personDeserialisert)
    }

    @Test
    fun `perioder fra søknaden skal serialiseres og deserialiseres riktig`() {
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val json = jsonBuilder.toString()

        val result = SerialisertPerson(json).deserialize()
        val jsonBuilder2 = JsonBuilder()
        result.accept(jsonBuilder2)
        val json2 = jsonBuilder2.toString()

        assertEquals(json, json2)
        assertJsonEquals(person, result)
    }

    private val aktørId = "12345"
    private val fnr = "12029240045"
    private val orgnummer = "987654321"

    private lateinit var aktivitetslogg: Aktivitetslogg

    private lateinit var person: Person

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()

        person = Person(aktørId, fnr.somFødselsnummer()).apply {
            håndter(sykmelding)
            håndter(søknad)
        }
    }

    private val sykmelding get() = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = listOf(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent)),
        sykmeldingSkrevet = 4.april.atStartOfDay(),
        mottatt = 4.april.atStartOfDay()
    )

    private val søknad get() = Søknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = orgnummer,
        perioder = listOf(
            Sykdom(1.januar,  2.januar, 100.prosent),
            Egenmelding(2.januar, 2.januar),
            Arbeid(3.januar, 3.januar),
            Ferie(4.januar, 4.januar),
            Permisjon(5.januar, 5.januar),
            Utdanning(5.januar, 5.januar)
        ),
        andreInntektskilder = emptyList(),
        sendtTilNAVEllerArbeidsgiver = 5.januar.atStartOfDay(),
        permittert = false,
        merknaderFraSykmelding = emptyList(),
        sykmeldingSkrevet = LocalDateTime.now()
    )
}

