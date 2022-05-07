package watermark

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun main() {
    println("Input the image filename:")
    val image = inputImage("image")
    println("Input the watermark image filename:")
    val watermark = inputImage("watermark")

    if (image.height < watermark.height || image.width < watermark.width) {
        println("The watermark's dimensions are larger.")
        exitProcess(0)
    }
    var isAlpha = false
    var isTransColor = false
    var transColor = List(3) { 0 }

    if (watermark.colorModel.transparency == 3) {
        println("Do you want to use the watermark's Alpha channel?")
        isAlpha = readln().lowercase() == "yes"
    } else {
        println("Do you want to set a transparency color?")
        isTransColor = readln().lowercase() == "yes"

        if (isTransColor) {
            println("Input a transparency color ([Red] [Green] [Blue]):")
            transColor = try {
                readln().split(" ").map { it.toInt() }
            } catch (e: NumberFormatException) {
                println("The transparency color input is invalid.")
                exitProcess(0)
            }
            if (
                transColor.size != 3 ||
                transColor[0] !in 0..255 ||
                transColor[1] !in 0..255 ||
                transColor[2] !in 0..255
            ) {
                println("The transparency color input is invalid.")
                exitProcess(0)
            }
        }
    }

    println("Input the watermark transparency percentage (Integer 0-100):")
    val transPercent = try {
        readln().toInt()
    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(0)
    }
    if (transPercent !in 0..100) {
        println("The transparency percentage is out of range.")
        exitProcess(0)
    }

    var isSingle = false
    var singlePosition = List(2) {0}
    println("Choose the position method (single, grid):")
    val positionMetod = readln().lowercase()
    if (positionMetod !in listOf("single", "grid")) {
        println("The position method input is invalid.")
        exitProcess(0)
    } else {
        if (positionMetod == "single") {
            val dx = image.width - watermark.width
            val dy = image.height - watermark.height
            println("Input the watermark position ([x 0-$dx] [y 0-$dy]):")
            singlePosition = try {
                readln().split(" ").map { it.toInt() }
            } catch (e: NumberFormatException) {
                println("The position input is invalid.")
                exitProcess(0)
            }
            if (
                singlePosition.size != 2 ||
                singlePosition[0] !in 0..dx ||
                singlePosition[1] !in 0..dy
            ) {
                println("The position input is out of range.")
                exitProcess(0)
            }
            isSingle = true
        }
    }

    println("Input the output image filename (jpg or png extension):")
    val outputFileName = readln()
    val filExtension = outputFileName.substringAfterLast('.')
    if (filExtension !in listOf("jpg", "png")) {
        println("The output file extension isn't \"jpg\" or \"png\".")
        exitProcess(0)
    }
    val imageOut = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
    creatWatermark(image, watermark, transPercent, imageOut, isAlpha, isTransColor, transColor, isSingle, singlePosition)
    val outputFile = File(outputFileName)
    ImageIO.write(imageOut, filExtension, outputFile)
    println("The watermarked image $outputFileName has been created.")
}

fun creatWatermark(
    image: BufferedImage,
    watermark: BufferedImage,
    transPercent: Int,
    imageOut: BufferedImage,
    isAlpha: Boolean,
    isTransColor: Boolean,
    transColor: List<Int>,
    isSingle: Boolean,
    singlePosition: List<Int>
) {

    val col = { w: Color, i: Color ->
        Color(
            (transPercent * w.red + (100 - transPercent) * i.red) / 100,
            (transPercent * w.green + (100 - transPercent) * i.green) / 100,
            (transPercent * w.blue + (100 - transPercent) * i.blue) / 100
        )

    }

    var wx = 0
    var wy = 0
    var dx = 0
    var dy = 0

    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            val i = Color(image.getRGB(x, y))
            val color: Color

            val waterColor = { al: Boolean ->
                if (isSingle) {
                    if (
                        x - singlePosition[0] in 0 until watermark.width &&
                        y - singlePosition[1] in 0 until watermark.height
                    ) {
                        wx = x - singlePosition[0]
                        wy = y - singlePosition[1]

                    }
                } else {

                    if (x % watermark.width == 0) dx = x
                    if (y % watermark.height == 0) dy = y
                    wx = x - dx
                    wy = y - dy
                }
                Color(watermark.getRGB(wx, wy), al)
            }

            color = if (isAlpha) {

                val w = waterColor(true)
                val wA = w.alpha

                if (wA == 255) {
                    col(w, i)
                } else i

            } else {
                if (isTransColor) {
                    val w = waterColor(false)
                    val tc = Color(transColor[0], transColor[1], transColor[2])
                    if (w != tc) {
                        col(w, i)
                    } else i

                } else {
                    val w = waterColor(false)
                    col(w, i)
                }
            }
            imageOut.setRGB(x, y, color.rgb)
        }
    }
}

fun inputImage(str: String = "image"): BufferedImage {

    fun BufferedImage.isCorrect() {
        if (this.colorModel.numColorComponents != 3) {
            println("The number of $str color components isn't 3.")
            exitProcess(0)
        }
        if (this.colorModel.pixelSize !in listOf(24, 32)) {
            println("The $str isn't 24 or 32-bit.")
            exitProcess(0)
        }
    }

    val imageFileName = readln()
    val imageFile = File(imageFileName)
    if (imageFile.exists()) {
        val image: BufferedImage = ImageIO.read(imageFile)
        image.isCorrect()
        return image
    } else {
        println("The file $imageFileName doesn't exist.")
        exitProcess(0)
    }
}