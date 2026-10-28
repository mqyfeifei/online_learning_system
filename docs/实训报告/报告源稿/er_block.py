# ============================================================
# 图2-4 / 图2-5 系统ER图（标准 Chen 表示法：矩形实体 + 椭圆属性 + 菱形联系 + 1/n 基数）
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
          120, 2130, 1150, 60)
write_diagram("图2-5_系统ER图-考核与互动域.drawio", c, width=2080, height=2280)
