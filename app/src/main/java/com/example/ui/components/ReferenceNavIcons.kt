package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ReferenceNavIconKind {
    HOME, GROUPS, STUDENTS, CALENDAR, FINANCE, SESSION,
    THEME, LANGUAGE, NOTIFICATION, CARDS, SECURITY, DATA, SOCIAL, ABOUT
}

/** Line icons matching the visual language used by the bundled Ostazi reference app. */
@Composable
fun ReferenceNavIcon(
    kind: ReferenceNavIconKind,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * .085f
        val line = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (kind) {
            ReferenceNavIconKind.HOME -> {
                val house = Path().apply {
                    moveTo(w * .14f, h * .44f)
                    lineTo(w * .50f, h * .15f)
                    lineTo(w * .86f, h * .44f)
                    lineTo(w * .86f, h * .84f)
                    lineTo(w * .14f, h * .84f)
                    close()
                }
                drawPath(house, color, style = line)
                val door = Path().apply {
                    moveTo(w * .38f, h * .84f)
                    lineTo(w * .38f, h * .62f)
                    quadraticTo(w * .50f, h * .53f, w * .62f, h * .62f)
                    lineTo(w * .62f, h * .84f)
                }
                drawPath(door, color, style = line)
            }
            ReferenceNavIconKind.GROUPS -> {
                drawCircle(color, w * .15f, Offset(w * .60f, h * .30f), style = Stroke(stroke))
                drawPath(Path().apply {
                    moveTo(w * .36f, h * .86f)
                    quadraticTo(w * .36f, h * .58f, w * .60f, h * .58f)
                    quadraticTo(w * .84f, h * .58f, w * .84f, h * .86f)
                }, color, style = line)
                drawCircle(color, w * .12f, Offset(w * .28f, h * .36f), style = Stroke(stroke * .85f))
                drawPath(Path().apply {
                    moveTo(w * .12f, h * .86f)
                    quadraticTo(w * .12f, h * .64f, w * .28f, h * .64f)
                }, color, style = Stroke(stroke * .85f, cap = StrokeCap.Round))
            }
            ReferenceNavIconKind.STUDENTS -> {
                drawCircle(color, w * .18f, Offset(w * .50f, h * .34f), style = Stroke(stroke))
                drawPath(Path().apply {
                    moveTo(w * .18f, h * .86f)
                    quadraticTo(w * .18f, h * .60f, w * .50f, h * .60f)
                    quadraticTo(w * .82f, h * .60f, w * .82f, h * .86f)
                }, color, style = line)
            }
            ReferenceNavIconKind.CALENDAR -> {
                drawRoundRect(color, Offset(w * .12f, h * .22f), Size(w * .76f, h * .66f), CornerRadius(w * .16f), style = Stroke(stroke))
                drawLine(color, Offset(w * .12f, h * .42f), Offset(w * .88f, h * .42f), stroke * .85f)
                drawLine(color, Offset(w * .32f, h * .12f), Offset(w * .32f, h * .24f), stroke, StrokeCap.Round)
                drawLine(color, Offset(w * .68f, h * .12f), Offset(w * .68f, h * .24f), stroke, StrokeCap.Round)
                drawCircle(color, w * .045f, Offset(w * .38f, h * .62f))
                drawCircle(color, w * .045f, Offset(w * .62f, h * .62f))
            }
            ReferenceNavIconKind.FINANCE -> {
                drawRoundRect(color, Offset(w * .12f, h * .14f), Size(w * .76f, h * .74f), CornerRadius(w * .18f), style = Stroke(stroke))
                drawLine(color, Offset(w * .30f, h * .70f), Offset(w * .30f, h * .52f), stroke * 1.1f, StrokeCap.Round)
                drawLine(color, Offset(w * .50f, h * .70f), Offset(w * .50f, h * .34f), stroke * 1.1f, StrokeCap.Round)
                drawLine(color, Offset(w * .70f, h * .70f), Offset(w * .70f, h * .46f), stroke * 1.1f, StrokeCap.Round)
            }
            ReferenceNavIconKind.SESSION -> {
                val len = w * .28f
                val s = w * .09f
                listOf(
                    Path().apply { moveTo(w*.12f,h*.12f+len); lineTo(w*.12f,h*.12f); lineTo(w*.12f+len,h*.12f) },
                    Path().apply { moveTo(w*.88f-len,h*.12f); lineTo(w*.88f,h*.12f); lineTo(w*.88f,h*.12f+len) },
                    Path().apply { moveTo(w*.12f,h*.88f-len); lineTo(w*.12f,h*.88f); lineTo(w*.12f+len,h*.88f) },
                    Path().apply { moveTo(w*.88f-len,h*.88f); lineTo(w*.88f,h*.88f); lineTo(w*.88f,h*.88f-len) }
                ).forEach { drawPath(it, color, style = Stroke(s, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                drawCircle(color, w * .08f, Offset(w * .5f, h * .5f))
            }
            ReferenceNavIconKind.THEME -> {
                val moon = Path().apply {
                    moveTo(w * .68f, h * .15f)
                    cubicTo(w * .42f, h * .20f, w * .30f, h * .44f, w * .39f, h * .65f)
                    cubicTo(w * .48f, h * .86f, w * .73f, h * .88f, w * .87f, h * .68f)
                    cubicTo(w * .52f, h * .72f, w * .39f, h * .36f, w * .68f, h * .15f)
                }
                drawPath(moon, color, style = line)
            }
            ReferenceNavIconKind.LANGUAGE -> {
                drawCircle(color, w * .36f, Offset(w * .5f, h * .5f), style = Stroke(stroke))
                drawOval(color, Offset(w*.34f,h*.14f), Size(w*.32f,h*.72f), style = Stroke(stroke*.78f))
                drawLine(color, Offset(w*.16f,h*.50f), Offset(w*.84f,h*.50f), stroke*.78f, StrokeCap.Round)
            }
            ReferenceNavIconKind.NOTIFICATION -> {
                val bell = Path().apply {
                    moveTo(w*.24f,h*.66f); quadraticTo(w*.32f,h*.58f,w*.32f,h*.38f)
                    quadraticTo(w*.32f,h*.18f,w*.50f,h*.18f)
                    quadraticTo(w*.68f,h*.18f,w*.68f,h*.38f)
                    quadraticTo(w*.68f,h*.58f,w*.76f,h*.66f)
                    lineTo(w*.24f,h*.66f)
                }
                drawPath(bell,color,style=line)
                drawLine(color,Offset(w*.43f,h*.78f),Offset(w*.57f,h*.78f),stroke,StrokeCap.Round)
            }
            ReferenceNavIconKind.CARDS -> {
                drawRoundRect(color,Offset(w*.13f,h*.22f),Size(w*.74f,h*.58f),CornerRadius(w*.12f),style=Stroke(stroke))
                drawCircle(color,w*.09f,Offset(w*.32f,h*.48f),style=Stroke(stroke*.8f))
                drawLine(color,Offset(w*.48f,h*.42f),Offset(w*.74f,h*.42f),stroke*.75f,StrokeCap.Round)
                drawLine(color,Offset(w*.48f,h*.58f),Offset(w*.68f,h*.58f),stroke*.75f,StrokeCap.Round)
            }
            ReferenceNavIconKind.SECURITY -> {
                val shield = Path().apply {
                    moveTo(w*.50f,h*.12f); lineTo(w*.79f,h*.24f); lineTo(w*.75f,h*.58f)
                    quadraticTo(w*.68f,h*.80f,w*.50f,h*.89f)
                    quadraticTo(w*.32f,h*.80f,w*.25f,h*.58f); lineTo(w*.21f,h*.24f); close()
                }
                drawPath(shield,color,style=line)
                drawPath(Path().apply { moveTo(w*.37f,h*.51f); lineTo(w*.47f,h*.61f); lineTo(w*.65f,h*.41f) },color,style=line)
            }
            ReferenceNavIconKind.DATA -> {
                drawOval(color,Offset(w*.18f,h*.16f),Size(w*.64f,h*.22f),style=Stroke(stroke))
                drawLine(color,Offset(w*.18f,h*.27f),Offset(w*.18f,h*.72f),stroke,StrokeCap.Round)
                drawLine(color,Offset(w*.82f,h*.27f),Offset(w*.82f,h*.72f),stroke,StrokeCap.Round)
                drawArc(color,-180f,180f,false,Offset(w*.18f,h*.59f),Size(w*.64f,h*.22f),style=Stroke(stroke))
                drawArc(color,0f,180f,false,Offset(w*.18f,h*.61f),Size(w*.64f,h*.22f),style=Stroke(stroke))
            }
            ReferenceNavIconKind.SOCIAL -> {
                drawCircle(color,w*.095f,Offset(w*.25f,h*.35f),style=Stroke(stroke))
                drawCircle(color,w*.095f,Offset(w*.72f,h*.22f),style=Stroke(stroke))
                drawCircle(color,w*.095f,Offset(w*.70f,h*.72f),style=Stroke(stroke))
                drawLine(color,Offset(w*.34f,h*.33f),Offset(w*.63f,h*.25f),stroke*.8f,StrokeCap.Round)
                drawLine(color,Offset(w*.33f,h*.40f),Offset(w*.62f,h*.66f),stroke*.8f,StrokeCap.Round)
            }
            ReferenceNavIconKind.ABOUT -> {
                drawCircle(color,w*.36f,Offset(w*.5f,h*.5f),style=Stroke(stroke))
                drawCircle(color,w*.045f,Offset(w*.5f,h*.32f))
                drawLine(color,Offset(w*.5f,h*.47f),Offset(w*.5f,h*.69f),stroke,StrokeCap.Round)
            }
        }
    }
}
