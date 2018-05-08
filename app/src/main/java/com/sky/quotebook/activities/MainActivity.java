package com.sky.quotebook.activities;

import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Parcelable;
import android.renderscript.RenderScript;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.transition.Explode;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sky.quotebook.fragments.GeneralDialogFragment;
import com.sky.quotebook.fragments.NoteFragment;
import com.sky.quotebook.R;
import com.sky.quotebook.adapter.RecyclerViewAdapter;
import com.sky.quotebook.fragments.AuthorListFragment;
import com.sky.quotebook.fragments.CategoryListFragment;
import com.sky.quotebook.fragments.SetupFragment;
import com.sky.quotebook.interfaces.ItemClickListener;
import com.sky.quotebook.model.AppPreferences;
import com.sky.quotebook.model.Author;
import com.sky.quotebook.model.Constant;
import com.sky.quotebook.view.ItemTouchHelperCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends AppCompatActivity implements GeneralDialogFragment.OnDialogFragmentClickListener {

    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter adapter;
    private List<String> data;
    private String insertData;
    private boolean loading;
    private int loadTimes;
    private List<Author> authors;
    private ItemClickListener mListener;

    BottomNavigationView bottomNavigation;
    Fragment fragment;
    FragmentManager fragmentManager;
    TextView mlaunchCount;
    boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Random quotes");

        //display back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getWindow().setGravity(toolbar.TEXT_ALIGNMENT_GRAVITY);


        //launch rate app dialog
        mlaunchCount = (TextView) findViewById(R.id.launchCount);
        AppPreferences.getInstance(getApplicationContext()).incrementLaunchCount();
        mlaunchCount.setText(getString(R.string.app_message,
                AppPreferences.getInstance(getApplicationContext()).getLaunchCount()));
        showRateAppDialog();


        //BottomNavigationView for fragment author, category and note
        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.inflateMenu(R.menu.bottom_menu);
        fragmentManager = getSupportFragmentManager();
        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id) {
                    case R.id.action_author:
                        fragment = new AuthorListFragment();
                        break;
                    case R.id.action_category:
                        fragment = new CategoryListFragment();
                        break;
                    case R.id.action_note:
                        fragment = new NoteFragment();
                        break;
                }
                final FragmentTransaction transaction = fragmentManager.beginTransaction();

                //Custom animation on fragment appear and disappear
                transaction.setCustomAnimations(R.anim.fragment_slide_left_enter,
                        R.anim.fragment_slide_left_exit,
                        R.anim.fragment_slide_right_enter,
                        R.anim.fragment_slide_right_exit);

                transaction.replace(R.id.main_container, fragment);

                //Back to host activity
                transaction.addToBackStack(null);
                transaction.commit();
                return true;
            }
        });


        initData();
        initView();
    }

    @Override
    public void onBackPressed() {

        //2nd condition for fragment to exit when back pressed one time.
        if (doubleBackToExitPressedOnce || fragmentManager.getBackStackEntryCount() != 0) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "press back again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }


    //rate us dialog
    private void showRateAppDialog() {
        boolean bool = AppPreferences.getInstance(getApplicationContext()).getAppRate();
        int i = AppPreferences.getInstance(getApplicationContext()).getLaunchCount();
        if ((bool) && (i == 2)) {
            createAppRatingDialog(getString(R.string.rate_app_title), getString(R.string.rate_app_message)).show();
        }
    }

    //created rate dialog, having three button, "Later", "Feedback" & "Rate Us"
    private AlertDialog createAppRatingDialog(String rateAppTitle, String rateAppMessage) {
        AlertDialog dialog = new AlertDialog.Builder(this).setIcon(R.drawable.ic_setup_red_white_60dp).
                setPositiveButton(getString(R.string.dialog_app_rate), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                        openAppInPlayStore(MainActivity.this);
                        AppPreferences.getInstance(MainActivity.this.getApplicationContext()).setAppRate(false);
                    }
                }).setNegativeButton(getString(R.string.dialog_your_feedback), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                openFeedback(MainActivity.this);
                AppPreferences.getInstance(MainActivity.this.getApplicationContext()).setAppRate(false);
            }
        }).setNeutralButton(getString(R.string.dialog_ask_later), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                paramAnonymousDialogInterface.dismiss();
                AppPreferences.getInstance(MainActivity.this.getApplicationContext()).resetLaunchCount();
            }
        }).setMessage(rateAppMessage).setTitle(rateAppTitle).create();
        return dialog;
    }

    //Path to our app on playStore
    public static void openAppInPlayStore(Context paramContext) {
        paramContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/")));
    }

    //user will email his/her feedBack on Emails Address only
    public static void openFeedback(Context paramContext) {
        Intent localIntent = new Intent(Intent.ACTION_SEND);
        localIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"muhammadyasir.2551.@gmail.com"});
        localIntent.putExtra(Intent.EXTRA_CC, "");
        String str = null;
        try {
            str = paramContext.getPackageManager().getPackageInfo(paramContext.getPackageName(), 0).versionName;
            localIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for Your Android App");
            localIntent.putExtra(Intent.EXTRA_TEXT, "\n\n----------------------------------\n Device OS: Android \n Device OS version: " +
                    Build.VERSION.RELEASE + "\n App Version: " + str + "\n Device Brand: " + Build.BRAND +
                    "\n Device Model: " + Build.MODEL + "\n Device Manufacturer: " + Build.MANUFACTURER);
            localIntent.setType("message/rfc822");
            paramContext.startActivity(Intent.createChooser(localIntent, "Choose an Email client :"));
        } catch (Exception e) {
            Log.d("OpenFeedback", e.getMessage());
        }
    }


    private void initData() {
        data = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            data.add(i + "");
        }
        insertData = "0";
        loadTimes = 0;
    }

    private void initView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_1);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        adapter = new RecyclerViewAdapter(this);
        mRecyclerView.setAdapter(adapter);
        adapter.setItems(data);
        adapter.addFooter();

        //support swapping left/Right --> delete a rec-view-item, Drag above/below --> to position an item,
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(adapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);


        RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!loading && linearLayoutManager.getItemCount() == (linearLayoutManager.findLastVisibleItemPosition() + 1)) {

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (loadTimes <= 5) {
                                adapter.removeFooter();
                                loading = false;
                                adapter.addItems(data);
                                adapter.addFooter();
                                loadTimes++;
                            } else {
                                adapter.removeFooter();
                                Snackbar.make(mRecyclerView, ("no more data"), Snackbar.LENGTH_SHORT).setCallback(new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar transientBottomBar, int event) {
                                        super.onDismissed(transientBottomBar, event);
                                        loading = false;
                                        adapter.addFooter();
                                    }
                                }).show();
                            }
                        }
                    }, 1500);
                    loading = true;
                }
            }
        };
        //add new items on scrolling
        mRecyclerView.addOnScrollListener(scrollListener);
    }
    // rec-view ended here


    //Menu Items, Toolbar items
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);

                //Starting a new activity will have these applied animations
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
                break;
            }
            case R.id.action_about: {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);

                //animation applied
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
                break;
            }

            case R.id.action_save: {
                //add your fav quote, author, and category
                Toast.makeText(getApplicationContext(), "save", Toast.LENGTH_SHORT).show();
            }
            case R.id.action_share: {
                // onShareClick(bottomNavigation);
                GeneralDialogFragment generalDialogFragment =
                        GeneralDialogFragment.newInstance("Click 'share' to complete this action\n", "Here are two options");
                generalDialogFragment.show(getSupportFragmentManager(), "dialog");
                //  Toast.makeText(getApplicationContext(), "share", Toast.LENGTH_SHORT).show();
                break;
            }
        }
        return false;
    }

    /*
    * "onShareClick" will only share your content on Facebook, Twitter and whatsApp
    * */
    public void onShareClick(View v) {
        List<Intent> targetShareIntents = new ArrayList<Intent>();
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        List<ResolveInfo> resInfos = getPackageManager().queryIntentActivities(shareIntent, 0);
        if (!resInfos.isEmpty()) {
            System.out.println("Have package");
            for (ResolveInfo resInfo : resInfos) {
                String packageName = resInfo.activityInfo.packageName;

                //gets all of the installed packages names
                Log.i("Package Name    ****** ", packageName);

                //choose these three only to share
                if (packageName.contains("com.twitter.android") || packageName.contains("com.facebook.katana") || packageName.contains("com.whatsapp")) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(packageName, resInfo.activityInfo.name));
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, "Text");
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                    intent.setPackage(packageName);
                    targetShareIntents.add(intent);
                }
            }
            if (!targetShareIntents.isEmpty()) {
                System.out.println("Have Intent");
                Intent chooserIntent = Intent.createChooser(targetShareIntents.remove(0), "Choose app to share");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetShareIntents.toArray(new Parcelable[]{}));
                startActivity(chooserIntent);
            } else {
                System.out.println("Do not Have Intent");
                //show Dialog (this);
                Toast.makeText(getApplicationContext(), "Don't share", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Dialog appear having the message and Button. Button "Share" --> share your content
    @Override
    public void onOkClicked(GeneralDialogFragment dialog) {
        //called this method to share only on Twitter, Facebook and WhatsApp
        onShareClick(bottomNavigation);
    }

    /*@Override
    public void onCancelClicked(GeneralDialogFragment dialog) {
        Toast.makeText(getApplicationContext(), "cancelled Dialog fragment", Toast.LENGTH_SHORT).show();

    }*/
}