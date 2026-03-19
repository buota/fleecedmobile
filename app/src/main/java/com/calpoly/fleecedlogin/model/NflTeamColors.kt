package com.calpoly.fleecedlogin.model

import androidx.compose.ui.graphics.Color

/**
 * NFL team colors for jersey
 */
object NflTeamColors {
    private val primaryByAbbreviation = mapOf(
        "ARI" to Color(0xFF97233F),
        "ATL" to Color(0xFFA71930),
        "BAL" to Color(0xFF241773),
        "BUF" to Color(0xFF00338D),
        "CAR" to Color(0xFF0085CA),
        "CHI" to Color(0xFF0B162A),
        "CIN" to Color(0xFFFB4F14),
        "CLE" to Color(0xFF311D00),
        "DAL" to Color(0xFF003594),
        "DEN" to Color(0xFFFB4F14),
        "DET" to Color(0xFF0076B6),
        "GB" to Color(0xFF203731),
        "HOU" to Color(0xFF03202F),
        "IND" to Color(0xFF002C5F),
        "JAX" to Color(0xFF006778),
        "KC" to Color(0xFFE31837),
        "LV" to Color(0xFF000000),
        "LAC" to Color(0xFF0080C6),
        "LAR" to Color(0xFF003594),
        "MIA" to Color(0xFF008E97),
        "MIN" to Color(0xFF4F2683),
        "NE" to Color(0xFF002244),
        "NO" to Color(0xFFD3BC8D),
        "NYG" to Color(0xFF0B2265),
        "NYJ" to Color(0xFF125740),
        "PHI" to Color(0xFF004C54),
        "PIT" to Color(0xFFFFB612),
        "SF" to Color(0xFFAA0000),
        "SEA" to Color(0xFF002244),
        "TB" to Color(0xFFD50A0A),
        "TEN" to Color(0xFF0C2340),
        "WAS" to Color(0xFF5A1414)
    )

    private val aliasToAbbreviation = mapOf(
        "ARIZONA" to "ARI",
        "ARIZONA CARDINALS" to "ARI",
        "CARDINALS" to "ARI",
        "ARI" to "ARI",

        "ATLANTA" to "ATL",
        "ATLANTA FALCONS" to "ATL",
        "FALCONS" to "ATL",
        "ATL" to "ATL",

        "BALTIMORE" to "BAL",
        "BALTIMORE RAVENS" to "BAL",
        "RAVENS" to "BAL",
        "BAL" to "BAL",

        "BUFFALO" to "BUF",
        "BUFFALO BILLS" to "BUF",
        "BILLS" to "BUF",
        "BUF" to "BUF",

        "CAROLINA" to "CAR",
        "CAROLINA PANTHERS" to "CAR",
        "PANTHERS" to "CAR",
        "CAR" to "CAR",

        "CHICAGO" to "CHI",
        "CHICAGO BEARS" to "CHI",
        "BEARS" to "CHI",
        "CHI" to "CHI",

        "CINCINNATI" to "CIN",
        "CINCINNATI BENGALS" to "CIN",
        "BENGALS" to "CIN",
        "CIN" to "CIN",

        "CLEVELAND" to "CLE",
        "CLEVELAND BROWNS" to "CLE",
        "BROWNS" to "CLE",
        "CLE" to "CLE",

        "DALLAS" to "DAL",
        "DALLAS COWBOYS" to "DAL",
        "COWBOYS" to "DAL",
        "DAL" to "DAL",

        "DENVER" to "DEN",
        "DENVER BRONCOS" to "DEN",
        "BRONCOS" to "DEN",
        "DEN" to "DEN",

        "DETROIT" to "DET",
        "DETROIT LIONS" to "DET",
        "LIONS" to "DET",
        "DET" to "DET",

        "GREEN BAY" to "GB",
        "GREEN BAY PACKERS" to "GB",
        "PACKERS" to "GB",
        "GB" to "GB",

        "HOUSTON" to "HOU",
        "HOUSTON TEXANS" to "HOU",
        "TEXANS" to "HOU",
        "HOU" to "HOU",

        "INDIANAPOLIS" to "IND",
        "INDIANAPOLIS COLTS" to "IND",
        "COLTS" to "IND",
        "IND" to "IND",

        "JACKSONVILLE" to "JAX",
        "JACKSONVILLE JAGUARS" to "JAX",
        "JAGUARS" to "JAX",
        "JAGS" to "JAX",
        "JAX" to "JAX",

        "KANSAS CITY" to "KC",
        "KANSAS CITY CHIEFS" to "KC",
        "CHIEFS" to "KC",
        "KC" to "KC",

        "LAS VEGAS" to "LV",
        "LAS VEGAS RAIDERS" to "LV",
        "RAIDERS" to "LV",
        "LV" to "LV",

        "LOS ANGELES CHARGERS" to "LAC",
        "CHARGERS" to "LAC",
        "LAC" to "LAC",

        "LOS ANGELES RAMS" to "LAR",
        "RAMS" to "LAR",
        "LAR" to "LAR",

        "MIAMI" to "MIA",
        "MIAMI DOLPHINS" to "MIA",
        "DOLPHINS" to "MIA",
        "MIA" to "MIA",

        "MINNESOTA" to "MIN",
        "MINNESOTA VIKINGS" to "MIN",
        "VIKINGS" to "MIN",
        "MIN" to "MIN",

        "NEW ENGLAND" to "NE",
        "NEW ENGLAND PATRIOTS" to "NE",
        "PATRIOTS" to "NE",
        "NE" to "NE",

        "NEW ORLEANS" to "NO",
        "NEW ORLEANS SAINTS" to "NO",
        "SAINTS" to "NO",
        "NO" to "NO",

        "NEW YORK GIANTS" to "NYG",
        "GIANTS" to "NYG",
        "NYG" to "NYG",

        "NEW YORK JETS" to "NYJ",
        "JETS" to "NYJ",
        "NYJ" to "NYJ",

        "PHILADELPHIA" to "PHI",
        "PHILADELPHIA EAGLES" to "PHI",
        "EAGLES" to "PHI",
        "PHI" to "PHI",

        "PITTSBURGH" to "PIT",
        "PITTSBURGH STEELERS" to "PIT",
        "STEELERS" to "PIT",
        "PIT" to "PIT",

        "SAN FRANCISCO" to "SF",
        "SAN FRANCISCO 49ERS" to "SF",
        "49ERS" to "SF",
        "NINERS" to "SF",
        "SF" to "SF",

        "SEATTLE" to "SEA",
        "SEATTLE SEAHAWKS" to "SEA",
        "SEAHAWKS" to "SEA",
        "SEA" to "SEA",

        "TAMPA BAY" to "TB",
        "TAMPA BAY BUCCANEERS" to "TB",
        "BUCCANEERS" to "TB",
        "BUCS" to "TB",
        "TB" to "TB",

        "TENNESSEE" to "TEN",
        "TENNESSEE TITANS" to "TEN",
        "TITANS" to "TEN",
        "TEN" to "TEN",

        "WASHINGTON" to "WAS",
        "WASHINGTON COMMANDERS" to "WAS",
        "WASHINGTON FOOTBALL TEAM" to "WAS",
        "COMMANDERS" to "WAS",
        "WFT" to "WAS",
        "WAS" to "WAS"
    )

    private fun normalizeTeam(value: String): String {
        return value
            .uppercase()
            .replace(".", " ")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun primary(teamName: String): Color {
        val normalized = normalizeTeam(teamName)
        val abbr = aliasToAbbreviation[normalized] ?: normalized
        return primaryByAbbreviation[abbr] ?: Color(0xFF5B7FD4)
    }
}
