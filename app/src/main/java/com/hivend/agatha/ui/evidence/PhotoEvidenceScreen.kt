package com.hivend.agatha.ui.evidence

import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.size
import com.hivend.agatha.ui.theme.AgathaTheme
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.hivend.agatha.core.util.createEvidenceUri
import com.hivend.agatha.domain.model.PhotoEvidence
import com.hivend.agatha.ui.components.BackTopBar
import com.hivend.agatha.ui.components.ConnectivityBar
import com.hivend.agatha.ui.components.SitePill
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.hivend.agatha.R

/**
 * Evidencia fotográfica (HE-06, opcional). Corresponde al nodo 34:266 de Figma. La captura
 * se delega en la app de cámara del sistema vía [ActivityResultContracts.TakePicture] y en
 * el Photo Picker nativo de Android 13+ vía [ActivityResultContracts.PickVisualMedia] — ninguno
 * de los dos requiere pedir permisos de cámara o almacenamiento en tiempo de ejecución.
 */
@Composable
fun PhotoEvidenceScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoEvidenceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onSaved(viewModel.alertId)
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingUri?.let { viewModel.onPhotoAdded(it.toString()) }
    }
    val pickFromGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.onPhotoAdded(it.toString()) } }

    Column(modifier = modifier.fillMaxSize().background(AgathaTheme.colors.background)) {
        BackTopBar(title = stringResource(R.string.evidence_top_bar_title), onBack = onBack)
        ConnectivityBar(connected = false, message = stringResource(R.string.connectivity_offline_saved_locally))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).padding(16.dp),
        ) {
            SitePill(site = uiState.alert?.site ?: stringResource(R.string.common_empty_value))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AgathaTheme.colors.surface, RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.evidence_title), color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.evidence_subtitle, uiState.alert?.sensorId ?: viewModel.alertId),
                    color = AgathaTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 10.dp),
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(uiState.evidence, key = { it.uri }) { evidence ->
                        EvidenceTile(evidence = evidence, onDescriptionChanged = viewModel::onDescriptionChanged)
                    }
                    item {
                        AddEvidenceTile(
                            onTakePhoto = {
                                val uri = createEvidenceUri(context)
                                pendingUri = uri
                                takePhotoLauncher.launch(uri)
                            },
                            onPickFromGallery = {
                                pickFromGalleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        )
                    }
                }

                Button(
                    onClick = viewModel::saveEvidence,
                    enabled = !uiState.saving,
                    colors = ButtonDefaults.buttonColors(containerColor = AgathaTheme.colors.positive),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    if (!uiState.saving) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                    Text(stringResource(if (uiState.saving) R.string.common_saving else R.string.evidence_save))
                }
            }
        }
    }
}

@Composable
private fun EvidenceTile(evidence: PhotoEvidence, onDescriptionChanged: (String, String) -> Unit) {
    Column {
        AsyncImage(
            model = evidence.uri,
            contentDescription = evidence.description,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(AgathaTheme.colors.surfaceVariant, RoundedCornerShape(10.dp)),
        )
        OutlinedTextField(
            value = evidence.description,
            onValueChange = { onDescriptionChanged(evidence.uri, it) },
            placeholder = { Text(stringResource(R.string.evidence_add_description), style = MaterialTheme.typography.bodySmall) },
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun AddEvidenceTile(onTakePhoto: () -> Unit, onPickFromGallery: () -> Unit) {
    var showOptions by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(AgathaTheme.colors.brandContainer, RoundedCornerShape(10.dp))
            .border(1.dp, AgathaTheme.colors.brand, RoundedCornerShape(10.dp))
            .clickable { showOptions = !showOptions },
    ) {
        if (showOptions) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.evidence_take_photo),
                    color = AgathaTheme.colors.brand,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable { showOptions = false; onTakePhoto() }.padding(6.dp),
                )
                Text(
                    stringResource(R.string.evidence_pick_gallery),
                    color = AgathaTheme.colors.brand,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable { showOptions = false; onPickFromGallery() }.padding(6.dp),
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = AgathaTheme.colors.brand)
                Text(stringResource(R.string.evidence_add_hint), color = AgathaTheme.colors.brand, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
