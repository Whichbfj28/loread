package me.wizos.loread.viewmodel;

import android.app.Application;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.socks.library.KLog;

import me.wizos.loread.App;
import me.wizos.loread.Contract;
import me.wizos.loread.R;
import me.wizos.loread.activity.login.LoginFormState;
import me.wizos.loread.activity.login.LoginResult;
import me.wizos.loread.db.CoreDB;
import me.wizos.loread.db.User;
import me.wizos.loread.db.UserDao;
import me.wizos.loread.network.api.TinyRSSApi;
import me.wizos.loread.network.callback.CallbackX;

// LiveData通常结合ViewModel一起使用。我们知道ViewModel是用来存放数据的，因此我们可以将数据库放在ViewModel中进行实例化。
// 但数据库在实例化的时候需要Context，而ViewModel不能传入任何带有Context引用的对象，所以应该用它的子类AndroidViewModel，它可以接受Application作为参数，用于数据库的实例化。
public class TinyRSSUserViewModel extends AndroidViewModel {
    private UserDao userDao;
    // Creates a PagedList object with 50 items per page.
    public TinyRSSUserViewModel(@NonNull Application application) {
        super(application);
        this.userDao = CoreDB.i().userDao();
    }


    private MutableLiveData<LoginFormState> loginFormLiveData = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResultLiveData = new MutableLiveData<>();

    public LiveData<LoginFormState> getLoginFormLiveData() {
        return loginFormLiveData;
    }

    public LiveData<LoginResult> getLoginResult() {
        return loginResultLiveData;
    }

    public void login(String host, String username, String password) {
        TinyRSSApi tinyRSSApi = new TinyRSSApi(host);

        tinyRSSApi.login(username, password, new CallbackX<String,String>() {
            @Override
            public void onSuccess(String auth) {
                User user = new User();
                user.setSource(Contract.PROVIDER_TINYRSS);
                user.setId(Contract.PROVIDER_TINYRSS + "_" + username);
                user.setUserId(username);
                user.setUserName(username);
                user.setUserPassword(password);
                user.setAuth(auth);
                user.setExpiresTimestamp(0);
                user.setHost(host);
                tinyRSSApi.setAuthorization(auth);
                App.i().getKeyValue().putString(Contract.UID, user.getId());
                KLog.i("登录成功：" + user.getId());
                User userTmp = userDao.getById(user.getId());
                if (userTmp != null) {
                    CoreDB.i().userDao().update(user);
                }else {
                    CoreDB.i().userDao().insert(user);
                }

                LoginResult loginResult = new LoginResult().setSuccess(true).setData(auth);
                loginResultLiveData.postValue(loginResult);
            }

            @Override
            public void onFailure(String error) {
                LoginResult loginResult = new LoginResult().setSuccess(false).setData(App.i().getString(R.string.login_failed_reason, error));
                loginResultLiveData.postValue(loginResult);
            }
        });
    }

    public void loginDataChanged(String host, String username, String password) {
        LoginFormState loginFormState = new LoginFormState();

        if (!isHostValid(host)) {
            loginFormState.setHostHint(R.string.invalid_host);
        } else if (!isUserNameValid(username)) {
            loginFormState.setUsernameHint(R.string.invalid_username);
        } else if (!isPasswordValid(password)) {
            loginFormState.setPasswordHint(R.string.invalid_password);
        } else {
            loginFormState.setDataValid(true);
        }

        loginFormLiveData.setValue(loginFormState);
    }

    private boolean isHostValid(String host) {
        return !TextUtils.isEmpty(host) && Patterns.WEB_URL.matcher(host).matches();
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        return !TextUtils.isEmpty(username) && !username.trim().isEmpty();
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return !TextUtils.isEmpty(password) && password.trim().length() > 5;
    }
}
