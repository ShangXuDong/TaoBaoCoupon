package com.sxd.taobaocoupon.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.sxd.taobaocoupon.R;
import com.sxd.taobaocoupon.model.entity.SearchRecommend;
import com.sxd.taobaocoupon.model.entity.SearchResult;
import com.sxd.taobaocoupon.presenter.ISearchPresenter;
import com.sxd.taobaocoupon.presenter.impl.SearchPresenter;
import com.sxd.taobaocoupon.ui.activity.TicketActivity;
import com.sxd.taobaocoupon.ui.adapter.SearchResultAdapter;
import com.sxd.taobaocoupon.ui.custom.TextFlowLayout;
import com.sxd.taobaocoupon.util.ConstantsUtils;
import com.sxd.taobaocoupon.view.ISearchViewCallback;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class SearchFragment extends BaseFragment implements ISearchViewCallback {

    @BindView(R.id.cancel_btn)
    TextView cancel_btn;

    @BindView(R.id.search_input_box)
    EditText search_input_box;

    @BindView(R.id.search_history_view)
    TextFlowLayout hostoryTextFlow;

    @BindView(R.id.search_recommend_view)
    TextFlowLayout recommendTextFlow;

    @BindView(R.id.search_result_container)
    SmartRefreshLayout resultRefreshLayout;

    @BindView(R.id.search_result_list)
    RecyclerView searchResultList;

    // ?????????????????? ??????
    @BindView(R.id.search_history_delete)
    ImageView historyDeleteBtn;

    // ????????????Layout
    @BindView(R.id.search_history_container)
    LinearLayout historyLayout;

    // ???????????????Layout
    @BindView(R.id.search_recommend_container)
    LinearLayout recommnedLayout;

    SearchResultAdapter searchResultAdapter;

    private ISearchPresenter searchPresenter;

    public SearchFragment() {
        searchPresenter = new SearchPresenter(this);
    }

    @Override
    protected View loadRootView(LayoutInflater inflater, @Nullable ViewGroup container) {
        return LayoutInflater.from(getContext()).inflate(R.layout.fragment_search, container, false);
    }

    @Override
    int getLayoutId() {
        return R.layout.fragment_search_container;
    }

    @Override
    protected void initView() {
        searchResultAdapter = new SearchResultAdapter();
        searchResultList.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultList.setAdapter(searchResultAdapter);
        searchResultList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.top = 6;
                outRect.bottom = 6;
            }
        });

        // ??????????????????
        resultRefreshLayout.setEnableRefresh(false);
        // ?????????RecycleView??????????????????????????????????????????????????????????????????
        resultRefreshLayout.setEnableLoadMore(false);
    }

    @Override
    protected void initListener() {
        // ??????????????????
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recommnedLayout.getVisibility() == View.GONE) {
                    // ????????????????????????????????????
                    // ????????????????????????
                    searchPresenter.getHistory();
                    // ?????????????????????
                    searchPresenter.getRecommend();
                    // ????????????
                    searchResultList.setVisibility(View.GONE);
                    // ????????????
                    resultRefreshLayout.setEnableLoadMore(false);
                    // ?????????????????????????????????
                    historyLayout.setVisibility(View.VISIBLE);
                    recommnedLayout.setVisibility(View.VISIBLE);
                } else {
                    // ??????
                    search_input_box.setText("");
                    // ????????????
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    // ???????????????
                    imm.hideSoftInputFromWindow(cancel_btn.getWindowToken(),0);
                }
            }
        });

        // ???????????????
        search_input_box.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    // ??????
                    // ???????????????????????????????????????return
                    if (v.getText().toString().length() == 0)
                        return true;
                    searchPresenter.search(v.getText().toString());
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    // ???????????????
                    imm.hideSoftInputFromWindow(cancel_btn.getWindowToken(),0);
                    return true;
                }
                return false;
            }
        });

        // ????????????????????????
        historyDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchPresenter.deleteHistory();
                hostoryTextFlow.setTextList(null);
            }
        });

        // ??????????????????
        resultRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                // ???????????????????????????????????????????????????????????????????????? ???????????????
                // ?????????????????????????????????
                searchPresenter.loadMore(search_input_box.getText().toString());
            }
        });

        // ???TextFlowLayout???item??????????????????
        // ????????????
        hostoryTextFlow.setItemClickListener(new TextFlowLayout.ItemClickListener() {
            @Override
            public void onClick(String key) {
                search_input_box.setText(key);
                searchPresenter.search(key);
            }
        });

        recommendTextFlow.setItemClickListener(new TextFlowLayout.ItemClickListener() {
            @Override
            public void onClick(String key) {
                search_input_box.setText(key);
                searchPresenter.search(key);
            }
        });


        // ?????????????????????????????? ????????????????????????
        searchResultAdapter.setItemListener(new SearchResultAdapter.ClickedListener() {
            @Override
            public void onItemClicked(String title, String url, String cover) {
                Intent intent = new Intent(getActivity(), TicketActivity.class);
                intent.putExtra(ConstantsUtils.KEY_INTENT_TICKET_TITLE, title);
                intent.putExtra(ConstantsUtils.KEY_INTENT_TICKET_COVER, cover);
                intent.putExtra(ConstantsUtils.KEY_INTENT_TICKET_URL, url);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void loadData() {
        searchPresenter.getHistory();
        searchPresenter.getRecommend();
        setUpState(State.SUCCESS);
    }


    @Override
    public void onHistoryLoadedSuccess(List<String> history) {
        hostoryTextFlow.setTextList(history);
    }


    @Override
    public void onSearchSuccess(SearchResult result) {
        // ???????????? ?????????????????????????????????
        historyLayout.setVisibility(View.GONE);
        recommnedLayout.setVisibility(View.GONE);
        searchResultAdapter.setData(result.getData());
        searchResultList.setVisibility(View.VISIBLE);
        // ????????????
        resultRefreshLayout.setEnableLoadMore(true);
    }

    @Override
    public void onSearchLoadedMoreSuccess(SearchResult result) {
        // ??????adapter????????????
        searchResultAdapter.addData(result.getData());
        // ????????????
        resultRefreshLayout.finishLoadMore();
    }


    @Override
    public void onSearchRecommend(SearchRecommend recommend) {
        List<String> recommendKeys = new ArrayList<>();
        for (SearchRecommend.DataBean datum : recommend.getData()) {
            recommendKeys.add(datum.getKeyword());
        }
        recommendTextFlow.setTextList(recommendKeys);
    }
}
