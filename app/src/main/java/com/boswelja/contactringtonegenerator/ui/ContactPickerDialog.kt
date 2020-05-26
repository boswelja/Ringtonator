package com.boswelja.contactringtonegenerator.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.boswelja.contactringtonegenerator.R
import com.boswelja.contactringtonegenerator.contacts.Contact
import com.boswelja.contactringtonegenerator.contacts.ContactManager
import com.boswelja.contactringtonegenerator.databinding.ContactPickerDialogBinding

class ContactPickerDialog : DialogFragment() {

    private var dialog: AlertDialog? = null
    private var useNicknames: Boolean = true

    private var binding: ContactPickerDialogBinding? = null

    val dialogEventListeners = ArrayList<DialogEventListener>()

    var wantsContactUpdate = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (dialog == null) {
            createDialog()
        }
        return dialog!!
    }

    override fun onResume() {
        super.onResume()

        setLoading(true)
        updateContacts()
        setLoading(false)
    }

    private fun createDialog() {
        binding = DataBindingUtil.inflate(LayoutInflater.from(context),
            R.layout.contact_picker_dialog, null, false)

        binding!!.contactsRecyclerview.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = ContactPickerAdapter().apply {
                setUseNicknames(useNicknames)
            }
        }
        dialog = AlertDialog.Builder(context!!).apply {
            setTitle(R.string.contact_picker_dialog_title)
            setPositiveButton(R.string.dialog_button_done) { _, _ ->
                for (listener in dialogEventListeners) {
                    listener.onContactsSelected(
                        (binding!!.contactsRecyclerview.adapter as ContactPickerAdapter).selectedContacts)
                }
            }
            setView(binding!!.root)
        }.create()
    }

    private fun setLoading(isLoading: Boolean) {
        binding!!.apply {
            if (isLoading) {
                loadingIndicator.visibility = View.VISIBLE
                contactsRecyclerview.visibility = View.GONE
                noContactsView.visibility = View.GONE
            } else {
                loadingIndicator.visibility = View.GONE
                if ((contactsRecyclerview.adapter as ContactPickerAdapter).itemCount > 0) {
                    noContactsView.visibility = View.GONE
                    contactsRecyclerview.visibility = View.VISIBLE
                } else {
                    noContactsView.visibility = View.VISIBLE
                    contactsRecyclerview.visibility = View.GONE
                }
            }
        }

    }

    private fun updateContacts() {
        if (wantsContactUpdate) {
            wantsContactUpdate = false
            (binding!!.contactsRecyclerview.adapter as ContactPickerAdapter)
                .setContacts(ContactManager.getContacts(context!!))
        }
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, "ContactPickerDialog")
    }

    fun getSelectedContacts(): List<Contact> {
        return (binding?.contactsRecyclerview?.adapter as ContactPickerAdapter?)?.selectedContacts
            ?: ArrayList()
    }

    fun setUseNicknames(useNicknames: Boolean) {
        this.useNicknames = useNicknames
        (binding?.contactsRecyclerview?.adapter as ContactPickerAdapter?)?.setUseNicknames(useNicknames)
    }

    interface DialogEventListener {
        fun onContactsSelected(selectedContacts: List<Contact>)
    }
}