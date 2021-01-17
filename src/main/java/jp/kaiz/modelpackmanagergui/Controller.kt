package jp.kaiz.modelpackmanagergui

import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXListView
import com.jfoenix.controls.JFXTextArea
import com.jfoenix.controls.JFXTextField
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontSmoothingType
import javafx.scene.text.Text
import javafx.stage.DirectoryChooser
import java.io.*
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class Controller {
    @FXML
    var folderLocation: JFXTextField? = null

    @FXML
    var packListView: JFXListView<Pane>? = null

    @FXML
    var fileListView: JFXListView<Pane>? = null

    @FXML
    var searchBox: JFXTextField? = null

    @FXML
    var searchInZip: JFXCheckBox? = null

    @FXML
    var useReg: JFXCheckBox? = null

    @FXML
    var letterCase: JFXCheckBox? = null

    @FXML
    var viewPanel: AnchorPane? = null
    var folder: File? = null
        set(value) {
            if (value == null) {
                return
            }
            field = value
            folderLocation!!.text = value.path
        }

    private var fileList: Array<File>? = null
    private var selectedFileList: List<File>? = null
    private var matchFiles: Map<File, List<File>>? = null
    private var xOffset = 0.0
    private var yOffset = 0.0

    fun onSelectFolder() {
        val dc = DirectoryChooser()
        dc.title = "フォルダーを選択"
        dc.initialDirectory = File(System.getProperty("user.home"))
        folder = (dc.showDialog(Main.primaryStage))
    }

    @Synchronized
    fun onLoadModel(actionEvent: ActionEvent?) {
        val text = folderLocation!!.text
        if (folder == null) {
            if (File(text!!).isDirectory) {
                folder = File(text)
            } else {
                return
            }
        } else {
            if (folder!!.path != text) {
                if (File(text).isDirectory) {
                    folder = File(text)
                } else {
                    return
                }
            }
        }
        fileList = folder!!.listFiles()
        if (fileList == null) {
            return
        }
        matchFiles = this.findFileInZip({ true })
        packListView!!.items = FXCollections.observableArrayList()
        val s = matchFiles!!.map { entry: Map.Entry<File, List<File>> ->
            val pane: Pane = AnchorPane()
            pane.setPrefSize(360.0, 40.0)
            val name = Text(entry.key.name)
            name.font = Font("System Bold", 16.0)
            name.translateY = 10.0
            name.fontSmoothingType = FontSmoothingType.LCD
            pane.children.add(name)
            val modelCount = Text("MatchFile: " + entry.value.size)
            modelCount.font = Font("System Regular", 12.0)
            modelCount.translateY = 40.0
            modelCount.fontSmoothingType = FontSmoothingType.LCD
            modelCount.fill = Color.GRAY
            pane.children.add(modelCount)
            pane
        }.toList()
        packListView!!.items.addAll(s)
    }

    private fun findFile(match: Predicate<File>): Map<File, MutableList<File>> {
        val zipList = fileList!!.filter { file: File -> file.name.endsWith(".zip") }.toList()
        val foundFiles: MutableMap<File, MutableList<File>> = mutableMapOf()
        zipList.stream().filter(match).forEach { file: File -> foundFiles[file] = mutableListOf(file) }
        return foundFiles
    }

    private fun findFileInZip(match: Predicate<File>, zipName: String): MutableMap<File, MutableList<File>> {
        return findFileInZip(
            match,
            fileList!!.filter { file: File -> file.name == zipName }.toList()
        )
    }

    private fun findFileInZip(
        match: Predicate<File>,
        zipList: List<File> = fileList!!.filter { file: File -> file.name.endsWith(".zip") }.toMutableList()
    ): MutableMap<File, MutableList<File>> {
        val foundFiles: MutableMap<File, MutableList<File>> = mutableMapOf()
        for (file in zipList) {
            foundFiles[file] = mutableListOf()
            val zip = ZipFile(file)
            zip.stream()
                .filter { x: ZipEntry -> !x.isDirectory }
                .map { zipEntry: ZipEntry -> File(zip.name, zipEntry.name) }
                .filter(match)
                .forEach { file1: File -> foundFiles[file]!!.add(file1) }
            zip.close()
        }
        return foundFiles
    }

    private fun readText(zipName: String, filename: String?): List<String> {
        val inputStream = getInputStream(zipName, filename)
        val bis = BufferedInputStream(inputStream)
        val charset = Detector.getCharsetName(getInputStream(zipName, filename))
        val br = BufferedReader(InputStreamReader(bis, charset /*"JISAutoDetect"*/))
        return br.lines().collect(Collectors.toList())
    }

    private fun getInputStream(zipName: String, filename: String?): InputStream? {
        var inputStream: InputStream? = null
        val zipList = fileList!!.filter { file: File -> file.name == zipName }.toList()
        for (file in zipList) {
            val zip = ZipFile(file)
            val entry = zip.stream()
                .filter { zipEntry: ZipEntry -> !zipEntry.isDirectory && zipEntry.name.endsWith(filename!!) }
                .findFirst().get()
            inputStream = zip.getInputStream(entry)
            //                zip.close();
        }
        return inputStream
    }

    fun onClickSearchButton(actionEvent: ActionEvent?) {
        if (fileList == null) {
            return
        }
        val text = if (letterCase!!.isSelected) searchBox!!.text else searchBox!!.text.toLowerCase()
        matchFiles =
            if (text.isEmpty()) {
                this.findFileInZip({ inZipFile: File -> inZipFile.name.endsWith(".json") && inZipFile.name.startsWith("Model") })
            } else {
                val match = { file: File ->
                    val fileName = if (letterCase!!.isSelected) file.name else file.name.toLowerCase()
                    if (useReg!!.isSelected) fileName.matches(Regex.fromLiteral(text)) else fileName.contains(text)
                }
                if (searchInZip!!.isSelected) this.findFileInZip(match) else findFile(match)
            }
        packListView!!.items = FXCollections.observableArrayList()
        val s = matchFiles!!.entries
            .filter { fileListEntry: Map.Entry<File, List<File>> -> fileListEntry.value.isNotEmpty() }
            .map { entry: Map.Entry<File, List<File>> ->
                val pane: Pane = AnchorPane()
                pane.setPrefSize(360.0, 40.0)
                val name = Text(entry.key.name)
                name.font = Font("System Bold", 16.0)
                name.translateY = 10.0
                name.fontSmoothingType = FontSmoothingType.LCD
                pane.children.add(name)
                val modelCount = Text("MatchFile: " + entry.value.size)
                modelCount.font = Font("System Regular", 12.0)
                modelCount.translateY = 40.0
                modelCount.fontSmoothingType = FontSmoothingType.LCD
                modelCount.fill = Color.GRAY
                pane.children.add(modelCount)
                pane
            }.toList()
        packListView!!.items.addAll(s)
    }

    fun onClickPackListViewItem(mouseEvent: MouseEvent?) {
        val selectedItem = packListView!!.selectionModel.selectedItem ?: return
        val zipName = selectedItem.children[0] as Text
        selectedFileList =
            if (matchFiles == null) {
                this.findFileInZip({ true }, zipName.text).entries.stream().findFirst().get().value
            } else {
                matchFiles!!.entries.stream()
                    .filter { fileListEntry: Map.Entry<File, List<File>> -> fileListEntry.key.name == zipName.text }
                    .findFirst().get().value
            }
        fileListView!!.items = FXCollections.observableArrayList()
        selectedFileList!!.forEach { file: File ->
            val pane: Pane = AnchorPane()
            pane.setPrefSize(360.0, 10.0)
            val fileName = Text(file.name)
            fileName.font = Font("System Bold", 12.0)
            fileName.translateY = 2.0
            fileName.fontSmoothingType = FontSmoothingType.LCD
            pane.children.add(fileName)
            fileListView!!.items.add(pane)
        }
    }

    fun onClickFileListViewItem(mouseEvent: MouseEvent?) {
        val selectedZip = packListView!!.selectionModel.selectedItem ?: return
        val zipName = selectedZip.children[0] as Text
        val selectedFile = fileListView!!.selectionModel.selectedItem ?: return
        val fileName = selectedFile.children[0] as Text
        viewPanel!!.children.clear()
        when {
            textFileTypes.any { suffix: String -> fileName.text.endsWith(suffix) } -> {
                val text = this.readText(zipName.text, fileName.text)
                val textArea = JFXTextArea()
                viewPanel!!.children.add(textArea)
                textArea.isWrapText = true
                textArea.prefWidth = 420.0
                textArea.prefHeight = 640.0
                textArea.stylesheets.add(javaClass.classLoader.getResource("scroll.css").toExternalForm())
                textArea.isEditable = false
                textArea.font = Font("System Regular", 14.0)
                textArea.isFocusTraversable = false
                text.forEach(Consumer { text1: String ->
                    textArea.appendText("$text1\n")
                })
                textArea.scrollTop = Double.MAX_VALUE
            }
            imageFileTypes.any { suffix: String -> fileName.text.endsWith(suffix) } -> {
                val image = Image(getInputStream(zipName.text, fileName.text))
                val imageView = ImageView(image)
                viewPanel!!.children.add(imageView)
                imageView.fitWidthProperty().set(viewPanel!!.width)
                imageView.fitHeightProperty().set(viewPanel!!.height)
                imageView.isPreserveRatio = true
                imageView.isSmooth = true
                imageView.maxWidth(viewPanel!!.width)
                imageView.maxHeight(viewPanel!!.height)
                viewPanel!!.onScroll = EventHandler { event: ScrollEvent ->
                    val scale = 0.0.coerceAtLeast(imageView.scaleX + event.deltaY / 500)
                    imageView.scaleX = scale
                    imageView.scaleY = scale
                }
                viewPanel!!.onMousePressed = EventHandler { event: MouseEvent ->
                    xOffset = imageView.x
                    yOffset = imageView.y
                }
                viewPanel!!.onMouseDragged = EventHandler { event: MouseEvent ->
                    imageView.translateX = event.sceneX - xOffset
                    imageView.translateY = event.sceneY - yOffset
                }
            }
            soundFileTypes.any { suffix: String -> fileName.text.endsWith(suffix) } -> {
            }
        }
    }

    companion object {
        private val textFileTypes = arrayOf(".txt", ".js", ".json")
        private val imageFileTypes = arrayOf(".png", ".jpeg")
        private val soundFileTypes = arrayOf(".ogg", ".mp3")
    }
}