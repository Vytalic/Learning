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
            quizViewModel.moveToNext()
            updateQuestion()
        }

        binding.backButton.setOnClickListener {
            quizViewModel.moveToPrevious()
            updateQuestion()
        }

        binding.questionTextView.setOnClickListener {
            quizViewModel.moveToNext()
            val nextQuestionTextResId = quizViewModel.getCurrentQuestionTextResId()
            val nextQuestionText = "Next: " + getString(nextQuestionTextResId)
            Snackbar.make(binding.root, nextQuestionText, Snackbar.LENGTH_SHORT).show()
            quizViewModel.moveToPrevious()
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
        val questionTextResId = quizViewModel.getCurrentQuestionTextResId()
        binding.questionTextView.setText(questionTextResId)
        resetButtons()

    }

    private fun resetButtons() {
        // Reset buttons depending on answered status
        if (quizViewModel.currentQuestionAnswered) {
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
        val correctAnswer = quizViewModel.currentQuestionAnswer


        val messageResId = if (userAnswer == correctAnswer && quizViewModel.currentQuestionAnswered == false) {
            quizViewModel.setCurrentAnswer(true)
            R.string.correct_toast
        } else if (userAnswer != correctAnswer && quizViewModel.currentQuestionAnswered == false){
            quizViewModel.setCurrentAnswer(false)
            R.string.incorrect_toast
        } else {
            R.string.answered
        }
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()

        // Mark question as answered
        quizViewModel.setAsAnswered(true)
        resetButtons()

        checkGrade()
    }

    private fun checkGrade() {
        val gradePercentage = quizViewModel.checkGrade()

        if (gradePercentage != null)
        {
            val messageResId = "Grade: $gradePercentage%"
            Toast.makeText(this,messageResId, Toast.LENGTH_LONG).show()
        }

    }

}