#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
小辽项目 — 本周进度监控表生成脚本
两组并行：M1组(杨胜威,宋丽娜) M2组(郑永涛,师润佳)
"""

import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side, numbers
from openpyxl.utils import get_column_letter
from datetime import date

wb = openpyxl.Workbook()

# ─── 颜色定义 ───
C_HEADER   = "1F4E79"  # 深蓝表头
C_HEADER_F = "FFFFFF"  # 白字
C_M1       = "FFF3E0"  # 浅橙 — M1组
C_M2       = "E3F2FD"  # 浅蓝 — M2组
C_MON      = "E8F5E9"  # 浅绿 — 周一
C_TUE      = "FFF8E1"  # 浅黄 — 周二
C_WED      = "FCE4EC"  # 浅粉 — 周三
C_THU      = "F3E5F5"  # 浅紫 — 周四
C_FRI      = "E0F7FA"  # 浅青 — 周五
C_DONE     = "C8E6C9"  # 已完成绿
C_DOING    = "FFF9C4"  # 进行中黄
C_TODO     = "FFFFFF"  # 未开始白
C_BLOCK    = "FFCDD2"  # 阻塞红
C_SUBHEAD  = "F5F5F5"  # 子表头灰
C_WEEKEND  = "ECEFF1"  # 周末灰

DAY_COLORS = [C_MON, C_TUE, C_WED, C_THU, C_FRI]

# ─── 通用样式 ───
thin_border = Border(
    left=Side(style='thin', color='BDBDBD'),
    right=Side(style='thin', color='BDBDBD'),
    top=Side(style='thin', color='BDBDBD'),
    bottom=Side(style='thin', color='BDBDBD')
)

def style_header(cell, bg=C_HEADER, fg=C_HEADER_F, size=11):
    cell.font = Font(name='微软雅黑', bold=True, size=size, color=fg)
    cell.fill = PatternFill('solid', fgColor=bg)
    cell.alignment = Alignment(horizontal='center', vertical='center', wrap_text=True)
    cell.border = thin_border

def style_cell(cell, bg=None, bold=False, size=10, align='left'):
    cell.font = Font(name='微软雅黑', bold=bold, size=size)
    cell.alignment = Alignment(horizontal=align, vertical='center', wrap_text=True)
    cell.border = thin_border
    if bg:
        cell.fill = PatternFill('solid', fgColor=bg)

def style_status(cell, status):
    color_map = {
        '已完成': C_DONE,
        '进行中': C_DOING,
        '未开始': C_TODO,
        '阻塞':   C_BLOCK,
    }
    bg = color_map.get(status, C_TODO)
    cell.font = Font(name='微软雅黑', bold=True, size=10)
    cell.alignment = Alignment(horizontal='center', vertical='center')
    cell.border = thin_border
    cell.fill = PatternFill('solid', fgColor=bg)

# ═════════════════════════════════════════════
# Sheet 1: 本周计划总览
# ═════════════════════════════════════════════
ws1 = wb.active
ws1.title = "本周计划总览"

# 标题行
ws1.merge_cells('A1:H1')
c = ws1['A1']
c.value = "小辽项目 — 本周开发计划总览（8月3日-8月9日）"
c.font = Font(name='微软雅黑', bold=True, size=16, color=C_HEADER_F)
c.fill = PatternFill('solid', fgColor=C_HEADER)
c.alignment = Alignment(horizontal='center', vertical='center')
ws1.row_dimensions[1].height = 38

# 分组信息行
ws1.merge_cells('A2:H2')
c = ws1['A2']
c.value = "M1组（杨胜威、宋丽娜）→ 模块一 签到情绪    |    M2组（郑永涛、师润佳）→ 模块二 脑力游戏"
c.font = Font(name='微软雅黑', bold=True, size=11, color="333333")
c.fill = PatternFill('solid', fgColor=C_SUBHEAD)
c.alignment = Alignment(horizontal='center', vertical='center')
ws1.row_dimensions[2].height = 26

# 表头
headers = ["序号", "模块", "负责人", "任务内容", "类型", "计划日期", "预计工时", "状态"]
for col, h in enumerate(headers, 1):
    cell = ws1.cell(row=3, column=col, value=h)
    style_header(cell)
ws1.row_dimensions[3].height = 30

# ─── M1组任务数据 ───
m1_tasks = [
    # 杨胜威 — 后端
    ("M1-01", "签到情绪", "杨胜威", "需求评审 + 改V1建表脚本(mood 5档) + 创建4个DTO类", "后端", "周一 8/3", "1天", "未开始"),
    ("M1-02", "签到情绪", "杨胜威", "CheckinService — 签到方法 + 查今日签到 + 防重复签到", "后端", "周二 8/4", "1天", "未开始"),
    ("M1-03", "签到情绪", "杨胜威", "CheckinService — 连续签到天数计算 + 情绪月历查询 + 首页信息接口", "后端", "周三 8/5", "1天", "未开始"),
    ("M1-04", "签到情绪", "杨胜威", "CheckinController 3个接口 + 连续负面情绪检测方法(供M7调用)", "后端", "周四 8/6", "1天", "未开始"),
    ("M1-05", "签到情绪", "杨胜威", "Postman接口测试 + 修bug + 整理接口文档给前端", "后端", "周五 8/7", "1天", "未开始"),
    # 宋丽娜 — 前端
    ("M1-06", "签到情绪", "宋丽娜", "需求评审 + 小程序开发环境搭建 + 研读原型设计稿", "前端", "周一 8/3", "1天", "未开始"),
    ("M1-07", "签到情绪", "宋丽娜", "首页页面 — 日期/星期/季节/问候语/连续天数/签到入口按钮", "前端", "周二 8/4", "1天", "未开始"),
    ("M1-08", "签到情绪", "宋丽娜", "签到情绪页 — 5档天气emoji选择 + 文字输入框 + 提交按钮", "前端", "周三 8/5", "1天", "未开始"),
    ("M1-09", "签到情绪", "宋丽娜", "签到成功正反馈动画 + 情绪月历页(日历网格+emoji)", "前端", "周四 8/6", "1天", "未开始"),
    ("M1-10", "签到情绪", "宋丽娜", "对接后端3个接口 + 联调测试 + 适配老人大字号", "前端", "周五 8/7", "1天", "未开始"),
]

# ─── M2组任务数据 ───
m2_tasks = [
    # 郑永涛 — 后端
    ("M2-01", "脑力游戏", "郑永涛", "需求评审 + 确认game_records表结构 + 难度自适应引擎方案设计", "后端", "周一 8/3", "1天", "未开始"),
    ("M2-02", "脑力游戏", "郑永涛", "GameRecord Entity/Mapper + 游戏记录写入接口 POST /api/game/record", "后端", "周二 8/4", "1天", "未开始"),
    ("M2-03", "脑力游戏", "郑永涛", "难度自适应算法实现(正确率≥80%升级/≤40%降级) + 查询用户当前难度", "后端", "周三 8/5", "1天", "未开始"),
    ("M2-04", "脑力游戏", "郑永涛", "游戏成绩查询接口 + 跨域训练分布统计 + 放弃率埋点", "后端", "周四 8/6", "1天", "未开始"),
    ("M2-05", "脑力游戏", "郑永涛", "Postman接口测试 + 修bug + 整理接口文档给前端", "后端", "周五 8/7", "1天", "未开始"),
    # 师润佳 — 前端
    ("M2-06", "脑力游戏", "师润佳", "需求评审 + 研究小程序Canvas/动画方案 + 研读原型设计稿", "前端", "周一 8/3", "1天", "未开始"),
    ("M2-07", "脑力游戏", "师润佳", "游戏列表页 — 4款游戏卡片展示(记忆/注意/推理/语言) + 难度等级显示", "前端", "周二 8/4", "1天", "未开始"),
    ("M2-08", "脑力游戏", "师润佳", "G1翻牌配对游戏 — 难度2×2→4×4 + 翻错不扣分提示 + 完成星级", "前端", "周三 8/5", "1天", "未开始"),
    ("M2-09", "脑力游戏", "师润佳", "G2找不同游戏 — 两图对比 + 自适应干扰物 + 重准确不抢速", "前端", "周四 8/6", "1天", "未开始"),
    ("M2-10", "脑力游戏", "师润佳", "G3找规律 + G4看图说词 初步开发 + 对接后端记录写入接口", "前端", "周五 8/7", "1天", "未开始"),
]

row = 4
for task in m1_tasks + m2_tasks:
    for col, val in enumerate(task, 1):
        cell = ws1.cell(row=row, column=col, value=val)
        bg = C_M1 if "M1" in task[0] else C_M2
        style_cell(cell, bg=bg, bold=(col == 1), align='center' if col in [1,2,3,5,6,7] else 'left')
        if col == 8:
            style_status(cell, val)
    row += 1

# 列宽
widths = [8, 10, 10, 52, 8, 12, 8, 10]
for i, w in enumerate(widths, 1):
    ws1.column_dimensions[get_column_letter(i)].width = w

# 冻结表头
ws1.freeze_panes = 'A4'


# ═════════════════════════════════════════════
# Sheet 2: M1签到情绪组 — 每日任务
# ═════════════════════════════════════════════
ws2 = wb.create_sheet("M1签到情绪组")

ws2.merge_cells('A1:F1')
c = ws2['A1']
c.value = "M1 签到情绪组 — 杨胜威(后端) / 宋丽娜(前端)"
c.font = Font(name='微软雅黑', bold=True, size=14, color=C_HEADER_F)
c.fill = PatternFill('solid', fgColor=C_HEADER)
c.alignment = Alignment(horizontal='center', vertical='center')
ws2.row_dimensions[1].height = 34

# 需求摘要
ws2.merge_cells('A2:F2')
c = ws2['A2']
c.value = ("需求要点：5档情绪(晴/多云/阴/雨/雷雨) | ≤2步签到 | 连续签到天数(不惩罚断签) | "
           "情绪月历回看 | 连续3天负面→触发M7关怀 | 字号≥20sp 按钮≥48dp | 不强制签到才能用其他功能")
c.font = Font(name='微软雅黑', size=9, color="555555")
c.fill = PatternFill('solid', fgColor=C_SUBHEAD)
c.alignment = Alignment(horizontal='left', vertical='center', wrap_text=True)
ws2.row_dimensions[2].height = 36

# 表头
headers2 = ["日期", "杨胜威（后端）", "交付物", "宋丽娜（前端）", "交付物", "联调点"]
for col, h in enumerate(headers2, 1):
    cell = ws2.cell(row=3, column=col, value=h)
    style_header(cell)
ws2.row_dimensions[3].height = 28

m1_daily = [
    ("周一 8/3",
     "需求评审；改V1__init.sql mood注释(3档→5档)；创建CheckinRequest/CheckinResponse/HomeResponse/CalendarResponse 4个DTO",
     "V1脚本修正\n4个DTO类",
     "需求评审；小程序开发环境搭建(微信开发者工具)；研读原型设计稿；理解5档情绪交互",
     "环境就绪\n原型理解",
     "确认接口字段定义"),
    ("周二 8/4",
     "CheckinService — 签到方法(insert+防重复) + 查今日签到方法；CheckinController 搭建",
     "POST /api/checkin\nGET /api/checkin/today",
     "首页页面开发 — 日期/星期/季节/问候语展示 + 连续签到天数 + 签到入口按钮",
     "首页页面",
     "首页信息接口字段对齐"),
    ("周三 8/5",
     "CheckinService — 连续签到天数计算(往前数到断) + 情绪月历查询 + 首页信息接口(问候语/季节/streak)",
     "GET /api/checkin/home\nGET /api/checkin/calendar",
     "签到情绪页 — 5档天气emoji大按钮 + 文字输入框 + 提交，字号≥20sp 按钮≥48dp",
     "签到情绪页",
     "签到接口请求/响应格式确认"),
    ("周四 8/6",
     "CheckinController 补全3个接口 + hasConsecutiveNegativeMood()方法(阴/雨/雷雨算负面,供M7调用)",
     "全部接口就绪\n负面情绪检测方法",
     "签到成功正反馈动画(emoji放大+鼓励语) + 情绪月历页(日历网格+每天emoji)",
     "反馈动画\n月历页",
     "月历接口数据格式确认"),
    ("周五 8/7",
     "Postman全接口测试；修bug；整理接口文档(Markdown)给前端；编译验证",
     "接口文档\n测试通过",
     "对接后端3个接口(签到/首页/月历)；联调测试；老人字号适配检查",
     "联调通过\n可演示",
     "端到端联调"),
]

for i, (day, yang, yang_out, song, song_out, sync) in enumerate(m1_daily):
    r = 4 + i
    bg = DAY_COLORS[i]
    ws2.cell(row=r, column=1, value=day)
    ws2.cell(row=r, column=2, value=yang)
    ws2.cell(row=r, column=3, value=yang_out)
    ws2.cell(row=r, column=4, value=song)
    ws2.cell(row=r, column=5, value=song_out)
    ws2.cell(row=r, column=6, value=sync)
    for col in range(1, 7):
        cell = ws2.cell(row=r, column=col)
        align = 'center' if col in [1, 3, 5, 6] else 'left'
        bold = (col == 1)
        style_cell(cell, bg=bg, bold=bold, align=align)
    ws2.row_dimensions[r].height = 80

widths2 = [12, 48, 18, 48, 18, 18]
for i, w in enumerate(widths2, 1):
    ws2.column_dimensions[get_column_letter(i)].width = w
ws2.freeze_panes = 'A4'


# ═════════════════════════════════════════════
# Sheet 3: M2脑力游戏组 — 每日任务
# ═════════════════════════════════════════════
ws3 = wb.create_sheet("M2脑力游戏组")

ws3.merge_cells('A1:F1')
c = ws3['A1']
c.value = "M2 脑力游戏组 — 郑永涛(后端) / 师润佳(前端)"
c.font = Font(name='微软雅黑', bold=True, size=14, color=C_HEADER_F)
c.fill = PatternFill('solid', fgColor=C_HEADER)
c.alignment = Alignment(horizontal='center', vertical='center')
ws3.row_dimensions[1].height = 34

ws3.merge_cells('A2:F2')
c = ws3['A2']
c.value = ("需求要点：4款游戏(G1翻牌记忆/G2找不同注意/G3找规律推理/G4看图说词语言) | "
           "难度自适应(≥80%升/≤40%降) | 翻错不扣分给提示 | 每局3-5分钟 | 展示今日训练了哪个能力 | "
           "无挫败式反馈 | 正确率/用时/完成等级/放弃率埋点")
c.font = Font(name='微软雅黑', size=9, color="555555")
c.fill = PatternFill('solid', fgColor=C_SUBHEAD)
c.alignment = Alignment(horizontal='left', vertical='center', wrap_text=True)
ws3.row_dimensions[2].height = 36

for col, h in enumerate(headers2, 1):
    # 替换人名
    h = h.replace("杨胜威（后端）", "郑永涛（后端）").replace("宋丽娜（前端）", "师润佳（前端）")
    cell = ws3.cell(row=3, column=col, value=h)
    style_header(cell)
ws3.row_dimensions[3].height = 28

m2_daily = [
    ("周一 8/3",
     "需求评审；确认game_records表结构(score/duration/details JSONB)；难度自适应引擎方案设计",
     "表结构确认\n自适应方案",
     "需求评审；研究小程序Canvas/动画方案(wxss+js vs Canvas)；研读原型设计稿；理解4款游戏玩法",
     "技术选型\n原型理解",
     "确认游戏记录数据结构"),
    ("周二 8/4",
     "GameRecord Entity/Mapper(已有)；游戏记录写入接口 POST /api/game/record；查询当前难度等级",
     "POST /api/game/record\nGET /api/game/level",
     "游戏列表页 — 4款游戏卡片(记忆/注意/推理/语言) + 当前难度等级显示 + 今日训练能力展示",
     "游戏列表页",
     "记录接口字段对齐"),
    ("周三 8/5",
     "难度自适应算法实现：查最近N局正确率→≥80%升级/≤40%降级/中间维持；返回新难度+训练能力名称",
     "自适应算法\nGET /api/game/adaptive",
     "G1翻牌配对 — 难度2×2起(动物/老物件图案)；翻错不扣分给提示再来；完成给星级(1-3星)",
     "G1翻牌配对\n可玩",
     "难度等级参数传递"),
    ("周四 8/6",
     "游戏成绩查询接口(按游戏类型/按日期) + 跨域训练分布统计(4个能力各练了多少) + 放弃率埋点",
     "GET /api/game/history\nGET /api/game/stats",
     "G2找不同 — 两图找不同 + 连续做对增加干扰物/缩短提示 + 重准确不抢速 + 无挫败反馈",
     "G2找不同\n可玩",
     "成绩查询接口确认"),
    ("周五 8/7",
     "Postman全接口测试；修bug；整理接口文档给前端；编译验证",
     "接口文档\n测试通过",
     "G3找规律(续数列/物品归类) + G4看图说词 初步开发；对接后端记录写入接口",
     "G3/G4初版\nG1/G2对接",
     "端到端联调G1+G2"),
]

for i, (day, shi, shi_out, zheng, zheng_out, sync) in enumerate(m2_daily):
    r = 4 + i
    bg = DAY_COLORS[i]
    ws3.cell(row=r, column=1, value=day)
    ws3.cell(row=r, column=2, value=shi)
    ws3.cell(row=r, column=3, value=shi_out)
    ws3.cell(row=r, column=4, value=zheng)
    ws3.cell(row=r, column=5, value=zheng_out)
    ws3.cell(row=r, column=6, value=sync)
    for col in range(1, 7):
        cell = ws3.cell(row=r, column=col)
        align = 'center' if col in [1, 3, 5, 6] else 'left'
        bold = (col == 1)
        style_cell(cell, bg=bg, bold=bold, align=align)
    ws3.row_dimensions[r].height = 80

for i, w in enumerate(widths2, 1):
    ws3.column_dimensions[get_column_letter(i)].width = w
ws3.freeze_panes = 'A4'


# ═════════════════════════════════════════════
# Sheet 4: 每日进度打卡
# ═════════════════════════════════════════════
ws4 = wb.create_sheet("每日进度打卡")

ws4.merge_cells('A1:F1')
c = ws4['A1']
c.value = "每日进度打卡（下班前填写）"
c.font = Font(name='微软雅黑', bold=True, size=14, color=C_HEADER_F)
c.fill = PatternFill('solid', fgColor=C_HEADER)
c.alignment = Alignment(horizontal='center', vertical='center')
ws4.row_dimensions[1].height = 34

headers4 = ["日期", "姓名", "今日完成", "完成度", "遇到的问题", "明日计划"]
for col, h in enumerate(headers4, 1):
    cell = ws4.cell(row=2, column=col, value=h)
    style_header(cell)
ws4.row_dimensions[2].height = 28

people = ["杨胜威", "宋丽娜", "郑永涛", "师润佳"]
days = ["周一 8/3", "周二 8/4", "周三 8/5", "周四 8/6", "周五 8/7"]

row = 3
for day_idx, day in enumerate(days):
    for person in people:
        ws4.cell(row=row, column=1, value=day)
        ws4.cell(row=row, column=2, value=person)
        ws4.cell(row=row, column=3, value="")
        ws4.cell(row=row, column=4, value="0%")
        ws4.cell(row=row, column=5, value="")
        ws4.cell(row=row, column=6, value="")
        bg = DAY_COLORS[day_idx]
        for col in range(1, 7):
            cell = ws4.cell(row=row, column=col)
            align = 'center' if col in [1, 2, 4] else 'left'
            bold = (col <= 2)
            style_cell(cell, bg=bg, bold=bold, align=align)
        ws4.row_dimensions[row].height = 50
        row += 1
    # 空行分隔
    row += 1

widths4 = [12, 10, 40, 10, 30, 30]
for i, w in enumerate(widths4, 1):
    ws4.column_dimensions[get_column_letter(i)].width = w
ws4.freeze_panes = 'A3'


# ═════════════════════════════════════════════
# Sheet 5: 验收清单
# ═════════════════════════════════════════════
ws5 = wb.create_sheet("验收清单")

ws5.merge_cells('A1:E1')
c = ws5['A1']
c.value = "本周交付验收清单"
c.font = Font(name='微软雅黑', bold=True, size=14, color=C_HEADER_F)
c.fill = PatternFill('solid', fgColor=C_HEADER)
c.alignment = Alignment(horizontal='center', vertical='center')
ws5.row_dimensions[1].height = 34

headers5 = ["模块", "验收项", "验收标准", "负责人", "是否通过"]
for col, h in enumerate(headers5, 1):
    cell = ws5.cell(row=2, column=col, value=h)
    style_header(cell)
ws5.row_dimensions[2].height = 28

acceptance = [
    # M1
    ("M1签到", "5档情绪签到", "晴/多云/阴/雨/雷雨 5档可选，点击即记录", "杨胜威+宋丽娜", "待验收"),
    ("M1签到", "≤2步完成签到", "选情绪→(可选写一句)→完成，不超过2步", "宋丽娜", "待验收"),
    ("M1签到", "防重复签到", "同一天重复签到返回已有记录，不报错", "杨胜威", "待验收"),
    ("M1签到", "连续签到天数", "正确计算连续天数，断签不惩罚不催促", "杨胜威", "待验收"),
    ("M1签到", "签到正反馈", "签到后返回对应心情的暖句，前端展示动画", "杨胜威+宋丽娜", "待验收"),
    ("M1签到", "情绪月历", "可回看本月每天的情绪emoji", "杨胜威+宋丽娜", "待验收"),
    ("M1签到", "首页信息", "日期/星期/季节/问候语/连续天数/今日是否已签", "杨胜威+宋丽娜", "待验收"),
    ("M1签到", "负面情绪检测", "连续3天阴/雨/雷雨→返回true供M7调用", "杨胜威", "待验收"),
    ("M1签到", "适老化", "字号≥20sp 按钮≥48dp 高对比", "宋丽娜", "待验收"),
    ("M1签到", "不强制签到", "签到与其他功能完全独立，无拦截", "杨胜威", "待验收"),
    # M2
    ("M2游戏", "G1翻牌配对", "2×2起→4×4，翻错不扣分给提示，完成给星级", "师润佳", "待验收"),
    ("M2游戏", "G2找不同", "两图找不同，自适应干扰物，重准确不抢速", "师润佳", "待验收"),
    ("M2游戏", "G3找规律", "续写数列/物品归类，初步可玩", "师润佳", "待验收"),
    ("M2游戏", "G4看图说词", "初步可玩", "师润佳", "待验收"),
    ("M2游戏", "难度自适应", "≥80%升级/≤40%降级/中间维持，每局结束返回新难度", "郑永涛", "待验收"),
    ("M2游戏", "游戏记录写入", "正确率/用时/完成等级写入DB", "郑永涛", "待验收"),
    ("M2游戏", "训练能力展示", "每局结束展示'今天练了记忆力/注意力/推理力/语言力'", "郑永涛+师润佳", "待验收"),
    ("M2游戏", "无挫败反馈", "错误操作不产生挫败式提示，只鼓励", "师润佳", "待验收"),
    ("M2游戏", "单局≤5分钟", "每局3-5分钟内可完成", "师润佳", "待验收"),
]

for i, (mod, item, criteria, owner, status) in enumerate(acceptance):
    r = 3 + i
    bg = C_M1 if "M1" in mod else C_M2
    ws5.cell(row=r, column=1, value=mod)
    ws5.cell(row=r, column=2, value=item)
    ws5.cell(row=r, column=3, value=criteria)
    ws5.cell(row=r, column=4, value=owner)
    ws5.cell(row=r, column=5, value=status)
    for col in range(1, 6):
        cell = ws5.cell(row=r, column=col)
        align = 'center' if col in [1, 4, 5] else 'left'
        bold = (col <= 2)
        style_cell(cell, bg=bg, bold=bold, align=align)
    ws5.row_dimensions[r].height = 30

widths5 = [10, 18, 50, 18, 10]
for i, w in enumerate(widths5, 1):
    ws5.column_dimensions[get_column_letter(i)].width = w
ws5.freeze_panes = 'A3'


# ═════════════════════════════════════════════
# Sheet 6: 图例说明
# ═════════════════════════════════════════════
ws6 = wb.create_sheet("图例说明")

ws6.merge_cells('A1:C1')
c = ws6['A1']
c.value = "状态颜色图例"
c.font = Font(name='微软雅黑', bold=True, size=14, color=C_HEADER_F)
c.fill = PatternFill('solid', fgColor=C_HEADER)
c.alignment = Alignment(horizontal='center', vertical='center')
ws6.row_dimensions[1].height = 34

legends = [
    (C_DONE, "已完成", "任务完成，交付物已产出"),
    (C_DOING, "进行中", "正在开发中"),
    (C_TODO, "未开始", "尚未开始"),
    (C_BLOCK, "阻塞", "遇到阻碍无法继续，需要协调"),
    (C_M1, "M1组", "签到情绪模块任务"),
    (C_M2, "M2组", "脑力游戏模块任务"),
]

for i, (color, label, desc) in enumerate(legends):
    r = 2 + i
    ws6.cell(row=r, column=1, value="")
    ws6.cell(row=r, column=1).fill = PatternFill('solid', fgColor=color)
    ws6.cell(row=r, column=1).border = thin_border
    ws6.cell(row=r, column=2, value=label)
    ws6.cell(row=r, column=3, value=desc)
    for col in [2, 3]:
        style_cell(ws6.cell(row=r, column=col), bold=(col == 2), align='left')
    ws6.row_dimensions[r].height = 26

ws6.column_dimensions['A'].width = 12
ws6.column_dimensions['B'].width = 12
ws6.column_dimensions['C'].width = 40


# ─── 保存 ───
output = "/Users/robot/WorkBuddy/2026-07-31-08-30-38/xiaoliao/prototype/本周进度监控表_0803-0809.xlsx"
wb.save(output)
print(f"已生成: {output}")
print(f"共 {len(wb.sheetnames)} 个Sheet: {wb.sheetnames}")
