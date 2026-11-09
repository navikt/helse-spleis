package no.nav.helse.spleis.opptjening

import io.ktor.http.HttpStatusCode
import no.nav.helse.dto.deserialisering.VilkårsgrunnlagInnDto
import no.nav.helse.dto.deserialisering.VilkårsgrunnlagInnslagInnDto
import no.nav.helse.dto.deserialisering.VilkårsgrunnlaghistorikkInnDto
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.nyttFødselsnummer
import no.nav.helse.person.Person
import no.nav.helse.serde.tilPersonData
import no.nav.helse.spleis.AbstractApiTest
import no.nav.helse.spleis.objectMapper
import no.nav.helse.spleis.testhelpers.arbeidstakerMedInfotrygdforlengelse
import no.nav.helse.spleis.testhelpers.selvstendig
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert
import tools.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

class OpptjeningsvurderingApiTest : AbstractApiTest() {
    // unikt per testmetode (ny instans per @Test) — deles av `sendt.person`/`person_alias`, som har `fnr` som unik kolonne i den delte databasen
    private val FNR = nyttFødselsnummer()

    @ParameterizedTest
    @ValueSource(strings = ["SPLEIS", "INFOTRYGD"])
    fun `viser unike rettighetskategorier med første tidspunkt for hver kombinasjon`(kilde: String) {
        val personDto = arbeidstakerMedInfotrygdforlengelse().dto().tilPersonData().tilPersonDto()
        val eksisterendeHistorikk = personDto.vilkårsgrunnlagHistorikk.historikk
        val grunnlag = eksisterendeHistorikk.flatMap { it.vilkårsgrunnlag }
        val spleis = grunnlag.filterIsInstance<VilkårsgrunnlagInnDto.Spleis>().first()
        val infotrygd = grunnlag.filterIsInstance<VilkårsgrunnlagInnDto.Infotrygd>().first()
        val vurderingId = UUID.randomUUID()
        val annenVurderingId = UUID.randomUUID()
        val førsteKategoriId = UUID.randomUUID()
        val andreKategoriId = UUID.randomUUID()

        fun vilkårsgrunnlag(
            opptjeningsvurderingId: UUID,
            rettighetskategoriId: UUID,
        ): VilkårsgrunnlagInnDto.Komplett =
            when (kilde) {
                "SPLEIS" ->
                    spleis.copy(
                        vilkårsgrunnlagId = UUID.randomUUID(),
                        opptjeningsvurderingId = opptjeningsvurderingId,
                        rettighetskategoriId = rettighetskategoriId,
                    )
                else ->
                    infotrygd.copy(
                        vilkårsgrunnlagId = UUID.randomUUID(),
                        opptjeningsvurderingId = opptjeningsvurderingId,
                        rettighetskategoriId = rettighetskategoriId,
                    )
            }

        fun innslag(
            opprettet: String,
            vararg vilkårsgrunnlag: VilkårsgrunnlagInnDto,
        ) = VilkårsgrunnlagInnslagInnDto(UUID.randomUUID(), LocalDateTime.parse(opprettet), vilkårsgrunnlag.toList())

        val historikk =
            listOf(
                innslag(
                    "2026-01-04T12:00:00",
                    vilkårsgrunnlag(vurderingId, andreKategoriId),
                ),
                innslag(
                    "2026-01-01T12:00:00",
                    vilkårsgrunnlag(annenVurderingId, andreKategoriId),
                    VilkårsgrunnlagInnDto.Rettighetskategori(UUID.randomUUID(), spleis.skjæringstidspunkt.plusDays(1), førsteKategoriId),
                ),
                innslag(
                    "2026-01-02T12:00:00",
                    vilkårsgrunnlag(vurderingId, førsteKategoriId),
                ),
                innslag(
                    "2026-01-03T12:00:00",
                    vilkårsgrunnlag(vurderingId, andreKategoriId),
                ),
            ) + eksisterendeHistorikk
        val person = Person.gjenopprett(EmptyLog, personDto.copy(vilkårsgrunnlagHistorikk = VilkårsgrunnlaghistorikkInnDto(historikk)))

        blackboxTestApplication({ it.ds.lagrePerson(FNR, person) }) {
            "/api/opptjeningsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to FNR)) {
                val vurderinger = objectMapper.readTree(this).path("opptjeningsvurderinger")
                val vurdering = vurderinger.single { it.path("opptjeningsvurderingId").asText() == vurderingId.toString() }

                @Language("JSON")
                val forventet = """
                    [
                      {"id": "$andreKategoriId", "opprettet": "2026-01-03T12:00"},
                      {"id": "$førsteKategoriId", "opprettet": "2026-01-02T12:00"}
                    ]
                """
                JSONAssert.assertEquals(forventet, vurdering.path("rettighetskategorier").toString(), true)
                assertEquals("2026-01-02T12:00", vurdering.path("opprettet").asText())
                val annenVurdering = vurderinger.single { it.path("opptjeningsvurderingId").asText() == annenVurderingId.toString() }
                JSONAssert.assertEquals(
                    """[{"id": "$andreKategoriId", "opprettet": "2026-01-01T12:00"}]""",
                    annenVurdering.path("rettighetskategorier").toString(),
                    true,
                )
            }
        }
    }

    @Test
    fun `hent opptjeningsvurderinger for person som finnes`() =
        blackboxTestApplication({ it.ds.lagrePerson(FNR, arbeidstakerMedInfotrygdforlengelse()) }) {
            "/api/opptjeningsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to FNR)) {
                val actual = assertOgNormaliserGenererteFelter(this)

                @Language("JSON")
                val expected = """
            {
              "opptjeningsvurderinger": [
                {
                  "opptjeningsvurderingId": "b89e2ae5-59e3-388e-98cd-42a8e7350773",
                  "opprettet": "2022-08-31T09:52:08.494020",
                  "type": "ARBEIDSTAKER",
                  "skjæringstidspunkt": "2018-01-01",
                  "kilde": "INFOTRYGD",
                  "rettighetskategorier": [{"id": "<id>", "opprettet": "<opprettet>"}]
                },
                {
                  "opptjeningsvurderingId": "00000000-0000-0000-0000-000000000000",
                  "opprettet": "<opprettet>",
                  "type": "ARBEIDSTAKER",
                  "skjæringstidspunkt": "2018-04-01",
                  "kilde": "SPLEIS",
                  "rettighetskategorier": [{"id": "<id>", "opprettet": "<opprettet>"}],
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
    fun `hent opptjeningsvurderinger for person som ikke finnes`() =
        blackboxTestApplication({}) {
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
    fun `hent opptjeningsvurderinger for selvstendig som finnes`() =
        blackboxTestApplication({ it.ds.lagrePerson(FNR, selvstendig(FNR)) }) {
            "/api/opptjeningsvurderinger".httpPost(HttpStatusCode.OK, mapOf("fødselsnummer" to FNR)) {
                val actual = assertOgNormaliserGenererteFelter(this)

                @Language("JSON")
                val expected = """
            {
              "opptjeningsvurderinger": [
                {
                  "opptjeningsvurderingId": "00000000-0000-0000-0000-000000000000",
                  "opprettet": "<opprettet>",
                  "skjæringstidspunkt": "2018-04-01",
                  "kilde": "SPLEIS",
                  "type": "SELVSTENDIG",
                  "rettighetskategorier": [{"id": "<id>", "opprettet": "<opprettet>"}]
                }
              ]
            }
            """
                JSONAssert.assertEquals(expected, actual, true)
            }
        }

    private fun assertOgNormaliserGenererteFelter(response: String): String {
        val spleisOpptjeningsvurderingIder = mutableListOf<UUID>()
        return objectMapper
            .readTree(response)
            .apply {
                val forventetAntall = path("opptjeningsvurderinger").count { it.path("kilde").asText() == "SPLEIS" }
                path("opptjeningsvurderinger").forEach { opptjeningsvurdering ->
                    opptjeningsvurdering.path("rettighetskategorier").forEach { kategori ->
                        kategori as ObjectNode
                        assertDoesNotThrow { UUID.fromString(kategori.path("id").asText()) }
                        assertDoesNotThrow { LocalDateTime.parse(kategori.path("opprettet").asText()) }
                        kategori.put("id", "<id>")
                        kategori.put("opprettet", "<opprettet>")
                    }
                    if (opptjeningsvurdering.path("kilde").asText() == "INFOTRYGD") return@forEach
                    opptjeningsvurdering as ObjectNode
                    val opptjeningsvurderingId =
                        assertDoesNotThrow { UUID.fromString(opptjeningsvurdering.path("opptjeningsvurderingId").asText()) }
                    spleisOpptjeningsvurderingIder.add(opptjeningsvurderingId)
                    opptjeningsvurdering.put("opptjeningsvurderingId", "00000000-0000-0000-0000-000000000000")
                    assertDoesNotThrow { LocalDateTime.parse(opptjeningsvurdering.path("opprettet").asText()) }
                    opptjeningsvurdering.put("opprettet", "<opprettet>")
                }
                assertEquals(forventetAntall, spleisOpptjeningsvurderingIder.toSet().size)
            }.toString()
    }
}
