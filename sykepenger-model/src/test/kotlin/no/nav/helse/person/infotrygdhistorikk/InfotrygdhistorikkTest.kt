package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dsl.Behovsoppsamler
import no.nav.helse.dto.deserialisering.InfotrygdhistorikkInnDto
import no.nav.helse.februar
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.EventBus
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElementTest.Companion.eksisterendeInfotrygdHistorikkelement
import no.nav.helse.serde.PersonData
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InfotrygdhistorikkTest {

    @Test
    fun `må oppfriske tom historikk`() {
        infotrygdhistorikkTest(
            setup = { historikk, eventBus, aktivitetslogg ->
                 historikk.oppfrisk(aktivitetslogg, eventBus, tidligsteDato)
            },
            assertions = { historikk, nyeBehov, _ ->
                assertEquals(1, nyeBehov.size)
                assertEquals(0, historikk.inspektør.elementer())
            }
        )
    }

    @Test
    fun `kan justere perioden for oppfrisk`() {
        infotrygdhistorikkTest(
            setup = { historikk, eventBus, aktivitetslogg ->
                historikk.oppfrisk(aktivitetslogg, eventBus, tidligsteDato)
            },
            assertions = { _, nyeBehov, _ ->
                assertEquals(1, nyeBehov.size)
                val behov = nyeBehov.single()
                assertEquals(tidligsteDato.minusYears(4), behov.periode.start)
                assertEquals(LocalDate.now(), behov.periode.endInclusive)
            }
        )

        infotrygdhistorikkTest(
            setup = { historikk, eventBus, aktivitetslogg ->
                historikk.oppfrisk(aktivitetslogg, eventBus, 1.februar)
            },
            assertions = { _, nyeBehov, _ ->
                assertEquals(1, nyeBehov.size)
                val behov = nyeBehov.single()
                assertEquals(1.februar.minusYears(4), behov.periode.start)
                assertEquals(LocalDate.now(), behov.periode.endInclusive)
            }
        )
    }

    @Test
    fun `tømme historikk - ingen data`() {
        infotrygdhistorikkTest(
            setup = { historikk, eventBus, aktivitetslogg ->
                historikk.tøm()
                historikk.oppfrisk(aktivitetslogg, eventBus, tidligsteDato)
            },
            assertions = { historikk, nyeBehov, _ ->
                assertEquals(1, nyeBehov.size)
                assertEquals(0, historikk.inspektør.elementer())
            }
        )
    }

    @Test
    fun `tømme historikk - med tom data`() {
        infotrygdhistorikkTest(
            setup = { historikk, eventBus, aktivitetslogg ->
                val tidsstempel = LocalDateTime.now()
                historikk.oppdaterHistorikk(historikkelement(oppdatert = tidsstempel))
                historikk.tøm()
                historikk.oppfrisk(aktivitetslogg, eventBus, tidligsteDato)
            },
            assertions = { historikk, nyeBehov, _ ->
                assertEquals(1, nyeBehov.size)
                assertEquals(1,historikk.inspektør.elementer())
            }
        )
    }

    @Test
    fun `tømme historikk - med ulagret data`() {
        val tidsstempel = LocalDateTime.now()

        infotrygdhistorikkTest(
            setup = { historikk, eventBus, aktivitetslogg ->
                historikk.oppdaterHistorikk(
                    historikkelement(
                        oppdatert = tidsstempel,
                        perioder = listOf(Friperiode(1.januar, 10.januar))
                    )
                )
                historikk.tøm()
                historikk.oppfrisk(aktivitetslogg, eventBus, tidligsteDato)
            },
            assertions = { historikk, nyeBehov, _ ->
                assertEquals(1, nyeBehov.size)
                assertEquals(1, historikk.inspektør.elementer())
            }
        )
    }

    @Test
    fun `tømme historikk - med flere ulagret data`() {
        val tidsstempel1 = LocalDateTime.now().minusHours(1)
        val tidsstempel2 = LocalDateTime.now()

        infotrygdhistorikkTest(
            setup = { historikk, eventBus, aktivitetslogg ->
                historikk.oppdaterHistorikk(
                    historikkelement(
                        oppdatert = tidsstempel1,
                        perioder = listOf(Friperiode(1.januar, 5.januar))
                    )
                )
                historikk.oppdaterHistorikk(
                    historikkelement(
                        oppdatert = tidsstempel2,
                        perioder = listOf(Friperiode(1.januar, 10.januar))
                    )
                )
                historikk.tøm()
                historikk.oppfrisk(aktivitetslogg, eventBus, tidligsteDato)
            },
            assertions = { historikk, nyeBehov, _ ->
                assertEquals(1, nyeBehov.size)
                assertTrue(tidsstempel2 < historikk.inspektør.opprettet(0))
                assertEquals(tidsstempel2, historikk.inspektør.oppdatert(0))
            }
        )
    }

    @Test
    fun `tømme historikk - etter lagring av tom inntektliste`() {
        infotrygdhistorikkTest(
            historikk = Infotrygdhistorikk.gjenopprett(
                InfotrygdhistorikkInnDto(
                    listOf(
                        eksisterendeInfotrygdHistorikkelement(),
                        eksisterendeInfotrygdHistorikkelement()
                    )
                )
            ),
            setup = { historikk, eventBus, aktivitetslogg ->
                historikk.tøm()
                historikk.oppfrisk(aktivitetslogg, eventBus, tidligsteDato)
            },
            assertions = { historikk, nyeBehov, _ ->
                assertEquals(1, nyeBehov.size)
                assertEquals(1, historikk.inspektør.elementer())
            }
        )
    }

    @Test
    fun `trenger ikke oppfriske gammel historikk`() {
        infotrygdhistorikkTest(
            setup = { historikk, eventBus, aktivitetslogg ->
                historikk.oppdaterHistorikk(historikkelement(oppdatert = LocalDateTime.now().minusHours(24)))
                historikk.oppfrisk(aktivitetslogg, eventBus, tidligsteDato)
            },
            assertions = { historikk, nyeBehov, _ ->
                assertEquals(1, nyeBehov.size)
                assertEquals(1, historikk.inspektør.elementer())
            }
        )
    }

    @Test
    fun `kan bestemme tidspunkt selv`() {
        infotrygdhistorikkTest(
            setup = { historikk, eventBus, aktivitetslogg ->
                val tidsstempel = LocalDateTime.now().minusHours(24)
                historikk.oppdaterHistorikk(historikkelement(oppdatert = tidsstempel))
                historikk.oppfrisk(aktivitetslogg, eventBus, tidligsteDato)
            },
            assertions = { historikk, nyeBehov, _ ->
                assertEquals(1, nyeBehov.size)
                assertEquals(1, historikk.inspektør.elementer())
            }
        )
    }

    @Test
    fun `oppfrisker ikke ny historikk`() {
        infotrygdhistorikkTest(
            setup = { historikk, eventBus, aktivitetslogg ->
                historikk.oppdaterHistorikk(historikkelement())
                historikk.oppfrisk(aktivitetslogg, eventBus, tidligsteDato)
            },
            assertions = { _, nyeBehov, _ ->
                assertEquals(1, nyeBehov.size)
            }
        )
    }

    @Test
    fun `oppdaterer tidspunkt når ny historikk er lik gammel`() {
        val nå = LocalDateTime.now()

        infotrygdhistorikkTest(
            setup = { historikk, eventBus, aktivitetslogg ->
                val perioder = listOf(
                    ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 31.januar)
                )
                val gammel = nå.minusHours(24)
                assertEquals(1.januar, historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = gammel)))
                assertNull(historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = nå)))
                historikk.oppfrisk(aktivitetslogg, eventBus, tidligsteDato)
            },
            assertions = { historikk, _, _ ->
                assertEquals(1, historikk.inspektør.elementer())
                assertTrue(nå < historikk.inspektør.opprettet(0))
                assertEquals(nå, historikk.inspektør.oppdatert(0))
            }
        )
    }

    @Test
    fun `tom utbetalingstidslinje`() {
        infotrygdhistorikkTest(
            setup = { _, _, _ -> },
            assertions = { historikk, _,_ ->
                assertTrue(historikk.utbetalingstidslinje().isEmpty())
            }
        )
    }

    @Test
    fun `utbetalingstidslinje kuttes ikke`() {
        infotrygdhistorikkTest(
            setup = { historikk, _, _ ->
                historikk.oppdaterHistorikk(
                    historikkelement(
                        listOf(
                            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 31.januar)
                        )
                    )
                )
            },
            assertions = { historikk, _,_ ->
                historikk.utbetalingstidslinje().also {
                    assertEquals(januar, it.periode())
                }
            }
        )
    }

    @Test
    fun `rekkefølge respekteres ved deserialisering`() {
        val perioder = listOf(
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 31.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.februar, 28.februar),
            Friperiode(1.mars, 31.mars)
        )
        val nå = LocalDateTime.now()
        val gjenopprettetHistorikk = Infotrygdhistorikk.gjenopprett(
            InfotrygdhistorikkInnDto(
                elementer = listOf(
                    PersonData.InfotrygdhistorikkElementData(
                        id = UUID.randomUUID(),
                        tidsstempel = nå,
                        hendelseId = UUID.randomUUID(),
                        ferieperioder = listOf(PersonData.InfotrygdhistorikkElementData.FerieperiodeData(1.mars, 31.mars)),
                        arbeidsgiverutbetalingsperioder = listOf(
                            PersonData.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData("orgnr", 1.februar, 28.februar),
                            PersonData.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData("orgnr", 1.januar, 31.januar)
                        ),
                        personutbetalingsperioder = emptyList(),
                        oppdatert = nå
                    ).tilDto()
                )
            )
        )

        infotrygdhistorikkTest(
            historikk = gjenopprettetHistorikk,
            setup = { _, _, _ -> },
            assertions = { historikk, nyeBehov, _ ->
                assertEquals(1, historikk.inspektør.elementer())
                assertNull(historikk.oppdaterHistorikk(historikkelement(perioder)))
                assertEquals(1, historikk.inspektør.elementer())
            }
        )
    }

    @Test
    fun `tom historikk validerer`() {
        infotrygdhistorikkTest(
            setup = { _, _, _ -> },
            assertions = { historikk, nyeBehov, aktivitetslogg ->
                assertTrue(historikk.validerMedFunksjonellFeil(aktivitetslogg, 1.januar til 31.januar))
                assertFalse(aktivitetslogg.harFunksjonelleFeil())
            }
        )
    }

    @Test
    fun `nyere opplysninger i Infotrygd`() {
        fun oppdater(historikk: Infotrygdhistorikk) {
            historikk.oppdaterHistorikk(
                historikkelement(
                    listOf(
                        ArbeidsgiverUtbetalingsperiode("ag1", 1.februar, 15.februar),
                        Friperiode(15.mars, 20.mars)
                    )
                )
            )
        }

        infotrygdhistorikkTest(
            setup = { historikk, _, _ ->
                oppdater(historikk)
            },
            assertions = { historikk, _, aktivitetslogg ->
                historikk.validerNyereOpplysninger(aktivitetslogg, 1.januar til 31.januar)
                assertFalse(aktivitetslogg.harFunksjonelleFeil())
                aktivitetslogg.assertVarsel(Varselkode.RV_IT_1)
            }
        )

        infotrygdhistorikkTest(
            setup = { historikk, _, _ ->
                oppdater(historikk)
            },
            assertions = { historikk, nyeBehov, aktivitetslogg ->
                assertFalse(historikk.validerMedFunksjonellFeil(aktivitetslogg, 20.februar til 28.februar))
                assertTrue(aktivitetslogg.harVarslerEllerVerre())
                aktivitetslogg.assertFunksjonellFeil(Varselkode.RV_IT_37)
            }
        )

        infotrygdhistorikkTest(
            setup = { historikk, _, _ ->
                oppdater(historikk)
            },
            assertions = { historikk, nyeBehov, aktivitetslogg ->
                assertTrue(historikk.validerMedFunksjonellFeil(aktivitetslogg, 1.mai til 5.mai))
                assertFalse(aktivitetslogg.harVarslerEllerVerre())
            }
        )
    }

    @Test
    fun skjæringstidspunkt() {
        infotrygdhistorikkTest(
            setup = { historikk, _, _ ->
                historikk.oppdaterHistorikk(
                    historikkelement(
                        listOf(
                            ArbeidsgiverUtbetalingsperiode("ag1", 5.januar, 10.januar),
                            Friperiode(11.januar, 12.januar),
                            ArbeidsgiverUtbetalingsperiode("ag2", 13.januar, 15.januar),
                            ArbeidsgiverUtbetalingsperiode("ag1", 16.januar, 20.januar),
                            ArbeidsgiverUtbetalingsperiode("ag1", 1.februar, 28.februar)
                        )
                    )
                )
            },
            assertions = { historikk, _, _ ->
                assertEquals(5.januar, historikk.skjæringstidspunkt(emptyList()).sisteOrNull(5.januar til 31.januar))
                assertEquals(1.januar, historikk.skjæringstidspunkt(listOf(2.S, 3.S)).sisteOrNull(januar))
            }
        )
    }

    private fun infotrygdhistorikkTest(
        historikk: Infotrygdhistorikk = Infotrygdhistorikk(),
        setup: (historikk: Infotrygdhistorikk, eventBus: EventBus, aktivitetslogg: Aktivitetslogg) -> Unit,
        assertions: (historikk: Infotrygdhistorikk, nyeBehov: Set<Behovsoppsamler.Behovsdetaljer.OppdatertHistorikkFraInfotrygd>, aktivitetslogg: Aktivitetslogg) -> Unit,
    ) {
        resetSeed(1.januar)
        val aktivitetslogg = Aktivitetslogg()
        val behovsoppsamler = Behovsoppsamler.opprettBehovsoppsamler()
        val eventBus = EventBus().apply {
            register(behovsoppsamler)
        }
        val behovFør = behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.OppdatertHistorikkFraInfotrygd>().toSet()
        setup(historikk, eventBus, aktivitetslogg)
        (behovsoppsamler as? Behovsoppsamler.FraAktivitetslogg)?.registrerFra(aktivitetslogg)
        val nyeBehov = behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.OppdatertHistorikkFraInfotrygd>().toSet() - behovFør

        assertions(historikk, nyeBehov, aktivitetslogg)
    }

    private fun historikkelement(
        perioder: List<Infotrygdperiode> = emptyList(),
        hendelseId: UUID = UUID.randomUUID(),
        oppdatert: LocalDateTime = LocalDateTime.now()
    ) =
        InfotrygdhistorikkElement.opprett(
            oppdatert = oppdatert,
            hendelseId = MeldingsreferanseId(hendelseId),
            perioder = perioder
        )

    private companion object {
        private val tidligsteDato = 1.januar
    }
}
