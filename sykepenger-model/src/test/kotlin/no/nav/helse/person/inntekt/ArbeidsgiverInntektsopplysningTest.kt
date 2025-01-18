package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.april
import no.nav.helse.dsl.SubsumsjonsListLog
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.etterlevelse.Bokstav
import no.nav.helse.etterlevelse.KontekstType
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.etterlevelse.Subsumsjonskontekst
import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.EmptyLog
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.januar
import no.nav.helse.person.Opptjening
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.aktiver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.deaktiver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.funksjoneltLik
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.overstyrInntekter
import no.nav.helse.person.inntekt.Skatteopplysning.Inntekttype.LØNNSINNTEKT
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ArbeidsgiverInntektsopplysningTest {

    private val subsumsjonslogg = SubsumsjonsListLog()
    private val jurist = BehandlingSubsumsjonslogg(
        subsumsjonslogg, listOf(
        Subsumsjonskontekst(KontekstType.Fødselsnummer, "fnr"),
        Subsumsjonskontekst(KontekstType.Organisasjonsnummer, "orgnr"),
        Subsumsjonskontekst(KontekstType.Vedtaksperiode, "${UUID.randomUUID()}"),
    )
    )

    @Test
    fun `overstyr inntekter`() {
        val skjæringstidspunkt = 1.januar
        val opptjening = Opptjening.nyOpptjening(emptyList(), skjæringstidspunkt)
        val a1Opplysning = ArbeidsgiverInntektsopplysning("a1", skjæringstidspunkt til LocalDate.MAX, Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig))
        val a2Opplysning = ArbeidsgiverInntektsopplysning("a2", skjæringstidspunkt til LocalDate.MAX, Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 2000.månedlig))
        val a1Overstyrt = ArbeidsgiverInntektsopplysning("a1", skjæringstidspunkt til LocalDate.MAX, Saksbehandler(skjæringstidspunkt, UUID.randomUUID(), 3000.månedlig, "", null, LocalDateTime.now()))
        val a3Overstyrt = ArbeidsgiverInntektsopplysning("a3", skjæringstidspunkt til LocalDate.MAX, Saksbehandler(skjæringstidspunkt, UUID.randomUUID(), 4000.månedlig, "", null, LocalDateTime.now()))

        val original = listOf(a1Opplysning, a2Opplysning)
        val expected = listOf(a1Opplysning, a2Opplysning)
        val new = listOf(a1Overstyrt)

        assertEquals(
            expected, original.overstyrInntekter(
            skjæringstidspunkt,
            opptjening,
            listOf(a1Overstyrt, a1Overstyrt),
            EmptyLog
        )
        ) { "kan ikke velge mellom inntekter for samme orgnr" }

        assertEquals(
            expected, original.overstyrInntekter(
            skjæringstidspunkt,
            opptjening,
            emptyList(),
            EmptyLog
        )
        )
        assertTrue(listOf(a1Overstyrt, a2Opplysning).funksjoneltLik(original.overstyrInntekter(skjæringstidspunkt, opptjening, new, EmptyLog)))
        val forMange = listOf(a1Overstyrt, a3Overstyrt)
        assertTrue(listOf(a1Overstyrt, a2Opplysning).funksjoneltLik(original.overstyrInntekter(skjæringstidspunkt, opptjening, forMange, EmptyLog)))
    }

    @Test
    fun `ny inntektsmelding uten endring i beløp endrer kun omregnet årsinntekt for skjønnsmessig fastsatt`() {
        val skjæringstidspunkt = 1.januar
        val opptjening = Opptjening.nyOpptjening(emptyList(), skjæringstidspunkt)
        val inntektsmeldinginntektA1 = Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig)
        val inntektsmeldinginntektA2 = Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 2000.månedlig)
        val inntektsmeldinginntektA3 = Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 3000.månedlig)

        val inntektsmeldinginntektA1Ny = Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig)
        val overstyrtA1Opplysning = ArbeidsgiverInntektsopplysning("a1", skjæringstidspunkt til LocalDate.MAX, Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig))
        val forventetA1Opplysning = ArbeidsgiverInntektsopplysning("a1", skjæringstidspunkt til LocalDate.MAX, skjønnsmessigFastsatt(skjæringstidspunkt, 900.månedlig, inntektsmeldinginntektA1Ny))

        val a1Opplysning = ArbeidsgiverInntektsopplysning("a1", skjæringstidspunkt til LocalDate.MAX, skjønnsmessigFastsatt(skjæringstidspunkt, 900.månedlig, inntektsmeldinginntektA1))
        val a2Opplysning = ArbeidsgiverInntektsopplysning("a2", skjæringstidspunkt til LocalDate.MAX, skjønnsmessigFastsatt(skjæringstidspunkt, 950.månedlig, inntektsmeldinginntektA2))
        val a3Opplysning = ArbeidsgiverInntektsopplysning("a3", skjæringstidspunkt til LocalDate.MAX, skjønnsmessigFastsatt(skjæringstidspunkt, 975.månedlig, inntektsmeldinginntektA3))

        val original = listOf(a1Opplysning, a2Opplysning, a3Opplysning)
        val expected = listOf(forventetA1Opplysning, a2Opplysning, a3Opplysning)
        val new = listOf(overstyrtA1Opplysning)

        val actual = original.overstyrInntekter(
            skjæringstidspunkt,
            opptjening,
            new,
            EmptyLog
        )
        assertTrue(expected.funksjoneltLik(actual)) { "kan ikke velge mellom inntekter for samme orgnr" }
    }

    @Test
    fun `ny inntektsmelding uten endring i beløp i forhold Skatt endrer kun omregnet årsinntekt for skjønnsmessig fastsatt`() {
        val skjæringstidspunkt = 1.januar
        val opptjening = Opptjening.nyOpptjening(emptyList(), skjæringstidspunkt)
        val skattA1 = skattSykepengegrunnlag(
            UUID.randomUUID(), skjæringstidspunkt, listOf(
            Skatteopplysning(UUID.randomUUID(), 1000.månedlig, skjæringstidspunkt.minusMonths(1).yearMonth, LØNNSINNTEKT, "", ""),
            Skatteopplysning(UUID.randomUUID(), 1000.månedlig, skjæringstidspunkt.minusMonths(2).yearMonth, LØNNSINNTEKT, "", ""),
            Skatteopplysning(UUID.randomUUID(), 1000.månedlig, skjæringstidspunkt.minusMonths(3).yearMonth, LØNNSINNTEKT, "", "")
        ), emptyList())
        val inntektsmeldinginntektA2 = Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 2000.månedlig)
        val inntektsmeldinginntektA3 = Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 3000.månedlig)

        val inntektsmeldinginntektA1Ny = Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig)
        val overstyrtA1Opplysning = ArbeidsgiverInntektsopplysning("a1", skjæringstidspunkt til LocalDate.MAX, Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig))
        val forventetA1Opplysning = ArbeidsgiverInntektsopplysning("a1", skjæringstidspunkt til LocalDate.MAX, skjønnsmessigFastsatt(skjæringstidspunkt, 900.månedlig, inntektsmeldinginntektA1Ny))

        val a1Opplysning = ArbeidsgiverInntektsopplysning("a1", skjæringstidspunkt til LocalDate.MAX, skjønnsmessigFastsatt(skjæringstidspunkt, 900.månedlig, skattA1))
        val a2Opplysning = ArbeidsgiverInntektsopplysning("a2", skjæringstidspunkt til LocalDate.MAX, skjønnsmessigFastsatt(skjæringstidspunkt, 950.månedlig, inntektsmeldinginntektA2))
        val a3Opplysning = ArbeidsgiverInntektsopplysning("a3", skjæringstidspunkt til LocalDate.MAX, skjønnsmessigFastsatt(skjæringstidspunkt, 975.månedlig, inntektsmeldinginntektA3))

        val original = listOf(a1Opplysning, a2Opplysning, a3Opplysning)
        val expected = listOf(forventetA1Opplysning, a2Opplysning, a3Opplysning)
        val new = listOf(overstyrtA1Opplysning)

        val actual = original.overstyrInntekter(
            skjæringstidspunkt,
            opptjening,
            new,
            EmptyLog
        )
        assertTrue(expected.funksjoneltLik(actual))
    }

    @Test
    fun `subsummerer etter overstyring`() {
        val skjæringstidspunkt = 1.april
        val ansattFom = 1.januar
        val orgnummer = "a1"

        val opptjening = Opptjening.nyOpptjening(
            listOf(
                Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                    orgnummer, listOf(
                    Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold(
                        ansattFom,
                        null,
                        false
                    )
                )
                )
            ), skjæringstidspunkt
        )

        val paragraf = Paragraf.PARAGRAF_8_28
        val ledd = Ledd.LEDD_3
        val bokstav = Bokstav.BOKSTAV_B
        val overstyrtBeløp = 3000.månedlig

        val subsumsjon = Subsumsjon(paragraf.ref, ledd.nummer, bokstav.ref.toString())
        val a1Opplysning = ArbeidsgiverInntektsopplysning(orgnummer, skjæringstidspunkt til LocalDate.MAX, Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig))
        val a1Overstyrt = ArbeidsgiverInntektsopplysning(orgnummer, skjæringstidspunkt til LocalDate.MAX, Saksbehandler(skjæringstidspunkt, UUID.randomUUID(), overstyrtBeløp, "Jeg bare måtte gjøre det", subsumsjon, LocalDateTime.now()))

        listOf(a1Opplysning).overstyrInntekter(skjæringstidspunkt, opptjening, listOf(a1Overstyrt), jurist)
        SubsumsjonInspektør(subsumsjonslogg).assertBeregnet(
            paragraf = paragraf,
            versjon = LocalDate.of(2019, 1, 1),
            ledd = ledd,
            punktum = null,
            bokstav = bokstav,
            input = mapOf(
                "organisasjonsnummer" to orgnummer,
                "skjæringstidspunkt" to skjæringstidspunkt,
                "startdatoArbeidsforhold" to ansattFom,
                "overstyrtInntektFraSaksbehandler" to mapOf("dato" to skjæringstidspunkt, "beløp" to overstyrtBeløp.månedlig),
                "forklaring" to "Jeg bare måtte gjøre det"
            ),
            output = mapOf(
                "beregnetGrunnlagForSykepengegrunnlagPrÅr" to overstyrtBeløp.årlig,
                "beregnetGrunnlagForSykepengegrunnlagPrMåned" to overstyrtBeløp.månedlig
            )
        )
    }

    @Test
    fun `deaktiverer en inntekt`() {
        val skjæringstidspunkt = 1.januar
        val a1Opplysning = ArbeidsgiverInntektsopplysning("a1", skjæringstidspunkt til LocalDate.MAX, Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig))
        val a2Opplysning = ArbeidsgiverInntektsopplysning("a2", skjæringstidspunkt til LocalDate.MAX, IkkeRapportert(skjæringstidspunkt, UUID.randomUUID()))

        val opprinnelig = listOf(a1Opplysning, a2Opplysning)
        val (aktive, deaktiverte) = opprinnelig.deaktiver(emptyList(), "a2", "Denne må bort", EmptyLog)
        assertEquals(a1Opplysning, aktive.single())
        assertEquals(a2Opplysning, deaktiverte.single())

        val (nyDeaktivert, nyAktivert) = deaktiverte.aktiver(aktive, "a2", "Jeg gjorde en feil, jeg angrer!", EmptyLog)
        assertEquals(0, nyDeaktivert.size)
        assertTrue(opprinnelig.funksjoneltLik(nyAktivert))

        assertThrows<RuntimeException> { opprinnelig.deaktiver(emptyList(), "a3", "jeg vil deaktivere noe som ikke finnes", EmptyLog) }
        assertThrows<RuntimeException> { emptyList<ArbeidsgiverInntektsopplysning>().aktiver(opprinnelig, "a3", "jeg vil aktivere noe som ikke finnes", EmptyLog) }
    }

    @Test
    fun `subsummerer deaktivering`() {
        val skjæringstidspunkt = 1.januar
        val a1Opplysning = ArbeidsgiverInntektsopplysning("a1", skjæringstidspunkt til LocalDate.MAX, Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig))
        val a2Opplysning = ArbeidsgiverInntektsopplysning("a2", skjæringstidspunkt til LocalDate.MAX, IkkeRapportert(skjæringstidspunkt, UUID.randomUUID()))

        val opprinnelig = listOf(a1Opplysning, a2Opplysning)
        val (aktive, deaktiverte) = opprinnelig.deaktiver(emptyList(), "a2", "Denne må bort", jurist)
        assertEquals(a1Opplysning, aktive.single())
        assertEquals(a2Opplysning, deaktiverte.single())
        SubsumsjonInspektør(subsumsjonslogg).assertOppfylt(
            paragraf = Paragraf.PARAGRAF_8_15,
            versjon = LocalDate.of(1998, 12, 18),
            ledd = null,
            punktum = null,
            bokstav = null,
            input = mapOf(
                "organisasjonsnummer" to "a2",
                "skjæringstidspunkt" to skjæringstidspunkt,
                "inntekterSisteTreMåneder" to emptyList<Any>(),
                "forklaring" to "Denne må bort"
            ),
            output = mapOf("arbeidsforholdAvbrutt" to "a2")
        )
    }

    @Test
    fun `subsummerer aktivering`() {
        val skjæringstidspunkt = 1.januar
        val a1Opplysning = ArbeidsgiverInntektsopplysning("a1", skjæringstidspunkt til LocalDate.MAX, Inntektsmeldinginntekt(skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig))
        val a2Opplysning = ArbeidsgiverInntektsopplysning("a2", skjæringstidspunkt til LocalDate.MAX, IkkeRapportert(skjæringstidspunkt, UUID.randomUUID()))

        val opprinneligAktive = listOf(a1Opplysning)
        val opprinneligDeaktiverte = listOf(a2Opplysning)

        val (deaktiverte, aktive) = opprinneligDeaktiverte.aktiver(opprinneligAktive, "a2", "Denne må tilbake", jurist)
        assertTrue(listOf(a1Opplysning, a2Opplysning).funksjoneltLik(aktive))
        assertEquals(0, deaktiverte.size)
        SubsumsjonInspektør(subsumsjonslogg).assertIkkeOppfylt(
            paragraf = Paragraf.PARAGRAF_8_15,
            versjon = LocalDate.of(1998, 12, 18),
            ledd = null,
            punktum = null,
            bokstav = null,
            input = mapOf(
                "organisasjonsnummer" to "a2",
                "skjæringstidspunkt" to skjæringstidspunkt,
                "inntekterSisteTreMåneder" to emptyList<Any>(),
                "forklaring" to "Denne må tilbake"
            ),
            output = mapOf("aktivtArbeidsforhold" to "a2")
        )
    }

    @Test
    fun equals() {
        val inntektID = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()
        val inntektsopplysning1 = ArbeidsgiverInntektsopplysning(
            orgnummer = "orgnummer",
            gjelder = 1.januar til LocalDate.MAX,
            inntektsopplysning = infotrygd(
                id = inntektID,
                dato = 1.januar,
                hendelseId = hendelseId,
                beløp = 25000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertTrue(
            inntektsopplysning1.funksjoneltLik(
                ArbeidsgiverInntektsopplysning(
                    orgnummer = "orgnummer",
                    gjelder = 1.januar til LocalDate.MAX,
                    inntektsopplysning = infotrygd(
                        id = inntektID,
                        dato = 1.januar,
                        hendelseId = hendelseId,
                        beløp = 25000.månedlig,
                        tidsstempel = tidsstempel
                    )
                )
            )
        )
        assertFalse(
            inntektsopplysning1.funksjoneltLik(
                ArbeidsgiverInntektsopplysning(
                    orgnummer = "orgnummer2",
                    gjelder = 1.januar til LocalDate.MAX,
                    inntektsopplysning = infotrygd(
                        id = inntektID,
                        dato = 1.januar,
                        hendelseId = hendelseId,
                        beløp = 25000.månedlig,
                        tidsstempel = tidsstempel
                    )
                )
            )
        )
        assertFalse(
            inntektsopplysning1.funksjoneltLik(
                ArbeidsgiverInntektsopplysning(
                    orgnummer = "orgnummer",
                    gjelder = 1.januar til LocalDate.MAX,
                    inntektsopplysning = infotrygd(
                        id = inntektID,
                        dato = 5.januar,
                        hendelseId = hendelseId,
                        beløp = 25000.månedlig,
                        tidsstempel = tidsstempel
                    )
                )
            )
        )
    }
}
