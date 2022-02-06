package ru.amsocat.screenshotapp

import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils.fromFXImage
import javafx.embed.swing.SwingFXUtils.toFXImage
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.*
import java.util.*
import javax.imageio.ImageIO
import kotlin.concurrent.schedule
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class ScreenshotApp(private val primaryStage: Stage) : VBox() {

    @FXML
    private lateinit var canvasLayout: StackPane

    @FXML
    private lateinit var colorOfBrushPicker: ColorPicker

    @FXML
    private lateinit var cropCanvas: Canvas

    @FXML
    private lateinit var cropImageButton: Button

    @FXML
    private lateinit var defaultSaveImageMenuItem: MenuItem

    @FXML
    private lateinit var drawCanvas: Canvas

    @FXML
    private lateinit var exitImageMenuItem: MenuItem

    @FXML
    private lateinit var hideCheckBox: CheckBox

    @FXML
    private lateinit var imageCanvas: Canvas

    @FXML
    private lateinit var makeScreenshotMenuItem: MenuItem

    @FXML
    private lateinit var openImageMenuItem: MenuItem

    @FXML
    private lateinit var saveImageMenuItem: MenuItem

    @FXML
    private lateinit var screenshotButton: Button

    @FXML
    private lateinit var sizeOfBrushPicker: ChoiceBox<Any>

    @FXML
    private lateinit var timingSlider: Slider

    private var brushSize = 10
    private var isCropping = false
    private var firstX = 0.0
    private var firstY = 0.0
    private var lastX = 1.0
    private var lastY = 0.0

    @FXML
    fun initialize() {
        for (i in 1 until 51) sizeOfBrushPicker.items.add(i)
        sizeOfBrushPicker.value = 10
        sizeOfBrushPicker.setOnAction {
            brushSize = sizeOfBrushPicker.value as Int
        }
        makeScreenshotMenuItem.onAction = EventHandler {
            this.makeScreenshot()
        }
        openImageMenuItem.onAction = EventHandler {
            clearCanvasLayoutAndSetImage(openImage())
        }
        defaultSaveImageMenuItem.onAction = EventHandler {
            saveImage(true)
        }
        saveImageMenuItem.onAction = EventHandler {
            saveImage(false)
        }
        exitImageMenuItem.onAction = EventHandler {
            clearImage()
        }
        cropImageButton.onAction = EventHandler {
            this.isCropping = true
        }
        canvasLayout.onMouseReleased = EventHandler {
            if (this.isCropping) {
                this.isCropping = false
                cropImage()
            }
        }
        canvasLayout.onMouseDragged = EventHandler { evt ->
            if (!this.isCropping) {
                if (evt.button == MouseButton.PRIMARY) {
                    drawCircle(this.brushSize, colorOfBrushPicker.value, evt.x, evt.y)
                }
                if (evt.button == MouseButton.SECONDARY) {
                    clearRect(this.brushSize, evt.x, evt.y)
                }
            } else {
                lastX = evt.x
                lastY = evt.y
                cropCanvas.graphicsContext2D.clearRect(0.0, 0.0, cropCanvas.width, cropCanvas.height)
                cropCanvas.graphicsContext2D.strokeRect(
                    if (firstX < lastX) firstX else lastX,
                    if (firstY < lastY) firstY else lastY,
                    abs(firstX - lastX),
                    abs(firstY - lastY)
                )
            }
        }
        canvasLayout.onMousePressed = EventHandler { evt ->
            if (!this.isCropping) {
                if (evt.button == MouseButton.PRIMARY) {
                    drawCircle(this.brushSize, colorOfBrushPicker.value, evt.x, evt.y)
                }
                if (evt.button == MouseButton.SECONDARY) {
                    clearRect(this.brushSize, evt.x, evt.y)
                }
            } else {
                this.firstX = evt.x
                this.firstY = evt.y
            }
        }
        screenshotButton.onAction = EventHandler {
            this.makeScreenshot()
        }
    }

    private fun openImage(): WritableImage? {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg"))
        val file = fileChooser.showOpenDialog(Stage())
        return toFXImage(ImageIO.read(file), null)
    }

    private fun saveImage(useDefault: Boolean) {
        val file: File
        if (useDefault) {
            file = File(System.getenv("USERPROFILE") + "\\Documents\\Screenshot" + ".png")
        } else {
            val fileChooser = FileChooser()
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Image Files", "*.png"))
            fileChooser.initialDirectory = File(getLastDirectory())
            file = fileChooser.showSaveDialog(Stage())
        }
        val params = SnapshotParameters()
        params.fill = Color.TRANSPARENT
        imageCanvas.graphicsContext2D.drawImage(drawCanvas.snapshot(params, null), 0.0, 0.0)
        val image = imageCanvas.snapshot(params, null)
        clearImage()
        ImageIO.write(fromFXImage(image, null), "png", file)
        if (!useDefault) {
            setLastDirectory(file.path)
        }
    }

    private fun setLastDirectory(text: String) {
        try {
            BufferedWriter(PrintWriter(System.getenv("USERPROFILE") + "\\Documents\\ScreenshotAppConfig.txt")).use { bw ->
                val file = File(text)
                if (file.isFile)
                    bw.write(file.parent)
                else
                    bw.write(text)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getLastDirectory(): String {
        try {
            BufferedReader(FileReader(System.getenv("USERPROFILE") + "\\Documents\\ScreenshotAppConfig.txt")).use { reader ->
                return reader.readLine()
            }
        } catch (e: IOException) {
            return System.getenv("USERPROFILE") + "\\Documents\\"
        }
    }

    private fun clearImage() {
        canvasLayout.children.forEach {
            it as Canvas
            it.width = 0.0
            it.height = 0.0
        }
    }

    private fun clearRect(brushSize: Int, x: Double, y: Double) {
        drawCanvas.graphicsContext2D.clearRect(
            x - brushSize / 2,
            y - brushSize / 2,
            brushSize.toDouble(),
            brushSize.toDouble()
        )
    }

    private fun drawCircle(brushSize: Int, color: Color, x: Double, y: Double) {
        drawCanvas.graphicsContext2D.fill = color
        drawCanvas.graphicsContext2D.fillOval(
            x - brushSize / 2,
            y - brushSize / 2,
            brushSize.toDouble(),
            brushSize.toDouble()
        )
    }

    private fun makeScreenshot() {
        if (hideCheckBox.isSelected) {
            primaryStage.isIconified = true
        }
        Timer().schedule((timingSlider.value * 1000 + (if (hideCheckBox.isSelected) 200 else 0)).toLong()) {
            Platform.runLater {
                clearCanvasLayoutAndSetImage(getScreenshotImage())
                if (hideCheckBox.isSelected) {
                    primaryStage.isIconified = false
                }
            }

        }
    }

    private fun cropImage() {
        val params = SnapshotParameters()
        params.fill = Color.TRANSPARENT
        val drawTmp = WritableImage(
            drawCanvas.snapshot(params, null).pixelReader,
            max(min(firstX, lastX), 0.0).toInt(),
            max(min(firstY, lastY), 0.0).toInt(),
            min(abs(lastX - firstX), drawCanvas.width - max(min(firstX, lastX), 0.0).toInt()).toInt(),
            min(abs(lastY - firstY), drawCanvas.height - max(min(firstY, lastY), 0.0).toInt()).toInt()
        )
        clearCanvasLayoutAndSetImage(
            WritableImage(
                imageCanvas.snapshot(params, null).pixelReader,
                max(min(firstX, lastX), 0.0).toInt(),
                max(min(firstY, lastY), 0.0).toInt(),
                min(abs(lastX - firstX), imageCanvas.width - max(min(firstX, lastX), 0.0).toInt()).toInt(),
                min(abs(lastY - firstY), imageCanvas.height - max(min(firstY, lastY), 0.0).toInt()).toInt()
            )
        )
        drawCanvas.graphicsContext2D.drawImage(drawTmp, 0.0, 0.0)
    }

    private fun clearCanvasLayoutAndSetImage(wImage: WritableImage?) {
        if (wImage != null) {
            canvasLayout.children.forEach {
                it as Canvas
                it.width = wImage.width
                it.height = wImage.height
                it.graphicsContext2D.clearRect(0.0, 0.0, wImage.width, wImage.height)
            }
            imageCanvas.graphicsContext2D.drawImage(wImage, 0.0, 0.0)
        }
    }

    private fun getScreenshotImage(): WritableImage? {
        val robot = Robot()
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val captureRect = Rectangle(0, 0, screenSize.width, screenSize.height)
        return toFXImage(robot.createScreenCapture(captureRect), null)
    }

    init {
        val loader = FXMLLoader(javaClass.getResource("screenshotApp.fxml"))
        loader.setRoot(this)
        loader.setController(this)
        loader.load<Any>()
    }
}
