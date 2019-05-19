package com.suntech.iot.sewing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suntech.iot.sewing.base.BaseFragment
import com.suntech.iot.sewing.common.AppGlobal
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.fragment_count_view.ll_charts
import kotlinx.android.synthetic.main.fragment_count_view.ll_component_count
import kotlinx.android.synthetic.main.fragment_count_view.ll_total_count
import kotlinx.android.synthetic.main.layout_top_menu.*
import org.joda.time.DateTime

class CountViewFragment : BaseFragment() {

    private var is_loop: Boolean = false

    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    private var _total_target = 0

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            computeCycleTime()
            updateView()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_count_view, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_refresh, IntentFilter("need.refresh"))
        is_loop = true
//        computeCycleTime()
        fetchColorData()     // Get Color
        updateView()
        startHandler()
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(_need_to_refresh)
        is_loop = false
    }

    override fun onSelected() {
        activity.tv_title?.visibility = View.VISIBLE

        if ((activity as MainActivity).countViewType == 1) {
            ll_total_count.visibility = View.VISIBLE
            ll_component_count.visibility = View.GONE
            if ((activity as MainActivity).countViewMode == 1) {
                ll_count_mode.visibility = View.VISIBLE
                ll_repair_mode.visibility = View.GONE
            } else {
                ll_count_mode.visibility = View.GONE
                ll_repair_mode.visibility = View.VISIBLE
            }
        } else {
            ll_total_count.visibility = View.GONE
            ll_component_count.visibility = View.VISIBLE
//            fetchFilterWos()    // 기존 선택된 WOS 가 있으면 로드해서 화면에 표시한다.
        }

//        computeCycleTime()
    }

    override fun initViews() {
        super.initViews()

        btn_go_repair_mode.setOnClickListener {
            (activity as MainActivity).countViewMode = 2
            ll_count_mode.visibility = View.GONE
            ll_repair_mode.visibility = View.VISIBLE
        }
        btn_go_count_mode.setOnClickListener {
            (activity as MainActivity).countViewMode = 1
            ll_count_mode.visibility = View.VISIBLE
            ll_repair_mode.visibility = View.GONE
        }

        // Button click in Count view
        tv_btn_wos_count.setOnClickListener {
            (activity as MainActivity).countViewType = 2
            ll_total_count.visibility = View.GONE
            ll_component_count.visibility = View.VISIBLE
        }
        ll_btn_wos_count.setOnClickListener {
            (activity as MainActivity).countViewType = 2
            ll_total_count.visibility = View.GONE
            ll_component_count.visibility = View.VISIBLE
        }

        // Button click in Component count view
        btn_total_count_view.setOnClickListener {
            (activity as MainActivity).countViewType = 1
            ll_total_count.visibility = View.VISIBLE
            ll_component_count.visibility = View.GONE
        }
        fetchColorData()    // Get Color
    }

    // 값에 변화가 생길때만 화면을 리프레쉬 하기 위한 변수
    var _current_target_count = -1
    var _current_actual_count = -1
    var _current_compo_target_count = -1
    var _current_compo_actual_count = -1

    private fun updateView() {
        // 콤포넌트 기능을 사용하는지 체크
        if (AppGlobal.instance.get_with_component()) ll_charts.visibility = View.VISIBLE
        else ll_charts.visibility = View.GONE

//        countTarget()

        // Total count view 화면 정보 표시
        val total_actual = AppGlobal.instance.get_current_shift_actual_cnt()

        // 값에 변화가 있을때만 갱신
        if (_current_target_count != _total_target || _current_actual_count != total_actual) {
            _current_target_count = _total_target
            _current_actual_count = total_actual
            var ratio = 0
            var ratio_txt = "N/A"

            if (_total_target > 0) {
                ratio = (total_actual.toFloat() / _total_target.toFloat() * 100).toInt()
                if (ratio > 999) ratio = 999
                ratio_txt = "" + ratio + "%"
            }

            tv_count_view_target.text = "" + _total_target
            tv_count_view_actual.text = "" + total_actual
            tv_count_view_ratio.text = ratio_txt

            var maxEnumber = 0
            var color_code = "ffffff"

            for (i in 0..(_list.size - 1)) {
                val snumber = _list[i]["snumber"]?.toInt() ?: 0
                val enumber = _list[i]["enumber"]?.toInt() ?: 0
                if (maxEnumber < enumber) maxEnumber = enumber
                if (snumber <= ratio && enumber >= ratio) color_code = _list[i]["color_code"].toString()
            }
            tv_count_view_target.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_actual.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_ratio.setTextColor(Color.parseColor("#" + color_code))
        }

        // Component count 정보 표시
//        var db = DBHelperForComponent(activity)
//        val work_idx = AppGlobal.instance.get_work_idx()

        if ((activity as MainActivity).countViewType == 1) {
            if ((activity as MainActivity).countViewMode == 1) {
                tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
            }
        } else if ((activity as MainActivity).countViewType == 2) {
            tv_component_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
        }
    }

    var handle_cnt = 0
    fun startHandler() {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                updateView()
//                checkBlink()
                if (handle_cnt++ > 15) {
                    handle_cnt = 0
//                    computeCycleTime()
                }
                startHandler()
            }
        }, 1000)
    }

    // Get Color code
    private fun fetchColorData() {
        var list = AppGlobal.instance.get_color_code()

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var map=hashMapOf(
                "idx" to item.getString("idx"),
                "snumber" to item.getString("snumber"),
                "enumber" to item.getString("enumber"),
                "color_name" to item.getString("color_name"),
                "color_code" to item.getString("color_code")
            )
            _list.add(map)
        }
    }
}