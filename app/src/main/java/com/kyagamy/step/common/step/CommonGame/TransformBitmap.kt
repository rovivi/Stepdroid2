package com.kyagamy.step.Common.step.CommonGame

import android.content.Context
import android.graphics.*
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.RequiresApi

class TransformBitmap {
    companion object {
        @JvmStatic
        public fun RotateBitmap(source: Bitmap, angle: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(
                source,
                0,
                0,
                source.width,
                source.height,
                matrix,
                true
            )
        }

        @JvmStatic
        public fun FlipBitmap(source: Bitmap, eje: Boolean): Bitmap {
            val matrix = Matrix()
            if (eje) {
                matrix.postScale(-1f, 1f)
            } else {
                matrix.postScale(1f, -1f)
            }
            return Bitmap.createBitmap(
                source,
                0,
                0,
                source.width,
                source.height,
                matrix,
                true
            )
        }
        @JvmStatic
        fun makeTransparent(src: Bitmap, value: Int): Bitmap {
            val width = src.width
            val height = src.height
            val transBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(transBitmap)
            canvas.drawARGB(0, 0, 0, 0)
            // config paint
            val paint = Paint()
            paint.alpha = value
            canvas.drawBitmap(src, 0f, 0f, paint)
            return transBitmap
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
        @JvmStatic
        fun myblur(image: Bitmap?, context: Context?): Bitmap? {
            if (image == null) {
                return null
            }
            try {
                val BITMAP_SCALE = 0.4f
                val BLUR_RADIUS = 8.5f
                val width = Math.round(image.width * BITMAP_SCALE)
                val height = Math.round(image.height * BITMAP_SCALE)
                val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
                val outputBitmap = Bitmap.createBitmap(inputBitmap)
                val rs = RenderScript.create(context)
                val theIntrinsic =
                    ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
                val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
                val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)
                theIntrinsic.setRadius(BLUR_RADIUS)
                theIntrinsic.setInput(tmpIn)
                theIntrinsic.forEach(tmpOut)
                tmpOut.copyTo(outputBitmap)
                return outputBitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
        @JvmStatic
        fun getscaledBitmap(bm: Bitmap, percent: Int): Bitmap {
            val newWidth = (bm.width * percent.toFloat() / 100).toInt()
            val newHeight = (bm.width * percent.toFloat() / 100).toInt()
            val width = bm.width
            val height = bm.height
            val scaleWidth = newWidth.toFloat() / width
            val scaleHeight = newHeight.toFloat() / height
            // CREATE A MATRIX FOR THE MANIPULATION
            val matrix = Matrix()
            // RESIZE THE BIT MAP
            matrix.postScale(scaleWidth, scaleHeight)

            // "RECREATE" THE NEW BITMAP
            val resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false
            )
            bm.recycle()
            return resizedBitmap
        }
        @JvmStatic
        fun cutBitmap(fullLengthBitmap: Bitmap, percentO: Float): Bitmap {
            var percent = percentO
            percent /= 100f
            return if (percent > 0.05) {
                val backDrop = Bitmap.createBitmap(
                    (fullLengthBitmap.width * percent).toInt(),
                    fullLengthBitmap.height,
                    Bitmap.Config.RGB_565
                )
                val can = Canvas(backDrop)
                can.drawBitmap(fullLengthBitmap, 0f, 0f, null)
                backDrop
            } else {
                fullLengthBitmap
            }
        }
        @JvmStatic
        fun overlay(bmp2: Bitmap?, bmp1: Bitmap): Bitmap {
            val bmOverlay = Bitmap.createBitmap(200, 200, bmp1.config)
            val canvas = Canvas(bmOverlay)
            canvas.drawBitmap(
                bmp2!!,
                null,
                Rect(0, 0, canvas.width, canvas.height),
                Paint()
            )
            canvas.drawBitmap(
                bmp1,
                null,
                Rect(0, 0, canvas.width, canvas.height),
                Paint()
            )
            return bmOverlay
        }

        @JvmStatic
        fun adjustedContrast(src: Bitmap, value: Double): Bitmap {
            // image size
            val width = src.width
            val height = src.height
            // create output bitmap

            // create a mutable empty bitmap
            val bmOut = Bitmap.createBitmap(width, height, src.config)

            // create a canvas so that we can draw the bmOut Bitmap from source bitmap
            val c = Canvas()
            c.setBitmap(bmOut)

            // draw bitmap to bmOut from src bitmap so we can modify it
            c.drawBitmap(src, 0f, 0f, Paint(Color.BLACK))


            // color information
            var A: Int
            var R: Int
            var G: Int
            var B: Int
            var pixel: Int
            // get contrast value
            val contrast = Math.pow((100 + value) / 100, 2.0)

            // scan through all pixels
            for (x in 0 until width) {
                for (y in 0 until height) {
                    // get pixel color
                    pixel = src.getPixel(x, y)
                    A = Color.alpha(pixel)
                    // apply filter contrast for every channel R, G, B
                    R = Color.red(pixel)
                    R = (((R / 255.0 - 0.5) * contrast + 0.5) * 255.0).toInt()
                    if (R < 0) {
                        R = 0
                    } else if (R > 255) {
                        R = 255
                    }
                    G = Color.green(pixel)
                    G = (((G / 255.0 - 0.5) * contrast + 0.5) * 255.0).toInt()
                    if (G < 0) {
                        G = 0
                    } else if (G > 255) {
                        G = 255
                    }
                    B = Color.blue(pixel)
                    B = (((B / 255.0 - 0.5) * contrast + 0.5) * 255.0).toInt()
                    if (B < 0) {
                        B = 0
                    } else if (B > 255) {
                        B = 255
                    }

                    // set new pixel color to output bitmap
                    bmOut.setPixel(x, y, Color.argb(A, R, G, B))
                }
            }
            return bmOut
        }

        @JvmStatic
        fun returnMask(original: Bitmap, mask: Bitmap?): Bitmap {

            //ImageView mImageView= (ImageView)findViewById(R.id.imageview_id);
            /* Bitmap original = BitmapFactory.decodeResource(getResources(),R.drawable.content_image);
             Bitmap mask = BitmapFactory.decodeResource(getResources(),R.drawable.mask);
            */
            val result =
                Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
            val mCanvas = Canvas(result)
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            mCanvas.drawBitmap(original, 0f, 0f, null)
            // mCanvas.drawBitmap(mask, , paint);
            mCanvas.drawBitmap(
                mask!!,
                null,
                Rect(
                    -original.width,
                    -original.height,
                    original.width * 2,
                    original.height * 2
                ),
                paint
            )
            paint.xfermode = null
            return result
        }
        @JvmStatic
        fun returnMaskCut(original: Bitmap, mask: Bitmap?): Bitmap {

            //ImageView mImageView= (ImageView)findViewById(R.id.imageview_id);
            /* Bitmap original = BitmapFactory.decodeResource(getResources(),R.drawable.content_image);
             Bitmap mask = BitmapFactory.decodeResource(getResources(),R.drawable.mask);
            */
            val result =
                Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
            val mCanvas = Canvas(result)
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            mCanvas.drawBitmap(original, 0f, 0f, null)
            // mCanvas.drawBitmap(mask, , paint);
            mCanvas.drawBitmap(
                mask!!,
                null,
                Rect(0, 0, original.width, original.height),
                paint
            )
            paint.xfermode = null
            return result
        }

        /**
         * @param sprite rawImage Sprite
         * @param sizeX  divide on X blocks bitmap
         * @param sizeY  divide on Y blocks bitmap
         * @param params array of  coordinates in sprite
         * follow the next sequence
         * 012
         * 345
         * 678
         * representes of square 3x3
         * @return
         */
        fun customSpriteArray(
            sprite: Bitmap,
            sizeX: Int,
            sizeY: Int,
            vararg params: Int
        ): Array<Bitmap?> {
            val spriteArray = arrayOfNulls<Bitmap>(params.size) //crete array
            val frameWhith = sprite.width / sizeX
            val frameHeight = sprite.height / sizeY
            for (index in 0 until params.size) {
                if (params[index] < sizeX * sizeY && sizeX > 0 && sizeY > 0) { //verify for errors
                    val x = params[index] % sizeX
                    val y = params[index] / sizeX
                    spriteArray[index] = Bitmap.createBitmap(
                        sprite,
                        x * frameWhith,
                        y * frameHeight,
                        frameWhith,
                        frameHeight
                    )
                }
            }
            return spriteArray
        }

        @JvmStatic
        fun replaceColor(b: Bitmap, fromColor: Int, toColor: Int): Bitmap {
            val out = Bitmap.createBitmap(b.width, b.height, b.config)
            for (x in 0 until b.width) {
                for (y in 0 until b.height) {
                    if (b.getPixel(x, y) == fromColor) {
                        out.setPixel(x, y, toColor)
                    } else {
                        out.setPixel(x, y, b.getPixel(x, y))
                    }
                }
            }
            return out
        }



    }
}
