package no.nav.helse

object Toggles {
    var replayEnabled = false
    // TODO: Ikke send ut EgenAnsatt-behovet som en del av vilkårsprøvingen når featuren er ferdig testet og togglen kan fjernes
    val vilkårshåndteringInfotrygd = System.getenv()["VILKÅRSHÅNDTERING_INFOTRYGD_FEATURE_TOGGLE"]?.toBoolean() ?: false
}
