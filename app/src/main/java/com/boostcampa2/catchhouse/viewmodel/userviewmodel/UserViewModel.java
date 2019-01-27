package com.boostcampa2.catchhouse.viewmodel.userviewmodel;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;

import com.boostcampa2.catchhouse.R;
import com.boostcampa2.catchhouse.data.userdata.UserRepository;
import com.boostcampa2.catchhouse.data.userdata.pojo.User;
import com.boostcampa2.catchhouse.viewmodel.ReactiveViewModel;
import com.boostcampa2.catchhouse.viewmodel.ViewModelListener;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import io.reactivex.schedulers.Schedulers;

public class UserViewModel extends ReactiveViewModel {

    private Application mAppContext;
    private UserRepository mRepository;
    private ViewModelListener mListener;
    private MutableLiveData<FirebaseUser> mFirebaseUser;
    private User mUser;
    public MutableLiveData<String> mEmail;
    public MutableLiveData<String> mPassword;
    private MutableLiveData<String> mNickName;
    private MutableLiveData<String> mGender;

    UserViewModel(Application application, UserRepository repository, ViewModelListener listener) {
        super();
        this.mAppContext = application;
        this.mRepository = repository;
        this.mListener = listener;
        this.mFirebaseUser = new MutableLiveData<>();
        this.mEmail = new MutableLiveData<>();
        this.mNickName = new MutableLiveData<>();
        this.mGender = new MutableLiveData<>();
    }

    public GoogleSignInClient requestGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(mAppContext.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(mAppContext, gso);
    }

    public void signUpFirebaseWithGoogle(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        GoogleSignInAccount account = task.getResult();
        mUser = new User(account.getDisplayName());
        if (account.getPhotoUrl() != null) {
            mUser.setProfile(account.getPhotoUrl().toString());
        }
        handlingSignUpData(GoogleAuthProvider.getCredential(account.getIdToken(), null));
    }

    public LoginManager requestFacebookSignIn(CallbackManager callbackManager) {
        LoginManager loginManager = LoginManager.getInstance();
        loginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                mListener.isWorking();
                getDetailDataFromFaceBook(loginResult);
            }

            @Override
            public void onCancel() {
                mListener.isFinished();
            }

            @Override
            public void onError(FacebookException error) {
                mListener.isFinished();
                mListener.onError(error);
            }
        });
        return loginManager;
    }

    private void getDetailDataFromFaceBook(LoginResult loginResult) {
        mListener.isWorking();
        getCompositeDisposable().add(mRepository.getDetailInfoFromRemote(loginResult.getAccessToken())
                .subscribeOn(Schedulers.io())
                .subscribe(info -> mUser = info));
        handlingSignUpData(FacebookAuthProvider.getCredential(loginResult.getAccessToken().getToken()));
    }

    private void handlingSignUpData(AuthCredential credential) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInWithCredential(credential).addOnSuccessListener(success -> {
            String uuid = auth.getCurrentUser().getUid();
            getCompositeDisposable().add(mRepository.setUserToRemote(uuid, mUser)
                    .subscribe(() -> {
                        mListener.isFinished();
                        mFirebaseUser.setValue(auth.getCurrentUser());
                    }, error -> mListener.onError(error)));
        })
                .addOnFailureListener(error -> {
                    mListener.isFinished();
                    mListener.onError(error);
                });
    }

    public LiveData<FirebaseUser> getUserInfo() {
        return mFirebaseUser;
    }
}
