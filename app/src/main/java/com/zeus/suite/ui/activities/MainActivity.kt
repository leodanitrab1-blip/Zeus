package com.zeus.suite.ui.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.zeus.suite.R
import com.zeus.suite.ui.adapters.ModuleAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var moduleAdapter: ModuleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupViewPager()
        setupTabLayout()
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
    }

    private fun setupViewPager() {
        moduleAdapter = ModuleAdapter(this)
        viewPager.adapter = moduleAdapter
        
        // Deshabilitar swipe entre modulos para mejor UX
        viewPager.isUserInputEnabled = false
        
        // Mantener todos los fragmentos en memoria
        viewPager.offscreenPageLimit = 3
    }

    private fun setupTabLayout() {
        val tabTitles = arrayOf(
            "PDF",
            "Big Data",
            "IA",
            "Auto"
        )
        
        val tabIcons = intArrayOf(
            R.drawable.ic_pdf,
            R.drawable.ic_bigdata,
            R.drawable.ic_ai,
            R.drawable.ic_auto
        )

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
            tab.setIcon(tabIcons[position])
            tab.contentDescription = "Modulo ${tabTitles[position]}"
        }.attach()

        // Estilizar el TabLayout
        tabLayout.tabRippleColor = resources.getColorStateList(
            R.color.zeus_primary_light, 
            theme
        )
        tabLayout.tabIconTint = resources.getColorStateList(
            R.color.tab_icon_color, 
            theme
        )
    }

    override fun onBackPressed() {
        // Si no estamos en el primer modulo, volver a el
        if (viewPager.currentItem > 0) {
            viewPager.currentItem = 0
        } else {
            super.onBackPressed()
        }
    }
}