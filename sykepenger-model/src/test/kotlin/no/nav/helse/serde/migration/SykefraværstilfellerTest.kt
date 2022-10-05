package no.nav.helse.serde.migration

import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.september
import no.nav.helse.serde.migration.Sykefraværstilfeller.AktivVedtaksperiode
import no.nav.helse.serde.migration.Sykefraværstilfeller.ForkastetVedtaksperiode
import no.nav.helse.serde.migration.Sykefraværstilfeller.Sykefraværstilfelle
import no.nav.helse.serde.migration.Sykefraværstilfeller.sykefraværstilfeller
import no.nav.helse.serde.migration.Sykefraværstilfeller.vedtaksperioder
import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykefraværstilfellerTest {

    @Test
    fun `sammenhengende periode hvor tidligste skjæringstidspunkt er før perioden`() {
        val vedtaksperioder = listOf(
            AktivVedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.januar til 31.januar, "AVSLUTTET"),
            AktivVedtaksperiode(skjæringstidspunkt = 15.desember(2017), periode = 1.februar til 28.februar, "AVSLUTTET"),
            AktivVedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.mars til 31.mars, "AVSLUTTET")
        )
        val forventet = setOf(Sykefraværstilfelle(
            tidligsteSkjæringstidspunkt = 15.desember(2017),
            aktiveSkjæringstidspunkter = setOf(15.desember(2017), 1.januar),
            sisteDag = 31.mars,
            sisteDagISpleis = 31.mars
        ))
        assertEquals(forventet, sykefraværstilfeller(vedtaksperioder))
    }

    @Test
    fun `fjerner duplikate sykefraværstilfeller`() {
        val vedtaksperioder = listOf(
            AktivVedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.januar til 5.januar, "AVSLUTTET"),
            AktivVedtaksperiode(skjæringstidspunkt = 1.januar, periode = 8.januar til 31.januar, "AVSLUTTET"),
        )
        val forventet = setOf(Sykefraværstilfelle(
            tidligsteSkjæringstidspunkt = 1.januar,
            aktiveSkjæringstidspunkter = setOf(1.januar),
            sisteDag = 31.januar,
            sisteDagISpleis = 31.januar
        ))
        assertEquals(forventet, sykefraværstilfeller(vedtaksperioder))
    }

    @Test
    fun `perioder med helg mellom, men i samme sykefraværstilfellet hvor tidligste skjæringstidspunkt er før perioden`() {
        val vedtaksperioder = listOf(
            AktivVedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.januar til 5.januar, "AVSLUTTET"),
            AktivVedtaksperiode(skjæringstidspunkt = 1.januar, periode = 8.januar til 12.januar, "AVSLUTTET"),
            AktivVedtaksperiode(skjæringstidspunkt = 1.januar, periode = 15.januar til 19.januar, "AVSLUTTET"),
        )
        val forventet = setOf(Sykefraværstilfelle(
            tidligsteSkjæringstidspunkt = 1.januar,
            aktiveSkjæringstidspunkter = setOf(1.januar),
            sisteDag = 19.januar,
            sisteDagISpleis = 19.januar
        ))
        assertEquals(forventet, sykefraværstilfeller(vedtaksperioder))
    }

    @Test
    fun `finner sykefraværstilfeller fra aktive vedtaksperioder i person-json`() {
        val forventet = setOf(Sykefraværstilfelle(
            tidligsteSkjæringstidspunkt = 1.januar,
            aktiveSkjæringstidspunkter = setOf(1.januar),
            sisteDag = 31.mars,
            sisteDagISpleis = 29.mars
        ))
        val vedtaksperioder = vedtaksperioder(serdeObjectMapper.readTree(vedtaksperioderPåTversAvArbeidsgivere))
        assertEquals(forventet, sykefraværstilfeller(vedtaksperioder))
    }

    @Test
    fun `utelater sykefraværstilfeller med bare forkastede perioder`() {
        val vedtaksperioder = listOf(
            ForkastetVedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.januar til 5.januar, "TIL_INFOTRYGD")
        )
        val forventet = emptySet<Sykefraværstilfelle>()
        assertEquals(forventet, sykefraværstilfeller(vedtaksperioder))
    }

    @Test
    fun `utelater frittstående forkastede perioder`() {
        val vedtaksperioder = listOf(
            ForkastetVedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.januar til 5.januar, "TIL_INFOTRYGD"),
            AktivVedtaksperiode(skjæringstidspunkt = 2.januar, periode = 6.januar til 10.januar, "AVSLUTTET"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 2.januar, periode = 11.januar til 20.januar, "TIL_INFOTRYGD"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 1.februar, periode = 1.februar til 28.februar, "TIL_INFOTRYGD")
        )
        val forventet = setOf(Sykefraværstilfelle(
            tidligsteSkjæringstidspunkt = 1.januar,
            aktiveSkjæringstidspunkter = setOf(2.januar),
            sisteDag = 20.januar,
            sisteDagISpleis = 10.januar
        ))
        assertEquals(forventet, sykefraværstilfeller(vedtaksperioder))
    }

    @Test
    fun `hensyntar forkastede perioder med helg mellom`() {
        val vedtaksperioder = listOf(
            ForkastetVedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.januar til 5.januar, "TIL_INFOTRYGD"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 2.januar, periode = 8.januar til 12.januar, "TIL_INFOTRYGD"),
            AktivVedtaksperiode(skjæringstidspunkt = 2.januar, periode = 15.januar til 20.januar, "AVSLUTTET"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 1.februar, periode = 1.februar til 28.februar, "TIL_INFOTRYGD")
        )
        val forventet = setOf(Sykefraværstilfelle(
            tidligsteSkjæringstidspunkt = 1.januar,
            aktiveSkjæringstidspunkter = setOf(2.januar),
            sisteDag = 20.januar,
            sisteDagISpleis = 20.januar
        ))
        assertEquals(forventet, sykefraværstilfeller(vedtaksperioder))
    }

    @Test
    fun `overlappende sykefraværstilfeller`() {
        val vedtaksperioder = listOf(
            AktivVedtaksperiode(skjæringstidspunkt = 11.januar, periode = 11.januar til 28.januar, "AVSLUTTET"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 11.januar, periode = 29.januar til 25.februar, "TIL_INFOTRYGD"),
            AktivVedtaksperiode(skjæringstidspunkt = 14.februar, periode = 21.mars til 18.april, "AVSLUTTET"),
            AktivVedtaksperiode(skjæringstidspunkt = 14.februar, periode = 19.april til 8.mai, "AVSLUTTET"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 14.februar, periode = 9.mai til 28.mai, "TIL_INFOTRYGD"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 11.januar, periode = 29.mai til 17.juni, "TIL_INFOTRYGD"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 11.januar, periode = 18.juni til 16.juli, "TIL_INFOTRYGD"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 11.januar, periode = 17.juli til 15.august, "TIL_INFOTRYGD"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 11.januar, periode = 15.august til 4.september, "TIL_INFOTRYGD"),
        )

        val forventet = setOf(
            Sykefraværstilfelle(
                aktiveSkjæringstidspunkter = setOf(11.januar),
                tidligsteSkjæringstidspunkt = 11.januar,
                sisteDag = 25.februar,
                sisteDagISpleis = 28.januar
            ),
            Sykefraværstilfelle(
                aktiveSkjæringstidspunkter = setOf(14.februar, 11.januar),
                tidligsteSkjæringstidspunkt = 11.januar,
                sisteDag = 4.september,
                sisteDagISpleis = 8.mai
            )
        )
        assertEquals(forventet, sykefraværstilfeller(vedtaksperioder))
    }

    @Test
    fun `mange forkastede perioder`() {
        val vedtaksperioder = listOf(
            ForkastetVedtaksperiode(skjæringstidspunkt = 17.januar, periode = 1.januar til 31.januar, "TIL_INFOTRYGD"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 1.februar, periode = 1.februar til 28.februar, "TIL_INFOTRYGD"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 1.mars, periode = 1.mars til 31.mars, "TIL_INFOTRYGD"),
            ForkastetVedtaksperiode(skjæringstidspunkt = 1.april, periode = 1.april til 30.april, "TIL_INFOTRYGD"),
            AktivVedtaksperiode(skjæringstidspunkt = 1.mai, periode = 1.mai til 31.mai, "AVSLUTTET"),
        )

        val forventet = setOf(Sykefraværstilfelle(
            tidligsteSkjæringstidspunkt = 17.januar,
            aktiveSkjæringstidspunkter = setOf(1.mai),
            sisteDag = 31.mai,
            sisteDagISpleis = 31.mai
        ))

        assertEquals(forventet, sykefraværstilfeller(vedtaksperioder))
    }
}

@Language("Json")
private val vedtaksperioderPåTversAvArbeidsgivere = """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "fom": "2017-12-15",
          "tom": "2017-12-29",
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
          "tom": "2018-03-29",
          "tilstand": "AVSLUTTET",
          "skjæringstidspunkt": "2018-01-01"
        }
      ], 
      "forkastede": [
        {
          "vedtaksperiode": {
            "fom": "2018-03-30",
            "tom": "2018-03-31",
            "tilstand": "TIL_INFOTRYGD",
            "skjæringstidspunkt": "2018-01-01"
          }
        }
      ]
    }
  ]
}"""
