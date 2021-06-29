package no.nav.helse.person.vilkår

import org.intellij.lang.annotations.Language

class Lovtrace {
    fun `§2`(oppfylt: Boolean) {}

    fun `§8-2 ledd 1`(oppfylt: Boolean) {}

    fun `§8-3 ledd 2`(oppfylt: Boolean) {}

    fun `§8-12 ledd 1`(oppbrukt: Boolean, hvor: String) {}

    fun `§8-12 ledd 2`() {}

    fun `§8-30 ledd 2`(oppfylt: Boolean) {}
}

@Language("JSON")
private val test = """[{
        "fom": "2020-10-01",
        "tom": "2021-02-01",
        "oppfylt": false,
        "folketrygdlovenVersjon": "2021-06-01",
        "paragraf": "8-12",
        "ledd": 1,
        "data": {
            "type": "ARBEIDSTAKER",
            "sykedager": [
                {"fom": "2020-10-01", "tom": "2020-10-31", "dagtype": "NAVDAG"},
                {"fom": "2021-01-01", "tom": "2021-02-28", "dagtype": "NAVDAG"}
            ],
            "antallForbrukteDager": 90,
            "antallGjenståendeDager": 158,
            "maksdato": "2021-12-01"
        }
    },{
        "fom": "2021-02-02",
        "tom": "2021-02-28",
        "oppfylt": true,
        "folketrygdlovenVersjon": "2021-06-01",
        "paragraf": "8-12",
        "ledd": 1,
        "data": {
            "type": "ARBEIDSTAKER",
            "sykedager": [
                {"fom": "2020-10-01", "tom": "2020-10-31", "dagtype": "NAVDAG"},
                {"fom": "2021-01-01", "tom": "2021-02-28", "dagtype": "NAVDAG"}
            ],
            "antallForbrukteDager": 90,
            "antallGjenståendeDager": 158,
            "maksdato": "2021-12-01"
        }
    },
    {
        "fom": "2021-03-01",
        "tom": "2021-03-31",
        "oppfylt": false,
        "folketrygdlovenVersjon": "2021-06-01",
        "paragraf": "8-12",
        "ledd": 1,
        "data": {
            "type": "ARBEIDSTAKER",
            "sykedager": [
                {"fom": "2020-10-01", "tom": "2020-10-31", "dagtype": "NAVDAG"},
                {"fom": "2021-01-01", "tom": "2021-03-31", "dagtype": "NAVDAG"}
            ],
            "antallForbrukteDager": 148,
            "antallGjenståendeDager": 0,
            "maksdato": "2021-02-28"
        }
    }
]"""
