package com.sam.lib.banner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.GravityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

open class Banner<T> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : BannerBaseView(context, attrs, defStyleAttr) {

    private val TAG = "Banner"

    private val mLock = Any()

    private var mAttached: Boolean = false

    private var mBannerItemResourceId: Int = 0
    private var mIndicatorMargin: Int = 0
    private var mIndicatorGravity: Int = 0

    private var mRecyclerView: RecyclerView? = null
    private var mLinearLayout: LinearLayout? = null

    private var mBannerAdapter: WrapBannerAdapter? = null

    private val mHandler = Handler()

    private var isPlaying: Boolean = false
    private var mIsIndicatorShow: Boolean = false
    private var mNestedEnabled: Boolean = false

    private var mFilter = IntentFilter()

    private var mOnItemPickListener: OnItemPickListener<T>? = null

    private var mOnItemBindListener: OnItemBindListener<T>? = null

    private var mOnItemClickListener: OnItemClickListener<T>? = null

    private var mIndicatorGainDrawable: Drawable? = null
    private var mIndicatorMissDrawable: Drawable? = null

    private var mInterval: Int = 0
    private var mCurrentIndex: Int = 0
    private var mIndicatorSize: Int = 0
    private var mIndicatorSpace: Int = 0

    companion object {
        private const val DEFAULT_GAIN_COLOR = -0x1
        private const val DEFAULT_MISS_COLOR = 0x50ffffff
    }

    private val mBannerTask = object : Runnable {

        override fun run() {
            if (isPlaying) {
                val firstPos =
                    (mRecyclerView!!.layoutManager as BannerLayoutManager).findFirstVisibleItemPosition()
                if (firstPos >= mCurrentIndex) {
                    mRecyclerView!!.smoothScrollToPosition(++mCurrentIndex)
                    switchIndicator()
                    mHandler.postDelayed(this, mInterval.toLong())
                } else {
                    mHandler.postDelayed(this, (mInterval * 2).toLong())
                }
            }
        }

    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when {
                Intent.ACTION_USER_PRESENT == action -> {
                    setPlaying(true)
                }
                Intent.ACTION_SCREEN_ON == action -> {
                    setPlaying(true)
                }
                Intent.ACTION_SCREEN_OFF == action -> {
                    setPlaying(false)
                }
                BannerAction.ACTION_LOOP == action -> {
                    setPlaying(true)
                }
                BannerAction.ACTION_STOP == action -> {
                    setPlaying(false)
                }
            }
        }
    }

    interface OnItemClickListener<T> {

        fun onItemClick(position: Int, item: T)
    }

    interface OnItemPickListener<T> {

        fun onItemPick(position: Int, item: T)
    }

    interface OnItemBindListener<T> {

        fun onItemBind(position: Int, item: T, view: ImageView, holder: BannerViewHolder)

    }

    init {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.Banner)
        val gainDrawable = attributes.getDrawable(R.styleable.Banner_indicator_gain)//指示器选中的样式
        val missDrawable = attributes.getDrawable(R.styleable.Banner_indicator_miss)//指示器未选中的样式
        mIndicatorGravity = attributes.getInt(R.styleable.Banner_indicator_gravity, 1)//START,CENTER,END
        mIsIndicatorShow = attributes.getBoolean(R.styleable.Banner_indicator_show, true)//指示器是否显示
        mNestedEnabled = attributes.getBoolean(R.styleable.Banner_nested_enabled, true)
        val mInch = attributes.getFloat(R.styleable.Banner_banner_inch, 100f)//轮播动画时长
        mInterval = attributes.getInt(R.styleable.Banner_banner_interval, 3000)//轮播切换时长
        mIndicatorSize = attributes.getDimensionPixelSize(R.styleable.Banner_indicator_size, 0)//指示器大小 最小2dp
        mIndicatorSpace = attributes.getDimensionPixelSize(R.styleable.Banner_indicator_space, dp2px(4))//指示器之间的距离
        mIndicatorMargin = attributes.getDimensionPixelSize(R.styleable.Banner_indicator_margin, dp2px(8))//距离上下左右间距
        mBannerItemResourceId = attributes.getResourceId(R.styleable.Banner_banner_item_layout, R.layout.lib_banner_imageview)//自定义轮播布局Id

        mIndicatorGainDrawable = if (gainDrawable == null) {
            getDefaultDrawable(DEFAULT_GAIN_COLOR)
        } else {
            if (gainDrawable is ColorDrawable) {
                getDefaultDrawable(gainDrawable)
            } else {
                gainDrawable
            }
        }

        mIndicatorMissDrawable = if (missDrawable == null) {
            getDefaultDrawable(DEFAULT_MISS_COLOR)
        } else {
            if (missDrawable is ColorDrawable) {
                getDefaultDrawable(missDrawable)
            } else {
                missDrawable
            }
        }

        when (mIndicatorGravity) {
            0 -> mIndicatorGravity = GravityCompat.START
            1 -> mIndicatorGravity = Gravity.CENTER
            2 -> mIndicatorGravity = GravityCompat.END
        }

        attributes.recycle()

        mRecyclerView = RecyclerView(context)
        mLinearLayout = LinearLayout(context)

        mFilter.addAction(Intent.ACTION_SCREEN_ON)
        mFilter.addAction(Intent.ACTION_SCREEN_OFF)
        mFilter.addAction(Intent.ACTION_USER_PRESENT)
        mFilter.addAction(BannerAction.ACTION_LOOP)
        mFilter.addAction(BannerAction.ACTION_STOP)

        PagerSnapHelper().attachToRecyclerView(mRecyclerView)
        mBannerAdapter = WrapBannerAdapter(SimpleBannerAdapter(this))
        mRecyclerView!!.adapter = mBannerAdapter
        mRecyclerView!!.overScrollMode = View.OVER_SCROLL_NEVER
        mRecyclerView!!.isNestedScrollingEnabled = mNestedEnabled
        mRecyclerView!!.layoutManager =
            BannerLayoutManager(context, LinearLayoutManager.HORIZONTAL, false, mInch)
        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val sPos =
                        (recyclerView.layoutManager as BannerLayoutManager).findFirstVisibleItemPosition()
                    val ePos =
                        (recyclerView.layoutManager as BannerLayoutManager).findLastVisibleItemPosition()
                    if (sPos == ePos && mCurrentIndex != ePos) {
                        mCurrentIndex = ePos
                        switchIndicator()
                    }
                }
            }
        })

        val recyclerViewParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val linearLayoutParams =
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        linearLayoutParams.gravity = Gravity.BOTTOM or mIndicatorGravity
        linearLayoutParams.setMargins(
            mIndicatorMargin,
            mIndicatorMargin,
            mIndicatorMargin,
            mIndicatorMargin
        )
        mLinearLayout!!.orientation = LinearLayout.HORIZONTAL
        mLinearLayout!!.gravity = Gravity.CENTER

        addView(mRecyclerView, recyclerViewParams)
        addView(mLinearLayout, linearLayoutParams)
    }

    private fun getDefaultDrawable(drawable: Drawable): Drawable {
        return getDefaultDrawable((drawable as ColorDrawable).color)
    }

    private fun getDefaultDrawable(color: Int): Drawable {
        val gradient = GradientDrawable()
        gradient.setSize(dp2px(6), dp2px(6))
        gradient.cornerRadius = dp2px(6).toFloat()
        gradient.setColor(color)
        return gradient
    }

    fun setDefaultGainColor(color: Int) {
        mIndicatorGainDrawable = getDefaultDrawable(color)
    }

    fun setDefaultMissColor(color: Int) {
        mIndicatorMissDrawable = getDefaultDrawable(color)
    }

    fun setIndicatorGravity(gravity: Int) {
        mIndicatorGravity = gravity
        val params = mLinearLayout!!.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM or mIndicatorGravity
        mLinearLayout!!.layoutParams = params
    }

    fun setIndicatorMargin(margin: Int) {
        mIndicatorMargin = dp2px(margin)
        val params = mLinearLayout!!.layoutParams as FrameLayout.LayoutParams
        params.setMargins(mIndicatorMargin, mIndicatorMargin, mIndicatorMargin, mIndicatorMargin)
        mLinearLayout!!.layoutParams = params
    }

    fun getCurrentIndex(): Int {
        val size = mBannerAdapter?.getWrappedAdapter()?.getItemCount() ?: 0
        return if (size == 0) -1 else mCurrentIndex % size
    }

    fun getCurrentItem(): T? {
        val size = mBannerAdapter?.getWrappedAdapter()?.getItemCount() ?: 0
        return if (size > 0) mBannerAdapter?.getWrappedAdapter()?.getItem(size) else null
    }

    private fun createIndicators() {
        mLinearLayout?.run {
            removeAllViews()
            visibility = View.GONE
            if (mIsIndicatorShow && mIsNotSingleData) {
                visibility = View.VISIBLE
                val size = mBannerAdapter?.getWrappedAdapter()?.getItemCount() ?: 0
                for (i in 0 until size) {
                    val img = AppCompatImageView(context)
                    val lp = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    lp.leftMargin = mIndicatorSpace / 2
                    lp.rightMargin = mIndicatorSpace / 2
                    if (mIndicatorSize >= dp2px(4)) {
                        lp.height = mIndicatorSize
                        lp.width = lp.height
                    } else {
                        img.minimumWidth = dp2px(2)
                        img.minimumHeight = dp2px(2)
                    }
                    img.setImageDrawable(if (i == 0) mIndicatorGainDrawable else mIndicatorMissDrawable)
                    addView(img, lp)
                }
            }
        }
    }

    private fun switchIndicator() {
        mBannerAdapter?.mWrappedAdapter?.run {
            if (getItemCount() > 0) {
                val position = mCurrentIndex % getItemCount()
                val data = getItem(position)
                if (mIsIndicatorShow && mLinearLayout != null && mLinearLayout!!.childCount > 0) {
                    for (i in 0 until mLinearLayout!!.childCount) {
                        (mLinearLayout!!.getChildAt(i) as AppCompatImageView).setImageDrawable(if (i == position) mIndicatorGainDrawable else mIndicatorMissDrawable)
                    }
                }

                if (mOnItemPickListener != null) {
                    mOnItemPickListener!!.onItemPick(position, data)
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> setPlaying(false)
            MotionEvent.ACTION_MOVE -> {
            }
            MotionEvent.ACTION_UP -> setPlaying(true)
            MotionEvent.ACTION_CANCEL -> setPlaying(true)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.e(TAG, "Banner onAttachedToWindow")
        onAttachedAction()
        regReceiver()
    }

    override fun onAttachedAction() {
        setPlaying(true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.e(TAG, "Banner onDetachedFromWindow")
        setPlaying(false)
        unrReceiver()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        setPlaying(true)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        setPlaying(false)
    }

    var mIsNotSingleData = false

    fun setBannerData(data: List<T>?, onItemBindListener: OnItemBindListener<T>) {
        mOnItemBindListener = onItemBindListener
        setPlaying(false)
        val innerAdapter = SimpleBannerAdapter<T>(this)
        innerAdapter.setData(data)
        if (innerAdapter.getItemCount() > 1) {
            mIsNotSingleData = true
            mCurrentIndex = innerAdapter.getItemCount() * 10
            mBannerAdapter?.setWrappedAdapter(innerAdapter)
            mRecyclerView?.scrollToPosition(mCurrentIndex)
            createIndicators()
            setPlaying(true)
        } else {
            mCurrentIndex = 0
            mIsNotSingleData = false
            mBannerAdapter?.setWrappedAdapter(innerAdapter)
            mBannerAdapter?.notifyDataSetChanged()
            createIndicators()
        }
    }

    fun setBannerData(innerAdapter: BaseBannerAdapter<T>) {
        setPlaying(false)
        if(innerAdapter.getItemCount()>1){
            mIsNotSingleData = true
            mCurrentIndex = innerAdapter.getItemCount() * 10
            mBannerAdapter?.setWrappedAdapter(innerAdapter)
            mRecyclerView?.scrollToPosition(mCurrentIndex)
            createIndicators()
            setPlaying(true)
        }else{
            mCurrentIndex = 0
            mIsNotSingleData = false
            mBannerAdapter?.setWrappedAdapter(innerAdapter)
            mBannerAdapter?.notifyDataSetChanged()
            createIndicators()
        }
    }

    fun scrollToCurrentPosition() {
        if (mRecyclerView != null) {
            mRecyclerView!!.scrollToPosition(mCurrentIndex)
            switchIndicator()
        }
    }

    interface BaseBannerAdapter<T> {
        fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder?
        fun getItemCount(): Int
        fun onBindViewHolder(holder: BannerViewHolder, position: Int)
        fun getItem(position: Int): T
        fun getItemViewType(position: Int):Int
    }

    fun dispatchItemBindListener(position: Int, item: T, view: ImageView, holder: BannerViewHolder){
        mOnItemBindListener?.onItemBind(position,item,view , holder)
    }

    open class BannerViewHolder constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val mViews = SparseArray<View>()

        fun <T : View?> getView(@IdRes resID: Int): T {
            var view = mViews[resID]
            if (view == null) {
                view = itemView.findViewById(resID)
                mViews.put(resID, view)
            }
            return view as T
        }

        val imageView: ImageView = itemView.findViewById(R.id.banner_image_view_id)

    }



    class SimpleBannerAdapter<T>(var mBanner: Banner<T>) : BaseBannerAdapter<T> {

        private var mData: List<T>? = null

        fun setData(data: List<T>?) {
            mData = data
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder? = null

        override fun getItemCount() = mData?.size ?: 0

        override fun getItem(position: Int) = mData!![position]

        override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
            mBanner.dispatchItemBindListener(position, mData!![position], holder.imageView, holder)
        }

        override fun getItemViewType(position: Int) = 0

    }

    inner class WrapBannerAdapter(var mWrappedAdapter: BaseBannerAdapter<T>) :
        RecyclerView.Adapter<BannerViewHolder>() {

        fun setWrappedAdapter(adapter: BaseBannerAdapter<T>) {
            this.mWrappedAdapter = adapter
            notifyDataSetChanged()
        }

        fun getWrappedAdapter() = mWrappedAdapter

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {

            var holder = mWrappedAdapter.onCreateViewHolder(parent, viewType)
            if (holder == null) {
                val inflater = LayoutInflater.from(parent.context)
                val itemView = inflater.inflate(mBannerItemResourceId, parent, false)
                holder = BannerViewHolder(itemView)
            }
            holder.itemView.setOnClickListener {
                val position = mCurrentIndex % mWrappedAdapter.getItemCount()
                val data = mWrappedAdapter.getItem(position)
                mOnItemClickListener?.onItemClick(position, data)

            }
            return holder
        }

        override fun getItemCount(): Int {
            val size = mWrappedAdapter.getItemCount()
            return if (size < 2) size else Integer.MAX_VALUE
        }

        override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
            val pos = position % mWrappedAdapter.getItemCount()
            mWrappedAdapter.onBindViewHolder(holder, pos)
        }

        override fun getItemViewType(position: Int): Int {
            val pos = position % mWrappedAdapter.getItemCount()
            return mWrappedAdapter.getItemViewType(pos)
        }

    }


    fun setPlaying(playing: Boolean) {
        synchronized(mLock) {
            if (playing) {
                playBanner()
            } else {
                stopBanner()
            }
        }
    }

    private fun playBanner() {
        if (!isPlaying && mBannerAdapter!!.itemCount > 1) {
            isPlaying = true
            mHandler.removeCallbacks(mBannerTask)
            mHandler.postDelayed(mBannerTask, mInterval.toLong())
            Log.e(TAG, "Play Banner")
        }
    }

    private fun stopBanner() {
        isPlaying = false
        mHandler.removeCallbacks(mBannerTask)
        Log.e(TAG, "Stop Banner")
    }

    fun setOnItemClickListener(listener: OnItemClickListener<T>) {
        mOnItemClickListener = listener
    }

    fun setOnItemPickListener(listener: OnItemPickListener<T>) {
        mOnItemPickListener = listener
    }

    private fun dp2px(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()
    }

    private fun regReceiver() {
        if (!mAttached) {
            mAttached = true
            context.registerReceiver(mReceiver, mFilter)
        }
    }

    private fun unrReceiver() {
        if (mAttached) {
            context.unregisterReceiver(mReceiver)
            mAttached = false
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
    }

}
