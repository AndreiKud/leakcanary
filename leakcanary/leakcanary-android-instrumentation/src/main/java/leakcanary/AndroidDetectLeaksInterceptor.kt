package leakcanary

import android.app.Instrumentation
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import leakcanary.GcTrigger.Default
import leakcanary.HeapAnalysisDecision.NoHeapAnalysis
import leakcanary.HeapAnalysisDecision.AnalyzeHeap

class AndroidDetectLeaksInterceptor(
  private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
  private val objectWatcher: ObjectWatcher = AppWatcher.objectWatcher,
  private val retainedDelayMillis: Long = AppWatcher.retainedDelayMillis
) : DetectLeaksInterceptor {

  @Suppress("ReturnCount")
  override fun waitUntilReadyForHeapAnalysis(): HeapAnalysisDecision {
    val leakDetectionTime = SystemClock.uptimeMillis()

    if (!objectWatcher.hasWatchedObjects) {
      return NoHeapAnalysis("No watched objects.")
    }

    instrumentation.waitForIdleSync()
    if (!objectWatcher.hasWatchedObjects) {
      return NoHeapAnalysis("No watched objects after waiting for idle sync.")
    }

    Default.runGc()
    if (!objectWatcher.hasWatchedObjects) {
      return NoHeapAnalysis("No watched objects after triggering an explicit GC.")
    }

    // Waiting for any delayed UI post (e.g. scroll) to clear. This shouldn't be needed, but
    // Android simply has way too many delayed posts that aren't canceled when views are detached.
    SystemClock.sleep(2000)

    if (!objectWatcher.hasWatchedObjects) {
      return NoHeapAnalysis("No watched objects after delayed UI post is cleared.")
    }

    // Aaand we wait some more.
    // 4 seconds (2+2) is greater than the 3 seconds delay for
    // FINISH_TOKEN in android.widget.Filter
    SystemClock.sleep(2000)

    val endOfWatchDelay = retainedDelayMillis - (SystemClock.uptimeMillis() - leakDetectionTime)
    if (endOfWatchDelay > 0) {
      SystemClock.sleep(endOfWatchDelay)
    }

    Default.runGc()

    if (!objectWatcher.hasRetainedObjects) {
      return NoHeapAnalysis("No retained objects after waiting for retained delay.")
    }
    return AnalyzeHeap
  }
}
