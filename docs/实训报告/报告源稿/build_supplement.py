# -*- coding: utf-8 -*-
"""生成《报告补充内容与插入位置说明.docx》：不改动报告本身，只给插入指引与可复制内容。"""
import docx
from docx.shared import Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

OUT = r"D:\mqy\java\SpringBoot\online_learning_system\docs\实训报告\报告补充内容与插入位置说明.docx"

doc = docx.Document()

def set_font(run, east="宋体", ascii_f="Times New Roman", size=12, bold=False, color=None):
    run.font.name = ascii_f
    run.font.size = Pt(size)
    run.font.bold = bold
    rpr = run._element.get_or_add_rPr()
    rf = rpr.find(qn('w:rFonts'))
    if rf is None:
        rf = OxmlElement('w:rFonts'); rpr.append(rf)
    rf.set(qn('w:eastAsia'), east)
    if color:
        run.font.color.rgb = RGBColor(*color)

def h(text, size=14, space_before=14, color=None):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(space_before)
    p.paragraph_format.space_after = Pt(6)
    set_font(p.add_run(text), east="黑体", size=size, bold=True, color=color)
    return p

def para(text, size=12, bold=False, indent=True, color=None):
    p = doc.add_paragraph()
    p.paragraph_format.line_spacing = 1.5
    p.paragraph_format.space_after = Pt(2)
    if indent:
        p.paragraph_format.first_line_indent = Pt(size * 2)
    set_font(p.add_run(text), size=size, bold=bold, color=color)
    return p

def tree(text):
    p = doc.add_paragraph()
    p.paragraph_format.line_spacing = 1.0
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.left_indent = Pt(12)
    set_font(p.add_run(text), east="宋体", ascii_f="Consolas", size=9.5)
    return p

def shade(p, color="F2F2F2"):
    ppr = p._element.get_or_add_pPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear'); shd.set(qn('w:fill'), color)
    ppr.append(shd)

# ---------- 标题 ----------
t = doc.add_paragraph()
t.alignment = WD_ALIGN_PARAGRAPH.CENTER
set_font(t.add_run("实训报告补充内容与插入位置说明"), east="黑体", size=16, bold=True)
sub = doc.add_paragraph()
sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
set_font(sub.add_run("—— 对应《在线学习平台系统设计与实现-实训报告-莫秋裕.docx》"), east="楷体", size=10.5, color=(89, 89, 89))
sub.paragraph_format.space_after = Pt(10)

para("本文件不修改报告本身。下面给出两处建议补充的内容：每处包括插入位置、可直接复制进报告的具体文字、排版要求以及插入后需要同步调整的事项。两处均建议放在第 3 章 3.1 节“开发环境与工程结构”中，与该节主题（环境、工程结构、版本管理）完全吻合。", size=11)

# ---------- 补充内容一 ----------
h("一、补充内容一：工程目录结构树", 14)
para("【插入位置】第 3 章 3.1 节“开发环境与工程结构”：表3-1（开发与运行环境）之后、原段落“后端根包 com.mqy.olearning 下按 common、config……”之前。", bold=True)
para("【插入方式】将下面灰底区域的目录树文本整体粘贴为连续段落，字体建议设为 Consolas 9.5 磅（中文字体宋体）、左对齐、单倍行距、首行不缩进——与报告中代码片段的排版保持一致（若报告中使用了“实训代码”样式，直接套用该样式即可）。", size=11)
para("【勘误说明】你提供的目录树里有两处数字与项目实际不符，已在下文中按实际修正：entity 包对应 20 张数据表（不是 16 张）、init.sql 建 20 张表（不是 17 张）；冒烟测试 27 项断言与报告 3.3.3 节一致，保留不变。", size=11, color=(192, 0, 0))
para("【具体内容】（复制以下全部行）", size=11, bold=True)

tree_lines = [
"online_learning_system/",
"├── olearning-server/            后端（Spring Boot 3.5.16，Java 17）",
"│   └── src/main/java/com/mqy/olearning/",
"│       ├── common/              统一响应体、业务异常、全局异常处理",
"│       ├── config/              MyBatis-Plus、跨域与拦截器注册、接口文档",
"│       ├── security/            JWT 工具、拦截器、角色注解、用户上下文",
"│       ├── controller/          认证 / 公开 / 学生 / 教师 / 管理 / 文件 六组控制器",
"│       ├── service/             业务逻辑层（16 个业务服务）",
"│       ├── mapper/              MyBatis-Plus Mapper 持久层",
"│       ├── entity/              20 张数据表对应的实体类",
"│       ├── dto/                 请求 / 响应对象",
"│       └── utils/               密码加盐等工具",
"├── olearning-web/               前端（Vue 3 + Vite + TypeScript）",
"│   └── src/",
"│       ├── views/               前台页面（首页、课程广场、详情、学习、测验、成绩、讨论、个人中心）",
"│       ├── views/teacher/       教师端：学情看板、课程、章节、题库、试卷、作业、学生",
"│       ├── views/admin/         管理端：统计、用户、审核、分类、公告",
"│       ├── layouts/             前台布局、后台管理布局",
"│       ├── router/              路由表与角色守卫",
"│       ├── stores/              Pinia 登录态",
"│       └── utils/               Axios 封装",
"├── sql/init.sql                 建库脚本 + 20 张表 + 演示数据",
"├── sql/upgrade-v3.sql           老库升级到 v3（幂等，可重复执行）",
"├── sql/upgrade-v4.sql           老库升级到 v4（幂等，可重复执行）",
"├── scripts/smoke-test.sh        接口冒烟测试（27 项断言）",
"├── docs/screenshots/            联调验收截图",
"├── docker-compose.yml           容器化一键部署",
"└── README.md                    项目说明文档",
]
for ln in tree_lines:
    shade(tree(ln))

doc.add_paragraph()
para("【衔接建议】目录树后面紧跟的原有段落“后端根包 com.mqy.olearning 下按 common、config、security……工程全部源码由本人逐行编写、调试并整合。”是对目录树的文字概括，建议保留；若觉得与目录树重复，可精简为一句：“工程按上述结构组织，全部源码由本人逐行编写、调试并整合。”", size=11)

# ---------- 补充内容二 ----------
h("二、补充内容二：GitHub 远程仓库截图与版本管理说明", 14)
para("【插入位置】3.1 节末尾，即目录树（或“后端根包……”段落）之后、3.2 节“关键功能实现”标题之前。", bold=True)
para("【正文段落】（可直接复制，宋体小四、两端对齐、首行缩进 2 字符，与正文样式一致）", size=11, bold=True)
p = para("工程源码通过 Git 进行版本管理，并按功能粒度提交，提交信息与功能点一一对应（如“教师注册提醒、题库批量导入素材与课件预览修复”），提交历史完整记录了从工程初始化、完整功能实现到两轮功能迭代的演进轨迹；远程仓库托管于 GitHub（github.com/mqyfeifei/online_learning_system），仓库主页如图3-1所示。.gitignore 排除了 node_modules、target 等构建产物，仓库仅承载源码、脚本与文档。", size=12)
shade(p)

para("【插图】在上述段落之后插入 GitHub 仓库主页截图，图片下方居中放置图注：", size=11, bold=True)
cap = doc.add_paragraph()
cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
set_font(cap.add_run("图3-1 项目远程仓库主页（GitHub）"), size=10.5, bold=True)
shade(cap)
para("【截图方法】浏览器打开 https://github.com/mqyfeifei/online_learning_system ，截取仓库 Code 主页（含文件列表与提交信息，最好带上 README 区）。你提供的这张截图（mqyfeifei/online_learning_system 主页，含“这是一个前后端分离的在线学习系统”简介与语言占比条）内容完整、可以直接使用；建议裁掉浏览器标签页与书签栏，保留页面主体即可。插入后图片宽度约占版心 90%、居中，图注用与正文图一致的题注格式（宋体五号加粗居中，或套用报告中的“实训图注”样式）。", size=11)

# ---------- 连锁调整 ----------
h("三、插入后需要同步调整的事项", 14)
para("1. 图号顺延：原第 3 章的 图3-1～图3-9（位于 3.3.5 节“系统运行效果”的 9 张系统截图）顺延为 图3-2～图3-10。需要修改的只有这 9 处图注，正文其它地方没有引用这些图号。操作建议：Word 中 Ctrl+H 查找替换，务必按倒序执行——先把“图3-9”替换为“图3-10”，再“图3-8”→“图3-9”，……最后“图3-1”→“图3-2”，否则会产生连锁错乱。替换完成后 Ctrl+F 搜索“图3-”通查一遍。", size=11)
para("2. 表3-1“开发与运行环境”中“版本管理 | Git | Gitee 远程托管”一行，建议将“Gitee 远程托管”改为“GitHub 远程托管”，与截图保持一致。", size=11)
para("3. 字数影响：两处补充共增加约 350 字，报告总字数仍在 18000～28000 区间内，无需其他调整。", size=11)
para("4. 若目录树作为普通段落插入，注意它不应计入“图表”编号体系，也无需题注；若你更想把它做成插图（截图式），也可以改成“图3-2 工程目录结构”，但那样第 3 章图号还需再顺延一位，一般不建议。", size=11)

doc.save(OUT)
print("saved:", OUT)
