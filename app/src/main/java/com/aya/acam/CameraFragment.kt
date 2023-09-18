package com.aya.acam

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.aya.acam.databinding.FragmentCameraBinding
import com.aya.acam.transformer.ZoomInTransformer
import com.aya.acam.utils.MediaUtils
import com.google.android.material.tabs.TabLayoutMediator
import timber.log.Timber

class CameraFragment : Fragment() {

    companion object {
        fun newInstance() = CameraFragment()
    }

    private lateinit var viewModel: CameraViewModel
    private var galleryViewModel: GalleryViewModel? = null
    private var imageViewHeight = -1;
    private lateinit var binding: FragmentCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)
        galleryViewModel = ViewModelProvider(requireActivity()).get(GalleryViewModel::class.java)
        viewModel.initCamera()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCameraBinding.inflate(inflater)

        binding.imageView.setOnClickListener { navigateToGalleryFragment() }

        // 创建适配器并设置给 ViewPager2
        val adapter = CameraViewAdapter(viewModel)
        binding.viewPager2.adapter = adapter
        binding.viewPager2.setPageTransformer(ZoomInTransformer())
        binding.viewPager2.isUserInputEnabled = false
//        binding.viewPager2.offscreenPageLimit = 1
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // 页面切换时的操作
                Timber.d("onPageSelected: $position")
                viewModel.startCamera(position, this@CameraFragment)
            }
        })

        TabLayoutMediator(binding.tabLayout, binding.viewPager2, true, false) { tab, position ->
            // 根据位置设置选项卡的标题
            tab.text = viewModel.views[position].getTypeString()
            tab.customView
        }.attach()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.imageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                Timber.d("imageView measuredHeight = ${binding.imageView.measuredHeight}")
                imageViewHeight = binding.imageView.measuredHeight

                galleryViewModel?.mediaItemListLiveData?.observe(viewLifecycleOwner) { list ->

                    if (list.isEmpty()) {
                        Timber.d("galleryViewModel :list.isEmpty() ")
                    } else {
                        val bitmap =
                            MediaUtils.getBitmap(requireContext(), list.first(), imageViewHeight)
                        binding.imageView.setImageBitmap(bitmap)
                    }
                }
            }
        })

        binding.shutter.setOnClickListener {
            binding.shutter.progress = 0f
            binding.shutter.playAnimation()
            viewModel.shot(binding.viewPager2.currentItem)
        }

    }

    private fun navigateToGalleryFragment() {
        findNavController().navigate(R.id.action_cameraFragment_to_galleryFragment)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releaseCamera()
    }

}