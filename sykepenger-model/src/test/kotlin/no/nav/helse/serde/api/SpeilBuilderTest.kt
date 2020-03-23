package no.nav.helse.serde.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.JsonBuilderTest.Companion.ingenBetalingsperson
import no.nav.helse.serde.JsonBuilderTest.Companion.inntektsmelding
import no.nav.helse.serde.JsonBuilderTest.Companion.manuellSaksbehandling
import no.nav.helse.serde.JsonBuilderTest.Companion.person
import no.nav.helse.serde.JsonBuilderTest.Companion.sykmelding
import no.nav.helse.serde.JsonBuilderTest.Companion.søknad
import no.nav.helse.serde.JsonBuilderTest.Companion.vilkårsgrunnlag
import no.nav.helse.serde.JsonBuilderTest.Companion.ytelser
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*


internal class SpeilBuilderTest {
    private val aktørId = "1234"
    private val fnr = "5678"

    @Test
    internal fun `dager før førsteFraværsdag og etter sisteSykedag skal kuttes vekk fra utbetalingstidslinje`() {
        val person = person()
        val jsonBuilder = SpeilBuilder()
        person.accept(jsonBuilder)

        val json = jacksonObjectMapper().readTree(jsonBuilder.toString())
        assertEquals(
            1.januar,
            LocalDate.parse(json["arbeidsgivere"][0]["vedtaksperioder"][0]["utbetalingstidslinje"].first()["dato"].asText())
        )
        assertEquals(
            31.januar,
            LocalDate.parse(json["arbeidsgivere"][0]["vedtaksperioder"][0]["utbetalingstidslinje"].last()["dato"].asText())
        )
    }

    @Test
    internal fun `person uten utbetalingsdager`() {
        val person = ingenBetalingsperson()
        val jsonBuilder = SpeilBuilder()
        person.accept(jsonBuilder)

        val json = jacksonObjectMapper().readTree(jsonBuilder.toString())
        assertEquals(
            1.januar,
            LocalDate.parse(json["arbeidsgivere"][0]["vedtaksperioder"][0]["utbetalingstidslinje"].first()["dato"].asText())
        )
        assertEquals(
            9.januar,
            LocalDate.parse(json["arbeidsgivere"][0]["vedtaksperioder"][0]["utbetalingstidslinje"].last()["dato"].asText())
        )
    }

    @Test
    internal fun `person med foreldet dager`() {
        val person = person(1.juni)
        val jsonBuilder = SpeilBuilder()
        person.accept(jsonBuilder)

        val json = jacksonObjectMapper().readTree(jsonBuilder.toString())
        assertEquals(1, json["arbeidsgivere"][0]["vedtaksperioder"].size())
        val utbetalingstidslinje = json["arbeidsgivere"][0]["vedtaksperioder"][0]["utbetalingstidslinje"]
        assertEquals("ArbeidsgiverperiodeDag", utbetalingstidslinje.first()["type"].asText())
        assertEquals("ArbeidsgiverperiodeDag", utbetalingstidslinje[15]["type"].asText())
        assertEquals("ForeldetDag", utbetalingstidslinje[16]["type"].asText())
        assertEquals("ForeldetDag", utbetalingstidslinje.last()["type"].asText())
    }

    @Test
    fun `tom utbetalingstidslinje hvis kun sykmelding mottatt`() {
        val person = Person(aktørId, fnr).apply {
            håndter(sykmelding(hendelseId = UUID.randomUUID(), fom = 1.januar, tom = 31.januar))
        }
        val (json, _) = serializePersonForSpeil(person)

        val arbeidsgiver = json["arbeidsgivere"][0]
        val vedtaksperioder = arbeidsgiver["vedtaksperioder"]

        assertEquals(0, vedtaksperioder.first()["utbetalingstidslinje"].size())
    }

    @Test
    fun `passer på at vedtakene har en referanse til hendelsene`() {
        var vedtaksperiodeIder: Set<String>
        val hendelseIderVedtak1 = (0.until(3)).map { UUID.randomUUID() }
        val hendelseIderVedtak2 = (0.until(3)).map { UUID.randomUUID() }

        val person = Person(aktørId, fnr).apply {

            håndter(sykmelding(hendelseId = hendelseIderVedtak1[0], fom = 1.januar, tom = 31.januar))
            håndter(søknad(hendelseId = hendelseIderVedtak1[1], fom = 1.januar, tom = 31.januar))
            håndter(inntektsmelding(hendelseId = hendelseIderVedtak1[2], fom = 1.januar))

            vedtaksperiodeIder = collectVedtaksperiodeIder()

            håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeIder.last()))

            håndter(sykmelding(hendelseId = hendelseIderVedtak2[0], fom = 1.februar, tom = 14.februar))
            håndter(søknad(hendelseId = hendelseIderVedtak2[1], fom = 1.februar, tom = 14.februar))
            håndter(inntektsmelding(hendelseId = hendelseIderVedtak2[2], fom = 1.februar))

            vedtaksperiodeIder = collectVedtaksperiodeIder()

            håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeIder.last()))
        }

        val (json, _) = serializePersonForSpeil(person)

        val vedtaksperioder = json["arbeidsgivere"][0]["vedtaksperioder"]

        assertEquals(2, vedtaksperioder.size())
        assertEquals(vedtaksperioder[0]["hendelser"].sortedUUIDs(), hendelseIderVedtak1.sorted())
        assertEquals(vedtaksperioder[1]["hendelser"].sortedUUIDs(), hendelseIderVedtak2.sorted())
    }

    @Test
    fun `json-en inneholder de feltene Speil forventer`() {
        val person = person()
        val (json, _) = serializePersonForSpeil(person)

        assertTrue(json.hasNonNull("aktørId"))
        assertTrue(json.hasNonNull("fødselsnummer"))
        assertTrue(json.hasNonNull("arbeidsgivere"))

        assertTrue(json["arbeidsgivere"].first().hasNonNull("organisasjonsnummer"))
    }

    private fun JsonNode.sortedUUIDs() = map(JsonNode::asText).map(UUID::fromString).sorted()

    private fun Person.collectVedtaksperiodeIder() = mutableSetOf<String>().apply {
        accept(object : PersonVisitor {
            override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
                add(id.toString())
            }
        })
    }
}

