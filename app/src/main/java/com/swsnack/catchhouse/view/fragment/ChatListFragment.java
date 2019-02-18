package com.swsnack.catchhouse.view.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.swsnack.catchhouse.R;
import com.swsnack.catchhouse.adapter.chattingadapter.ChattingListAdapter;
import com.swsnack.catchhouse.adapter.chattingadapter.ChattingListItemHolder;
import com.swsnack.catchhouse.data.model.Chatting;
import com.swsnack.catchhouse.data.model.User;
import com.swsnack.catchhouse.databinding.FragmentChatListBinding;
import com.swsnack.catchhouse.databinding.ItemNavHeaderBinding;
import com.swsnack.catchhouse.view.BaseFragment;
import com.swsnack.catchhouse.view.activitity.BottomNavActivity;
import com.swsnack.catchhouse.view.activitity.ChattingMessageActivity;
import com.swsnack.catchhouse.viewmodel.chattingviewmodel.ChattingViewModel;
import com.swsnack.catchhouse.viewmodel.userviewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.swsnack.catchhouse.Constant.ParcelableData.CHATTING_DATA;
import static com.swsnack.catchhouse.Constant.ParcelableData.USER_DATA;

public class ChatListFragment extends BaseFragment<FragmentChatListBinding, ChattingViewModel> {

    @Override
    protected int getLayout() {
        return R.layout.fragment_chat_list;
    }

    @Override
    protected Class<ChattingViewModel> getViewModelClass() {
        return ChattingViewModel.class;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BottomNavActivity) {
            ((BottomNavActivity) Objects.requireNonNull(getActivity())).setViewPagerListener(this::getChattingList);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setNavigationDrawer();

        getBinding().setHandler(getViewModel());
        ChattingListAdapter chattingListAdapter = new ChattingListAdapter(getContext(), getViewModel());
        getBinding().rvChatList.setAdapter(chattingListAdapter);
        getBinding().rvChatList.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        chattingListAdapter.setOnItemClickListener((v, position) -> {
            Chatting chatting = chattingListAdapter.getItem(position);
            User user = ((ChattingListItemHolder) v).getBinding().getUserData();

            startActivity(
                    new Intent(getContext(),
                            ChattingMessageActivity.class)
                            .putExtra(CHATTING_DATA, chatting)
                            .putExtra(USER_DATA, user));
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().getChattingRoomList();
    }

    @Override
    public void onStop() {
        super.onStop();
        getViewModel().cancelChattingListChangingListening();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.cancelChattingListChangingListening();
    }

    private void getChattingList() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            getBinding().tvChatListNotSigned.setVisibility(View.VISIBLE);
            getViewModel().setChattingList(new ArrayList<>());
        } else {
            getViewModel().cancelChattingListChangingListening();
            getViewModel().getChattingRoomList();
            getBinding().tvChatListNotSigned.setVisibility(View.GONE);
        }
    }

    private void setNavigationDrawer() {
        UserViewModel mUserViewModel;
        ItemNavHeaderBinding mItemNavHeaderBinding;

        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).setSupportActionBar(getBinding().tbChatList);

        mUserViewModel = ViewModelProviders.of(getActivity()).get(UserViewModel.class);
        mItemNavHeaderBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_nav_header, getBinding().navView, false);
        mItemNavHeaderBinding.setUserViewModel(mUserViewModel);
        mItemNavHeaderBinding.setLifecycleOwner(this);
        getBinding().navView.addHeaderView(mItemNavHeaderBinding.getRoot());

        mItemNavHeaderBinding.navHeaderBack.setOnClickListener(__ -> {
            getBinding().drawerLayout.closeDrawer(GravityCompat.END);
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuInflater menuInflater = getActivity().getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        return;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.menu_search) {
            getBinding().drawerLayout.openDrawer(GravityCompat.END);
            return true;
        }
        return true;
    }
}