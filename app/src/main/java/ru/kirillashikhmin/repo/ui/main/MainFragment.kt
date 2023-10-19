package ru.kirillashikhmin.repo.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.serialization.ExperimentalSerializationApi
import ru.kirillashikhmin.repo.R
import ru.kirillashikhmin.repo.databinding.FragmentMainBinding

@OptIn(ExperimentalSerializationApi::class)
class MainFragment : Fragment(R.layout.fragment_main) {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!


    @OptIn(ExperimentalSerializationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.cacheDir = requireActivity().cacheDir.absolutePath
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
        binding.fetchButton.setOnClickListener { viewModel.fetchData() }
        binding.sendButton.setOnClickListener { viewModel.send() }
        binding.fetchProductsButton.setOnClickListener { viewModel.fetchProducts() }
        binding.fetchSpecificProductButton.setOnClickListener {
            viewModel.fetchProduct(binding.idEditText.text?.toString())
        }
        binding.invalidateCheckbox.setOnCheckedChangeListener { _, checked ->
            viewModel.invalidateCache = checked
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
