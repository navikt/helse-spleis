package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dsl.SubsumsjonsListLog
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.etterlevelse.KontekstType
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.etterlevelse.Subsumsjonskontekst
import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.EmptyLog
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.januar
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.aktiver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.deaktiver
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
            listOf(a1Overstyrt, a1Overstyrt)
        )
        ) { "kan ikke velge mellom inntekter for samme orgnr" }

        assertEquals(
            expected, original.overstyrInntekter(
            skjæringstidspunkt,
            emptyList()
        )
        )
        assertTrue(listOf(a1Overstyrt, a2Opplysning).funksjoneltLik(original.overstyrInntekter(skjæringstidspunkt, new)))
        val forMange = listOf(a1Overstyrt, a3Overstyrt)
        assertTrue(listOf(a1Overstyrt, a2Opplysning).funksjoneltLik(original.overstyrInntekter(skjæringstidspunkt, forMange)))
    }

    @Test
    fun `ny inntektsmelding uten endring i beløp endrer kun omregnet årsinntekt for skjønnsmessig fastsatt`() {
        val skjæringstidspunkt = 1.januar
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
            new
        )
        assertTrue(expected.funksjoneltLik(actual)) { "kan ikke velge mellom inntekter for samme orgnr" }
    }

    @Test
    fun `ny inntektsmelding uten endring i beløp i forhold Skatt endrer kun omregnet årsinntekt for skjønnsmessig fastsatt`() {
        val skjæringstidspunkt = 1.januar
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
            new
        )
        assertTrue(expected.funksjoneltLik(actual))
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

internal fun List<ArbeidsgiverInntektsopplysning>.funksjoneltLik(other: List<ArbeidsgiverInntektsopplysning>): Boolean {
    if (this.size != other.size) return false
    return this
        .zip(other) { a, b -> a.funksjoneltLik(b) }
        .none { it == false }
}

internal fun ArbeidsgiverInntektsopplysning.funksjoneltLik(other: ArbeidsgiverInntektsopplysning): Boolean {
    return this.gjelder == other.gjelder && this.orgnummer == other.orgnummer && this.inntektsopplysning.funksjoneltLik(other.inntektsopplysning)
}
