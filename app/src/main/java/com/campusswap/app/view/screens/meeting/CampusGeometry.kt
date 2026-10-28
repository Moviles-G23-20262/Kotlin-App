package com.campusswap.app.screens.meeting

import android.content.Context
import com.campusswap.app.R
import com.campusswap.app.domain.GeoPoint
import org.json.JSONArray
import org.json.JSONObject

enum class RoadWeight { FOOTPATH, STREET, AVENUE }

data class CampusBuilding(val label: String?, val outline: List<GeoPoint>)

data class CampusRoad(val weight: RoadWeight, val line: List<GeoPoint>)

data class CampusMapData(
    val greens: List<List<GeoPoint>> = emptyList(),
    val water: List<List<GeoPoint>> = emptyList(),
    val roads: List<CampusRoad> = emptyList(),
    val buildings: List<CampusBuilding> = emptyList(),
)

/**
 * The Universidad de los Andes campus as OpenStreetMap has it: green areas, water, streets and
 * paths, and the building footprints with the block letters. It ships with the app, so the map
 * needs no tile server, no API key and no connection.
 */
object CampusGeometry {
    @Volatile
    private var cached: CampusMapData? = null

    fun load(context: Context): CampusMapData =
        cached ?: synchronized(this) { cached ?: read(context).also { cached = it } }

    private fun read(context: Context): CampusMapData = runCatching {
        val raw = context.resources.openRawResource(R.raw.campus_buildings).bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        CampusMapData(
            greens = root.getJSONArray("greens").shapes(),
            water = root.getJSONArray("water").shapes(),
            roads = root.getJSONArray("roads").map { road ->
                CampusRoad(RoadWeight.entries[road.getInt("k")], road.getJSONArray("p").points())
            },
            buildings = root.getJSONArray("buildings").map { building ->
                CampusBuilding(
                    label = building.optString("l").takeIf { it.isNotEmpty() && it != "null" },
                    outline = building.getJSONArray("p").points(),
                )
            },
        )
    }.getOrDefault(CampusMapData())

    private fun <T> JSONArray.map(item: (JSONObject) -> T) = (0 until length()).map { item(getJSONObject(it)) }

    private fun JSONArray.shapes() = map { it.getJSONArray("p").points() }

    private fun JSONArray.points() = (0 until length()).map {
        val pair = getJSONArray(it)
        GeoPoint(pair.getDouble(0), pair.getDouble(1))
    }
}
