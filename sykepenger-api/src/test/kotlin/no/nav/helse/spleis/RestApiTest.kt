package no.nav.helse.spleis

import com.github.navikt.tbd_libs.signed_jwt_issuer_test.Issuer
import com.github.navikt.tbd_libs.test_support.TestDataSource
import io.ktor.http.HttpStatusCode
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.februar
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.testhelpers.YrkesaktivitetHendelsefabrikk
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class RestApiTest : AbstractApiTest() {
    companion object {
        private const val UNG_PERSON_FNR = "12029240045"
        @JvmStatic private val UNG_PERSON_FØDSELSDATO = 12.februar(1992)
        private const val ORGNUMMER = "987654321"
        @JvmStatic private val MELDINGSREFERANSE = UUID.randomUUID()
    }

    @Test
    fun sporingapi() = blackboxTestApplication(::opprettTestdata) {
        "/api/vedtaksperioder".httpGet(HttpStatusCode.OK, mapOf("fnr" to UNG_PERSON_FNR))
    }

    @Test
    fun `hent personJson med fnr`() = blackboxTestApplication(::opprettTestdata) {
        "/api/person-json".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to UNG_PERSON_FNR))
    }

    @Test
    fun `personApi krever gyldig access token`() = blackboxTestApplication(::opprettTestdata) {
        val body = """{"fødselsnummer": "tullball"}"""
        val annenIssuer = Issuer("annen", "annen_audience")

        post("/api/person", body, HttpStatusCode.Unauthorized, accessToken = null)
        post("/api/person", body, HttpStatusCode.Unauthorized, accessToken = issuer.accessToken {
            withAudience("feil_audience")
        })
        post("/api/person", body, HttpStatusCode.Unauthorized, accessToken = annenIssuer.accessToken())
        // med gyldig token kommer vi forbi autentiseringen og helt fram til validering av requesten
        post("/api/person", body, HttpStatusCode.BadRequest, accessToken = issuer.accessToken())
    }

    @Test
    fun `finner ikke melding`() = blackboxTestApplication(::opprettTestdata) {
        "/api/hendelse-json/${UUID.randomUUID()}".httpGet(HttpStatusCode.NotFound)
    }

    @Test
    fun `finner melding`() = blackboxTestApplication(::opprettTestdata) {
        "/api/hendelse-json/${MELDINGSREFERANSE}".httpGet(HttpStatusCode.OK)
    }

    private fun opprettTestdata(testDataSource: TestDataSource) {
        val eventBus = EventBus()
        val aktivitetslogg = Aktivitetslogg()
        val fom = LocalDate.of(2018, 9, 10)
        val tom = fom.plusDays(16)
        val person = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)
        val fabrikk = YrkesaktivitetHendelsefabrikk(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(ORGNUMMER))
        val søknad = fabrikk.lagSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, 100.prosent), arbeidssituasjon = Søknad.Arbeidssituasjon.ARBEIDSTAKER)
        person.håndterSøknad(eventBus, søknad, aktivitetslogg)
        val vedtaksperiodeId = eventBus.events.filterIsInstance<EventSubscription.VedtaksperiodeOpprettet>().single().vedtaksperiodeId
        val arbeidsgiveropplysninger = fabrikk.lagArbeidsgiveropplysninger(arbeidsgiverperioder = listOf(fom til fom.plusDays(15)), vedtaksperiodeId = vedtaksperiodeId, beregnetInntekt = 31000.månedlig)
        person.håndterArbeidsgiveropplysninger(eventBus, arbeidsgiveropplysninger, Aktivitetslogg())
        testDataSource.ds.lagrePerson(UNG_PERSON_FNR, person)
        testDataSource.ds.lagreHendelse(meldingsReferanse = MELDINGSREFERANSE, fødselsnummer = UNG_PERSON_FNR)
    }

}
