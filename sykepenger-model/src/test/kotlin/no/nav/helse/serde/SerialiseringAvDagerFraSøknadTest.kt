package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SerialiseringAvDagerFraSøknadTest {

    private val objectMapper = jacksonObjectMapper()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .setMixIns(
            mutableMapOf(
                Arbeidsgiver::class.java to ArbeidsgiverMixin::class.java,
                Vedtaksperiode::class.java to VedtaksperiodeMixin::class.java
            )
        )
        .registerModule(JavaTimeModule())

    @JsonIgnoreProperties("person")
    private class ArbeidsgiverMixin

    @JsonIgnoreProperties("person", "arbeidsgiver", "kontekst")
    private class VedtaksperiodeMixin

    @Test
    fun `perioder fra søknaden skal serialiseres og deserialiseres riktig - jackson`() {
        val person = person
        val personPre = objectMapper.writeValueAsString(person)
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val personDeserialisert = SerialisertPerson(jsonBuilder.toString())
            .deserialize()
        val personPost = objectMapper.writeValueAsString(personDeserialisert)

        assertEquals(personPre, personPost)
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
        assertDeepEquals(person, result)
    }

    private val aktørId = "12345"
    private val fnr = "12020052345"
    private val orgnummer = "987654321"

    private lateinit var aktivitetslogg: Aktivitetslogg

    private lateinit var person: Person

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()

        person = Person(aktørId, fnr).apply {
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
            Søknad.Søknadsperiode.Sykdom(1.januar,  2.januar, 100.prosent),
            Søknad.Søknadsperiode.Egenmelding(2.januar, 2.januar),
            Søknad.Søknadsperiode.Arbeid(3.januar, 3.januar),
            Søknad.Søknadsperiode.Ferie(4.januar, 4.januar),
            Søknad.Søknadsperiode.Permisjon(5.januar, 5.januar),
            Søknad.Søknadsperiode.Utdanning(5.januar, 5.januar)
        ),
        andreInntektskilder = emptyList(),
        sendtTilNAV = 5.januar.atStartOfDay(),
        permittert = false,
        merknaderFraSykmelding = emptyList(),
        sykmeldingSkrevet = LocalDateTime.now()
    )
}

