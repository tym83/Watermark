package watermark

import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun main() {
    val watermarkCreator = WatermarkCreator()
    val originalImage = watermarkCreator.takeOriginalImage()
    val waterMarkImage = watermarkCreator.takeWatermarkImage(originalImage)
    watermarkCreator.setBlendImageParameters(originalImage, waterMarkImage)
}
class WatermarkCreator (
    private var needTransparency: Boolean = false,
    private var transparencyColor: Color? = null,
    private var positionMethod: String = "",
    private var positionInput: MutableList<Int> = mutableListOf()
) {
    fun takeOriginalImage(): BufferedImage {
        println("Input the image filename:")
        val imageName = File(readln())
        if (!imageName.exists()) {
            println("The file $imageName doesn't exist.")
            exitProcess(-1)
        }

        val image: BufferedImage = ImageIO.read(imageName)

        if (image.colorModel.numColorComponents != 3) {
            println("The number of image color components isn't 3.")
            exitProcess(-1)
        }
        if (image.colorModel.pixelSize != 24 && image.colorModel.pixelSize != 32) {
            println("The image isn't 24 or 32-bit.")
            exitProcess(-1)
        }

        return image
    }

    fun takeWatermarkImage(originalImage: BufferedImage): BufferedImage {
        println("Input the watermark image filename:")
        val waterMarkName = File(readln())
        if (!waterMarkName.exists()) {
            println("The file $waterMarkName doesn't exist.")
            exitProcess(-1)
        }

        val waterMark: BufferedImage = ImageIO.read(waterMarkName)

        when {
            waterMark.colorModel.numColorComponents != 3 -> {
                println("The number of watermark color components isn't 3.")
                exitProcess(-1)
            }
            waterMark.colorModel.pixelSize != 24 && waterMark.colorModel.pixelSize != 32 -> {
                println("The watermark isn't 24 or 32-bit.")
                exitProcess(-1)
            }
            waterMark.height > originalImage.height || waterMark.width > originalImage.width -> {
                println("The watermark's dimensions are larger.")
                exitProcess(-1)
            }
            waterMark.colorModel.transparency == 3 -> {
                println("Do you want to use the watermark's Alpha channel?")
                if (readln() == "yes") needTransparency = true
            }
            else -> {
                println("Do you want to set a transparency color?")
                if (readln() == "yes") {
                    needTransparency = true
                    println("Input a transparency color ([Red] [Green] [Blue]):")
                    val inputCol = readln().split(" ").map { it }
                    if (inputCol.count { it.matches("\\d*".toRegex()) && it.toInt() in 0..255 } != 3) {
                        println("The transparency color input is invalid.")
                        exitProcess(-1)
                    } else {
                        transparencyColor = Color(inputCol[0].toInt(), inputCol[0].toInt(), inputCol[0].toInt())
                    }
                }
            }
        }

        return waterMark
    }

    fun setBlendImageParameters(originalImage: BufferedImage, waterMarkImage: BufferedImage) {
        println("Input the watermark transparency percentage (Integer 0-100):")
        val inputTransparency = readln()
        try {
            inputTransparency.toInt()
            if (inputTransparency.toInt() !in 0..100) {
                println("The transparency percentage is out of range.")
                exitProcess(-1)
            }
        } catch (e: NumberFormatException) {
            println("The transparency percentage isn't an integer number.")
            exitProcess(-1)
        }

        val transpPercent = inputTransparency.toInt()

        println("Choose the position method (single, grid):")
        positionMethod = readln()

        when (positionMethod) {
            "single" -> {
                println("Input the watermark position " +
                        "([x 0-${originalImage.width - waterMarkImage.width}] " +
                        "[y 0-${originalImage.height - waterMarkImage.height}]):"
                )
                val inputPos = readln().split(" ").map { it }
                if (inputPos.count { it.matches("-?\\d*".toRegex()) } != 2) {
                    println("The position input is invalid.")
                    exitProcess(-1)
                } else if (
                    inputPos[0].toInt() !in 0..originalImage.width - waterMarkImage.width ||
                    inputPos[1].toInt() !in 0..originalImage.height - waterMarkImage.height ||
                    inputPos.count { it.matches("-\\d*".toRegex()) } > 0
                ) {
                    println("The position input is out of range.")
                    exitProcess(-1)
                } else {
                    inputPos.forEach { positionInput.add(it.toInt()) }
                }
            }
            "grid" -> {
                repeat(2) {
                    positionInput.add(0)
                }
            }
            else -> {
                println("The position method input is invalid.")
                exitProcess(-1)
            }
        }

        println("Input the output image filename (jpg or png extension):")
        val outputFileName = File(readln())

        if (!outputFileName.name.contains(".jpg") && !outputFileName.name.contains(".png")) {
            println("The output file extension isn't \"jpg\" or \"png\".")
            exitProcess(-1)
        }
        makeWatermark(transpPercent, originalImage, waterMarkImage, outputFileName)
    }

    private fun makeWatermark(
        transpPercent: Int,
        originalImage: BufferedImage,
        waterMarkImage: BufferedImage,
        outputFileName: File
    ) {
        val blendedImage = BufferedImage(originalImage.width, originalImage.height, BufferedImage.TYPE_INT_RGB)
        val fileType = outputFileName.name.takeLast(3)

        if (positionMethod == "single") {
            for (x in 0 until blendedImage.width) {
                for (y in 0 until blendedImage.height) {
                    val origCol = Color(originalImage.getRGB(x ,y))
                    val waterCol = if ((x in positionInput[0] until positionInput[0] + waterMarkImage.width &&
                        y in positionInput[1] until positionInput[1] + waterMarkImage.height)
                    ) {
                        Color(waterMarkImage.getRGB(x - positionInput[0] ,y - positionInput[1]), needTransparency)
                    } else {
                        Color(0, 0, 0, 0)
                    }
                    val outputColor = if (waterCol.alpha == 0 || transparencyColor == waterCol) {
                        Color(origCol.rgb)
                    } else {
                        Color(
                            (transpPercent * waterCol.red + (100 - transpPercent) * origCol.red) / 100,
                            (transpPercent * waterCol.green + (100 - transpPercent) * origCol.green) / 100,
                            (transpPercent * waterCol.blue + (100 - transpPercent) * origCol.blue) / 100
                        )
                    }
                    blendedImage.setRGB(x, y, outputColor.rgb)
                }
            }
        } else {
            for (x in 0 until blendedImage.width) {
                for (y in 0 until blendedImage.height) {
                    val origCol = Color(originalImage.getRGB(x ,y))
                    val waterCol = Color(waterMarkImage
                        .getRGB(x % waterMarkImage.width,y % waterMarkImage.height), needTransparency
                    )
                    val outputColor = if (waterCol.alpha == 0 || transparencyColor == waterCol) {
                        Color(origCol.rgb)
                    } else {
                        Color(
                            (transpPercent * waterCol.red + (100 - transpPercent) * origCol.red) / 100,
                            (transpPercent * waterCol.green + (100 - transpPercent) * origCol.green) / 100,
                            (transpPercent * waterCol.blue + (100 - transpPercent) * origCol.blue) / 100
                        )
                    }
                    blendedImage.setRGB(x, y, outputColor.rgb)
                }
            }
        }

        ImageIO.write(blendedImage, fileType, outputFileName)
        println("The watermarked image $outputFileName has been created.")
    }
}
