package com.aya.acam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import timber.log.Timber

class GalleryFragment : Fragment() {

    companion object {
        fun newInstance() = GalleryFragment()
        val NUMBER_OF_COLUMS = 3
    }

    private var mRecyclerView: RecyclerView? = null
    private var mPhotoAdapter: GalleryAdapter? = null
    private var mGridLayoutManager: GridLayoutManager? = null
    var swipeRefreshLayout: SwipeRefreshLayout? = null

    private lateinit var viewModel: GalleryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(GalleryViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        swipeRefreshLayout =
            view?.findViewById<SwipeRefreshLayout?>(R.id.swipe_refresh_layout)?.apply {
                this.setOnRefreshListener { ->
                    //Todo:refresh date
                    viewModel.updatePhotos()
                }
            }

        mGridLayoutManager =
            GridLayoutManager(requireContext(), NUMBER_OF_COLUMS).also {
                it.spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (mPhotoAdapter!!.getItemViewType(position)) {
                            GalleryAdapter.TITLE_TYPE -> NUMBER_OF_COLUMS
                            GalleryAdapter.ITEM_TYPE -> 1
                            else -> 2
                        }
                    }
                }
            }

        mPhotoAdapter = GalleryAdapter(
            requireActivity(),
            mGridLayoutManager!!
        )

        mRecyclerView = requireView().findViewById<RecyclerView?>(R.id.photo_recycleView).also {
            it.layoutManager = mGridLayoutManager
            it.adapter = mPhotoAdapter
        }

        viewModel.mediaItemListLiveData?.observe(viewLifecycleOwner, Observer {
            Timber.d("notifyDataSetChanged")
            mPhotoAdapter?.setData(it)
            swipeRefreshLayout?.isRefreshing = false
        })
    }

}