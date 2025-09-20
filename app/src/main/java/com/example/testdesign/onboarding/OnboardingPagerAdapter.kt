package com.example.testdesign.onboarding

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingPagerAdapter(
    host: Fragment,
    private val pages: List<OnboardingPage>
) : FragmentStateAdapter(host) {

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment =
        OnboardingPageFragment.newInstance(pages[position])
}
