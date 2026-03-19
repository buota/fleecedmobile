package com.calpoly.fleecedlogin.model

enum class Rank(val title: String, val minPoints: Int) {
    ROOKIE("Rookie", 0),
    PRACTICE_SQUAD("Practice Squad", 150),
    STARTER("Starter", 300),
    PRO_BOWLER("Pro Bowler", 450),
    ALL_PRO("All-Pro", 600),
    HALL_OF_FAMER("Hall of Famer", 750)
}

// Special positional ranks awarded by leaderboard position
const val RANK_MVP = "MVP"       // #1 on Seasonal leaderboard
const val RANK_GOAT = "GOAT"     // #1 on All Time leaderboard (overrides MVP)

fun getRankForPoints(points: Int): Rank {
    return Rank.entries.lastOrNull { points >= it.minPoints } ?: Rank.ROOKIE
}
