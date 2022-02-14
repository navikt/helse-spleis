package no.nav.helse.spleis.etterlevelse

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import no.nav.helse.Toggle
import no.nav.helse.januar
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Tidslinjedag
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.SubsumsjonMediator
import no.nav.helse.spleis.TestHendelseMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.URI

internal class SubsumsjonsmeldingTest {
    private val fnr = "12029240045"
    private val versjonAvKode = "1.0.0"

    private lateinit var subsumsjonMediator: SubsumsjonMediator
    private lateinit var testRapid: TestRapid

    private lateinit var jurist: MaskinellJurist

    @BeforeEach
    fun beforeEach() {
        jurist = MaskinellJurist()
            .medFødselsnummer(fnr.somFødselsnummer())
            .medOrganisasjonsnummer("123456789")
        subsumsjonMediator = SubsumsjonMediator(jurist, fnr, TestHendelseMessage(fnr), versjonAvKode)
        testRapid = TestRapid()
    }

    @Test
    fun `en melding på gyldig format`() = Toggle.SubsumsjonHendelser.enable {
        jurist.`§ 8-17 ledd 2`(1.januar(2018), MutableList(31) { Tidslinjedag((it + 1).januar, "NAVDAG", 100) })
        subsumsjonMediator.finalize(testRapid)
        assertSubsumsjonsmelding(testRapid.inspektør.message(0)["subsumsjon"])
    }

    private val schema by lazy {
        JsonSchemaFactory
            .getInstance(SpecVersion.VersionFlag.V7)
            .getSchema(URI("https://raw.githubusercontent.com/navikt/helse/c53bc453251b7878135f31d5d1070e5406ae4af1/subsumsjon/json-schema-1.0.0.json"))
    }

    private fun assertSubsumsjonsmelding(melding: JsonNode) {
        try {
            assertEquals(emptySet<ValidationMessage>(), schema.validate(melding))
        } catch (_: Exception) {
            LoggerFactory.getLogger(SubsumsjonsmeldingTest::class.java).warn("Kunne ikke kjøre kontrakttest for subsumsjoner. Mangler du internett?")
        }
    }
}
