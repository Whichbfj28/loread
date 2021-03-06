package me.wizos.loread.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.ToastUtils;
import com.jeremyliao.liveeventbus.LiveEventBus;
import com.kyleduo.switchbutton.SwitchButton;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.enums.PopupAnimation;
import com.lxj.xpopup.interfaces.OnSelectListener;
import com.socks.library.KLog;
import com.yanzhenjie.recyclerview.OnItemClickListener;
import com.yanzhenjie.recyclerview.OnItemLongClickListener;
import com.yanzhenjie.recyclerview.OnItemMenuClickListener;
import com.yanzhenjie.recyclerview.OnItemSwipeListener;
import com.yanzhenjie.recyclerview.SwipeMenu;
import com.yanzhenjie.recyclerview.SwipeMenuBridge;
import com.yanzhenjie.recyclerview.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.SwipeMenuItem;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.wizos.loread.App;
import me.wizos.loread.R;
import me.wizos.loread.adapter.ArticlePagedListAdapter;
import me.wizos.loread.adapter.ExpandedAdapter;
import me.wizos.loread.db.Article;
import me.wizos.loread.db.Collection;
import me.wizos.loread.db.CoreDB;
import me.wizos.loread.db.User;
import me.wizos.loread.network.SyncWorker;
import me.wizos.loread.network.callback.CallbackX;
import me.wizos.loread.utils.SnackbarUtil;
import me.wizos.loread.utils.TimeUtil;
import me.wizos.loread.view.IconFontView;
import me.wizos.loread.view.SwipeRefreshLayoutS;
import me.wizos.loread.view.colorful.Colorful;
import me.wizos.loread.view.colorful.setter.ViewGroupSetter;
import me.wizos.loread.viewmodel.ArticleViewModel;


/**
 * @author Wizos on 2016‎年5‎月23‎日
 */
public class MainActivity extends BaseActivity implements SwipeRefreshLayoutS.OnRefreshListener {
    private static final String TAG = "MainActivity";
    private IconFontView vPlaceHolder;
    private ImageView vToolbarAutoMark;
    private Toolbar toolbar;
    private SwipeRefreshLayoutS swipeRefreshLayoutS;
    private SwipeRecyclerView articleListView;
    // private MultiTypeAdapter articlesAdapter;
    private ArticlePagedListAdapter articlesAdapter;
    private IconFontView refreshIcon;

    // 方案3
    private SwipeRecyclerView tagListView;
    private ExpandedAdapter tagListAdapter;

    private TextView countTips;

    private Integer[] scrollIndex;
    private View articlesHeaderView;

    private BottomSheetDialog quickSettingDialog;
    private BottomSheetDialog tagBottomSheetDialog;
    private RelativeLayout relativeLayout;
    //private StickyHeaderLayout stickyHeaderLayout;
    private boolean autoMarkReaded = false;
    private static Handler maHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initToolbar();
        initIconView();
        initArtListView();
        initTagListView();
        initSwipeRefreshLayout(); // 必须要放在 initArtListView() 之后，不然无论 ListView 滚动到第几页，一下拉就会触发刷新
        showAutoSwitchThemeSnackBar();
        applyPermissions();
        super.onCreate(savedInstanceState);// 由于使用了自动换主题，所以要放在这里
        getArtData();  // 获取文章列表数据为 App.articleList
        autoMarkReaded = App.i().getUser().isMarkReadOnScroll();
        initWorkRequest();
    }

    private void initWorkRequest(){
//        Constraints.Builder builder = new Constraints.Builder();
//        if(App.i().getUser().isAutoSync()){
//            if( App.i().getUser().isAutoSyncOnlyWifi() ){
//                builder.setRequiredNetworkType(NetworkType.UNMETERED);
//            }else {
//                builder.setRequiredNetworkType(NetworkType.CONNECTED);
//            }
//            PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, App.i().getUser().getAutoSyncFrequency(), TimeUnit.MINUTES)
//                    .setConstraints(builder.build())
//                    .addTag(SyncWorker.TAG)
//                    .build();
//            WorkManager.getInstance(this).enqueueUniquePeriodicWork(SyncWorker.TAG, ExistingPeriodicWorkPolicy.KEEP,syncRequest);
//            KLog.i("SyncWorker Id: " + syncRequest.getId());
//        }

        Constraints.Builder builder = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED);
        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, App.i().getUser().getAutoSyncFrequency(), TimeUnit.MINUTES)
                .setConstraints(builder.build())
                .addTag(SyncWorker.TAG)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(SyncWorker.TAG, ExistingPeriodicWorkPolicy.KEEP,syncRequest);
        KLog.i("SyncWorker Id: " + syncRequest.getId());

        if(App.i().getUser().isAutoSync()){
        }

        LiveEventBus.get(SyncWorker.SYNC_TASK_STATUS,Boolean.class)
                .observeSticky(this, new Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean isSyncing) {
                        KLog.e("任务状态："  + isSyncing );
                        swipeRefreshLayoutS.setRefreshing(false);
                        if(isSyncing){
                            swipeRefreshLayoutS.setEnabled(false);
                        }else {
                            swipeRefreshLayoutS.setEnabled(true);
                        }
                    }
                });
        LiveEventBus
                .get(SyncWorker.SYNC_PROCESS_FOR_SUBTITLE, String.class)
                .observe(this, new Observer<String>() {
                    @Override
                    public void onChanged(@Nullable String tips) {
                        toolbar.setSubtitle( tips );
                    }
                });
        LiveEventBus.get(SyncWorker.NEW_ARTICLE_NUMBER,Integer.class)
                .observe(this, new Observer<Integer>() {
                    @Override
                    public void onChanged(Integer integer) {
                        if(integer == 0){
                            return;
                        }
                        SnackbarUtil.Long(articleListView,bottomBar, getResources().getQuantityString(R.plurals.has_new_articles,integer,integer) )
                                .setAction(getString(R.string.view), new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        refreshData();
                                    }
                                }).show();
                        refreshIcon.setVisibility(View.VISIBLE);
                    }
                });
    }


    private void applyPermissions() {
        XXPermissions.with(this)
                //.constantRequest() //可设置被拒绝后继续申请，直到用户授权或者永久拒绝
                //.permission(Permission.SYSTEM_ALERT_WINDOW) //支持请求6.0悬浮窗权限8.0请求安装权限
                .permission(Permission.Group.STORAGE) //不指定权限则自动获取清单中的危险权限
                .request(new OnPermission() {
                    @Override
                    public void hasPermission(List<String> granted, boolean isAll) {
                    }

                    @Override
                    public void noPermission(List<String> denied, boolean quick) {
                        for (String id : denied) {
                            KLog.e("无法获取权限" + id);
                        }
                        ToastUtils.show(getString(R.string.plz_grant_permission_tips));
                    }
                });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (articlesAdapter != null) {
            articlesAdapter.notifyDataSetChanged();
        }
    }

    private void showAutoSwitchThemeSnackBar() {
        if (!App.i().getUser().isAutoToggleTheme()) {
            return;
        }
        int hour = TimeUtil.getCurrentHour();
        int themeMode;
        if (hour >= 7 && hour < 20) {
            themeMode = App.THEME_DAY;
        } else {
            themeMode = App.THEME_NIGHT;
        }
        if (App.i().getUser().getThemeMode() == themeMode) {
            return;
        }

        SnackbarUtil.Long(articleListView, bottomBar, getString(R.string.theme_switched_automatically))
                .setAction(getString(R.string.cancel), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        manualToggleTheme();
                    }
                }).show();
    }


    protected void initIconView() {
        vToolbarAutoMark = findViewById(R.id.main_toolbar_auto_mark);
        if (App.i().getUser().isMarkReadOnScroll()) {
            vToolbarAutoMark.setVisibility(View.VISIBLE);
        }
        vPlaceHolder = findViewById(R.id.main_placeholder);
        refreshIcon = findViewById(R.id.main_bottombar_refresh_articles);
    }

    public void clickSearchIcon(View view) {
        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
        startActivityForResult(intent, 0);
        overridePendingTransition(R.anim.in_from_bottom, R.anim.fade_out);
    }

    protected void initSwipeRefreshLayout() {
        swipeRefreshLayoutS = findViewById(R.id.main_swipe_refresh);
        if (swipeRefreshLayoutS == null) {
            return;
        }
        swipeRefreshLayoutS.setOnRefreshListener(this);
        //设置样式刷新显示的位置
        swipeRefreshLayoutS.setProgressViewOffset(true, 0, 120);
        swipeRefreshLayoutS.setViewGroup(articleListView);
    }

    @Override
    public void onRefresh() {
        if (!swipeRefreshLayoutS.isEnabled()) {
            return;
        }
        KLog.i("【刷新中】");
        Constraints.Builder builder = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED);
        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(builder.build())
                .addTag(SyncWorker.TAG)
                .build();
        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest);
    }

    // 按下back键时会调用onDestroy()销毁当前的activity，重新启动此activity时会调用onCreate()重建；
    // 而按下home键时会调用onStop()方法，并不销毁activity，重新启动时则是调用onResume()
    @Override
    protected void onDestroy() {
        // 参数为null，会将所有的Callbacks和Messages全部清除掉。
        // 这样做的好处是在Activity退出的时候，可以避免内存泄露。因为 handler 内可能引用 Activity
        maHandler.removeCallbacksAndMessages(null);
        //EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    /**
     * App.StreamState 包含 3 个状态：All，Unread，Stared
     * App.streamId 至少包含 1 个状态： Reading-list
     */
    protected void refreshData() { // 获取 App.articleList , 并且根据 App.articleList 的到未读数目
        //KLog.e("refreshData：" + App.i().getUser().getStreamId() + " = " + App.i().getUser().getStreamStatus() + "   " + App.i().getUser().getUserId());
        getArtData();
        refreshIcon.setVisibility(View.GONE);
    }


    private ArticleViewModel articleViewModel;
    private void getArtData() {
        String uid = App.i().getUser().getId();
        int streamStatus = App.i().getUser().getStreamStatus();
        int streamType = App.i().getUser().getStreamType();
        String streamId = App.i().getUser().getStreamId();
        articlesAdapter.setLastPos(linearLayoutManager.findLastVisibleItemPosition()-1);

        if(articleViewModel.articles != null && articleViewModel.articles.hasObservers()){
            articleViewModel.articles.removeObservers(this);
            articleViewModel.articles = null;
        }
        articleViewModel.getArticles(uid,streamId,streamType,streamStatus).observe(this, new Observer<PagedList<Article>>() {
            @Override
            public void onChanged(PagedList<Article> articles) {
//                if( articlesAdapter.getCurrentList() != null ){
//                    KLog.e("更新列表数据 A : " + articlesAdapter.getCurrentList().getLastKey()  +  " , " + (linearLayoutManager.findLastVisibleItemPosition()-1) );
//                }
//                if( articlesAdapter.getCurrentList() != null ){
//                    KLog.e("更新列表数据 B : " + articlesAdapter.getCurrentList().getLastKey()  +  " , " + (linearLayoutManager.findLastVisibleItemPosition()-1) );
//                }
                articlesAdapter.submitList(articles);
//                if( articlesAdapter.getCurrentList() != null ){
//                    KLog.e("更新列表数据 C : " + articlesAdapter.getCurrentList().getLastKey()  +  " , " + (linearLayoutManager.findLastVisibleItemPosition()-1) );
//                }
                loadViewByData( articles.size() );
            }
        });
        articleListView.scrollToPosition(0);
        // KLog.e("更新列表数据 A  , " + articlesAdapter.getItemCount() );
    }

    private void loadViewByData(int size) {
        // 在setSupportActionBar(toolbar)之后调用toolbar.setTitle()的话。 在onCreate()中调用无效。在onStart()中调用无效。 在onResume()中调用有效。
        getSupportActionBar().setTitle(App.i().getUser().getStreamTitle());
        countTips.setText( getResources().getQuantityString(R.plurals.articles_count, size, size ) );

//        KLog.i("【loadViewByData】" + App.i().getUser().getStreamId()+ "--" + App.i().getUser().getStreamTitle() + "--" + App.i().getUser().getStreamStatus() + "--" + toolbar.getTitle() + articlesAdapter.getItemCount());
        if (articlesAdapter == null || articlesAdapter.getItemCount() == 0) {
            vPlaceHolder.setVisibility(View.VISIBLE);
            articleListView.setVisibility(View.GONE);
        } else {
            vPlaceHolder.setVisibility(View.GONE);
            articleListView.setVisibility(View.VISIBLE);
        }
    }

    public void showTagDialog(final Collection category) {
        // 重命名弹窗的适配器
        MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(new MaterialSimpleListAdapter.Callback() {
            @Override
            public void onMaterialListItemSelected(MaterialDialog dialog, int index, MaterialSimpleListItem item) {
                if (index == 0) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title(R.string.edit_name)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .inputRange(1, 22)
                            .input(null, category.getTitle(), new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NotNull MaterialDialog dialog, CharSequence input) {
                                    renameTag(input.toString(), category);
                                }
                            })
                            .positiveText(R.string.confirm)
                            .negativeText(android.R.string.cancel)
                            .show();
                }
                dialog.dismiss();
            }
        });
        adapter.add(new com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem.Builder(MainActivity.this)
                .content(R.string.rename)
                .icon(R.drawable.ic_rename)
                .backgroundColor(Color.TRANSPARENT)
                .build());

        new MaterialDialog.Builder(MainActivity.this)
                .adapter(adapter, new LinearLayoutManager(MainActivity.this))
                .show();
    }

    public void renameTag(final String renamedTagTitle, Collection category) {
        KLog.i("renameTag", renamedTagTitle);
        if (renamedTagTitle.equals("") || category.getTitle().equals(renamedTagTitle)) {
            return;
        }
        final String newCategoryId = "user/" + App.i().getUser().getUserId() + "/" + renamedTagTitle;
        final String oldCategoryId = category.getId();
        App.i().getApi().renameTag(oldCategoryId, renamedTagTitle, new CallbackX<String,String>() {
            @Override
            public void onSuccess(String result) {
                CoreDB.i().categoryDao().updateId(App.i().getUser().getId(),oldCategoryId,newCategoryId);
                CoreDB.i().feedCategoryDao().updateCategoryId(App.i().getUser().getId(),oldCategoryId,newCategoryId);
                tagListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String error) {
                ToastUtils.show(getString(R.string.rename_failed));
            }
        });
    }

    public void showFeedActivity(final int parentPosition, final int childPosition) {
        Collection feed = tagListAdapter.getChild(parentPosition, childPosition);
        if (feed == null) {
            return;
        }
        Intent intent = new Intent(MainActivity.this, FeedActivity.class);
        intent.putExtra("feedId", feed.getId());
        startActivity(intent);
    }


    LinearLayoutManager linearLayoutManager;
    public void initArtListView() {
        articleListView = findViewById(R.id.main_slv);
        linearLayoutManager = new LinearLayoutManager(this);
        articleListView.setLayoutManager(linearLayoutManager);

        // HeaderView。
        articlesHeaderView = getLayoutInflater().inflate(R.layout.main_item_header, articleListView, false);
        countTips = (TextView) articlesHeaderView.findViewById(R.id.main_header_title);
        ImageView eye = articlesHeaderView.findViewById(R.id.main_header_eye);
        eye.setOnClickListener(v -> ToastUtils.show(R.string.display_filter_in_development) );
        articleListView.addHeaderView(articlesHeaderView);

        articleListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, final int position) {
                new XPopup.Builder(MainActivity.this)
                        .isCenterHorizontal(true) //是否与目标水平居中对齐
                        .offsetY(-view.getHeight() / 2)
                        .hasShadowBg(true)
                        .popupAnimation(PopupAnimation.ScaleAlphaFromCenter)
                        .atView(view)  // 依附于所点击的View，内部会自动判断在上方或者下方显示
                        .asAttachList(
                                new String[]{getString(R.string.speak_article), getString(R.string.mark_up), getString(R.string.mark_down), getString(R.string.mark_unread)},
                                new int[]{R.drawable.ic_volume, R.drawable.ic_mark_up, R.drawable.ic_mark_down, R.drawable.ic_mark_unread},
                                new OnSelectListener() {
                                    @Override
                                    public void onSelect(int which, String text) {
                                        switch (which) {
                                            case 0:
                                                Intent intent = new Intent(MainActivity.this,TTSActivity.class);
                                                intent.putExtra("articleNo",position);
                                                intent.putExtra("isQueue",true);
                                                startActivity(intent);
                                                break;
                                            case 1:
                                                Integer[] index = new Integer[2];
                                                index[0] = position + 1;
                                                index[1] = 0;
                                                new MarkListReadedAsyncTask().execute(index);
                                                break;
                                            case 2:
                                                showConfirmDialog(position,articlesAdapter.getItemCount());
                                                break;
                                            case 3:
                                                Article article = articlesAdapter.getItem(position);
//                                                Article article = CoreDB.i().articleDao().getById(App.i().getUser().getId(),articlesAdapter.getItem(position).getId());
                                                if( article == null ){
                                                    return;
                                                }

                                                if (article.getReadStatus() == App.STATUS_READED) {
                                                    int oldReadStatus = article.getReadStatus();
                                                    App.i().getApi().markArticleUnread(article.getId(), new CallbackX() {
                                                        @Override
                                                        public void onSuccess(Object result) {
                                                        }
                                                        @Override
                                                        public void onFailure(Object error) {
                                                            article.setReadStatus(oldReadStatus);
                                                            CoreDB.i().articleDao().update(article);
                                                        }
                                                    });
                                                }
                                                article.setReadStatus(App.STATUS_UNREADING);
                                                CoreDB.i().articleDao().update(article);
                                                articlesAdapter.notifyItemChanged(position);
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                })
                        .show();
            }
        });

        articleListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            // 正在被外部拖拽,一般为用户正在用手指滚动 SCROLL_STATE_DRAGGING，自动滚动 SCROLL_STATE_SETTLING，正在滚动（SCROLL_STATE_IDLE）
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                articlesAdapter.setLastPos(linearLayoutManager.findLastVisibleItemPosition()-1);
                //KLog.i("【滚动】" + ((RecyclerView.LayoutParams) recyclerView.getChildAt(1).getLayoutParams()).getViewAdapterPosition() + " = "+  linearLayoutManager.findFirstVisibleItemPosition() + " , " + linearLayoutManager.findLastVisibleItemPosition());
                if (!autoMarkReaded) {
                    return;
                }
                //  || RecyclerView.SCROLL_STATE_SETTLING == newState
                //KLog.i("滚动：" + newState);
                if (RecyclerView.SCROLL_STATE_DRAGGING == newState && scrollIndex == null) {
                    scrollIndex = new Integer[2];
                    scrollIndex[0] = ((RecyclerView.LayoutParams) recyclerView.getChildAt(0).getLayoutParams()).getViewAdapterPosition();
                    KLog.i("滚动开始：" + scrollIndex[0] + " = "+  linearLayoutManager.findFirstVisibleItemPosition() );
                } else if (RecyclerView.SCROLL_STATE_IDLE == newState && scrollIndex != null) {
                    scrollIndex[1] = ((RecyclerView.LayoutParams) recyclerView.getChildAt(0).getLayoutParams()).getViewAdapterPosition();
                    new MarkListReadedAsyncTask().execute(scrollIndex);
                    KLog.i("滚动结束：" + scrollIndex[1]  + " = "+  linearLayoutManager.findFirstVisibleItemPosition() );
                    scrollIndex = null;
                }
            }
        });

        // 创建菜单：

        SwipeMenuCreator mSwipeMenuCreator = new SwipeMenuCreator() {
            @Override
            public void onCreateMenu(SwipeMenu leftMenu, SwipeMenu rightMenu, int position) {
                Article article = articlesAdapter.getItem(position);
//                Article article = CoreDB.i().articleDao().getById(App.i().getUser().getId(),articlesAdapter.getArticleId(position));
                if(article==null){
                    //KLog.i("文章数据为null: " + position  + " , " +  articlesAdapter.getCurrentList().getLastKey() + " , " + articlesAdapter.getCurrentList().getLoadedCount()  );
                    return;
                }
//                else {
//                    KLog.i("文章数据为: " + position + " , " + articlesAdapter.getCurrentList().getLoadedCount() + " , " +  articlesAdapter.getCurrentList().getLastKey() );
//                }

                int width = getResources().getDimensionPixelSize(R.dimen.dp_80);
                int margin = getResources().getDimensionPixelSize(R.dimen.dp_30);

//                KLog.e("添加左右菜单" + position );
                // 1. MATCH_PARENT 自适应高度，保持和Item一样高;  2. 指定具体的高，比如80; 3. WRAP_CONTENT，自身高度，不推荐;
                int height = ViewGroup.LayoutParams.MATCH_PARENT;

                SwipeMenuItem starItem = new SwipeMenuItem(MainActivity.this); // 各种文字和图标属性设置。
                if (article.getStarStatus() == App.STATUS_STARED) {
                    starItem.setImage(R.drawable.ic_state_unstar);
                } else {
                    starItem.setImage(R.drawable.ic_state_star);
                }
                starItem.setWeight(width);
                starItem.setHeight(height);
                starItem.setMargins(margin, 0, margin, 0);
                leftMenu.addMenuItem(starItem); // 在Item左侧添加一个菜单。

                SwipeMenuItem readItem = new SwipeMenuItem(MainActivity.this); // 各种文字和图标属性设置。
                if (article.getReadStatus() == App.STATUS_READED) {
                    readItem.setImage(R.drawable.ic_state_unread);
                } else {
                    readItem.setImage(R.drawable.ic_read);
                }
                readItem.setWeight(width);
                readItem.setHeight(height);
                readItem.setMargins(margin, 0, margin, 0);
                rightMenu.addMenuItem(readItem); // 在Item右侧添加一个菜单。
                // 注意：哪边不想要菜单，那么不要添加即可。
            }
        };
        articleListView.setSwipeMenuCreator(mSwipeMenuCreator);

        articleListView.setOnItemSwipeListener(new OnItemSwipeListener() {
            @Override
            public void onClose(View swipeMenu, int direction, int adapterPosition) {
            }

            @Override
            public void onCloseLeft(int position) {
                KLog.i("onCloseLeft：" + position + "  ");
                toggleStarState(position);
            }

            @Override
            public void onCloseRight(int position) {
                KLog.i("onCloseRight：" + position + "  ");
                toggleReadState(position);
            }
        });

        OnItemMenuClickListener mItemMenuClickListener = new OnItemMenuClickListener() {
            @Override
            public void onItemClick(SwipeMenuBridge menuBridge, int position) {
                // 任何操作必须先关闭菜单，否则可能出现Item菜单打开状态错乱。
                menuBridge.closeMenu();

                // 左侧还是右侧菜单：
                int direction = menuBridge.getDirection();

                if (direction == SwipeRecyclerView.RIGHT_DIRECTION) {
                    KLog.i("onItemClick  onCloseRight：" + position + "  ");
                    if (position > -1) {
                        toggleReadState(position);
                    }
                } else if (direction == SwipeRecyclerView.LEFT_DIRECTION) {
                    KLog.i("onItemClick  onCloseLeft：" + position + "  ");
                    if (position > -1) {
                        toggleStarState(position);
                    }
                }
            }
        };
        // 菜单点击监听。
        articleListView.setOnItemMenuClickListener(mItemMenuClickListener);

        articleListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (position < 0) {
                    return;
                }
                Intent intent = new Intent(MainActivity.this, ArticleActivity.class);
                intent.putExtra("theme", App.i().getUser().getThemeMode());

                String articleId = articlesAdapter.getItem(position).getId();
                //String articleId = articlesAdapter.getArticleId(position);

                intent.putExtra("articleId", articleId);
                // 下标从 0 开始
                intent.putExtra("articleNo", position);
                intent.putExtra("articleCount", articlesAdapter.getItemCount());
                startActivityForResult(intent, 0);
                overridePendingTransition(R.anim.in_from_bottom, R.anim.fade_out);

                //KLog.i("点击了" + articleID + "，位置：" + position + "，文章ID：" + articleID + "    " + App.articleList.size());
            }
        });
        articleViewModel = new ViewModelProvider(this).get(ArticleViewModel.class);
        articlesAdapter = new ArticlePagedListAdapter();
        articleListView.setAdapter(articlesAdapter);
        App.i().articlesAdapter = articlesAdapter;
    }


//    private CategoryViewModel categoryViewModel;
//    public void onTagIconClicked(View view) {
//        KLog.i("tag按钮被点击");
//        tagBottomSheetDialog.show();
//        if(categoryViewModel == null){
//            categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
//        }
//        categoryViewModel.getCategoriesLiveData().observe(this, new Observer<List<Category>>() {
//            @Override
//            public void onChanged(List<Category> categories) {
//                String userId = App.i().getUser().getUserId();
//                // 总分类
//                Category rootCategory = new Category();
//                rootCategory.setTitle(getString(R.string.all));
//                int unreadCount = CoreDB.i().articleDao().getUnreadCount(App.i().getUser().getId());
//                rootCategory.setUnreadCount(unreadCount);
//                rootCategory.setId("user/" + userId + App.CATEGORY_ALL);
//
//                // 未分类
//                Category unCategory = new Category();
//                unCategory.setTitle(getString(R.string.un_category));
//                int unreadUnCategoryCount = CoreDB.i().articleDao().getUnreadUncategoryCount(App.i().getUser().getId());
//                unCategory.setUnreadCount(unreadUnCategoryCount);
//                unCategory.setId("user/" + userId + App.CATEGORY_UNCATEGORIZED);
//
//                categories.add(0,rootCategory);
//                categories.add(1,unCategory);
//                tagListAdapter.setParents(categories);
//                tagListAdapter.notifyDataChanged();
////                CategoryDiffCallback callback = new CategoryDiffCallback(tagListAdapter.getCategories(), categories);
////                //对比数据
////                DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback);
////                //然后刷新，完事儿
////                result.dispatchUpdatesTo(tagListAdapter);
//            }
//        });
//    }

    public void onClickCategoryIcon(View view) {
        tagBottomSheetDialog.show();
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                String uid = App.i().getUser().getUserId();

                // 总分类
                Collection rootCategory = new Collection();
                rootCategory.setTitle(getString(R.string.all));
                int unreadCount = CoreDB.i().articleDao().getUnreadCount(App.i().getUser().getId());
                rootCategory.setId("user/" + uid + App.CATEGORY_ALL);

                // 未分类
                Collection unCategory = new Collection();
                unCategory.setTitle(getString(R.string.un_category));
                int unreadUnCategoryCount = CoreDB.i().articleDao().getUncategoryUnreadCount(App.i().getUser().getId());
                unCategory.setId("user/" + uid + App.CATEGORY_UNCATEGORIZED);

                // 已分类
                List<Collection> categories;

                if( App.i().getUser().getStreamStatus() == App.STATUS_UNREAD ){
                    rootCategory.setCount(CoreDB.i().articleDao().getUnreadCount(App.i().getUser().getId()));
                    unCategory.setCount(CoreDB.i().articleDao().getUncategoryUnreadCount(App.i().getUser().getId()));
                    categories = CoreDB.i().categoryDao().getCategoriesUnreadCount(App.i().getUser().getId());
                }else if( App.i().getUser().getStreamStatus() == App.STATUS_STARED ){
                    rootCategory.setCount(CoreDB.i().articleDao().getStarCount(App.i().getUser().getId()));
                    unCategory.setCount(CoreDB.i().articleDao().getUncategoryStarCount(App.i().getUser().getId()));
                    categories = CoreDB.i().categoryDao().getCategoriesStarCount(App.i().getUser().getId());
                }else {
                    rootCategory.setCount(CoreDB.i().articleDao().getAllCount(App.i().getUser().getId()));
                    unCategory.setCount(CoreDB.i().articleDao().getUncategoryAllCount(App.i().getUser().getId()));
                    categories = CoreDB.i().categoryDao().getCategoriesAllCount(App.i().getUser().getId());
                }

                List<Collection> categoryListTemp = new ArrayList<>();
                categoryListTemp.add(rootCategory);
                categoryListTemp.add(unCategory);
                // 数据库中的所有分类
                categoryListTemp.addAll(categories);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tagListAdapter.setParents(categoryListTemp);
                        tagListAdapter.notifyDataChanged();
                        KLog.i("tag按钮被点击");
                    }
                });
            }
        });
    }

//    public void onClickCategoryIcon2(View view) {
//        tagBottomSheetDialog.show();
//        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
//            @Override
//            public void run() {
//                String uid = App.i().getUser().getUserId();
//
//                // 总分类
//                Category rootCategory = new Category();
//                rootCategory.setTitle(getString(R.string.all));
//                int unreadCount = CoreDB.i().articleDao().getUnreadCount(App.i().getUser().getId());
//                rootCategory.setUnreadCount(unreadCount);
//                rootCategory.setId("user/" + uid + App.CATEGORY_ALL);
//
//                // 未分类
//                Category unCategory = new Category();
//                unCategory.setTitle(getString(R.string.un_category));
//                int unreadUnCategoryCount = CoreDB.i().articleDao().getUnreadUncategoryCount(App.i().getUser().getId());
//                unCategory.setUnreadCount(unreadUnCategoryCount);
//                unCategory.setId("user/" + uid + App.CATEGORY_UNCATEGORIZED);
//
//                List<Category> categoryListTemp = new ArrayList<>();
//                categoryListTemp.add(rootCategory);
//                categoryListTemp.add(unCategory);
////                categoryListTemp.add(tagCategory);
//
//                List<Category> categories;
//                categories = CoreDB.i().categoryDao().getAll(App.i().getUser().getId());
//
//                // 数据库中的所有分类
//                categoryListTemp.addAll(categories);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        tagListAdapter.setParents(categoryListTemp);
//                        tagListAdapter.notifyDataChanged();
//                        KLog.i("tag按钮被点击");
//                    }
//                });
//            }
//        });
//    }

    public void initTagListView() {
        tagBottomSheetDialog = new BottomSheetDialog(MainActivity.this);
        tagBottomSheetDialog.setContentView(R.layout.bottom_sheet_category);
        relativeLayout = tagBottomSheetDialog.findViewById(R.id.sheet_tag);

        IconFontView iconFontView = tagBottomSheetDialog.findViewById(R.id.main_tag_close);
        iconFontView.setOnClickListener(view -> {
            tagBottomSheetDialog.dismiss();
            // iconFontView.setVisibility(View.GONE);
        });

        tagListView = tagBottomSheetDialog.findViewById(R.id.main_tag_list_view);

        tagListView.setLayoutManager(new LinearLayoutManager(this));
        // 还有另外一种方案，通过设置动画执行时间为0来解决问题：
        tagListView.getItemAnimator().setChangeDuration(0);
        // 关闭默认的动画
        ((SimpleItemAnimator) tagListView.getItemAnimator()).setSupportsChangeAnimations(false);

        //stickyHeaderLayout = tagBottomSheetDialog.findViewById(R.id.sticky_header_layout);
        //headerPinnedView = getLayoutInflater().inflate(R.layout.tag_expandable_item_group_header, stickyHeaderLayout, false);
        //stickyHeaderLayout.setStickyHeaderView(headerPinnedView);

        tagListAdapter = new ExpandedAdapter(this);
        tagListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int adapterPosition) {
                tagBottomSheetDialog.dismiss();
                // 根据原position判断该item是否是parent item
                int groupPosition = tagListAdapter.parentItemPosition(adapterPosition);
                User user = App.i().getUser();
                if (tagListAdapter.isParentItem(adapterPosition)) {
                    App.i().getUser().setStreamId( tagListAdapter.getGroup(groupPosition).getId().replace("\"", "")  );
                    App.i().getUser().setStreamTitle( tagListAdapter.getGroup(groupPosition).getTitle() );
                    App.i().getUser().setStreamType( App.TYPE_GROUP );
                    //KLog.i("【 TagList 被点击】" + App.i().getUser().toString());
                    refreshData();
                } else {
                    int childPosition = tagListAdapter.childItemPosition(adapterPosition);
                    Collection feed = tagListAdapter.getChild(groupPosition, childPosition);
                    App.i().getUser().setStreamId( feed.getId() );
                    App.i().getUser().setStreamTitle( feed.getTitle() );
                    App.i().getUser().setStreamType( App.TYPE_FEED );
                    refreshData();
                }
                CoreDB.i().userDao().update(user);
            }
        });

        tagListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, int adapterPosition) {
                // KLog.e("被长安，view的id是" + allArticleHeaderView.getId() + "，parent的id" + parent.getId() + "，Tag是" + allArticleHeaderView.getCategoryById() + "，位置是" + tagListView.getPositionForView(allArticleHeaderView));
                // 根据原position判断该item是否是parent item
                if (tagListAdapter.isParentItem(adapterPosition)) {
                    int parentPosition = tagListAdapter.parentItemPosition(adapterPosition);
                    showTagDialog(tagListAdapter.getGroup(parentPosition));
                } else {
                    // 换取child position
                    int parentPosition = tagListAdapter.parentItemPosition(adapterPosition);
                    int childPosition = tagListAdapter.childItemPosition(adapterPosition);
                    showFeedActivity(parentPosition, childPosition);
                }
            }
        });
        tagListView.setAdapter(tagListAdapter);
    }

    private void showConfirmDialog(final int start, final int end) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(R.string.main_dialog_confirm_mark_article_list)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    Integer[] index = new Integer[2];
                    index[0] = start;
                    index[1] = end;
                    new MarkListReadedAsyncTask().execute(index);
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }


    // 标记以上/以下为已读
    @SuppressLint("StaticFieldLeak")
    private class MarkListReadedAsyncTask extends AsyncTask<Integer, Integer, Integer> {
        private List<Article> articleList;
        private List<String> articleIDs;
        private void handleArticle(int i){
            try {
                int retry = 0;
                Article article = articlesAdapter.getItem(i);
                if( article == null ){
                    articlesAdapter.load(i);
                    do {
                        //KLog.i("文章为空：" + i );
                        Thread.sleep(500);
                        retry ++;
                        article = articlesAdapter.getItem(i);
                    }while ( article == null && retry < 3 );
                    //KLog.e("文章是否为空：" + (article==null)  + "   ,  " + (articlesAdapter.getItem(i)==null) );
                    if( article == null ){ return; }
                }

                articlesAdapter.setLastItem(linearLayoutManager.findLastVisibleItemPosition()-1);
                if (article.getReadStatus() == App.STATUS_UNREAD) {
                    article.setReadStatus(App.STATUS_READED);
                    articleList.add(article);
                    articleIDs.add(article.getId());
                    //提交之后，会执行onProcessUpdate方法，通知对应这个item更新界面
                    publishProgress(i);
                }
            } catch (IllegalStateException | InterruptedException e) {
                KLog.e("获取数据错误");
                e.printStackTrace();
            }
        }
        @Override
        protected Integer doInBackground(Integer... params) {
            int startIndex, endIndex;
            boolean desc;
            desc = params[0] >= params[1];
            startIndex = params[0];
            endIndex = params[1];

            if( desc ){
                articleList = new ArrayList<>(startIndex - endIndex);
                articleIDs = new ArrayList<>(startIndex - endIndex);
                for (int i = startIndex - 1; i >= endIndex; i--){
                    handleArticle(i);
                }
            }else {
                articleList = new ArrayList<>(endIndex - startIndex);
                articleIDs = new ArrayList<>(endIndex - startIndex);
                for (int i = startIndex; i < endIndex; i++){
                    handleArticle(i);
                }
            }

            if (articleIDs.size() == 0) {
                return 0;
            }
            if (desc) {
                Collections.reverse(articleList);
                Collections.reverse(articleIDs);
            }

            int needCount = articleIDs.size();
            int hadCount = 0;
            int num = 0;

            while (needCount > 0) {
                num = Math.min(100, needCount);
                List<Article> subArticles = articleList.subList(hadCount, hadCount + num);
                List<String> subArticleIDs = articleIDs.subList(hadCount, hadCount + num);
                hadCount = hadCount + num;
                CoreDB.i().articleDao().update(subArticles);
                App.i().getApi().markArticleListReaded(subArticleIDs, new CallbackX() {
                    @Override
                    public void onSuccess(Object result) {
                    }

                    @Override
                    public void onFailure(Object error) {
                        for (Article article: subArticles) {
                            article.setReadStatus(App.STATUS_UNREAD);
                        }
                        CoreDB.i().articleDao().update(subArticles);
                    }
                });

                needCount = articleIDs.size() - hadCount;
            }

            //返回结果
            return 0;
        }

//        /**
//         * 在调用cancel方法后会执行到这里
//         */
//        @Override
//        protected void onCancelled() {
//        }
//
//        /**
//         * 在doInbackground之后执行
//         */
//        @Override
//        protected void onPostExecute(Integer args3) {
//        }
//
//        /**
//         * 在doInBackground之前执行
//         */
//        @Override
//        protected void onPreExecute() {
//        }

        /**
         * 特别赞一下这个多次参数的方法，特别方便
         *
         * @param progress
         */
        @Override
        protected void onProgressUpdate(Integer... progress) {
            KLog.e("更新进度" + progress[0] );
            // 应该是去通知对应的那个 item 改变。
            articlesAdapter.notifyItemChanged(progress[0]);
        }
    }

    private void showSearchResult(String keyword) {
        User user = App.i().getUser();
        user.setStreamId(App.CATEGORY_SEARCH);
        user.setStreamTitle(getString(R.string.main_toolbar_title_search) + keyword);
        CoreDB.i().userDao().update(user);

        if(articleViewModel.articles != null && articleViewModel.articles.hasObservers()){
            articleViewModel.articles.removeObservers(this);
            articleViewModel.articles = null;
        }
        articleViewModel.getAllByKeyword(App.i().getUser().getId(),keyword).observe(this, new Observer<PagedList<Article>>() {
            @Override
            public void onChanged(PagedList<Article> articles) {
                articlesAdapter.submitList(articles);
                loadViewByData( articles.size() );
            }
        });
        articleListView.scrollToPosition(0);
    }


    private void toggleReadState(final int position) {
        if (position < 0) {
            return;
        }
        // String articleId = articlesAdapter.getItem(position).getId();
        // Article article = CoreDB.i().articleDao().getById(App.i().getUser().getId(),articleId);
        Article article = articlesAdapter.getItem(position);
        if (autoMarkReaded && article.getReadStatus() == App.STATUS_UNREAD) {
            article.setReadStatus(App.STATUS_UNREADING);
            CoreDB.i().articleDao().update(article);
        } else if (article.getReadStatus() == App.STATUS_READED) {
            article.setReadStatus(App.STATUS_UNREADING);
            CoreDB.i().articleDao().update(article);
            App.i().getApi().markArticleUnread(article.getId(), new CallbackX() {
                @Override
                public void onSuccess(Object result) {
                }

                @Override
                public void onFailure(Object error) {
                    article.setReadStatus(App.STATUS_READED);
                    CoreDB.i().articleDao().update(article);
                    KLog.e("失败的原因是：" + error );
                }
            });
        } else {
            article.setReadStatus(App.STATUS_READED);
            CoreDB.i().articleDao().update(article);
            App.i().getApi().markArticleReaded(article.getId(), new CallbackX() {
                @Override
                public void onSuccess(Object result) {
                }

                @Override
                public void onFailure(Object error) {
                    article.setReadStatus(App.STATUS_UNREAD);
                    CoreDB.i().articleDao().update(article);
                }
            });
        }
//        KLog.e("修改状态：" + position + "  "  + article);
        articlesAdapter.notifyItemChanged(position);
    }


    private void toggleStarState(final int position) {
        if (position < 0) {
            return;
        }

        Article article = articlesAdapter.getItem(position);
//        String articleId = articlesAdapter.getItem(position).getId();
//        Article article = CoreDB.i().articleDao().getById(App.i().getUser().getId(),articleId);

        if (article.getStarStatus() == App.STATUS_STARED) {
            article.setStarStatus(App.STATUS_UNSTAR);
            CoreDB.i().articleDao().update(article);

            App.i().getApi().markArticleUnstar(article.getId(), new CallbackX() {
                 @Override
                 public void onSuccess(Object result) {
                 }
                 @Override
                 public void onFailure(Object error) {
                     article.setStarStatus(App.STATUS_STARED);
                     CoreDB.i().articleDao().update(article);
                 }
             });

        } else {
            article.setStarStatus(App.STATUS_STARED);
            CoreDB.i().articleDao().update(article);
            App.i().getApi().markArticleStared(article.getId(), new CallbackX() {
                 @Override
                 public void onSuccess(Object result) {
                 }

                 @Override
                 public void onFailure(Object error) {
                     article.setStarStatus(App.STATUS_UNSTAR);
                     CoreDB.i().articleDao().update(article);
                 }
             });
        }
        articlesAdapter.notifyItemChanged(position);
    }


    // TODO: 2018/3/4 改用观察者模式。http://iaspen.cn/2015/05/09/观察者模式在android上的最佳实践
    /**
     * 在android中从A页面跳转到B页面，然后B页面进行某些操作后需要通知A页面去刷新数据，
     * 我们可以通过startActivityForResult来唤起B页面，然后再B页面结束后在A页面重写onActivityResult来接收返回结果从而来刷新页面。
     * 但是如果跳转路径是这样的A->B->C->…..，C或者C以后的页面来刷新A，这个时候如果还是使用这种方法就会非常的棘手。
     * 使用这种方法可能会存在以下几个弊端：
     * 1、多个路径或者多个事件的传递处理起来会非常困难。
     * 2、数据更新不及时，往往需要用户去等待，降低系统性能和用户体验。
     * 3、代码结构混乱，不易编码和扩展。
     * 因此考虑使用观察者模式去处理这个问题。
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
//        KLog.e("------------------------------------------" + resultCode + requestCode);
        switch (resultCode) {
            case App.ActivityResult_ArtToMain:
                // 在文章页的时候读到了第几篇文章，好让列表也自动将该项置顶
                int articleNo = intent.getExtras().getInt("articleNo");

//                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) articleListView.getLayoutManager();
                assert linearLayoutManager != null;
                if (articleNo > linearLayoutManager.findLastVisibleItemPosition() - 1) {
                    slvSetSelection(articleNo);
                }
                articlesAdapter.notifyDataSetChanged();
                break;
            case App.ActivityResult_SearchLocalArtsToMain:
//                KLog.e("被搜索的词是" + intent.getExtras().getString("searchWord"));
                showSearchResult(intent.getExtras().getString("searchWord"));
                articlesAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    // 滚动到指定位置
    private void slvSetSelection(final int position) {
        // 保证滚动到指定位置时，view至最顶端
        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(this) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        smoothScroller.setTargetPosition(position);
        Objects.requireNonNull(articleListView.getLayoutManager()).startSmoothScroll(smoothScroller);
    }

    public void clickRefreshIcon(View view) {
        refreshData();
    }

    public void onQuickSettingIconClicked(View view) {
        quickSettingDialog = new BottomSheetDialog(MainActivity.this);
        quickSettingDialog.setContentView(R.layout.main_bottom_sheet_more);
//        quickSettingDialog.dismiss(); //dialog消失
//        quickSettingDialog.setCanceledOnTouchOutside(false);  //触摸dialog之外的地方，dialog不消失
//        quickSettingDialog.setCancelable(false); // dialog无法取消，按返回键都取消不了

        View moreSetting = quickSettingDialog.findViewById(R.id.more_setting);
        moreSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                quickSettingDialog.dismiss();
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_bottom, R.anim.fade_out);
            }
        });

        SwitchButton autoMarkWhenScrolling = quickSettingDialog.findViewById(R.id.auto_mark_when_scrolling_switch);
        autoMarkWhenScrolling.setChecked(App.i().getUser().isMarkReadOnScroll());
        autoMarkWhenScrolling.setOnCheckedChangeListener((compoundButton, b) -> {
            KLog.e("onClickedAutoMarkWhenScrolling图标被点击");
            User user = App.i().getUser();
            user.setMarkReadOnScroll(b);
            CoreDB.i().userDao().update(user);
            autoMarkReaded = b;
            if (autoMarkReaded) {
                vToolbarAutoMark.setVisibility(View.VISIBLE);
            } else {
                vToolbarAutoMark.setVisibility(View.GONE);
            }
        });

        SwitchButton downImgOnWifiSwitch = quickSettingDialog.findViewById(R.id.down_img_on_wifi_switch);
        downImgOnWifiSwitch.setChecked(App.i().getUser().isDownloadImgOnlyWifi());
        downImgOnWifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                User user = App.i().getUser();
                user.setDownloadImgOnlyWifi(b);
                CoreDB.i().userDao().update(user);
            }
        });

        SwitchButton nightThemeWifiSwitch = quickSettingDialog.findViewById(R.id.night_theme_switch);
        nightThemeWifiSwitch.setChecked(App.i().getUser().getThemeMode() == App.THEME_NIGHT);
        nightThemeWifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                quickSettingDialog.dismiss();
                manualToggleTheme();
            }
        });

        RadioGroup radioGroup = quickSettingDialog.findViewById(R.id.article_list_state_radio_group);
        final RadioButton radioAll = quickSettingDialog.findViewById(R.id.radio_all);
        final RadioButton radioUnread = quickSettingDialog.findViewById(R.id.radio_unread);
        final RadioButton radioStarred = quickSettingDialog.findViewById(R.id.radio_starred);
        if (App.i().getUser().getStreamStatus() == App.STATUS_STARED) {
            radioStarred.setChecked(true);
        } else if (App.i().getUser().getStreamStatus() == App.STATUS_UNREAD) {
            radioUnread.setChecked(true);
        } else {
            radioAll.setChecked(true);
        }
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                User user = App.i().getUser();
                if (i == radioStarred.getId()) {
                    user.setStreamStatus(App.STATUS_STARED);
                    toolbar.setNavigationIcon(R.drawable.ic_state_star);
                } else if (i == radioUnread.getId()) {
                    user.setStreamStatus(App.STATUS_UNREAD);
                    toolbar.setNavigationIcon(R.drawable.ic_state_unread);
                } else {
                    user.setStreamStatus(App.STATUS_ALL);
                    toolbar.setNavigationIcon(R.drawable.ic_state_all);
                }
                CoreDB.i().userDao().update(user);
                refreshData();
                quickSettingDialog.dismiss();
            }
        });

        IconFontView iconFontView = quickSettingDialog.findViewById(R.id.main_more_close);
        iconFontView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                quickSettingDialog.dismiss();
            }
        });

        quickSettingDialog.show();
    }


    @OnClick(R.id.main_toolbar)
    public void clickToolbar(View view) {
        if (maHandler.hasMessages(App.MSG_DOUBLE_TAP)) {
            maHandler.removeMessages(App.MSG_DOUBLE_TAP);
            articleListView.smoothScrollToPosition(0);
        } else {
            maHandler.sendEmptyMessageDelayed(App.MSG_DOUBLE_TAP, ViewConfiguration.getDoubleTapTimeout());
        }
    }


    /**
     * 监听返回键，弹出提示退出对话框
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 后者为短期内按下的次数
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            quitDialog();
            //返回真表示返回键被屏蔽掉
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void quitDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.main_dialog_esc_confirm)
                .setPositiveButton(R.string.main_dialog_esc_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                    }
                })
                .setNegativeButton(R.string.main_dialog_esc_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private RelativeLayout bottomBar;
    private void initToolbar() {
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        // 这个小于4.0版本是默认为true，在4.0及其以上是false。该方法的作用：决定左上角的图标是否可以点击(没有向左的小图标)，true 可点
        getSupportActionBar().setHomeButtonEnabled(true);
        // 决定左上角图标的左侧是否有向左的小箭头，true 有小箭头
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        if (App.i().getUser().getStreamStatus() == App.STATUS_ALL) {
            toolbar.setNavigationIcon(R.drawable.ic_state_all);
        } else if (App.i().getUser().getStreamStatus() == App.STATUS_STARED) {
            toolbar.setNavigationIcon(R.drawable.ic_state_star);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_state_unread);
        }
        // 左上角图标是否显示，false则没有程序图标，仅标题。否则显示应用程序图标，对应id为android.R.id.home，对应ActionBar.DISPLAY_SHOW_HOME
        // setDisplayShowHomeEnabled(true)
        // 使自定义的普通View能在title栏显示，即actionBar.setCustomView能起作用，对应ActionBar.DISPLAY_SHOW_CUSTOM
        // setDisplayShowCustomEnabled(true)

        bottomBar = findViewById(R.id.main_bottombar);
    }


    /**
     * 设置各个视图与颜色属性的关联
     */
    @Override
    protected Colorful.Builder buildColorful(Colorful.Builder mColorfulBuilder) {
        ViewGroupSetter articlesHeaderVS = new ViewGroupSetter((ViewGroup) articlesHeaderView);
        articlesHeaderVS.childViewBgColor(R.id.main_header, R.attr.root_view_bg);
        articlesHeaderVS.childViewTextColor(R.id.main_header_title, R.attr.lv_item_desc_color);
        // articlesHeaderVS.childViewBgDrawable(R.id.main_header_eye, R.attr.lv_item_desc_color);

        ViewGroupSetter artListViewSetter = new ViewGroupSetter(articleListView);
        // 绑定ListView的Item View中的news_title视图，在换肤时修改它的text_color属性
        // artListViewSetter.childViewBgColor(R.id.main_slv_item, R.attr.root_view_bg);
        artListViewSetter.childViewTextColor(R.id.main_slv_item_title, R.attr.lv_item_title_color);
        artListViewSetter.childViewTextColor(R.id.main_slv_item_summary, R.attr.lv_item_desc_color);
        artListViewSetter.childViewTextColor(R.id.main_slv_item_author, R.attr.lv_item_info_color);
        artListViewSetter.childViewTextColor(R.id.main_slv_item_time, R.attr.lv_item_info_color);
        artListViewSetter.childViewBgColor(R.id.main_slv_item_divider, R.attr.lv_item_divider);
        artListViewSetter.childViewBgColor(R.id.main_list_item_surface, R.attr.root_view_bg);
        // artListViewSetter.childViewBgColor(R.id.main_list_item_menu_left, R.attr.root_view_bg);
        // artListViewSetter.childViewBgColor(R.id.main_list_item_menu_right, R.attr.root_view_bg);
        // artListViewSetter.childViewBgColor(R.id.swipe_layout, R.attr.root_view_bg);

        // 绑定ListView的Item View中的news_title视图，在换肤时修改它的text_color属性
        ViewGroupSetter tagListViewSetter = new ViewGroupSetter(tagListView);
        tagListViewSetter.childViewBgColor(R.id.group_item, R.attr.root_view_bg);  // 这个不生效，反而会影响底色修改
        tagListViewSetter.childViewTextColor(R.id.group_item_icon, R.attr.tag_slv_item_icon);
        tagListViewSetter.childViewTextColor(R.id.group_item_title, R.attr.lv_item_title_color);
        tagListViewSetter.childViewTextColor(R.id.group_item_count, R.attr.lv_item_desc_color);
        // tagListViewSetter.childViewBgDrawable(R.id.group_item_count, R.attr.bubble_bg);

        ViewGroupSetter relative = new ViewGroupSetter(relativeLayout);
        relative.childViewBgColor(R.id.main_tag_close, R.attr.root_view_bg);
        relative.childViewTextColor(R.id.main_tag_close, R.attr.bottombar_fg);
        relative.childViewBgColor(R.id.sheet_tag, R.attr.root_view_bg);
        relative.childViewBgColor(R.id.main_tag_list_view, R.attr.root_view_bg);


        tagListViewSetter.childViewBgColor(R.id.child_item, R.attr.root_view_bg);  // 这个不生效，反而会影响底色修改
        tagListViewSetter.childViewTextColor(R.id.child_item_title, R.attr.lv_item_title_color);
        tagListViewSetter.childViewTextColor(R.id.child_item_count, R.attr.lv_item_desc_color);
//        tagListViewSetter.childViewBgDrawable(R.id.child_item_count, R.attr.bubble_bg);

//        ViewGroupSetter headerHomeViewSetter = new ViewGroupSetter((ViewGroup) headerHomeView);
//        headerHomeViewSetter.childViewBgColor(R.id.header_home, R.attr.root_view_bg);  // 这个不生效，反而会影响底色修改
//        headerHomeViewSetter.childViewTextColor(R.id.header_home_icon, R.attr.tag_slv_item_icon);
//        headerHomeViewSetter.childViewTextColor(R.id.header_home_title, R.attr.lv_item_title_color);
//        headerHomeViewSetter.childViewTextColor(R.id.header_home_count, R.attr.lv_item_desc_color);

//        ViewGroupSetter headerPinnedViewSetter = new ViewGroupSetter((ViewGroup) headerPinnedView);
//        headerPinnedViewSetter.childViewBgColor(R.id.header_item, R.attr.root_view_bg);
//        headerPinnedViewSetter.childViewTextColor(R.id.header_item_icon, R.attr.tag_slv_item_icon);
//        headerPinnedViewSetter.childViewTextColor(R.id.header_item_title, R.attr.lv_item_title_color);
//        headerPinnedViewSetter.childViewTextColor(R.id.header_item_count, R.attr.lv_item_desc_color);
//        headerPinnedViewSetter.childViewBgDrawable(R.id.header_item_count, R.attr.bubble_bg);

        mColorfulBuilder
                // 这里做设置，实质都是直接生成了一个View（根据Activity的findViewById），并直接添加到 colorful 内的 mElements 中。
                .backgroundColor(R.id.main_swipe_refresh, R.attr.root_view_bg)
                // 设置 toolbar
                .backgroundColor(R.id.main_toolbar, R.attr.topbar_bg)
                //.textColor(R.id.main_toolbar_hint, R.attr.topbar_fg)

                .backgroundColor(R.id.sheet_tag, R.attr.root_view_bg)

                // 设置 bottombar
                .backgroundColor(R.id.main_bottombar, R.attr.bottombar_bg)
                // 设置中屏和底栏之间的分割线
                .backgroundColor(R.id.main_bottombar_divider, R.attr.bottombar_divider)
                .textColor(R.id.main_bottombar_search, R.attr.bottombar_fg)
                .textColor(R.id.main_bottombar_setting, R.attr.bottombar_fg)
                .textColor(R.id.main_bottombar_tag, R.attr.bottombar_fg)
                .textColor(R.id.main_bottombar_refresh_articles, R.attr.bottombar_fg)

                // 设置 listview 背景色
                // 这里做设置，实质是将View（根据Activity的findViewById），并直接添加到 colorful 内的 mElements 中。
                .setter(relative)
//                .setter(headerPinnedViewSetter)
//                .setter(headerHomeViewSetter)
                .setter(articlesHeaderVS)
                .setter(artListViewSetter)
                .setter(tagListViewSetter);
        return mColorfulBuilder;
    }
}
