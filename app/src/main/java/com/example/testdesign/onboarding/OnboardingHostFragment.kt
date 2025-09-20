package com.example.testdesign.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.testdesign.databinding.FragmentOnboardingHostBinding


/**
 * Tiny host so ViewPager2 can use FragmentStateAdapter safely.
 */
class OnboardingHostFragment : Fragment() {

    private var _binding: FragmentOnboardingHostBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingHostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
