package com.campusswap.app.screens.meeting

import android.content.Context
import com.campusswap.app.R
import com.campusswap.app.domain.GeoPoint
import org.json.JSONObject

data class CampusBuilding(val label: String?, val outline: List<GeoPoint>)

/**
 * Real footprints of the Universidad de los Andes campus, taken from OpenStreetMap
 * and bundled with the app so the map works with no tile server and no API key.
 */
object CampusGeometry {
    @Volatile
    private var cached: List<CampusBuilding>? = null

    fun buildings(context: Context): List<CampusBuilding> =
        cached ?: synchronized(this) { cached ?: load(context).also { cached = it } }

    private fun load(context: Context): List<CampusBuilding> = runCatching {
        val raw = context.resources.openRawResource(R.raw.campus_buildings).bufferedReader().use { it.readText() }
        val list = JSONObject(raw).getJSONArray("buildings")
        (0 until list.length()).map { i ->
            val item = list.getJSONObject(i)
            val points = item.getJSONArray("p")
            CampusBuilding(
                label = item.optString("l").takeIf { it.isNotEmpty() && it != "null" },
                outline = (0 until points.length()).map { j ->
                    val pair = points.getJSONArray(j)
                    GeoPoint(pair.getDouble(0), pair.getDouble(1))
                },
            )
        }
    }.getOrDefault(emptyList())
}
