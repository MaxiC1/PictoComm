package com.inacap.picto_comm.ui.screens

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.inacap.picto_comm.PictoCommApplication
import com.inacap.picto_comm.R

/**
 * Actividad de configuración de la aplicación
 * Permite cambiar entre modo claro, oscuro y automático
 * Permite habilitar/deshabilitar el brillo automático
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var radioGroupTheme: RadioGroup
    private lateinit var radioSistema: RadioButton
    private lateinit var radioClaro: RadioButton
    private lateinit var radioOscuro: RadioButton
    private lateinit var switchBrilloAutomatico: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        inicializarVistas()
        configurarToolbar()
        cargarPreferencias()
        configurarListeners()
    }

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        radioGroupTheme = findViewById(R.id.radio_group_theme)
        radioSistema = findViewById(R.id.radio_sistema)
        radioClaro = findViewById(R.id.radio_claro)
        radioOscuro = findViewById(R.id.radio_oscuro)
        switchBrilloAutomatico = findViewById(R.id.switch_brillo_automatico)
    }

    private fun configurarToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun cargarPreferencias() {
        val sessionManager = (application as PictoCommApplication).sessionManager

        // Cargar preferencia de tema
        when (sessionManager.obtenerModoOscuro()) {
            null -> radioSistema.isChecked = true   // Seguir el sistema
            false -> radioClaro.isChecked = true     // Modo claro
            true -> radioOscuro.isChecked = true     // Modo oscuro
        }

        // Cargar preferencia de brillo automático
        switchBrilloAutomatico.isChecked = sessionManager.obtenerBrilloAutomatico()
    }

    private fun configurarListeners() {
        radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val sessionManager = (application as PictoCommApplication).sessionManager

            when (checkedId) {
                R.id.radio_sistema -> {
                    sessionManager.guardarModoOscuro(null)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                R.id.radio_claro -> {
                    sessionManager.guardarModoOscuro(false)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                R.id.radio_oscuro -> {
                    sessionManager.guardarModoOscuro(true)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
        }

        switchBrilloAutomatico.setOnCheckedChangeListener { _, isChecked ->
            val sessionManager = (application as PictoCommApplication).sessionManager
            sessionManager.guardarBrilloAutomatico(isChecked)
        }
    }
}
