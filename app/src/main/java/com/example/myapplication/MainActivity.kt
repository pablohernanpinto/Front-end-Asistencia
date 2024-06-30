package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.format.Formatter
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val gson = Gson()
    private var phoneNumber: String? = null
    private var ssid: String? = null
    private var ip: String? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var scannedQRCode:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btn_scan: Button = findViewById(R.id.btn_scan)


        //MAIN
        btn_scan.setOnClickListener {
            IntentIntegrator(this).initiateScan()
            displayWifiInfo()
            Log.e("TAG", "SSID: $ssid, IP: $ip")
            phoneNumber = getNumber(it)
            Log.e("TAG", "Phone Number: $phoneNumber")
            Log.e("TAG", "Qr: $scannedQRCode")
        }
        //fetchPruebaData()

    }

    private fun fetchPruebaData() {
        //aqui se manda la data
        CoroutineScope(Dispatchers.IO).launch {
            val request = Request.Builder()
                .url("http://localhost:3000/prueba")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val json = responseBody.string()
                        val listType = object : TypeToken<List<Prueba>>() {}.type
                        val pruebaList: List<Prueba> = gson.fromJson(json, listType)

                        withContext(Dispatchers.Main) {
                            // Aquí puedes actualizar la UI con los datos obtenidos
                            //findViewById<TextView>(R.id.textView).text = pruebaList.joinToString("\n")
                        }
                    }
                } else {
                    // Manejar el error de la solicitud
                    withContext(Dispatchers.Main) {
                        //findViewById<TextView>(R.id.textView).text = "Error: ${response.code}"
                    }
                }
            }
        }
    }

    //funcion para obtener numero
    fun getNumber(v: View?): String? {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_NUMBERS
            ) ==
            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Verificar permisos
            val telephonyManager = this.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            return telephonyManager.line1Number
        } else {
            // Solicitar permisos
            requestPermission()
            return null
        }
    }
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.READ_PHONE_STATE
                ), 100
            )
        }
    }

    //funcion para obtener ip y ssd¿id
    private fun displayWifiInfo() {
        ssid = getCurrentSsid(this)
        ip = getIpAddress(this)
    }

    private fun getCurrentSsid(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo = wifiManager.connectionInfo
        return wifiInfo.ssid
    }
    private fun getIpAddress(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ip = wifiInfo.ipAddress
        return Formatter.formatIpAddress(ip)
    }

    // Manejo de la respuesta de la solicitud de permisos
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                phoneNumber = getNumber(null)
                Log.e("TAG", "Phone Number: $phoneNumber")


            } else {
                // Permisos no concedidos
                Log.e("TAG", "Permisos no concedidos")
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                // El escaneo fue cancelado
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_LONG).show()
            } else {
                // El escaneo fue exitoso
                scannedQRCode = result.contents

                Toast.makeText(this, "Código escaneado: " + result.contents, Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }



}

data class Prueba(
    val SSID: String,
    val Numero: String,
    val IP: String
)
