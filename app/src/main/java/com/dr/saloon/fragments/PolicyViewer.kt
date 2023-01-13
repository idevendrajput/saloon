package com.dr.saloon.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentPolicyViewerBinding
import com.dr.saloon.utils.AppConstaints.CONTENT
import com.dr.saloon.utils.AppConstaints.DEFAULT_ENCODING
import com.dr.saloon.utils.AppConstaints.STYLE_FOR_WEB_VIEW
import com.dr.saloon.utils.AppConstaints.THE_TITLE
import com.dr.saloon.utils.AppConstaints.WEB_VIEW_MEME_TYPE_FOR_HTML

class PolicyViewer : Fragment() {

    lateinit var binding: FragmentPolicyViewerBinding
    lateinit var mContext: Context
    lateinit var title : String
    lateinit var content : String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPolicyViewerBinding.inflate(layoutInflater)
        title = arguments?.getString(THE_TITLE).toString()
        content = arguments?.getString(CONTENT).toString()
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.title.text = title

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.webView.loadDataWithBaseURL(
            null,  STYLE_FOR_WEB_VIEW + content,
            WEB_VIEW_MEME_TYPE_FOR_HTML,  DEFAULT_ENCODING, null
        )

    }
}