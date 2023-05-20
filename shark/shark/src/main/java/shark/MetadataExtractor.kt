package shark

/**
 * Extracts metadata from a hprof to be reported in [HeapAnalysisSuccess.metadata].
 *
 * This is a functional interface with which you can create a [MetadataExtractor] from a lambda.
 */
fun interface MetadataExtractor {
  fun extractMetadata(graph: HeapGraph): Map<String, String>

  companion object {

    /**
     * A no-op [MetadataExtractor]
     */
    val NO_OP = MetadataExtractor { emptyMap() }
  }
}
