package com.suntech.iot.sewing.popup

import android.os.Bundle
import android.widget.Toast
import com.suntech.iot.sewing.R
import com.suntech.iot.sewing.base.BaseActivity
import com.suntech.iot.sewing.common.AppGlobal
import com.suntech.iot.sewing.db.DBHelperForReport
import kotlinx.android.synthetic.main.activity_actual_total_count_edit.*
import org.joda.time.DateTime
import org.json.JSONObject

class ActualTotalCountEditActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actual_total_count_edit)
        initView()
    }

    private fun initView() {
        val actual = AppGlobal.instance.get_current_shift_actual_cnt()

        tv_work_actual?.setText(actual.toString())
        et_defective_qty?.setText(actual.toString())

        btn_actual_count_edit_plus.setOnClickListener {
            et_defective_qty.setText((et_defective_qty.text.toString().toInt() + 1).toString())
        }
        btn_actual_count_edit_minus.setOnClickListener {
            var value = et_defective_qty.text.toString().toInt()
            if (value > 0) {
                value--
                et_defective_qty.setText(value.toString())
            }
        }
        btn_confirm.setOnClickListener {
            val value = et_defective_qty.text.toString()
            sendCountData(value)
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }

    private fun sendCountData(count: String) {

        if (AppGlobal.instance.get_server_ip()=="") {
            Toast.makeText(this, getString(R.string.msg_has_not_server_info), Toast.LENGTH_SHORT).show()
            return
        }

        // 토탈 카운트 재계산
        val origin_actual = AppGlobal.instance.get_current_shift_actual_cnt()

        val new_actual = count.toInt()                  // 사용자가 입력한 새 Actual 값
        val inc_count = new_actual - origin_actual      // 사용자가 입력한 Actual로 계산된 증분값

        var shift_idx = AppGlobal.instance.get_current_shift_idx()
        if (shift_idx == "") shift_idx = "0"
        val seq = 1

        val uri = "/Scount.php"
        var params = listOf(
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "count" to inc_count.toString(),
            "total_count" to new_actual,
            "shift_idx" to  shift_idx,
            "seq" to seq)

        request(this, uri, true,false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                // Total count 의 Actual 값 갱신
                AppGlobal.instance.set_current_shift_actual_cnt(new_actual)

                // Report DB 값 수정
                ReportDBUpdate(inc_count)

                ToastOut(this, result.getString("msg"))

                finish(true, 0, "ok", null)
            } else {
                Toast.makeText(this, result.getString("msg"), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun ReportDBUpdate(inc_count: Int) {
        // 작업 시간인지 확인
        val cur_shift: JSONObject?= AppGlobal.instance.get_current_shift_time()

        if (cur_shift != null) {
            val now = cur_shift["date"]
            val date = now.toString()
            val houly = DateTime().toString("HH")
            val shift_idx = cur_shift["shift_idx"]      // 현재 작업중인 Shift

            val _report_db = DBHelperForReport(this)    // 날짜의 Shift별 한시간 간격의 Actual 수량 저장

            val rep = _report_db.get(date, houly, shift_idx.toString())
            if (rep == null) {
                _report_db.add(date, houly, shift_idx.toString(), inc_count, 0)
            } else {
                val idx = rep!!["idx"].toString()
                val actual = rep!!["actual"].toString().toInt() + inc_count
                _report_db.updateActual(idx, actual)
            }
        }
    }
}
