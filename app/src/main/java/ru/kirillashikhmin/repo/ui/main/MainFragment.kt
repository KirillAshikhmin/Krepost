package ru.kirillashikhmin.repo.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.kirillashikhmin.repo.R
import ru.kirillashikhmin.repo.databinding.FragmentMainBinding

class MainFragment : Fragment(R.layout.fragment_main) {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.resultLiveData.observe(viewLifecycleOwner) {
            binding.resultTextView.text = it
        }
        viewModel.statusLiveData.observe(viewLifecycleOwner) {
            binding.statusTextView.text = it
        }
        binding.loadingButton.setOnClickListener { viewModel.fetchData() }
        binding.loadingErrorButton.setOnClickListener { viewModel.fetchWithError() }
        binding.invokeActionButton.setOnClickListener { viewModel.invokeAction() }
        binding.noFlowButton.setOnClickListener { viewModel.fetchWithoutFlow() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
