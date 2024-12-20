package com.bignerdranch.android.geoquiz

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import com.bignerdranch.android.geoquiz.R.color.purple_500
import com.bignerdranch.android.geoquiz.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    private val quizViewModel: QuizViewModel by viewModels()

    private val cheatLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        // Handle the result
        if (result.resultCode == RESULT_OK) {
            quizViewModel.isCheater = result.data?.getBooleanExtra(EXTRA_ANSWER_SHOWN, false) ?: false
        }
    }



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

        binding.cheatButton.setOnClickListener {
            // Start CheatActivity
            //val intent = Intent(this, CheatActivity::class.java)
            val answerIsTrue = quizViewModel.currentQuestionAnswer
            val intent = CheatActivity.newIntent(this@MainActivity, answerIsTrue)
            //startActivity(intent)
            cheatLauncher.launch(intent)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurCheatButton()
        }
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

        val messageResId = when {
            quizViewModel.isCheater -> R.string.judgment_toast
            userAnswer == correctAnswer && !quizViewModel.currentQuestionAnswered -> {
                quizViewModel.setCurrentAnswer(true)
                R.string.correct_toast
            }
            userAnswer != correctAnswer && !quizViewModel.currentQuestionAnswered -> {
                quizViewModel.setCurrentAnswer(false)
                R.string.incorrect_toast
            }
            else -> R.string.answered



        }

//        val messageResId = if (userAnswer == correctAnswer && quizViewModel.currentQuestionAnswered == false) {
//            quizViewModel.setCurrentAnswer(true)
//            R.string.correct_toast
//        } else if (userAnswer != correctAnswer && quizViewModel.currentQuestionAnswered == false){
//            quizViewModel.setCurrentAnswer(false)
//            R.string.incorrect_toast
//        } else {
//            R.string.answered
//        }
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

    @RequiresApi(Build.VERSION_CODES.S)
    private fun blurCheatButton() {
        val effect = RenderEffect.createBlurEffect(
            10.0f,
            10.0f,
            Shader.TileMode.CLAMP
        )
        binding.cheatButton.setRenderEffect(effect)
    }



}