package com.suntech.iot.sewing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.suntech.iot.sewing.base.BaseFragment
import com.suntech.iot.sewing.common.AppGlobal
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.layout_bottom_info.*
import kotlinx.android.synthetic.main.layout_top_menu.*

class HomeFragment : BaseFragment() {

    private var _running: Boolean = false

    private val _need_to_home_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateView()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_home_refresh, IntentFilter("need.refresh"))
        updateView()
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(_need_to_home_refresh)
    }

    override fun initViews() {
        tv_app_version.text = "v " + activity.packageManager.getPackageInfo(activity.packageName, 0).versionName

        // Click event
        btn_count_view.setOnClickListener {
            (activity as MainActivity).changeFragment(1)
        }
        btn_component_info.setOnClickListener {
            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
            } else {
                designInfofunc()
            }
        }

        btn_work_info.setOnClickListener {
            if (AppGlobal.instance.get_factory() == "" || AppGlobal.instance.get_room() == "" || AppGlobal.instance.get_line() == "") {
                Toast.makeText(activity, getString(R.string.msg_no_setting), Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(activity, WorkInfoActivity::class.java))
            }
        }
        btn_setting_view.setOnClickListener { startActivity(Intent(activity, SettingActivity::class.java)) }

        updateView()

        startAutoSettingHandler()
    }

    override fun onSelected() {
        activity?.tv_title?.visibility = View.GONE
        updateView()
    }

    private fun startAutoSettingHandler() {
        val handler = Handler()
        handler.postDelayed({
            if (AppGlobal.instance.get_auto_setting()) {
                if (!_running) checkAutoSetting()
                startAutoSettingHandler()
            }
        }, 800)
    }

    private fun checkAutoSetting() {
        if (AppGlobal.instance.get_factory() == "" || AppGlobal.instance.get_room() == "" || AppGlobal.instance.get_line() == "") {
            _running = true
            getBaseActivity().startActivity(Intent(activity, SettingActivity::class.java), { r, c, m, d ->
                _running = false
            })
        } else if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
            _running = true
            getBaseActivity().startActivity(Intent(activity, WorkInfoActivity::class.java), { r, c, m, d ->
                _running = false
            })
        } else if (AppGlobal.instance.get_design_info_idx() == "") {
            designInfofunc()
        }
    }

    private fun designInfofunc() {
        _running = true

        val intent = Intent(activity, DesignInfoActivity::class.java)
        getBaseActivity().startActivity(intent, { r, c, m, d ->
            _running = false

            if (r && d!=null) {
                val idx = d!!["idx"]!!
                val cycle_time = d["ct"]!!.toInt()
                val model = d["model"]!!.toString()
                val article = d["article"]!!.toString()
                val material_way = d["material_way"]!!.toString()
                val component = d["component"]!!.toString()

                (activity as MainActivity).startNewProduct(idx, cycle_time, model, article, material_way, component)
            }
            if (AppGlobal.instance.get_auto_setting()) {
                AppGlobal.instance.set_auto_setting(false)
                (activity as MainActivity).changeFragment(1)
            }
        })
    }

    private fun updateView() {
        tv_factory.text = AppGlobal.instance.get_factory()
        tv_room.text = AppGlobal.instance.get_room()
        tv_line.text = AppGlobal.instance.get_line()
        tv_mc_no.text = AppGlobal.instance.get_mc_no1() //+ "-" + AppGlobal.instance.get_mc_no2()
        tv_employee_no.text = AppGlobal.instance.get_worker_no()
        tv_employee_name.text = AppGlobal.instance.get_worker_name()
        tv_shift.text = AppGlobal.instance.get_current_shift_name()
    }
}