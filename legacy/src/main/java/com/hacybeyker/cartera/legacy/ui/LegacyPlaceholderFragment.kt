package com.hacybeyker.cartera.legacy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hacybeyker.cartera.legacy.databinding.LegacyFragmentPlaceholderBinding

internal class LegacyPlaceholderFragment : Fragment() {

    private var binding: LegacyFragmentPlaceholderBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        LegacyFragmentPlaceholderBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
