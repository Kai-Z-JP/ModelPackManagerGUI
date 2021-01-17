package jp.kaiz.modelpackmanagergui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class Main : Application() {
    override fun start(stage: Stage) {
        val root = FXMLLoader.load<Parent>(javaClass.classLoader.getResource("main.fxml"))
        stage.title = "ModelPackManagerGUI"
        stage.scene = Scene(root, 1280.0, 720.0)
        stage.icons.add(Image(javaClass.classLoader.getResourceAsStream("icon.png")))
        stage.show()
        primaryStage = stage
    }

    companion object {
        var primaryStage: Stage? = null
    }
}

fun main(args: Array<String>) {
    Application.launch(Main::class.java, *args)
}