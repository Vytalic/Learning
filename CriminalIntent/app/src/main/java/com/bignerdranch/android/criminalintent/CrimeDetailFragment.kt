package com.bignerdranch.android.criminalintent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeDetailBinding
import java.util.Date
import java.util.UUID

class CrimeDetailFragment : Fragment() {

    private lateinit var binding: FragmentCrimeDetailBinding

    private lateinit var crime: Crime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        crime = Crime(
            id = UUID.randomUUID(),
            title = "",
            date = Date(),
            isSolved = false
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCrimeDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
        // return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add Listener, invoked when text is changed
        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                crime = crime.copy(title = text.toString())
            }

            // Display date of crime
            crimeDate.apply {
                text = crime.date.toString()
                isEnabled = false
            }

            // Add listener to checkbox
            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                crime = crime.copy(isSolved = isChecked)
            }
        }
    }
}