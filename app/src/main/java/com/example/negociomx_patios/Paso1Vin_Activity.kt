package com.example.negociomx_patios

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_patios.BE.StatusFotoVehiculo
import com.example.negociomx_patios.BE.Vehiculo
import com.example.negociomx_patios.DAL.DALVehiculo
//import com.example.negociomx_pos.Utils.FileUploadUtil
import com.example.negociomx_patios.Utils.ParametrosSistema
import com.example.negociomx_patios.databinding.ActivityPaso1VinBinding
import com.journeyapps.barcodescanner.ScanContract
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import com.example.negociomx_patios.databinding.ActivityPaso1SocBinding


class Paso1Vin_Activity : AppCompatActivity() {
    //Paso1
    private lateinit var binding: ActivityPaso1VinBinding
    private val dalVehiculo = DALVehiculo()
    private var vehiculoActual: Vehiculo? = null


    private lateinit var loadingContainer: LinearLayout
    private lateinit var tvLoadingText: TextView
    private lateinit var tvLoadingSubtext: TextView
    private lateinit var btnGuardar: Button
    private var loadingHandler: Handler? = null
    private var loadingRunnable: Runnable? = null


    private var evidencia1Capturada: Boolean = false
    private var evidencia2Capturada: Boolean = false
    private var currentPhotoType: Int = 0 // Para saber qué evidencia estamos capturando
    private var fotoUri: Uri? = null
    private var vehiculo: Vehiculo? = null

    private var idUsuarioNubeAlta: Int =
        ParametrosSistema.usuarioLogueado.Id?.toInt()!!// Reemplaza con el ID del usuario actual
    private var fotosExistentes: Int = 0 // Para controlar cuántas fotos ya existen

    // ✅ LAUNCHER PARA ESCÁNER DE CÓDIGOS
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
        else {
            binding.etVIN.setText(result.contents)
            Toast.makeText(this, "VIN escaneado: ${result.contents}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaso1VinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarEventos()

    }


    private fun configurarEventos() {


        // ✅ BOTÓN ESCANEAR VIN
        binding.etVIN.requestFocus()

        // Configurando Captura de enter en el QR del VIN
        binding.etVIN.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                verificaVINSuministrado()
                return@setOnKeyListener true
            }
            false
        }


        // ✅ BOTÓN CONSULTAR VEHÍCULO
        binding.btnConsultarVehiculo.setOnClickListener {
            verificaVINSuministrado()
        }


        loadingContainer = findViewById(R.id.loadingContainer)
        tvLoadingText = findViewById(R.id.tvLoadingText)
        tvLoadingSubtext = findViewById(R.id.tvLoadingSubtext)



        // Configurar botón Ir a Fotos
        binding.btnIrAFotos.setOnClickListener {
            irAFotos()
        }


    }

    private fun verificaVINSuministrado() {
        val vin = binding.etVIN.text.toString().trim()
        if (vin.isNotEmpty() && vin.length > 16) {
            consultarVehiculo(vin)
        } else {
            Toast.makeText(this, "Ingrese un VIN válido", Toast.LENGTH_SHORT).show()
        }
    }


    private fun consultarVehiculo(vin: String) {
        lifecycleScope.launch {
            try {
                Log.d("Paso1SOC", "🔍 Consultando vehículo con VIN: $vin")
                mostrarCargaConsulta()

                Toast.makeText(
                    this@Paso1Vin_Activity,
                    "Consultando vehículo...",
                    Toast.LENGTH_SHORT
                ).show()

                vehiculo = dalVehiculo.consultarVehiculoPorVIN(vin)
                if (vehiculo != null) {
                    vehiculoActual = vehiculo

                    // ✅ CONSULTAR DATOS SOC EXISTENTES
                    val datosSOCExistentes =
                        dalVehiculo.consultarDatosSOCExistentes(vehiculo?.Id?.toInt()!!)


                    mostrarInformacionVehiculo(vehiculo!!)


                    ocultarCargaConsulta()
                } else {

                    Toast.makeText(
                        this@Paso1Vin_Activity,
                        "❌ Vehículo no encontrado",
                        Toast.LENGTH_LONG
                    ).show()
                    ocultarCargaConsulta()

                    binding.etVIN.selectAll()
                }

            } catch (e: Exception) {
                Log.e("Paso1SOC", "💥 Error consultando vehículo: ${e.message}")
                Toast.makeText(this@Paso1Vin_Activity, "Error: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                ocultarCargaConsulta()
                binding.etVIN.selectAll()
            }
        }
    }


    private fun mostrarInformacionVehiculo(vehiculo: Vehiculo) {
        binding.apply {
            tvBlVehiculo.text = "MBL: ${vehiculo.BL}"
            tvMarcaModeloAnnio.text = "${vehiculo.Marca} - ${vehiculo.Modelo}, ${vehiculo.Anio}"
            tvColorExterior.text = "Color Ext.: ${vehiculo.ColorExterior}"
            tvColorInterior.text = "Color Int.: ${vehiculo.ColorInterior}"
            tvTipoCombustible.text = "Combustible: ${vehiculo.TipoCombustible}"
            tvTipoVehiculo.text = "Tipo de Vehiculo: ${vehiculo.TipoVehiculo}"



            layoutInfoVehiculo.visibility = View.VISIBLE
            btnIrAFotos.visibility = View.VISIBLE
        }
    }




    private fun mostrarCargaConsulta() {
        // Mostrar loading
        loadingContainer.visibility = View.VISIBLE
        binding.btnConsultarVehiculo.isEnabled = false
        binding.btnConsultarVehiculo.alpha = 0.5f

        var mensajeIndex = 0

    }

    private fun ocultarCargaConsulta() {
        loadingContainer.visibility = View.GONE
        binding.btnConsultarVehiculo.isEnabled = true
        binding.btnConsultarVehiculo.alpha = 1.0f

        // Limpiar handlers
        loadingHandler?.removeCallbacks(loadingRunnable!!)
        loadingHandler = null
        loadingRunnable = null
    }


    private fun irAFotos() {
        val vehiculo = vehiculoActual
        if (vehiculo == null) {
            Toast.makeText(this, "Primero consulte un vehículo", Toast.LENGTH_SHORT).show()
            return
        }


        Toast.makeText(this, "Navegando a sección de fotos...", Toast.LENGTH_SHORT).show()
        limpiarFormulario()
    }


    private fun limpiarFormulario() {
        binding.apply {
            etVIN.setText("")
            layoutInfoVehiculo.visibility = View.GONE
            btnIrAFotos.visibility = View.GONE
        }
        vehiculoActual = null
    }



}