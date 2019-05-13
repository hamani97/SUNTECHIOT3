package com.suntech.iot.sewing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import com.suntech.iot.sewing.base.BaseActivity
import com.suntech.iot.sewing.common.AppGlobal
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*

class SettingActivity : BaseActivity() {

    private var tab_pos: Int = 1
    private var _selected_target_type: String = "device"
    private var _selected_blink_color: String = AppGlobal.instance.get_blink_color()

    private var _selected_factory_idx: String = ""
    private var _selected_room_idx: String = ""
    private var _selected_line_idx: String = ""
    private var _selected_mc_no_idx: String = ""
    private var _selected_mc_model_idx: String = ""

    val _broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.getAction()
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false))
                    btn_wifi_state.isSelected = true
                else
                    btn_wifi_state.isSelected = false

            } else if (action.equals("need.refresh.server.state")) {
                val state = intent.getStringExtra("state")
                if (state == "Y") {
                    btn_server_state.isSelected = true
                } else btn_server_state.isSelected = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initView()
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(_broadcastReceiver)
    }

    private fun initView() {
        tv_title.setText(R.string.title_setting)

        // system setting
        _selected_factory_idx = AppGlobal.instance.get_factory_idx()
        _selected_room_idx = AppGlobal.instance.get_room_idx()
        _selected_line_idx = AppGlobal.instance.get_line_idx()
        _selected_mc_no_idx = AppGlobal.instance.get_mc_no_idx()
        _selected_mc_model_idx = AppGlobal.instance.get_mc_model_idx()

        // widget
//        tv_setting_wifi.text = AppGlobal.instance.getWiFiSSID(this)
//        tv_setting_ip.text = AppGlobal.instance.get_local_ip()
//        tv_setting_mac.text = AppGlobal.instance.get_mac_address()
//        tv_setting_factory.text = AppGlobal.instance.get_factory()
//        tv_setting_room.text = AppGlobal.instance.get_room()
//        tv_setting_line.text = AppGlobal.instance.get_line()
//        tv_setting_mc_model.text = AppGlobal.instance.get_mc_model()
//        tv_setting_mc_no1.setText(AppGlobal.instance.get_mc_no1())
//        et_setting_mc_serial.setText(AppGlobal.instance.get_mc_serial())

        et_setting_server_ip.setText(AppGlobal.instance.get_server_ip())
        et_setting_port.setText(AppGlobal.instance.get_server_port())

        sw_long_touch.isChecked = AppGlobal.instance.get_long_touch()
        sw_sound_at_count.isChecked = AppGlobal.instance.get_sound_at_count()
        sw_without_component.isChecked = AppGlobal.instance.get_without_component()
        sw_screen_blink_effect.isChecked = AppGlobal.instance.get_screen_blink()

        // 깜박임 기능. 0일때는 10으로 초기화
        val remain = if (AppGlobal.instance.get_remain_number()==0) "10" else AppGlobal.instance.get_remain_number().toString()
        et_remain_number.setText(remain)

        if (_selected_blink_color == "") _selected_blink_color = "ff0000"
        blinkColorChange(_selected_blink_color)

        blink_color_f8ad13.setOnClickListener {
            blinkColorChange("f8ad13")
        }
        blink_color_ff0000.setOnClickListener {
            blinkColorChange("ff0000")
        }
        blink_color_0079BA.setOnClickListener {
            blinkColorChange("0079BA")
        }
        blink_color_888888.setOnClickListener {
            blinkColorChange("888888")
        }

        // count setting
//        _selected_layer_0 = AppGlobal.instance.get_layer_pairs("0")     // 1 layer = 0.5 pair
//        _selected_layer_1 = AppGlobal.instance.get_layer_pairs("1")     // 2 layer = 1 pair
//        _selected_layer_2 = AppGlobal.instance.get_layer_pairs("2")     // 4 layer = 2 pairs
//        _selected_layer_3 = AppGlobal.instance.get_layer_pairs("3")     // 6 layer = 3 pairs
//        _selected_layer_4 = AppGlobal.instance.get_layer_pairs("4")     // 8 layer = 4 pairs
//        _selected_layer_5 = AppGlobal.instance.get_layer_pairs("5")     // 10 layer = 5 pairs
//
//        // widget
//        if (_selected_layer_0 != "") tv_layer_0.text = addPairText(_selected_layer_0)
//        if (_selected_layer_1 != "") tv_layer_1.text = addPairText(_selected_layer_1)
//        if (_selected_layer_2 != "") tv_layer_2.text = addPairText(_selected_layer_2)
//        if (_selected_layer_3 != "") tv_layer_3.text = addPairText(_selected_layer_3)
//        if (_selected_layer_4 != "") tv_layer_4.text = addPairText(_selected_layer_4)
//        if (_selected_layer_5 != "") tv_layer_5.text = addPairText(_selected_layer_5)
//
//        // target setting
//        if (AppGlobal.instance.get_target_type() == "") targetTypeChange("device_per_accumulate")
//        else targetTypeChange(AppGlobal.instance.get_target_type())
//
//        tv_shift_1.setText(AppGlobal.instance.get_target_manual_shift("1"))
//        tv_shift_2.setText(AppGlobal.instance.get_target_manual_shift("2"))
//        tv_shift_3.setText(AppGlobal.instance.get_target_manual_shift("3"))


        // click listener
        // Tab button
        btn_setting_server.setOnClickListener { tabChange(1) }
        btn_setting_device.setOnClickListener { tabChange(2) }
        btn_setting_count.setOnClickListener { tabChange(3) }
        btn_setting_target.setOnClickListener { tabChange(4) }
        btn_setting_test.setOnClickListener { tabChange(5) }

//        // System setting button listener
//        tv_setting_factory.setOnClickListener { fetchDataForFactory() }
//        tv_setting_room.setOnClickListener { fetchDataForRoom() }
//        tv_setting_line.setOnClickListener { fetchDataForLine() }
//        tv_setting_mc_model.setOnClickListener { fetchDataForMCModel() }

//        // Count setting button listener
//        tv_layer_0.setOnClickListener { fetchPairData("0") }
//        tv_layer_1.setOnClickListener { fetchPairData("1") }
//        tv_layer_2.setOnClickListener { fetchPairData("2") }
//        tv_layer_3.setOnClickListener { fetchPairData("3") }
//        tv_layer_4.setOnClickListener { fetchPairData("4") }
//        tv_layer_5.setOnClickListener { fetchPairData("5") }

//        // Target setting button listener
////        btn_server_accumulate.setOnClickListener { targetTypeChange("server_per_accumulate") }
////        btn_server_hourly.setOnClickListener { targetTypeChange("server_per_hourly") }
////        btn_server_shifttotal.setOnClickListener { targetTypeChange("server_per_day_total") }
//        btn_server_accumulate.setOnClickListener {
//            Toast.makeText(this, "Not yet supported.", Toast.LENGTH_SHORT).show()
//        }
//        btn_server_hourly.setOnClickListener {
//            Toast.makeText(this, "Not yet supported.", Toast.LENGTH_SHORT).show()
//        }
//        btn_server_shifttotal.setOnClickListener {
//            Toast.makeText(this, "Not yet supported.", Toast.LENGTH_SHORT).show()
//        }
//        btn_manual_accumulate.setOnClickListener { targetTypeChange("device_per_accumulate") }
//        btn_manual_hourly.setOnClickListener { targetTypeChange("device_per_hourly") }
//        btn_manual_shifttotal.setOnClickListener { targetTypeChange("device_per_day_total") }

//        // check server button
//        btn_setting_check_server.setOnClickListener {
//            checkServer(true)
//            var new_ip = et_setting_server_ip.text.toString()
//            var old_ip = AppGlobal.instance.get_server_ip()
//            if (!new_ip.equals(old_ip)) {
//                tv_setting_factory.text = ""
//                tv_setting_room.text = ""
//                tv_setting_line.text = ""
//                tv_setting_mc_model.text = ""
//            }
//        }
//
//        // Save button click
//        btn_setting_confirm.setOnClickListener {
//            saveSettingData()
//        }

        // Cancel button click
        btn_setting_cancel.setOnClickListener { finish() }

        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false

        if (AppGlobal.instance._server_state) btn_server_state.isSelected = true
        else btn_server_state.isSelected = false

        if (et_setting_server_ip.text.toString() == "") et_setting_server_ip.setText("49.247.203.100")     // 10.10.10.90
        if (et_setting_port.text.toString() == "") et_setting_port.setText("80")
    }

    private fun tabChange(v : Int) {
        if (tab_pos == v) return
        when (tab_pos) {
            1 -> {
                btn_setting_server.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_server.setBackgroundResource(R.color.colorButtonDefault)
                layout_setting_server.visibility = View.GONE
            }
            2 -> {
                btn_setting_device.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_device.setBackgroundResource(R.color.colorButtonDefault)
                layout_setting_device.visibility = View.GONE
            }
            3 -> {
                btn_setting_count.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_count.setBackgroundResource(R.color.colorButtonDefault)
                layout_setting_count.visibility = View.GONE
            }
            4 -> {
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_target.setBackgroundResource(R.color.colorButtonDefault)
                layout_setting_target.visibility = View.GONE
            }
            5 -> {
                btn_setting_test.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_test.setBackgroundResource(R.color.colorButtonDefault)
                layout_setting_test.visibility = View.GONE
            }
        }
        tab_pos = v
        when (tab_pos) {
            1 -> {
                btn_setting_server.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_server.setBackgroundResource(R.color.colorButtonBlue)
                layout_setting_server.visibility = View.VISIBLE
            }
            2 -> {
                btn_setting_device.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_device.setBackgroundResource(R.color.colorButtonBlue)
                layout_setting_device.visibility = View.VISIBLE
            }
            3 -> {
                btn_setting_count.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_count.setBackgroundResource(R.color.colorButtonBlue)
                layout_setting_count.visibility = View.VISIBLE
            }
            4 -> {
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_target.setBackgroundResource(R.color.colorButtonBlue)
                layout_setting_target.visibility = View.VISIBLE
            }
            5 -> {
                btn_setting_test.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_test.setBackgroundResource(R.color.colorButtonBlue)
                layout_setting_test.visibility = View.VISIBLE
            }
        }
    }

    private fun blinkColorChange(v : String) {
        when (_selected_blink_color) {
            "f8ad13" -> blink_color_f8ad13.text = ""
            "ff0000" -> blink_color_ff0000.text = ""
            "0079BA" -> blink_color_0079BA.text = ""
            "888888" -> blink_color_888888.text = ""
        }
        _selected_blink_color = v
        when (_selected_blink_color) {
            "f8ad13" -> blink_color_f8ad13.text = "V"
            "ff0000" -> blink_color_ff0000.text = "V"
            "0079BA" -> blink_color_0079BA.text = "V"
            "888888" -> blink_color_888888.text = "V"
        }
    }
}