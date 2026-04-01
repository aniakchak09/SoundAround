package licenta.soundaround.map.presentation

enum class MatchFilter(val label: String, val minScore: Float) {
    ALL("All", 0f),
    SOME("Any overlap", 0.05f),
    GOOD("Good", 0.15f),
    GREAT("Great", 0.30f),
    TOP("Top match", 0.55f)
}
