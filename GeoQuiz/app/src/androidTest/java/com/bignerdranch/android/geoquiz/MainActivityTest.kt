package com.bignerdranch.android.geoquiz

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        scenario = launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

//    @Test
//    fun showsFirstQuestionOnLaunch() {
//        onView(withId(R.id.question_text_view))
//            .check(matches(withText(R.string.question_australia)))
//    }
//
//    @Test
//    fun showsSecondQuestionAfterNextPress() {
//        onView(withId(R.id.next_button)).perform(click())
//        onView(withId(R.id.question_text_view))
//            .check(matches(withText(R.string.question_oceans)))
//    }
//
//    @Test
//    fun handlesActivityRecreation() {
//        onView(withId(R.id.next_button)).perform(click())
//        scenario.recreate()
//        onView(withId(R.id.question_text_view))
//            .check(matches(withText(R.string.question_oceans)))
//    }

    @Test
    fun questionAnswerIsCorrectInViewModel() {
        val savedStateHandle = SavedStateHandle(mapOf(CURRENT_INDEX_KEY to 0))
        val quizViewModel = QuizViewModel(savedStateHandle)

        val expectedAnswers = listOf(true, true, false, false, true, true)

        expectedAnswers.forEachIndexed { index, expectedAnswer ->
            if (expectedAnswer) {
                assertTrue("Question $index failed", quizViewModel.currentQuestionAnswer)
            } else {
                assertFalse("Question $index failed", quizViewModel.currentQuestionAnswer)
            }
            quizViewModel.moveToNext()
        }
    }




}