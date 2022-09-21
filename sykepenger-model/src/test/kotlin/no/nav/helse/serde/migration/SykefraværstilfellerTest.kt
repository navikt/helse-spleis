package no.nav.helse.serde.migration

import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.serde.migration.Sykefraværstilfeller.Vedtaksperiode
import no.nav.helse.serde.migration.Sykefraværstilfeller.sykefraværstilfeller
import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykefraværstilfellerTest {

    @Test
    fun `sammenhengende periode hvor tidligste skjæringstidspunkt er før perioden`() {
        val vedtaksperioder = listOf(
            Vedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.januar til 31.januar),
            Vedtaksperiode(skjæringstidspunkt = 15.desember(2017), periode = 1.februar til 28.februar),
            Vedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.mars til 31.mars)
        )
        assertEquals(setOf(15.desember(2017) til 31.mars), sykefraværstilfeller(vedtaksperioder))
    }

    @Test
    fun `fjerner duplikate sykefraværstilfeller`() {
        val vedtaksperioder = listOf(
            Vedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.januar til 5.januar),
            Vedtaksperiode(skjæringstidspunkt = 1.januar, periode = 8.januar til 31.januar),
        )
        assertEquals(setOf(1.januar til 31.januar), sykefraværstilfeller(vedtaksperioder))
    }

    @Test
    fun `perioder med helg mellom, men i samme sykefraværstilfellet hvor tidligste skjæringstidspunkt er før perioden`() {
        val vedtaksperioder = listOf(
            Vedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.januar til 5.januar),
            Vedtaksperiode(skjæringstidspunkt = 1.januar, periode = 8.januar til 12.januar),
            Vedtaksperiode(skjæringstidspunkt = 1.januar, periode = 15.januar til 19.januar),
        )
        assertEquals(setOf(1.januar til 19.januar), sykefraværstilfeller(vedtaksperioder))
    }

    @Test
    fun `finner sykefraværstilfeller fra aktive vedtaksperioder i person-json`() {
        assertEquals(setOf(1.januar til 31.mars), sykefraværstilfeller(serdeObjectMapper.readTree(json)))
    }
}

@Language("Json")
private val json = """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "fom": "2017-12-15",
          "tom": "2017-12-31",
          "tilstand": "AVSLUTTET_UTEN_UTBETALING",
          "skjæringstidspunkt": "2017-12-15"
        },
        {
          "fom": "2018-01-01",
          "tom": "2018-01-31",
          "tilstand": "AVSLUTTET",
          "skjæringstidspunkt": "2018-01-01"
        }
      ]
    },
    {
      "vedtaksperioder": [
        {
          "fom": "2018-02-01",
          "tom": "2018-02-28",
          "tilstand": "AVSLUTTET",
          "skjæringstidspunkt": "2018-01-01"
        }
      ]
    },
    {
      "vedtaksperioder": [
        {
          "fom": "2018-03-01",
          "tom": "2018-03-31",
          "tilstand": "AVSLUTTET",
          "skjæringstidspunkt": "2018-01-01"
        }
      ]
    }
  ]
}"""