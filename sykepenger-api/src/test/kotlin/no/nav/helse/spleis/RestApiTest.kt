package no.nav.helse.spleis

import com.github.navikt.tbd_libs.signed_jwt_issuer_test.Issuer
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.long
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import com.github.navikt.tbd_libs.sql_dsl.transaction
import com.github.navikt.tbd_libs.test_support.TestDataSource
import io.ktor.http.HttpStatusCode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
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
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.intellij.lang.annotations.Language
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
    fun `hent personJson med fnr`() = blackboxTestApplication {
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

        val annenIssuer = Issuer("annen", "annen_audience")

        post(body, HttpStatusCode.Unauthorized, accessToken = null)
        post(body, HttpStatusCode.Unauthorized, accessToken = issuer.accessToken {
            withAudience("feil_audience")
        })
        post(body, HttpStatusCode.Unauthorized, accessToken = annenIssuer.accessToken())
        post(body, HttpStatusCode.OK, accessToken = issuer.accessToken())
    }

    private fun blackboxTestApplication(testblokk: suspend Applikasjonsservere.BlackboxTestContext.() -> Unit) {
        appservere.kjørTest(::opprettTestdata, testblokk)
    }

    private fun opprettTestdata(testDataSource: TestDataSource) {
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
            harFlereInntektsmeldinger = false,
            opphørAvNaturalytelser = emptyList(),
            førsteFraværsdag = LocalDate.of(2018, 9, 10),
            mottatt = LocalDateTime.now()
        )
        val person = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)
        person.håndterSykmelding(sykmelding, Aktivitetslogg())
        person.håndterInntektsmelding(inntektsmelding, Aktivitetslogg())
        testDataSource.ds.lagrePerson(UNG_PERSON_FNR, person)
        testDataSource.ds.lagreHendelse(MELDINGSREFERANSE)
    }

    private fun DataSource.lagrePerson(fødselsnummer: String, person: Person) {
        val serialisertPerson = person.dto().tilPersonData().tilSerialisertPerson()
        connection {
            transaction {
                @Language("PostgreSQL")
                val opprettPerson = "INSERT INTO person(skjema_versjon, fnr, data) VALUES(:skjemaVersjon, :fnr, :data) RETURNING id"
                val personId = prepareStatementWithNamedParameters(opprettPerson) {
                    withParameter("fnr", fødselsnummer.toLong())
                    withParameter("skjemaVersjon", serialisertPerson.skjemaVersjon)
                    withParameter("data", serialisertPerson.json)
                }.use {
                    it.executeQuery().use { rs ->
                        rs.single { it.long(1) }
                    }
                }

                @Language("PostgreSQL")
                val opprettPersonAlias = "INSERT INTO person_alias (fnr, person_id) VALUES (:fnr, :personId)"
                prepareStatementWithNamedParameters(opprettPersonAlias) {
                    withParameter("fnr", fødselsnummer.toLong())
                    withParameter("personId", personId)
                }.use {
                    it.execute()
                }
            }

        }
    }

    private fun DataSource.lagreHendelse(
        meldingsReferanse: UUID,
        meldingstype: HendelseDao.Meldingstype = HendelseDao.Meldingstype.INNTEKTSMELDING,
        fødselsnummer: String = UNG_PERSON_FNR,
        data: String = """{ "@opprettet": "${LocalDateTime.now()}" }"""
    ) {
        @Language("PostgreSQL")
        val sql = "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (:fnr, :meldingId, :meldingType, cast(:data as json))"
        connection {
            prepareStatementWithNamedParameters(sql) {
                withParameter("fnr", fødselsnummer.toLong())
                withParameter("meldingId", meldingsReferanse)
                withParameter("meldingType", meldingstype.name)
                withParameter("data", data)
            }.use { it.execute() }
        }
    }
}
