package no.nav.helse.spleis.etterlevelse

import no.nav.helse.ForventetFeil
import no.nav.helse.Toggle
import no.nav.helse.januar
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.SubsumsjonMediator
import no.nav.helse.spleis.TestHendelseMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

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
            .medVedtaksperiode(UUID.randomUUID(), emptyList())
        subsumsjonMediator = SubsumsjonMediator(jurist, fnr, TestHendelseMessage(fnr), versjonAvKode)
        testRapid = TestRapid()
    }

    @Test
    fun `en melding på gyldig format`() = Toggle.SubsumsjonHendelser.enable {
        jurist.`§ 8-17 ledd 2`(1.januar(2018))
        subsumsjonMediator.finalize(testRapid)
        assertSubsumsjonsmelding(testRapid.inspektør.message(0)["subsumsjon"])
    }
}
