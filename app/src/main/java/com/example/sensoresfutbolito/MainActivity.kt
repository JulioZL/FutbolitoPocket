package com.example.sensoresfutbolito

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.fuchopro.R

private var auxAnchura: Int? = null
private var auxAltura: Int? = null
private var contUno: Int = 0
private var contDos: Int = 0

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    //Declaración de variables de sensores
    private var sA: Sensor? = null
    private var sB: Sensor? = null
    private lateinit var sensorManager: SensorManager
    lateinit var viewDibujo: ProcessClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ocultar la barra de navegación y de estado
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        // Ocultar la barra de título
        supportActionBar?.hide()
        // Establecer la vista de la actividad en pantalla completa
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // Obtener las dimensiones de la pantalla
        val metricDsp = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metricDsp)
        auxAltura = metricDsp.heightPixels
        auxAnchura = metricDsp.widthPixels
        // Crear y establecer la vista de dibujo
        viewDibujo = ProcessClass(this)
        setContentView(viewDibujo)
        // Obtener el servicio de sensores
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Buscar el sensor de gravedad
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
            val gravSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_GRAVITY)
            // Utilizar la versión 3 del sensor de gravedad de Google
            sB = gravSensors.firstOrNull { it.vendor.contains("Google LLC") && it.version == 3 }
        }
        // Si no se encuentra el sensor de gravedad, utilizar el acelerómetro
        if (sB == null) {
            sB = if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            } else {
                null
            }
        }
        // Obtener el sensor de aceleración
        sA = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        // Se comprueba si el sensor del acelerómetro está disponible.
        // Si lo está, se registra el listener de sensores con una velocidad normal (SENSOR_DELAY_NORMAL)
        sA?.also {
            sensorManager.registerListener(
                viewDibujo, it, SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        // Se desregistra el listener de sensores cuando la actividad pasa a segundo plano.
        super.onPause()
        sensorManager.unregisterListener(viewDibujo)
    }

    override fun onDestroy() {
        // Se desregistra el listener de sensores cuando la actividad es destruida.
        super.onDestroy()
        sensorManager.unregisterListener(viewDibujo)
    }
}

class ProcessClass(ctx: Context) : View(ctx), SensorEventListener {
    // Variables de posición y velocidad de la pelota
    var auxX = auxAnchura!! / 2f
    var auxY = auxAltura!! / 2f
    var auxAcX: Float = 0f
    var auxAcY: Float = 0f
    var auxVelX: Float = 0.0f
    var auxVelY: Float = 0.0f
    var auxRad = 50f

    // Se carga la imagen del fondo y se crean los rectángulos para los canvas
    val map = BitmapFactory.decodeResource(resources, R.drawable.fondo)
    val cnvaRect = Rect(0, 0, auxAnchura!!, auxAltura!!)
    val mapRect = RectF(0f, 0f, map.width.toFloat(), map.height.toFloat())

    // Se definen los Paints para la pelota y el marcador
    var paintPelota = Paint()
    var paintMarcador = Paint()

    // Arreglos de aceleración y gravedad
    private var auxGrav = FloatArray(3)
    private var auxLinAcc = FloatArray(3)

    // En el bloque init se configuran los Paints
    init {
        paintPelota.color = Color.WHITE
        paintMarcador.color = Color.BLACK
        paintMarcador.textSize = 90f
        paintMarcador.style = Paint.Style.FILL_AND_STROKE
    }

    // Método que dibuja la pelota y el marcador en el canvas
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // Se calcula el rectángulo para la imagen del fondo
        mapRect.offsetTo(
            cnvaRect.centerX() - mapRect.width() / 2,
            cnvaRect.centerY() - mapRect.height() / 2
        )

        // Se dibuja la imagen del fondo y la pelota en el canvas
        canvas!!.drawBitmap(map, null, cnvaRect, null)
        canvas.drawCircle(auxX, auxY, auxRad, paintPelota)

        // Se dibuja el marcador en el canvas
        canvas.drawText("[ $contUno : $contDos ]", auxAnchura!! / 2.6f, auxAltura!! / 3f, paintMarcador)

        // Se fuerza un redibujado constante
        invalidate()
    }

    // Método que se llama cuando se detecta un cambio en el sensor
    override fun onSensorChanged(event: SensorEvent?) {
        // Coeficiente de filtro de gravedad
        val auxAlp = 0.8f
        // Se aplica un filtro pasa bajo para la gravedad
        auxGrav[0] = auxAlp * auxGrav[0] + (1 - auxAlp) * event!!.values[0]
        auxGrav[1] = auxAlp * auxGrav[1] + (1 - auxAlp) * event.values[1]
        auxGrav[2] = auxAlp * auxGrav[2] + (1 - auxAlp) * event.values[2]
        // Se remueve la contribución de la gravedad con un filtro pasa alto
        auxLinAcc[0] = event.values[0] - auxGrav[0]   //x
        auxLinAcc[1] = event.values[1] - auxGrav[1]   //y
        auxLinAcc[2] = event.values[2] - auxGrav[2]   //z
        processPelota(auxLinAcc[0], auxLinAcc[1] * -1)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun processPelota(auxOrX: Float, auxOrY: Float) {
        auxAcX = auxOrX
        auxAcY = auxOrY
        cambioX()
        cambioY()
        anotacion()
    }

    // Actualiza la posición en X del objeto
    fun cambioX() {
        // Si el objeto no ha chocado con los bordes laterales de la pantalla, actualiza su posición normalmente
        if (auxX < auxAnchura!! - auxRad && auxX > 0 + auxRad) {
            auxVelX -= auxAcX * 3f // Reduce la velocidad en X por la aceleración en X
            auxX += auxVelX // Actualiza la posición en X con la nueva velocidad
        }
        // Si el objeto ha chocado con el borde derecho de la pantalla, lo posiciona justo al borde y lo hace rebotar hacia la izquierda
        else if (auxX >= auxAnchura!! - auxRad) {
            auxX = auxAnchura!! - auxRad * 2 + 1 // Posiciona el objeto justo al borde derecho
            auxVelX -= auxAcX * 3f // Reduce la velocidad en X por la aceleración en X
            auxX += auxVelX // Actualiza la posición en X con la nueva velocidad
        }
        // Si el objeto ha chocado con el borde izquierdo de la pantalla, lo posiciona justo al borde y lo hace rebotar hacia la derecha
        else if (auxX <= 0 + auxRad) {
            auxX = auxRad * 2 + 1 // Posiciona el objeto justo al borde izquierdo
            auxVelX -= auxAcX * 3f // Reduce la velocidad en X por la aceleración en X
            auxX += auxVelX // Actualiza la posición en X con la nueva velocidad
        }
    }
    // Actualiza la posición en Y del objeto
    fun cambioY() {
        // Si el objeto no ha chocado con los bordes superior o inferior de la pantalla, actualiza su posición normalmente
        if (auxY < auxAltura!! - auxRad && auxY > 0 + auxRad) {
            auxVelY -= auxAcY * 3f // Reduce la velocidad en Y por la aceleración en Y
            auxY += auxVelY // Actualiza la posición en Y con la nueva velocidad
        }
        // Si el objeto ha chocado con el borde inferior de la pantalla, lo posiciona justo encima del borde y lo hace rebotar hacia arriba
        else if (auxY >= auxAltura!! - auxRad) {
            auxY = auxAltura!! - auxRad * 3 + 50f // Posiciona el objeto justo encima del borde inferior
            auxVelY -= auxAcY * 3f // Reduce la velocidad en Y por la aceleración en Y
            auxY += auxVelY // Actualiza la posición en Y con la nueva velocidad
        }
        // Si el objeto ha chocado con el borde superior de la pantalla, lo posiciona justo debajo del borde y lo hace rebotar hacia abajo
        else if (auxY <= 0 + auxRad) {
            auxY = auxRad * 3 + 50f // Posiciona el objeto justo debajo del borde superior
            auxVelY -= auxAcY * 3f // Reduce la velocidad en Y por la aceleración en Y
            auxY += auxVelY // Actualiza la posición en Y con la nueva velocidad
        }
    }

    fun anotacion() {
        // Verifica si la pelota ha cruzado la línea superior de la pantalla
        if (auxY >= auxAltura!! - auxRad * 2 && (auxX <= auxAnchura!! / 2f + 50 && auxX >= auxAnchura!! / 2f - 50)) {
            contUno++    // Registra una anotación para el jugador 1
            auxX = auxAnchura!! / 2f    // Devuelve la pelota a la posición central en el eje X
            auxY = auxAltura!! / 2f    // Devuelve la pelota a la posición central en el eje Y
        }

        // Verifica si la pelota ha cruzado la línea inferior de la pantalla
        if (auxY <= 0 + auxRad * 2 && (auxX <= auxAnchura!! / 2f + 50 && auxX >= auxAnchura!! / 2f - 50)) {
            contDos++    // Registra una anotación para el jugador 2
            auxX = auxAnchura!! / 4f    // Devuelve la pelota a la posición central en el eje X (para el jugador 2)
            auxY = auxAltura!! / 2f    // Devuelve la pelota a la posición central en el eje Y
        }
    }
}