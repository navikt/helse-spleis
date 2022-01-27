package no.nav.helse.serde

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MetrikkTest {

    @Test
    fun `regner ut prosentandel av enkeltverdi i forhold til total json`() {
        assertStørrelse(
            forventetStreng = """"value"""",
            metrikk = Metrikk.metrikkerAv(
                json,
                listOf("key")
            ).single()
        )
    }

    @Test
    fun `regner ut prosentandel av objekt i forhold til total json`() {
        assertStørrelse(
            forventetStreng = """{"key":"value"}""",
            metrikk = Metrikk.metrikkerAv(
                json,
                listOf("indreVerdi")
            ).single()
        )
    }

    @Test
    fun `regner ut prosentandel av indre verdi i forhold til total json`() {
        assertStørrelse(
            forventetStreng = """"value"""",
            metrikk = Metrikk.metrikkerAv(
                json,
                listOf("indreVerdi", "key")
            ).single()
        )
    }

    @Test
    fun `regner ut prosentandel av innhold i liste i forhold til total json`() {
        assertStørrelse(
            forventetStreng = """"hei""oof"""",
            metrikk = Metrikk.metrikkerAv(
                json,
                listOf("liste")
            ).single()
        )
    }

    @Test
    fun `regner ut prosentandel av verdier i indre objekt i liste i forhold til total json`() {
        assertStørrelse(
            forventetStreng = """"hei""oof"""",
            metrikk = Metrikk.metrikkerAv(
                json,
                listOf("listeMedObjekter", "key")
            ).single()
        )
    }

    @Test
    fun `regner ut prosentandel av liste med verdier i indre objekt i liste i forhold til total json`() {
        assertStørrelse(
            forventetStreng = """"hei""oof""hei""oof"""",
            metrikk = Metrikk.metrikkerAv(
                json,
                listOf("listeMedIndreObjektMedListe", "liste")
            ).single()
        )
    }

    private fun assertStørrelse(forventetStreng: String, metrikk: Metrikk) {
        assertEquals(forventetStreng.length, metrikk.størrelse())
    }


    @Language("JSON")
    val json = """
{
    "key": "value",
    "indreVerdi": {
        "key": "value"
    },
    "liste": ["hei", "oof"],
    "listeMedObjekter": [
        {
            "key": "hei"
        },
        {
            "key": "oof"
        }
    ],
    "listeMedIndreObjektMedListe": [
        {
            "liste": ["hei", "oof"]
        },
        {
            "liste": ["hei", "oof"]
        }
    ]
}
    """

    private val totalStørrelse = serdeObjectMapper.readTree(json).toString().length
}
