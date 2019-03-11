package com.gdavidpb.tuindice.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.gdavidpb.tuindice.R
import com.gdavidpb.tuindice.domain.model.Account
import com.gdavidpb.tuindice.domain.usecase.coroutines.Continuous
import com.gdavidpb.tuindice.presentation.viewmodel.MainViewModel
import com.gdavidpb.tuindice.ui.adapters.SummaryAdapter
import com.gdavidpb.tuindice.utils.observe
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_summary.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

open class SummaryFragment : Fragment() {

    private val viewModel: MainViewModel by sharedViewModel()

    private val picasso: Picasso by inject()

    private val adapter = SummaryAdapter(callback = SummaryManager())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rViewSummary.layoutManager = LinearLayoutManager(context)
        rViewSummary.adapter = adapter

        with(viewModel) {
            observe(account, ::accountObserver)

            loadAccount(trySync = false)

            sRefreshSummary.setOnRefreshListener { loadAccount(trySync = true) }
        }
    }

    private fun accountObserver(result: Continuous<Account>?) {
        when (result) {
            is Continuous.OnStart -> {
                sRefreshSummary.isRefreshing = true
            }
            is Continuous.OnNext -> {
                adapter.setAccount(account = result.value)
            }
            is Continuous.OnComplete -> {
                sRefreshSummary.isRefreshing = false
            }
            is Continuous.OnError -> {
                sRefreshSummary.isRefreshing = false

                // todo failure show image on recycler
            }
        }
    }

    inner class SummaryManager : SummaryAdapter.AdapterCallback {
        override fun provideImageLoader(provider: (picasso: Picasso) -> Unit) = provider(picasso)
    }
}