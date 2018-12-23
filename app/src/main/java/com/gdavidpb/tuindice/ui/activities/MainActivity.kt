package com.gdavidpb.tuindice.ui.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.gdavidpb.tuindice.R
import com.gdavidpb.tuindice.data.utils.notNull
import com.gdavidpb.tuindice.domain.model.Account
import com.gdavidpb.tuindice.presentation.viewmodel.MainActivityViewModel
import com.google.android.material.navigation.NavigationView
import io.reactivex.observers.DisposableMaybeObserver
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.email
import org.jetbrains.anko.share
import org.jetbrains.anko.startActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val viewModel: MainActivityViewModel by viewModel()

    private val activeAccountObserver = ActiveAccountObserver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.getActiveAccount(activeAccountObserver)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START))
            drawer_layout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId

        when (itemId) {
            R.id.nav_share -> share(text = getString(R.string.aboutShareMessage, packageName))
            R.id.nav_twitter -> browse(url = getString(R.string.devTwitter))
            R.id.nav_contact -> email(email = getString(R.string.devEmail), subject = getString(R.string.devContactSubject))
            else -> loadFragment(itemId)
        }

        drawer_layout.closeDrawer(GravityCompat.START)

        return true
    }

    private fun onViewCreated() {
        setSupportActionBar(toolbar)

        ActionBarDrawerToggle(
                this, drawer_layout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close).let { toggle ->
            drawer_layout.addDrawerListener(toggle)

            toggle.syncState()
        }

        nav_view.setNavigationItemSelectedListener(this)

        arrayOf(R.id.nav_share, R.id.nav_twitter, R.id.nav_contact)
                .forEach {
                    nav_view.menu.findItem(it).setActionView(R.layout.view_open)
                }
    }

    private fun loadFragment(@IdRes itemId: Int) {
        when (itemId) {
            R.id.nav_enrollment -> R.string.nav_enrollment to Fragment()
            else -> null
        }.notNull {
            supportFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    .replace(R.id.content_fragment, it.second)
                    .commit()
        }
    }

    inner class ActiveAccountObserver : DisposableMaybeObserver<Account>() {
        override fun onSuccess(t: Account) {
            setTheme(R.style.AppTheme_NoActionBar)

            setContentView(R.layout.activity_main)

            onViewCreated()

            //todo load account
        }

        override fun onComplete() {
            startActivity<LoginActivity>()
            finish()
        }

        override fun onError(e: Throwable) {
            //todo handle error
        }
    }
}