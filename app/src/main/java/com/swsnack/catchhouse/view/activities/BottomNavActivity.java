package com.swsnack.catchhouse.view.activities;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.bumptech.glide.load.engine.GlideException;
import com.facebook.FacebookException;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DatabaseException;
import com.swsnack.catchhouse.R;
import com.swsnack.catchhouse.constants.Constants;
import com.swsnack.catchhouse.data.AppDataManager;
import com.swsnack.catchhouse.data.roomsdata.RoomsRepository;
import com.swsnack.catchhouse.data.userdata.api.AppAPIManager;
import com.swsnack.catchhouse.data.userdata.remote.AppUserDataManager;
import com.swsnack.catchhouse.databinding.ActivityBottomNavBinding;
import com.swsnack.catchhouse.view.BaseActivity;
import com.swsnack.catchhouse.view.fragments.HomeFragment;
import com.swsnack.catchhouse.view.fragments.MapFragment;
import com.swsnack.catchhouse.view.fragments.MyPageFragment;
import com.swsnack.catchhouse.view.fragments.SignInFragment;
import com.swsnack.catchhouse.viewmodel.ViewModelListener;
import com.swsnack.catchhouse.viewmodel.roomsviewmodel.RoomsViewModel;
import com.swsnack.catchhouse.viewmodel.roomsviewmodel.RoomsViewModelFactory;
import com.swsnack.catchhouse.viewmodel.searchviewmodel.SearchViewModel;
import com.swsnack.catchhouse.viewmodel.searchviewmodel.SearchViewModelFactory;
import com.swsnack.catchhouse.viewmodel.userviewmodel.InSufficientException;
import com.swsnack.catchhouse.viewmodel.userviewmodel.UserViewModel;
import com.swsnack.catchhouse.viewmodel.userviewmodel.UserViewModelFactory;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;

import static com.swsnack.catchhouse.constants.Constants.ExceptionReason.DELETED_USER;
import static com.swsnack.catchhouse.constants.Constants.ExceptionReason.DUPLICATE_NICK_NAME;
import static com.swsnack.catchhouse.constants.Constants.ExceptionReason.IN_SUFFICIENT_INFO;
import static com.swsnack.catchhouse.constants.Constants.ExceptionReason.NOT_SIGNED_USER;
import static com.swsnack.catchhouse.constants.Constants.ExceptionReason.SAME_NICK_NAME;
import static com.swsnack.catchhouse.constants.Constants.ExceptionReason.SHORT_PASSWORD;
import static com.swsnack.catchhouse.constants.Constants.ExceptionReason.SIGN_UP_EXCEPTION;

public class BottomNavActivity extends BaseActivity<ActivityBottomNavBinding> implements ViewModelListener, HomeFragment.OnSearchButtonListener {

    private FragmentManager mFragmentManager;
    private CompositeDisposable mDisposable;

    @Override
    protected int getLayout() {
        return R.layout.activity_bottom_nav;
    }

    // FIXME 좋은 설계가 아닌것 같습니다. onError()에서 하는일은 Throwable case에 따라서 토스트 메세지를 호출하는것뿐이고 아래와 같은 문제가 있습니다.
    // 1. 비슷한 코드가 복붙되어 간결하지 않은 코드
    // 2. View에서 이미 ViewModel에서 일어날 에러에 대해서 알고 instanceof로 handling한다는것 자체가 독립적이지 않음
    // 각 에러에 대한 별도의 처리는 자체적으로 하고 showErrorMessage(String message)와 같은 패턴으로 하는게 더 좋아보입니다.
    @Override
    public void onError(Throwable throwable) {
        unFreezeUI();
        if (throwable instanceof FirebaseAuthInvalidCredentialsException || throwable instanceof FirebaseAuthInvalidUserException) {
            Snackbar.make(getBinding().getRoot(), R.string.snack_invalid_user, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (throwable instanceof FirebaseAuthUserCollisionException) {
            Snackbar.make(getBinding().getRoot(), R.string.snack_already_exist_email, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (throwable instanceof GlideException) {
            Snackbar.make(getBinding().getRoot(), R.string.snack_failed_load_image, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (throwable instanceof FacebookException || throwable instanceof GoogleAuthException) {
            Snackbar.make(getBinding().getRoot(), R.string.snack_fb_sign_in_failed, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (throwable instanceof DatabaseException) {
            Snackbar.make(getBinding().getRoot(), R.string.snack_database_exception, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (throwable instanceof InSufficientException) {
            switch (((InSufficientException) throwable).getReason()) {
                case IN_SUFFICIENT_INFO:
                    Snackbar.make(getBinding().getRoot(), R.string.snack_fill_info, Snackbar.LENGTH_SHORT).show();
                    break;
                case SHORT_PASSWORD:
                    Snackbar.make(getBinding().getRoot(), R.string.snack_short_password, Snackbar.LENGTH_SHORT).show();
                    break;
            }
            return;
        }

        switch (throwable.getMessage()) {
            case SIGN_UP_EXCEPTION:
                Snackbar.make(getBinding().getRoot(), R.string.snack_fb_sign_up_failed, Snackbar.LENGTH_SHORT).show();
                break;
            case NOT_SIGNED_USER:
                Snackbar.make(getBinding().getRoot(), R.string.snack_fb_not_signed_user, Snackbar.LENGTH_SHORT).show();
                break;
            case DELETED_USER:
                Snackbar.make(getBinding().getRoot(), R.string.snack_fb_not_signed_user, Snackbar.LENGTH_SHORT).show();
                mFragmentManager.beginTransaction().replace(R.id.fl_bottom_nav_container, new SignInFragment(), SignInFragment.class.getName()).commit();
                break;
            case SAME_NICK_NAME:
                Snackbar.make(getBinding().getRoot(), R.string.snack_same_nick_name, Snackbar.LENGTH_SHORT).show();
                break;
            case DUPLICATE_NICK_NAME:
                Snackbar.make(getBinding().getRoot(), R.string.snack_duplicate_nick_name, Snackbar.LENGTH_SHORT).show();
                break;
            default:
                Snackbar.make(getBinding().getRoot(), R.string.snack_error_occured, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSuccess(String success) {
        unFreezeUI();
        switch (success) {
            case Constants.UserStatus.SIGN_UP_SUCCESS:
                mFragmentManager.popBackStack();
                break;
            case Constants.UserStatus.SIGN_IN_SUCCESS:
                /*handle here : when sign in success replace fragment to my page*/
                mFragmentManager.beginTransaction().replace(R.id.fl_bottom_nav_container, new MyPageFragment(), MyPageFragment.class.getName()).commit();
                break;
            case Constants.UserStatus.DELETE_USER_SUCCESS:
                mFragmentManager.beginTransaction().replace(R.id.fl_bottom_nav_container, new SignInFragment(), SignInFragment.class.getName()).commit();
                break;
            case Constants.UserStatus.UPDATE_PASSWORD_SUCCESS:
                Snackbar.make(getBinding().getRoot(), getString(R.string.snack_re_sign_in), Snackbar.LENGTH_SHORT).show();
                mFragmentManager.beginTransaction().replace(R.id.fl_bottom_nav_container, new SignInFragment(), SignInFragment.class.getName()).commit();
                break;
            case Constants.UserStatus.UPDATE_PROFILE_SUCCESS:
                Snackbar.make(getBinding().getRoot(), R.string.snack_update_profile_success, Snackbar.LENGTH_SHORT).show();
                break;
            case Constants.UserStatus.UPDATE_NICK_NAME_SUCCESS:
                Snackbar.make(getBinding().getRoot(), getString(R.string.snack_change_nick_name_success), Snackbar.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void isWorking() {
        freezeUI();
    }

    @Override
    public void isFinished() {
        unFreezeUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createViewModels();
        mDisposable = new CompositeDisposable();
        mFragmentManager = getSupportFragmentManager();
        getBinding().bottomNav.setItemIconTintList(null);
        getBinding().bottomNav.setOnNavigationItemSelectedListener(v -> {
            onNavItemSelected(v);
            return true;
        });
        mFragmentManager.beginTransaction().add(R.id.fl_bottom_nav_container, new HomeFragment()).commit();
    }

    private void createViewModels() {
        createViewModel(UserViewModel.class, new UserViewModelFactory(getApplication(),
                AppDataManager.getInstance(AppAPIManager.getInstance(), AppUserDataManager.getInstance(getApplication())),
                this));
        createViewModel(RoomsViewModel.class, new RoomsViewModelFactory(getApplication(), RoomsRepository.getInstance(), this));
        createViewModel(SearchViewModel.class, new SearchViewModelFactory(getApplication(), RoomsRepository.getInstance(), this));
    }

    private void onNavItemSelected(MenuItem item) {
        mDisposable.add(Single.just(item)
                .map(MenuItem::getItemId)
                .subscribe(id -> {
                    switch (id) {
                        case R.id.action_home:
                            // FIXME FragmentManager로 fragment를 replace하는것보다는 ViewPager를 활용해서 index를 변경하는 패턴으로 수정해주세요
                            // 이러한 방식으로 하는경우 버튼을 누를때마다 Fragment가 새로 만들어집니다.
                            /* handle here: replace fragment on home btn Clicked */
                            mFragmentManager.beginTransaction().replace(R.id.fl_bottom_nav_container, new HomeFragment()).commit();
                            break;
                        case R.id.action_map:
                            mFragmentManager.beginTransaction().replace(R.id.fl_bottom_nav_container, new MapFragment()).commit();
                            break;
                        case R.id.action_message:
                            /* handle here: replace fragment on message btn Clicked */
                            break;
                        case R.id.action_my_page:
                            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                                mFragmentManager.beginTransaction().replace(R.id.fl_bottom_nav_container, new SignInFragment(), SignInFragment.class.getName()).commit();
                                return;
                            }
                            mFragmentManager.beginTransaction().replace(R.id.fl_bottom_nav_container, new MyPageFragment(), MyPageFragment.class.getName()).commit();
                            break;
                    }
                }));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDisposable.dispose();
    }

    private void freezeUI() {
        getBinding().pgBottomNav.setVisibility(View.VISIBLE);
        getBinding().getRoot().setAlpha(0.6f);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void unFreezeUI() {
        getBinding().pgBottomNav.setVisibility(View.GONE);
        getBinding().getRoot().setAlpha(1.0f);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    @Override
    public void onClicked() {
        getBinding().bottomNav.setSelectedItemId(R.id.action_map);
    }

}
