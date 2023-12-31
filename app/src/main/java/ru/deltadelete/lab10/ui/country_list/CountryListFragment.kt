package ru.deltadelete.lab10.ui.country_list

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.deltadelete.lab10.R
import ru.deltadelete.lab10.adapters.CountryAdapter
import ru.deltadelete.lab10.database.entities.Country
import ru.deltadelete.lab10.databinding.FragmentCountryListBinding
import ru.deltadelete.lab10.ui.town_list.TownListFragment
import ru.deltadelete.lab10.utils.dp
import ru.deltadelete.lab10.utils.value

class CountryListFragment : Fragment() {

    companion object {
        fun newInstance() = CountryListFragment()
    }

    private var binding: FragmentCountryListBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCountryListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.viewModel?.onAddCountryClick = null
        binding?.viewModel?.adapter?.value = null
        binding?.viewModel = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.viewModel = ViewModelProvider(requireActivity())[CountryListViewModel::class.java]
        binding?.viewModel?.onAddCountryClick = this::addCountryClick
        binding?.viewModel?.adapter?.value =
            CountryAdapter(view.context, emptyList<Country>().toMutableList()).apply {
                itemCallbacks.onItemClick = { item, view ->
                    itemClick(item, view)
                }
                itemCallbacks.onLongItemClick = { item, view ->
                    longItemClick(item, view)
                }
            }
    }

    public fun addCountryClick(view: View) {
        val context = requireContext()

        val name: MutableLiveData<String> = MutableLiveData("")
        val code: MutableLiveData<String> = MutableLiveData("")
        val isValid: MutableLiveData<Boolean> = MutableLiveData(false)

        val nameEditText = TextInputEditText(context).apply {
            addTextChangedListener { text: Editable? ->
                name.value = text.toString()
                error = if (name.value.isNullOrBlank()) {
                    "Название не может быть пустым"
                } else {
                    null
                }
            }
        }
        val nameInputLayout = TextInputLayout(context).apply {
            hint = "Название"
            addView(nameEditText)
        }

        val codeEditText = TextInputEditText(context).apply {
            filters = arrayOf<InputFilter>(
                InputFilter.LengthFilter(2)
            )
            addTextChangedListener { text: Editable? ->
                code.value = text.toString()
                error = if (code.value.isNullOrBlank()) {
                    "Код страны не может быть пустым"
                } else {
                    null
                }
            }
        }
        val codeInputLayout = TextInputLayout(context).apply {
            hint = "Код страны"
            addView(codeEditText)
        }

        MaterialAlertDialogBuilder(context).apply {
            setView(LinearLayout(this.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16.dp)
                addView(nameInputLayout)
                addView(codeInputLayout)
            })
            setTitle("Добавить страну")
            setPositiveButton("Добавить") { _, _ ->
                if (nameInputLayout.error != null || codeInputLayout.error != null) {
                    return@setPositiveButton
                }
                val item = Country(name = name.value!!, code = code.value!!)
                binding?.viewModel?.adapter?.value?.add(item)
                lifecycleScope.launch(Dispatchers.IO) {
                    binding?.viewModel?.database?.countryDao()?.insertAll(item)
                }
            }
            setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun itemClick(item: Country, view: View) {
        Snackbar.make(view, item.toString(), Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.fab)
            .setAction("Action", null).show()
        val args = Bundle().apply {
            this.putInt(TownListFragment.Companion.COUNTRY_ID_ARGUMENT, item.id)
        }
        NavHostFragment.findNavController(this)
            .navigate(R.id.action_CountryListFragment_to_townListFragment, args)
    }

    private fun longItemClick(item: Country, view: View): Boolean {
        val context = requireContext()

        MaterialAlertDialogBuilder(context).apply {
            setTitle(R.string.remove_country)
            setMessage(getString(R.string.remove_country_message, item.name))
            setPositiveButton(R.string.remove) { _, _ ->
                binding?.viewModel?.adapter?.value?.remove(item)
                lifecycleScope.launch(Dispatchers.IO) {
                    binding?.viewModel?.database?.countryDao()?.delete(item)
                }

                Snackbar.make(
                    view,
                    getString(R.string.removed_country, item.name), Snackbar.LENGTH_LONG
                )
                    .setAnchorView(R.id.fab)
                    .setAction("Action", null).show()
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
        return true;
    }
}