package net.mikaelzero.mojito.view.sketch

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.widget.ImageView
import net.mikaelzero.mojito.Mojito
import net.mikaelzero.mojito.interfaces.ContentType
import net.mikaelzero.mojito.loader.ContentLoader
import net.mikaelzero.mojito.loader.IMojitoConfig
import net.mikaelzero.mojito.loader.OnLongTapCallback
import net.mikaelzero.mojito.loader.OnTapCallback
import net.mikaelzero.mojito.tools.ScreenUtils
import net.mikaelzero.mojito.view.sketch.core.SketchImageView
import net.mikaelzero.mojito.view.sketch.core.decode.ImageSizeCalculator


/**
 * @Author: MikaelZero
 * @CreateDate: 2020/6/10 10:01 AM
 * @Description:
 */
class SketchImageContentLoaderImpl : ContentLoader {
    lateinit var sketchImageView: SketchImageView
    var isLongHeightImage = false
    var isLongWidthImage = false

    override fun providerRealView(): View {
        return sketchImageView
    }

    override fun providerView(): View {
        return sketchImageView
    }

    override val displayRect: RectF
        get() {
            val rectF = RectF()
            sketchImageView.zoomer?.getDrawRect(rectF)
            return RectF(rectF)
        }

    override fun init(context: Context) {
        sketchImageView = SketchImageView(context)
        sketchImageView.isZoomEnabled = true
        sketchImageView.options.isDecodeGifImage = true
    }

    override val contentType: ContentType = ContentType.IMAGE

    override fun dispatchTouchEvent(
        isDrag: Boolean,
        isActionUp: Boolean,
        isDown: Boolean,
        isRight: Boolean
    ): Boolean {
        return when {
            isLongHeightImage -> {
                val result =
                    when {
                        isDrag -> {
                            false
                        }
                        isActionUp -> {
                            !isDrag
                        }
                        else -> {
                            val rectF = Rect()
                            sketchImageView.zoomer?.getVisibleRect(rectF)
                            if (Mojito.mojitoConfig()
                                    .dragMode() == IMojitoConfig.DRAG_ONLY_BOTTOM
                            ) {
                                //长图处于顶部  并且有向下滑动的趋势
                                (sketchImageView.zoomer!!.zoomScale == sketchImageView.zoomer!!.maxZoomScale
                                        && rectF.top == 0 && isDown)
                                        ||
                                        //长图不处于顶部的时候
                                        sketchImageView.zoomer!!.zoomScale == sketchImageView.zoomer!!.maxZoomScale
                                        && rectF.top != 0
                                        ||
                                        //长图处于缩放状态  由于库的bug 会出现 8.99999  和  9
                                        sketchImageView.zoomer!!.maxZoomScale - sketchImageView.zoomer!!.zoomScale > 0.01f
                            } else {
                                val drawRect = RectF()
                                sketchImageView.zoomer?.getDrawRect(drawRect)
                                //长图处于顶部  并且有向下滑动的趋势
                                sketchImageView.zoomer!!.zoomScale == sketchImageView.zoomer!!.maxZoomScale
                                        && rectF.top == 0 && isDown
                                        ||
                                        //长图不处于顶部和底部的时候
                                        sketchImageView.zoomer!!.maxZoomScale - sketchImageView.zoomer!!.zoomScale <= 0.01f
                                        && rectF.top != 0
                                        && rectF.bottom < ScreenUtils.getScreenHeight(
                                    sketchImageView.context
                                )
                                        ||
                                        //长图处于缩放状态  由于库的bug 会出现 8.99999  和  9
                                        sketchImageView.zoomer!!.maxZoomScale - sketchImageView.zoomer!!.zoomScale > 0.01f
                                        ||
                                        sketchImageView.zoomer!!.zoomScale == sketchImageView.zoomer!!.maxZoomScale
                                        && !isDown
                                        && rectF.bottom >= drawRect.bottom
                            }
                        }
                    }
                result
            }
            isLongWidthImage -> {
                val rectF = Rect()
                sketchImageView.zoomer?.getVisibleRect(rectF)
                val result = when {
                    isDrag -> {
                        false
                    }
                    isActionUp -> {
                        !isDrag
                    }
                    else -> {
                        //长图处于最大化不需要事件
                        (sketchImageView.zoomer!!.maxZoomScale - sketchImageView.zoomer!!.zoomScale > 0.01f && !isDown) ||
                                //长图处于缩放  需要事件
                                sketchImageView.zoomer!!.maxZoomScale - sketchImageView.zoomer!!.zoomScale > 0.01f
                    }
                }
                result
            }
            else -> {
                sketchImageView.zoomer!!.zoomScale > sketchImageView.zoomer!!.fullZoomScale
            }
        }
    }

    override fun dragging(width: Int, height: Int, ratio: Float) {

    }

    override fun beginBackToMin(isResetSize: Boolean) {
        if (isLongHeightImage || isLongWidthImage) {

        } else {
            if (needReBuildSize()) {
                val currentScale = sketchImageView.zoomer!!.zoomScale
                if (isResetSize) {
                    sketchImageView.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            } else {
                if (isResetSize) {
                    sketchImageView.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
        }
    }

    override fun loadAnimFinish() {
        if (isLongHeightImage || isLongWidthImage) {
//            sketchImageView.scaleType = ImageView.ScaleType.FIT_CENTER
        } else {
            sketchImageView.zoomer?.setScaleType(ImageView.ScaleType.FIT_CENTER)
        }
    }

    override fun needReBuildSize(): Boolean {
        return sketchImageView.zoomer!!.zoomScale > sketchImageView.zoomer!!.fullZoomScale
    }

    override fun isLongImage(width: Int, height: Int): Boolean {
        val sizeCalculator = ImageSizeCalculator()
        isLongHeightImage = sizeCalculator.canUseReadModeByHeight(
            width,
            height
        ) && height > (ScreenUtils.getScreenHeight(sketchImageView.context) * 1.5)
        isLongWidthImage = sizeCalculator.canUseReadModeByWidth(
            width,
            height
        ) && width > (ScreenUtils.getScreenWidth(sketchImageView.context) * 1.5)
        sketchImageView.zoomer?.isReadMode = isLongHeightImage || isLongWidthImage
        if (isLongHeightImage || isLongWidthImage) {

        } else {
            sketchImageView.zoomer!!.setScaleType(ImageView.ScaleType.CENTER_CROP)
        }
        return isLongHeightImage || isLongWidthImage
    }

    override fun onTapCallback(onTapCallback: OnTapCallback) {
        sketchImageView.zoomer?.setOnViewTapListener { view, x, y ->
            onTapCallback.onTap(view, x, y)
        }
    }

    override fun onLongTapCallback(onLongTapCallback: OnLongTapCallback) {
        sketchImageView.zoomer?.setOnViewLongPressListener { view, x, y ->
            onLongTapCallback.onLongTap(view, x, y)
        }
    }
}