package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.MetronomeScreen
import com.example.ui.theme.MetronomeTheme
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
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MetronomeTheme {
        MetronomeScreen(
          bpm = 120,
          beatsPerMeasure = 4,
          beatValue = 4,
          isPlaying = false,
          currentBeatIndex = 0,
          isCurrentAccent = false,
          lastBeatTriggerTime = 0L,
          onBpmChange = {},
          onBeatsPerMeasureChange = {},
          onBeatValueChange = {},
          onTogglePlay = {},
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
