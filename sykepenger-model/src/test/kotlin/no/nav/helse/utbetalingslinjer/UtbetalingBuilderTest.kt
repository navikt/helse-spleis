package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Inntektshistorikk.Inntektsopplysning
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.Inntekter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjerFilter
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingBuilderTest {

    private lateinit var aktivitetslogg: Aktivitetslogg
    private val utbetalingstidslinjer = mutableMapOf<String, Utbetalingstidslinje>()
    private val utbetalinger = mutableMapOf<UUID, Utbetaling>()

    @BeforeEach
    internal fun clear() {
        aktivitetslogg = Aktivitetslogg()
        utbetalingstidslinjer.clear()
        utbetalinger.clear()
    }

    @Test
    fun `utbetalinger for en arbeidsgiver`() {
        nyBuilder(1.januar til 31.januar)
            .arbeidsgiver(
                organisasjonsnummer = arbeidsgiver1,
                sykdomstidslinje = 31.S,
                inntekter = inntekter(
                    inntektsmelding(1.januar, 1200.daglig)
                )
            )
            .vedtaksperiode(
                vedtaksperiodeId = vedtaksperiode1,
                organisasjonsnummer = arbeidsgiver1
            )
            .build()

        assertNotNull(utbetalinger.getValue(vedtaksperiode1))
        assertNotNull(utbetalingstidslinjer.getValue(arbeidsgiver1))
    }

    private fun nyBuilder(
        periode: Periode, infotrygdHistorikk:
        Infotrygdhistorikk = Infotrygdhistorikk()
    ) = Utbetaling.Builder(aktivitetslogg, periode)
        .fødselsnummer(fødselsnummer)
        .subsumsjonsObserver(subsumsjonObserver)
        .avvisDagerEtterDødsdatofilter(dummyUtbetalingstidslinjerFilter)
        .avvisInngangsvilkårfilter(dummyUtbetalingstidslinjerFilter)
        .infotrygdhistorikk(infotrygdHistorikk)

    private fun Utbetaling.Builder.arbeidsgiver(
        organisasjonsnummer: String,
        sykdomstidslinje: Sykdomstidslinje,
        inntekter: Inntekter,
        utbetalinger: List<Utbetaling> = emptyList(),
        refusjonshistorikk: Refusjonshistorikk = Refusjonshistorikk()
    ) = apply {
        arbeidsgiver(organisasjonsnummer, sykdomstidslinje, inntekter, utbetalinger, refusjonshistorikk) { utbetalingstidslinje ->
            utbetalingstidslinjer[organisasjonsnummer] = utbetalingstidslinje
            UUID.randomUUID()
        }
    }

    private fun Utbetaling.Builder.vedtaksperiode(
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
        sisteUtbetaling: Utbetaling? = null,
    ) = apply {
        vedtaksperiode(vedtaksperiodeId, organisasjonsnummer, sisteUtbetaling) { utbetaling ->
            utbetalinger[vedtaksperiodeId] = utbetaling
        }
    }

    private companion object {
        private val dummyUtbetalingstidslinjerFilter = object : UtbetalingstidslinjerFilter {
            override fun filter(
                tidslinjer: List<Utbetalingstidslinje>,
                periode: Periode,
                aktivitetslogg: IAktivitetslogg,
                subsumsjonObserver: SubsumsjonObserver
            ) = tidslinjer
        }
        private val subsumsjonObserver = MaskinellJurist()

        private val fødselsnummer = Fødselsnummer.tilFødselsnummer("12029240045")
        private val arbeidsgiver1 = "987654321"
        private val vedtaksperiode1 = UUID.fromString("a0ec1467-4ac1-46a9-b131-436c6befc9a5")

        private fun inntektsmelding(skjæringstidspunkt: LocalDate, inntekt: Inntekt) =
            skjæringstidspunkt to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), inntekt)

        private fun inntekter(
            vararg inntektsopplysninger: Pair<LocalDate, Inntektsopplysning>
        ) = Inntekter(
            skjæringstidspunkter = inntektsopplysninger.map { it.first },
            inntektPerSkjæringstidspunkt = inntektsopplysninger.toMap(),
            regler = NormalArbeidstaker,
            subsumsjonObserver = subsumsjonObserver
        )
    }
}