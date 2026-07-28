package com.zeus.suite.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.zeus.suite.ui.fragments.PDFModuleFragment
import com.zeus.suite.ui.fragments.BigDataFragment
import com.zeus.suite.ui.fragments.AIFragment
import com.zeus.suite.ui.fragments.AutoFragment

class ModuleAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val MODULE_PDF = 0
        const val MODULE_BIG_DATA = 1
        const val MODULE_AI = 2
        const val MODULE_AUTOMATION = 3
        const val TOTAL_MODULES = 4
    }

    override fun getItemCount(): Int = TOTAL_MODULES

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            MODULE_PDF -> PDFModuleFragment.newInstance()
            MODULE_BIG_DATA -> BigDataFragment.newInstance()
            MODULE_AI -> AIFragment.newInstance()
            MODULE_AUTOMATION -> AutoFragment.newInstance()
            else -> throw IllegalArgumentException("Posicion de modulo invalida: $position")
        }
    }

    fun getModuleName(position: Int): String {
        return when (position) {
            MODULE_PDF -> "PDF"
            MODULE_BIG_DATA -> "Big Data"
            MODULE_AI -> "IA"
            MODULE_AUTOMATION -> "Automatizacion"
            else -> "Desconocido"
        }
    }
}