# -*- coding: utf-8 -*-
"""
把 upload/course/<课程名> 下的学习资料导入成平台课程。

做两件事：
1. 把源文件复制到 upload/yyyy/MM/uuid.ext（和网页上传走同一套目录与命名规则），
   访问地址固定为 /files/yyyy/MM/uuid.ext
2. 生成 SQL：课程 -> 章节 -> course_resource 记录，写进 build/import_courses.sql

用法：python scripts/import_courses.py     （只生成 SQL + 复制文件，不直接写库）
      mysql ... online_learning < build/import_courses.sql
"""
import os
import shutil
import uuid
from datetime import datetime

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
UPLOAD = os.path.join(BASE, "olearning-server", "upload")
SRC = os.path.join(UPLOAD, "course")
OUT = os.path.join(BASE, "build")

# ---------------------------------------------------------------- 课程定义
# chapters: (章节标题, [文件名, ...])；文件名必须真实存在于源目录
COURSES = [
    {
        "title": "编译原理",
        "teacher_id": 2,          # teacher1 刘老师
        "category": "编译原理",
        "description": "从编译程序的结构讲起，覆盖文法与语言、词法分析、自顶向下与自底向上语法分析、"
                       "语法制导翻译与中间代码生成。配套 7 章课件 PDF 与 6 个讲解视频。",
        "dir": "编译原理",
        "chapters": [
            ("第 1 章 编译原理概述", [
                "1 编译原理概述（2024）.pdf",
                "视频/1. 1 编译程序（系统）及其作用.mp4",
                "视频/1. 2 编译程序的组成.mp4",
            ]),
            ("第 2 章 文法和语言", [
                "2 文法和语言（汇总）.pdf",
                "视频/2 符号和符号串.mp4",
            ]),
            ("第 3 章 词法分析", [
                "第3章词法分析（汇总）.pdf",
                "视频/3 词法分析概述.mp4",
            ]),
            ("第 4 章 自顶向下的语法分析方法", [
                "第4章自顶向下的语法分析方法（汇总）.pdf",
                "视频/4 自顶向下的语法分析.mp4",
            ]),
            ("第 5 章 自底向上的优先分析法", [
                "第5章自底向上的优先分析法(汇总).pdf",
                "视频/5 自底向上的优先分析方法.mp4",
            ]),
            ("第 6 章 自底向上的 LR 分析法", [
                "第6章自底向上的LR分析法(2023).pdf",
            ]),
            ("第 7 章 语法制导翻译和中间代码生成", [
                "第7章语法制导翻译和中间代码的生成（2020）.pdf",
            ]),
        ],
    },
    {
        "title": "机器学习",
        "teacher_id": 3,          # teacher2 陈老师
        "category": "人工智能",
        "description": "数据预处理、模式挖掘与关联分析、分类与聚类、距离度量与概率图模型，"
                       "最后落到强化学习。课件按章节挂载，可在线查看与下载。",
        "dir": "机器学习",
        "chapters": [
            ("第 1 章 机器学习概述与数据预处理", [
                "1-1机器学习概述.pdf",
                "1-2机器学习模型及方法.pdf",
                "1-3数据挖掘引论.pdf",
                "1-4数据预处理 (1).pdf",
            ]),
            ("第 2 章 模式挖掘与关联分析", [
                "2模式挖掘与关联分析.pdf",
            ]),
            ("第 3 章 数据分类技术", [
                "3-1数据分类技术.pdf",
                "3-2线性模型.pdf",
                "3-3多层感知器+反向传播.pdf",
                "3-4支持向量机.pdf",
            ]),
            ("第 4 章 聚类与离群点分析", [
                "4-1数据聚类技术.pdf",
                "4-2离群点分析.pdf",
                "4-3维度约简.pdf",
            ]),
            ("第 5 章 距离度量与无监督 EM", [
                "5距离度量+无监督EM.pdf",
            ]),
            ("第 6 章 概率图模型", [
                "6概率图模型-贝叶斯网络.pdf",
            ]),
            ("第 7 章 强化学习", [
                "7强化学习-更新.pdf",
                "7参考资料.pdf",
            ]),
        ],
    },
]

VIDEO_EXT = {".mp4", ".webm", ".ogg", ".mov"}


def sql_escape(s: str) -> str:
    return s.replace("\\", "\\\\").replace("'", "''")


def main():
    os.makedirs(OUT, exist_ok=True)
    date_dir = datetime.now().strftime("%Y/%m")
    target_dir = os.path.join(UPLOAD, date_dir)
    os.makedirs(target_dir, exist_ok=True)

    lines = []
    lines.append("-- 由 scripts/import_courses.py 生成，可重复执行（先删同名课程再插）")
    lines.append("USE online_learning;")
    lines.append("")

    for c in COURSES:
        lines.append(
            "DELETE FROM course WHERE title = '%s' AND teacher_id = %d;"
            % (sql_escape(c["title"]), c["teacher_id"])
        )
        lines.append(
            "SET @cat := (SELECT id FROM course_category WHERE name = '%s' LIMIT 1);"
            % sql_escape(c["category"])
        )
        lines.append("INSERT INTO course (teacher_id, category_id, title, description, status, "
                     "enroll_audit, resource_downloadable, export_scope, view_count) VALUES "
                     "(%d, @cat, '%s', '%s', 2, 0, 1, 'ENROLLED', 0);"
                     % (c["teacher_id"], sql_escape(c["title"]), sql_escape(c["description"])))
        lines.append("SET @course_id := LAST_INSERT_ID();")

        src_root = os.path.join(SRC, c["dir"])
        for idx, (ch_title, files) in enumerate(c["chapters"], start=1):
            lines.append(
                "INSERT INTO chapter (course_id, title, sort, content) VALUES "
                "(@course_id, '%s', %d, '%s');"
                % (sql_escape(ch_title), idx, sql_escape(ch_title + " 课件与讲解视频"))
            )
            lines.append("SET @chapter_id := LAST_INSERT_ID();")

            for rel in files:
                src = os.path.join(src_root, *rel.split("/"))
                if not os.path.isfile(src):
                    print("  !! 缺少文件：%s" % src)
                    continue
                ext = os.path.splitext(src)[1].lower()
                new_name = uuid.uuid4().hex + ext
                shutil.copy2(src, os.path.join(target_dir, new_name))
                url = "/files/%s/%s" % (date_dir, new_name)
                size = os.path.getsize(src)
                # 视频挂到章节的 video_url 上，学生学习页能直接播
                if ext in VIDEO_EXT:
                    lines.append(
                        "UPDATE chapter SET video_url = '%s' WHERE id = @chapter_id "
                        "AND (video_url IS NULL OR video_url = '');" % url
                    )
                lines.append(
                    "INSERT INTO course_resource (course_id, chapter_id, name, file_url, file_type, file_size) "
                    "VALUES (@course_id, @chapter_id, '%s', '%s', '%s', %d);"
                    % (sql_escape(os.path.basename(src)), url, ext.lstrip("."), size)
                )
        lines.append("")

    path = os.path.join(OUT, "import_courses.sql")
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print("生成 SQL：%s" % path)


if __name__ == "__main__":
    main()
