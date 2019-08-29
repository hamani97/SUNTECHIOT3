package com.suntech.iot.sewing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.sewing.base.BaseFragment
import com.suntech.iot.sewing.common.AppGlobal
import com.suntech.iot.sewing.db.DBHelperForComponent
import com.suntech.iot.sewing.db.DBHelperForDesign
import com.suntech.iot.sewing.popup.StitchCountEditActivity
import com.suntech.iot.sewing.popup.TrimCountEditActivity
import com.suntech.iot.sewing.popup.TrimStitchCountEditActivity
import com.suntech.iot.sewing.util.OEEUtil
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.layout_bottom_info_3.*
import kotlinx.android.synthetic.main.layout_side_menu.*
import kotlinx.android.synthetic.main.layout_top_menu.*
import org.joda.time.DateTime
import org.json.JSONObject
import kotlin.math.ceil

class CountViewFragment : BaseFragment() {

    private var is_loop: Boolean = false

    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    private var _total_target = 0

//    private var _list_for_wos_adapter: ListWosAdapter? = null
//    private var _list_for_wos: java.util.ArrayList<java.util.HashMap<String, String>> = arrayListOf()
//
//    private var _selected_component_pos = -1

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            computeCycleTime()
            resetDefectiveCount()    // DB에서 기본값을 가져다 화면에 출력
            updateView()
        }
    }

    fun resetDefectiveCount() {
        val db = DBHelperForDesign(activity)
        val count = db.sum_defective_count()
        if (count==null || count<0) {
            tv_defective_count.text = "0"
        } else {
            tv_defective_count.text = count.toString()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_count_view, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_refresh, IntentFilter("need.refresh"))
        is_loop = true
        computeCycleTime()
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
        activity?.tv_title?.visibility = View.VISIBLE

        ll_total_count.visibility = View.VISIBLE
        ll_component_count.visibility = View.GONE

        // 선택된 제품 표시 (TRIM or STITCH)
        if (AppGlobal.instance.get_count_type() == "trim") {
            ll_t_s_block.visibility = View.GONE

            tv_kind_name.text = getString(R.string.label_count_trim_colon) // "TRIM  :  "
            tv_kind_qty.text = "" + (activity as MainActivity).trim_qty
            tv_kind_pairs.text = "" + (activity as MainActivity).trim_pairs

            // bottom view
            tv_trim_qty_bottom.text = AppGlobal.instance.get_trim_qty()
            tv_trim_pairs_bottom.text = AppGlobal.instance.get_trim_pairs()

            tv_trim_qty_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
            tv_trim_pairs_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))

            tv_stitch_qty_bottom.text = "0"
            tv_stitch_delay_time_bottom.text = "0"
            tv_stitch_pairs_bottom.text = "0"

            tv_stitch_qty_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))
            tv_stitch_delay_time_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))
            tv_stitch_pairs_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))

        } else if (AppGlobal.instance.get_count_type() == "stitch") {
            ll_t_s_block.visibility = View.GONE

            tv_kind_name.text = getString(R.string.label_count_stitch_colon) // "STITCH  :  "
            tv_kind_qty.text = "" + (activity as MainActivity).stitch_qty
            tv_kind_pairs.text = "" + (activity as MainActivity).stitch_pairs

//                tv_stitch_qty.text = "STITCH  :  " + AppGlobal.instance.get_stitch_qty_start() + "~" + AppGlobal.instance.get_stitch_qty_end()
//                tv_stitch_qty.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
//                tv_trim_qty.text = "TRIM  :  0"
//                tv_trim_qty.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))

            // bottom view
            tv_stitch_qty_bottom.text = AppGlobal.instance.get_stitch_qty_start() + " ~ " + AppGlobal.instance.get_stitch_qty_end()
            tv_stitch_delay_time_bottom.text = AppGlobal.instance.get_stitch_delay_time()
            tv_stitch_pairs_bottom.text = AppGlobal.instance.get_stitch_pairs()

            tv_stitch_qty_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
            tv_stitch_delay_time_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
            tv_stitch_pairs_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))

            tv_trim_qty_bottom.text = "0"
            tv_trim_pairs_bottom.text= "0"

            tv_trim_qty_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))
            tv_trim_pairs_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))

        } else if (AppGlobal.instance.get_count_type() == "t_s") {
            ll_t_s_block.visibility = View.VISIBLE

            tv_kind_name.text = "T  :  "
            tv_kind_qty.text = "" + (activity as MainActivity).trim_qty
            tv_stitch_qty.text = "" + (activity as MainActivity).stitch_qty

            tv_kind_pairs.text = "" + (activity as MainActivity).t_s_pairs

            // bottom view
            tv_trim_qty_bottom.text = AppGlobal.instance.get_trim_qty2()
            tv_trim_pairs_bottom.text = AppGlobal.instance.get_trim_stitch_pairs()

            tv_stitch_qty_bottom.text = AppGlobal.instance.get_stitch_qty_start2() + " ~ " + AppGlobal.instance.get_stitch_qty_end2()
            tv_stitch_delay_time_bottom.text = "-"
            tv_stitch_pairs_bottom.text = AppGlobal.instance.get_trim_stitch_pairs()

            tv_trim_qty_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
            tv_trim_pairs_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))

            tv_stitch_qty_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
            tv_stitch_delay_time_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))
            tv_stitch_pairs_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
        }

        // Worker info
        if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
            if (AppGlobal.instance.get_message_enable()) {
                Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
//            (activity as MainActivity).changeFragment(0)
            }
        }
        viewWorkInfo()
        computeCycleTime()
    }

    override fun initViews() {
        super.initViews()

        // Init widget
        // Total count view
        tv_count_view_target.text = "0"
        tv_count_view_actual.text = "0"
        tv_count_view_ratio.text = "0%"

        // Server charts
        oee_progress.progress = 0
        availability_progress.progress = 0
        performance_progress.progress = 0
        quality_progress.progress = 0
        tv_oee_rate.text = "0%"
        tv_availability_rate.text = "0%"
        tv_performance_rate.text = "0%"
        tv_quality_rate.text = "0%"

        // Click event
        // Button click in Count view
        btn_go_repair_mode.setOnClickListener {
            (activity as MainActivity).countViewMode = 2
            ll_count_mode.visibility = View.GONE
            ll_repair_mode.visibility = View.VISIBLE

            (activity as MainActivity).repairModeType = 1  // Repair mode 로 세팅
            tv_repair_title.text = getString(R.string.repair_mode) // "REPAIR MODE"
            ll_test_layout.visibility = View.INVISIBLE
            btn_go_test_mode.text = getString(R.string.test_mode) // "TEST MODE"
        }
        btn_go_test_mode.setOnClickListener {
            if ((activity as MainActivity).repairModeType == 1) {
                (activity as MainActivity).repairModeType = 2  // Test mode 로 세팅
                tv_repair_title.text = getString(R.string.test_mode) // "TEST MODE"
                ll_test_layout.visibility = View.VISIBLE
                btn_go_test_mode.text = "CLOSE TEST"
            } else {
                (activity as MainActivity).repairModeType = 1  // Repair mode 로 세팅
                tv_repair_title.text = getString(R.string.repair_mode) // "REPAIR MODE"
                ll_test_layout.visibility = View.INVISIBLE
                btn_go_test_mode.text = getString(R.string.test_mode) // "TEST MODE"
            }
        }
        btn_test_mode_refresh.setOnClickListener {
            (activity as MainActivity).test_trim_qty = 0
            (activity as MainActivity).test_stitch_qty = 0
            tv_test_trim.text = "0"
            tv_test_stitch.text = "0"
        }
        btn_go_count_mode.setOnClickListener {
            (activity as MainActivity).countViewMode = 1
            ll_count_mode.visibility = View.VISIBLE
            ll_repair_mode.visibility = View.GONE
        }
        btn_init_actual.setOnClickListener {
            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
            } else if (AppGlobal.instance.get_factory() == "" || AppGlobal.instance.get_room() == "" || AppGlobal.instance.get_line() == "") {
                Toast.makeText(activity, getString(R.string.msg_no_setting), Toast.LENGTH_SHORT).show()
            } else if (AppGlobal.instance.get_design_info_idx() == "") {
                Toast.makeText(activity, getString(R.string.msg_design_not_selected), Toast.LENGTH_SHORT).show()
            } else {
                if (AppGlobal.instance.get_count_type() == "trim") {
                    val intent = Intent(activity, TrimCountEditActivity::class.java)
                    intent.putExtra("trim", "" + (activity as MainActivity).trim_qty)
                    intent.putExtra("pairs", "" + (activity as MainActivity).trim_pairs)
                    (activity as MainActivity).startActivity(intent, { r, c, m, d ->
                        if (r) {
                            val trim = d?.get("trim")
                            val pairs = d?.get("pairs")
                            if (trim != null && trim != "") {
                                (activity as MainActivity).trim_qty = trim.toInt()
                                tv_kind_qty.text = trim.toString()
                            }
                            if (pairs != null && pairs != "") {
                                (activity as MainActivity).trim_pairs = pairs.toInt()
                                tv_kind_pairs.text = pairs.toString()
                            }
                        }
                    })
                } else if (AppGlobal.instance.get_count_type() == "stitch") {
                    val intent = Intent(activity, StitchCountEditActivity::class.java)
                    intent.putExtra("stitch", "" + (activity as MainActivity).stitch_qty)
                    intent.putExtra("pairs", "" + (activity as MainActivity).stitch_pairs)
                    (activity as MainActivity).startActivity(intent, { r, c, m, d ->
                        if (r) {
                            val stitch = d?.get("stitch")
                            val pairs = d?.get("pairs")
                            if (stitch != null && stitch != "") {
                                (activity as MainActivity).stitch_qty = stitch.toInt()
                                tv_kind_qty.text = stitch.toString()
                            }
                            if (pairs != null && pairs != "") {
                                (activity as MainActivity).stitch_pairs = pairs.toInt()
                                tv_kind_pairs.text = pairs.toString()
                            }
                        }
                    })
                } else if (AppGlobal.instance.get_count_type() == "t_s") {
                    val intent = Intent(activity, TrimStitchCountEditActivity::class.java)
                    intent.putExtra("trim", "" + (activity as MainActivity).trim_qty)
                    intent.putExtra("stitch", "" + (activity as MainActivity).stitch_qty)
                    intent.putExtra("pairs", "" + (activity as MainActivity).t_s_pairs)
                    (activity as MainActivity).startActivity(intent, { r, c, m, d ->
                        if (r) {
                            val trim = d?.get("trim")
                            val stitch = d?.get("stitch")
                            val pairs = d?.get("pairs")
                            if (trim != null && trim != "") {
                                (activity as MainActivity).trim_qty = trim.toInt()
                                tv_kind_qty.text = trim.toString()
                            }
                            if (stitch != null && stitch != "") {
                                (activity as MainActivity).stitch_qty = stitch.toInt()
                                tv_stitch_qty.text = stitch.toString()
                            }
                            if (pairs != null && pairs != "") {
                                (activity as MainActivity).t_s_pairs = pairs.toInt()
                                tv_kind_pairs.text = pairs.toString()
                            }
                        }
                    })
                }
            }
        }
        btn_defective_plus.setOnClickListener {
            val cur_shift: JSONObject?= AppGlobal.instance.get_current_shift_time()

            // 작업 시간인지 확인
            if (cur_shift == null) {
                Toast.makeText(activity, getString(R.string.msg_not_start_work), Toast.LENGTH_SHORT).show()
            } else {
                val work_idx = AppGlobal.instance.get_product_idx()
                if (work_idx == "") {
                    Toast.makeText(activity, getString(R.string.msg_design_not_selected), Toast.LENGTH_SHORT).show()
                } else {
                    val db = DBHelperForDesign(activity)
                    val row = db.get(work_idx)
                    var seq = row!!["seq"].toString().toInt()
                    if (row == null || seq == null) {
                        seq = 1
                    }
                    val uri = "/defectivedata.php"
                    var params = listOf(
                        "mac_addr" to AppGlobal.instance.getMACAddress(),
                        "didx" to AppGlobal.instance.get_design_info_idx(),
                        "defective_idx" to "99",
                        "cnt" to "1",
                        "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
                        "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                        "factory_idx" to AppGlobal.instance.get_room_idx(),
                        "line_idx" to AppGlobal.instance.get_line_idx(),
                        "seq" to seq
                    )
                    getBaseActivity().request(activity, uri, true, false, params, { result ->
                        val code = result.getString("code")

                        Toast.makeText(activity, result.getString("msg"), Toast.LENGTH_SHORT).show()

                        if (code == "00") {
                            val item = db.get(work_idx)
                            val defective = if (item != null) item["defective"].toString().toInt() else 0
                            db.updateDefective(work_idx, defective + 1)
                            resetDefectiveCount()    // DB에서 기본값을 가져다 화면에 출력
                        }
                    })
                }
            }
        }

        viewWorkInfo()
        fetchColorData()    // Get Color
    }

    fun viewWorkInfo() {
        // WOS INFO 하단 bottom
        tv_idx?.text = AppGlobal.instance.get_design_info_idx()
        tv_cycle_time?.text = AppGlobal.instance.get_cycle_time().toString()
        tv_model?.text = AppGlobal.instance.get_model()
        tv_material?.text = AppGlobal.instance.get_material_way()
        tv_component?.text = AppGlobal.instance.get_component()
    }

    // 해당 시간에만 카운트 값을 변경하기 위한 변수
    // 타이밍 값을 미리 계산해 놓는다.
    var _current_cycle_time = 300   // 5분

    // Total target을 표시할 사이클 타임을 계산한다.
    private fun computeCycleTime() {
        val target_type = AppGlobal.instance.get_target_type()  // setting menu 메뉴에서 선택한 타입
        if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
            _current_cycle_time = AppGlobal.instance.get_cycle_time()
            if (_current_cycle_time == 0 ) _current_cycle_time = 30
            else if (_current_cycle_time < 10) _current_cycle_time = 10        // 너무 자주 리프레시 되는걸 막기위함 (10초)
        } else {
            _current_cycle_time = 180   // 3분
        }
        Log.e("Count Time", "Current time = " + _current_cycle_time.toString())
    }
//    private fun computeCycleTime() {
//        force_count = true
//        val target = AppGlobal.instance.get_current_shift_target_cnt()
//        if (target == null || target == "") {
//            // 작업 시간이 아니므로 값을 초기화 한다.
//            _current_cycle_time = 15
//            _total_target = 0
//            return
//        }
//
//        val total_target = target.toInt()
//        val target_type = AppGlobal.instance.get_target_type()
//
//        if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
//            val shift_total_time = AppGlobal.instance.get_current_shift_total_time()
//            _current_cycle_time = if (total_target > 0) (shift_total_time / total_target) else 0
//            if (_current_cycle_time < 5) _current_cycle_time = 5        // 너무 자주 리프레시 되는걸 막기위함
//
//        } else if (target_type=="device_per_hourly" || target_type=="server_per_hourly") {
//            _current_cycle_time = 86400
//
//        } else if (target_type=="device_per_day_total" || target_type=="server_per_day_total") {
//            _current_cycle_time = 86400
//        }
//    }

    // 무조건 계산해야 할경우 true
//    var force_count = true

//    private fun countTarget() {
//        if (_current_cycle_time >= 86400 && force_count == false) return
//
//        val shift_now_time = AppGlobal.instance.get_current_shift_accumulated_time()
//        if (shift_now_time <= 0 && force_count == false) return
//
//        if (shift_now_time % _current_cycle_time == 0 || force_count) {
//            force_count = false
//
//            var target = AppGlobal.instance.get_current_shift_target_cnt()
//            if (target == null || target == "") target = "0"
//
//            var total_target = target.toInt()
//
//            val target_type = AppGlobal.instance.get_target_type()
//
//            if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
//                val target = (shift_now_time / _current_cycle_time).toInt() + 1
//                _total_target = if (target > total_target) total_target else target
//
//            } else if (target_type=="device_per_hourly" || target_type=="server_per_hourly") {
//                val shift_total_time = AppGlobal.instance.get_current_shift_total_time()    // 현시프트의 총 시간
//                val target_per_hour = total_target.toFloat() / shift_total_time.toFloat() * 3600    // 시간당 만들어야 할 갯수
//                val target = ((shift_now_time / 3600).toInt() * target_per_hour + target_per_hour).toInt()    // 현 시간에 만들어야 할 갯수
//                _total_target = if (target > total_target) total_target else target
//
//                Log.e("test -----", "target_per_hour = " + target_per_hour + ", _total_target = " + _total_target + ", _current_cycle_time = " + _current_cycle_time)
//
//            } else if (target_type=="device_per_day_total" || target_type=="server_per_day_total") {
//                _total_target = total_target
//            }
//        }
//    }

    // 변화를 체크하기 위함
    var last_total_target = -1
    var last_total_actual = -1

    // 값에 변화가 생길때만 화면을 리프레쉬 하기 위한 변수
    var _current_target_count = -1
    var _current_actual_count = -1
    var _current_compo_target_count = -1
    var _current_compo_actual_count = -1

    private fun updateView() {
        // 기본 출력
        tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")

        // 타입에 맞는 기본 출력
        if (AppGlobal.instance.get_count_type() == "trim") {
            val pairs = AppGlobal.instance.get_trim_pairs()
            var pairs_str = ""
            when (pairs) {
                "1/2" -> pairs_str = "/2"
                "1/4" -> pairs_str = "/4"
                "1/8" -> pairs_str = "/8"
            }
            tv_kind_qty.text = "" + (activity as MainActivity).trim_qty
            tv_kind_pairs.text = "" + (activity as MainActivity).trim_pairs + pairs_str
        } else if (AppGlobal.instance.get_count_type() == "stitch") {
            val pairs = AppGlobal.instance.get_stitch_pairs()
            var pairs_str = ""
            when (pairs) {
                "1/2" -> pairs_str = "/2"
                "1/4" -> pairs_str = "/4"
                "1/8" -> pairs_str = "/8"
            }
            tv_kind_qty.text = "" + (activity as MainActivity).stitch_qty
            tv_kind_pairs.text = "" + (activity as MainActivity).stitch_pairs + pairs_str
        } else if (AppGlobal.instance.get_count_type() == "t_s") {
            val pairs = AppGlobal.instance.get_trim_stitch_pairs()
            var pairs_str = ""
            when (pairs) {
                "1/2" -> pairs_str = "/2"
                "1/4" -> pairs_str = "/4"
                "1/8" -> pairs_str = "/8"
            }
            tv_kind_qty.text = "" + (activity as MainActivity).trim_qty
            tv_stitch_qty.text = "" + (activity as MainActivity).stitch_qty
            tv_kind_pairs.text = "" + (activity as MainActivity).t_s_pairs + pairs_str
        }

        drawChartView2()

        // 현재 시프트의 휴식시간 미리 계산
        val shift_time = AppGlobal.instance.get_current_shift_time()
        if (shift_time == null) {
            refreshScreen("", 0, 0, 0)
            return
        }

        val work_stime = shift_time["work_stime"].toString()
        val work_etime = shift_time["work_etime"].toString()
        val shift_idx = shift_time["shift_idx"].toString()


        // 디자인이 선택되었는지 체크
        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") {
            if (AppGlobal.instance.get_message_enable() && (DateTime().millis/1000) % 10 == 0L) {  // 10초마다 출력
                Toast.makeText(activity, getString(R.string.msg_design_not_selected), Toast.LENGTH_SHORT).show()
            }
//            refreshScreen(shift_idx, 0, 0)
            return
        }

        var db = DBHelperForDesign(activity)

        // DB에서 디자인 데이터를 가져온다.
        val db_item = db.get(work_idx)
        if (db_item == null || db_item.toString() == "") {
//            refreshScreen(shift_idx, 0, 0)
            return
        }


        // 가져온 DB가 현 시프트의 정보가 아니라면 리턴
        if (db_item["end_dt"].toString() == null) {
            if (db_item["end_dt"].toString() < work_stime) return
        } else {
            if (db_item["start_dt"].toString() < work_stime) return
        }


        val now = DateTime()        // 현재
        val start_dt = OEEUtil.parseDateTime(db_item["start_dt"].toString())    // 디자인의 시작시간
        val shift_end_dt = OEEUtil.parseDateTime(work_etime)    // 시프트의 종료 시간

        // 설정되어 있는 휴식 시간
        val _planned1_stime = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString())
        val _planned1_etime = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString())
        val _planned2_stime = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString())
        val _planned2_etime = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString())


        // 현 디자인의 휴식 시간 계산
//        var d1 = 0
//        var d2 = 0


        val target_type = AppGlobal.instance.get_target_type()  // setting menu 메뉴에서 선택한 타입
        var current_cycle_time = AppGlobal.instance.get_cycle_time()

        var shift_total_target = 0
        var total_target = 0
        var total_actual = 0

        if (target_type.substring(0, 6) == "server") {

            // 전체 디자인을 가져온다.
            var db_list = db.gets()

            for (i in 0..((db_list?.size ?: 1) - 1)) {

                val item = db_list?.get(i)
                val work_idx2 = item?.get("work_idx").toString()
                val actual2 = item?.get("actual").toString().toInt()
                val target2 = item?.get("target").toString().toInt()

                total_actual += actual2


                if (work_idx == work_idx2) {        // 현재 진행중인 디자인

                    if (current_cycle_time == 0) continue

                    // 끝나는 시간까지 계산 (시프트의 총 타겟수를 구하기 위해 무조건 계산함)
                    val d1 = AppGlobal.instance.compute_time(start_dt, shift_end_dt, _planned1_stime, _planned1_etime)
                    val d2 = AppGlobal.instance.compute_time(start_dt, shift_end_dt, _planned2_stime, _planned2_etime)

                    // 디자인의 시작부터 시프트 종료시간까지 (초)
                    val work_time = ((shift_end_dt.millis - start_dt.millis) / 1000) - d1 - d2 -1

                    val count = (work_time / current_cycle_time).toInt() + 1 // 현 시간에 만들어야 할 갯수
                    shift_total_target += count

                    if (target_type == "server_per_day_total") {
                        total_target += count
                        // target값이 변형되었으면 업데이트
                        if (work_idx != null && target2 != count) {
                            db.updateWorkTarget(work_idx, count, count)
                        }
                    } else if (target_type == "server_per_accumulate") {
                        val d1 = AppGlobal.instance.compute_time(start_dt, now, _planned1_stime, _planned1_etime)
                        val d2 = AppGlobal.instance.compute_time(start_dt, now, _planned2_stime, _planned2_etime)

                        // 디자인의 시작부터 현재까지 시간(초)
                        val work_time = ((now.millis - start_dt.millis) / 1000) - d1 - d2 -1

                        val count = (work_time / current_cycle_time).toInt() + 1 // 현 시간에 만들어야 할 갯수
                        total_target += count

                        // target값이 변형되었으면 업데이트
                        if (work_idx != null && target2 != count) {
                            db.updateWorkTarget(work_idx, count, count)
                        }
                    }

//                    if (target_type == "server_per_accumulate") {
//                        val d1 = AppGlobal.instance.compute_time(start_dt, now, _planned1_stime, _planned1_etime)
//                        val d2 = AppGlobal.instance.compute_time(start_dt, now, _planned2_stime, _planned2_etime)
//
//                        // 디자인의 시작부터 현재까지 시간(초)
//                        val work_time = ((now.millis - start_dt.millis) / 1000) - d1 - d2 -1
//
//                        val count = (work_time / current_cycle_time).toInt() + 1 // 현 시간에 만들어야 할 갯수
//                        total_target += count
//
//                        // target값이 변형되었으면 업데이트
//                        if (work_idx != null && target2 != count) {
//                            db.updateWorkTarget(work_idx, count, count)
//                        }
//
//                    } else if (target_type == "server_per_day_total") {
//                        val d1 = AppGlobal.instance.compute_time(start_dt, shift_end_dt, _planned1_stime, _planned1_etime)
//                        val d2 = AppGlobal.instance.compute_time(start_dt, shift_end_dt, _planned2_stime, _planned2_etime)
//
//                        // 디자인의 시작부터 시프트 종료시간까지 (초)
//                        val work_time = ((shift_end_dt.millis - start_dt.millis) / 1000) - d1 - d2 -1
//
//                        val count = (work_time / current_cycle_time).toInt() + 1 // 현 시간에 만들어야 할 갯수
//                        total_target += count
//
//                        // target값이 변형되었으면 업데이트
//                        if (work_idx != null && target2 != count) {
//                            db.updateWorkTarget(work_idx, count, count)
//                        }
//                    }

                } else {        // 지난 디자인 작업

                    val end_dt2 = OEEUtil.parseDateTime(item?.get("end_dt"))
                    if (end_dt2 != null) {
                        val start_dt2 = OEEUtil.parseDateTime(item?.get("start_dt"))
                        val cycle_time2 = item?.get("cycle_time").toString().toInt()

                        if (start_dt2 != null && cycle_time2 > 0) {
                            // 휴식 시간을 뺀 시간 계산
                            val d1 = AppGlobal.instance.compute_time(start_dt2, end_dt2, _planned1_stime, _planned1_etime)
                            val d2 = AppGlobal.instance.compute_time(start_dt2, end_dt2, _planned2_stime, _planned2_etime)

                            val work_time2 = ((end_dt2.millis - start_dt2.millis) / 1000) - d1 - d2 -1

                            val count = (work_time2 / cycle_time2).toInt() + 1 // 시작할때 1부터 시작이므로 1을 더함
                            total_target += count   // 현재 계산된 카운트를 더한다.
                            shift_total_target += count   // 현재 계산된 카운트를 시트프 총합에 더한다.

                            // target값이 변형되었으면 업데이트
                            if (work_idx2 != null && target2 != count) {
//                                Log.e("DB", i.toString() + " = " + item.toString())
//                                Log.e("DB", i.toString() + " = db target = " + target2 + ", new target = " + count)
                                db.updateWorkTarget(work_idx2, count, count)
                            }
                        }
                    }
                }
            }
        } else if (target_type.substring(0, 6) == "device") {
            when (shift_idx) {
                "1" -> total_target = AppGlobal.instance.get_target_manual_shift("1").toInt()
                "2" -> total_target = AppGlobal.instance.get_target_manual_shift("2").toInt()
                "3" -> total_target = AppGlobal.instance.get_target_manual_shift("3").toInt()
            }
        }

        // 값에 변화가 생겼을 때만 리프레시
        refreshScreen(shift_idx, total_actual, total_target, shift_total_target)
    }

    private fun refreshScreen(shift_idx:String, total_actual:Int, total_target:Int, shift_total_target:Int) {
        // 값에 변화가 생겼을 때만 리프레시
        if (total_target != last_total_target || total_actual != last_total_actual) {
            var ratio = 0
            var ratio_txt = "N/A"

            if (total_target > 0) {
                ratio = (total_actual.toFloat() / total_target.toFloat() * 100).toInt()
                if (ratio > 999) ratio = 999
                ratio_txt = "" + ratio + "%"
            }

            tv_count_view_target.text = "" + total_target
            tv_count_view_actual.text = "" + total_actual
            tv_count_view_ratio.text = ratio_txt

            var color_code = "ffffff"

            for (i in 0..(_list.size - 1)) {
                val snumber = _list[i]["snumber"]?.toInt() ?: 0
                val enumber = _list[i]["enumber"]?.toInt() ?: 0
                if (snumber <= ratio && enumber >= ratio) color_code = _list[i]["color_code"].toString()
            }
            tv_count_view_target.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_actual.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_ratio.setTextColor(Color.parseColor("#" + color_code))

            //
            AppGlobal.instance.set_current_shift_actual_cnt(total_actual)
            tv_report_count?.text = "" + total_actual

            // 타겟 수량이 바뀌면 서버에 통보한다.
            if (total_target != last_total_target) {
                if (shift_idx != "") {
                    updateCurrentWorkTarget(shift_idx, total_target, shift_total_target)
                }
            }

            // 최종값 업데이트
            last_total_target = total_target
            last_total_actual = total_actual
        }
    }

    // 현재 target을 서버에 저장
    private fun updateCurrentWorkTarget(shift_idx: String, target: Int, shift_target: Int) {
        Log.e("updateCurrentWorkTarget", "total_target=" + target + ", shift_total_target=" + shift_target)
        if (target >= 0) {
            // 신서버용
            val uri = "/Starget.php"
            var params = listOf(
                "mac_addr" to AppGlobal.instance.getMACAddress(),
                "didx" to AppGlobal.instance.get_design_info_idx(),
                "target" to shift_target,
                "shift_idx" to  shift_idx
            )

            getBaseActivity().request(activity, uri, true,false, params, { result ->
                var code = result.getString("code")
                var msg = result.getString("msg")
                Log.e("Starget result", "= " + msg.toString())
                if(code != "00"){
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // 값에 변화가 생길때만 화면을 리프레쉬 하기 위한 변수
    var _availability = ""
    var _performance = ""
    var _quality = ""

    private fun drawChartView2() {
        var availability = AppGlobal.instance.get_availability()
        var performance = AppGlobal.instance.get_performance()
        var quality = AppGlobal.instance.get_quality()

        if (availability=="") availability = "0"
        if (performance=="") performance = "0"
        if (quality=="") quality = "0"

        // 값에 변화가 있을때만 갱신
        if (_availability != availability || _performance != performance || _quality != quality) {
            _availability = availability
            _performance = performance
            _quality = quality

            Log.e("drawChartView2", "oee graph redraw")

            var oee = availability.toFloat() * performance.toFloat() * quality.toFloat() / 10000.0f
            var oee2 = String.format("%.1f", oee)
            oee2 = oee2.replace(",", ".")//??

            tv_oee_rate.text = oee2 + "%"
            tv_availability_rate.text = availability + "%"
            tv_performance_rate.text = performance + "%"
            tv_quality_rate.text = quality + "%"

            val oee_int = oee.toInt()
            val availability_int = ceil(availability.toFloat()).toInt()
            val performance_int = ceil(performance.toFloat()).toInt()
            val quality_int = ceil(quality.toFloat()).toInt()

            oee_progress.progress = oee_int
            availability_progress.progress = availability_int
            performance_progress.progress = performance_int
            quality_progress.progress = quality_int

            var oee_color_code = "ff0000"
            var availability_color_code = "ff0000"
            var performance_color_code = "ff0000"
            var quality_color_code = "ff0000"

            for (i in 0..(_list.size - 1)) {
                val snumber = _list[i]["snumber"]?.toInt() ?: 0
                val enumber = _list[i]["enumber"]?.toInt() ?: 0
                if (snumber <= oee_int && enumber >= oee_int) oee_color_code = _list[i]["color_code"].toString()
                if (snumber <= availability_int && enumber >= availability_int) availability_color_code = _list[i]["color_code"].toString()
                if (snumber <= performance_int && enumber >= performance_int) performance_color_code = _list[i]["color_code"].toString()
                if (snumber <= quality_int && enumber >= quality_int) quality_color_code = _list[i]["color_code"].toString()
            }

            oee_progress.progressStartColor = Color.parseColor("#" + oee_color_code)
            oee_progress.progressEndColor = Color.parseColor("#" + oee_color_code)

            availability_progress.progressStartColor = Color.parseColor("#" + availability_color_code)
            availability_progress.progressEndColor = Color.parseColor("#" + availability_color_code)

            performance_progress.progressStartColor = Color.parseColor("#" + performance_color_code)
            performance_progress.progressEndColor = Color.parseColor("#" + performance_color_code)

            quality_progress.progressStartColor = Color.parseColor("#" + quality_color_code)
            quality_progress.progressEndColor = Color.parseColor("#" + quality_color_code)
        }
    }

//    var handle_cnt = 0
    fun startHandler() {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                updateView()
                checkBlink()
//                if (handle_cnt++ > 15) {
//                    handle_cnt = 0
//                    computeCycleTime()
//                }
                startHandler()
            }
        }, 1000)
    }

    var blink_cnt = 0
    private fun checkBlink() {
//        if (AppGlobal.instance.get_with_component() == false) return
//
//        var is_toggle = false
//        if (AppGlobal.instance.get_screen_blink()) {
//            if (_current_compo_target_count != -1 || _current_compo_actual_count != -1) {
//                if (_current_compo_target_count - _current_compo_actual_count <= AppGlobal.instance.get_remain_number()) {
//                    blink_cnt = 1 - blink_cnt
//                    is_toggle = true
//                }
//            }
//        }
//        if (is_toggle && blink_cnt==1) {
//            if ((activity as MainActivity).countViewType == 1) {
//                ll_btn_wos_count.setBackgroundColor(Color.parseColor("#" + AppGlobal.instance.get_blink_color()))
//            } else {
//                ll_component_count.setBackgroundColor(Color.parseColor("#" + AppGlobal.instance.get_blink_color()))
//            }
//        } else {
//            if ((activity as MainActivity).countViewType == 1) {
//                ll_btn_wos_count.setBackgroundResource(R.color.colorBlack2)
//            } else {
//                ll_component_count.setBackgroundResource(R.color.colorBackground)
//            }
//        }
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