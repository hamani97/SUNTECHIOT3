package com.suntech.iot.sewing.popup

import android.os.Bundle
import com.suntech.iot.sewing.R
import com.suntech.iot.sewing.base.BaseActivity
import com.suntech.iot.sewing.common.AppGlobal
import kotlinx.android.synthetic.main.activity_stitch_count_edit.*
import kotlinx.android.synthetic.main.activity_stitch_count_edit.btn_cancel
import kotlinx.android.synthetic.main.activity_stitch_count_edit.btn_confirm

class StitchCountEditActivity : BaseActivity() {

    private var _stitch = 0
    private var _pairs = 0

    private var _max_pairs = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stitch_count_edit)

        val stitch = intent.getStringExtra("stitch")
        val pairs = intent.getStringExtra("pairs")

        _stitch = stitch.toInt()
        _pairs = pairs.toInt()

        val tmp = AppGlobal.instance.get_trim_pairs()
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

        tv_stitch_count.setText(stitch)
        et_stitch_count.setText(stitch)

        tv_stitch_pairs.setText(pairs + pairs_str)
        et_stitch_pairs.setText(pairs)

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
        btn_stitch_pairs_plus.setOnClickListener {
            if (_pairs + 1 < _max_pairs) {
                _pairs++
                et_stitch_pairs.setText(_pairs.toString())
            }
        }
        btn_stitch_pairs_minus.setOnClickListener {
            if (_pairs > 0) {
                _pairs--
                et_stitch_pairs.setText(_pairs.toString())
            }
        }
        btn_confirm.setOnClickListener {
            finish(true, 0, "ok", hashMapOf("stitch" to ""+_stitch, "pairs" to ""+_pairs))
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }
}
