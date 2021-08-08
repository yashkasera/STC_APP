package com.mstc.mstcapp.ui.explore.event

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.mstc.mstcapp.ui.loadState.LoadStateAdapter
import com.mstc.mstcapp.databinding.FragmentRecyclerViewBinding
import com.mstc.mstcapp.injection.EventInjection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "EventFragment"

class EventFragment : Fragment() {

    companion object {
        fun newInstance() = EventFragment()
    }

    private lateinit var binding: FragmentRecyclerViewBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val viewModel = ViewModelProvider(
            this, EventInjection.provideViewModelFactory(
                context = requireContext(),
                owner = this
            )
        ).get(EventViewModel::class.java)

//        val decoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
//        binding.recyclerView.addItemDecoration(decoration)

        // bind the state
        binding.bindState(
            uiState = viewModel.state,
            uiActions = viewModel.accept
        )
    }

    private fun FragmentRecyclerViewBinding.bindState(
        uiState: StateFlow<UiState>,
        uiActions: (UiAction) -> Unit,
    ) {
        val eventAdapter = EventAdapter()
        val header = LoadStateAdapter { eventAdapter.retry() }

        recyclerView.adapter = eventAdapter.withLoadStateHeaderAndFooter(
            header = header,
            footer = LoadStateAdapter { eventAdapter.retry() }
        )

        bindRecylerView(
            header = header,
            eventAdapter = eventAdapter,
            uiState = uiState,
            onScrollChanged = uiActions
        )
    }


    private fun FragmentRecyclerViewBinding.bindRecylerView(
        header: LoadStateAdapter,
        eventAdapter: EventAdapter,
        uiState: StateFlow<UiState>,
        onScrollChanged: (UiAction.Scroll) -> Unit,
    ) {
        retryButton.setOnClickListener { eventAdapter.retry() }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0)
                    onScrollChanged(UiAction.Scroll(currentQuery = uiState.value.query))

            }
        })
        val notLoading = eventAdapter.loadStateFlow
            // Only emit when REFRESH LoadState for RemoteMediator changes.
            .distinctUntilChangedBy { it.refresh }
            // Only react to cases where Remote REFRESH completes i.e., NotLoading.
            .map { it.refresh is LoadState.NotLoading }

        val hasNotScrolledForCurrentSearch = uiState
            .map { it.hasNotScrolledForCurrentSearch }
            .distinctUntilChanged()

        val shouldScrollToTop = combine(
            notLoading,
            hasNotScrolledForCurrentSearch,
            Boolean::and
        )
            .distinctUntilChanged()

        val pagingData = uiState
            .map { it.pagingData }
            .distinctUntilChanged()

        lifecycleScope.launch {
            combine(shouldScrollToTop, pagingData, ::Pair)
                // Each unique PagingData should be submitted once, take the latest from
                // shouldScrollToTop
                .distinctUntilChangedBy { it.second }
                .collectLatest { (shouldScroll, pagingData) ->
                    eventAdapter.submitData(pagingData)
                    // Scroll only after the data has been submitted to the adapter,
                    // and is a fresh search
                    if (shouldScroll) recyclerView.scrollToPosition(0)
                }
        }

        lifecycleScope.launch {
            eventAdapter.loadStateFlow.collect { loadState ->
                // Show a retry header if there was an error refreshing, and items were previously
                // cached OR default to the default prepend state
                header.loadState = loadState.mediator
                    ?.refresh
                    ?.takeIf { it is LoadState.Error && eventAdapter.itemCount > 0 }
                    ?: loadState.prepend

                val isListEmpty =
                    loadState.refresh is LoadState.NotLoading && eventAdapter.itemCount == 0
                // show empty list
                emptyList.isVisible = isListEmpty
                // Only show the list if refresh succeeds, either from the the local db or the remote.
                recyclerView.isVisible =
                    loadState.source.refresh is LoadState.NotLoading || loadState.mediator?.refresh is LoadState.NotLoading
                // Show loading spinner during initial load or refresh.
                progressBar.isVisible = loadState.mediator?.refresh is LoadState.Loading
                // Show the retry state if initial load or refresh fails.
                retryButton.isVisible =
                    loadState.mediator?.refresh is LoadState.Error && eventAdapter.itemCount == 0
                // Toast on any error, regardless of whether it came from RemoteMediator or PagingSource
                val errorState = loadState.source.append as? LoadState.Error
                    ?: loadState.source.prepend as? LoadState.Error
                    ?: loadState.append as? LoadState.Error
                    ?: loadState.prepend as? LoadState.Error
//                errorState?.let {
//                    Toast.makeText(
//                        requireContext(),
//                        "\uD83D\uDE28 Wooops ${it.error}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
            }
        }
    }


}