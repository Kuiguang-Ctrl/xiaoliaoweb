from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side, numbers
from openpyxl.utils import get_column_letter

wb = Workbook()

# ── Colors ──
GREEN_FILL = PatternFill('solid', fgColor='C8E6C9')
YELLOW_FILL = PatternFill('solid', fgColor='FFF9C4')
GRAY_FILL = PatternFill('solid', fgColor='EEEEEE')
ORANGE_FILL = PatternFill('solid', fgColor='FFE0B2')
BLUE_FILL = PatternFill('solid', fgColor='BBDEFB')
RED_FILL = PatternFill('solid', fgColor='FFCDD2')
HEADER_FILL = PatternFill('solid', fgColor='1565C0')
HEADER_FILL2 = PatternFill('solid', fgColor='FF7043')
HEADER_FILL3 = PatternFill('solid', fgColor='43A047')
HEADER_FILL4 = PatternFill('solid', fgColor='7B1FA2')
HEADER_FILL5 = PatternFill('solid', fgColor='F9A825')
HEADER_FONT = Font(name='Arial', bold=True, color='FFFFFF', size=11)
TITLE_FONT = Font(name='Arial', bold=True, size=14, color='333333')
BOLD_FONT = Font(name='Arial', bold=True, size=11, color='333333')
NORMAL_FONT = Font(name='Arial', size=11, color='333333')
SMALL_FONT = Font(name='Arial', size=10, color='666666')
RED_FONT = Font(name='Arial', size=11, color='C62828', bold=True)
GRAY_FONT = Font(name='Arial', size=11, color='999999')
THIN_BORDER = Border(
    left=Side(style='thin', color='DDDDDD'),
    right=Side(style='thin', color='DDDDDD'),
    top=Side(style='thin', color='DDDDDD'),
    bottom=Side(style='thin', color='DDDDDD'),
)
CENTER = Alignment(horizontal='center', vertical='center', wrap_text=True)
LEFT_WRAP = Alignment(horizontal='left', vertical='center', wrap_text=True)

def style_header(ws, row, cols, fill=HEADER_FILL):
    for c in range(1, cols + 1):
        cell = ws.cell(row=row, column=c)
        cell.font = HEADER_FONT
        cell.fill = fill
        cell.alignment = CENTER
        cell.border = THIN_BORDER

def style_row(ws, row, cols, font=NORMAL_FONT, fill=None):
    for c in range(1, cols + 1):
        cell = ws.cell(row=row, column=c)
        cell.font = font
        if fill:
            cell.fill = fill
        cell.alignment = LEFT_WRAP if c > 2 else CENTER
        cell.border = THIN_BORDER

def set_col_widths(ws, widths):
    for i, w in enumerate(widths, 1):
        ws.column_dimensions[get_column_letter(i)].width = w

# ══════════════════════════════════════════
# Sheet 1: 进度总览
# ══════════════════════════════════════════
ws1 = wb.active
ws1.title = '进度总览'
ws1.sheet_properties.tabColor = '1565C0'

ws1.merge_cells('A1:I1')
ws1['A1'] = '小辽 — 项目进程监督表'
ws1['A1'].font = TITLE_FONT
ws1['A1'].alignment = Alignment(horizontal='center', vertical='center')
ws1.row_dimensions[1].height = 36

ws1.merge_cells('A2:I2')
ws1['A2'] = '更新日期：2026-08-03  |  当前阶段：Phase 0 收尾 → Phase 1 启动  |  前端策略：实习生分组，节奏放缓'
ws1['A2'].font = SMALL_FONT
ws1['A2'].alignment = Alignment(horizontal='center', vertical='center')
ws1.row_dimensions[2].height = 24

headers = ['序号', '模块/功能', '优先级', '负责人', '依赖项', '状态', '计划开始', '预计完成', '备注']
for c, h in enumerate(headers, 1):
    ws1.cell(row=4, column=c, value=h)
style_header(ws1, 4, len(headers))
ws1.row_dimensions[4].height = 28

overview_data = [
    # Infrastructure
    ['', '【基础设施】', '', '', '', '', '', '', ''],
    ['1', '项目骨架(Maven多模块)', 'P0', '后端', '无', '✅ 已完成', '07/31', '07/31', 'common/api/task三模块'],
    ['2', '数据库7张表+Flyway', 'P0', '后端', '无', '✅ 已完成', '07/31', '07/31', 'PostgreSQL+pgvector'],
    ['3', 'Entity+Mapper(14文件)', 'P0', '后端', 'DB表', '✅ 已完成', '07/31', '07/31', 'MyBatis-Plus BaseMapper'],
    ['4', 'JWT登录体系', 'P0', '后端', '用户表', '✅ 已完成', '08/03', '08/03', 'TokenUtil+AuthController+UserService'],
    ['5', 'Docker Compose部署', 'P0', '后端', '无', '✅ 已完成', '07/31', '07/31', 'PG+Redis+API三服务'],
    ['6', '企微对接代码(Java)', 'P0', '后端', '无', '✅ 已完成', '07/31', '07/31', '回调+解密+异步+发消息+卡片'],
    ['7', 'Docker启动验证', 'P0', '后端', 'Docker', '⏳ 待开始', '-', '-', '从未实际启动过，需优先验证'],
    # Backend APIs
    ['', '【后端业务接口】', '', '', '', '', '', '', ''],
    ['8', 'M1 签到情绪API', 'P0', '后端', 'M1表', '⏳ 待开始', '-', '-', 'POST签到+GET今日+GET历史'],
    ['9', 'M2 算术达人API', 'P0', '后端', 'M2表', '⏳ 待开始', '-', '-', '存游戏记录+排名'],
    ['10', 'M2 记忆翻牌API', 'P0', '后端', 'M2表', '⏳ 待开始', '-', '-', '存游戏记录+排名'],
    ['11', 'M2 词语连连看API', 'P0', '后端', 'M2表', '⏳ 待开始', '-', '-', '存游戏记录+排名'],
    ['12', 'M2 找不同API', 'P0', '后端', 'M2表', '⏳ 待开始', '-', '-', '存游戏记录+排名'],
    ['13', 'M3 心理练习API', 'P0', '后端', 'M3表', '⏳ 待开始', '-', '-', '练习列表+提交+历史'],
    ['14', 'M5 社区API(基础)', 'P1', '后端', '--', '⏳ 待开始', '-', '-', '发帖+帖子列表+点赞'],
    ['15', 'M9 测评记录API', 'P0', '后端', 'M9表+AI', '⏳ 待开始', '-', '-', '存AI测评结果+历史'],
    # AI Engine
    ['', '【Python AI引擎】', '', '', '', '', '', '', ''],
    ['16', 'Python FastAPI骨架', 'P0', 'AI同事', '无', '📋 同事负责', '-', '-', 'LangChain+FastAPI'],
    ['17', '主Agent(DeepSeek-V3)', 'P0', 'AI同事', '骨架', '📋 同事负责', '-', '-', '对话生成'],
    ['18', '副Agent(检验+训练)', 'P0', 'AI同事', '主Agent', '📋 同事负责', '-', '-', '安全/适老/CBT检验'],
    ['19', 'RAG三源检索', 'P0', 'AI同事', '知识库', '📋 同事负责', '-', '-', '用户画像+CBT知识+教训库'],
    ['20', 'Java↔Python联调', 'P0', '后端+AI', '双方就绪', '⏳ 待开始', '-', '-', 'RestClient调通'],
    # Frontend A
    ['', '【前端A组(实习)】', '', '', '', '', '', '', ''],
    ['21', '小程序框架搭建', 'P0', '前端A(实习)', '无', '⏳ 待开始', '-', '-', '微信原生/uni-app待定'],
    ['22', '首页', 'P0', '前端A(实习)', '框架', '⏳ 待开始', '-', '-', '4入口+签到横幅+连续天数'],
    ['23', '签到情绪页', 'P0', '前端A(实习)', '首页+M1接口', '⏳ 待开始', '-', '-', '选心情+写文字+提交'],
    ['24', '心理练习列表+详情', 'P0', '前端A(实习)', '首页+M3接口', '⏳ 待开始', '-', '-', '4个练习+引导步骤+输入'],
    ['25', '测评记录页', 'P0', '前端A(实习)', '首页+M9接口', '⏳ 待开始', '-', '-', '历史测评结果+趋势'],
    # Frontend B
    ['', '【前端B组(实习)】', '', '', '', '', '', '', ''],
    ['26', '脑力游戏列表', 'P0', '前端B(实习)', '框架', '⏳ 待开始', '-', '-', '4款游戏卡片入口'],
    ['27', '算术达人页', 'P0', '前端B(实习)', '游戏列表+M2接口', '⏳ 待开始', '-', '-', '计时+题目+选项+得分'],
    ['28', '记忆翻牌页', 'P0', '前端B(实习)', '游戏列表+M2接口', '⏳ 待开始', '-', '-', '翻牌配对动效'],
    ['29', '词语连连看页', 'P0', '前端B(实习)', '游戏列表+M2接口', '⏳ 待开始', '-', '-', '连线交互'],
    ['30', '找不同页', 'P0', '前端B(实习)', '游戏列表+M2接口', '⏳ 待开始', '-', '-', '点击差异+高亮'],
    ['31', '社区页', 'P0', '前端B(实习)', '框架+M5接口', '⏳ 待开始', '-', '-', '帖子流+点赞+发帖'],
    ['32', '个人进展页', 'P1', '前端B(实习)', '全部接口', '⏳ 待开始', '-', '-', '签到趋势+游戏成绩+练习记录'],
]

row = 5
for data in overview_data:
    ws1.row_dimensions[row].height = 26
    is_section = data[0] == ''
    is_done = '已完成' in str(data[5])
    is_waiting = '待开始' in str(data[5]) and data[0] == ''
    is_risk = '⚠️' in str(data[5])

    for c, val in enumerate(data, 1):
        ws1.cell(row=row, column=c, value=val)

    if is_section:
        style_row(ws1, row, len(headers), BOLD_FONT, BLUE_FILL)
    elif is_done:
        style_row(ws1, row, len(headers), NORMAL_FONT, GREEN_FILL)
    elif '同事负责' in str(data[5]):
        style_row(ws1, row, len(headers), NORMAL_FONT, YELLOW_FILL)
    else:
        style_row(ws1, row, len(headers))
    row += 1

# Summary row
ws1.merge_cells(f'A{row}:D{row}')
ws1.cell(row=row, column=1, value='合计：7已完成 / 1待验证 / 20待开始 / 5同事负责')
ws1.cell(row=row, column=1).font = BOLD_FONT
ws1.cell(row=row, column=1).alignment = CENTER
style_row(ws1, row, len(headers), BOLD_FONT)

set_col_widths(ws1, [6, 28, 8, 14, 16, 16, 12, 12, 32])

# ══════════════════════════════════════════
# Sheet 2: 后端任务
# ══════════════════════════════════════════
ws2 = wb.create_sheet('后端任务')
ws2.sheet_properties.tabColor = 'FF7043'

ws2.merge_cells('A1:H1')
ws2['A1'] = '后端开发任务清单'
ws2['A1'].font = TITLE_FONT
ws2['A1'].alignment = Alignment(horizontal='center', vertical='center')
ws2.row_dimensions[1].height = 36

headers2 = ['序号', '任务', '模块', '接口', '优先级', '状态', '预计工时', '说明']
for c, h in enumerate(headers2, 1):
    ws2.cell(row=3, column=c, value=h)
style_header(ws2, 3, len(headers2), HEADER_FILL2)

backend_tasks = [
    ['1', 'Docker启动验证', '基础设施', '--', 'P0-紧急', '⏳ 待开始', '0.5天', 'docker-compose up → PG/Flyway/Spring Boot 全部跑通'],
    ['2', '签到-今日签到', 'M1', 'POST /api/checkin', 'P0', '⏳ 待开始', '0.5天', '校验mood值+今日去重+存DB+返warm greeting'],
    ['3', '签到-查询今日', 'M1', 'GET /api/checkin/today', 'P0', '⏳ 待开始', '0.3天', '查今日是否已签'],
    ['4', '签到-历史趋势', 'M1', 'GET /api/checkin/history', 'P1', '⏳ 待开始', '0.3天', '近30天签到+情绪趋势'],
    ['5', '游戏-算术记录', 'M2', 'POST /api/game/record', 'P0', '⏳ 待开始', '0.3天', '存游戏类型+得分+用时'],
    ['6', '游戏-排行榜', 'M2', 'GET /api/game/ranking', 'P1', '⏳ 待开始', '0.3天', '好友/社区排行'],
    ['7', '练习-练习列表', 'M3', 'GET /api/exercise/list', 'P0', '⏳ 待开始', '0.3天', '返回4个练习+完成状态'],
    ['8', '练习-提交练习', 'M3', 'POST /api/exercise/submit', 'P0', '⏳ 待开始', '0.5天', '存用户填写内容'],
    ['9', '练习-练习历史', 'M3', 'GET /api/exercise/history', 'P1', '⏳ 待开始', '0.3天', '已完成练习列表'],
    ['10', '社区-发帖', 'M5', 'POST /api/community/post', 'P0', '⏳ 待开始', '0.5天', '帖子内容+存DB'],
    ['11', '社区-帖子流', 'M5', 'GET /api/community/posts', 'P0', '⏳ 待开始', '0.5天', '分页+排序（热门/最新）'],
    ['12', '社区-点赞', 'M5', 'POST /api/community/like', 'P0', '⏳ 待开始', '0.3天', '点赞/取消点赞'],
    ['13', '测评-存记录', 'M9', 'POST /api/assessment/record', 'P0', '⏳ 待开始', '0.3天', 'AI返回测评结果→存DB'],
    ['14', '测评-查历史', 'M9', 'GET /api/assessment/history', 'P0', '⏳ 待开始', '0.3天', '用户测评记录+趋势'],
    ['15', '企微-联调测试', 'M7', '--', 'P0', '⏳ 待开始', '1天', 'ngrok暴露+企微后台配回调+端到端测试'],
    ['16', '企微-用户绑定补全', 'M7', '--', 'P0', '⏳ 待开始', '0.5天', '企微external_userid↔users表完善'],
]

row = 4
for data in backend_tasks:
    ws2.row_dimensions[row].height = 26
    for c, val in enumerate(data, 1):
        ws2.cell(row=row, column=c, value=val)
    is_done = '已完成' in str(data[5])
    style_row(ws2, row, len(headers2), fill=GREEN_FILL if is_done else None)
    row += 1

set_col_widths(ws2, [6, 24, 8, 32, 8, 16, 10, 40])

# ══════════════════════════════════════════
# Sheet 3: 前端A组(实习)
# ══════════════════════════════════════════
ws3 = wb.create_sheet('前端A组(实习)')
ws3.sheet_properties.tabColor = '43A047'

ws3.merge_cells('A1:H1')
ws3['A1'] = '前端A组 — 实习任务（节奏放缓 · 给足学习时间）'
ws3['A1'].font = TITLE_FONT
ws3['A1'].alignment = Alignment(horizontal='center', vertical='center')
ws3.row_dimensions[1].height = 36

ws3.merge_cells('A2:H2')
ws3['A2'] = '⚠️ 实习生注意：每项任务预留学习+试错时间，预计工时仅为参考，以实际掌握为准。有问题随时问，不赶进度。'
ws3['A2'].font = Font(name='Arial', size=11, color='C62828', bold=True)
ws3['A2'].alignment = Alignment(horizontal='center', vertical='center')
ws3.row_dimensions[2].height = 28

headers3 = ['序号', '页面', '需要后端接口', '技术要点', '状态', '前置知识', '参考工时', '给实习生的建议']
for c, h in enumerate(headers3, 1):
    ws3.cell(row=4, column=c, value=h)
style_header(ws3, 4, len(headers3), HEADER_FILL3)

team_a = [
    ['1', '小程序框架搭建', '验证token接口', '微信小程序注册、项目结构、页面路由、token读取验证', '⏳ 待开始', 'HTML/CSS/JS基础', '2天', '先看微信小程序官方文档的"起步"章节，照例子搭，不用从头想'],
    ['2', '首页', '无(纯展示)', '大按钮布局、签到天数展示、模块色块、flex布局', '⏳ 待开始', '框架OK', '2天', '参考原型里的首页设计，先写死数据，接口好了再接'],
    ['3', '签到情绪页', 'POST签到+GET今日', '3个心情大按钮选中态、文字输入框、提交toast', '⏳ 待开始', '首页OK+wx.request', '2天', '先做UI，再学wx.request调接口，心情按钮用单选状态切换'],
    ['4', '心理练习列表页', 'GET练习列表', '卡片布局、已完成/推荐/新 状态标签、列表滚动', '⏳ 待开始', '首页OK+wx.request', '1.5天', '和首页类似的设计思路，重点是卡片的状态区分'],
    ['5', '心理练习详情+提交', 'POST提交练习', '步骤引导UI、多行输入框、提交按钮+loading', '⏳ 待开始', '列表页OK', '2天', '先做步骤引导的文字排版，再做输入框，最后接接口'],
    ['6', '测评记录页', 'GET测评历史', '历史列表、简单趋势展示（可用canvas或echarts）', '⏳ 待开始', '首页OK+图表库', '2天', '图表可以最后做，先用列表形式展示记录就行'],
]

row = 5
for data in team_a:
    ws3.row_dimensions[row].height = 52
    for c, val in enumerate(data, 1):
        ws3.cell(row=row, column=c, value=val)
    style_row(ws3, row, len(headers3))
    row += 1

set_col_widths(ws3, [6, 22, 22, 36, 14, 18, 10, 40])

# ══════════════════════════════════════════
# Sheet 4: 前端B组(实习)
# ══════════════════════════════════════════
ws4 = wb.create_sheet('前端B组(实习)')
ws4.sheet_properties.tabColor = '7B1FA2'

ws4.merge_cells('A1:H1')
ws4['A1'] = '前端B组 — 实习任务（游戏交互为主 · 需要更多练习时间）'
ws4['A1'].font = TITLE_FONT
ws4['A1'].alignment = Alignment(horizontal='center', vertical='center')
ws4.row_dimensions[1].height = 36

ws4.merge_cells('A2:H2')
ws4['A2'] = '⚠️ 游戏交互比普通页面复杂，建议先做游戏列表，再做简单游戏（算术），最后做复杂游戏（翻牌/找不同）。'
ws4['A2'].font = Font(name='Arial', size=11, color='C62828', bold=True)
ws4['A2'].alignment = Alignment(horizontal='center', vertical='center')
ws4.row_dimensions[2].height = 28

for c, h in enumerate(headers3, 1):
    ws4.cell(row=4, column=c, value=h)
style_header(ws4, 4, len(headers3), HEADER_FILL4)

team_b = [
    ['1', '脑力游戏列表页', '无(纯展示)', '4个游戏卡片、图标+标签+箭头、点击跳转', '⏳ 待开始', 'HTML/CSS/JS基础', '1.5天', '和A组共享框架，专注于卡片布局，用原型里的设计就行'],
    ['2', '算术达人游戏页', 'POST游戏记录', '计时器(setInterval)、随机出题、4选项按钮、答对/错动效、得分、进度条', '⏳ 待开始', 'JS逻辑能力、setInterval', '3天', '先做出题+选项的静态版本，再加计时器，动效最后做。建议先console.log调试逻辑'],
    ['3', '记忆翻牌游戏页', 'POST游戏记录', '网格布局、翻牌动画(css transform)、配对逻辑、记忆时间', '⏳ 待开始', 'CSS动画、数组操作', '3.5天', '难点在翻牌动画和配对逻辑，建议先用卡片正反面做基础，动画用transition'],
    ['4', '词语连连看页', 'POST游戏记录', 'Canvas/SVG画线、拖拽或点击连线、词语配对数据', '⏳ 待开始', 'Canvas或touch事件', '3天', '可以先做点击连线版（点第一个词再点第二个词连线），拖拽版后期再优化'],
    ['5', '找不同游戏页', 'POST游戏记录', '两张对比图展示、点击差异区域、圈出标记、倒计时', '⏳ 待开始', '图片加载、绝对定位', '3天', '需要UI设计师提供对比图素材，可以先用色块模拟，素材就绪再替换'],
    ['6', '社区页', 'GET帖子流+POST点赞+POST发帖', '列表滚动、点赞切换、发帖弹窗/页面、刷新加载', '⏳ 待开始', '列表渲染、wx.request', '2天', '点赞按钮要处理状态切换(已赞/未赞)，发帖先做文字帖，图片帖后期'],
    ['7', '个人进展页', '全部GET接口', '签到趋势折线图、游戏成绩柱状图、练习完成统计', '⏳ 待开始', '图表库(echarts)', '2天', '等所有接口就绪后再做，先做静态展示再换图表'],
]

row = 5
for data in team_b:
    ws4.row_dimensions[row].height = 52
    for c, val in enumerate(data, 1):
        ws4.cell(row=row, column=c, value=val)
    style_row(ws4, row, len(headers3))
    row += 1

set_col_widths(ws4, [6, 22, 22, 38, 14, 18, 10, 42])

# ══════════════════════════════════════════
# Sheet 5: AI引擎
# ══════════════════════════════════════════
ws5 = wb.create_sheet('AI引擎')
ws5.sheet_properties.tabColor = 'F9A825'

ws5.merge_cells('A1:G1')
ws5['A1'] = 'Python AI 引擎 — 同事负责'
ws5['A1'].font = TITLE_FONT
ws5['A1'].alignment = Alignment(horizontal='center', vertical='center')
ws5.row_dimensions[1].height = 36

headers5 = ['序号', '任务', '依赖', '状态', '备注', '后端依赖', '联调点']
for c, h in enumerate(headers5, 1):
    ws5.cell(row=3, column=c, value=h)
style_header(ws5, 3, len(headers5), HEADER_FILL5)

ai_tasks = [
    ['1', 'Python FastAPI 骨架', '无', '📋 待同事启动', 'LangChain + FastAPI + Docker', 'AiService已写好', 'HTTP接口格式对齐'],
    ['2', '主Agent (DeepSeek-V3)', '骨架', '📋 待同事启动', '对话生成、意图识别、聊天记忆', 'AiService.chat()调用', '请求/响应格式确认'],
    ['3', '副Agent (检验+训练)', '主Agent', '📋 待同事启动', '安全检验、适老检验、CBT检验、错误聚类→补丁', '无直接依赖', '检验结果格式'],
    ['4', 'RAG三源检索', '知识库', '📋 待同事启动', '用户画像库 + CBT知识库 + 教训库', '共享PG+pgvector', '向量存储结构对齐'],
    ['5', 'CBT知识库构建', '无', '📋 待同事启动', '参考CBT知识库_Agent版.md', '无', '无'],
    ['6', '端到端联调', '全部就绪', '⏳ 等待', 'Java↔Python↔企微 三条链路打通', 'AiService RestClient', 'ping通 → chat联调 → 完整对话'],
]

row = 4
for data in ai_tasks:
    ws5.row_dimensions[row].height = 26
    for c, val in enumerate(data, 1):
        ws5.cell(row=row, column=c, value=val)
    style_row(ws5, row, len(headers5))
    row += 1

set_col_widths(ws5, [6, 28, 18, 16, 36, 22, 26])

# ══════════════════════════════════════════
# Sheet 6: 里程碑
# ══════════════════════════════════════════
ws6 = wb.create_sheet('里程碑')
ws6.sheet_properties.tabColor = '00897B'

ws6.merge_cells('A1:E1')
ws6['A1'] = '项目里程碑'
ws6['A1'].font = TITLE_FONT
ws6['A1'].alignment = Alignment(horizontal='center', vertical='center')
ws6.row_dimensions[1].height = 36

headers6 = ['阶段', '目标', '关键交付物', '依赖条件', '风险']
for c, h in enumerate(headers6, 1):
    ws6.cell(row=3, column=c, value=h)
style_header(ws6, 3, len(headers6), PatternFill('solid', fgColor='00897B'))

milestones = [
    ['Phase 0: 基础设施(2周)', '项目骨架+DB+部署就绪', 'Docker启动成功\nPG建表成功\n企微回调验证通过', '全部代码已写，待实际启动验证', '⚠️ 从未实际启动过Docker，未知坑'],
    ['Phase 1: M7企微+AI对话(2周)', 'AI对话跑通+老人能聊天', '企微消息收发正常\nPython AI引擎联调成功\n小程序卡片跳转成功', 'Docker启动成功\nAI引擎就绪\n企微后台配置', '⚠️ 最大不确定因素：AI同事进度\n⚠️ ngrok公网暴露'],
    ['Phase 2: M1+M3功能(2周)', '签到+练习可用', '签到接口+页面可用\n练习接口+页面可用\n前端A组交付', 'M7对话跑通\n前端A组学习完毕', '前端实习生效率波动'],
    ['Phase 3: M2脑力游戏(3周)', '4款游戏可用', '4款游戏接口+页面\n游戏记录存入DB', 'M1+M3联调经验\n前端B组学习完毕', '⚠️ 游戏交互复杂度最高\n需UI设计素材'],
    ['Phase 4: M9+M5收尾(2周)', '测评+社区可用', '测评记录+社区帖子\n全部接口上线', 'AI引擎可做对话式测评\n前端两组都熟悉流程', '社区内容审核策略待定'],
]

row = 4
for data in milestones:
    ws6.row_dimensions[row].height = 60
    for c, val in enumerate(data, 1):
        ws6.cell(row=row, column=c, value=val)
    style_row(ws6, row, len(headers6))
    row += 1

set_col_widths(ws6, [24, 24, 30, 28, 30])

# ── Save ──
output_path = '/Users/robot/WorkBuddy/2026-07-31-08-30-38/xiaoliao/prototype/项目进程监督表.xlsx'
wb.save(output_path)
print(f'Saved to {output_path}')
