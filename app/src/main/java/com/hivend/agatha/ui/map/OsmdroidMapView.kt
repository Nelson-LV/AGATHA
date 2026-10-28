package com.hivend.agatha.ui.map

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorNodeStatus
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

    AndroidView(
        factory = { mapView },
        modifier = modifier,
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
    setBuiltInZoomControls(true)
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
        val routeOverlay = Polyline(mapView).apply {
            setPoints(route)
            outlinePaint.color = PIPELINE_COLOR
            outlinePaint.strokeWidth = mapView.context.resources.displayMetrics.density * 4f
            isGeodesic = true
        }
        mapView.overlays.add(routeOverlay)
    }

    nodes.forEach { node ->
        val latitude = node.latitude ?: return@forEach
        val longitude = node.longitude ?: return@forEach
        val marker = Marker(mapView).apply {
            position = GeoPoint(latitude, longitude)
            title = node.id
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
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

private val PIPELINE_COLOR = Color.rgb(13, 93, 149)
