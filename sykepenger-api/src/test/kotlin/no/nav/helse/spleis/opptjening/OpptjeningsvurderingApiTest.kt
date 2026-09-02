package no.nav.helse.spleis.opptjening

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.test_support.TestDataSource
import io.ktor.http.HttpStatusCode
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.april
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.gjenopprettFraJSON
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.PensjonsgivendeInntekt
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.AbstractApiTest
import no.nav.helse.spleis.objectMapper
import no.nav.helse.spleis.testhelpers.PersonHendelsefabrikk
import no.nav.helse.spleis.testhelpers.YrkesaktivitetHendelsefabrikk
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.skyscreamer.jsonassert.JSONAssert

class OpptjeningsvurderingApiTest : AbstractApiTest() {

    companion object {
        private const val FNR = "12029240046"
        private const val ORGNUMMER = "987654322"
    }

    @Test
    fun `hent opptjeningsvurderinger for person som finnes`() = blackboxTestApplication(::opprettArbeidstakerTestdata) {
        "/api/opptjeningsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to FNR)) {
            val actual = assertOgNullifiserSpleisOpptjeningsvurderingIder(this)

            @Language("JSON")
            val expected = """
            {
              "opptjeningsvurderinger": [
                {
                  "opptjeningsvurderingId": "b89e2ae5-59e3-388e-98cd-42a8e7350773",
                  "type": "ARBEIDSTAKER",
                  "skjæringstidspunkt": "2018-01-01",
                  "kilde": "INFOTRYGD"
                },
                {
                  "opptjeningsvurderingId": "00000000-0000-0000-0000-000000000000",
                  "type": "ARBEIDSTAKER",
                  "skjæringstidspunkt": "2018-04-01",
                  "kilde": "SPLEIS",
                  "oppfylt": false,
                  "antallDager": 0,
                  "opptjeningsperiode": null,
                  "arbeidsforhold": []
                },
                {
                  "opptjeningsvurderingId": "00000000-0000-0000-0000-000000000000",
                  "type": "ARBEIDSTAKER",
                  "skjæringstidspunkt": "2018-04-01",
                  "kilde": "SPLEIS",
                  "arbeidsforhold": [
                    {
                      "organisasjonsnummer": "987654322",
                      "ansettelsesperioder": [
                        {
                          "fom": "2017-04-01",
                          "tom": null
                        }
                      ]
                    }
                  ],
                  "opptjeningsperiode": {
                    "fom": "2017-04-01",
                    "tom": "2018-03-31"
                  },
                  "oppfylt": true,
                  "antallDager": 365
                }
              ]
            }
            """
            JSONAssert.assertEquals(expected, actual, true)
        }
    }

    @Test
    fun `hent opptjeningsvurderinger for person som ikke finnes`() = blackboxTestApplication({}) {
        "/api/opptjeningsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to "11111111111")) {
            @Language("JSON")
            val expected = """
            {
              "opptjeningsvurderinger": []
            }
            """
            JSONAssert.assertEquals(expected, this, true)
        }
    }

    @Test
    fun `hent opptjeningsvurderinger for selvstendig som finnes`() = blackboxTestApplication(::opprettSelvstendigTestdata) {
        "/api/opptjeningsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to FNR)) {
            val actual = assertOgNullifiserSpleisOpptjeningsvurderingIder(this)

            @Language("JSON")
            val expected = """
            {
              "opptjeningsvurderinger": [
                {
                  "opptjeningsvurderingId": "00000000-0000-0000-0000-000000000000",
                  "skjæringstidspunkt": "2018-04-01",
                  "kilde": "SPLEIS",
                  "type": "SELVSTENDIG"
                }
              ]
            }
            """
            JSONAssert.assertEquals(expected, actual, true)
        }
    }

    private fun assertOgNullifiserSpleisOpptjeningsvurderingIder(response: String): String {
        val spleisOpptjeningsvurderingIder = mutableListOf<UUID>()
        return objectMapper.readTree(response).apply {
            val forventetAntall = path("opptjeningsvurderinger").count { it.path("kilde").asText() == "SPLEIS" }
            path("opptjeningsvurderinger").forEach { opptjeningsvurdering ->
                if (opptjeningsvurdering.path("kilde").asText() == "INFOTRYGD") return@forEach
                opptjeningsvurdering as ObjectNode
                val opptjeningsvurderingId = assertDoesNotThrow { UUID.fromString(opptjeningsvurdering.path("opptjeningsvurderingId").asText()) }
                spleisOpptjeningsvurderingIder.add(opptjeningsvurderingId)
                opptjeningsvurdering.put("opptjeningsvurderingId", "00000000-0000-0000-0000-000000000000")
            }
            assertEquals(forventetAntall, spleisOpptjeningsvurderingIder.toSet().size)
        }.toString()
    }

    private fun opprettArbeidstakerTestdata(testDataSource: TestDataSource) {
        val eventBus = EventBus()
        val fom = 1.april
        val tom = 30.april

        val person = gjenopprettFraJSON("/personer/infotrygdforlengelse.json", skjemaversjon = 312, regelverkslogg = EmptyLog)

        val fabrikk = YrkesaktivitetHendelsefabrikk(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(ORGNUMMER))

        val søknad = fabrikk.lagSøknad(Søknad.Søknadsperiode.Sykdom(fom = fom, tom = tom, sykmeldingsgrad = 100.prosent), arbeidssituasjon = Søknad.Arbeidssituasjon.ARBEIDSTAKER)
        person.håndterSøknad(eventBus, søknad, Aktivitetslogg())

        val vedtaksperiodeId = eventBus.events.filterIsInstance<EventSubscription.VedtaksperiodeOpprettet>().single().vedtaksperiodeId

        val arbeidsgiveropplysninger = fabrikk.lagArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.april til 16.april),
            beregnetInntekt = 31000.månedlig,
            vedtaksperiodeId = vedtaksperiodeId
        )
        person.håndterArbeidsgiveropplysninger(eventBus, arbeidsgiveropplysninger, Aktivitetslogg())

        val arbeidsgiverinntekt = ArbeidsgiverInntekt(
            arbeidsgiver = ORGNUMMER,
            inntekter = listOf(YearMonth.of(2018, 1), YearMonth.of(2018, 2), YearMonth.of(2018, 3)).map {
                ArbeidsgiverInntekt.MånedligInntekt(
                    yearMonth = it, inntekt = 31000.månedlig, type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, fordel = "a", beskrivelse = "b"
                )
            },
        )

        val vilkårsgrunnlag = fabrikk.lagVilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = 1.april,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(
                    orgnummer = ORGNUMMER,
                    ansettelseperiode = 1.april(2017) til LocalDate.MAX,
                    type = Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(arbeidsgiverinntekt)
            ),
            inntekterForOpptjeningsvurdering = InntekterForOpptjeningsvurdering(listOf(arbeidsgiverinntekt).map {
                it.copy(inntekter = listOf(it.inntekter.last()))
            }),
            forsikringsvurderingId = null,
        )
        person.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag, Aktivitetslogg())

        val overstyrArbeidsforhold = PersonHendelsefabrikk().lagOverstyrArbeidsforhold(
            skjæringstidspunkt = 1.april,
            OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(ORGNUMMER, deaktivert = true, "test")
        )
        person.håndterOverstyrArbeidsforhold(eventBus, overstyrArbeidsforhold, Aktivitetslogg())

        testDataSource.ds.lagrePerson(FNR, person)
    }

    private fun opprettSelvstendigTestdata(testDataSource: TestDataSource) {
        val eventBus = EventBus()
        val aktivitetslogg = Aktivitetslogg()
        val fom = 1.april
        val tom = 30.april

        val person = Person(Personidentifikator(FNR), 1.januar(1990).alder, EmptyLog)

        val fabrikk = YrkesaktivitetHendelsefabrikk(Behandlingsporing.Yrkesaktivitet.Selvstendig)

        val søknad = fabrikk.lagSøknad(
            Søknad.Søknadsperiode.Sykdom(fom = fom, tom = tom, sykmeldingsgrad = 100.prosent),
            arbeidssituasjon = Søknad.Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE,
            pensjonsgivendeInntekter = listOf(
                PensjonsgivendeInntekt(Year.of(2017), 1_000_000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                PensjonsgivendeInntekt(Year.of(2016), 1_000_000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                PensjonsgivendeInntekt(Year.of(2015), 1_000_000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true)
            )
        )
        person.håndterSøknad(eventBus, søknad, aktivitetslogg)

        val vedtaksperiodeId = eventBus.events.filterIsInstance<EventSubscription.VedtaksperiodeOpprettet>().single().vedtaksperiodeId

        person.håndterUtbetalingshistorikk(eventBus, fabrikk.lagUtbetalingshistorikk(vedtaksperiodeId), aktivitetslogg)

        val arbeidsgiverinntekt = ArbeidsgiverInntekt(
            arbeidsgiver = ORGNUMMER,
            inntekter = listOf(YearMonth.of(2018, 1), YearMonth.of(2018, 2), YearMonth.of(2018, 3)).map {
                ArbeidsgiverInntekt.MånedligInntekt(
                    yearMonth = it, inntekt = 31000.månedlig, type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, fordel = "a", beskrivelse = "b"
                )
            },
        )

        val vilkårsgrunnlag = fabrikk.lagVilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = 1.april,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(
                    orgnummer = ORGNUMMER,
                    ansettelseperiode = 1.april(2017) til LocalDate.MAX,
                    type = Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            inntekterForOpptjeningsvurdering = InntekterForOpptjeningsvurdering(emptyList()),
            forsikringsvurderingId = null,
        )
        person.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag, aktivitetslogg)

        testDataSource.ds.lagrePerson(FNR, person)
    }
}
