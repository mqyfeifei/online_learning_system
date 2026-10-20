#!/usr/bin/env bash
# ============================================================
# 接口冒烟测试脚本
# 用法： bash scripts/smoke-test.sh [BASE_URL]
#       默认 BASE_URL = http://localhost:8080
# 前提：后端已启动、数据库已执行 sql/init.sql
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

ENROLL2=$(req -X POST "$BASE/api/courses/2/enroll" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "选新课成功" "$ENROLL2" 200

FINISH=$(req -X POST "$BASE/api/chapters/1/finish?courseId=1" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "学完本章打卡成功" "$FINISH" 200

GRADES=$(req "$BASE/api/my/grades" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "我的成绩可访问" "$GRADES" 200

# ---------- 5. 测验判分 ----------
echo ""
echo "[5] 测验模块"
START=$(req "$BASE/api/papers/1/start" -H "Authorization: Bearer $STUDENT_TOKEN")
expect_code "开始答题成功" "$START" 200
if echo "$START" | grep -q '"answer"'; then
  ng "学生答题数据不应包含答案" "响应里出现了 answer 字段"
else
  ok "学生答题数据不含答案（防作弊）"
fi

SUBMIT=$(req -X POST "$BASE/api/exams/submit" \
  -H "Authorization: Bearer $STUDENT_TOKEN" -H "Content-Type: application/json" \
  -d '{"paperId":1,"answers":{"9":"T","10":"T"}}')
expect_code "交卷判分成功" "$SUBMIT" 200

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

# ---------- 汇总 ----------
echo ""
echo "=========================================="
echo " 通过：$PASS    失败：$FAIL"
echo "=========================================="
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
