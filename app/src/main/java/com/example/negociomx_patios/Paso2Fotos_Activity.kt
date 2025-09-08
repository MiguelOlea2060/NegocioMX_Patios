package com.example.negociomx_patios

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_patios.databinding.ActivityPaso2FotosBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class Paso2Fotos_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityPaso2FotosBinding
    private var vinVehiculo: String = ""
    private var idVehiculo: String = ""
    private var marcaModelo: String = ""

    private var currentPhotoType: Int = 0
    private var fotoUri: Uri? = null
    private val fotosCapturadas = mutableMapOf<Int, Bitmap>()
    private var modoLectura = false

    // Launcher para cámara
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            procesarFotoCapturada()
        }
    }

    // Launcher para permisos
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            abrirCamara(currentPhotoType)
        } else {
            Toast.makeText(this, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaso2FotosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        obtenerDatosIntent()
        configurarEventos()
        verificarFotosExistentes()
    }

    private fun obtenerDatosIntent() {
        vinVehiculo = intent.getStringExtra("VIN") ?: ""
        idVehiculo = intent.getStringExtra("IdVehiculo") ?: ""
        marcaModelo = intent.getStringExtra("MarcaModelo") ?: ""

        binding.tvInfoVehiculo.text = "VIN: $vinVehiculo\n$marcaModelo"
    }

    private fun configurarEventos() {
        // Configurar clicks en placeholders
        binding.layoutFoto1.setOnClickListener { tomarFoto(1) }
        binding.layoutFoto2.setOnClickListener { tomarFoto(2) }
        binding.layoutFoto3.setOnClickListener { tomarFoto(3) }
        binding.layoutFoto4.setOnClickListener { tomarFoto(4) }
        binding.layoutFoto5.setOnClickListener { tomarFoto(5) }

        // Botones
        binding.btnVolver.setOnClickListener { finish() }
        binding.btnGuardar.setOnClickListener { guardarFotos() }
    }

    private fun verificarFotosExistentes() {
        // TODO: Consultar si ya existen fotos para este vehículo
        // Si existen, cargarlas y activar modo lectura
        lifecycleScope.launch {
            try {
                // Aquí iría la consulta a la base de datos
                // Por ahora, asumimos que no hay fotos existentes
                modoLectura = false
                actualizarInterfaz()
            } catch (e: Exception) {
                Log.e("Paso2Fotos", "Error verificando fotos: ${e.message}")
            }
        }
    }

    private fun tomarFoto(tipoFoto: Int) {
        if (modoLectura) {
            Toast.makeText(this, "Las fotos ya están guardadas", Toast.LENGTH_SHORT).show()
            return
        }

        currentPhotoType = tipoFoto

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara(tipoFoto)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara(tipoFoto: Int) {
        try {
            val photoFile = File(getExternalFilesDir(null), "temp_foto_$tipoFoto.jpg")
            fotoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
            cameraLauncher.launch(fotoUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Error abriendo cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun procesarFotoCapturada() {
        try {
            fotoUri?.let { uri ->
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                fotosCapturadas[currentPhotoType] = bitmap

                // Mostrar la foto en el ImageView correspondiente
                val imageView = when (currentPhotoType) {
                    1 -> binding.ivFoto1
                    2 -> binding.ivFoto2
                    3 -> binding.ivFoto3
                    4 -> binding.ivFoto4
                    5 -> binding.ivFoto5
                    else -> null
                }

                imageView?.setImageBitmap(bitmap)
                imageView?.scaleType = ImageView.ScaleType.CENTER_CROP

                actualizarInterfaz()
                Toast.makeText(this, "Foto capturada correctamente", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error procesando foto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarInterfaz() {
        // Mostrar botón guardar si hay al menos una foto
        binding.btnGuardar.visibility = if (fotosCapturadas.isNotEmpty() && !modoLectura) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Actualizar apariencia de placeholders según modo
        val layouts = listOf(
            binding.layoutFoto1, binding.layoutFoto2, binding.layoutFoto3,
            binding.layoutFoto4, binding.layoutFoto5
        )

        layouts.forEachIndexed { index, layout ->
            layout.alpha = if (modoLectura) 0.7f else 1.0f
        }
    }

    private fun guardarFotos() {
        if (fotosCapturadas.isEmpty()) {
            Toast.makeText(this, "No hay fotos para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // TODO: Implementar guardado en base de datos
                // Por ahora, solo guardamos localmente

                fotosCapturadas.forEach { (tipo, bitmap) ->
                    guardarFotoLocal(bitmap, tipo)
                }

                Toast.makeText(this@Paso2Fotos_Activity, "Fotos guardadas correctamente", Toast.LENGTH_LONG).show()

                // Activar modo lectura
                modoLectura = true
                actualizarInterfaz()

            } catch (e: Exception) {
                Toast.makeText(this@Paso2Fotos_Activity, "Error guardando fotos: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun guardarFotoLocal(bitmap: Bitmap, tipo: Int) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "vehiculo_${vinVehiculo}_tipo${tipo}_$timestamp.jpg"
            val file = File(getExternalFilesDir("fotos_vehiculos"), filename)

            file.parentFile?.mkdirs()

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            Log.d("Paso2Fotos", "Foto guardada: ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e("Paso2Fotos", "Error guardando foto local: ${e.message}")
        }
    }
}