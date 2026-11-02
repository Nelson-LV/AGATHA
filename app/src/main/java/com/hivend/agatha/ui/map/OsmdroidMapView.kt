package com.hivend.agatha.ui.map

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorNodeStatus
import com.hivend.agatha.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
internal fun OsmdroidMapView(
    route: List<MapCoordinate>,
    nodes: List<SensorNode>,
    selectedNodeId: String?,
    onNodeTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    remember(context) { configureOsmdroid(context) }
    val mapView = remember(context) { createMapView(context) }
    val geoRoute by produceState(initialValue = emptyList<GeoPoint>(), route) {
        value = withContext(Dispatchers.IO) {
            route.map { point -> GeoPoint(point.latitude, point.longitude) }
        }
    }
    var hasFramedPipeline by remember(mapView) { mutableStateOf(false) }

    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.matchParentSize().clipToBounds(),
            update = { view ->
                updateMapOverlays(
                    mapView = view,
                    route = geoRoute,
                    nodes = nodes,
                    selectedNodeId = selectedNodeId,
                    onNodeTap = onNodeTap,
                )
                if (!hasFramedPipeline && geoRoute.size > 1) {
                    view.zoomToBoundingBox(PIPELINE_BOUNDS, true, context.dpToPx(32))
                    hasFramedPipeline = true
                }
            },
        )
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
        ) {
            Column {
                IconButton(
                    onClick = { mapView.controller.zoomIn() },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.map_zoom_in))
                }
                IconButton(
                    onClick = { mapView.controller.zoomOut() },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.map_zoom_out))
                }
            }
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mapView.onPause()
            mapView.onDetach()
        }
    }
}

private fun configureOsmdroid(context: Context) {
    val privateBaseDirectory = File(context.cacheDir, "osmdroid").apply { mkdirs() }
    val privateTileCache = File(privateBaseDirectory, "tiles").apply { mkdirs() }
    Configuration.getInstance().apply {
        userAgentValue = context.packageName
        osmdroidBasePath = privateBaseDirectory
        osmdroidTileCache = privateTileCache
    }
}

private fun createMapView(context: Context): MapView = MapView(context).apply {
    setTileSource(TileSourceFactory.MAPNIK)
    setMultiTouchControls(true)
    setTilesScaledToDpi(true)
    setScrollableAreaLimitDouble(PIPELINE_BOUNDS)
    minZoomLevel = 8.0
    maxZoomLevel = 19.0
    setBackgroundColor(Color.rgb(238, 242, 239))
    controller.setZoom(10.0)
    controller.setCenter(GeoPoint(5.93, -73.575))
}

private fun updateMapOverlays(
    mapView: MapView,
    route: List<GeoPoint>,
    nodes: List<SensorNode>,
    selectedNodeId: String?,
    onNodeTap: (String) -> Unit,
) {
    mapView.overlays.clear()

    if (route.size > 1) {
        mapView.overlays.add(
            createPipelinePolyline(
                mapView = mapView,
                route = route,
                color = PIPELINE_OUTLINE_COLOR,
                strokeWidthDp = PIPELINE_OUTLINE_WIDTH_DP,
            ),
        )
        mapView.overlays.add(
            createPipelinePolyline(
                mapView = mapView,
                route = route,
                color = PIPELINE_MAIN_COLOR,
                strokeWidthDp = PIPELINE_MAIN_WIDTH_DP,
            ),
        )
    }

    nodes.forEach { node ->
        val latitude = node.latitude ?: return@forEach
        val longitude = node.longitude ?: return@forEach
        val marker = Marker(mapView).apply {
            position = GeoPoint(latitude, longitude)
            title = node.id
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setInfoWindow(null)
            icon = nodeMarkerIcon(mapView.context, node.nodeStatus, node.id == selectedNodeId)
            setOnMarkerClickListener { tappedMarker, _ ->
                onNodeTap(node.id)
                tappedMarker.closeInfoWindow()
                true
            }
        }
        mapView.overlays.add(marker)
    }

    mapView.invalidate()
}

private fun createPipelinePolyline(
    mapView: MapView,
    route: List<GeoPoint>,
    color: Int,
    strokeWidthDp: Float,
): Polyline = Polyline(mapView).apply {
    setPoints(route)
    outlinePaint.apply {
        this.color = color
        strokeWidth = strokeWidthDp * mapView.context.resources.displayMetrics.density
        isAntiAlias = true
        strokeJoin = android.graphics.Paint.Join.ROUND
        strokeCap = android.graphics.Paint.Cap.ROUND
    }
    isGeodesic = true
}

private fun nodeMarkerIcon(
    context: Context,
    status: SensorNodeStatus,
    selected: Boolean,
): GradientDrawable {
    val density = context.resources.displayMetrics.density
    val diameter = if (selected) 22f else 17f
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(status.markerColor())
        setSize((diameter * density).toInt(), (diameter * density).toInt())
        setStroke((2f * density).toInt(), Color.WHITE)
    }
}

private fun SensorNodeStatus.markerColor(): Int = when (this) {
    SensorNodeStatus.NORMAL -> Color.rgb(46, 125, 50)
    SensorNodeStatus.YELLOW -> Color.rgb(245, 190, 0)
    SensorNodeStatus.ORANGE -> Color.rgb(239, 108, 0)
    SensorNodeStatus.RED -> Color.rgb(198, 40, 40)
    SensorNodeStatus.NO_COMMUNICATION -> Color.rgb(117, 117, 117)
}

private fun Context.dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

private val PIPELINE_BOUNDS = BoundingBox(
    6.06,
    -73.45,
    5.80,
    -73.70,
)

internal const val PIPELINE_MAIN_COLOR = 0xFF0D5D95.toInt()
internal const val PIPELINE_OUTLINE_COLOR = 0xFFFFFFFF.toInt()
internal const val PIPELINE_MAIN_WIDTH_DP = 5f
internal const val PIPELINE_OUTLINE_WIDTH_DP = 8f
