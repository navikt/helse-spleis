package no.nav.helse.spleis.medlemskap

import io.ktor.http.HttpStatusCode
import no.nav.helse.april
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.gjenopprettFraJSON
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering.Medlemskapstatus
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.nyttFødselsnummer
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.AbstractApiTest
import no.nav.helse.spleis.objectMapper
import no.nav.helse.spleis.testhelpers.PersonHendelsefabrikk
import no.nav.helse.spleis.testhelpers.YrkesaktivitetHendelsefabrikk
import no.nav.helse.testdatabase.TestDataSource
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.skyscreamer.jsonassert.JSONAssert
import tools.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals

class MedlemskapsvurderingApiTest : AbstractApiTest() {
    // unikt per testmetode (ny instans per @Test) — deles av `sendt.person`/`person_alias`, som har `fnr` som unik kolonne i den delte databasen
    private val FNR = nyttFødselsnummer()

    companion object {
        private const val ORGNUMMER = "987654322"
    }

    @Test
    fun `hent medlemskapsvurderinger for person som finnes`() =
        blackboxTestApplication({ opprettTestdata(it, Medlemskapstatus.Ja) }) {
            "/api/medlemskapsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to FNR)) {
                val actual = assertOgNullifiserSpleisMedlemskapsvurderingIder(this)

                @Language("JSON")
                val expected = """
            {
              "medlemskapsvurderinger": [
                {
                  "medlemskapsvurderingId": "135757db-11b5-33af-841d-db2a75e8a838",
                  "opprettet": "2022-08-31T09:52:08.494020",
                  "skjæringstidspunkt": "2018-01-01",
                  "kilde": "INFOTRYGD"
                },
                {
                  "medlemskapsvurderingId": "00000000-0000-0000-0000-000000000000",
                  "opprettet": "<opprettet>",
                  "skjæringstidspunkt": "2018-04-01",
                  "kilde": "SPLEIS",
                  "medlemskapstatus": "JA"
                }
              ]
            }
            """
                JSONAssert.assertEquals(expected, actual, true)
            }
        }

    @Test
    fun `mapper medlemskapstatus fra Spleis`() {
        listOf(
            Medlemskapstatus.Nei to "NEI",
            Medlemskapstatus.VetIkke to "VET_IKKE",
            Medlemskapstatus.UavklartMedBrukerspørsmål to "UAVKLART_MED_BRUKERSPORSMAAL",
        ).forEach { (medlemskapstatus, forventet) ->
            val fnr = nyttFødselsnummer()
            blackboxTestApplication({ opprettTestdata(it, medlemskapstatus, fnr) }) {
                "/api/medlemskapsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to fnr)) {
                    val spleis =
                        objectMapper
                            .readTree(this)
                            .path("medlemskapsvurderinger")
                            .single { it.path("kilde").asText() == "SPLEIS" }
                    assertEquals(forventet, spleis.path("medlemskapstatus").asText())
                }
            }
        }
    }

    @Test
    fun `hent medlemskapsvurderinger for person som ikke finnes`() =
        blackboxTestApplication({}) {
            "/api/medlemskapsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to "11111111111")) {
                @Language("JSON")
                val expected = """
            {
              "medlemskapsvurderinger": []
            }
            """
                JSONAssert.assertEquals(expected, this, true)
            }
        }

    private fun assertOgNullifiserSpleisMedlemskapsvurderingIder(response: String): String {
        val spleisMedlemskapsvurderingIder = mutableListOf<UUID>()
        return objectMapper
            .readTree(response)
            .apply {
                path("medlemskapsvurderinger").forEach { medlemskapsvurdering ->
                    medlemskapsvurdering as ObjectNode
                    if (medlemskapsvurdering.path("kilde").asText() == "INFOTRYGD") return@forEach
                    val medlemskapsvurderingId =
                        assertDoesNotThrow { UUID.fromString(medlemskapsvurdering.path("medlemskapsvurderingId").asText()) }
                    spleisMedlemskapsvurderingIder.add(medlemskapsvurderingId)
                    medlemskapsvurdering.put("medlemskapsvurderingId", "00000000-0000-0000-0000-000000000000")
                    assertDoesNotThrow { LocalDateTime.parse(medlemskapsvurdering.path("opprettet").asText()) }
                    medlemskapsvurdering.put("opprettet", "<opprettet>")
                }
                assertEquals(1, spleisMedlemskapsvurderingIder.size)
            }.toString()
    }

    // Overstyring av arbeidsforhold kopierer grunnlaget med samme medlemskapsvurderingId, så det blir bare én vurdering fra Spleis
    private fun opprettTestdata(
        testDataSource: TestDataSource,
        medlemskapstatus: Medlemskapstatus,
        fnr: String = FNR,
    ) {
        val eventBus = EventBus()
        val fom = 1.april
        val tom = 30.april

        val person = gjenopprettFraJSON("/personer/infotrygdforlengelse.json", skjemaversjon = 312, regelverkslogg = EmptyLog)

        val fabrikk = YrkesaktivitetHendelsefabrikk(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(ORGNUMMER))

        val søknad =
            fabrikk.lagSøknad(
                Søknad.Søknadsperiode.Sykdom(fom = fom, tom = tom, sykmeldingsgrad = 100.prosent),
                arbeidssituasjon = Søknad.Arbeidssituasjon.ARBEIDSTAKER,
            )
        person.håndterSøknad(eventBus, søknad, Aktivitetslogg())

        val vedtaksperiodeId =
            eventBus.events
                .filterIsInstance<EventSubscription.VedtaksperiodeOpprettet>()
                .single()
                .vedtaksperiodeId

        val arbeidsgiveropplysninger =
            fabrikk.lagArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.april til 16.april),
                beregnetInntekt = 31000.månedlig,
                vedtaksperiodeId = vedtaksperiodeId,
            )
        person.håndterArbeidsgiveropplysninger(eventBus, arbeidsgiveropplysninger, Aktivitetslogg())

        val arbeidsgiverinntekt =
            ArbeidsgiverInntekt(
                arbeidsgiver = ORGNUMMER,
                inntekter =
                    listOf(YearMonth.of(2018, 1), YearMonth.of(2018, 2), YearMonth.of(2018, 3)).map {
                        ArbeidsgiverInntekt.MånedligInntekt(
                            yearMonth = it,
                            inntekt = 31000.månedlig,
                            type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                            fordel = "a",
                            beskrivelse = "b",
                        )
                    },
            )

        val vilkårsgrunnlag =
            fabrikk.lagVilkårsgrunnlag(
                vedtaksperiodeId = vedtaksperiodeId,
                skjæringstidspunkt = 1.april,
                medlemskapstatus = medlemskapstatus,
                arbeidsforhold =
                    listOf(
                        Vilkårsgrunnlag.Arbeidsforhold(
                            orgnummer = ORGNUMMER,
                            ansettelseperiode = 1.april(2017) til LocalDate.MAX,
                            type = Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT,
                        ),
                    ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(listOf(arbeidsgiverinntekt)),
                inntekterForOpptjeningsvurdering =
                    InntekterForOpptjeningsvurdering(
                        listOf(arbeidsgiverinntekt).map {
                            it.copy(inntekter = listOf(it.inntekter.last()))
                        },
                    ),
                forsikringsvurderingId = null,
            )
        person.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag, Aktivitetslogg())

        val overstyrArbeidsforhold =
            PersonHendelsefabrikk().lagOverstyrArbeidsforhold(
                skjæringstidspunkt = 1.april,
                OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(ORGNUMMER, deaktivert = true, "test"),
            )
        person.håndterOverstyrArbeidsforhold(eventBus, overstyrArbeidsforhold, Aktivitetslogg())

        testDataSource.ds.lagrePerson(fnr, person)
    }
}
