package com.example.ud4.muzicplayer;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/** HostFragment Class */
//*******************************************************
public class HostFragment extends Fragment
{
    public HostFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_host, container, false);
    }

    @Override
    public void onResume() 
    {
        super.onResume();
        ViewPager viewPager = (ViewPager) getView().findViewById(R.id.view_pager);
        MainActivity.SectionsPagerAdapter pagerAdapter = new MainActivity.SectionsPagerAdapter(getChildFragmentManager());

        TabLayout tabLayout = (TabLayout) getView().findViewById(R.id.tabs);

        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }
}//end-class
