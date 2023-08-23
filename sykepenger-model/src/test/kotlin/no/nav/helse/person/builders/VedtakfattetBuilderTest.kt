package no.nav.helse.person.builders

import java.util.UUID
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.VedtakFattetEvent.FastsattIInfotrygd
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class VedtakfattetBuilderTest {

    @Test
    fun `kaster exception dersom utbetalingsId er satt og sykepengegrunnlagsfakta er null`(){
        assertDoesNotThrow { builder(null, null).result() }
        assertDoesNotThrow { builder(UUID.randomUUID(), FastsattIInfotrygd(0.0)).result() }
        assertDoesNotThrow { builder(null, FastsattIInfotrygd(0.0)).result() }
        assertThrows<IllegalStateException> { builder(UUID.randomUUID(), null).result() }
    }
    private fun builder(utbetalingsId: UUID?, sykepengegrunnlagsfakta: PersonObserver.VedtakFattetEvent.Sykepengegrunnlagsfakta?): VedtakFattetBuilder {
        val builder = VedtakFattetBuilder("123", "123", "123", UUID.randomUUID(), 1.januar til 2.januar, emptySet(), 1.januar)
        if (utbetalingsId != null) builder.utbetalingId(utbetalingsId)
        if (sykepengegrunnlagsfakta != null) builder.sykepengegrunnlagsfakta(sykepengegrunnlagsfakta)
        return builder
    }
}