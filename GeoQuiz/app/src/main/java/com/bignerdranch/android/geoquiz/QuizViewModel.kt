package com.bignerdranch.android.geoquiz

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

private const val TAG = "QuizViewModel"
const val CURRENT_INDEX_KEY = "CURRENT_INDEX_KEY"

class QuizViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
//    init {
//        Log.d(TAG, "ViewModel instance created")
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        Log.d(TAG, "ViewModel instance about to be destroyed")
//    }

    private val questionBank = listOf(
        Question(
            textResId = R.string.question_australia,
            answer = true,
            answered = false,
            correct = false
        ),
        Question(
            textResId = R.string.question_oceans,
            answer = true,
            answered = false,
            correct = false
        ),
        Question(
            textResId = R.string.question_mideast,
            answer = false,
            answered = false,
            correct = false
        ),
        Question(
            textResId = R.string.question_africa,
            answer = false,
            answered = false,
            correct = false
        ),
        Question(
            textResId = R.string.question_americas,
            answer = true,
            answered = false,
            correct = false
        ),
        Question(R.string.question_asia,
            answer = true,
            answered = false,
            correct = false)
    )

    private var currentIndex: Int
        get() = savedStateHandle[CURRENT_INDEX_KEY] ?: 0
        set(value) = savedStateHandle.set(CURRENT_INDEX_KEY, value)

    val currentQuestionAnswer: Boolean
        get() = questionBank[currentIndex].answer

    val currentQuestionText: Int
        get() = questionBank[currentIndex].textResId

    val currentQuestionAnswered: Boolean
        get() = questionBank[currentIndex].answered

    fun moveToNext() {
        currentIndex = (currentIndex + 1) % questionBank.size
    }

    fun moveToPrevious() {
        currentIndex = if (currentIndex - 1 < 0) {
            questionBank.size - 1
        } else {
            (currentIndex - 1) % questionBank.size
        }
    }

    fun getCurrentQuestionTextResId(): Int {
        return questionBank[currentIndex].textResId
    }

    fun setCurrentAnswer(isCorrect: Boolean) {
        questionBank[currentIndex].correct = isCorrect
    }

    fun setAsAnswered(isAnswered: Boolean) {
        questionBank[currentIndex].answered = isAnswered
    }

    fun checkGrade(): Double? {
        val allAnswered = questionBank.all { it.answered }

        return if (allAnswered) {
            val totalQuestions = questionBank.size
            val correctAnswers = questionBank.count { it.correct }
            val gradePercentage = (correctAnswers.toDouble() / totalQuestions) * 100

            // Reset the quiz
            questionBank.forEach { it.answered = false }

            gradePercentage
        } else {
            null
        }
    }
}