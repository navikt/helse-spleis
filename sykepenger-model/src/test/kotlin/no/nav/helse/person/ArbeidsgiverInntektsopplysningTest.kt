package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.medInntekt
import no.nav.helse.person.Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT
import no.nav.helse.person.Inntektshistorikk.Skatt.Sykepengegrunnlag
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class ArbeidsgiverInntektsopplysningTest {

    @Test
    fun equals() {
        val inntektID = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()
        val inntektsopplysning1 = ArbeidsgiverInntektsopplysning(
            orgnummer = "orgnummer",
            inntektsopplysning = Inntektshistorikk.Infotrygd(
                id = inntektID,
                dato = 1.januar,
                hendelseId = hendelseId,
                beløp = 25000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertEquals(
            inntektsopplysning1,
            ArbeidsgiverInntektsopplysning(
                orgnummer = "orgnummer",
                inntektsopplysning = Inntektshistorikk.Infotrygd(
                    id = inntektID,
                    dato = 1.januar,
                    hendelseId = hendelseId,
                    beløp = 25000.månedlig,
                    tidsstempel = tidsstempel
                )
            )
        )
        assertNotEquals(
            inntektsopplysning1,
            ArbeidsgiverInntektsopplysning(
                orgnummer = "orgnummer2",
                inntektsopplysning = Inntektshistorikk.Infotrygd(
                    id = inntektID,
                    dato = 1.januar,
                    hendelseId = hendelseId,
                    beløp = 25000.månedlig,
                    tidsstempel = tidsstempel
                )
            )
        )
        assertNotEquals(
            inntektsopplysning1,
            ArbeidsgiverInntektsopplysning(
                orgnummer = "orgnummer",
                inntektsopplysning = Inntektshistorikk.Infotrygd(
                    id = inntektID,
                    dato = 5.januar,
                    hendelseId = hendelseId,
                    beløp = 25000.månedlig,
                    tidsstempel = tidsstempel
                )
            )
        )
    }

    @Test
    fun `setter negativt omregnet årsinntekt til 0`() {
        val arbeidsgiverInntektsopplysning = ArbeidsgiverInntektsopplysning(
            "orgnummer",
            Inntektshistorikk.SkattComposite(
                UUID.randomUUID(), inntektsopplysninger = listOf(
                    Sykepengegrunnlag(
                        dato = 1.januar,
                        hendelseId = UUID.randomUUID(),
                        beløp = (-2500).daglig,
                        måned = desember(2017),
                        type = LØNNSINNTEKT,
                        fordel = "fordel",
                        beskrivelse = "beskrivelse"
                    )
                )
            )
        )

        val økonomi = listOf(arbeidsgiverInntektsopplysning).medInntekt(
            organisasjonsnummer = "orgnummer",
            skjæringstidspunkt = 1.januar,
            dato = 1.januar,
            økonomi = Økonomi.sykdomsgrad(100.prosent),
            regler = NormalArbeidstaker,
            subsumsjonObserver = NullObserver,
            arbeidsgiverperiode = null
        )
        assertNotNull(økonomi)
        assertEquals(Inntekt.INGEN, økonomi.inspektør.dekningsgrunnlag)
        assertEquals(Inntekt.INGEN, økonomi.inspektør.aktuellDagsinntekt)
    }
}