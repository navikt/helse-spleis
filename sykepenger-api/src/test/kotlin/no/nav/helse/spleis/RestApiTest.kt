package no.nav.helse.spleis

import com.github.navikt.tbd_libs.signed_jwt_issuer_test.Issuer
import com.github.navikt.tbd_libs.test_support.TestDataSource
import io.ktor.http.HttpStatusCode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.februar
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.person.EventBus
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
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
    fun `finner ikke melding`() = blackboxTestApplication(::opprettTestdata) {
        "/api/hendelse-json/${UUID.randomUUID()}".httpGet(HttpStatusCode.NotFound)
    }

    @Test
    fun `finner melding`() = blackboxTestApplication(::opprettTestdata) {
        "/api/hendelse-json/${MELDINGSREFERANSE}".httpGet(HttpStatusCode.OK)
    }

    @Test
    fun `request med manglende eller feil access token`() = blackboxTestApplication(::opprettTestdata) {
        val query = """
            {
                person(fnr: \"${UNG_PERSON_FNR}\") { } 
            }
        """

        val body = """{"query": "$query"}"""

        val annenIssuer = Issuer("annen", "annen_audience")

        post(body, HttpStatusCode.Unauthorized, accessToken = null)
        post(body, HttpStatusCode.Unauthorized, accessToken = issuer.accessToken {
            withAudience("feil_audience")
        })
        post(body, HttpStatusCode.Unauthorized, accessToken = annenIssuer.accessToken())
        post(body, HttpStatusCode.OK, accessToken = issuer.accessToken())
    }


    private fun opprettTestdata(testDataSource: TestDataSource) {
        val eventBus = EventBus()
        val fom = LocalDate.of(2018, 9, 10)
        val tom = fom.plusDays(16)
        val sykeperioder = listOf(Sykmeldingsperiode(fom, tom))
        val sykmelding = Sykmelding(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(ORGNUMMER),
            sykeperioder = sykeperioder
        )
        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            refusjon = Inntektsmelding.Refusjon(
                beløp = 12000.månedlig,
                opphørsdato = null
            ),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
                organisasjonsnummer = ORGNUMMER
            ),
            beregnetInntekt = 12000.månedlig,
            arbeidsgiverperioder = listOf(Periode(LocalDate.of(2018, 9, 10), LocalDate.of(2018, 9, 25))),
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            opphørAvNaturalytelser = emptyList(),
            førsteFraværsdag = LocalDate.of(2018, 9, 10),
            mottatt = LocalDateTime.now(),
            arbeidsforholdId = null
        )
        val person = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)
        person.håndterSykmelding(eventBus, sykmelding, Aktivitetslogg())
        person.håndterInntektsmelding(eventBus, inntektsmelding, Aktivitetslogg())
        testDataSource.ds.lagrePerson(UNG_PERSON_FNR, person)
        testDataSource.ds.lagreHendelse(meldingsReferanse = MELDINGSREFERANSE, fødselsnummer = UNG_PERSON_FNR)
    }

}
