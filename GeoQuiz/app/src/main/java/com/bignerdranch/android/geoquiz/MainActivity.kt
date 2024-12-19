package com.bignerdranch.android.geoquiz

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.bignerdranch.android.geoquiz.R.color.purple_500
import com.bignerdranch.android.geoquiz.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    private val quizViewModel: QuizViewModel by viewModels()

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
        Question(R.string.question_asia, answer = true, answered = false, correct = false))

    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate(Bundle?) called")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "Got a QuizViewModel: $quizViewModel")


        binding.trueButton.setOnClickListener { view: View ->
            checkAnswer(true)
        }


        binding.falseButton.setOnClickListener { view: View ->
            checkAnswer(false)
        }

        binding.nextButton.setOnClickListener {
            currentIndex = (currentIndex + 1) % questionBank.size
            updateQuestion()
        }

        binding.backButton.setOnClickListener {
            currentIndex = if (currentIndex - 1 < 0) {
                questionBank.size - 1
            } else {
                (currentIndex - 1) % questionBank.size
            }
            updateQuestion()
        }

        binding.questionTextView.setOnClickListener {
            // Get current question\
            val questionText = "Next: " + getString(questionBank[(currentIndex + 1) % questionBank.size].textResId)
            Snackbar.make(binding.root, questionText, Snackbar.LENGTH_SHORT).show()
        }

        updateQuestion()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")
    }

    private fun updateQuestion() {
        val questionTextResId = questionBank[currentIndex].textResId
        binding.questionTextView.setText(questionTextResId)
        resetButtons()

    }

    private fun resetButtons() {
        // Reset buttons depending on answered status
        if (questionBank[currentIndex].answered == true) {
            binding.trueButton.isEnabled = true
            binding.falseButton.isEnabled = true

            binding.trueButton.setBackgroundColor(255)
            binding.falseButton.setBackgroundColor(255)
        } else {
            binding.trueButton.isEnabled = true
            binding.falseButton.isEnabled = true

            binding.trueButton.setBackgroundColor(resources.getColor(purple_500, theme))
            binding.falseButton.setBackgroundColor(resources.getColor(purple_500, theme))
        }
    }

    private fun checkAnswer(userAnswer: Boolean) {
        val correctAnswer = questionBank[currentIndex].answer


        val messageResId = if (userAnswer == correctAnswer && questionBank[currentIndex].answered == false) {
            questionBank[currentIndex].correct = true
            R.string.correct_toast
        } else if (userAnswer != correctAnswer && questionBank[currentIndex].answered == false){
            questionBank[currentIndex].correct = false
            R.string.incorrect_toast
        } else {
            R.string.answered
        }
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()

        // Mark question as answered
        questionBank[currentIndex].answered = true
        resetButtons()

        checkGrade()
    }

    private fun checkGrade() {
        val allAnswered = questionBank.all { it.answered }

        if (allAnswered == true) {
            val totalQuestions = questionBank.size
            val correctAnswers = questionBank.count { it.correct }
            val gradePercentage = (correctAnswers.toDouble() / totalQuestions) * 100

            val messageResId = "Grade: $gradePercentage%"

            Toast.makeText(this,messageResId, Toast.LENGTH_LONG).show()

            // Reset the quiz
            questionBank.forEach { it.answered = false }
        }

    }

}