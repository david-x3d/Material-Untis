package dev.x3d.dayline.ui.login

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import dev.x3d.dayline.R
import dev.x3d.dayline.data.rpc.QrLoginParser
import java.util.concurrent.Executors
import org.koin.androidx.compose.koinViewModel

@Composable
fun QrScanScreen(
    onDone: () -> Unit,
    draftStore: QrDraftStore = remember { org.koin.core.context.GlobalContext.get().get() },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var invalid by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.qr_title), style = MaterialTheme.typography.headlineMedium)
        if (!granted) {
            Text(stringResource(R.string.login_camera_denied), modifier = Modifier.padding(top = 16.dp))
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val reader = MultiFormatReader().apply {
                        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    val executor = Executors.newSingleThreadExecutor()
                    analysis.setAnalyzer(executor) { image ->
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        val source = PlanarYUVLuminanceSource(bytes, image.width, image.height, 0, 0, image.width, image.height, false)
                        val bitmap = BinaryBitmap(HybridBinarizer(source))
                        val text = runCatching { reader.decodeWithState(bitmap).text }.getOrNull()
                        reader.reset()
                        if (text != null) {
                            val parsed = QrLoginParser.parse(text)
                            if (parsed != null) {
                                image.close()
                                draftStore.latest = QrDraft(parsed.user, parsed.secret, parsed.school)
                                previewView.post(onDone)
                                return@setAnalyzer
                            } else {
                                invalid = true
                            }
                        }
                        image.close()
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    previewView
                },
            )
            if (invalid) {
                Text(stringResource(R.string.qr_invalid), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
