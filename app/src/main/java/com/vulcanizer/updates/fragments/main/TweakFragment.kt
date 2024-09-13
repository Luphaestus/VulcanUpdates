package com.vulcanizer.updates.fragments.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SeslProgressBar
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayout
import com.vulcanizer.updates.MainActivity
import com.vulcanizer.updates.R
import com.vulcanizer.updates.fragments.tweaks.AppsFragment
import com.vulcanizer.updates.fragments.tweaks.SUFragment
import com.vulcanizer.updates.fragments.tweaks.TwrpFragment
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.widget.MarginsTabLayout

public class TweakFragment : Fragment() {
    private lateinit var appsFragment: AppsFragment
    private lateinit var suFragment: SUFragment
    private lateinit var twrpFragment: TwrpFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tweak_refresh, container, false)
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
        setHasOptionsMenu(true)
        swipeRefreshLayout.setOnRefreshListener {
            // Restart the main activity
            restartMainActivity()
        }
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTabLayout()

        // Initialize fragments
        appsFragment = AppsFragment()
        suFragment = SUFragment()
        twrpFragment = TwrpFragment()

        // Add all fragments to the FragmentManager
        childFragmentManager.beginTransaction()
            .add(R.id.tweakcontianer, twrpFragment)
            .add(R.id.tweakcontianer, suFragment).hide(suFragment) // Add and hide
            .add(R.id.tweakcontianer, appsFragment).hide(appsFragment) // Add and hide
            .commitAllowingStateLoss()
    }

    fun restartMainActivity() {
        // Create an intent to restart the main activity
        val intent = Intent(requireContext(), MainActivity::class.java) // Replace with your main activity class
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        activity?.finish() // Optional: Call finish() if you want to close the current activity

        // Override the default transition animations
        activity?.overridePendingTransition(0, 0) // No animation
    }

    private fun restartFragment() {
        // Create a new instance of TweakFragment
        val newFragment = TweakFragment()

        // Replace the current fragment with the new instance
        childFragmentManager.beginTransaction()
            .replace(R.id.tweakcontianer, newFragment)
            .commitAllowingStateLoss()
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val binding = (requireActivity() as MainActivity).binding
            binding.drawerLayoutMain.setTitle("Vulcan Updates", "Vulcan Updates")
            val mainTabs = requireActivity().findViewById<MarginsTabLayout>(R.id.main_tabs)
            mainTabs.visibility = View.VISIBLE
            mainTabs.getTabAt(0)?.select()
            scrollToTop()
            (requireActivity() as MainActivity).hide_menu(true  )
            requireActivity().actionBar?.hide()

        } else {
            val mainTabs = requireActivity().findViewById<MarginsTabLayout>(R.id.main_tabs)
            mainTabs.visibility = View.GONE
            (requireActivity() as MainActivity).hide_menu(false)
        }
    }




    private fun scrollToTop() {
        val scrollView = (requireActivity() as MainActivity).findViewById<DrawerLayout>(R.id.drawer_layout_main)
        scrollView?.scrollTo(0, 0)
    }

    private fun initTabLayout() {
        val mainTabs = requireActivity().findViewById<MarginsTabLayout>(R.id.main_tabs)

        mainTabs.tabMode = TabLayout.SESL_MODE_FIXED_AUTO
        mainTabs.removeAllTabs()

        if (mainTabs.tabCount == 0) {
            mainTabs.addTab(mainTabs.newTab().setText("Twrp"))
            mainTabs.addTab(mainTabs.newTab().setText("Modules"))
            mainTabs.addTab(mainTabs.newTab().setText("Apps"))
            mainTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    val fragmentToShow: Fragment = when (tab.text) {
                        "Apps" -> appsFragment
                        "Modules" -> suFragment
                        "Twrp" -> twrpFragment
                        else -> twrpFragment
                    }

                    // Show the selected fragment and hide others
                    childFragmentManager.beginTransaction().apply {
                        // Hide all fragments
                        hide(appsFragment)
                        hide(suFragment)
                        hide(twrpFragment)

                        // Show the selected fragment
                        show(fragmentToShow)

                        // Commit the transaction
                        commitAllowingStateLoss()
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }
    }
}



fun createTweak(
    title: String = "N/A",
    brief: String = "N/A",
    heading: String = "N/A",
    description: String = "N/A",
    images: List<String> = listOf(),
    root: ViewGroup,
    context: Context?,
    icon: String = "https://raw.githubusercontent.com/Luphaestus/VulcanUpdate/main/Apps/discussion-curious-just-curious-why-momo-is-reporting-this-v0-73ntwoz0t8091.jpg",
    data: Any? = null,
    fragment: Fragment,
    download: ((linearLayout: LinearLayout, textView: TextView, previous: String, data: Any?, context: Context) -> Unit)? = null
): LinearLayout {
    val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val roundLinearLayout = inflater.inflate(R.layout.tweaks_layout, null) as LinearLayout
    root.addView(roundLinearLayout)

    roundLinearLayout.findViewById<TextView>(R.id.title).text = title
    val imageView = roundLinearLayout.findViewById<ShapeableImageView>(R.id.logo)

    // Check if context is valid before loading the image
    if (fragment.isAdded) {
        Glide.with(context)
            .load(icon)
            .into(imageView)
    }

    val briefView = roundLinearLayout.findViewById<TextView>(R.id.brief)
    briefView.text = brief
    roundLinearLayout.findViewById<TextView>(R.id.heading).text = heading

    val descriptionTextView = roundLinearLayout.findViewById<TextView>(R.id.description)
    val formattedDescription = description.replace("\\n", "\n")
    descriptionTextView.text = formattedDescription

    val imageContainer = roundLinearLayout.findViewById<LinearLayout>(R.id.image_container)
    for (imageUrl in images) imageContainer.addView(createCardView(context, imageUrl, fragment))

    val commonClickListener = View.OnClickListener {
        roundLinearLayout.findViewById<SeslProgressBar>(R.id.progress).visibility = View.VISIBLE
        roundLinearLayout.findViewById<AppCompatImageButton>(R.id.pause).visibility = View.VISIBLE
        roundLinearLayout.findViewById<AppCompatImageButton>(R.id.stop).visibility = View.VISIBLE
        roundLinearLayout.findViewById<AppCompatImageButton>(R.id.download).visibility = View.GONE
        roundLinearLayout.findViewById<AppCompatImageButton>(R.id.play).visibility = View.GONE
        briefView.text = "Waiting..."
        download?.invoke(roundLinearLayout, briefView, brief, data, context)
    }

    roundLinearLayout.findViewById<AppCompatImageButton>(R.id.download).setOnClickListener(commonClickListener)
    roundLinearLayout.findViewById<AppCompatImageButton>(R.id.play).setOnClickListener(commonClickListener)

    val showImagesButton: View = roundLinearLayout.findViewById(R.id.expand_button)
    val imagesLayout: View = roundLinearLayout.findViewById(R.id.images_layout)

    // Set click listener to the button
    showImagesButton.setOnClickListener {
        showImagesButton.animate().rotationBy(180f).start()
        if (imagesLayout.visibility == View.VISIBLE) {
            fadeOut(imagesLayout)
            retract(roundLinearLayout)
        } else {
            fadeIn(imagesLayout)
            expand(roundLinearLayout)
        }
    }
    return roundLinearLayout
}


private fun expand(view: View) {
    // Set the minimum height to the current height of the view
    view.minimumHeight = view.height

    // Get the current height of the view
    val initialHeight = view.height

    // Measure the view to get its current height
    view.measure(
        View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )

    // Get the target height after measuring
    val targetHeight = view.measuredHeight

    // Create the animation
    val animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            // Calculate the new height based on the interpolated time
            view.layoutParams.height =
                if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT else (initialHeight + (targetHeight - initialHeight) * interpolatedTime).toInt()
            // Request layout to update the view
            view.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }
    // Set animation duration
    animation.duration = 500
    // Start the animation
    view.startAnimation(animation)
}




private fun retract(view: View) {
    val initialHeight = view.height
    val targetHeight = view.minimumHeight // Assuming minimumHeight is set to the original height of the view

    val animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val newHeight = (initialHeight * (1 - interpolatedTime)).toInt()
            view.layoutParams.height = newHeight.coerceAtLeast(targetHeight) // Ensure newHeight doesn't go below targetHeight
            view.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }
    animation.duration = 500
    view.startAnimation(animation)
}

private fun fadeIn(view: View) {
    val anim = AlphaAnimation(0.0f, 1.0f)
    anim.duration = 500 // You can adjust the duration of the fade
    view.visibility = View.VISIBLE
    view.startAnimation(anim)
}

private fun fadeOut(view: View) {
    val anim = AlphaAnimation(1.0f, 0.0f)
    anim.duration = 500 // You can adjust the duration of the fade
    anim.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {}
        override fun onAnimationEnd(animation: Animation) {
            view.visibility = View.GONE
        }

        override fun onAnimationRepeat(animation: Animation) {}
    })
    view.startAnimation(anim)
}

fun createCardView(context: Context, draw:String, fragment: Fragment): CardView {
    // Create CardView
    val cardView = CardView(context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = 2.dpToPx(context) // Adjust margin as needed
        }
        radius = 8.dpToPx(context).toFloat() // Adjust corner radius as needed
    }

    // Create ImageView
    val imageView = ImageView(context).apply {
        layoutParams = ViewGroup.LayoutParams(100.dpToPx(context), 225.dpToPx(context))
        scaleType = ImageView.ScaleType.FIT_XY
    }
    if (fragment.isAdded) {
        Glide.with(context)
            .load(draw)
            .into(imageView)
    }
    // Add ImageView to CardView
    cardView.addView(imageView)

    return cardView
}

// Extension function to convert dp to pixels
fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}
