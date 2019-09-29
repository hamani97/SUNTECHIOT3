package com.suntech.iot.sewing.popup

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.suntech.iot.sewing.R
import com.suntech.iot.sewing.base.BaseActivity
import com.suntech.iot.sewing.common.AppGlobal
import kotlinx.android.synthetic.main.activity_trim_stitch_count_edit.*

class TrimStitchCountEditActivity : BaseActivity() {

    private var _trim = 0
    private var _stitch = 0
    private var _pairs = 0

    private var _max_trim = 8
    private var _max_pairs = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trim_stitch_count_edit)

        val trim = intent.getStringExtra("trim")
        val stitch = intent.getStringExtra("stitch")
        val pairs = intent.getStringExtra("pairs")

        _trim = trim.toInt()
        _stitch = stitch.toInt()
        _pairs = pairs.toInt()

        _max_trim = AppGlobal.instance.get_trim_qty2().toInt()

        val tmp = AppGlobal.instance.get_trim_stitch_pairs()
        var pairs_str = ""
        when (tmp) {
            "1" -> _max_pairs = 1
            "1/2" -> { _max_pairs = 2
                pairs_str = "/2" }
            "1/4" -> { _max_pairs = 4
                pairs_str = "/4" }
            "1/8" -> { _max_pairs = 8
                pairs_str = "/8" }
        }

        tv_trim_count.setText(trim)
        et_trim_count.setText(trim)

        tv_stitch_count.setText(stitch)
        et_stitch_count.setText(stitch)

        tv_trim_stitch_pairs.setText(pairs + pairs_str)
        et_trim_stitch_pairs.setText(pairs)

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
        btn_stitch_count_plus.setOnClickListener {
            _stitch++
            et_stitch_count.setText(_stitch.toString())
        }
        btn_stitch_count_minus.setOnClickListener {
            if (_stitch > 0) {
                _stitch--
                et_stitch_count.setText(_stitch.toString())
            }
        }
        btn_trim_stitch_pairs_plus.setOnClickListener {
            if (_pairs + 1 < _max_pairs) {
                _pairs++
                et_trim_stitch_pairs.setText(_pairs.toString())
            }
        }
        btn_trim_stitch_pairs_minus.setOnClickListener {
            if (_pairs > 0) {
                _pairs--
                et_trim_stitch_pairs.setText(_pairs.toString())
            }
        }
        btn_confirm.setOnClickListener {
            finish(true, 0, "ok", hashMapOf("trim" to ""+_trim, "stitch" to ""+_stitch, "pairs" to ""+_pairs))
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
