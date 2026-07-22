package com.zayaanify.privagallery.presentation.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class EditorTab { ADJUST, FILTER, CROP }

enum class PhotoFilter(val displayName: String) {
    NONE("Original"),
    GRAYSCALE("Black & White"),
    SEPIA("Sepia"),
    WARM("Warm"),
    COOL("Cool"),
    VIVID("Vivid"),
    FADE("Fade")
}

data class PhotoEditorUiState(
    val originalBitmap: Bitmap? = null,
    val editedBitmap: Bitmap? = null,
    val sourceUri: String = "",
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val rotation: Float = 0f,
    val selectedFilter: PhotoFilter = PhotoFilter.NONE,
    val activeTab: EditorTab = EditorTab.ADJUST,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class PhotoEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val photoUri: String = savedStateHandle.get<String>("photoUri") ?: ""

    private val _uiState = MutableStateFlow(PhotoEditorUiState(sourceUri = photoUri))
    val uiState: StateFlow<PhotoEditorUiState> = _uiState.asStateFlow()

    init {
        loadPhoto()
    }

    private fun loadPhoto() {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(photoUri)
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, this)
                        }
                        val maxSize = 2048
                        inSampleSize = calculateInSampleSize(
                            outWidth, outHeight, maxSize, maxSize
                        )
                        inJustDecodeBounds = false
                    }
                    context.contentResolver.openInputStream(Uri.parse(photoUri))?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }
                } catch (e: Exception) {
                    null
                }
            }
            _uiState.value = _uiState.value.copy(
                originalBitmap = bitmap,
                editedBitmap = bitmap,
                isLoading = false
            )
        }
    }

    /** UCrop থেকে ক্রপ করা ছবি পাওয়ার পর এই ফাংশন call হবে। */
    fun onCropResult(croppedUri: Uri) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                        context.contentResolver.openInputStream(croppedUri)?.use {
                            BitmapFactory.decodeStream(it, null, this)
                        }
                        inSampleSize = calculateInSampleSize(
                            outWidth, outHeight, 2048, 2048
                        )
                        inJustDecodeBounds = false
                    }
                    context.contentResolver.openInputStream(croppedUri)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }
                } catch (e: Exception) {
                    null
                }
            } ?: return@launch

            // ক্রপ করা ছবিকে নতুন base হিসেবে সেট করা
            _uiState.value = _uiState.value.copy(
                originalBitmap = bitmap,
                editedBitmap = bitmap,
                // adjust/filter reset করা ক্রপের পর
                brightness = 0f,
                contrast = 1f,
                saturation = 1f,
                rotation = 0f,
                selectedFilter = PhotoFilter.NONE
            )
        }
    }

    fun setActiveTab(tab: EditorTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun setBrightness(value: Float) {
        _uiState.value = _uiState.value.copy(brightness = value)
        applyEdits()
    }

    fun setContrast(value: Float) {
        _uiState.value = _uiState.value.copy(contrast = value)
        applyEdits()
    }

    fun setSaturation(value: Float) {
        _uiState.value = _uiState.value.copy(saturation = value)
        applyEdits()
    }

    fun setFilter(filter: PhotoFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        applyEdits()
    }

    fun rotateRight() {
        _uiState.value = _uiState.value.copy(
            rotation = (_uiState.value.rotation + 90f) % 360f
        )
        applyEdits()
    }

    fun rotateLeft() {
        _uiState.value = _uiState.value.copy(
            rotation = (_uiState.value.rotation - 90f + 360f) % 360f
        )
        applyEdits()
    }

    fun resetEdits() {
        viewModelScope.launch {
            // আসল ছবি reload করা
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(photoUri))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                } catch (e: Exception) {
                    null
                }
            }
            _uiState.value = _uiState.value.copy(
                originalBitmap = bitmap,
                editedBitmap = bitmap,
                brightness = 0f,
                contrast = 1f,
                saturation = 1f,
                rotation = 0f,
                selectedFilter = PhotoFilter.NONE
            )
        }
    }

    private fun applyEdits() {
        viewModelScope.launch(Dispatchers.Default) {
            val original = _uiState.value.originalBitmap ?: return@launch
            val state = _uiState.value
            val result = applyColorMatrix(original, state)
            val rotated = applyRotation(result, state.rotation)
            _uiState.value = _uiState.value.copy(editedBitmap = rotated)
        }
    }

    private fun applyColorMatrix(bitmap: Bitmap, state: PhotoEditorUiState): Bitmap {
        val paint = Paint()
        val colorMatrix = ColorMatrix()

        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(state.saturation)
        colorMatrix.postConcat(saturationMatrix)

        val filterMatrix = getFilterMatrix(state.selectedFilter)
        if (filterMatrix != null) colorMatrix.postConcat(filterMatrix)

        val b = state.brightness * 255f
        val c = state.contrast
        val brightnessContrastMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, b,
                0f, c, 0f, 0f, b,
                0f, 0f, c, 0f, b,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(brightnessContrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        val result = Bitmap.createBitmap(
            bitmap.width, bitmap.height,
            bitmap.config ?: Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun getFilterMatrix(filter: PhotoFilter): ColorMatrix? {
        return when (filter) {
            PhotoFilter.NONE -> null
            PhotoFilter.GRAYSCALE -> ColorMatrix().apply { setSaturation(0f) }
            PhotoFilter.SEPIA -> ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.WARM -> ColorMatrix(
                floatArrayOf(
                    1.2f, 0f, 0f, 0f, 10f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 0.8f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.COOL -> ColorMatrix(
                floatArrayOf(
                    0.8f, 0f, 0f, 0f, -10f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 1.2f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.VIVID -> ColorMatrix().apply { setSaturation(1.8f) }
            PhotoFilter.FADE -> ColorMatrix(
                floatArrayOf(
                    0.8f, 0f, 0f, 0f, 30f,
                    0f, 0.8f, 0f, 0f, 30f,
                    0f, 0f, 0.8f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
    }

    private fun applyRotation(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    fun savePhoto() {
        viewModelScope.launch {
            val bitmap = _uiState.value.editedBitmap ?: return@launch
            _uiState.value = _uiState.value.copy(isSaving = true)

            val saved = withContext(Dispatchers.IO) {
                try {
                    val fileName = "edited_${System.currentTimeMillis()}.jpg"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                            put(
                                MediaStore.Images.Media.RELATIVE_PATH,
                                "Pictures/PrivaGallery"
                            )
                        }
                    }

                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ) ?: return@withContext false

                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(uri, contentValues, null, null)
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }

            _uiState.value = _uiState.value.copy(
                isSaving = false,
                isSaved = saved,
                message = if (saved) "ছবি সেভ হয়েছে" else "সেভ করা যায়নি"
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun calculateInSampleSize(
        width: Int, height: Int,
        reqWidth: Int, reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}