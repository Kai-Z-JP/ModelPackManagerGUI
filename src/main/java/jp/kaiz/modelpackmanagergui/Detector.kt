package jp.kaiz.modelpackmanagergui

import org.mozilla.universalchardet.UniversalDetector
import java.io.InputStream
import java.nio.charset.Charset

object Detector {
    fun getCharsetName(inputStream: InputStream?): Charset {
        val buf = ByteArray(4096)
        val detector = UniversalDetector(null)
        var nread: Int
        while (inputStream!!.read(buf).also { nread = it } > 0 && !detector.isDone) {
            detector.handleData(buf, 0, nread)
        }

        //推測結果を取得する
        detector.dataEnd()
        val detectedCharset = detector.detectedCharset
        detector.reset()
        return if (detectedCharset != null && detectedCharset.isNotEmpty()) {
            Charset.forName(detectedCharset)
        } else
            Charset.forName(System.getProperty("file.encoding"))
        //文字コードを取得できなかった場合、環境のデフォルトを使用する
    }
}