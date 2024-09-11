package com.example.goodfoodapp

interface UnsavedChangesListener {
    fun hasUnsavedChanges(): Boolean
    fun showUnsavedChangesDialog(onDiscardChanges: () -> Unit)
}
