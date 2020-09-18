/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.onboarding_tabs_tray_layout.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.onboarding.OnboardingRadioButton
import org.mozilla.fenix.utils.view.addToRadioGroup

class OnboardingTabsTrayLayoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private var fennecStyleTabsScreen: OnboardingRadioButton = view.tabs_tray_fennec_style
    private var fenixStyleTabsTray: OnboardingRadioButton = view.tabs_tray_fenix_style

    init {
        addToRadioGroup(fennecStyleTabsScreen, fenixStyleTabsTray)

        fennecStyleTabsScreen.isChecked =
            itemView.context.settings().shouldUseFennecStyleTabsScreen
        fenixStyleTabsTray.isChecked =
            !itemView.context.settings().shouldUseFennecStyleTabsScreen

        fennecStyleTabsScreen.onClickListener {
            setFennecStyleTabsScreen(true)
        }

        fenixStyleTabsTray.onClickListener {
            setFennecStyleTabsScreen(false)
        }
    }

    private fun setFennecStyleTabsScreen(enabled: Boolean) {
        itemView.context.settings().apply {
            enableCompactTabs = enabled
            useFullScreenTabScreen = enabled
            reverseTabOrderInTabsTray = !enabled
            useNewTabFloatingActionButton = !enabled
            placeNewTabFloatingActionButtonAtTop = false
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.onboarding_tabs_tray_layout
    }
}
