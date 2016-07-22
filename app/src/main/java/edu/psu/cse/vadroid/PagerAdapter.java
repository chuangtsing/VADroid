package edu.psu.cse.vadroid;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import edu.psu.cse.vadroid.ProcessFragment;

/**
 * Created by zblas_000 on 8/11/2015.
 */
public class PagerAdapter extends FragmentStatePagerAdapter {
    private int tabs;

    public PagerAdapter(android.support.v4.app.FragmentManager fm, int tabs) {
        super(fm);
        this.tabs = tabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ProcessFragment();
            case 1:
                return new SettingsFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return tabs;
    }
}
