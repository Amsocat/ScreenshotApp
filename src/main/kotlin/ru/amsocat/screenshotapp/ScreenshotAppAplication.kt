package ru.amsocat.screenshotapp

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

class ScreenshotAppApplication : Application() {
    override fun start(stage: Stage) {
        val scene = Scene(ScreenshotApp(stage), 1280.0, 720.0)
        stage.title = "\"Shoot your screen\" APP"
        stage.scene = scene
        stage.show()
    }
}

fun main() {
    Application.launch(ScreenshotAppApplication::class.java)
}