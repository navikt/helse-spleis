package no.nav.helse

object Toggles {
    var replayEnabled = false
    val vilkårshåndteringInfotrygd = System.getenv()["VILKARSHANDTERING_INFOTRYGD_FEATURE_TOGGLE"]?.toBoolean() ?: false
}
