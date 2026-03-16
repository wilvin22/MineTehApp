package com.example.mineteh.utils

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible

object ViewUtils {
    
    /**
     * Show loading state with optional message
     */
    fun showLoading(
        loadingView: View,
        contentView: View? = null,
        errorView: View? = null,
        message: String? = null
    ) {
        loadingView.isVisible = true
        contentView?.isVisible = false
        errorView?.isVisible = false
        
        message?.let { msg ->
            loadingView.findViewById<TextView>(R.id.loadingText)?.text = msg
        }
    }
    
    /**
     * Show content state
     */
    fun showContent(
        loadingView: View,
        contentView: View,
        errorView: View? = null
    ) {
        loadingView.isVisible = false
        contentView.isVisible = true
        errorView?.isVisible = false
    }
    
    /**
     * Show error state with optional message and retry action
     */
    fun showError(
        loadingView: View,
        contentView: View? = null,
        errorView: View,
        message: String? = null,
        onRetry: (() -> Unit)? = null
    ) {
        loadingView.isVisible = false
        contentView?.isVisible = false
        errorView.isVisible = true
        
        message?.let { msg ->
            errorView.findViewById<TextView>(R.id.errorMessage)?.text = msg
        }
        
        onRetry?.let { retry ->
            errorView.findViewById<View>(R.id.btnRetry)?.setOnClickListener { retry() }
        }
    }
    
    /**
     * Show empty state with optional message and action
     */
    fun showEmptyState(
        loadingView: View,
        contentView: View? = null,
        emptyView: View,
        title: String? = null,
        message: String? = null,
        actionText: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        loadingView.isVisible = false
        contentView?.isVisible = false
        emptyView.isVisible = true
        
        title?.let { 
            emptyView.findViewById<TextView>(R.id.emptyTitle)?.text = it
        }
        
        message?.let { 
            emptyView.findViewById<TextView>(R.id.emptyMessage)?.text = it
        }
        
        val actionButton = emptyView.findViewById<View>(R.id.btnAction)
        if (actionText != null && onAction != null) {
            actionButton?.isVisible = true
            (actionButton as? TextView)?.text = actionText
            actionButton?.setOnClickListener { onAction() }
        } else {
            actionButton?.isVisible = false
        }
    }
    
    /**
     * Enable/disable a group of views
     */
    fun setViewsEnabled(enabled: Boolean, vararg views: View) {
        views.forEach { it.isEnabled = enabled }
    }
    
    /**
     * Set visibility for a group of views
     */
    fun setViewsVisible(visible: Boolean, vararg views: View) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        views.forEach { it.visibility = visibility }
    }
}