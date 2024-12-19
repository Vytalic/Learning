package com.bignerdranch.android.geoquiz

//class Question {
//}
import androidx.annotation.StringRes

data class Question(@StringRes val textResId: Int,
                    val answer: Boolean,
                    var answered: Boolean,
                    var correct: Boolean)