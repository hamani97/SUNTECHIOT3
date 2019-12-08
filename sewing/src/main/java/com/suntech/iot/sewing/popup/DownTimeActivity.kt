package com.suntech.iot.sewing.popup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.sewing.R
import com.suntech.iot.sewing.base.BaseActivity
import com.suntech.iot.sewing.db.DBHelperForDownTime
import com.suntech.iot.sewing.util.OEEUtil
import kotlinx.android.synthetic.main.activity_down_time.*
import kotlinx.android.synthetic.main.list_item_downtime_total.*
import java.util.*

class DownTimeActivity : BaseActivity() {

    private var _db = DBHelperForDownTime(this)
    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    private val _timer_task1 = Timer()

    val _start_down_time_activity = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_down_time)

        btn_confirm.setOnClickListener {
            val count = _db.counts_for_notcompleted()
            if (count>0) {
                ToastOut(this, R.string.msg_has_notcompleted, true)
                return@setOnClickListener
            }
            finish(true, 1, "ok", null)
        }

        lv_downtimes.setOnItemClickListener { adapterView, view, i, l ->
            val idx = _list[i]["idx"].toString()
            val start_dt = _list[i]["start_dt"].toString()
            val completed = _list[i]["completed"]
            if (completed=="Y") return@setOnItemClickListener

            val intent = Intent(this, DownTimeInputActivity::class.java)
            intent.putExtra("idx", idx)
            intent.putExtra("start_dt", start_dt)
            startActivity(intent, { r, c, m, d ->
                if (r) {
                    if (_db.counts_for_notcompleted() > 0) updateView()
                    else finish()
                }
            })
        }

        updateView()
        start_timer()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(_start_down_time_activity, IntentFilter("start.downtime"))
        is_loop = true
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    public override fun onPause() {
        super.onPause()
        is_loop = false
        overridePendingTransition(0, 0)
        unregisterReceiver(_start_down_time_activity)
    }

    private fun updateView() {
        _list = _db.gets() ?: _list
        list_adapter = ListAdapter(this, _list)
        lv_downtimes.adapter = list_adapter

        var total_downtime = 0
        var total_planned = 0

        _list?.forEach { item ->
            item.put("downtime", "")
            val start_dt = OEEUtil.parseDateTime(item["start_dt"])
            if (item["end_dt"]!=null) {
                val millis = item["millis"].toString().toInt()
                val downtime = item["real_millis"].toString().toInt()
                val plannedtime = millis - downtime

                total_downtime += (millis / 60)
                total_planned += plannedtime

                item.set("downtime", (millis / 60).toString()+ " min")
            }
//            if (item["end_dt"]!=null) {
//                val end_dt = OEEUtil.parseDateTime(item["end_dt"])
//                val realtime = item["real_millis"].toString().toInt()
//
//                var dif = end_dt.millis - start_dt.millis
//                val downtime = (dif / 1000 / 60 ).toInt()
//                total_downtime += downtime
//
//                val plannedtime = (dif / 1000).toInt() - realtime
//                total_planned += plannedtime
//
//                item.set("downtime", downtime.toString()+ " min")
//            }
        }
        total_planned = total_planned / 60

        tv_item_downtime_total?.text = ""+total_downtime + " min"
        tv_item_downtime_total1?.text = "-" + total_planned + " min"
        tv_item_downtime_total2?.text = "" + (total_downtime - total_planned) + " min"
    }

    private var is_loop = true

    private fun start_timer () {
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (is_loop) {
                        if (_list.size == 0) updateView()
                    }
                }
            }
        }
        _timer_task1.schedule(task1, 500, 1000)
    }
    private fun cancel_timer () {
        _timer_task1.cancel()
    }

    private class ListAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

        private var _list: ArrayList<HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_item_downtime, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_shift_name.text = _list[position]["shift_name"]
//            vh.tv_item_design_idx.text = _list[position]["design_idx"]
            vh.tv_item_start_time.text = _list[position]["start_dt"]
            vh.tv_item_end_time.text = _list[position]["end_dt"]
            vh.tv_item_downtime.text = _list[position]["downtime"]
            vh.tv_item_completed.text = _list[position]["completed"]
            vh.tv_item_list.text = _list[position]["list"]

            if (_list[position]["completed"]=="N") {
                vh.tv_item_shift_name.setTextColor(Color.parseColor("#ff0000"))
//                vh.tv_item_design_idx.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_start_time.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_completed.setTextColor(Color.parseColor("#ff0000"))
            }
            else {
                vh.tv_item_shift_name.setTextColor(Color.parseColor("#000000"))
//                vh.tv_item_design_idx.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_start_time.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_completed.setTextColor(Color.parseColor("#000000"))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_shift_name: TextView
            //            val tv_item_design_idx: TextView
            val tv_item_start_time: TextView
            val tv_item_end_time: TextView
            val tv_item_downtime: TextView
            val tv_item_completed: TextView
            val tv_item_list: TextView

            init {
                this.tv_item_shift_name = row?.findViewById<TextView>(R.id.tv_item_shift_name) as TextView
//                this.tv_item_design_idx = row?.findViewById<TextView>(R.id.tv_item_design_idx) as TextView
                this.tv_item_start_time = row?.findViewById<TextView>(R.id.tv_item_start_time) as TextView
                this.tv_item_end_time = row?.findViewById<TextView>(R.id.tv_item_end_time) as TextView
                this.tv_item_downtime = row?.findViewById<TextView>(R.id.tv_item_downtime) as TextView
                this.tv_item_completed = row?.findViewById<TextView>(R.id.tv_item_completed) as TextView
                this.tv_item_list = row?.findViewById<TextView>(R.id.tv_item_list) as TextView
            }
        }
    }
}