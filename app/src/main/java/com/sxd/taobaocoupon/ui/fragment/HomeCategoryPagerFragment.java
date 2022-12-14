package com.sxd.taobaocoupon.ui.fragment;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.ScrollBoundaryDecider;
import com.sxd.taobaocoupon.R;
import com.sxd.taobaocoupon.model.entity.Categories;
import com.sxd.taobaocoupon.model.entity.CategoryDetail;
import com.sxd.taobaocoupon.presenter.ICategoryPagerPresenter;
import com.sxd.taobaocoupon.presenter.impl.CategoryPagerPresenterImpl;
import com.sxd.taobaocoupon.ui.activity.TicketActivity;
import com.sxd.taobaocoupon.ui.adapter.HomeCategoryViewPagerListViewAdapter;
import com.sxd.taobaocoupon.ui.adapter.LooperAdapter;
import com.sxd.taobaocoupon.ui.custom.AutoLoopViewPager;
import com.sxd.taobaocoupon.ui.custom.ShopNestedScrollView;
import com.sxd.taobaocoupon.util.ConstantsUtils;
import com.sxd.taobaocoupon.util.SizeUtils;
import com.sxd.taobaocoupon.view.IHomeCategoryPagerViewCallback;

import java.util.List;

import butterknife.BindView;

public class HomeCategoryPagerFragment extends BaseFragment implements IHomeCategoryPagerViewCallback {

    @BindView(R.id.home_pager_list)
    public RecyclerView categoryDetailRecycleList;

    @BindView(R.id.looper_pager)
    public AutoLoopViewPager looperPager;

    @BindView(R.id.tv_home_pager_title)
    public TextView catetoryTitleTextView;

    @BindView(R.id.home_category_pager_refresh_list)
    SmartRefreshLayout categoryRefreshLayout;

    @BindView(R.id.home_pager_title)
    LinearLayout title;

    @BindView(R.id.category_pager_parent)
    LinearLayout pagerParent;

    @BindView(R.id.scrollView)
    ShopNestedScrollView nestedScrollView;

    @BindView(R.id.looper_and_title)
    LinearLayout looperAndTitle;

    ICategoryPagerPresenter mPagerPresenter;

    // ??????????????????TabLayout???????????????????????????ViewPager?????????FragmentPagerAdapter????????????Fragment???
    int categoryId;

    HomeCategoryViewPagerListViewAdapter mHomeCategotyViewPagerListAdapter;

    LooperAdapter mLooperAdapter;

    boolean dataLoaded = false;

    public static HomeCategoryPagerFragment newInstance(Categories.DataBean category) {
        HomeCategoryPagerFragment homePagerFragment = new HomeCategoryPagerFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ConstantsUtils.KEY_HOME_PAGE_CATEGORY_TITLE, category.getTitle());
        bundle.putInt(ConstantsUtils.KEY_HOME_PAGE_CATEGORY_ID, category.getId());
        homePagerFragment.setArguments(bundle);
        return homePagerFragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Fragment???????????????Looper?????????
        looperPager.startAutoLoop();
    }

    @Override
    public void onPause() {
        super.onPause();
        // TabLayout ?????????????????? ?????????onPause
        looperPager.stopAutoLoop();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @Override
    int getLayoutId() {
        return R.layout.fragment_home_category_pager;
    }

    @Override
    protected void initView() {

        Bundle arguments = getArguments();

        categoryId = arguments.getInt(ConstantsUtils.KEY_HOME_PAGE_CATEGORY_ID);

        categoryDetailRecycleList.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryDetailRecycleList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.top = 6;
                outRect.bottom = 6;
            }
        });
        mHomeCategotyViewPagerListAdapter = new HomeCategoryViewPagerListViewAdapter();
        categoryDetailRecycleList.setAdapter(mHomeCategotyViewPagerListAdapter);

        mLooperAdapter = new LooperAdapter();
        looperPager.setAdapter(mLooperAdapter);

        // ??????refreshLayout ?????????????????????
        categoryRefreshLayout.setEnableRefresh(false);
        setUpState(State.SUCCESS);
    }

    @Override
    protected void initListener() {

        nestedScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int measuredHeight = nestedScrollView.getMeasuredHeight();
                nestedScrollView.setHeaderHeight(looperAndTitle.getMeasuredHeight());
                // getLayoutParams -1 -2 ??????match_parent wrap_content ?????????????????????
                ViewGroup.LayoutParams layoutParams = categoryDetailRecycleList.getLayoutParams();
                if (layoutParams.height == -1 || layoutParams.height == -2) {
                    layoutParams.height = measuredHeight;
                    categoryDetailRecycleList.setLayoutParams(layoutParams);
                    nestedScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
//                Log.e("sxd", "layout????????????");
            }
        });

        catetoryTitleTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("sxd", categoryDetailRecycleList.getMeasuredHeight() + "");
                Log.e("sxd", categoryDetailRecycleList.getHeight() + "");
            }
        });
        looperPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                int size = mLooperAdapter.getDataSize();
                updateLooperIndicator(position % size);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        categoryRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                mPagerPresenter.loadMore(getCategoryId());
            }
        });

        categoryRefreshLayout.setScrollBoundaryDecider(new ScrollBoundaryDecider() {
            @Override
            public boolean canRefresh(View content) {
                return false;
            }

            @Override
            public boolean canLoadMore(View content) {
                //
//                if mHomeCategotyViewPagerListAdapter.
                ShopNestedScrollView v = (ShopNestedScrollView) content;

                RecyclerView recyclerView = (RecyclerView) ((LinearLayout)v.getChildAt(0)).getChildAt(1);


                int scrollRange = recyclerView.computeVerticalScrollRange(); // 14336
                int scrollOffset = recyclerView.computeVerticalScrollOffset(); // ????????????
                int height = recyclerView.getHeight(); // 1888
                Log.e("sxd", recyclerView.computeVerticalScrollRange() +"");
                Log.e("sxd", recyclerView.computeVerticalScrollOffset() +"");

                // ????????????
                if (scrollRange <= (scrollOffset + height))
                    return true;

                return false;
            }
        });


        // ???list?????????????????????????????????????????????Activity
        mHomeCategotyViewPagerListAdapter.setOnItemClickListener(new HomeCategoryViewPagerListViewAdapter.onListItemClickedListener() {
            @Override
            public void onItemClick(CategoryDetail.DataBean item) {
                Log.e("sxd", "??????");
                handleItemClicked(item);
            }
        });

        // ???Looper????????????
        mLooperAdapter.setOnItemClickListener(new LooperAdapter.onLooperClickedListener() {
            @Override
            public void onItemClick(CategoryDetail.DataBean item) {

                handleItemClicked(item);
            }
        });

    }

    private void updateLooperIndicator(int targetPosition) {
        for (int i = 0; i < looperPointContainer.getChildCount(); i++) {
            View point = looperPointContainer.getChildAt(i);
            if (i == targetPosition)
                point.setBackgroundResource(R.drawable.shape_indicator_selected_bg);
            else
                point.setBackgroundResource(R.drawable.shape_indicator_normal_bg);
        }
    }

    @Override
    protected void loadData() {
        setUpState(State.LOADING);
        mPagerPresenter.getCategoryById(categoryId);
        catetoryTitleTextView.setText(getArguments().getString(ConstantsUtils.KEY_HOME_PAGE_CATEGORY_TITLE));
        setUpState(State.SUCCESS);
    }

    @Override
    protected void initPresenter() {
        mPagerPresenter = CategoryPagerPresenterImpl.getInstance(this);
        mPagerPresenter.registerCallback(this);
    }


    @Override
    public void onContentLoaded(List<CategoryDetail.DataBean> contents) {
        mHomeCategotyViewPagerListAdapter.setData(contents);
        // ????????????????????????Title?????????
        setTitleVisiable();
    }

    private void setTitleVisiable() {
        if (dataLoaded == false) {
            title.setVisibility(View.VISIBLE);
            dataLoaded = true;
        }
    }

    @Override
    public void onError() {

    }

    @Override
    public void onEmpty() {

    }

    @Override
    public void onLoading() {

    }

    @Override
    public void onLoadedMoreError() {

    }

    @Override
    public void onLoadMoreEmpty() {
        // ???????????????null ????????????????????????????????????????????????????????????
        categoryRefreshLayout.finishLoadMore();
        categoryRefreshLayout.setEnableLoadMore(false);
        Toast.makeText(getContext(), "??????????????????!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLoadMoreLoaded(List<CategoryDetail.DataBean> contents) {
        // ??????contents????????????null????????????????????????????????????????????????????????????
        Log.e("sxd", contents + "");
        mHomeCategotyViewPagerListAdapter.addData(contents);
        categoryRefreshLayout.finishLoadMore();

    }

    @BindView(R.id.looper_point_container)
    LinearLayout looperPointContainer;

    @Override
    public void onLooperListLoaded(List<CategoryDetail.DataBean> contents) {

        mLooperAdapter.setData(contents);
        looperPager.setCurrentItem((contents.size() * 2));
        int i = 0;
        for (CategoryDetail.DataBean content : contents) {
            View point = new View(getContext());
            int size = SizeUtils.dip2px(getContext(), 8);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(size, size);
            layoutParams.leftMargin = SizeUtils.dip2px(getContext(), 5);
            layoutParams.rightMargin = SizeUtils.dip2px(getContext(), 5);
            point.setLayoutParams(layoutParams);

            if (i++ == 0)
                point.setBackgroundResource(R.drawable.shape_indicator_selected_bg);
            else
                point.setBackgroundResource(R.drawable.shape_indicator_normal_bg);

            looperPointContainer.addView(point);
        }
    }


    @Override
    public int getCategoryId() {
        return categoryId;
    }

    private void handleItemClicked(CategoryDetail.DataBean item) {
        String title = item.getTitle();
        String url = item.getCoupon_click_url();
        String cover = item.getPict_url();

        Intent intent = new Intent(getActivity(), TicketActivity.class);
        intent.putExtra(ConstantsUtils.KEY_INTENT_TICKET_TITLE, title);
        intent.putExtra(ConstantsUtils.KEY_INTENT_TICKET_COVER, cover);
        intent.putExtra(ConstantsUtils.KEY_INTENT_TICKET_URL, url);
        startActivity(intent);
    }
}