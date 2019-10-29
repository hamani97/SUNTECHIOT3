package com.suntech.iot.sewing.popup

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.suntech.iot.sewing.R
import com.suntech.iot.sewing.base.BaseActivity
import com.suntech.iot.sewing.common.AppGlobal
import com.suntech.iot.sewing.db.DBHelperForDesign
import kotlinx.android.synthetic.main.activity_trim_count_edit.*

class TrimCountEditActivity : BaseActivity() {

    private var _trim = 0
    private var _pairs = 0
    private var _defective = 0

    private var _max_trim = 8
    private var _max_pairs = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trim_count_edit)

        val trim = intent.getStringExtra("trim")
        val pairs = intent.getStringExtra("pairs")

        _trim = trim.toInt()
        _pairs = pairs.toInt()

        _max_trim = AppGlobal.instance.get_trim_qty().toInt()
        val tmp = AppGlobal.instance.get_trim_pairs()
        var pairs_str = ""
        when (tmp) {
            "1" -> { _max_pairs = 1 }
            "1/2" -> { _max_pairs = 2; pairs_str = "/2" }
            "1/4" -> { _max_pairs = 4; pairs_str = "/4" }
            "1/8" -> { _max_pairs = 8; pairs_str = "/8" }
        }

        tv_trim_count.setText(trim)
        et_trim_count.setText(trim)

        tv_trim_pairs.setText(pairs + pairs_str)
        et_trim_pairs.setText(pairs)

        // defective
        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") {
            ToastOut(this, R.string.msg_design_not_selected, true)
            _defective = 0
        } else {
            val db = DBHelperForDesign(this)
            val row = db.get(work_idx)
            if (row != null) {
                _defective = row!!["defective"].toString().toInt()
            }
        }
        tv_defective_count?.setText(_defective.toString())
        et_defective_count?.setText(_defective.toString())

        btn_trim_count_plus.setOnClickListener {
            if (_trim + 1 < _max_trim) {
                _trim++
                et_trim_count.setText(_trim.toString())
            }
        }
        btn_trim_count_minus.setOnClickListener {
            if (_trim > 0) {
                _trim--
                et_trim_count.setText(_trim.toString())
            }
        }
        btn_trim_pairs_plus.setOnClickListener {
            if (_pairs + 1 < _max_pairs) {
                _pairs++
                et_trim_pairs.setText(_pairs.toString())
            }
        }
        btn_trim_pairs_minus.setOnClickListener {
            if (_pairs > 0) {
                _pairs--
                et_trim_pairs.setText(_pairs.toString())
            }
        }
        btn_defective_plus.setOnClickListener {
            _defective++
            et_defective_count?.setText(_defective.toString())
        }
        btn_defective_minus.setOnClickListener {
            if (_defective > 0) {
                _defective--
                et_defective_count?.setText(_defective.toString())
            }
        }

        btn_confirm.setOnClickListener {
            // defective
            val work_idx = AppGlobal.instance.get_product_idx()
            if (work_idx == "") {
                ToastOut(this, R.string.msg_design_not_selected, true)
            } else {
                val db = DBHelperForDesign(this)
                val row = db.get(work_idx)
                if (row != null) {
                    val defective = row!!["defective"].toString().toInt()
                    var seq = row!!["seq"].toString().toInt()
                    if (seq == null) seq = 1

                    val cnt = (_defective - defective)

                    if (cnt != 0) {

                        val uri = "/defectivedata.php"
                        var params = listOf(
                            "mac_addr" to AppGlobal.instance.getMACAddress(),
                            "didx" to AppGlobal.instance.get_design_info_idx(),
                            "defective_idx" to "99",
                            "cnt" to cnt.toString(),
                            "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
                            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                            "factory_idx" to AppGlobal.instance.get_room_idx(),
                            "line_idx" to AppGlobal.instance.get_line_idx(),
                            "seq" to seq
                        )
                        request(this, uri, true, false, params, { result ->
                            val code = result.getString("code")
                            if (code == "00") {
                                db.updateDefective(work_idx, _defective)
                            } else {
                                ToastOut(this, result.getString("msg"), true)
                            }
                        })
                    }
                }
            }

            finish(true, 0, "ok", hashMapOf("trim" to ""+_trim, "pairs" to ""+_pairs))
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }

    fun parentSpaceClick(view: View) {
        var view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
