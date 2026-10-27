#!/usr/bin/env bash
# ============================================================
# 接口冒烟测试脚本
# 用法： bash scripts/smoke-test.sh [BASE_URL]
#       默认 BASE_URL = http://localhost:8080
# 前提：后端已启动、数据库已执行 sql/init.sql
# 说明：脚本可重复执行。选课前会先退课，所以连跑多次结果一致
#       （不做这一步的话，第二次跑会在「选新课成功」上报 400 已选）
# ============================================================

BASE="${1:-http://localhost:8080}"
PASS=0
FAIL=0

# 统一请求：走本地直连，绕过系统代理
req() {
  curl -s --noproxy '*' -m 10 "$@"
}

ok() {
  PASS=$((PASS + 1))
  echo "  [PASS] $1"
}

ng() {
  FAIL=$((FAIL + 1))
  echo "  [FAIL] $1  ->  $2"
}

# 断言响应里包含某个 code
expect_code() {
  local name="$1" body="$2" want="$3"
  if echo "$body" | grep -q "\"code\":$want"; then
    ok "$name"
  else
    ng "$name" "$(echo "$body" | head -c 200)"
  fi
}

# 提取 token
get_token() {
  req -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"123456\"}" \
    | grep -o '"token":"[^"]*"' | cut -d'"' -f4
}

echo "=========================================="
echo " 在线学习平台 接口冒烟测试"
echo " 目标：$BASE"
echo "=========================================="

# ---------- 1. 认证 ----------
echo ""
echo "[1] 认证模块"
ADMIN_TOKEN=$(get_token admin)
TEACHER_TOKEN=$(get_token teacher1)
STUDENT_TOKEN=$(get_token student1)

[ -n "$ADMIN_TOKEN" ]   && ok "管理员登录，拿到 token"   || ng "管理员登录" "无 token"
[ -n "$TEACHER_TOKEN" ] && ok "教师登录，拿到 token"     || ng "教师登录" "无 token"
[ -n "$STUDENT_TOKEN" ] && ok "学生登录，拿到 token"     || ng "学生登录" "无 token"

BAD=$(req -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" \
      -d '{"username":"student1","password":"wrong-pwd"}')
expect_code "TC-02 错误密码被拒绝" "$BAD" 400

NOTOKEN=$(req "$BASE/api/my/courses")
expect_code "未登录访问受保护接口返回 401" "$NOTOKEN" 401

# ---------- 2. 前台公开接口 ----------
echo ""
echo "[2] 前台公开接口"
COURSES=$(req "$BASE/api/public/courses?sort=hot&size=3")
expect_code "课程广场可访问" "$COURSES" 200

DETAIL=$(req "$BASE/api/public/courses/1")
expect_code "课程详情可访问" "$DETAIL" 200

CATS=$(req "$BASE/api/public/categories")
expect_code "分类树可访问" "$CATS" 200

NOTICES=$(req "$BASE/api/public/notices")
expect_code "公告列表可访问" "$NOTICES" 200

# ---------- 3. 权限校验 ----------
echo ""
echo "[3] 权限校验"
FORBID=$(req "$BASE/api/teacher/courses" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "TC-03 学生调教师接口返回 403" "$FORBID" 403

STU_OK=$(req "$BASE/api/teacher/courses" -H "Authorization: Bearer $TEACHER_TOKEN")
expect_code "教师访问教师接口正常" "$STU_OK" 200

# ---------- 4. 学生端 ----------
echo ""
echo "[4] 学生端业务"
MY=$(req "$BASE/api/my/courses" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "我的课程可访问" "$MY" 200

ENROLL_AGAIN=$(req -X POST "$BASE/api/courses/1/enroll" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "TC-04 重复选课被拒绝" "$ENROLL_AGAIN" 400

# 先退课，保证脚本可重复执行（首次跑时本来就没选，退课返回 400，忽略即可）
req -X DELETE "$BASE/api/courses/2/enroll" -H "Authorization: Bearer $STUDENT_TOKEN" > /dev/null

ENROLL2=$(req -X POST "$BASE/api/courses/2/enroll" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "选新课成功" "$ENROLL2" 200

FINISH=$(req -X POST "$BASE/api/chapters/1/finish?courseId=1" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "学完本章打卡成功" "$FINISH" 200

GRADES=$(req "$BASE/api/my/grades" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "我的成绩可访问" "$GRADES" 200

# ---------- 5. 测验判分 ----------
echo ""
echo "[5] 测验模块"
# v4 起作业/测验只能提交一次，所以这里分两条路径：
#   没交过 → 完整跑一遍「开始答题 → 交卷 → 不能再进」
#   交过了 → 验证「不能再作答 + 不能重复交卷」这两条拦截规则
START=$(req "$BASE/api/papers/1/start" -H "Authorization: Bearer $STUDENT_TOKEN")
if echo "$START" | grep -q '"code":200'; then
  ok "开始答题成功"
  if echo "$START" | grep -q '"answer"'; then
    ng "学生答题数据不应包含答案" "响应里出现了 answer 字段"
  else
    ok "学生答题数据不含答案（防作弊）"
  fi

  SUBMIT=$(req -X POST "$BASE/api/exams/submit" \
    -H "Authorization: Bearer $STUDENT_TOKEN" -H "Content-Type: application/json" \
    -d '{"paperId":1,"answers":{"9":"T","10":"T"}}')
  expect_code "交卷判分成功" "$SUBMIT" 200

  AGAIN=$(req "$BASE/api/papers/1/start" -H "Authorization: Bearer $STUDENT_TOKEN")
  expect_code "交卷后不能再次进入答题" "$AGAIN" 400
else
  expect_code "已完成的测验不能重复作答" "$START" 400
  SUBMIT=$(req -X POST "$BASE/api/exams/submit" \
    -H "Authorization: Bearer $STUDENT_TOKEN" -H "Content-Type: application/json" \
    -d '{"paperId":1,"answers":{"9":"T","10":"T"}}')
  expect_code "重复交卷被拦截" "$SUBMIT" 400
fi

# ---------- 6. 教师端 ----------
echo ""
echo "[6] 教师端业务"
TC=$(req "$BASE/api/teacher/courses" -H "Authorization: Bearer $TEACHER_TOKEN")
expect_code "教师课程列表可访问" "$TC" 200

STUDENTS=$(req "$BASE/api/teacher/courses/1/students" -H "Authorization: Bearer $TEACHER_TOKEN")
expect_code "学生名单可访问" "$STUDENTS" 200

STATS=$(req "$BASE/api/teacher/stats" -H "Authorization: Bearer $TEACHER_TOKEN")
expect_code "学情看板数据可访问" "$STATS" 200

SUBS=$(req "$BASE/api/teacher/assignments/1/submissions" -H "Authorization: Bearer $TEACHER_TOKEN")
expect_code "作业提交列表可访问" "$SUBS" 200

# ---------- 7. 管理端 ----------
echo ""
echo "[7] 管理端业务"
USERS=$(req "$BASE/api/admin/users?current=1&size=5" -H "Authorization: Bearer $ADMIN_TOKEN")
expect_code "用户列表可访问" "$USERS" 200

AUDIT=$(req "$BASE/api/admin/courses?status=1" -H "Authorization: Bearer $ADMIN_TOKEN")
expect_code "待审核课程列表可访问" "$AUDIT" 200

STAT=$(req "$BASE/api/admin/stats" -H "Authorization: Bearer $ADMIN_TOKEN")
expect_code "平台统计可访问" "$STAT" 200

ADMIN_FORBID=$(req "$BASE/api/admin/users" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "学生调管理端接口返回 403" "$ADMIN_FORBID" 403

# ---------- 8. 学习增强（收藏 / 笔记 / 概览） ----------
# 说明：收藏是「开 / 关」切换，这里连点两次回到初始状态，保证脚本可重复执行
echo ""
echo "[8] 学习增强模块"
OVERVIEW=$(req "$BASE/api/my/overview" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "学习概览可访问" "$OVERVIEW" 200
if echo "$OVERVIEW" | grep -q '"completionRate"'; then
  ok "概览返回完成率字段"
else
  ng "概览返回完成率字段" "$(echo "$OVERVIEW" | head -c 200)"
fi

FAV1=$(req -X POST "$BASE/api/courses/1/favorite" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "收藏切换成功" "$FAV1" 200
FAV_STATE=$(req "$BASE/api/courses/1/favorite" -H "Authorization: Bearer $STUDENT_TOKEN")
if echo "$FAV_STATE" | grep -q '"favored":true'; then
  ok "收藏状态为已收藏"
else
  ng "收藏状态为已收藏" "$(echo "$FAV_STATE" | head -c 200)"
fi
# 再点一次取消，恢复到脚本运行前的状态
req -X POST "$BASE/api/courses/1/favorite" -H "Authorization: Bearer $STUDENT_TOKEN" > /dev/null

MYFAV=$(req "$BASE/api/my/favorites" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "我的收藏可访问" "$MYFAV" 200

NOTE=$(req -X POST "$BASE/api/my/notes" \
  -H "Authorization: Bearer $STUDENT_TOKEN" -H "Content-Type: application/json" \
  -d '{"courseId":1,"title":"冒烟测试笔记","content":"这是一条由 smoke-test 创建的笔记，可直接删除。"}')
expect_code "新增笔记成功" "$NOTE" 200
# 接口直接返回笔记 id（{"data":123}），这里也兼容返回对象（{"data":{"id":123}}）的情况
NOTE_ID=$(echo "$NOTE" | sed -n 's/.*"data":\([0-9]*\).*/\1/p' | head -1)
[ -z "$NOTE_ID" ] && NOTE_ID=$(echo "$NOTE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
if [ -n "$NOTE_ID" ]; then
  DEL=$(req -X DELETE "$BASE/api/my/notes/$NOTE_ID" -H "Authorization: Bearer $STUDENT_TOKEN")
  expect_code "删除笔记成功" "$DEL" 200
else
  ng "删除笔记成功" "新增接口没有返回 id：$(echo "$NOTE" | head -c 200)"
fi
MYNOTES=$(req "$BASE/api/my/notes" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "我的笔记可访问" "$MYNOTES" 200

# 游客可浏览课件（不需要登录）
PUBRES=$(req "$BASE/api/public/courses/1/resources")
expect_code "游客可浏览课程课件" "$PUBRES" 200

FAV_FORBID=$(req "$BASE/api/my/favorites")
expect_code "未登录访问收藏返回 401" "$FAV_FORBID" 401

# ---------- 汇总 ----------
echo ""
echo "=========================================="
echo " 通过：$PASS    失败：$FAIL"
echo "=========================================="
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
