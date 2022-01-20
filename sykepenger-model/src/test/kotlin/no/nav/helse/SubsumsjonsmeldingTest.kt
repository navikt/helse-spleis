package no.nav.helse

import no.nav.helse.SubsumsjonAssertions.assertSubsumsjonsmelding
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class SubsumsjonsmeldingTest {

    @Test
    fun `en melding på gyldig format`() {
        @Language("JSON")
        val melding = """
        {
            "@id": "85d194ae-a387-40e6-b220-917548bdcb19",
            "@versjon": "1.0.0",
            "@event_name": "subsumsjon",
            "@kilde": "Spleis",
            "versjonAvKode": "ghcr.io/navikt/helse-spleis/spleis:3d29345",
            "fødselsnummer": "12312312222",
            "organisasjonsnummer": "123456878",
            "sporing": {
                "123456": "SØKNAD"
            },
            "tidsstempel": "2018-11-13T20:20:39Z",
            "lovverk": "FOLKETRYGDELOVEN",
            "lovverksversjon": "2020-01-01",
            "paragraf": "§5-12",
            "ledd": 1,
            "punktum": 2,
            "bokstav": "a",
            "input": {
                "foo": 1
            },
            "output": {
                "foo": 2
            },
            "utfall": "VILKÅR_BEREGNET"
        }
        """
        assertSubsumsjonsmelding(melding)
    }
}
