package com.carles.jogging.main;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.carles.jogging.BaseFragment;
import com.carles.jogging.R;
import com.limecreativelabs.sherlocksupport.ActionBarDrawerToggleCompat;

/**
 * Created by carles1 on 20/04/14.
 */
public class NavigationDrawerFragment extends BaseFragment {

    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggleCompat mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;

    private NavigationDrawerCallbacks mCallbacks;

    public NavigationDrawerFragment() {}


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*- open drawer anytime */
        mUserLearnedDrawer = false;

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }

        // Select either the default item (0) or the last selected item.
        selectItem(mCurrentSelectedPosition);
    }

    private void selectItem(int position) {

        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }

        mCurrentSelectedPosition = position;
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        mDrawerListView = (ListView)view.findViewById(R.id.list);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
        mDrawerListView.setAdapter(new DrawerAdapter(getSherlockActivity()));
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar
        setHasOptionsMenu(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        /*- Forward the new configuration the drawer toggle component. */
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        /*- If the drawer is open, show the global app actions in the action bar. */
        /*- showGlobalContextActionBar which controls the top-left area of the action bar. */
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return getSherlockActivity().getSupportActionBar();
    }

    @Override
    /*- Per the navigation drawer design guidelines, updates the action bar to show the global app 'context', rather than just what's in the screen. */
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        /*- set a custom shadow that overlays the main content when the drawer opens */
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        /*- set up the drawer's list view with items and click listener */
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        /*- ActionBarDrawerToggle ties the proper interactions between the drawer and the action bar app icon. */
        mDrawerToggle = new MyActionBarDrawerToggleCompat();

        /*- If the user hasn't 'learned' about the drawer open it to introduce them to the drawer */
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        /*- Defer code dependent on restoration of previous instance state. */
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int position);
    }

    /*- ********************************************************************************** */
    /*- ********************************************************************************** */
    private class MyActionBarDrawerToggleCompat extends ActionBarDrawerToggleCompat {

        public MyActionBarDrawerToggleCompat() {
            super(getActivity(),                    /* host Activity */
                    mDrawerLayout,                    /* DrawerLayout object */
                    R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                    R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                    R.string.navigation_drawer_close  /* "close drawer" description for accessibility */);
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            super.onDrawerClosed(drawerView);
            if (!isAdded()) {
                return;
            }
            getActivity().supportInvalidateOptionsMenu(); /*- calls onPrepareOptionsMenu() */
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            if (!isAdded()) {
                return;
            }

            if (!mUserLearnedDrawer) {
                    /*-store this flag to prevent auto-showing the drawer automatically in the future */
                mUserLearnedDrawer = true;
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
            }

            getActivity().supportInvalidateOptionsMenu(); /*- calls onPrepareOptionsMenu() */
        }
    }

}