@file:Suppress("DANGEROUS_CHARACTERS")

package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.person.BehandlingObserver
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OmfordelRefusjonsopplysningerTest {

    @Test
    fun `klarer vi å lage en behandling?`() {
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
        val endring = Behandlinger.Behandling.Endring(
            id = UUID.randomUUID(),
            tidsstempel = LocalDateTime.now(),
            sykmeldingsperiode = 1.januar til 31.januar,
            periode = januar,
            grunnlagsdata = null,
            utbetaling = null,
            dokumentsporing = Dokumentsporing.søknad(UUID.randomUUID()),
            sykdomstidslinje = Sykdomstidslinje(),
            utbetalingstidslinje = Utbetalingstidslinje(),
            refusjonstidslinje = Beløpstidslinje(),
            skjæringstidspunkt = 1.januar,
            arbeidsgiverperiode = listOf(1.januar til 16.januar),
            maksdatoresultat = Maksdatoresultat.IkkeVurdert
        )
        val behandling = Behandlinger.Behandling(
            observatører = listOf(observatør),
            tilstand = Behandlinger.Behandling.Tilstand.Uberegnet,
            endringer = mutableListOf(endring),
            avsluttet = LocalDateTime.now(),
            kilde = Behandlinger.Behandlingkilde(
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                Avsender.SYSTEM
            )
        )
        val historikk = Refusjonshistorikk()
        val refusjon = Refusjonshistorikk.Refusjon(
            meldingsreferanseId = UUID.randomUUID(),
            førsteFraværsdag = 1.januar,
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beløp = 1000.månedlig,
            sisteRefusjonsdag = null,
            endringerIRefusjon = emptyList(),
            tidsstempel = LocalDateTime.now()
        )
        historikk.leggTilRefusjon(refusjon)

        val resultat = DingsForOmfordeling(listOf(behandling), historikk).reomfordel()

        assertTrue(resultat.isEmpty())
    }

}

internal class DingsForOmfordeling(
    val behandlinger: List<Behandlinger.Behandling>,
    val historikkSomSkalFordeles: Refusjonshistorikk
) {
    fun reomfordel(): List<Beløpstidslinje> = emptyList()
}