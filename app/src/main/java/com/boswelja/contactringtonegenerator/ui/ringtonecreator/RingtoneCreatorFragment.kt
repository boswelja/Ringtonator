package com.boswelja.contactringtonegenerator.ui.ringtonecreator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.boswelja.contactringtonegenerator.databinding.FragmentRingtoneCreatorBinding
import com.boswelja.contactringtonegenerator.databinding.RingtoneCreatorAvailableItemBinding
import com.boswelja.contactringtonegenerator.ui.MainActivity
import com.boswelja.contactringtonegenerator.ui.common.BaseDataFragment
import com.boswelja.contactringtonegenerator.ringtonegen.item.BaseItem
import com.boswelja.contactringtonegenerator.ringtonegen.item.ContactName
import com.boswelja.contactringtonegenerator.ringtonegen.item.ID
import com.boswelja.contactringtonegenerator.ringtonegen.item.TextItem
import com.boswelja.contactringtonegenerator.ringtonegen.item.Utils.ALL_ITEMS
import com.google.android.material.chip.Chip

class RingtoneCreatorFragment : BaseDataFragment<ArrayList<BaseItem>>(), RingtoneCreatorAdapter.DataEventListener {

    private val adapter = RingtoneCreatorAdapter(this)

    private val onAvailableItemClickListener = View.OnClickListener {
        if (it is Chip) {
            val item = when (ALL_ITEMS.first { item -> item.getLabel() == it.text }.id) {
                ID.CONTACT_NAME -> ContactName()
                ID.TEXT_ITEM -> TextItem()
            }
            adapter.addItem(item)
        }
    }

    private var isDataEmpty: Boolean = true
    private var isDataValid: Boolean = true

    private lateinit var binding: FragmentRingtoneCreatorBinding

    override fun onSaveData(activity: MainActivity, data: ArrayList<BaseItem>) {
        activity.ringtoneItems.apply {
            clear()
            addAll(data)
        }
    }

    override fun requestData(): ArrayList<BaseItem>? = adapter.getItems()

    override fun onItemAdded() {
        isDataEmpty = false
        updateNoDataViewVisibility()
        binding.messageBuilderView.smoothScrollToPosition(adapter.itemCount - 1)
    }

    override fun onItemRemoved(isEmpty: Boolean) {
        isDataEmpty = isEmpty
        updateNoDataViewVisibility()
    }

    override fun onDataValidityChanged(isDataValid: Boolean) {
        this.isDataValid = isDataValid
        updateNextButtonEnabled()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRingtoneCreatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupAvailableMessageItems()
        setupMessageCreatorView()
        updateNextButtonEnabled()
        updateNoDataViewVisibility()
        binding.nextButton.setOnClickListener {
            findNavController().navigate(RingtoneCreatorFragmentDirections.toGeneratingFlow())
        }
    }

    private fun setupMessageCreatorView() {
        binding.messageBuilderView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = this@RingtoneCreatorFragment.adapter
            AdapterGestureHelper.attachToRecyclerView(this)
        }
    }

    private fun setupAvailableMessageItems() {
        ALL_ITEMS.forEach {
            val chipBinding = RingtoneCreatorAvailableItemBinding.inflate(layoutInflater)
            chipBinding.root.apply {
                text = it.getLabel()
                setOnClickListener(onAvailableItemClickListener)
            }
            binding.availableItems.addView(chipBinding.root)
        }
    }

    private fun updateNoDataViewVisibility() {
        val shouldShow = isDataEmpty
        binding.apply {
            if (shouldShow) {
                messageNoContentView.visibility = View.VISIBLE
            } else {
                messageNoContentView.visibility = View.GONE
            }
        }
    }

    private fun updateNextButtonEnabled() {
        val shouldEnable = !isDataEmpty && isDataValid
        binding.nextButton.isEnabled = shouldEnable
    }
}
