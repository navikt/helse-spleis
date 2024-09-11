
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource

class AftereEachTestOpenSpannerExtension : AfterEachCallback, CloseableResource {

    @Throws(Exception::class)
    override fun afterEach(context: ExtensionContext) {
        val testWatcher = SpannerEtterTestInterceptor()
        try {
                testWatcher.openTheSpanner(context)
                // The following line registers a callback hook when the root test context is
                // shut down
                context.root.getStore(GLOBAL).put("any unique name", this)
        } catch (e: Exception) {
            testWatcher.openTheSpanner(context, e.cause?.message)
        }
    }

    override fun close() {

    }
}