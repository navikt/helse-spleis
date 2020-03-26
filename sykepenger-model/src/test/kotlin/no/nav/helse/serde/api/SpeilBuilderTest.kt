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
import no.nav.helse.serde.api.SpeilBuilder.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juni
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
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
        val person = person(sendtSøknad = 1.juni)
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

    /**
     * Test for å verifisere at kontrakten mellom Spleis og Speil opprettholdes.
     * Hvis du trenger å gjøre endringer i denne testen må du sannsynligvis også gjøre endringer i Speil.
     */
    @Test
    fun `json-en inneholder de feltene Speil forventer`() {
        val fom = 1.januar
        val tom = 31.januar
        val person = person(fom = fom, tom = tom)
        val (json, _) = serializePersonForSpeil(person)

        assertTrue(json.hasNonNull("aktørId"))
        assertTrue(json.hasNonNull("fødselsnummer"))
        assertTrue(json.hasNonNull("arbeidsgivere"))

        val arbeidsgiver = json["arbeidsgivere"].first();
        assertTrue(arbeidsgiver.hasNonNull("organisasjonsnummer"))
        assertTrue(arbeidsgiver.hasNonNull("id"))
        assertTrue(arbeidsgiver.hasNonNull("vedtaksperioder"))

        val vedtaksperiode = arbeidsgiver["vedtaksperioder"].first();
        assertTrue(vedtaksperiode.hasNonNull("id"))
        assertTrue(vedtaksperiode.hasNonNull("fom"))
        val jsonFom = LocalDate.parse(vedtaksperiode["fom"].asText())
        assertEquals(fom, jsonFom)
        assertTrue(vedtaksperiode.hasNonNull("tom"))
        val jsonTom = LocalDate.parse(vedtaksperiode["tom"].asText())
        assertEquals(tom, jsonTom)
        assertTrue(vedtaksperiode.hasNonNull("maksdato"))
        assertTrue(vedtaksperiode.hasNonNull("forbrukteSykedager"))
        assertTrue(vedtaksperiode.hasNonNull("godkjentAv"))
        assertTrue(vedtaksperiode.hasNonNull("godkjenttidspunkt"))
        assertTrue(vedtaksperiode.hasNonNull("utbetalingsreferanse"))
        assertTrue(vedtaksperiode.hasNonNull("førsteFraværsdag"))
        assertTrue(vedtaksperiode.hasNonNull("inntektFraInntektsmelding"))
        assertTrue(vedtaksperiode.hasNonNull("totalbeløpArbeidstaker"))
        assertTrue(vedtaksperiode.hasNonNull("tilstand"))
        assertTilstand(vedtaksperiode)

        assertTrue(vedtaksperiode.hasNonNull("hendelser"))
        assertTrue(vedtaksperiode.hasNonNull("dataForVilkårsvurdering"))
        assertTrue(vedtaksperiode.hasNonNull("sykdomstidslinje"))
        assertTrue(vedtaksperiode.hasNonNull("utbetalingstidslinje"))
        assertTrue(vedtaksperiode.hasNonNull("utbetalingslinjer"))

        val dataForVilkårsvurdering = vedtaksperiode["dataForVilkårsvurdering"];
        assertTrue(dataForVilkårsvurdering.hasNonNull("erEgenAnsatt"))
        assertTrue(dataForVilkårsvurdering.hasNonNull("beregnetÅrsinntektFraInntektskomponenten"))
        assertTrue(dataForVilkårsvurdering.hasNonNull("avviksprosent"))
        assertTrue(dataForVilkårsvurdering.hasNonNull("antallOpptjeningsdagerErMinst"))
        assertTrue(dataForVilkårsvurdering.hasNonNull("harOpptjening"))

        val sykdomstidslinje = vedtaksperiode["sykdomstidslinje"]
        assertEquals(31, sykdomstidslinje.size())
        sykdomstidslinje.forEach {
            assertTrue(it.hasNonNull("dagen"))
            assertTrue(it.hasNonNull("type"))
            assertTrue(it.hasNonNull("grad"))
        }

        val utbetalingstidslinje = vedtaksperiode["utbetalingstidslinje"];
        assertEquals(31, utbetalingstidslinje.size())
        utbetalingstidslinje.forEach {
            assertTrue(it.hasNonNull("type"))
            assertTrue(it.hasNonNull("inntekt"))
            assertTrue(it.hasNonNull("dato"))
        }
        utbetalingstidslinje.filter { it["type"].asText() == "NavDag" }.forEach {
            assertTrue(it.hasNonNull("utbetaling"))
            assertTrue(it.hasNonNull("grad"))
        }

        val utbetalingslinjer = vedtaksperiode["utbetalingslinjer"];
        assertEquals(1, utbetalingslinjer.size())
        utbetalingslinjer.forEach {
            assertTrue(it.hasNonNull("fom"))
            assertTrue(it.hasNonNull("tom"))
            assertTrue(it.hasNonNull("dagsats"))
            assertTrue(it.hasNonNull("grad"))
        }
    }

    private fun JsonNode.sortedUUIDs() = map(JsonNode::asText).map(UUID::fromString).sorted()

    private fun Person.collectVedtaksperiodeIder() = mutableSetOf<String>().apply {
        accept(object : PersonVisitor {
            override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
                add(id.toString())
            }
        })
    }

    private fun assertTilstand(jsonNode: JsonNode) =
        assertDoesNotThrow { TilstandstypeDTO.valueOf(jsonNode["tilstand"].asText()) }
}

