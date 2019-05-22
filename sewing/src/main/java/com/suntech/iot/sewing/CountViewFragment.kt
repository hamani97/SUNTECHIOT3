package com.suntech.iot.sewing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.sewing.base.BaseFragment
import com.suntech.iot.sewing.common.AppGlobal
import com.suntech.iot.sewing.db.DBHelperForComponent
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.layout_top_menu.*
import org.joda.time.DateTime

class CountViewFragment : BaseFragment() {

    private var is_loop: Boolean = false

    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    private var _total_target = 0

    private var _list_for_wos_adapter: ListWosAdapter? = null
    private var _list_for_wos: java.util.ArrayList<java.util.HashMap<String, String>> = arrayListOf()

    private var _selected_component_pos = -1

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

        _list_for_wos_adapter = ListWosAdapter(activity, _list_for_wos)
        lv_wos_info2.adapter = _list_for_wos_adapter

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
        btn_select_component.setOnClickListener {
            val intent = Intent(activity, ComponentInfoActivity::class.java)
            getBaseActivity().startActivity(intent, { r, c, m, d ->
                if (r && d != null) {
                    (activity as MainActivity).countViewType = 2
                    (activity as MainActivity).changeFragment(1)

                    val wosno = d!!["wosno"]!!
                    val styleno = d["styleno"]!!.toString()
                    val model = d["model"]!!.toString()
                    val size = d["size"]!!.toString()
                    val target = d["target"]!!.toString()
                    val actual = d["actual"]!!.toString()

//                        val styleno = d["ct"]!!.toInt()
//                        val pieces_info = AppGlobal.instance.get_pieces_info()
                    viewWosData()
                    fetchFilterWos()

//                    (activity as MainActivity).startComponent(wosno, styleno, model, size, target, actual)
////                        (activity as MainActivity).startNewProduct(idx, pieces_info, cycle_time, model, article, material_way, component)
                }
            })
        }
        viewWosData()
        fetchColorData()    // Get Color
        fetchFilterWos()    // 기존 선택된 WOS 가 있으면 로드해서 화면에 표시한다.
    }

    fun viewWosData() {
        // WOS INFO
//        tv_wosno.text = AppGlobal.instance.get_compo_wos()
//        tv_model.text = AppGlobal.instance.get_compo_model()
//        tv_component.text = AppGlobal.instance.get_compo_component()
//        tv_style_no.text = AppGlobal.instance.get_compo_style()

        tv_count_view_csize.text = AppGlobal.instance.get_compo_size()
        tv_count_view_clayer.text = AppGlobal.instance.get_compo_layer()
        tv_count_view_ctarget.text = "" + AppGlobal.instance.get_compo_target()
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
        val work_idx = AppGlobal.instance.get_work_idx()

        if ((activity as MainActivity).countViewType == 1) {
            if ((activity as MainActivity).countViewMode == 1) {
                tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
            }
        } else if ((activity as MainActivity).countViewType == 2) {
            tv_component_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")

//            if (work_idx=="") {
//                tv_component_view_target.text = "0"
//                tv_component_view_actual.text = "0"
//                tv_component_view_ratio.text = "0%"
//
//                _current_compo_target_count = -1
//                _current_compo_actual_count = -1
//
//                _selected_component_pos = -1
//                _list_for_wos.removeAll(_list_for_wos)
//                _list_for_wos_adapter?.select(_selected_component_pos)
//                _list_for_wos_adapter?.notifyDataSetChanged()
//
//            } else {
//                var ratio = 0
//                var ratio_txt = "N/A"
//
//                var db = DBHelperForComponent(activity)
//
//                // component count view 화면을 보고 있을 경우 처리
//
//                val item = db.get(work_idx)
//                if (item != null && item.toString() != "") {
//                    val target = item["target"].toString().toInt()
//                    val actual = (item["actual"].toString().toInt())
//                    _current_compo_target_count = target
//                    _current_compo_actual_count = actual
//
//                    if (target > 0) {
//                        ratio = (actual.toFloat() / target.toFloat() * 100).toInt()
//                        if (ratio > 999) ratio = 999
//                        ratio_txt = "" + ratio + "%"
//                    }
//
//                    tv_component_view_target.text = "" + target
//                    tv_component_view_actual.text = "" + actual
//                    tv_component_view_ratio.text = ratio_txt
//
//                    var maxEnumber = 0
//                    var color_code = "ffffff"
//
//                    for (i in 0..(_list.size - 1)) {
//                        val snumber = _list[i]["snumber"]?.toInt() ?: 0
//                        val enumber = _list[i]["enumber"]?.toInt() ?: 0
//                        if (maxEnumber < enumber) maxEnumber = enumber
//                        if (snumber <= ratio && enumber >= ratio) color_code = _list[i]["color_code"].toString()
//                    }
//                    tv_component_view_target.setTextColor(Color.parseColor("#" + color_code))
//                    tv_component_view_actual.setTextColor(Color.parseColor("#" + color_code))
//                    tv_component_view_ratio.setTextColor(Color.parseColor("#" + color_code))
//
//                    // 리스트에서 첫번째 항목이 선택되어 있으면 같이 업데이트 한다.
//                    if (_selected_component_pos >= 0) {
//                        var item = _list_for_wos.get(_selected_component_pos)
//                        _list_for_wos[_selected_component_pos]["target"] = "" + target
//                        _list_for_wos[_selected_component_pos]["actual"] = "" + actual
//                        _list_for_wos[_selected_component_pos]["balance"] = "" + (target - actual).toString()
//                        _list_for_wos_adapter?.notifyDataSetChanged()
//                    }
//                }
//            }
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

    private fun outputWosList() {

        // 정렬
        val sort_key = AppGlobal.instance.get_compo_sort_key()
        var sortedList = _list_for_wos.sortedWith(compareBy({ it.get(if (sort_key=="BALANCE") "balance" else "size").toString().toInt() }))

        _list_for_wos.removeAll(_list_for_wos)
        _selected_component_pos = -1

        val wosno = AppGlobal.instance.get_compo_wos()
        val size = AppGlobal.instance.get_compo_size()

        if (size == "") {
            _list_for_wos.addAll(sortedList)
        } else {
            // 선택된 항목을 맨앞으로 뺀다.
            for (i in 0..(sortedList.size - 1)) {
                val item = sortedList.get(i)
                if (wosno == item["wosno"] && size == item["size"]) {
                    _list_for_wos.add(item)
                    _selected_component_pos = 0
                    break
                }
            }
            for (i in 0..(sortedList.size - 1)) {
                val item = sortedList.get(i)
                if (wosno != item["wosno"] || size != item["size"]) {
                    _list_for_wos.add(item)
                }
            }
        }
        _list_for_wos_adapter?.select(_selected_component_pos)
        _list_for_wos_adapter?.notifyDataSetChanged()
    }

    private fun fetchFilterWos() {

        _list_for_wos.removeAll(_list_for_wos)
        _selected_component_pos = -1
        _list_for_wos_adapter?.select(-1)
        _list_for_wos_adapter?.notifyDataSetChanged()

        val def_wosno = AppGlobal.instance.get_compo_wos().trim()
        val def_size = AppGlobal.instance.get_compo_size().trim()

        if (def_wosno == "" || def_size == "") return

        var db = DBHelperForComponent(activity)

        val uri = "/wos.php"
        val params = listOf(
            "code" to "wos",
            "wosno" to def_wosno)

        getBaseActivity().request(activity, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                _selected_component_pos = -1
                _list_for_wos.removeAll(_list_for_wos)

                var list = result.getJSONArray("item")
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var actual = "0"

                    val row = db.get(item.getString("wosno"), item.getString("size"))
                    if (row != null) actual = row["actual"].toString()

                    val balance = item.getString("target").toInt() - actual.toInt()

                    var map = hashMapOf(
                        "wosno" to item.getString("wosno"),
                        "styleno" to item.getString("styleno"),
                        "model" to item.getString("model"),
                        "size" to item.getString("size"),
                        "target" to item.getString("target"),
                        "actual" to actual,
                        "balance" to balance.toString()
                    )
                    _list_for_wos.add(map)
                }
                outputWosList()

            } else {
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private class ListWosAdapter(context: Context, list: java.util.ArrayList<java.util.HashMap<String, String>>) : BaseAdapter() {

        private var _list: java.util.ArrayList<java.util.HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null
        private var _selected_index = -1

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        fun select(index:Int) { _selected_index = index }
        fun getSelected(): Int { return _selected_index }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_component_info, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            val balance = Integer.parseInt(_list[position]["target"]) - Integer.parseInt(_list[position]["actual"])

            vh.tv_item_wosno.text = _list[position]["wosno"]
            vh.tv_item_model.text = _list[position]["model"]
            vh.tv_item_size.text = _list[position]["size"]
            vh.tv_item_target.text = _list[position]["target"]
            vh.tv_item_actual.text = _list[position]["actual"]
            vh.tv_item_balance.text = balance.toString()

            if (_selected_index == position) {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
            } else if (balance <= 0) {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
            } else {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_wosno: TextView
            val tv_item_model: TextView
            val tv_item_size: TextView
            val tv_item_target: TextView
            val tv_item_actual: TextView
            val tv_item_balance: TextView

            init {
                this.tv_item_wosno = row?.findViewById<TextView>(R.id.tv_item_wosno) as TextView
                this.tv_item_model = row?.findViewById<TextView>(R.id.tv_item_model) as TextView
                this.tv_item_size = row?.findViewById<TextView>(R.id.tv_item_size) as TextView
                this.tv_item_target = row?.findViewById<TextView>(R.id.tv_item_target) as TextView
                this.tv_item_actual = row?.findViewById<TextView>(R.id.tv_item_actual) as TextView
                this.tv_item_balance = row?.findViewById<TextView>(R.id.tv_item_balance) as TextView
            }
        }
    }
}