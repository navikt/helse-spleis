package no.nav.helse.spleis

import com.github.navikt.tbd_libs.test_support.TestDataSource
import io.ktor.http.HttpStatusCode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.EmptyLog
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.person.Person
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.Personidentifikator
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

internal class RestApiTest {
    private companion object {
        private const val UNG_PERSON_FNR = "12029240045"
        private val UNG_PERSON_FØDSELSDATO = 12.februar(1992)
        private const val ORGNUMMER = "987654321"
        private val MELDINGSREFERANSE = UUID.randomUUID()

        private val appservere = Applikasjonsservere()

        @AfterAll
        @JvmStatic
        fun teardown() {
            appservere.ryddOpp()
        }
    }

    @Test
    fun sporingapi() = blackboxTestApplication {
        "/api/vedtaksperioder".httpGet(HttpStatusCode.OK, mapOf("fnr" to UNG_PERSON_FNR))
    }

    @Test
    fun `hent personJson med fnr`() = blackboxTestApplication{
        "/api/person-json".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to UNG_PERSON_FNR))
    }

    @Test
    fun `finner ikke melding`() = blackboxTestApplication {
        "/api/hendelse-json/${UUID.randomUUID()}".httpGet(HttpStatusCode.NotFound)
    }

    @Test
    fun `finner melding`() = blackboxTestApplication {
        "/api/hendelse-json/${MELDINGSREFERANSE}".httpGet(HttpStatusCode.OK)
    }

    @Test
    fun `request med manglende eller feil access token`() = blackboxTestApplication {
            val query = """
            {
                person(fnr: \"${UNG_PERSON_FNR}\") { } 
            }
        """

            val body = """{"query": "$query"}"""

            val annenIssuer = Issuer("annen")

        post(body, HttpStatusCode.Unauthorized, accessToken = null)
        post(body, HttpStatusCode.Unauthorized, accessToken = issuer.createToken("feil_audience"))
        post(body, HttpStatusCode.Unauthorized, accessToken = annenIssuer.createToken())
        post(body, HttpStatusCode.OK, accessToken = issuer.createToken(Issuer.AUDIENCE))
    }

    private fun blackboxTestApplication(testblokk: suspend Applikasjonsservere.BlackboxTestContext.() -> Unit) {
        appservere.kjørTest(::opprettTestdata, testblokk)
    }

    private fun opprettTestdata(testDataSource: TestDataSource) {
        val fom = LocalDate.of(2018, 9, 10)
        val tom = fom.plusDays(16)
        val sykeperioder = listOf(Sykmeldingsperiode(fom, tom))
        val vedtaksperiodeId = UUID.randomUUID()
        val sykmelding = Sykmelding(
            meldingsreferanseId = vedtaksperiodeId,
            orgnummer = ORGNUMMER,
            sykeperioder = sykeperioder
        )
        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = vedtaksperiodeId,
            refusjon = Inntektsmelding.Refusjon(
                beløp = 12000.månedlig,
                opphørsdato = null
            ),
            orgnummer = ORGNUMMER,
            førsteFraværsdag = LocalDate.of(2018, 1, 1),
            beregnetInntekt = 12000.månedlig,
            arbeidsgiverperioder = listOf(Periode(LocalDate.of(2018, 9, 10), LocalDate.of(2018, 9, 10).plusDays(16))),
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            harFlereInntektsmeldinger = false,
            harOpphørAvNaturalytelser = false,
            avsendersystem = Inntektsmelding.Avsendersystem.NAV_NO(vedtaksperiodeId, LocalDate.EPOCH),
            mottatt = LocalDateTime.now()
        )
        val person = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)
        person.håndter(sykmelding, Aktivitetslogg())
        person.håndter(inntektsmelding, Aktivitetslogg())
        testDataSource.ds.lagrePerson(UNG_PERSON_FNR, person)
        testDataSource.ds.lagreHendelse(MELDINGSREFERANSE)
    }

    private fun DataSource.lagrePerson(fødselsnummer: String, person: Person) {
        val serialisertPerson = person.dto().tilPersonData().tilSerialisertPerson()
        sessionOf(this, returnGeneratedKey = true).use {
            val personId = it.run(queryOf("INSERT INTO person (fnr, skjema_versjon, data) VALUES (?, ?, (to_json(?::json)))",
                fødselsnummer.toLong(), serialisertPerson.skjemaVersjon, serialisertPerson.json).asUpdateAndReturnGeneratedKey)
            it.run(queryOf("INSERT INTO person_alias (fnr, person_id) VALUES (?, ?);",
                fødselsnummer.toLong(), personId!!).asExecute)

        }
    }

    private fun DataSource.lagreHendelse(
        meldingsReferanse: UUID,
        meldingstype: HendelseDao.Meldingstype = HendelseDao.Meldingstype.INNTEKTSMELDING,
        fødselsnummer: String = UNG_PERSON_FNR,
        data: String = "{}"
    ) {
        sessionOf(this).use {
            it.run(
                queryOf(
                    "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (?, ?, ?, (to_json(?::json)))",
                    fødselsnummer.toLong(),
                    meldingsReferanse.toString(),
                    meldingstype.toString(),
                    data
                ).asExecute
            )
        }
    }
}
