package com.lounah.gallery.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.lounah.gallery.R;
import com.lounah.gallery.ui.feed.FeedFragment;
import com.lounah.gallery.ui.navigation.NavigationController;
import com.lounah.gallery.ui.offline.OfflineFragment;
import com.lounah.gallery.ui.trash.TrashFragment;
import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItemAdapter;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItems;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerAppCompatActivity;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends DaggerAppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    ClearCacheDialogFragment.OnClickListener{

    @BindView(R.id.toolbar_main)
    Toolbar toolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.nav_view)
    NavigationView navView;

    @BindView(R.id.tabs_main)
    SmartTabLayout tabLayout;

    @BindView(R.id.viewpager_main)
    ViewPager viewPager;

    @Inject
    NavigationController mNavigationController;

    private static final int FEED_POSITION = 0;
    private static final int OFFLINE_POSITION = 1;
    private static final int TRASH_POSITION = 2;

    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 0;

    private static final String TAG = "MAIN_ACTIVITY";

    private ClearCacheDialogFragment clearCacheOptionsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissionsOrInitUI();
    }

    private void requestPermissionsOrInitUI() {

        final int writeExternalStoragePermission = ContextCompat
                .checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);

        final int readExternalStoragePermission = ContextCompat
                .checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE);

        if (writeExternalStoragePermission != PackageManager.PERMISSION_GRANTED ||
                readExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
        } else initUI();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            initUI();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initUI() {
        ButterKnife.bind(this);

        FragmentPagerItemAdapter adapter = new FragmentPagerItemAdapter(getSupportFragmentManager(),
                FragmentPagerItems.with(this)
                        .add(R.string.feed, FeedFragment.class)
                        .add(R.string.offline, OfflineFragment.class)
                        .add(R.string.trash, TrashFragment.class)
                        .create());

        viewPager.setAdapter(adapter);
        tabLayout.setViewPager(viewPager);
        tabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case FEED_POSITION:
                        navView.setCheckedItem(R.id.nav_feed);
                        break;
                    case OFFLINE_POSITION:
                        navView.setCheckedItem(R.id.nav_offline);
                        break;
                    case TRASH_POSITION:
                        navView.setCheckedItem(R.id.nav_trash);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navView.getMenu().getItem(FEED_POSITION).setChecked(true);
        navView.setNavigationItemSelectedListener(this);

    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mNavigationController.getBackStackEntryCount() > 0)
            mNavigationController.popBackStack(); else super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.nav_feed:
                viewPager.setCurrentItem(FEED_POSITION);
                break;

            case R.id.nav_offline:
                viewPager.setCurrentItem(OFFLINE_POSITION);
                break;

            case R.id.nav_trash:
                viewPager.setCurrentItem(TRASH_POSITION);
                break;

            case R.id.nav_clear_space:
                clearCacheOptionsDialog = new ClearCacheDialogFragment();
                clearCacheOptionsDialog.show(getFragmentManager(), TAG);
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onCancel() {
        clearCacheOptionsDialog.dismiss();
    }

    @Override
    public void onClearCache() {
        Completable.fromAction(() -> Glide.get(this).clearDiskCache())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> Glide.get(this).clearMemory())
                .subscribe(() -> clearCacheOptionsDialog.dismiss(),
                        e -> Toast.makeText(this, R.string.error_clear_cache,
                                    Toast.LENGTH_SHORT).show());
    }

}
