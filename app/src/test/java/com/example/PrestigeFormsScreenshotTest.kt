package com.example

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.Text
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import com.example.ui.components.PremiumAlertDialog
import com.example.ui.screens.GroupFormDialog
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.StudentFormDialog
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PrestigeFormsScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun premiumDialogSmoke() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MyApplicationTheme(darkTheme = false) {
                    PremiumAlertDialog(
                        onDismissRequest = {},
                        title = { Text("نافذة عربية") },
                        text = { Text("محتوى الاختبار") },
                        confirmButton = { Text("حفظ") }
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("نافذة عربية").assertIsDisplayed()
    }

    @Test
    fun groupFormArabicLight() {
        renderGroupForm(darkTheme = false)
        composeTestRule.onNodeWithText("إضافة مجموعة جديدة").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/group_form_ar_light.png"
        )
    }

    @Test
    fun groupFormArabicDark() {
        renderGroupForm(darkTheme = true)
        composeTestRule.onNodeWithText("إضافة مجموعة جديدة").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/group_form_ar_dark.png"
        )
    }

    @Test
    fun studentFormArabicLight() {
        renderStudentForm(darkTheme = false)
        composeTestRule.onNodeWithText("إضافة طالب جديد", substring = true).assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/student_form_ar_light.png"
        )
    }

    @Test
    fun studentFormArabicDark() {
        renderStudentForm(darkTheme = true)
        composeTestRule.onNodeWithText("إضافة طالب جديد", substring = true).assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/student_form_ar_dark.png"
        )
    }

    @Test
    fun groupsEmptyArabicLight() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(application).apply { setAppLanguage("ar") }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MyApplicationTheme(darkTheme = false) {
                    GroupsScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("إدارة المجموعات").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/groups_empty_ar_light.png"
        )
    }

    private fun renderGroupForm(darkTheme: Boolean) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MyApplicationTheme(darkTheme = darkTheme) {
                    GroupFormDialog(
                        group = null,
                        appLanguage = "ar",
                        onDismiss = {},
                        onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun renderStudentForm(darkTheme: Boolean) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MyApplicationTheme(darkTheme = darkTheme) {
                    StudentFormDialog(
                        student = null,
                        groups = emptyList(),
                        onDismiss = {},
                        onSave = { _, _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }
}
