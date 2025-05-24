package com.example.explorandes.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.explorandes.adapters.VisitedSimpleAdapter
import com.example.explorandes.databinding.FragmentVisitedBinding
import com.example.explorandes.utils.VisitedEventsManager

class VisitedFragment : Fragment() {
    private var _binding: FragmentVisitedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVisitedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = VisitedSimpleAdapter()
        binding.visitedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.visitedRecyclerView.adapter = adapter

        adapter.submitList(VisitedEventsManager.getVisitedEvents())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
