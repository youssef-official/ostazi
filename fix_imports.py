import sys

with open("app/src/main/java/com/example/ui/screens/GroupsScreen.kt", "r") as f:
    content = f.read()

imports_to_add = """
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.*
"""

content = content.replace("import androidx.compose.ui.unit.sp", "import androidx.compose.ui.unit.sp" + imports_to_add)

content = content.replace("Icons.AutoMirrored.Filled.MenuBook", "Icons.Default.MenuBook")
content = content.replace("Icons.AutoMirrored.Filled.Send", "Icons.Default.Send")

with open("app/src/main/java/com/example/ui/screens/GroupsScreen.kt", "w") as f:
    f.write(content)

