import com.google.firebase.firestore.PropertyName

data class User(
    var userId: String = "",
    var userName: String = "",
    var email: String = "",
    var totalPoints: Int = 0,
    var currentStreak: Int = 0,
    var longestStreak: Int = 0,
    var totalCompletions: Int = 0,

    @get:PropertyName("isGuest")
    @set:PropertyName("isGuest")
    var isGuest: Boolean = false
) {
    constructor() : this("", "", "", 0, 0, 0, 0, false)
}