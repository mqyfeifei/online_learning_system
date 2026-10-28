# -*- coding: utf-8 -*-
"""生成实训报告全部 draw.io 图（10 张）。"""
import os
from xml.sax.saxutils import escape
def esc(s):
    return escape(s, {'"': "&quot;", "'": "&apos;"})

OUT_DIR = r"D:\mqy\java\SpringBoot\online_learning_system\docs\实训报告\图"
os.makedirs(OUT_DIR, exist_ok=True)

_id = [100]
def nid():
    _id[0] += 1
    return f"n{_id[0]}"

# ---- 样式 ----
S_BOX   = "rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;"
S_BOXG  = "rounded=1;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;"
S_BOXO  = "rounded=1;whiteSpace=wrap;html=1;fillColor=#ffe6cc;strokeColor=#d79b00;"
S_BOXP  = "rounded=1;whiteSpace=wrap;html=1;fillColor=#e1d5e7;strokeColor=#9673a6;"
S_GRAY  = "rounded=1;whiteSpace=wrap;html=1;fillColor=#f5f5f5;strokeColor=#666666;"
S_DB    = ("shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=14;"
           "fillColor=#d5e8d4;strokeColor=#82b366;")
S_ACTOR = "shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;"
S_USE   = "ellipse;whiteSpace=wrap;html=1;fillColor=#fff2cc;strokeColor=#d6b656;"
S_BOUND = ("rounded=1;whiteSpace=wrap;html=1;fillColor=none;strokeColor=#666666;dashed=1;"
           "verticalAlign=top;fontStyle=1;fontSize=14;")
S_START = "ellipse;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;fontStyle=1;"
S_END   = "ellipse;whiteSpace=wrap;html=1;fillColor=#f8cecc;strokeColor=#b85450;fontStyle=1;"
S_DEC   = "rhombus;whiteSpace=wrap;html=1;fillColor=#ffe6cc;strokeColor=#d79b00;"
S_BAND  = "rounded=0;whiteSpace=wrap;html=1;fillColor=#f5f5f5;strokeColor=#999999;verticalAlign=top;fontStyle=1;fontSize=13;align=left;spacingLeft=8;"
S_EDGE  = "edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;jettySize=auto;orthogonalLoop=1;fontSize=11;"
S_EDGE_L= S_EDGE + "exitX=0.25;exitY=1;entryX=0.25;entryY=0;"
S_EDGE_R= S_EDGE + "exitX=0.75;exitY=0;entryX=0.75;entryY=1;"
S_LIFE  = "html=1;endArrow=none;dashed=1;strokeColor=#666666;"
S_MSG   = "html=1;verticalAlign=bottom;endArrow=block;curved=0;rounded=0;fontSize=11;"
S_MSGR  = "html=1;verticalAlign=bottom;endArrow=open;dashed=1;curved=0;rounded=0;fontSize=11;"
S_ROW   = ("text;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;spacingLeft=4;"
           "spacingRight=4;overflow=hidden;rotatable=0;whiteSpace=wrap;html=1;fontSize=11;")
S_TITLE = "text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=16;fontStyle=1;"

def cell(id_, value, style, x=None, y=None, w=120, h=40, parent="1", vertex=True, edge_attrs=""):
    geo = ""
    if vertex:
        geo = f'<mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/>'
    else:
        geo = '<mxGeometry relative="1" as="geometry"/>'
    return (f'<mxCell id="{id_}" value="{esc(value)}" style="{style}" vertex="{1 if vertex else 0}" '
            f'edge="{0 if vertex else 1}" parent="{parent}">{geo}{edge_attrs}</mxCell>')

def edge(id_, src, tgt, label="", style=S_EDGE, points=""):
    pts = ""
    if points:
        pts = f'<Array as="points">{points}</Array>'
    return (f'<mxCell id="{id_}" value="{esc(label)}" style="{style}" vertex="0" edge="1" parent="1" '
            f'source="{src}" target="{tgt}"><mxGeometry relative="1" as="geometry">{pts}</mxGeometry></mxCell>')

def freeline(id_, label, style, x1, y1, x2, y2):
    return (f'<mxCell id="{id_}" value="{esc(label)}" style="{style}" vertex="0" edge="1" parent="1">'
            f'<mxGeometry relative="1" as="geometry">'
            f'<mxPoint x="{x1}" y="{y1}" as="sourcePoint"/><mxPoint x="{x2}" y="{y2}" as="targetPoint"/>'
            f'</mxGeometry></mxCell>')

def uml_class(id0, x, y, w, title, rows, header_fill="#dae8fc", row_h=22, head_h=26, stereo=None):
    """swimlane 形式的 UML 类/ER 实体。返回 (cells, bottom_y)。"""
    cells = []
    n = len(rows)
    h = head_h + row_h * n
    head = title
    if stereo:
        head = f"&lt;&lt;{stereo}&gt;&gt;<br>{title}" if "<br>" not in title else title
    c = cell(id0, head,
             f"swimlane;fontStyle=1;childLayout=stackLayout;horizontal=1;startSize={head_h};"
             f"horizontalStack=0;resizeParent=0;collapsible=0;marginBottom=0;whiteSpace=wrap;html=1;"
             f"fillColor={header_fill};strokeColor=#6c8ebf;fontSize=12;",
             x, y, w, h)
    cells.append(c)
    yy = y + head_h
    for i, r in enumerate(rows):
        rid = f"{id0}r{i}"
        bold = "fontStyle=1;" if r.startswith("*") else ""
        r = r.lstrip("*")
        cells.append(cell(rid, r, S_ROW + bold, 0, yy - y, w, row_h, parent=id0))
        yy += row_h
    return cells, y + h

def write_diagram(filename, cells, width=1200, height=900):
    inner = "\n".join(cells)
    xml = f'''<mxfile host="app.diagrams.net" agent="report-generator" version="24.7.8">
  <diagram id="{nid()}" name="页面-1">
    <mxGraphModel dx="1200" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="{width}" pageHeight="{height}" math="0" shadow="0">
      <root>
        <mxCell id="0"/>
        <mxCell id="1" parent="0"/>
{inner}
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>'''
    path = os.path.join(OUT_DIR, filename)
    with open(path, "w", encoding="utf-8") as f:
        f.write(xml)
    print("生成:", path)

# ============================================================
# 图1-1 系统总体用例图
# ============================================================
_id = [100]
c = []
c.append(cell("bd", "在线学习平台", S_BOUND, 250, 30, 700, 1180))
use = [
    ("u_reg",  "登录 / 注册",           320, 70),
    ("u_brow", "浏览 / 搜索课程",       320, 140),
    ("u_enrl", "选课 / 退课",           320, 210),
    ("u_aud",  "选课审核",              320, 280),
    ("u_learn","章节学习与打卡",        320, 350),
    ("u_exam", "在线测验",              320, 430),
    ("u_grade","查看成绩与解析",        320, 510),
    ("u_hw",   "提交作业",              320, 580),
    ("u_note", "学习笔记 / 收藏",       320, 650),
    ("u_disc", "讨论区发帖回帖",        320, 720),
    ("u_cc",   "创建 / 维护课程",       640, 70),
    ("u_ch",   "维护章节与课件",        640, 140),
    ("u_q",    "维护题库",              640, 210),
    ("u_p",    "组卷与发布试卷",        640, 280),
    ("u_a",    "布置作业",              640, 350),
    ("u_mark", "批阅作业与主观题",      640, 430),
    ("u_stu",  "学生名单管理",          640, 510),
    ("u_dash", "查看学情看板",          640, 580),
    ("u_audit","课程审核",              640, 680),
    ("u_user", "用户管理 / 批量导入",   640, 750),
    ("u_cat",  "分类管理",              640, 820),
    ("u_not",  "公告管理",              640, 890),
    ("u_stat", "平台统计",              640, 960),
]
for uid, label, x, y in use:
    c.append(cell(uid, label, S_USE, x, y, 190, 50))
c.append(cell("a_guest", "游客",   S_ACTOR, 90, 100, 40, 80))
c.append(cell("a_stu",   "学生",   S_ACTOR, 90, 400, 40, 80))
c.append(cell("a_tea",   "教师",   S_ACTOR, 1080, 220, 40, 80))
c.append(cell("a_adm",   "管理员", S_ACTOR, 1080, 780, 40, 80))
guest = ["u_reg", "u_brow"]
for u in guest:
    c.append(edge(nid(), "a_guest", u))
stu = ["u_reg", "u_brow", "u_enrl", "u_learn", "u_exam", "u_grade", "u_hw", "u_note", "u_disc"]
for u in stu:
    c.append(edge(nid(), "a_stu", u))
tea = ["u_cc", "u_ch", "u_q", "u_p", "u_a", "u_mark", "u_stu", "u_dash", "u_aud", "u_disc"]
for u in tea:
    c.append(edge(nid(), "a_tea", u))
adm = ["u_audit", "u_user", "u_cat", "u_not", "u_stat"]
for u in adm:
    c.append(edge(nid(), "a_adm", u))
c.append(edge(nid(), "u_enrl", "u_aud", "&lt;&lt;extend&gt;&gt; 需审核时", S_EDGE))
c.append(edge(nid(), "u_exam", "u_grade", "&lt;&lt;include&gt;&gt;", S_EDGE))
c.append(edge(nid(), "u_hw", "u_mark", "", S_EDGE))
c.append(edge(nid(), "u_cc", "u_audit", "&lt;&lt;include&gt;&gt;", S_EDGE))
write_diagram("图1-1_系统总体用例图.drawio", c, width=1220, height=1260)

# ============================================================
# 图1-2 核心业务流程图
# ============================================================
_id = [100]
c = []
c.append(cell("s0", "开始", S_START, 40, 60, 90, 40))
c.append(cell("t_create", "教师创建课程（草稿）", S_BOX, 170, 55, 160, 50))
c.append(cell("t_edit", "完善章节 / 课件 / 题库", S_BOX, 170, 160, 160, 50))
c.append(cell("t_submit", "提交课程审核", S_BOX, 390, 160, 150, 50))
c.append(cell("d_audit", "管理员审核\n是否通过？", S_DEC, 590, 145, 130, 80))
c.append(cell("t_reject", "驳回并填写理由", S_BOXP, 600, 280, 130, 46))
c.append(cell("t_pub", "课程发布（学生可见）", S_BOXG, 780, 160, 150, 50))
c.append(cell("s_brow", "学生浏览 / 选课", S_BOX, 790, 270, 140, 46))
c.append(cell("d_audit2", "课程是否开启\n选课审核？", S_DEC, 775, 360, 160, 80))
c.append(cell("t_appr", "教师审批选课", S_BOXO, 990, 290, 130, 46))
c.append(cell("d_appr", "是否通过？", S_DEC, 1000, 380, 110, 70))
c.append(cell("t_ok", "选课成功（在读）", S_BOXG, 790, 490, 140, 46))
c.append(cell("s_learn", "逐章学习 + 打卡", S_BOX, 790, 580, 140, 46))
c.append(cell("s_exam", "参加测验（时间窗内一次提交）", S_BOX, 990, 580, 190, 46))
c.append(cell("d_essay", "含问答题？", S_DEC, 1030, 670, 110, 70))
c.append(cell("t_auto", "客观题自动判分 + 解析", S_BOX, 990, 780, 190, 46))
c.append(cell("t_manual", "教师批阅（作业/主观题）", S_BOXP, 790, 780, 160, 46))
c.append(cell("s_stat", "学情看板 / 成绩统计", S_BOX, 790, 880, 150, 46))
c.append(cell("e0", "结束", S_END, 470, 885, 90, 40))
c.append(edge(nid(), "s0", "t_create"))
c.append(edge(nid(), "t_create", "t_edit"))
c.append(edge(nid(), "t_edit", "t_submit", "内容完备"))
c.append(edge(nid(), "t_submit", "d_audit"))
c.append(edge(nid(), "d_audit", "t_reject", "否"))
c.append(edge(nid(), "t_reject", "t_edit", "修改课程",
           S_EDGE, '<mxPoint x="665" y="185"/>'))
c.append(edge(nid(), "d_audit", "t_pub", "是"))
c.append(edge(nid(), "t_pub", "s_brow"))
c.append(edge(nid(), "s_brow", "d_audit2"))
c.append(edge(nid(), "d_audit2", "t_appr", "是"))
c.append(edge(nid(), "d_audit2", "t_ok", "否"))
c.append(edge(nid(), "t_appr", "d_appr"))
c.append(edge(nid(), "d_appr", "t_ok", "通过", S_EDGE, '<mxPoint x="1055" y="513"/>'))
c.append(edge(nid(), "d_appr", "s_brow", "拒绝并通知",
           S_EDGE, '<mxPoint x="1190" y="415"/><mxPoint x="1190" y="243"/><mxPoint x="860" y="243"/>'))
c.append(edge(nid(), "t_ok", "s_learn"))
c.append(edge(nid(), "s_learn", "s_exam"))
c.append(edge(nid(), "s_exam", "d_essay"))
c.append(edge(nid(), "d_essay", "t_auto", "否"))
c.append(edge(nid(), "d_essay", "t_manual", "是", S_EDGE, '<mxPoint x="870" y="705"/>'))
c.append(edge(nid(), "t_auto", "s_stat", "汇总", S_EDGE, '<mxPoint x="1085" y="903"/>'))
c.append(edge(nid(), "t_manual", "s_stat"))
c.append(edge(nid(), "s_stat", "e0"))
write_diagram("图1-2_核心业务流程图.drawio", c, width=1200, height=990)

# ============================================================
# 图2-1 系统总体架构图
# ============================================================
_id = [100]
c = []
c.append(cell("b1", "客户端层", S_BAND, 40, 40, 1080, 90))
c.append(cell("c1", "桌面浏览器（Chrome / Edge 等，无需插件）", S_BOX, 80, 78, 560, 36))
c.append(cell("c2", "演示账号：学生 / 教师 / 管理员三种角色会话", S_GRAY, 680, 78, 400, 36))
c.append(cell("b2", "前端应用层（Vue 3 单页应用，Vite 构建）", S_BAND, 40, 150, 1080, 120))
c.append(cell("f1", "前台学习端页面", S_BOX, 80, 190, 240, 60))
c.append(cell("f2", "教师端页面", S_BOX, 350, 190, 240, 60))
c.append(cell("f3", "管理端页面", S_BOX, 620, 190, 240, 60))
c.append(cell("f4", "Router / Pinia / Axios / Element Plus / 主题样式", S_BOXP, 890, 190, 200, 60))
c.append(cell("api", "RESTful API（HTTP + JSON）    请求头携带 JWT Token", S_GRAY, 300, 292, 560, 36))
c.append(cell("b3", "后端服务层（Spring Boot 3.5，JDK 17）", S_BAND, 40, 350, 1080, 250))
c.append(cell("s0", "跨域配置 / JWT 认证拦截器 / @RequireRole 角色鉴权", S_BOXO, 80, 390, 1000, 40))
c.append(cell("s1", "Controller 控制层（认证 / 公开 / 学生 / 教师 / 管理 / 文件）", S_BOX, 80, 445, 480, 44))
c.append(cell("s2", "Service 业务层（16 个业务服务 + 事务控制）", S_BOX, 600, 445, 480, 44))
c.append(cell("s3", "Mapper 持久层（MyBatis-Plus 通用 CRUD + 条件构造器）", S_BOX, 80, 505, 480, 44))
c.append(cell("s4", "common：统一响应 / 全局异常        security：JWT 工具 / 用户上下文", S_GRAY, 600, 505, 480, 44))
c.append(cell("s5", "Apache POI 课件解析    Excel 导入导出    Word 导出    文件服务", S_BOXG, 80, 563, 1000, 30))
c.append(cell("b4", "数据与存储层", S_BAND, 40, 620, 1080, 110))
c.append(cell("d1", "MySQL 9.2\nonline_learning（20 张表）", S_DB, 200, 655, 260, 60))
c.append(cell("d2", "本地文件存储\nupload/（课件 / 视频 / 头像）", S_DB, 620, 655, 260, 60))
c.append(edge(nid(), "c1", "api", "Axios 请求"))
c.append(edge(nid(), "api", "s0", "HTTPS"))
c.append(edge(nid(), "s1", "s2"))
c.append(edge(nid(), "s2", "s3"))
c.append(edge(nid(), "s3", "d1", "SQL"))
c.append(edge(nid(), "s5", "d2", "读写"))
write_diagram("图2-1_系统总体架构图.drawio", c, width=1180, height=780)

# ============================================================
# 图2-2 前端架构图
# ============================================================
_id = [100]
c = []
c.append(cell("t0", "main.ts 入口 → createApp → 挂载 App.vue", S_GRAY, 320, 30, 480, 36))
c.append(cell("b1", "layouts 布局层", S_BAND, 40, 90, 1080, 80))
c.append(cell("l1", "前台学习布局（顶栏 + 内容区）", S_BOX, 120, 122, 300, 36))
c.append(cell("l2", "后台管理布局（侧边导航 + 面包屑）", S_BOX, 620, 122, 300, 36))
c.append(cell("b2", "views 页面层（24 个页面组件）", S_BAND, 40, 190, 1080, 100))
c.append(cell("v1", "前台：首页 / 课程广场 / 课程详情 / 学习页 / 测验 / 成绩 / 讨论 / 收藏 / 个人中心", S_BOX, 70, 230, 620, 44))
c.append(cell("v2", "教师端：学情看板 / 课程 / 章节 / 题库 / 试卷 / 作业 / 学生", S_BOX, 70, 288, 620, 44))
c.append(cell("v3", "管理端：统计 / 用户 / 审核 / 分类 / 公告", S_BOXP, 720, 230, 370, 44))
c.append(cell("v4", "登录 / 注册", S_BOXP, 720, 288, 370, 44))
c.append(cell("b3", "components 通用组件", S_BAND, 40, 350, 520, 130))
c.append(cell("m1", "FileUploader 统一上传", S_BOX, 70, 390, 220, 36))
c.append(cell("m2", "FilePreview 预览 + PPT 放映", S_BOX, 70, 434, 220, 36))
c.append(cell("b4", "支撑层", S_BAND, 580, 350, 540, 200))
c.append(cell("p1", "router：路由表 + 登录守卫 + 角色守卫", S_BOX, 610, 390, 250, 36))
c.append(cell("p2", "stores：Pinia 登录态（token / 用户信息）", S_BOX, 610, 434, 250, 36))
c.append(cell("p3", "utils/request：Axios 封装，统一携带 token、401 跳登录", S_BOX, 870, 390, 230, 70))
c.append(cell("p4", "utils/file：withToken / openFile / triggerDownload（直链带 ?token=）", S_BOX, 870, 468, 230, 66))
c.append(cell("b5", "styles 主题层", S_BAND, 40, 470, 520, 80))
c.append(cell("t1", "theme.css 设计令牌单一来源：暖米白底 #fdf7f1 + 主橙 #ff6b35，页面禁写裸色值；弹窗一律 append-to-body", S_BOXO, 70, 508, 460, 36))
c.append(edge(nid(), "t0", "l1"))
c.append(edge(nid(), "t0", "l2"))
c.append(edge(nid(), "l1", "v1"))
c.append(edge(nid(), "l2", "v3"))
c.append(edge(nid(), "v1", "m1", "上传 / 预览", S_EDGE, '<mxPoint x="300" y="360"/>'))
c.append(edge(nid(), "v2", "p1", "", S_EDGE))
c.append(edge(nid(), "v4", "p2"))
c.append(edge(nid(), "p3", "t1", "全站样式", S_EDGE))
write_diagram("图2-2_前端架构图.drawio", c, width=1160, height=590)

# ============================================================
# 图2-3 后端架构图
# ============================================================
_id = [100]
c = []
c.append(cell("t0", "HTTP 请求（Authorization: Bearer &lt;JWT&gt;）", S_GRAY, 330, 25, 460, 34))
c.append(cell("b0", "config / security 框架层", S_BAND, 40, 80, 1080, 80))
c.append(cell("w0", "WebConfig：跨域 + 拦截器注册", S_BOXO, 90, 112, 300, 36))
c.append(cell("w1", "JwtInterceptor：解析 token → UserContext → 校验 @RequireRole", S_BOXO, 430, 112, 640, 36))
c.append(cell("b1", "controller 控制层（按访问身份分组）", S_BAND, 40, 180, 1080, 90))
c.append(cell("c1", "AuthController\n登录注册", S_BOX, 60, 218, 150, 42))
c.append(cell("c2", "PublicController\n公开接口", S_BOX, 230, 218, 150, 42))
c.append(cell("c3", "StudentController\nStudyController", S_BOX, 400, 218, 170, 42))
c.append(cell("c4", "TeacherController\n教师端", S_BOX, 590, 218, 150, 42))
c.append(cell("c5", "AdminController\n管理端", S_BOX, 760, 218, 150, 42))
c.append(cell("c6", "FileController / PublicFileController / ExportController", S_BOX, 930, 218, 180, 42))
c.append(cell("b2", "service 业务层（业务规则 + 事务）", S_BAND, 40, 290, 1080, 130))
c.append(cell("s1", "ExamService 判分组卷批阅", S_BOX, 60, 330, 210, 40))
c.append(cell("s2", "CourseService / ChapterService / EnrollmentService", S_BOX, 290, 330, 280, 40))
c.append(cell("s3", "AssignmentService / StudyService / StatsService", S_BOX, 590, 330, 250, 40))
c.append(cell("s4", "DocPreviewService / FileService / ExportService / QuestionIOService / DiscussionService / UserService / NoticeService / CategoryService / CourseWorkService", S_BOXG, 60, 378, 1040, 36))
c.append(cell("b3", "mapper 持久层（MyBatis-Plus，20 个 Mapper）", S_BAND, 40, 430, 1080, 80))
c.append(cell("mp", "BaseMapper 通用 CRUD + LambdaQueryWrapper 条件构造 + 分页插件", S_BOX, 90, 468, 980, 34))
c.append(cell("b4", "公共与模型", S_BAND, 40, 520, 1080, 80))
c.append(cell("e1", "entity 实体（20 张表映射）", S_BOX, 90, 558, 300, 34))
c.append(cell("e2", "dto 请求 / 响应对象", S_BOX, 420, 558, 240, 34))
c.append(cell("e3", "common：Result 统一响应 / BusinessException / GlobalExceptionHandler", S_BOX, 690, 558, 420, 34))
c.append(cell("db", "MySQL 9.2\nonline_learning", S_DB, 430, 650, 240, 70))
c.append(edge(nid(), "t0", "w1"))
c.append(edge(nid(), "w1", "c4", "放行"))
c.append(edge(nid(), "c4", "s1", "调用"))
c.append(edge(nid(), "s2", "mp", "SQL"))
c.append(edge(nid(), "mp", "db"))
c.append(edge(nid(), "e3", "c4", "异常兜底", S_EDGE_L))
write_diagram("图2-3_后端架构图.drawio", c, width=1160, height=740)

# ============================================================
# 图2-4 / 图2-6 系统ER图（标准 Chen 表示法：矩形实体 + 椭圆属性 + 菱形联系 + 1/n 基数）
# ============================================================
S_ENT   = ("rounded=0;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#2f5597;"
           "fontSize=18;fontStyle=1;strokeWidth=2;")
S_ENTD  = ("rounded=0;whiteSpace=wrap;html=1;fillColor=#f5f5f5;strokeColor=#999999;dashed=1;"
           "fontSize=17;strokeWidth=2;")
S_ATTR  = "ellipse;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#2f5597;fontSize=14;strokeWidth=1;"
S_ATTRK = "ellipse;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#2f5597;fontSize=14;fontStyle=4;strokeWidth=1;"
S_REL   = "rhombus;whiteSpace=wrap;html=1;fillColor=#ffe6cc;strokeColor=#bf8f00;fontSize=15;strokeWidth=2;"
S_LINK  = "html=1;endArrow=none;strokeColor=#2f5597;strokeWidth=1;"
S_RELE  = ("html=1;endArrow=none;strokeColor=#333333;strokeWidth=1.5;fontSize=13;"
           "fontColor=#333333;labelBackgroundColor=#ffffff;")

SLOTS = {'NW': (-178, -138), 'NE': (178, -138), 'W': (-252, 0), 'E': (252, 0),
         'SW': (-178, 138), 'SE': (178, 138), 'N': (0, -156), 'S': (0, 156)}
ATTR_W, ATTR_H = 144, 46

def chen_entity(cid, name, cx, cy, attrs, slots, dashed=False, w=190, h=60):
    """标准 ER 实体：矩形 + 环绕椭圆属性。attrs: [(标签, 主键?)]，slots: 方位列表。"""
    c.append(cell(cid, name, S_ENTD if dashed else S_ENT, cx - w / 2, cy - h / 2, w, h))
    for i, (label, is_key) in enumerate(attrs):
        dx, dy = SLOTS[slots[i]]
        aid = f"{cid}_a{i}"
        c.append(cell(aid, label, S_ATTRK if is_key else S_ATTR,
                      cx + dx - ATTR_W / 2, cy + dy - ATTR_H / 2, ATTR_W, ATTR_H))
        c.append(edge(nid(), cid, aid, "", S_LINK))
    return cid

def chen_rel(rid, name, x, y, src, tgt, card_src="", card_tgt="", pts1="", pts2="", w=128, h=76):
    """标准 ER 联系：菱形 + 两端基数标注。"""
    c.append(cell(rid, name, S_REL, x - w / 2, y - h / 2, w, h))
    c.append(edge(nid(), src, rid, card_src, S_RELE, pts1))
    c.append(edge(nid(), rid, tgt, card_tgt, S_RELE, pts2))

def chen_note(nid_, text, x, y, w=520, h=40, size=14):
    c.append(cell(nid_, text,
                  f"text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=top;"
                  f"fontSize={size};fontColor=#555555;whiteSpace=wrap;", x, y, w, h))

# ---------------- 图2-4 教学组织与学习域 ----------------
_id = [100]
c = []
chen_entity("e_user", "用户", 430, 250,
            [("用户ID", True), ("用户编号", False), ("角色", False), ("账号状态", False)],
            ["NW", "NE", "W", "SW"])
chen_entity("e_course", "课程", 1060, 250,
            [("课程ID", True), ("课程名称", False), ("课程状态", False),
             ("选课审核", False), ("下载开关", False)],
            ["NW", "N", "NE", "SW", "SE"])
chen_entity("e_cat", "课程分类", 1720, 250,
            [("分类ID", True), ("分类名称", False), ("上级分类", False)],
            ["N", "NE", "E"])
chen_entity("e_chap", "章节", 1060, 790,
            [("章节ID", True), ("章节名称", False), ("排序号", False), ("内容", False)],
            ["NW", "NE", "S", "SE"])
chen_entity("e_res", "课件资源", 1720, 790,
            [("资源ID", True), ("资源名称", False), ("文件类型", False)],
            ["N", "NE", "SE"])
chen_entity("e_note", "学习笔记", 430, 790,
            [("笔记ID", True), ("笔记标题", False), ("内容摘要", False)],
            ["NW", "SW", "S"])

chen_rel("d_teach", "授课", 745, 250, "e_user", "e_course", "1", "n")
chen_rel("d_pick", "选课", 900, 250, "e_user", "e_course", "m", "n",
         '<mxPoint x="525" y="330"/><mxPoint x="836" y="330"/>',
         '<mxPoint x="964" y="330"/><mxPoint x="1050" y="300"/>')
chen_rel("d_fav", "收藏", 900, 470, "e_user", "e_course", "m", "n",
         '<mxPoint x="525" y="470"/>', '<mxPoint x="965" y="470"/>')
chen_rel("d_belong", "归类", 1390, 250, "e_course", "e_cat", "n", "1")
chen_rel("d_contain", "包含", 1060, 520, "e_course", "e_chap", "1", "n")
chen_rel("d_mount", "挂载", 1390, 790, "e_chap", "e_res", "1", "n")
chen_rel("d_study", "学习", 745, 520, "e_user", "e_chap", "m", "n")
chen_rel("d_writenote", "记录", 430, 520, "e_user", "e_note", "1", "n")

chen_note("nt1", "说明：带下划线的属性为主键；菱形为联系，两端标注联系基数（1/n/m）；"
                 "“选课”“学习”“收藏”为多对多联系，逻辑设计阶段各自转换为一张关联表。",
          120, 980, 1150, 50)
write_diagram("图2-4_系统ER图-教学组织与学习域.drawio", c, width=2080, height=1080)

# ---------------- 图2-5 考核与互动域 ----------------
c = []
chen_entity("f_user", "用户", 430, 830, [], [])
chen_entity("f_course", "课程", 1080, 230, [], [])
chen_entity("f_notice", "系统公告", 430, 230,
            [("公告ID", True), ("标题", False), ("置顶标志", False)],
            ["NW", "NE", "E"])
chen_entity("f_asg", "作业", 1080, 830,
            [("作业ID", True), ("截止时间", False), ("模板附件", False)],
            ["N", "NE", "SE"])
chen_entity("f_paper", "试卷", 1730, 830,
            [("试卷ID", True), ("总分", False), ("考试时长", False)],
            ["N", "NE", "E"])
chen_entity("f_post", "讨论帖", 430, 1430,
            [("帖子ID", True), ("标题", False), ("有帮助数", False)],
            ["W", "SW", "S"])
chen_entity("f_q", "题目", 1080, 1430,
            [("题目ID", True), ("题型", False), ("参考答案", False)],
            ["S", "SW", "SE"])
chen_entity("f_rec", "考试记录", 1730, 1430,
            [("记录ID", True), ("客观得分", False), ("批阅状态", False)],
            ["E", "SE", "S"])
chen_entity("f_reply", "讨论回复", 430, 2030,
            [("回复ID", True), ("回复内容", False), ("回复时间", False)],
            ["SW", "W", "S"])

chen_rel("g_assign", "布置", 1080, 530, "f_course", "f_asg", "1", "n")
chen_rel("g_submit", "提交", 755, 830, "f_user", "f_asg", "m", "n")
chen_rel("g_post", "发帖", 430, 1130, "f_user", "f_post", "1", "n")
chen_rel("g_help", "有帮助", 640, 980, "f_user", "f_post", "m", "n")
chen_rel("g_reply", "回复", 430, 1730, "f_post", "f_reply", "1", "n")
chen_rel("g_make", "组卷", 1405, 1130, "f_paper", "f_q", "m", "n")
chen_rel("g_mark", "批阅", 1405, 1430, "f_rec", "f_q", "m", "n")
chen_rel("g_match", "对应", 1730, 1130, "f_paper", "f_rec", "1", "n")
chen_rel("g_join", "参加", 900, 1180, "f_user", "f_rec", "1", "n",
         '<mxPoint x="525" y="1180"/><mxPoint x="836" y="1180"/>',
         '<mxPoint x="966" y="1180"/><mxPoint x="1660" y="1180"/><mxPoint x="1660" y="1430"/>')

chen_note("nt2", "说明：虚线矩形为在其他分图中已完整绘制的实体，此处仅示意联系关系；"
                 "“提交”“有帮助”“组卷”“批阅”为多对多联系，分别转换为作业提交表、有帮助表、试卷题目表、批阅明细表。",
          120, 2260, 1150, 60)
write_diagram("图2-5_系统ER图-考核与互动域.drawio", c, width=2080, height=2400)

# ============================================================
# 图2-6 JWT认证与权限校验时序图
# ============================================================
_id = [100]
c = []
def seq_participant(key, label, x):
    c.append(cell(key, label, S_BOX, x, 40, 160, 44))
    c.append(freeline(nid(), "", S_LIFE, x + 80, 84, x + 80, 860))
PX = {"cli": 40, "auth": 260, "int": 480, "ctx": 700, "biz": 900}
for k, label in [("cli", "浏览器（客户端）"), ("auth", "AuthController"), ("int", "JwtInterceptor"),
                 ("ctx", "UserContext（ThreadLocal）"), ("biz", "业务 Controller")]:
    seq_participant(k, label, PX[k])
def msg(y, src, dst, label, back=False):
    x1 = PX[src] + 80; x2 = PX[dst] + 80
    c.append(freeline(nid(), label, S_MSGR if back else S_MSG, x1, y, x2, y))
msg(130, "cli", "auth", "1：POST /api/auth/login（user_no + password）")
msg(180, "auth", "auth", "2：校验账号密码（MD5+盐比对）")
msg(230, "auth", "cli", "3：返回 JWT token", back=True)
msg(300, "cli", "int", "4：请求业务接口，携带 Authorization: Bearer token")
msg(350, "int", "int", "5：解析并校验 token 签名与有效期")
msg(400, "int", "ctx", "6：写入当前用户（ThreadLocal）")
msg(450, "int", "int", "7：读取并比对 @RequireRole 注解")
msg(520, "int", "cli", "8a：越权 → 返回 403 / 未登录 → 401", back=True)
msg(590, "int", "biz", "8b：校验通过 → 放行调用")
msg(650, "biz", "biz", "9：执行业务逻辑（Service / Mapper）")
msg(710, "biz", "cli", "10：返回统一响应 Result{code, message, data}", back=True)
msg(780, "int", "ctx", "11：请求结束，afterCompletion 清理 ThreadLocal")
write_diagram("图2-6_JWT认证与权限校验时序图.drawio", c, width=1120, height=900)

# ============================================================
# 图2-7 在线测验业务时序图
# ============================================================
_id = [100]
c = []
PX = {"stu": 40, "ctl": 280, "svc": 520, "db": 760, "tea": 960}
for k, label in [("stu", "学生（浏览器）"), ("ctl", "ExamController"), ("svc", "ExamService"),
                 ("db", "MySQL"), ("tea", "教师端")]:
    seq_participant(k, label, PX[k])
def msg(y, src, dst, label, back=False):
    x1 = PX[src] + 80; x2 = PX[dst] + 80
    c.append(freeline(nid(), label, S_MSGR if back else S_MSG, x1, y, x2, y))
msg(130, "stu", "ctl", "1：GET /api/papers/{id}/start 开始答题")
msg(180, "ctl", "svc", "2：startExam(paperId, userId)")
msg(230, "svc", "svc", "3：校验发布状态 / 时间窗 checkWindow / 是否已交卷")
msg(280, "svc", "db", "4：查询试卷与题目（withAnswer=false，剔除答案字段）")
msg(330, "db", "stu", "5：返回题目列表（不含 answer / analysis）", back=True)
msg(400, "stu", "stu", "6：loop 逐题作答，草稿防抖 800~1000ms 写入 localStorage")
msg(470, "stu", "ctl", "7：POST /api/exams/submit（answers 映射）")
msg(520, "ctl", "svc", "8：submit() 事务内：校验时间窗 + 重复提交拦截")
msg(570, "svc", "db", "9：逐题判分（客观题归一化比对 / 填空按空比对）")
msg(620, "svc", "db", "10：保存 exam_record（answers_json 快照 + 版本号）")
msg(680, "db", "stu", "11：返回成绩、逐题对错与解析", back=True)
msg(740, "svc", "tea", "12：含问答题 → mark_status=1 待批阅，教师逐题给分落 exam_mark")
msg(800, "tea", "stu", "13：学生查看最终成绩与教师评语", back=True)
write_diagram("图2-7_在线测验业务时序图.drawio", c, width=1120, height=870)

# ============================================================
# 图2-8 测验模块核心类图
# ============================================================
_id = [100]
c = []
svc_rows = [
    "- TYPE_SINGLE=1~TYPE_ESSAY=5 常量",
    "+ questions(courseId): List<Question>",
    "+ saveQuestion(q, teacherId) / removeQuestion()",
    "+ papers(courseId) / publishedPapers()",
    "+ savePaper(paper, teacherId): Long",
    "+ publishPaper(paperId, teacherId)  // 版本+1",
    "+ addQuestion() / removePaperQuestion()",
    "+ updatePaperQuestionScore()",
    "+ paperQuestions(paperId, withAnswer)",
    "+ startExam(paperId, userId): Map",
    "+ submit(userId, dto): ExamResultVO",
    "+ myRecords() / recordDetail() / paperRecords()",
    "- isCorrect(q, your): boolean",
    "- matchBlank(expected, actual): boolean",
    "- checkWindow(paper)  // 时间窗兜底",
    "- latestRecord(paperId, userId, version)",
]
cells, bottom = uml_class("cls_svc", 340, 40, 400, "ExamService（测验业务门面）", svc_rows)
c.extend(cells)
c.append(cell("stereo_svc", "«Service»  @Transactional 事务边界：submit / publishPaper", S_TITLE, 340, 12, 400, 24))

paper_rows = ["- id: Long", "- courseId: Long", "- title: String", "- totalScore / passScore: Integer",
              "- duration: Integer（分钟）", "- publishStatus: Integer", "- version: Integer（发布版本）",
              "- startTime / endTime: LocalDateTime"]
cells, _ = uml_class("cls_paper", 40, 340, 260, "ExamPaper 试卷", paper_rows, header_fill="#d5e8d4")
c.extend(cells)
q_rows = ["- id: Long", "- courseId: Long", "- type: Integer（1~5）", "- title: String",
          "- optionsJson: String", "- answer: String（判分依据）",
          "- referenceAnswer: String", "- analysis: String", "- difficulty: Integer"]
cells, _ = uml_class("cls_q", 340, 500, 260, "Question 题目", q_rows, header_fill="#d5e8d4")
c.extend(cells)
pq_rows = ["- id: Long", "- paperId: Long", "- questionId: Long", "- score: Integer（本题分值）", "- sort: Integer"]
cells, _ = uml_class("cls_pq", 40, 620, 260, "PaperQuestion 试卷题目关联", pq_rows, header_fill="#e1d5e7")
c.extend(cells)
rec_rows = ["- id: Long", "- paperId / paperVersion", "- userId: Long", "- answersJson: String（作答快照）",
            "- objectiveScore / subjectiveScore", "- score: Integer", "- markStatus: Integer（0/1/2）",
            "- passed: Integer", "- startTime / submitTime"]
cells, _ = uml_class("cls_rec", 760, 340, 270, "ExamRecord 考试记录", rec_rows, header_fill="#ffe6cc")
c.extend(cells)
mark_rows = ["- id: Long", "- recordId / questionId: Long", "- score: Integer（逐题给分）",
             "- comment: String（评语）", "- teacherId: Long"]
cells, _ = uml_class("cls_mark", 760, 640, 270, "ExamMark 批阅明细", mark_rows, header_fill="#ffe6cc")
c.extend(cells)
dto_rows = ["+ paperId: Long", "+ answers: Map<String,String>", "（题目ID → 作答，如 {\"9\":\"T\"}）"]
cells, _ = uml_class("cls_dto", 1060, 340, 230, "ExamSubmitDTO 交卷参数", dto_rows, header_fill="#f5f5f5")
c.extend(cells)
vo_rows = ["+ recordId / score", "+ objectiveScore / subjectiveScore", "+ markStatus / passed",
           "+ details: List<AnswerDetail>", "AnswerDetail：题目、我的答案、", "正确答案、解析、得分、needManual"]
cells, _ = uml_class("cls_vo", 1060, 560, 230, "ExamResultVO 判分结果", vo_rows, header_fill="#f5f5f5")
c.extend(cells)
c.append(edge(nid(), "cls_svc", "cls_paper", "查询 / 组卷 / 发布"))
c.append(edge(nid(), "cls_svc", "cls_q", "题库维护"))
c.append(edge(nid(), "cls_paper", "cls_pq", "1      n"))
c.append(edge(nid(), "cls_pq", "cls_q", "n      1", S_EDGE, '<mxPoint x="170" y="580"/>'))
c.append(edge(nid(), "cls_paper", "cls_rec", "1      n"))
c.append(edge(nid(), "cls_rec", "cls_mark", "1      n"))
c.append(edge(nid(), "cls_svc", "cls_dto", "submit 入参", S_EDGE, '<mxPoint x="770" y="330"/>'))
c.append(edge(nid(), "cls_svc", "cls_vo", "submit 返回", S_EDGE, '<mxPoint x="770" y="470"/>'))
write_diagram("图2-8_测验模块核心类图.drawio", c, width=1320, height=900)

# ============================================================
# 图2-9 课件在线预览处理流程图
# ============================================================
_id = [100]
c = []
c.append(cell("s0", "学生请求预览课件", S_START, 40, 180, 130, 44))
c.append(cell("auth", "鉴权与寻址（rid / url 双寻址，校验选课或公开权限）", S_BOX, 220, 180, 230, 44))
c.append(cell("d_type", "判断文件类型", S_DEC, 500, 160, 130, 80))
c.append(cell("pdf", "PDF：iframe 原生渲染", S_BOXG, 700, 60, 190, 40))
c.append(cell("img", "图片：img 标签渲染", S_BOXG, 700, 120, 190, 40))
c.append(cell("av", "音视频：video / audio 播放", S_BOXG, 700, 180, 190, 40))
c.append(cell("ooxml", "docx / pptx / xlsx\nApache POI（OOXML）", S_BOXO, 700, 250, 190, 56))
c.append(cell("ole2", "doc / ppt / xls（旧格式）\npoi-scratchpad（HWPF/HSLF/HSSF）", S_BOXO, 700, 330, 190, 56))
c.append(cell("na", "其他格式：提示不支持预览，可申请下载", S_GRAY, 700, 410, 190, 40))
c.append(cell("parse", "解析为统一内容块：段落文本 / 表格 / 逐页图片（base64，单文件 ≤4MB）", S_BOX, 460, 490, 300, 56))
c.append(cell("json", "返回 JSON 内容块列表", S_BOX, 460, 590, 200, 40))
c.append(cell("render", "前端 FilePreview 统一弹窗渲染（多页 / 滚动）", S_BOX, 200, 590, 210, 44))
c.append(cell("ppt", "pptx：全屏放映（方向键 / 空格翻页、自动播放、Esc 退出）", S_BOXP, 180, 680, 240, 56))
c.append(cell("dl", "下载按钮：受课程 resource_downloadable 开关控制", S_BOXP, 470, 680, 220, 56))
c.append(cell("e0", "预览完成", S_END, 470, 790, 120, 40))
c.append(edge(nid(), "s0", "auth"))
c.append(edge(nid(), "auth", "d_type"))
c.append(edge(nid(), "d_type", "pdf", "pdf"))
c.append(edge(nid(), "d_type", "img", "图片"))
c.append(edge(nid(), "d_type", "av", "音视频"))
c.append(edge(nid(), "d_type", "ooxml", "OOXML"))
c.append(edge(nid(), "d_type", "ole2", "旧二进制"))
c.append(edge(nid(), "d_type", "na", "其他"))
c.append(edge(nid(), "ooxml", "parse"))
c.append(edge(nid(), "ole2", "parse"))
c.append(edge(nid(), "pdf", "render", "直出", S_EDGE, '<mxPoint x="795" y="100"/><mxPoint x="150" y="100"/>'))
c.append(edge(nid(), "img", "render", "", S_EDGE, '<mxPoint x="150" y="140"/>'))
c.append(edge(nid(), "av", "render", "", S_EDGE, '<mxPoint x="150" y="200"/>'))
c.append(edge(nid(), "parse", "json"))
c.append(edge(nid(), "json", "render"))
c.append(edge(nid(), "render", "ppt", "pptx"))
c.append(edge(nid(), "render", "dl"))
c.append(edge(nid(), "ppt", "e0"))
c.append(edge(nid(), "dl", "e0"))
write_diagram("图2-9_课件在线预览处理流程图.drawio", c, width=950, height=880)

print("\n全部图表生成完毕")
