@file:Suppress("DANGEROUS_CHARACTERS")

package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.person.BehandlingObserver
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.beløpstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class OmfordelRefusjonsopplysningerTest {
    val observatør = object : BehandlingObserver {
        override fun avsluttetUtenVedtak(
            hendelse: IAktivitetslogg,
            behandlingId: UUID,
            tidsstempel: LocalDateTime,
            periode: Periode,
            dokumentsporing: Set<UUID>
        ) {

        }

        override fun vedtakIverksatt(
            hendelse: IAktivitetslogg,
            vedtakFattetTidspunkt: LocalDateTime,
            behandling: Behandlinger.Behandling
        ) {
        }

        override fun vedtakAnnullert(hendelse: IAktivitetslogg, behandlingId: UUID) {
        }

        override fun behandlingLukket(behandlingId: UUID) {
        }

        override fun behandlingForkastet(behandlingId: UUID, hendelse: Hendelse) {
        }

        override fun nyBehandling(
            id: UUID,
            periode: Periode,
            meldingsreferanseId: UUID,
            innsendt: LocalDateTime,
            registert: LocalDateTime,
            avsender: Avsender,
            type: PersonObserver.BehandlingOpprettetEvent.Type,
            søknadIder: Set<UUID>
        ) {
        }

        override fun utkastTilVedtak(utkastTilVedtak: PersonObserver.UtkastTilVedtakEvent) {
        }
    }
    val etTimestamp = LocalDateTime.now()
    private fun endring(periode: Periode, arbeidsgiverPeriode: Periode, skjæringstidspunkt: LocalDate = periode.start) = Behandlinger.Behandling.Endring(
        id = UUID.randomUUID(),
        tidsstempel = etTimestamp,
        sykmeldingsperiode = periode,
        periode = periode,
        grunnlagsdata = null,
        utbetaling = null,
        dokumentsporing = Dokumentsporing.søknad(UUID.randomUUID()),
        sykdomstidslinje = Sykdomstidslinje(),
        utbetalingstidslinje = Utbetalingstidslinje(),
        refusjonstidslinje = Beløpstidslinje(),
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgiverperiode = listOf(arbeidsgiverPeriode),
        maksdatoresultat = Maksdatoresultat.IkkeVurdert)

    private fun behandling(endring: Behandlinger.Behandling.Endring) = Behandlinger.Behandling(
        observatører = listOf(observatør),
        tilstand = Behandlinger.Behandling.Tilstand.Uberegnet,
        endringer = mutableListOf(endring),
        avsluttet = etTimestamp,
        kilde = Behandlinger.Behandlingkilde(
            UUID.randomUUID(),
            etTimestamp,
            etTimestamp,
            Avsender.SYSTEM
        )
    )

    private fun refusjon(meldingsref: UUID, førsteFraværsdag: LocalDate, arbeidsgiverPeriode: Periode, beløp: Inntekt) = Refusjonshistorikk.Refusjon(
        meldingsreferanseId = meldingsref,
        førsteFraværsdag = førsteFraværsdag,
        arbeidsgiverperioder = listOf(arbeidsgiverPeriode),
        beløp = beløp,
        sisteRefusjonsdag = null,
        endringerIRefusjon = emptyList(),
        tidsstempel = etTimestamp
    )

    @Test
    fun `klarer vi å lage en behandling?`() {
        val endring = endring(januar, 1.januar til 16.januar)
        val behandling = behandling(endring)
        val historikk = Refusjonshistorikk()
        val imUUID = UUID.randomUUID()
        val refusjon = refusjon(imUUID, 1.januar, 1.januar til 16.januar, 1000.månedlig)
        historikk.leggTilRefusjon(refusjon)

        val resultat = DingsForOmfordeling(listOf(behandling), historikk).reomfordel()

        assertEquals(
            DingsForOmfordeling.OmfordelteRefusjonstidslinjer(
                omplasserteRefusjonstidslinjer = mapOf(
                    behandling to Beløpstidslinje.fra(
                        januar,
                        1000.månedlig,
                        Kilde(imUUID, Avsender.ARBEIDSGIVER, etTimestamp)
                    )
                ), rest = null
            ),
            resultat
        )
    }

    @Disabled("nei, denne virker ikke i det hele tatt, fordi vi sliter med å få rett beløpstidslinje fra refusjonshistorikk")
    @Test
    fun `hva med to behandlinger?`() {
        val endringJan = endring(januar, 1.januar til 16.januar)
        val behandlingJan = behandling(endringJan)
        val endringFebruar = endring(februar, 1.januar til 16.januar, skjæringstidspunkt = 1.januar)
        val behandlingFebruar = behandling(endringFebruar)
        val historikk = Refusjonshistorikk()
        val imUUID = UUID.randomUUID()
        val refusjon = refusjon(imUUID, 1.januar, 1.januar til 16.januar, 1000.månedlig)
        historikk.leggTilRefusjon(refusjon)

        val resultat = DingsForOmfordeling(listOf(behandlingJan, behandlingFebruar), historikk).reomfordel()
        val ønsketResultat = DingsForOmfordeling.OmfordelteRefusjonstidslinjer(
            omplasserteRefusjonstidslinjer = mapOf(
                behandlingJan to Beløpstidslinje.fra(
                    januar,
                    1000.månedlig,
                    Kilde(imUUID, Avsender.ARBEIDSGIVER, etTimestamp)
                ),
                behandlingFebruar to Beløpstidslinje.fra(
                    februar,
                    1000.månedlig,
                    Kilde(imUUID, Avsender.ARBEIDSGIVER, etTimestamp)
                )
            ), rest = null
        )
        assertEquals(ønsketResultat, resultat)
    }

}

/**
 * Klarer å finne ut av hvilke refusjonsopplysninger som hører til hvile behandinger.
 *
 * Og hvilke refusjonsopplysninger som ikke hører til noenting.
 */
internal class DingsForOmfordeling(
    val sisteBehandlingIHverVedtaksperiode: List<Behandlinger.Behandling>,
    val historikkSomSkalFordeles: Refusjonshistorikk
) {
    fun reomfordel(): OmfordelteRefusjonstidslinjer {
        val beløpstidslinjerPerBehandling = refusjonTilBeløpstidslinjer()
        return OmfordelteRefusjonstidslinjer(beløpstidslinjerPerBehandling, null)
    }

    fun refusjonTilBeløpstidslinjer(): Map<Behandlinger.Behandling, Beløpstidslinje> =
        sisteBehandlingIHverVedtaksperiode.map { sisteBehandlingForÉnVedtaksperiode ->
            sisteBehandlingForÉnVedtaksperiode to (sisteBehandlingForÉnVedtaksperiode.skjæringstidspunkt til sisteBehandlingForÉnVedtaksperiode.periode().endInclusive)
        }.associate { (behandling, søkevindu) ->
            behandling to historikkSomSkalFordeles.beløpstidslinje(søkevindu)
        }

    data class OmfordelteRefusjonstidslinjer(
        /* en omplassert refusjonstidslinje er et map fra behandling til en liste
        * med dager med refusjonsbeløp
        * slik at første dag er første dag i en gitt vedtaksperiode
        * og siste dag er siste dag i den samme vedtaksperioden.
        *
        * right?
        * */
        val omplasserteRefusjonstidslinjer: Map<Behandlinger.Behandling, Beløpstidslinje>,
        /*
        dette et er refusjonsobjekt som inneholder de refusjonsopplysningene vi ikke
        fikk plass til i noen av de relevante behandlingene
         */
        val rest: Refusjonshistorikk.Refusjon?
    )
}