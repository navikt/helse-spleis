package no.nav.helse.spleis

import com.github.navikt.tbd_libs.naisful.test.TestContext
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.til
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.testhelpers.TestObservatør
import no.nav.helse.spleis.testhelpers.YrkesaktivitetHendelsefabrikk
import no.nav.helse.testdatabase.TestDataSource
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VedtaksperiodeApiTest : AbstractSpleisApiTest() {
    @Test
    fun `vedtaksperioden har med seg vilkårsgrunnlaget den er beregnet med`() {
        observatør = TestObservatør()
        val eventBus = EventBus().apply { register(observatør) }
        val simuleringsutfisker = Simuleringsutfisker()
        eventBus.register(simuleringsutfisker)
        person = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)

        spleisApiTestApplication(testdata = opprettPersonTilSimulering(eventBus)) {
            val vedtaksperiodeId = simuleringsutfisker.vedtaksperiodeId
            val json = objectMapper.readTree(hentVedtaksperiode(vedtaksperiodeId.toString()))

            assertEquals(vedtaksperiodeId.toString(), json["id"].asText())
            val vilkårsgrunnlagId =
                json["behandlinger"]
                    .last()["endringer"]
                    .last()["vilkårsgrunnlagId"]
                    .asText()
            val vilkårsgrunnlag = json["vilkårsgrunnlag"]
            assertEquals(vilkårsgrunnlagId, vilkårsgrunnlag["vilkårsgrunnlagId"].asText())
            assertEquals(FOM.toString(), vilkårsgrunnlag["skjæringstidspunkt"].asText())
            assertEquals("Vilkårsprøving", vilkårsgrunnlag["type"].asText())
            assertTrue(vilkårsgrunnlag["inntektsgrunnlag"]["arbeidsgiverInntektsopplysninger"].size() > 0)
        }
    }

    @Test
    fun `vedtaksperiode uten vilkårsgrunnlag gir null`() {
        val eventBus = EventBus()
        person = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)

        spleisApiTestApplication(testdata = {
            person.håndterSykmelding(eventBus, sykmelding(), Aktivitetslogg())
            person.håndterSøknad(eventBus, søknad(), Aktivitetslogg())
            lagrePerson(it.ds, UNG_PERSON_FNR, person)
        }) {
            val vedtaksperiodeId =
                eventBus.events
                    .filterIsInstance<EventSubscription.VedtaksperiodeOpprettet>()
                    .single()
                    .vedtaksperiodeId
            val json = objectMapper.readTree(hentVedtaksperiode(vedtaksperiodeId.toString()))
            assertTrue(json["vilkårsgrunnlag"].isNull)
        }
    }

    private fun opprettPersonTilSimulering(eventBus: EventBus): (TestDataSource) -> Unit =
        { testDataSource ->
            person.håndterSykmelding(eventBus, sykmelding(), Aktivitetslogg())
            person.håndterUtbetalingshistorikkEtterInfotrygdendring(eventBus, utbetalinghistorikk(), Aktivitetslogg())
            person.håndterSøknad(eventBus, søknad(), Aktivitetslogg())
            val vedtaksperiodeId =
                eventBus.events
                    .filterIsInstance<EventSubscription.VedtaksperiodeOpprettet>()
                    .single()
                    .vedtaksperiodeId
            person.håndterArbeidsgiveropplysninger(
                eventBus,
                YrkesaktivitetHendelsefabrikk(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(ORGNUMMER)).lagArbeidsgiveropplysninger(
                    arbeidsgiverperioder = listOf(FOM til FOM.plusDays(15)),
                    vedtaksperiodeId = vedtaksperiodeId,
                    beregnetInntekt = 31000.månedlig,
                ),
                Aktivitetslogg(),
            )
            person.håndterYtelser(eventBus, ytelser(), Aktivitetslogg())
            person.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag(), Aktivitetslogg())
            person.håndterYtelser(eventBus, ytelser(), Aktivitetslogg())
            lagrePerson(testDataSource.ds, UNG_PERSON_FNR, person)
        }

    private suspend fun TestContext.hentVedtaksperiode(vedtaksperiodeId: String): String {
        val response =
            client.post("/api/vedtaksperiode/$vedtaksperiodeId") {
                contentType(ContentType.Application.Json)
                setBody("""{"fødselsnummer":"$UNG_PERSON_FNR"}""")
            }
        val responseBody = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, responseBody)
        return responseBody
    }
}
