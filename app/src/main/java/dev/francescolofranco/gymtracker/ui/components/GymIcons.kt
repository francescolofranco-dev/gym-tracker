package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** App icons share the rounded outline treatment of the bottom navigation. */
object GymIcons {
    val Calendar by outline(
        "Calendar",
        "M5 4 H19 Q21 4 21 6 V19 Q21 21 19 21 H5 Q3 21 3 19 V6 Q3 4 5 4 Z " +
            "M16 2 V6 M8 2 V6 M3 10 H21",
        fillPath = "M7 14 H9 V16 H7 Z M11 14 H13 V16 H11 Z M15 14 H17 V16 H15 Z",
    )
    val Dumbbell by outline(
        "Dumbbell",
        "M6.5 12 H17.5 M6.5 6.5 V17.5 M17.5 6.5 V17.5 " +
            "M3.5 8.5 V15.5 M20.5 8.5 V15.5 M2 12 H3.5 M20.5 12 H22",
    )
    val BarChart by outline("BarChart", "M6 20 V14 M12 20 V10 M18 20 V4", strokeWidth = 2f)
    val Settings by outline(
        "Settings",
        "M10 3 H14 L14.7 5.8 L16 6.5 L18.8 5.8 L20.8 9.2 L18.8 11.3 V12.7 " +
            "L20.8 14.8 L18.8 18.2 L16 17.5 L14.7 18.2 L14 21 H10 L9.3 18.2 L8 17.5 " +
            "L5.2 18.2 L3.2 14.8 L5.2 12.7 V11.3 L3.2 9.2 L5.2 5.8 L8 6.5 L9.3 5.8 Z " +
            "M15 12 A3 3 0 1 1 9 12 A3 3 0 1 1 15 12 Z",
    )

    val ArrowBack by outline("ArrowBack", "M20 12 H4 M10 6 L4 12 L10 18", autoMirror = true)
    val ArrowForward by outline("ArrowForward", "M4 12 H20 M14 6 L20 12 L14 18", autoMirror = true)
    val ArrowUp by outline("ArrowUp", "M12 20 V4 M6 10 L12 4 L18 10")
    val ArrowDown by outline("ArrowDown", "M12 4 V20 M6 14 L12 20 L18 14")
    val ChevronForward by outline("ChevronForward", "M9 5 L16 12 L9 19", autoMirror = true)
    val ChevronUp by outline("ChevronUp", "M6 15 L12 9 L18 15")
    val ChevronDown by outline("ChevronDown", "M6 9 L12 15 L18 9")
    val TrendUp by outline("TrendUp", "M3 17 L9 11 L13 15 L21 7 M15 7 H21 V13", autoMirror = true)
    val TrendDown by outline("TrendDown", "M3 7 L9 13 L13 9 L21 17 M15 17 H21 V11", autoMirror = true)
    val SwapVertical by outline("SwapVertical", "M8 20 V4 M4 8 L8 4 L12 8 M16 4 V20 M12 16 L16 20 L20 16")

    val Add by outline("Add", "M12 4 V20 M4 12 H20")
    val Remove by outline("Remove", "M4 12 H20")
    val Close by outline("Close", "M6 6 L18 18 M18 6 L6 18")
    val Check by outline("Check", "M4.5 12 L9.5 17 L19.5 7")
    val Search by outline("Search", "M16 10 A6 6 0 1 1 4 10 A6 6 0 1 1 16 10 Z M14.25 14.25 L20 20")
    val Backspace by outline(
        "Backspace",
        "M9 5 H20 Q21 5 21 6 V18 Q21 19 20 19 H9 L2 12 Z M11 9 L17 15 M17 9 L11 15",
        autoMirror = true,
    )
    val MoreVertical by outline(
        "MoreVertical",
        "M12 4.5 V5.5 M12 11.5 V12.5 M12 18.5 V19.5",
    )
    val Edit by outline(
        "Edit",
        "M12 5 H5 Q3 5 3 7 V19 Q3 21 5 21 H17 Q19 21 19 19 V13 " +
            "M9.5 13 L18 4.5 Q18.75 3.75 19.5 4.5 L21 6 Q21.75 6.75 21 7.5 " +
            "L12.5 16 L8.5 17 Z",
    )
    val EditCalendar by outline(
        "EditCalendar",
        "M10 21 H5 Q3 21 3 19 V6 Q3 4 5 4 H19 Q21 4 21 6 V10 M8 2 V6 M16 2 V6 " +
            "M3 9 H21 M13 18 L19 12 L22 15 L16 21 L12 22 Z",
    )
    val EditNote by outline(
        "EditNote",
        "M3 5 H19 M3 10 H15 M3 15 H9 M12 17.5 L18.5 11 L22 14.5 L15.5 21 L11 22 Z",
        autoMirror = true,
    )
    val Notes by outline("Notes", "M4 6 H20 M4 12 H20 M4 18 H13", autoMirror = true)
    val Checklist by outline(
        "Checklist",
        "M3 6 H16 M3 11 H16 M3 16 H10 M13 16 L16 19 L21 13",
        autoMirror = true,
    )
    val List by outline(
        "List",
        "M5 3 H19 Q21 3 21 5 V19 Q21 21 19 21 H5 Q3 21 3 19 V5 Q3 3 5 3 Z " +
            "M7 8 H7.5 M11 8 H17 M7 12 H7.5 M11 12 H17 M7 16 H7.5 M11 16 H17",
        autoMirror = true,
    )
    val Delete by outline(
        "Delete",
        "M4 6 H20 M9 6 V3 H15 V6 M6 6 L7 19 Q7.2 21 9 21 H15 Q16.8 21 17 19 L18 6 " +
            "M10 10 V17 M14 10 V17",
    )
    val DeleteForever by outline(
        "DeleteForever",
        "M4 6 H20 M9 6 V3 H15 V6 M6 6 L7 19 Q7.2 21 9 21 H15 Q16.8 21 17 19 L18 6 " +
            "M10 11 L14 16 M14 11 L10 16",
    )
    val Skip by outline("Skip", "M21 12 A9 9 0 1 1 3 12 A9 9 0 1 1 21 12 Z M5.65 5.65 L18.35 18.35")
    val Play by outline("Play", "M7 4 L20 12 L7 20 Z")
    val Bookmark by outline("Bookmark", "M6 3 H18 V21 L12 17 L6 21 Z")
    val Pin by outline(
        "Pin",
        "M8 3 H16 L15 6 V10 L18 13 V15 H6 V13 L9 10 V6 Z M12 15 V22",
    )
    val Link by outline(
        "Link",
        "M10 13 A5 5 0 0 0 17.54 13.54 L20.54 10.54 A5 5 0 0 0 13.46 3.46 L11.75 5.18 " +
            "M14 11 A5 5 0 0 0 6.46 10.46 L3.46 13.46 A5 5 0 0 0 10.54 20.54 L12.25 18.82",
    )
    val Sliders by outline(
        "Sliders",
        "M4 6 H8 M12 6 H20 M8 3.5 V8.5 M4 12 H12 M16 12 H20 M16 9.5 V14.5 " +
            "M4 18 H8 M12 18 H20 M8 15.5 V20.5",
    )
    val Share by outline(
        "Share",
        "M8.5 10.5 L15.5 6.5 M8.5 13.5 L15.5 17.5 " +
            "M9 12 A3 3 0 1 1 3 12 A3 3 0 1 1 9 12 Z " +
            "M21 5 A3 3 0 1 1 15 5 A3 3 0 1 1 21 5 Z " +
            "M21 19 A3 3 0 1 1 15 19 A3 3 0 1 1 21 19 Z",
    )

    val Bolt by outline("Bolt", "M13 2 L5 13 H11 L10 22 L19 11 H13 Z")
    val Trophy by outline(
        "Trophy",
        "M7 3 H17 V9 A5 5 0 0 1 7 9 Z M7 5 H3 V7 Q3 11 7 11 M17 5 H21 V7 Q21 11 17 11 " +
            "M12 14 V19 M8 21 H16 M9 21 L10 19 H14 L15 21",
    )
    val Info by outline(
        "Info",
        "M21 12 A9 9 0 1 1 3 12 A9 9 0 1 1 21 12 Z M12 11 V16 M12 7.5 V7.7",
    )
    val Warning by outline("Warning", "M12 3 L22 20 H2 Z M12 9 V13 M12 16.5 V16.7")
    val Eye by outline(
        "Eye",
        "M2 12 Q6 5 12 5 Q18 5 22 12 Q18 19 12 19 Q6 19 2 12 Z " +
            "M15 12 A3 3 0 1 1 9 12 A3 3 0 1 1 15 12 Z",
    )
    val Clock by outline(
        "Clock",
        "M21 12 A9 9 0 1 1 3 12 A9 9 0 1 1 21 12 Z M12 7 V12 L15 14",
    )
    val Timer by outline(
        "Timer",
        "M20 14 A8 8 0 1 1 4 14 A8 8 0 1 1 20 14 Z M12 2 V6 M9 2 H15 M18 8.5 L20.5 6 " +
            "M12 10 V14",
    )
    val Notifications by outline(
        "Notifications",
        "M18 9 V13 L20 17 H4 L6 13 V9 A6 6 0 0 1 18 9 Z M10 20 Q12 23 14 20",
    )
    val NotificationsOff by outline(
        "NotificationsOff",
        "M9 3.8 A6 6 0 0 1 18 9 V12.5 M6.3 7 A6 6 0 0 0 6 9 V13 L4 17 H17 " +
            "M10 20 Q12 23 14 20 M3 3 L21 21",
    )

    val Refresh by outline("Refresh", "M20 4 V9 H15 M20 9 A8.5 8.5 0 1 0 20 15")
    val Sync by outline(
        "Sync",
        "M20 4 V9 H15 M20 9 A8.5 8.5 0 0 0 4 9 M4 20 V15 H9 M4 15 A8.5 8.5 0 0 0 20 15",
    )
    val Restore by outline(
        "Restore",
        "M3 4 V9 H8 M3 9 A9 9 0 1 1 4 17 M12 7 V12 L15 14",
    )

    private const val CloudOutline =
        "M7 19 H18 C20.5 19 22 17.1 22 14.8 C22 12.4 20.2 10.5 17.8 10.5 H17 " +
            "C16.3 7.3 14 5.5 11 5.5 C7.9 5.5 5.6 7.5 5 10.5 C3 11.3 2 12.8 2 14.7 " +
            "C2 17.2 4 19 7 19 Z"

    val CloudUpload by outline("CloudUpload", "$CloudOutline M12 16 V10 M9.5 12.5 L12 10 L14.5 12.5")
    val CloudDownload by outline("CloudDownload", "$CloudOutline M12 10 V16 M9.5 13.5 L12 16 L14.5 13.5")
    val CloudDone by outline("CloudDone", "$CloudOutline M8.5 13.5 L11 16 L15.5 11.5")
    val CloudSync by outline(
        "CloudSync",
        "$CloudOutline M15.5 10.5 V12.5 H13.5 M15.5 12.5 Q13 8.5 9 11.5 " +
            "M8.5 16.5 V14.5 H10.5 M8.5 14.5 Q11 18.5 15 15.5",
    )
    val CloudOff by outline(
        "CloudOff",
        "M8 6.4 C10.8 4.6 15.8 5.5 17 10.5 H17.8 C20.2 10.5 22 12.4 22 14.8 " +
            "Q22 17 20.3 18.2 M5.5 9 L5 10.5 C3 11.3 2 12.8 2 14.7 C2 17.2 4 19 7 19 H15 " +
            "M3 3 L21 21",
    )

    private fun outline(
        name: String,
        path: String,
        autoMirror: Boolean = false,
        fillPath: String? = null,
        strokeWidth: Float = 1.8f,
    ): Lazy<ImageVector> = lazy {
        val ink = SolidColor(Color.White)
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror,
        ).addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            stroke = ink,
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        if (fillPath != null) {
            builder.addPath(pathData = PathParser().parsePathString(fillPath).toNodes(), fill = ink)
        }
        builder.build()
    }
}
