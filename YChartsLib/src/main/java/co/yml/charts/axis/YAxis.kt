package co.yml.charts.axis

import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.chartcontainer.container.vertical
import co.yml.charts.common.extensions.getTextHeight
import co.yml.charts.common.extensions.getTextWidth
import co.yml.charts.common.model.Point
import kotlin.math.ceil

/**
 *
 * YAxis compose method used for drawing yAxis in any given graph.
 * @param modifier : All modifier related property.
 * @param yAxisData : All data needed to draw Yaxis.
 * @param scrollOffset : Offset of delta scrolled position.
 * @param zoomScale : Scale at which zoom transformation being applied.
 * @param chartData : List of data points used in the graph.
 * @param dataCategoryWidth length of horizontal bar
 * @param yStart start position of Y axis pointer
 * @see co.yml.charts.axis Data class to save all params related to Yaxis
 */

@Composable
fun YAxis(
    modifier: Modifier,
    yAxisData: AxisData,
    scrollOffset: Float = 0f,
    zoomScale: Float = 0f,
    chartData: List<Point> = emptyList(),
    dataCategoryWidth: Float = 0f,
    yStart: Float = 0f,
    barWidth: Float = 0f
) {
    with(yAxisData) {
        var yAxisWidth by remember { mutableStateOf(0.dp) }
        val isRightAligned = axisPos == Gravity.RIGHT

        Box {

            Column(modifier = modifier.clipToBounds()) {
                val steps = steps + 1
                Canvas(
                    modifier = modifier
                        .clipToBounds()
                        .width(yAxisWidth)
                        .semantics {
                            this.testTag = "y_axis"
                        }
                        .background(backgroundColor)
                ) {
                    val (yAxisHeight, segmentHeight) = getAxisInitValues(
                        yAxisData,
                        size.height,
                        axisBottomPadding.toPx(),
                        axisTopPadding.toPx(),
                        dataCategoryOptions.isDataCategoryInYAxis,
                        dataCategoryWidth
                    )
                    val (_, _, yAxisScale) = getYAxisScale(chartData, yAxisData.steps)
                    val yPositionFromBottom = yAxisHeight - yStart + scrollOffset

                    var yPos =
                        if (dataCategoryOptions.isDataCategoryStartFromBottom) yPositionFromBottom else {
                            if (zoomScale < 1) yPositionFromBottom else yStart - scrollOffset
                        }

                    val axisLabelPadding = if(yAxisData.unitsLabel.isBlank()) {
                        0.dp
                    } else
                    {
                        15.dp
                    }

                    for (index in 0 until steps) {
                        // Drawing the axis labels
                        yAxisWidth = drawAxisLabel(
                            yPos,
                            index,
                            yAxisData,
                            yAxisWidth,
                            isRightAligned,
                            yAxisHeight,
                            segmentHeight,
                            zoomScale,
                            steps - 1,
                            axisLabelPadding
                        )
                        drawAxisLineWithPointers(
                            yPos,
                            yAxisData,
                            index,
                            steps,
                            isRightAligned,
                            yAxisWidth,
                            yAxisHeight,
                            segmentHeight,
                            zoomScale,
                            yAxisScale,
                            yStart,
                            barWidth
                        )
                        val yPosChangeFromBottom = (axisStepSize.toPx() * (zoomScale * yAxisScale))
                        if (dataCategoryOptions.isDataCategoryStartFromBottom) {
                            yPos -= yPosChangeFromBottom
                        } else {
                            if (zoomScale < 1) {
                                yPos -= yPosChangeFromBottom
                            } else {
                                yPos += ((axisStepSize.toPx() * (zoomScale * yAxisScale)))
                            }
                        }

                    }
                }
            }

            Box(contentAlignment = Alignment.CenterStart,
                modifier = Modifier.align(Alignment.CenterStart)
                    .vertical()
                    .rotate(-90.0F)) {
                Text(yAxisData.unitsLabel,
                    style = MaterialTheme.typography.labelMedium)
            }


        }

    }
}

fun getAxisInitValues(
    axisData: AxisData,
    canvasHeight: Float,
    bottomPadding: Float,
    topPadding: Float,
    isDataCategoryInYAxis: Boolean = false,
    dataCategoryWidth: Float = 0f
): Pair<Float, Float> = with(axisData) {
    val yAxisHeight = canvasHeight - bottomPadding

    // Minus the top padding to avoid cropping at the top
    val segmentHeight = if (isDataCategoryInYAxis) {
        dataCategoryWidth
    } else {
        (yAxisHeight - topPadding) / axisData.steps
    }
    Pair(yAxisHeight, segmentHeight)
}


private fun DrawScope.drawAxisLineWithPointers(
    yPos: Float,
    axisData: AxisData,
    index: Int,
    totalSteps: Int,
    isRightAligned: Boolean,
    yAxisWidth: Dp,
    yAxisHeight: Float,
    segmentHeight: Float,
    zoomScale: Float,
    yAxisScale: Float,
    yStart: Float = 0f,
    barWidth: Float
) {
    with(axisData) {
        if (axisConfig.isAxisLineRequired) {
            // Draw line only until reqXLabelsQuo -1 else will be a extra line at top with no label
            val axisStepWidth = (axisStepSize.toPx() * (zoomScale * yAxisScale))

            if (startDrawPadding != 0.dp && dataCategoryOptions.isDataCategoryInYAxis) {
                drawLine(
                    start = Offset(
                        x = if (isRightAligned) 0.dp.toPx() else yAxisWidth.toPx(),
                        y = yAxisHeight - startDrawPadding.toPx() * zoomScale
                    ),
                    end = Offset(
                        x = if (isRightAligned) 0.dp.toPx() else yAxisWidth.toPx(),
                        y = yAxisHeight
                    ),
                    color = axisLineColor, strokeWidth = axisLineThickness.toPx()
                )
            }

            if (index != (totalSteps - 1)) {
                //Draw Yaxis line
                drawLine(
                    start = Offset(
                        x = if (isRightAligned) 0.dp.toPx() else yAxisWidth.toPx(),
                        y = if (dataCategoryOptions.isDataCategoryInYAxis) {
                            if (shouldDrawAxisLineTillEnd) {
                                if (dataCategoryOptions.isDataCategoryStartFromBottom) yPos else {
                                    if (zoomScale < 1) yPos else yPos - axisStepWidth / 2
                                }
                            } else
                                yPos
                        } else yAxisHeight - (segmentHeight * index)
                    ),
                    end = Offset(
                        x = if (isRightAligned) 0.dp.toPx() else yAxisWidth.toPx(),
                        y = if (dataCategoryOptions.isDataCategoryInYAxis) {
                            if (dataCategoryOptions.isDataCategoryStartFromBottom) {
                                if (shouldDrawAxisLineTillEnd) {
                                    yPos - axisStepWidth - barWidth / 2
                                } else {
                                    yPos - axisStepWidth
                                }
                            } else {
                                if (zoomScale < 1) {
                                    if (shouldDrawAxisLineTillEnd) {
                                        yPos - axisStepWidth - barWidth / 2
                                    } else {
                                        yPos - axisStepWidth
                                    }

                                } else {
                                    if (shouldDrawAxisLineTillEnd) {
                                        yPos + axisStepWidth + barWidth / 2 + (startDrawPadding.toPx() * zoomScale)
                                    } else {
                                        yPos + axisStepWidth + (startDrawPadding.toPx() * zoomScale)
                                    }
                                }
                            }
                        } else yAxisHeight - (segmentHeight * (index + 1))
                    ),
                    color = axisLineColor, strokeWidth = axisLineThickness.toPx()
                )
            }

            //Draw pointer lines on Yaxis
            drawLine(
                start = Offset(
                    x = if (isRightAligned) 0.dp.toPx() else {
                        yAxisWidth.toPx() - indicatorLineWidth.toPx()
                    },
                    y = if (dataCategoryOptions.isDataCategoryInYAxis) {
                        yPos
                    } else yAxisHeight - (segmentHeight * index)
                ),
                end = Offset(
                    x = if (isRightAligned) indicatorLineWidth.toPx() else yAxisWidth.toPx(),
                    y = if (dataCategoryOptions.isDataCategoryInYAxis) {
                        yPos
                    } else yAxisHeight - (segmentHeight * index)
                ),
                color = axisLineColor, strokeWidth = axisLineThickness.toPx()
            )
        }
    }
}


private fun DrawScope.drawAxisLabel(
    yPos: Float,
    index: Int,
    axisData: AxisData,
    yAxisWidth: Dp,
    isRightAligned: Boolean,
    yAxisHeight: Float,
    segmentHeight: Float,
    zoomScale: Float,
    lastIndex: Int,
    extraPadding: Dp
): Dp = with(axisData) {
    var calculatedYAxisWidth = yAxisWidth
    val yAxisTextPaint = TextPaint().apply {
        textSize = axisLabelFontSize.toPx()
        color = axisLabelColor.toArgb()
        textAlign = if (isRightAligned) Paint.Align.RIGHT else Paint.Align.LEFT
        typeface = axisData.typeface
    }
    val yAxisLabel = if (dataCategoryOptions.isDataCategoryInYAxis) {
        if (dataCategoryOptions.isDataCategoryStartFromBottom) labelData(index) else {
            if (zoomScale < 1) labelData(lastIndex - index) else labelData(index)
        }
    } else labelData(index)
    val measuredWidth = yAxisLabel.getTextWidth(yAxisTextPaint)
    val height: Int = yAxisLabel.getTextHeight(yAxisTextPaint)
    if (measuredWidth > calculatedYAxisWidth.toPx()) {
        val width =
            if (axisConfig.shouldEllipsizeAxisLabel) {
                axisConfig.minTextWidthToEllipsize
            } else measuredWidth.toDp()
        calculatedYAxisWidth =
            width + labelAndAxisLinePadding + axisOffset + axisStartPadding + extraPadding
    }
    val ellipsizedText = TextUtils.ellipsize(
        yAxisLabel,
        yAxisTextPaint,
        axisConfig.minTextWidthToEllipsize.toPx(),
        axisConfig.ellipsizeAt
    )
    drawContext.canvas.nativeCanvas.apply {
        drawText(
            if (axisConfig.shouldEllipsizeAxisLabel) ellipsizedText.toString() else yAxisLabel,
            if (isRightAligned) calculatedYAxisWidth.toPx() - labelAndAxisLinePadding.toPx() else {
                axisStartPadding.toPx() + extraPadding.toPx()
            },
            if (dataCategoryOptions.isDataCategoryInYAxis)
                yPos + height / 2
            else yAxisHeight + height / 2 - ((segmentHeight * index)),
            yAxisTextPaint
        )
    }
    return calculatedYAxisWidth
}

/**
 * Returns triple of Ymax, Ymin & scale for given list of points and steps
 * @param points: List of points in axis
 * @param steps: Total steps in axis
 */
fun getYAxisScale(
    points: List<Point>,
    steps: Int,
): Triple<Float, Float, Float> {
    val yMin = points.takeIf { it.isNotEmpty() }?.minOf { it.y } ?: 0f
    val yMax = points.takeIf { it.isNotEmpty() }?.maxOf { it.y } ?: 0f
    val totalSteps = (yMax - yMin)
    val temp = totalSteps / steps
    val scale = ceil(temp)
    return Triple(yMin, yMax, scale)
}

@Preview(showBackground = true)
@Composable
private fun YAxisPreview() {
    val yAxisData = AxisData.Builder()
        .steps(5)
        .bottomPadding(10.dp)
        .axisPosition(Gravity.LEFT)
        .axisLabelFontSize(14.sp)
        .labelData { index -> index.toString() }
        .build()
    YAxis(
        modifier = Modifier.height(300.dp), yAxisData = yAxisData,
        scrollOffset = 0f,
        zoomScale = 1f,
        chartData = listOf(),
        150f
    )
}
